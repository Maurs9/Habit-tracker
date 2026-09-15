package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BulkSkipCommandTest : BaseUnitTest() {
    @Test
    fun preservesRecordedValuesAndNotes() {
        val today = DateUtils.getTodayWithOffset()
        val habit = modelFactory.buildHabit()
        habitList.add(habit)
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL, "Done"))
        habit.originalEntries.add(Entry(today.minus(1), Entry.NO))
        habit.originalEntries.add(Entry(today.minus(2), Entry.UNKNOWN, "Keep this note"))
        habit.recompute()

        val command = BulkSkipCommand(habitList, listOf(habit), today.minus(4), today)
        assertEquals(2L, command.countEligibleEntries())
        command.run()
        assertEquals(Entry(today, Entry.YES_MANUAL, "Done"), habit.originalEntries.get(today))
        assertEquals(Entry.NO, habit.originalEntries.get(today.minus(1)).value)
        assertEquals("Keep this note", habit.originalEntries.get(today.minus(2)).notes)
        assertEquals(Entry.UNKNOWN, habit.originalEntries.get(today.minus(2)).value)
        assertEquals(Entry.SKIP, habit.originalEntries.get(today.minus(3)).value)
        assertEquals(Entry.SKIP, habit.originalEntries.get(today.minus(4)).value)
        assertEquals(0L, command.countEligibleEntries())
    }

    @Test
    fun worksForNumericalHabitsAndPreservesZero() {
        val today = DateUtils.getTodayWithOffset()
        val habit = modelFactory.buildHabit().apply { type = HabitType.NUMERICAL }
        habitList.add(habit)
        habit.originalEntries.add(Entry(today, 0))
        habit.recompute()
        BulkSkipCommand(habitList, listOf(habit), today.minus(1), today).run()
        assertEquals(0, habit.originalEntries.get(today).value)
        assertEquals(Entry.SKIP, habit.originalEntries.get(today.minus(1)).value)
    }

    @Test
    fun rechecksValuesChangedAfterPreview() {
        val today = DateUtils.getTodayWithOffset()
        val habit = modelFactory.buildHabit()
        habitList.add(habit)
        val command = BulkSkipCommand(habitList, listOf(habit), today, today)
        assertEquals(1L, command.countEligibleEntries())
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        command.run()
        assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(today).value)
    }

    @Test
    fun rejectsReversedRange() {
        val today = DateUtils.getTodayWithOffset()
        assertFailsWith<IllegalArgumentException> {
            BulkSkipCommand(habitList, emptyList(), today, today.minus(1))
        }
    }

    @Test
    fun rejectsEpochBeforeWritingEntries() {
        val habit = modelFactory.buildHabit()
        habitList.add(habit)
        assertFailsWith<IllegalArgumentException> {
            BulkSkipCommand(habitList, listOf(habit), Timestamp.ZERO, Timestamp(Timestamp.DAY_LENGTH)).run()
        }
        assertEquals(0, habit.originalEntries.getKnown().size)
    }
}
