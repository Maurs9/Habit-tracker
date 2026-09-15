package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitNotFoundException
import org.isoron.uhabits.core.models.ReminderTimes

class ChangeReminderTimesCommand(
    private val habits: HabitList,
    val habitId: Long,
    times: Collection<Int>
) : Command {
    private val times = ReminderTimes.normalize(times)

    override fun run() {
        val habit = habits.getById(habitId) ?: throw HabitNotFoundException()
        habit.replaceReminderTimes(times)
        habits.update(habit)
        habit.observable.notifyListeners()
    }
}
