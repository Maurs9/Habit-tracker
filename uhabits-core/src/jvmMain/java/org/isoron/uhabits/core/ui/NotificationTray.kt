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
package org.isoron.uhabits.core.ui

import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.commands.BulkSkipCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.ChangeReminderTimesCommand
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import java.util.HashMap
import java.util.Locale
import java.util.Objects
import javax.inject.Inject

@AppScope
class NotificationTray @Inject constructor(
    private val taskRunner: TaskRunner,
    private val commandRunner: CommandRunner,
    private val preferences: Preferences,
    private val systemTray: SystemTray
) : CommandRunner.Listener, Preferences.Listener {
    private val active: HashMap<Long, Pair<Habit, NotificationData>> = HashMap()
    fun cancel(habit: Habit) {
        val notificationId = getNotificationId(habit)
        systemTray.removeNotification(notificationId)
        active.remove(habit.id)
    }

    override fun onCommandFinished(command: Command) {
        if (command is ChangeHabitTagsCommand) return
        if (command is CreateRepetitionCommand) {
            val (_, habit) = command
            cancel(habit)
        }
        if (command is BulkSkipCommand) {
            for (habit in command.selected) {
                if (habit.isReminderSuppressedToday()) cancel(habit)
            }
        }
        if (command is DeleteHabitsCommand) {
            val (_, deleted) = command
            for (habit in deleted) cancel(habit)
        }
        for ((habit, _) in active.values.toList()) {
            if (!habit.hasReminder() || habit.isArchived ||
                habit.isReminderSuppressedToday() ||
                (command is ChangeReminderTimesCommand && command.habitId == habit.id)
            ) {
                cancel(habit)
            }
        }
    }

    override fun onNotificationsChanged() {
        reshowAll()
    }

    fun show(habit: Habit, timestamp: Timestamp, reminderTime: Long, snoozed: Boolean = false) {
        val id = habit.id
        if (id == null) {
            systemTray.log("Cannot show reminder for unsaved habit")
            return
        }
        val data = NotificationData(timestamp, reminderTime, snoozed)
        active[id] = habit to data
        taskRunner.execute(ShowNotificationTask(habit, data))
    }

    fun startListening() {
        commandRunner.addListener(this)
        preferences.addListener(this)
    }

    fun stopListening() {
        commandRunner.removeListener(this)
        preferences.removeListener(this)
    }

    private fun getNotificationId(habit: Habit): Int {
        val id = habit.id ?: return 0
        return (id % Int.MAX_VALUE).toInt()
    }

    private fun reshowAll() {
        for ((habit, data) in active.values) {
            taskRunner.execute(ShowNotificationTask(habit, data))
        }
    }

    fun reshow(habit: Habit) {
        active[habit.id]?.let { (_, data) ->
            taskRunner.execute(ShowNotificationTask(habit, data))
        }
    }

    interface SystemTray {
        fun removeNotification(notificationId: Int)
        fun showNotification(
            habit: Habit,
            notificationId: Int,
            timestamp: Timestamp,
            reminderTime: Long
        )

        fun log(msg: String)
    }

    internal class NotificationData(val timestamp: Timestamp, val reminderTime: Long, val snoozed: Boolean)
    private inner class ShowNotificationTask(private val habit: Habit, private val data: NotificationData) :
        Task {
        var isSuppressed = false
        private val timestamp: Timestamp = data.timestamp
        private val reminderTime: Long = data.reminderTime

        override fun doInBackground() {
            isSuppressed = habit.isReminderSuppressedToday()
        }

        override fun onPostExecute() {
            if (active[habit.id]?.second !== data) return
            systemTray.log("Showing notification for habit=" + habit.id)
            if (isSuppressed) {
                systemTray.log(
                    String.format(
                        Locale.US,
                        "Habit %d already checked. Skipping.",
                        habit.id
                    )
                )
                return
            }
            if (!habit.hasReminder()) {
                systemTray.log(
                    String.format(
                        Locale.US,
                        "Habit %d does not have a reminder. Skipping.",
                        habit.id
                    )
                )
                return
            }
            if (habit.isArchived) {
                systemTray.log(
                    String.format(
                        Locale.US,
                        "Habit %d is archived. Skipping.",
                        habit.id
                    )
                )
                return
            }
            if (!data.snoozed && !shouldShowReminderToday()) {
                systemTray.log(
                    String.format(
                        Locale.US,
                        "Habit %d not supposed to run today. Skipping.",
                        habit.id
                    )
                )
                return
            }
            systemTray.showNotification(
                habit,
                getNotificationId(habit),
                timestamp,
                reminderTime
            )
        }

        private fun shouldShowReminderToday(): Boolean {
            if (!habit.hasReminder()) return false
            val reminder = habit.reminder
            val reminderDays = Objects.requireNonNull(reminder)!!.days.toArray()
            val weekday = timestamp.weekday
            return reminderDays[weekday]
        }
    }

    companion object {
        const val REMINDERS_CHANNEL_ID = "REMINDERS"
    }
}
