package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangeHabitSectionCommandTest : BaseUnitTest() {
    @Test
    fun assignsAndClearsOnlySelectedHabitsAndNotifiesCache() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val first = modelFactory.buildHabit().apply {
            name = "Read"
            tags = setOf("Daily")
            sectionId = morning.id
        }
        val second = modelFactory.buildHabit().apply { sectionId = morning.id }
        habitList.add(first)
        habitList.add(second)
        val before = first.copy()
        val cache = HabitCardListCache(habitList, sectionList, commandRunner, taskRunner, StandardLogging())
        cache.onAttached()
        try {
            commandRunner.run(ChangeHabitSectionCommand(habitList, listOf(first), evening.id))
            assertEquals(before.copy(sectionId = evening.id), first)
            assertEquals(morning.id, second.sectionId)
            verify(habitList).update(listOf(first))
            assertEquals(evening.id, cache.getHabitByPosition(habitList.indexOf(first))!!.sectionId)
            commandRunner.run(ChangeHabitSectionCommand(habitList, listOf(first), null))
            assertNull(first.sectionId)
            assertEquals(before.copy(sectionId = null), first)
            assertEquals(morning.id, second.sectionId)
        } finally {
            cache.onDetached()
        }
    }

    @Test
    fun rejectsInvalidIdWithoutMutatingHabits() {
        val habit = modelFactory.buildHabit().also { habitList.add(it) }
        for (id in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                ChangeHabitSectionCommand(habitList, listOf(habit), id).run()
            }
        }
        assertNull(habit.sectionId)
    }
}
