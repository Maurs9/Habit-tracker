/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat.checkSelfPermission
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.HabitsActivity
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.core.ui.screens.habits.list.HabitListEmptyState
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.database.AutoBackup
import org.isoron.uhabits.inject.ActivityContextModule
import org.isoron.uhabits.inject.DaggerHabitsActivityComponent
import org.isoron.uhabits.inject.HabitsActivityComponent
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.dismissCurrentDialog

class ListHabitsActivity : HabitsActivity(), Preferences.Listener {

    private lateinit var appliedTheme: ThemeSwitcher.Variant
    private var launchIntentHandled = false
    lateinit var appComponent: HabitsApplicationComponent
    lateinit var component: HabitsActivityComponent
    lateinit var taskRunner: TaskRunner
    lateinit var adapter: HabitCardListAdapter
    lateinit var rootView: ListHabitsRootView
    lateinit var screen: ListHabitsScreen
    lateinit var prefs: Preferences
    lateinit var midnightTimer: MidnightTimer

    private var permissionAlreadyRequested = false
    private val permissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted && isContentReady) {
                scheduleReminders()
            } else if (!isGranted) {
                Log.i("ListHabitsActivity", "POST_NOTIFICATIONS denied")
            }
        }

    private lateinit var menu: ListHabitsMenu

    override fun onQuestionMarksChanged() {
        invalidateOptionsMenu()
        menu.behavior.onPreferencesChanged()
    }

    override fun onCreateReady(savedInstanceState: Bundle?) {
        launchIntentHandled = savedInstanceState?.getBoolean(LAUNCH_INTENT_HANDLED) == true
        appComponent = (applicationContext as HabitsApplication).component
        component = DaggerHabitsActivityComponent
            .builder()
            .activityContextModule(ActivityContextModule(this))
            .habitsApplicationComponent(appComponent)
            .build()
        component.themeSwitcher.apply()

        prefs = appComponent.preferences
        prefs.addListener(this)
        appliedTheme = component.themeSwitcher.themeVariant
        midnightTimer = appComponent.midnightTimer
        rootView = component.listHabitsRootView
        screen = component.listHabitsScreen
        adapter = component.habitCardListAdapter
        taskRunner = appComponent.taskRunner
        menu = component.listHabitsMenu
        rootView.onEmptyAction = { state ->
            when (state) {
                HabitListEmptyState.NO_HABITS -> menu.behavior.onCreateHabit()
                HabitListEmptyState.ARCHIVED -> {
                    if (!prefs.showArchived) menu.behavior.onToggleShowArchived()
                }
                HabitListEmptyState.ENTERED,
                HabitListEmptyState.COMPLETED -> {
                    if (!prefs.showCompleted) menu.behavior.onToggleShowCompleted()
                }
                else -> menu.behavior.onClearFilters()
            }
            invalidateOptionsMenu()
        }
        component.listHabitsBehavior.onStartup()
        rootView.applyRootViewInsets()
        setContentView(rootView)
    }

    override fun onPauseReady() {
        midnightTimer.onPause()
        screen.onDetached()
        adapter.cancelRefresh()
        dismissCurrentDialog(preserveEntryDrafts = true)
    }

    override fun onDestroyReady() {
        prefs.removeListener(this)
    }

    override fun onResumeReady() {
        if (component.themeSwitcher.themeVariant != appliedTheme) {
            recreate()
            return
        }
        adapter.refresh()
        screen.onAttached()
        rootView.postInvalidate()
        midnightTimer.onResume()

        if (appComponent.reminderScheduler.hasHabitsWithReminders()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                scheduleReminders()
            } else {
                if (checkSelfPermission(this, POST_NOTIFICATIONS) == PERMISSION_GRANTED) {
                    scheduleReminders()
                } else {
                    // If we have not requested the permission yet, request it. Otherwide do
                    // nothing. This check is necessary to avoid an infinite onResume loop in case
                    // the user denies the permission.
                    if (!permissionAlreadyRequested) {
                        Log.i("ListHabitsActivity", "Requestion permission: POST_NOTIFICATIONS")
                        permissionLauncher.launch(POST_NOTIFICATIONS)
                        permissionAlreadyRequested = true
                    }
                }
            }
        }

        taskRunner.run {
            try {
                AutoBackup(this@ListHabitsActivity).run()
                appComponent.widgetUpdater.updateWidgets()
            } catch (e: Exception) {
                Log.e("ListHabitActivity", "TaskRunner failed", e)
            }
        }
        parseIntents()
    }

    private fun scheduleReminders() {
        appComponent.reminderScheduler.scheduleAll()
    }

    override fun onCreateOptionsMenuReady(menu: Menu): Boolean {
        this.menu.onCreate(menuInflater, menu)
        return true
    }

    override fun onOptionsItemSelectedReady(item: MenuItem): Boolean {
        invalidateOptionsMenu()
        return menu.onItemSelected(item) || super.onOptionsItemSelectedReady(item)
    }

    override fun onActivityResultReady(requestCode: Int, resultCode: Int, data: Intent?) {
        screen.onResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceStateReady(outState: Bundle) {
        outState.putBoolean(LAUNCH_INTENT_HANDLED, launchIntentHandled)
    }

    private fun parseIntents() {
        // recreate() relaunches with the original intent; only handle it once per launch.
        if (launchIntentHandled || intent == null) return
        if (intent.action == ACTION_EDIT) {
            val habitId = intent.extras?.getLong("habit")
            val timestamp = intent.extras?.getLong("timestamp")
            if (habitId != null && timestamp != null) {
                val habit = appComponent.habitList.getById(habitId)
                if (habit != null) {
                    component.listHabitsBehavior.onEdit(habit, Timestamp(timestamp), 0f, 0f)
                } else {
                    Toast.makeText(this, R.string.reminder_habit_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        }
        launchIntentHandled = true
        intent = null
    }

    override fun onNewIntentReady(intent: Intent?) {
        setIntent(intent)
        launchIntentHandled = false
    }

    companion object {
        const val ACTION_EDIT = "org.isoron.uhabits.ACTION_EDIT"
        private const val LAUNCH_INTENT_HANDLED = "launchIntentHandled"
    }
}
