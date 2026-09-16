package org.isoron.uhabits.core.ui.screens.habits.show

import org.isoron.platform.time.DayOfWeek
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.views.LightTheme
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ShowHabitStateTest : BaseUnitTest() {
    @Test
    fun resolvesCurrentSectionNameAndKeepsUnsectionedStateEmpty() {
        val habit = fixtures.createEmptyHabit()
        val preferences: Preferences = mock()
        whenever(preferences.firstWeekday).thenReturn(DayOfWeek.SUNDAY)
        fun state() = ShowHabitPresenter.buildState(habit, preferences, LightTheme(), sectionList)
        assertNull(state().subtitle.sectionName)
        val section = sectionList.add("Morning")
        habit.sectionId = section.id
        assertEquals("Morning", state().subtitle.sectionName)
        sectionList.rename(section, "Evening")
        assertEquals("Evening", state().subtitle.sectionName)
        habit.sectionId = 999
        assertNull(state().subtitle.sectionName)
    }

    @Test
    fun tagCommandUpdatesTheDisplayedHabitWithoutReplacingIt() {
        val habit = fixtures.createEmptyHabit()
        habitList.add(habit)
        val preferences: Preferences = mock()
        whenever(preferences.firstWeekday).thenReturn(DayOfWeek.SUNDAY)
        ChangeHabitTagsCommand(habitList, listOf(habit), setOf(" Health ", "health", "Morning")).run()
        assertSame(habit, habitList.getById(habit.id!!))
        val state = ShowHabitPresenter.buildState(habit, preferences, LightTheme(), sectionList)
        assertEquals(setOf("Health", "Morning"), state.subtitle.tags)
    }
}
