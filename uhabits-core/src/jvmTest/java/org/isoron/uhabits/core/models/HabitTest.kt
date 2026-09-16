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
package org.isoron.uhabits.core.models

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual.equalTo
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.utils.DateUtils.Companion.getToday
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitTest : BaseUnitTest() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testUuidGeneration() {
        val uuid1 = modelFactory.buildHabit().uuid!!
        val uuid2 = modelFactory.buildHabit().uuid!!
        assertNotEquals(uuid1, uuid2)
    }

    @Test
    fun test_copyAttributes() {
        val model = modelFactory.buildHabit()
        model.isArchived = true
        model.color = PaletteColor(0)
        model.frequency = Frequency(10, 20)
        model.reminder = Reminder(8, 30, WeekdayList(1))
        model.sectionId = 42L
        val habit = modelFactory.buildHabit()
        habit.copyFrom(model)
        assertEquals(habit.isArchived, model.isArchived)
        assertThat(habit.isArchived, `is`(model.isArchived))
        assertThat(habit.color, `is`(model.color))
        assertThat(habit.frequency, equalTo(model.frequency))
        assertThat(habit.reminder, equalTo(model.reminder))
        assertEquals(model.sectionId, habit.sectionId)
        assertEquals(model, habit)
        assertEquals(model.hashCode(), habit.hashCode())
        habit.sectionId = null
        assertNotEquals(model, habit)
        assertNotEquals(model.hashCode(), habit.hashCode())
        model.sectionId = null
        habit.copyFrom(model)
        assertEquals(model, habit)
    }

    @Test
    fun test_hasReminder() {
        val h = modelFactory.buildHabit()
        assertThat(h.hasReminder(), `is`(false))
        h.reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
        assertThat(h.hasReminder(), `is`(true))
    }

    @Test
    @Throws(Exception::class)
    fun test_isCompleted() {
        val h = modelFactory.buildHabit()
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), Entry.YES_MANUAL))
        h.recompute()
        assertTrue(h.isCompletedToday())
    }

    @Test
    fun test_isCompleted_value() {
        val habit = modelFactory.buildHabit()
        for (value in listOf(Entry.YES_MANUAL, Entry.YES_AUTO, Entry.SKIP)) {
            assertTrue(habit.isCompleted(value))
        }
        for (value in listOf(Entry.NO, Entry.UNKNOWN)) {
            assertFalse(habit.isCompleted(value))
        }
    }

    @Test
    fun test_isCompleted_value_numerical() {
        val habit = modelFactory.buildHabit().apply {
            type = HabitType.NUMERICAL
            targetType = NumericalHabitType.AT_LEAST
            targetValue = 10.5
            frequency = Frequency(1, 7)
        }
        for (value in listOf(10500, 10501, 20000)) assertTrue(habit.isCompleted(value))
        for (value in listOf(10499, 2000, Entry.NO, Entry.UNKNOWN, Entry.SKIP)) {
            assertFalse(habit.isCompleted(value))
        }
        habit.targetType = NumericalHabitType.AT_MOST
        for (value in listOf(0, 10499, 10500, 10501, Entry.UNKNOWN, Entry.SKIP)) {
            assertFalse(habit.isCompleted(value))
        }
    }

    @Test
    fun test_isCompletedToday_usesComputedValueRule() {
        val habit = modelFactory.buildHabit()
        for (type in HabitType.entries) {
            habit.type = type
            for (targetType in NumericalHabitType.entries) {
                habit.targetType = targetType
                habit.targetValue = 10.5
                for (value in listOf(Entry.UNKNOWN, Entry.NO, Entry.SKIP, Entry.YES_MANUAL, 10500, 11000)) {
                    habit.originalEntries.add(Entry(getToday(), value))
                    habit.recompute()
                    assertEquals(
                        habit.isCompleted(habit.computedEntries.get(getToday()).value),
                        habit.isCompletedToday()
                    )
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun test_isEntered() {
        val h = modelFactory.buildHabit()
        assertFalse(h.isEnteredToday())
        h.originalEntries.add(Entry(getToday(), Entry.NO))
        h.recompute()
        assertTrue(h.isEnteredToday())
    }

    @Test
    @Throws(Exception::class)
    fun test_isCompleted_numerical() {
        val h = modelFactory.buildHabit()
        h.type = HabitType.NUMERICAL
        h.targetType = NumericalHabitType.AT_LEAST
        h.targetValue = 100.0
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 200000))
        h.recompute()
        assertTrue(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 100000))
        h.recompute()
        assertTrue(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 50000))
        h.recompute()
        assertFalse(h.isCompletedToday())
        h.targetType = NumericalHabitType.AT_MOST
        h.originalEntries.add(Entry(getToday(), 200000))
        h.recompute()
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 100000))
        h.recompute()
        assertFalse(h.isCompletedToday())
        h.originalEntries.add(Entry(getToday(), 50000))
        h.recompute()
        assertFalse(h.isCompletedToday())
    }

    @Test
    @Throws(Exception::class)
    fun testURI() {
        assertTrue(habitList.isEmpty)
        val h = modelFactory.buildHabit()
        habitList.add(h)
        assertThat(h.id, equalTo(0L))
        assertThat(h.uriString, equalTo("content://org.isoron.uhabits/habit/0"))
    }
}
