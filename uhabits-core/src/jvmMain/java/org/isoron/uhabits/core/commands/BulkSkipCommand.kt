package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.Timestamp

data class BulkSkipCommand(
    val habitList: HabitList,
    val selected: List<Habit>,
    val from: Timestamp,
    val to: Timestamp
) : Command {
    init {
        require(from <= to) { "The start date must not follow the end date" }
        require(from.unixTime >= Timestamp.DAY_LENGTH) {
            "Bulk skip requires dates after January 1, 1970"
        }
    }

    fun countEligibleEntries(): Long =
        selected.sumOf { habit -> eligibleDates(habit).count().toLong() }

    override fun run() {
        selected.forEach { habit ->
            val dates = eligibleDates(habit).toList()
            dates.forEach { habit.originalEntries.add(Entry(it, Entry.SKIP)) }
            if (dates.isNotEmpty()) habit.recompute()
        }
        habitList.resort()
    }

    private fun eligibleDates(habit: Habit): Sequence<Timestamp> =
        generateSequence(from) { current ->
            if (current >= to) null else current.plus(1)
        }.filter { date ->
            val original = habit.originalEntries.get(date)
            original.value == Entry.UNKNOWN &&
                original.notes.isEmpty() &&
                habit.computedEntries.get(date).value == Entry.UNKNOWN
        }
}
