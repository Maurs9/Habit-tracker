package org.isoron.uhabits.core.ui.screens.habits.show

import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.ui.screens.habits.show.views.TargetCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.TargetCardState
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TargetCardStateTest : BaseUnitTest() {
    @Test
    fun weeklyNumeratorScalesEveryPeriodIncludingPartialWeeks() {
        setToday(2026, 9, 17)
        val state = state(habit(Frequency(3, 7)))
        assertEquals(listOf(7, 30, 91, 365), state.intervals)
        assertTarget(state, 7, 30.0)
        assertTarget(state, 30, 30.0 / 7 * 30)
        assertTarget(state, 91, 30.0 / 7 * 92)
        assertTarget(state, 365, 30.0 / 7 * 365)
    }

    @Test
    fun quarterAndYearLengthsFollowGregorianCalendarIncludingLeapYears() {
        for ((year, month, quarterDays, yearDays) in listOf(
            listOf(2026, 1, 90, 365),
            listOf(2024, 2, 91, 366),
            listOf(2026, 4, 91, 365),
            listOf(2026, 7, 92, 365),
            listOf(2026, 10, 92, 365),
            listOf(2100, 2, 90, 365)
        )) {
            setToday(year, month, 15)
            val state = state(habit(Frequency.DAILY))
            assertTarget(state, 1, 10.0)
            assertTarget(state, 7, 70.0)
            assertTarget(state, 91, quarterDays * 10.0)
            assertTarget(state, 365, yearDays * 10.0)
        }
    }

    @Test
    fun skippedDaysReduceOnlyTheirCurrentPeriodsWithTheNumeratorIncluded() {
        val today = setToday(2026, 9, 14)
        val habit = habit(Frequency(3, 7))
        habit.originalEntries.add(Entry(today, Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.recompute()
        val state = state(habit)
        assertTarget(state, 7, 30.0 - 30.0 / 7)
        assertTarget(state, 30, (30 - 2) * 30.0 / 7)
        assertTarget(state, 91, (92 - 2) * 30.0 / 7)
        assertTrue(state.values.all { it == 0.0 })
    }

    @Test
    fun monthlyTargetsUseCalendarMonthsAndSkippedDatesActualMonthLengths() {
        val today = setToday(2024, 3, 15)
        for (denominator in listOf(30, 31)) {
            val habit = habit(Frequency(3, denominator))
            habit.originalEntries.add(Entry(Timestamp.fromLocalDate(LocalDate(2024, 2, 29)), Entry.SKIP))
            habit.originalEntries.add(Entry(today.minus(14), Entry.SKIP))
            habit.recompute()
            val state = state(habit)
            assertEquals(listOf(30, 91, 365), state.intervals)
            assertTarget(state, 30, 30.0 - 30.0 / 31)
            assertTarget(state, 91, 90.0 - 30.0 / 29 - 30.0 / 31)
            assertTarget(state, 365, 360.0 - 30.0 / 29 - 30.0 / 31)
        }
    }

    @Test
    fun fullySkippedLeapMonthHasZeroRemainingTarget() {
        val today = setToday(2024, 2, 29)
        val habit = habit(Frequency(3, 30))
        for (offset in 0 until 29) habit.originalEntries.add(Entry(today.minus(offset), Entry.SKIP))
        habit.recompute()
        val state = state(habit)
        assertTarget(state, 30, 0.0)
        assertTarget(state, 91, 60.0)
        assertTrue(state.targets.all { it >= 0.0 })
    }

    @Test
    fun weeklyValuesRespectFirstWeekdayAndIncludeLastYearAtNewYear() {
        val today = setToday(2024, 1, 1)
        val habit = habit(Frequency.DAILY)
        habit.originalEntries.add(Entry(today.minus(1), 10000))
        habit.recompute()
        assertEquals(0.0, value(state(habit, Calendar.MONDAY), 7))
        assertEquals(10.0, value(state(habit, Calendar.SUNDAY), 7))
        assertEquals(0.0, value(state(habit, Calendar.SUNDAY), 365))
    }

    private fun habit(frequency: Frequency): Habit = fixtures.createEmptyHabit().apply {
        type = HabitType.NUMERICAL
        targetValue = 10.0
        this.frequency = frequency
    }

    private fun state(habit: Habit, firstWeekday: Int = Calendar.MONDAY) =
        TargetCardPresenter.buildState(habit, firstWeekday, LightTheme())

    private fun setToday(year: Int, month: Int, day: Int): Timestamp =
        Timestamp.fromLocalDate(LocalDate(year, month, day)).also { DateUtils.setFixedLocalTime(it.unixTime) }

    private fun assertTarget(state: TargetCardState, interval: Int, expected: Double) {
        assertEquals(expected, state.targets[state.intervals.indexOf(interval)], 1e-8)
    }

    private fun value(state: TargetCardState, interval: Int): Double =
        state.values[state.intervals.indexOf(interval)]
}
