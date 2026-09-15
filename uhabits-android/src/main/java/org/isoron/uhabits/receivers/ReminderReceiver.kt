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
package org.isoron.uhabits.receivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.utils.DateUtils.Companion.getStartOfTodayWithOffset

/**
 * The Android BroadcastReceiver for Loop Habit Tracker.
 *
 *
 * All broadcast messages are received and processed by this class.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == null) return
        lastReceivedIntent = intent
        val app = context.applicationContext as HabitsApplication
        val appComponent = app.component
        val habits = appComponent.habitList
        val reminderController = appComponent.reminderController
        Log.i(TAG, String.format("Received intent: %s", intent.toString()))
        val today: Long = getStartOfTodayWithOffset()
        val data = intent.data
        val habit = data?.lastPathSegment?.toLongOrNull()?.let { habits.getById(it) }
        val timestamp = intent.getLongExtra("timestamp", today)
        val reminderTime = intent.getLongExtra("reminderTime", today)
        try {
            when (intent.action) {
                ACTION_SHOW_REMINDER, ACTION_SHOW_SNOOZED_REMINDER -> {
                    if (habit == null) return
                    val snoozed = intent.action == ACTION_SHOW_SNOOZED_REMINDER
                    Log.d(
                        "ReminderReceiver",
                        String.format(
                            "onShowReminder habit=%d timestamp=%d reminderTime=%d snoozed=%s",
                            habit.id,
                            timestamp,
                            reminderTime,
                            snoozed
                        )
                    )
                    reminderController.onShowReminder(
                        habit,
                        Timestamp(timestamp),
                        reminderTime,
                        snoozed = snoozed
                    )
                }
                ACTION_DISMISS_REMINDER -> {
                    if (habit == null) return
                    Log.d("ReminderReceiver", String.format("onDismiss habit=%d", habit.id))
                    reminderController.onDismiss(habit)
                }
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                    Log.d("ReminderReceiver", "onBootCompleted")
                    reminderController.onBootCompleted()
                }
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "could not process intent", e)
        }
    }

    companion object {
        const val ACTION_DISMISS_REMINDER = "org.isoron.uhabits.ACTION_DISMISS_REMINDER"
        const val ACTION_SHOW_REMINDER = "org.isoron.uhabits.ACTION_SHOW_REMINDER"
        const val ACTION_SHOW_SNOOZED_REMINDER = "org.isoron.uhabits.ACTION_SHOW_SNOOZED_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "org.isoron.uhabits.ACTION_SNOOZE_REMINDER"
        private const val TAG = "ReminderReceiver"
        var lastReceivedIntent: Intent? = null
            private set

        fun clearLastReceivedIntent() {
            lastReceivedIntent = null
        }
    }
}
