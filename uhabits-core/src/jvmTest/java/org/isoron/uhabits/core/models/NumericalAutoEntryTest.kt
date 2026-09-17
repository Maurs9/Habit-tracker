package org.isoron.uhabits.core.models

import org.isoron.platform.time.DayOfWeek
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.views.HistoryChart
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumericalAutoEntryTest : BaseUnitTest() {
    private val today get() = DateUtils.getTodayWithOffset()

    @Test
    fun tinyMeasurementsDoNotCompleteLargerTargetsOrSuppressReminders() {
        for (frequency in listOf(Frequency.DAILY, Frequency(1, 3))) {
            val habit = numericalHabit(frequency)
            habit.originalEntries.add(Entry(today, 1, "Measured"))
            habit.recompute()
            assertEquals(Entry(today, 1, "Measured"), habit.computedEntries.get(today))
            assertFalse(habit.isCompletedToday())
            assertFalse(habit.isReminderSuppressedToday())
            assertTrue(habit.isEnteredToday())
            assertTrue(habit.streaks.getBest(1).isEmpty())
            assertTrue(habit.scores[today].value > 0.0)
        }
    }

    @Test
    fun tinyMeasurementsReceiveTheirActualDailyScore() {
        val habit = numericalHabit(Frequency.DAILY).apply { targetValue = 0.002 }
        habit.originalEntries.add(Entry(today, 1))
        habit.recompute()
        assertEquals(Score.compute(1.0, 0.0, 0.5), habit.scores[today].value, 1e-9)

        habit.targetValue = 0.001
        habit.recompute()
        assertTrue(habit.isCompletedToday())
        assertEquals(Score.compute(1.0, 0.0, 1.0), habit.scores[today].value, 1e-9)
        assertEquals(1, habit.streaks.getBest(1).single().length)
    }

    @Test
    fun tinyAtMostMeasurementsAreNotAutomaticRestDays() {
        val habit = numericalHabit(Frequency.DAILY).apply {
            targetType = NumericalHabitType.AT_MOST
            targetValue = 0.0
        }
        habit.originalEntries.add(Entry(today, 1))
        habit.recompute()
        assertFalse(habit.isCompletedToday())
        assertTrue(habit.streaks.getBest(1).isEmpty())
        assertEquals(Score.compute(1.0, 1.0, 0.0), habit.scores[today].value, 1e-9)

        habit.targetValue = 5.0
        habit.recompute()
        assertFalse(habit.isCompletedToday())
        assertFalse(habit.isReminderSuppressedToday())
        assertEquals(1.0, habit.scores[today].value, 1e-9)
    }

    @Test
    fun automaticDaysRemainDerivedAndDoNotOverwriteTinyMeasurements() {
        val habit = numericalHabit(Frequency(1, 3))
        habit.originalEntries.add(Entry(today.minus(2), 5000))
        habit.originalEntries.add(Entry(today.minus(1), 1, "Measured"))
        habit.recompute()
        assertEquals(Entry.UNKNOWN, habit.originalEntries.get(today).value)
        assertEquals(Entry.NUMERICAL_AUTO, habit.computedEntries.get(today).value)
        assertEquals(Entry(today.minus(1), 1, "Measured"), habit.computedEntries.get(today.minus(1)))
        assertTrue(habit.isCompletedToday())
        assertTrue(habit.isReminderSuppressedToday())
        val snapshot = habit.computedEntries.getKnown()
        habit.recompute()
        assertEquals(snapshot, habit.computedEntries.getKnown())

        habit.frequency = Frequency.DAILY
        habit.recompute()
        assertEquals(Entry.UNKNOWN, habit.computedEntries.get(today).value)
        assertFalse(habit.isCompletedToday())
    }

    @Test
    fun historyAndTotalsDistinguishMeasurementsFromAutomaticDays() {
        val habit = numericalHabit(Frequency(1, 3))
        habit.originalEntries.add(Entry(today.minus(2), 5000))
        habit.originalEntries.add(Entry(today.minus(1), 1))
        habit.recompute()
        val state = HistoryCardPresenter.buildState(habit, DayOfWeek.SUNDAY, LightTheme())
        assertEquals(
            listOf(HistoryChart.Square.DIMMED, HistoryChart.Square.GREY, HistoryChart.Square.ON),
            state.series
        )
        val totals = habit.computedEntries.getKnown().groupedSum(
            DateUtils.TruncateField.YEAR,
            isNumerical = true
        )
        assertEquals(5001L, totals.sumOf { it.value })
    }

    @Test
    fun skipsCannotCreateNumericalRestDaysForTinyTargets() {
        for (type in NumericalHabitType.entries) {
            val habit = numericalHabit(Frequency(1, 3)).apply {
                targetType = type
                targetValue = 0.001
            }
            habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
            habit.recompute()
            assertEquals(Entry.UNKNOWN, habit.computedEntries.get(today).value)
            assertFalse(habit.isCompleted(Entry.SKIP))
            assertTrue(habit.streaks.getBest(1).isEmpty())
        }
    }

    private fun numericalHabit(frequency: Frequency) = modelFactory.buildHabit().apply {
        type = HabitType.NUMERICAL
        targetValue = 5.0
        this.frequency = frequency
    }
}
