package org.isoron.uhabits.core.ui.screens.habits.show

import org.isoron.platform.time.DayOfWeek
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.FrequencyCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartDataStateTest : BaseUnitTest() {
    @Test
    fun historyRetainsExactNumericalEntriesNotesAndUnits() {
        val habit = fixtures.createNumericalHabit().apply { unit = "km" }
        val today = DateUtils.getTodayWithOffset()
        habit.originalEntries.add(Entry(today, 1234, "Actual measurement"))
        habit.recompute()
        val state = HistoryCardPresenter.buildState(habit, DayOfWeek.MONDAY, LightTheme())
        assertTrue(state.isNumerical)
        assertEquals("km", state.unit)
        assertEquals(1234, state.entries.first { it.timestamp == today }.value)
        assertEquals("Actual measurement", state.entries.first { it.timestamp == today }.notes)
        assertEquals(state.series.size, state.entries.size)
    }

    @Test
    fun aggregateStatesRetainUnitsForAccessibleValues() {
        val habit = fixtures.createNumericalHabit().apply { unit = "km" }
        assertEquals("km", BarCardPresenter.buildState(habit, 1, 0, 0, LightTheme()).unit)
        assertEquals("km", FrequencyCardPresenter.buildState(habit, 1, LightTheme()).unit)
    }
}
