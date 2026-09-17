package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Canvas
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartBoundaryTest : BaseUnitTest() {
    private val theme = LightTheme()
    private val formatter = JavaLocalDateFormatter(Locale.US)

    @Test
    fun barRendersEmptyAndMixedSeriesWithoutDroppingNonemptyValues() {
        for (series in listOf(emptyList(), listOf(emptyList()), listOf(emptyList(), listOf(5.0)))) {
            val canvas = canvas()
            BarChart(theme, formatter).apply {
                this.series = series.toMutableList()
                colors = series.map { theme.color(0) }.toMutableList()
                axis = listOf(LocalDate(2026, 9, 17))
            }.draw(canvas)
            verify(canvas).fill()
            if (series.any { it.isNotEmpty() }) verify(canvas).drawText(eq("5"), any(), any())
        }
    }

    @Test
    fun barRendersHabitWithOnlyFutureEntries() {
        val habit = fixtures.createEmptyHabit().apply { type = HabitType.NUMERICAL }
        habit.originalEntries.add(Entry(DateUtils.getTodayWithOffset().plus(1), 10000))
        habit.recompute()
        val state = BarCardPresenter.buildState(habit, 1, 0, 0, theme)
        assertTrue(state.entries.isEmpty())
        val canvas = canvas()
        BarChart(theme, formatter).apply {
            series = mutableListOf(state.entries.map { it.value / 1000.0 })
            colors = mutableListOf(theme.color(0))
            axis = state.entries.map { it.timestamp.toLocalDate() }
        }.draw(canvas)
        verify(canvas).fill()
    }

    @Test
    fun historyAllowsEpochAndTodayButRejectsEarlierAndFutureDates() {
        val first = Timestamp.ZERO.toLocalDate()
        val today = first.plus(3)
        for (weekday in DayOfWeek.values()) {
            val shortClicks = mutableSetOf<LocalDate>()
            val longClicks = mutableSetOf<LocalDate>()
            val chart = HistoryChart(
                formatter, weekday, PaletteColor(0), emptyList(), HistoryChart.Square.OFF,
                emptyList(), theme, today,
                object : OnDateClickedListener {
                    override fun onDateShortPress(date: LocalDate) { shortClicks.add(date) }
                    override fun onDateLongPress(date: LocalDate) { longClicks.add(date) }
                }
            )
            chart.draw(canvas())
            for (x in 12 until 400 step 25) {
                for (y in 12 until 200 step 25) {
                    chart.onClick(x.toDouble(), y.toDouble())
                    chart.onLongClick(x.toDouble(), y.toDouble())
                }
            }
            assertEquals((0..3).map { first.plus(it) }.toSet(), shortClicks)
            assertEquals(shortClicks, longClicks)
            shortClicks.forEach { Timestamp.fromLocalDate(it) }
            chart.onClick(-25.0, 50.0)
            chart.onClick(50.0, -25.0)
            assertEquals(4, shortClicks.size)
        }
    }

    @Test
    fun historyRejectsDatesBeforeEpochAtMaximumEditorOffset() {
        val today = LocalDate(2026, 9, 17)
        val clicked = mutableListOf<LocalDate>()
        val chart = HistoryChart(
            formatter, DayOfWeek.MONDAY, PaletteColor(0), emptyList(), HistoryChart.Square.OFF,
            emptyList(), theme, today,
            object : OnDateClickedListener {
                override fun onDateShortPress(date: LocalDate) { clicked.add(date) }
            }
        )
        chart.dataOffset = chart.maximumDataOffset
        chart.draw(canvas())
        chart.onClick(12.0, 37.0)
        assertTrue(clicked.isEmpty())
    }

    @Test
    fun epochRemainsReachableInASingleColumnForEveryWeekdayAlignment() {
        val first = Timestamp.ZERO.toLocalDate()
        for (weekday in DayOfWeek.values()) {
            for (daysSinceEpoch in 0..13) {
                val clicked = mutableSetOf<LocalDate>()
                val chart = HistoryChart(
                    formatter, weekday, PaletteColor(0), emptyList(), HistoryChart.Square.OFF,
                    emptyList(), theme, first.plus(daysSinceEpoch),
                    object : OnDateClickedListener {
                        override fun onDateShortPress(date: LocalDate) { clicked.add(date) }
                    }
                )
                chart.dataOffset = chart.maximumDataOffset
                chart.draw(canvas(width = 50.0))
                for (y in 37 until 200 step 25) chart.onClick(12.0, y.toDouble())
                assertTrue(first in clicked, "$weekday, $daysSinceEpoch days since epoch")
                assertTrue(clicked.none { it.isOlderThan(first) })
            }
        }
    }

    private fun canvas(width: Double = 400.0): Canvas = mock<Canvas>().apply {
        whenever(getWidth()).thenReturn(width)
        whenever(getHeight()).thenReturn(200.0)
        whenever(getScaledFontSize(any())).thenAnswer { it.getArgument<Double>(0) }
    }
}
