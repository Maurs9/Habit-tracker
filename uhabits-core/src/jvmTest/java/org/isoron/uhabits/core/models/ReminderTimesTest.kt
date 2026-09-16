package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.ChangeReminderTimesCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

class ReminderTimesTest : BaseUnitTest() {
    @Test
    fun testLegacyPrimaryAndSharedWeekdaysArePreserved() {
        val habit = fixtures.createEmptyHabit()
        habit.reminder = Reminder(12, 30, WeekdayList(62))
        habit.replaceReminderTimes(listOf(8 * 60, 12 * 60 + 30, 20 * 60))
        assertEquals(Reminder(12, 30, WeekdayList(62)), habit.reminder)
        assertEquals(setOf(8 * 60, 20 * 60), habit.extraReminderTimes)
        habit.replaceReminderTimes(listOf(8 * 60, 20 * 60))
        assertEquals(Reminder(8, 0, WeekdayList(62)), habit.reminder)
        habit.replaceReminderTimes(emptyList())
        assertNull(habit.reminder)
        assertTrue(habit.extraReminderTimes.isEmpty())
    }

    @Test
    fun testAllDistinctMinutesAreAcceptedWithoutArtificialCap() {
        val habit = fixtures.createEmptyHabit()
        habit.replaceReminderTimes((0 until 24 * 60).toList())
        assertEquals(1440, habit.reminderTimes.size)
        assertEquals(1439, habit.extraReminderTimes.size)
    }

    @Test
    fun testNormalizationAndInvalidTimes() {
        assertEquals(setOf(0, 480, 1439), ReminderTimes.parse("1439,0,480,480"))
        assertEquals("0,480,1439", ReminderTimes.format(listOf(1439, 480, 0)))
        assertThrows(IllegalArgumentException::class.java) { ReminderTimes.normalize(listOf(-1)) }
        assertThrows(IllegalArgumentException::class.java) { ReminderTimes.parse("1440") }
        assertThrows(IllegalArgumentException::class.java) { ReminderTimes.parse("eight") }
        assertThrows(IllegalArgumentException::class.java) { Reminder(24, 0, WeekdayList.EVERY_DAY) }
    }

    @Test
    fun testEditAndCopyPreserveTimesAndTagsAndDisableClearsExtras() {
        val habit = fixtures.createEmptyHabit()
        habit.tags = setOf("Health")
        habit.replaceReminderTimes(listOf(480, 720, 1200))
        habitList.add(habit)
        val modified = modelFactory.buildHabit()
        modified.copyFrom(habit)
        modified.name = "Edited"
        modified.reminder = Reminder(9, 0, WeekdayList(62))
        EditHabitCommand(habitList, habit.id!!, modified).run()
        assertEquals(setOf(540, 720, 1200), habit.reminderTimes)
        assertEquals(setOf("Health"), habit.tags)
        val copy = habit.copy()
        assertEquals(habit, copy)
        assertEquals(habit.hashCode(), copy.hashCode())
        copy.extraReminderTimes = setOf(900)
        assertNotEquals(habit, copy)
        modified.reminder = null
        EditHabitCommand(habitList, habit.id!!, modified).run()
        assertTrue(habit.extraReminderTimes.isEmpty())
        assertTrue(habit.reminderTimes.isEmpty())
    }

    @Test
    fun testTimesCommandChangesOnlyReminderTimesAndCsvIncludesSchedule() {
        val habit = fixtures.createEmptyHabit()
        habit.tags = setOf("Health")
        habit.reminder = Reminder(8, 0, WeekdayList(62))
        habitList.add(habit)
        ChangeReminderTimesCommand(habitList, habit.id!!, listOf(480, 1230)).run()
        assertEquals(setOf("Health"), habit.tags)
        assertEquals(62, habit.reminder!!.days.toInteger())
        val csv = StringWriter().also { habitList.writeCSV(it) }.toString()
        assertTrue(csv.contains("Tags,Section,ReminderTimes,ReminderDays"))
        assertTrue(csv.contains("Health,,08:00;20:30,62"))
    }
}
