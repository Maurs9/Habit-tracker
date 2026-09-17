package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.io.Logging
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitNotFoundException
import org.isoron.uhabits.core.models.Timestamp

sealed class UpdateRepetitionCommand(
    private val habitList: HabitList,
    val habit: Habit,
    val timestamp: Timestamp,
    logging: Logging
) : Command {
    private val logger = logging.getLogger("UpdateRepetitionCommand")

    final override fun run() {
        val habitId = habit.id ?: throw HabitNotFoundException()
        val currentHabit = habitList.getById(habitId)
        if (currentHabit == null) {
            logger.info("Cancelled repetition update for deleted habit=$habitId")
            return
        }
        val entry = currentHabit.originalEntries.get(timestamp)
        CreateRepetitionCommand(
            habitList,
            currentHabit,
            timestamp,
            updatedValue(currentHabit, entry),
            entry.notes
        ).run()
    }

    protected abstract fun updatedValue(currentHabit: Habit, entry: Entry): Int

    class SetValue(
        habitList: HabitList,
        habit: Habit,
        timestamp: Timestamp,
        private val value: Int,
        logging: Logging
    ) : UpdateRepetitionCommand(habitList, habit, timestamp, logging) {
        override fun updatedValue(currentHabit: Habit, entry: Entry): Int = value
    }

    class Toggle(
        habitList: HabitList,
        habit: Habit,
        timestamp: Timestamp,
        private val isSkipEnabled: Boolean,
        private val areQuestionMarksEnabled: Boolean,
        logging: Logging
    ) : UpdateRepetitionCommand(habitList, habit, timestamp, logging) {
        override fun updatedValue(currentHabit: Habit, entry: Entry): Int =
            Entry.nextToggleValue(entry.value, isSkipEnabled, areQuestionMarksEnabled)
    }

    class Adjust(
        habitList: HabitList,
        habit: Habit,
        timestamp: Timestamp,
        private val delta: Int,
        logging: Logging
    ) : UpdateRepetitionCommand(habitList, habit, timestamp, logging) {
        override fun updatedValue(currentHabit: Habit, entry: Entry): Int {
            // Negative values represent statuses or invalid legacy adjustments, not quantities.
            val current = currentHabit.computedEntries.get(timestamp).value.coerceAtLeast(0)
            return (current.toLong() + delta).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
