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
package org.isoron.uhabits.core.reminders

import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.commands.BulkSkipCommand
import org.isoron.uhabits.core.commands.ChangeHabitColorCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.core.utils.DateUtils.Companion.getStartOfDayWithOffset
import org.isoron.uhabits.core.utils.DateUtils.Companion.getTodayWithOffset
import org.isoron.uhabits.core.utils.DateUtils.Companion.getUtcTime
import org.isoron.uhabits.core.utils.DateUtils.Companion.getZoneId
import org.isoron.uhabits.core.utils.DateUtils.Companion.removeTimezone
import java.time.Instant
import javax.inject.Inject

@AppScope
class ReminderScheduler @Inject constructor(
    private val commandRunner: CommandRunner,
    private val habitList: HabitList,
    private val sys: SystemScheduler,
    private val widgetPreferences: WidgetPreferences
) : CommandRunner.Listener {
    @Synchronized
    override fun onCommandFinished(command: Command) {
        if (command is ChangeHabitColorCommand || command is ChangeHabitTagsCommand) return
        if (command is BulkSkipCommand) {
            for (habit in command.selected) schedule(habit)
            return
        }
        if (command is DeleteHabitsCommand) {
            for (habit in command.selected) habit.id?.let { cancel(it) }
        }
        scheduleAll()
    }

    @Synchronized
    fun schedule(habit: Habit, afterTime: Long = getUtcTime()) {
        val id = habit.id
        if (id == null) {
            sys.log("ReminderScheduler", "Habit has null id. Returning.")
            return
        }
        val reminder = habit.reminder
        if (reminder == null || reminder.days.isEmpty || habit.isArchived) {
            cancel(id)
            return
        }
        sys.cancelReminders(id)
        widgetPreferences.scheduledReminderHabitIds =
            (widgetPreferences.scheduledReminderHabitIds.toSet() + id).toLongArray()
        val completedToday = habit.isReminderSuppressedToday()
        val snoozeTime = widgetPreferences.getSnoozeTime(id)
        // Startup may run before an already-due snooze broadcast is delivered.
        if (snoozeTime > 0L && !completedToday) {
            scheduleAtTime(habit, snoozeTime, snoozed = true)
            return
        }
        if (snoozeTime != 0L) widgetPreferences.removeSnoozeTime(id)

        // A rolling alarm avoids an OS alarm limit even when a habit has many daily times.
        val zone = getZoneId()
        val firstDate = Instant.ofEpochMilli(afterTime).atZone(zone).toLocalDate()
        val days = reminder.days.toArray()
        for (offset in 0L..8L) {
            val next = habit.reminderTimes.map { minute ->
                firstDate.plusDays(offset).atTime(minute / 60, minute % 60).atZone(zone).toInstant().toEpochMilli()
            }.filter { time ->
                val day = Timestamp(getStartOfDayWithOffset(removeTimezone(time)))
                time > afterTime && days[day.weekday] && (!completedToday || day != getTodayWithOffset())
            }.minOrNull()
            if (next != null) {
                scheduleAtTime(habit, next)
                return
            }
        }
    }

    @Synchronized
    fun scheduleAtTime(habit: Habit, reminderTime: Long, snoozed: Boolean = false) {
        sys.log("ReminderScheduler", "Scheduling alarm for habit=" + habit.id)
        if (!habit.hasReminder()) {
            sys.log("ReminderScheduler", "habit=" + habit.id + " has no reminder. Skipping.")
            return
        }
        if (habit.isArchived) {
            sys.log("ReminderScheduler", "habit=" + habit.id + " is archived. Skipping.")
            return
        }
        val timestamp = getStartOfDayWithOffset(removeTimezone(reminderTime))
        if (snoozed) {
            sys.scheduleSnoozedReminder(reminderTime, habit, timestamp)
        } else {
            sys.scheduleShowReminder(reminderTime, habit, timestamp)
        }
    }

    @Synchronized
    fun scheduleAll() {
        sys.log("ReminderScheduler", "Scheduling all alarms")
        val currentIds = habitList.mapNotNull { it.id }.toSet()
        for (id in widgetPreferences.scheduledReminderHabitIds) {
            if (id !in currentIds) cancel(id)
        }
        for (habit in habitList) schedule(habit)
    }

    private fun cancel(id: Long) {
        sys.cancelReminders(id)
        widgetPreferences.removeSnoozeTime(id)
        widgetPreferences.scheduledReminderHabitIds =
            widgetPreferences.scheduledReminderHabitIds.filter { it != id }.toLongArray()
    }

    @Synchronized
    fun onReminderFired(habit: Habit, reminderTime: Long, snoozed: Boolean): Boolean {
        val zone = getZoneId()
        val localTime = Instant.ofEpochMilli(reminderTime).atZone(zone)
        val snoozeTime = widgetPreferences.getSnoozeTime(habit.id!!)
        val day = Timestamp(getStartOfDayWithOffset(removeTimezone(reminderTime)))
        val enabled = habit.hasReminder() && !habit.isArchived
        val selectedDay = habit.reminder?.days?.toArray()?.get(day.weekday) == true
        val isCurrent = enabled && if (snoozed) {
            snoozeTime != 0L && snoozeTime == reminderTime
        } else {
            selectedDay && snoozeTime <= getUtcTime() && habit.reminderTimes.any {
                localTime.toLocalDate().atTime(it / 60, it % 60).atZone(zone).toInstant().toEpochMilli() == reminderTime
            }
        }
        if (snoozed && isCurrent) widgetPreferences.removeSnoozeTime(habit.id!!)
        schedule(habit, maxOf(getUtcTime(), reminderTime))
        if (!isCurrent) sys.log("ReminderScheduler", "Ignoring obsolete reminder for habit=${habit.id}")
        return isCurrent
    }

    @Synchronized
    fun hasHabitsWithReminders(): Boolean {
        return !habitList.getFiltered(HabitMatcher.WITH_ALARM).isEmpty
    }

    @Synchronized
    fun startListening() {
        commandRunner.addListener(this)
    }

    @Synchronized
    fun stopListening() {
        commandRunner.removeListener(this)
    }

    @Synchronized
    fun snoozeReminder(habit: Habit, minutes: Long) {
        require(minutes >= 0) { "Snooze delay must not be negative" }
        val now = getUtcTime()
        val snoozedUntil = now + minutes * 60 * 1000
        snoozeAtTime(habit, snoozedUntil)
    }

    @Synchronized
    fun snoozeAtTime(habit: Habit, time: Long) {
        widgetPreferences.setSnoozeTime(habit.id!!, time)
        schedule(habit)
    }

    interface SystemScheduler {
        fun scheduleShowReminder(
            reminderTime: Long,
            habit: Habit,
            timestamp: Long
        ): SchedulerResult

        fun scheduleSnoozedReminder(reminderTime: Long, habit: Habit, timestamp: Long): SchedulerResult
        fun cancelReminders(habitId: Long)
        fun scheduleWidgetUpdate(updateTime: Long): SchedulerResult?
        fun log(componentName: String, msg: String)
    }

    enum class SchedulerResult {
        IGNORED, OK
    }
}
