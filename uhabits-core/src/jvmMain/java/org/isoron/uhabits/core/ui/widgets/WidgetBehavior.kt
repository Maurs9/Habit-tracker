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
package org.isoron.uhabits.core.ui.widgets

import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.commands.UpdateRepetitionCommand
import org.isoron.uhabits.core.io.Logging
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.NotificationTray
import javax.inject.Inject

class WidgetBehavior @Inject constructor(
    private val habitList: HabitList,
    private val commandRunner: CommandRunner,
    private val notificationTray: NotificationTray,
    private val preferences: Preferences,
    private val logging: Logging
) {
    fun onAddRepetition(habit: Habit, timestamp: Timestamp?) {
        commandRunner.run(UpdateRepetitionCommand.SetValue(habitList, habit, timestamp!!, Entry.YES_MANUAL, logging))
        notificationTray.cancel(habit)
    }

    fun onRemoveRepetition(habit: Habit, timestamp: Timestamp?) {
        commandRunner.run(UpdateRepetitionCommand.SetValue(habitList, habit, timestamp!!, Entry.NO, logging))
        notificationTray.cancel(habit)
    }

    fun onToggleRepetition(habit: Habit, timestamp: Timestamp) {
        commandRunner.run(
            UpdateRepetitionCommand.Toggle(
                habitList,
                habit,
                timestamp,
                isSkipEnabled = preferences.isSkipEnabled,
                areQuestionMarksEnabled = preferences.areQuestionMarksEnabled,
                logging = logging
            )
        )
        notificationTray.cancel(habit)
    }

    fun onIncrement(habit: Habit, timestamp: Timestamp, amount: Int) {
        require(amount >= 0) { "Increment amount must not be negative" }
        commandRunner.run(UpdateRepetitionCommand.Adjust(habitList, habit, timestamp, amount, logging))
        notificationTray.cancel(habit)
    }

    fun onDecrement(habit: Habit, timestamp: Timestamp, amount: Int) {
        require(amount >= 0) { "Decrement amount must not be negative" }
        commandRunner.run(UpdateRepetitionCommand.Adjust(habitList, habit, timestamp, -amount, logging))
        notificationTray.cancel(habit)
    }

    fun setValue(habit: Habit, timestamp: Timestamp?, newValue: Int, notes: String) {
        commandRunner.run(
            CreateRepetitionCommand(habitList, habit, timestamp!!, newValue, notes)
        )
    }
}
