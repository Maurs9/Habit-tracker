package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HabitListEmptyStateTest : BaseUnitTest() {
    @Test
    fun distinguishesFirstUseArchiveAndMissingTags() {
        val habit = modelFactory.buildHabit().apply {
            isArchived = true
            tags = setOf("Morning")
        }
        assertEquals(HabitListEmptyState.NO_HABITS, HabitListEmptyState.classify(emptyList(), HabitMatcher()))
        assertEquals(HabitListEmptyState.ARCHIVED, HabitListEmptyState.classify(listOf(habit), HabitMatcher()))
        assertEquals(
            HabitListEmptyState.FILTERED,
            HabitListEmptyState.classify(listOf(habit), HabitMatcher(requiredTags = setOf("Evening")))
        )
        assertEquals(HabitListEmptyState.NONE, HabitListEmptyState.classify(listOf(habit), HabitMatcher(isArchivedAllowed = true)))
    }

    @Test
    fun distinguishesEnteredFromCompletedWithoutChangingLimitSemantics() {
        val habit = modelFactory.buildHabit().apply {
            type = HabitType.NUMERICAL
            targetType = NumericalHabitType.AT_MOST
            targetValue = 5.0
            originalEntries.add(Entry(DateUtils.getTodayWithOffset(), 2000))
            recompute()
        }
        assertFalse(habit.isCompletedToday())
        assertEquals(HabitListEmptyState.ENTERED, HabitListEmptyState.classify(listOf(habit), HabitMatcher(isEnteredAllowed = false)))
        assertEquals(HabitListEmptyState.NONE, HabitListEmptyState.classify(listOf(habit), HabitMatcher(isCompletedAllowed = false)))
        habit.type = HabitType.YES_NO
        habit.originalEntries.add(Entry(DateUtils.getTodayWithOffset(), Entry.YES_MANUAL))
        habit.recompute()
        assertEquals(HabitListEmptyState.COMPLETED, HabitListEmptyState.classify(listOf(habit), HabitMatcher(isCompletedAllowed = false)))
    }
}
