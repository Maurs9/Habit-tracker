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

package org.isoron.uhabits

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import org.isoron.uhabits.core.database.UnsupportedDatabaseVersionException
import org.isoron.uhabits.core.reminders.ReminderScheduler
import org.isoron.uhabits.core.tasks.StartupCoordinator
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.utils.DateUtils.Companion.setStartDayOffset
import org.isoron.uhabits.inject.AppContextModule
import org.isoron.uhabits.inject.DaggerHabitsApplicationComponent
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.inject.HabitsModule
import org.isoron.uhabits.utils.DatabaseUtils
import org.isoron.uhabits.widgets.WidgetUpdater
import java.io.File
import java.util.concurrent.Executor

/**
 * The Android application for Loop Habit Tracker.
 */
class HabitsApplication : Application() {

    private lateinit var context: Context
    private lateinit var widgetUpdater: WidgetUpdater
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var notificationTray: NotificationTray
    lateinit var startup: StartupCoordinator
        private set
    private var listenersStarted = false

    override fun onCreate() {
        super.onCreate()
        context = this
        BaseExceptionHandler.install(this)

        if (isTestMode()) {
            val db = DatabaseUtils.getDatabaseFile(context)
            if (db.exists()) db.delete()
        }

        try {
            DatabaseUtils.initializeDatabase(context)
        } catch (e: UnsupportedDatabaseVersionException) {
            val db = DatabaseUtils.getDatabaseFile(context)
            db.renameTo(File(db.absolutePath + ".invalid"))
            DatabaseUtils.initializeDatabase(context)
        }

        val db = DatabaseUtils.getDatabaseFile(this)
        HabitsApplication.component = DaggerHabitsApplicationComponent
            .builder()
            .appContextModule(AppContextModule(context))
            .habitsModule(HabitsModule(db))
            .build()

        val prefs = component.preferences
        prefs.lastAppVersion = BuildConfig.VERSION_CODE

        if (prefs.isMidnightDelayEnabled) {
            setStartDayOffset(3, 0)
        } else {
            setStartDayOffset(0, 0)
        }

        val appComponent = component
        val mainHandler = Handler(Looper.getMainLooper())
        val callbacks = Executor { task ->
            if (Looper.myLooper() == Looper.getMainLooper()) task.run() else mainHandler.post(task)
        }
        val worker = if (isTestMode()) {
            Executor { it.run() }
        } else {
            Executor { task ->
                Thread(
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        task.run()
                    },
                    "habit-initialization"
                ).start()
            }
        }
        startup = StartupCoordinator(
            worker,
            callbacks,
            initialize = {
                val started = System.nanoTime()
                val habits = appComponent.habitList
                for (habit in habits) habit.recompute()
                Log.i(
                    "HabitsApplication",
                    "Initialized ${habits.size()} habits in ${(System.nanoTime() - started) / 1_000_000} ms"
                )
            },
            finish = { startListeners(appComponent) }
        )
        startup.observe { state ->
            if (state is StartupCoordinator.State.Failed) {
                Log.e("HabitsApplication", "Could not initialize habit history", state.cause)
            }
        }
        startup.start()
    }

    private fun startListeners(appComponent: HabitsApplicationComponent) {
        widgetUpdater = appComponent.widgetUpdater
        reminderScheduler = appComponent.reminderScheduler
        notificationTray = appComponent.notificationTray
        try {
            widgetUpdater.startListening()
            reminderScheduler.startListening()
            notificationTray.startListening()
            widgetUpdater.scheduleStartDayWidgetUpdate()
            appComponent.taskRunner.execute {
                reminderScheduler.scheduleAll()
                widgetUpdater.updateWidgets()
            }
            listenersStarted = true
        } catch (e: RuntimeException) {
            widgetUpdater.stopListening()
            reminderScheduler.stopListening()
            notificationTray.stopListening()
            throw e
        }
    }

    override fun onTerminate() {
        if (listenersStarted) {
            reminderScheduler.stopListening()
            widgetUpdater.stopListening()
            notificationTray.stopListening()
        }
        super.onTerminate()
    }

    fun awaitStartup() {
        check(startup.state != StartupCoordinator.State.Loading || Looper.myLooper() != Looper.getMainLooper()) {
            "Habit initialization must not block the main thread"
        }
        startup.awaitReady()
    }

    val component: HabitsApplicationComponent
        get() = HabitsApplication.component

    companion object {
        lateinit var component: HabitsApplicationComponent

        fun isTestMode(): Boolean {
            return try {
                Class.forName("org.isoron.uhabits.BaseAndroidTest")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
