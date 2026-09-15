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

import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.reminders.ReminderScheduler
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.utils.DateUtils.Companion.getUpcomingTimeInMillis
import javax.inject.Inject

@AppScope
class ReminderController @Inject constructor(
    private val reminderScheduler: ReminderScheduler,
    private val notificationTray: NotificationTray,
    private val preferences: Preferences
) {
    fun onBootCompleted() {
        reminderScheduler.scheduleAll()
    }

    fun onShowReminder(
        habit: Habit,
        timestamp: Timestamp,
        reminderTime: Long,
        snoozed: Boolean = false
    ) {
        if (reminderScheduler.onReminderFired(habit, reminderTime, snoozed)) {
            notificationTray.show(habit, timestamp, reminderTime, snoozed)
        }
    }

    fun onSnoozeDelayPicked(habit: Habit, delayInMinutes: Int) {
        reminderScheduler.snoozeReminder(habit, delayInMinutes.toLong())
        notificationTray.cancel(habit)
    }

    fun onSnoozeTimePicked(habit: Habit, hour: Int, minute: Int) {
        val time: Long = getUpcomingTimeInMillis(hour, minute)
        reminderScheduler.snoozeAtTime(habit, time)
        notificationTray.cancel(habit)
    }

    fun onDismiss(habit: Habit) {
        if (preferences.shouldMakeNotificationsSticky()) {
            // This is a workaround to keep sticky notifications non-dismissible in Android 14+.
            // If the notification is dismissed, we immediately reshow it.
            notificationTray.reshow(habit)
        } else {
            notificationTray.cancel(habit)
        }
    }
}
