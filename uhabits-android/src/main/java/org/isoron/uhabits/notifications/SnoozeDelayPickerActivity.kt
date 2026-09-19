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
package org.isoron.uhabits.notifications

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.HabitsActivity
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.receivers.ReminderController
import org.isoron.uhabits.utils.SystemUtils
import java.util.Calendar

class SnoozeDelayPickerActivity : HabitsActivity() {
    private lateinit var habits: HabitList
    private lateinit var reminderController: ReminderController
    private var dialog: AlertDialog? = null
    private var androidColor: Int = 0

    override val showsStartupPanel: Boolean get() = false

    override fun onBeforeCreate() {
        val app = applicationContext as HabitsApplication
        AndroidThemeSwitcher(this, app.component.preferences).setTheme()
    }

    override fun onCreateReady(savedInstanceState: Bundle?) {
        val app = applicationContext as HabitsApplication
        val appComponent = app.component
        val themeSwitcher = AndroidThemeSwitcher(this, appComponent.preferences)
        themeSwitcher.setTheme()

        habits = appComponent.habitList
        reminderController = appComponent.reminderController
        val habit = getHabitOrFinish() ?: return
        androidColor = themeSwitcher.currentTheme.color(habit.color).toInt()

        val timePicker = supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as TimePickerDialog?
        if (timePicker != null) {
            bindTimePicker(timePicker)
        } else {
            showDelayPicker()
        }
        SystemUtils.unlockScreen(this)
    }

    override fun onNewIntentReady(intent: Intent?) {
        setIntent(intent)
        dismissDelayPicker()
        val timePicker = supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as TimePickerDialog?
        if (timePicker != null) {
            timePicker.setOnTimeSetListener(null)
            timePicker.setDismissListener(null)
            supportFragmentManager.beginTransaction().remove(timePicker).commitNowAllowingStateLoss()
        }
        val habit = getHabitOrFinish() ?: return
        val themeSwitcher = AndroidThemeSwitcher(this, (applicationContext as HabitsApplication).component.preferences)
        themeSwitcher.setTheme()
        androidColor = themeSwitcher.currentTheme.color(habit.color).toInt()
        showDelayPicker()
    }

    private fun getHabitOrFinish(): Habit? {
        val data = intent?.data
        val habitId = data?.takeIf {
            it.scheme == "content" &&
                it.authority == "org.isoron.uhabits" &&
                it.pathSegments.size == 2 &&
                it.pathSegments[0] == "habit"
        }?.lastPathSegment?.toLongOrNull()
        val habit = habitId?.let { habits.getById(it) }
        if (habit == null || !habit.hasReminder() || habit.isArchived) {
            Toast.makeText(this, R.string.reminder_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return null
        }
        return habit
    }

    private fun showDelayPicker() {
        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.select_snooze_delay)
            .setItems(R.array.snooze_picker_names) { _, position ->
                val habit = getHabitOrFinish() ?: return@setItems
                val delay = resources.getIntArray(R.array.snooze_picker_values)[position]
                if (delay >= 0) {
                    reminderController.onSnoozeDelayPicked(habit, delay)
                    finish()
                } else {
                    dismissDelayPicker()
                    showTimePicker()
                }
            }
            .create()
        dialog!!.setOnDismissListener { finish() }
        dialog!!.show()
    }

    private fun AndroidThemeSwitcher.setTheme() {
        if (this.isNightMode) {
            setTheme(R.style.BaseDialogDark)
            this.currentTheme = DarkTheme()
        } else {
            setTheme(R.style.BaseDialog)
            this.currentTheme = LightTheme()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val dialog = TimePickerDialog.newInstance(
            null,
            calendar[Calendar.HOUR_OF_DAY],
            calendar[Calendar.MINUTE],
            DateFormat.is24HourFormat(this),
            androidColor
        )
        bindTimePicker(dialog)
        dialog.show(supportFragmentManager, TIME_PICKER_TAG)
    }

    private fun bindTimePicker(picker: TimePickerDialog) {
        picker.setOnTimeSetListener { _, hour, minute ->
            val habit = getHabitOrFinish() ?: return@setOnTimeSetListener
            reminderController.onSnoozeTimePicked(habit, hour, minute)
            finish()
        }
        picker.setDismissListener { finish() }
    }

    private fun dismissDelayPicker() {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
    }

    override fun onDestroyReady() {
        dismissDelayPicker()
        (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as TimePickerDialog?)?.apply {
            setOnTimeSetListener(null)
            setDismissListener(null)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val TIME_PICKER_TAG = "timePicker"
    }
}
