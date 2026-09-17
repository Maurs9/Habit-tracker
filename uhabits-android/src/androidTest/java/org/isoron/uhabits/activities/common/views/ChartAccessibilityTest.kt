package org.isoron.uhabits.activities.common.views

import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.platform.gui.AndroidDataView
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.HistoryEditorDialog
import org.isoron.uhabits.activities.habits.show.ShowHabitActivity
import org.isoron.uhabits.activities.habits.show.views.BarCardView
import org.isoron.uhabits.activities.habits.show.views.HistoryCardView
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Score
import org.isoron.uhabits.core.models.Streak
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.ui.views.OnDateClickedListener
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.intents.IntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class ChartAccessibilityTest : BaseAndroidTest() {
    private fun controller(view: View) =
        view.getTag(R.id.chart_accessibility_controller) as ChartAccessibility

    @Test
    fun testSharedScrollHooksRespectPageSizeReversalAndRemainingRange() {
        val chart = object : ScrollableChart(targetContext) {
            override val accessibilityScrollStep: Int get() = 3
            override fun accessibilityScrollLabel(delta: Int): CharSequence = "Move $delta dates"
        }
        chart.setScrollerBucketSize(20)
        chart.setMaxDataOffset(5)
        chart.setScrollDirection(-1)
        assertEquals(5, chart.maximumDataOffset)
        assertEquals(-1, chart.scrollDirection)
        assertTrue(chart.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)))
        assertEquals(3, chart.dataOffset)
        val forward = chart.createAccessibilityNodeInfo().actionList.first {
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        assertEquals("Move 2 dates", forward.label)
        assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertEquals(5, chart.dataOffset)
        assertFalse(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
        assertEquals(2, chart.dataOffset)
    }

    @Test
    fun testScoreActionsOpenEveryHistoricalValueAndKeyboardChangesDate() {
        val habit = fixtures.createLongHabit()
        ActivityScenario.launch<ShowHabitActivity>(IntentFactory().startShowHabitActivity(targetContext, habit)).use { scenario ->
            scenario.onActivity { activity ->
                val chart = activity.findViewById<ScoreChart>(R.id.scoreView)
                val today = DateUtils.getTodayWithOffset()
                chart.setBucketSize(1)
                chart.setScores((0 until 70).map { Score(today.minus(it), it / 100.0) })
                chart.setScrollerBucketSize(20)
                val node = chart.createAccessibilityNodeInfo()
                assertTrue(node.isClickable)
                assertTrue(node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
                assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
                assertEquals(1, chart.dataOffset)
                assertTrue(chart.contentDescription.toString().contains(activity.chartDate(today.minus(1))))
                assertTrue(chart.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)))
                assertEquals(0, chart.dataOffset)
                assertFalse(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))

                chart.isFocusableInTouchMode = true
                assertTrue(chart.requestFocus())
                assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
                val dialog = controller(chart).dialog!!
                val list = dialog.findViewById<ListView>(R.id.chart_data_list)!!
                assertEquals(31, list.adapter.count)
                assertTrue(list.adapter.isEnabled(0))
                assertTrue(list.adapter.getItem(0).toString().contains(activity.chartDate(today)))
                assertFalse(dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertTrue(list.adapter.getItem(0).toString().contains(activity.chartDate(today.minus(31))))
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(8, list.adapter.count)
                assertTrue(list.adapter.getItem(7).toString().contains(activity.chartPercent(0.69)))
                assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
                dialog.dismiss()
                chart.setScores(emptyList())
                assertFalse(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
                assertFalse(chart.createAccessibilityNodeInfo().isScrollable)
            }
            scenario.onActivity { activity ->
                val chart = activity.findViewById<ScoreChart>(R.id.scoreView)
                assertNull(controller(chart).dialog)
                assertTrue(chart.hasFocus())
            }
        }
    }

    @Test
    fun testCalendarEditorAlternativeSelectsPastDatesAndReadsUpdatedNotes() {
        val habit = fixtures.createLongHabit()
        prefs.isShortToggleEnabled = true
        ActivityScenario.launch<ShowHabitActivity>(IntentFactory().startShowHabitActivity(targetContext, habit)).use { scenario ->
            scenario.onActivity { activity ->
                var selected: LocalDate? = null
                var shortPresses = 0
                val editor = HistoryEditorDialog().apply {
                    arguments = Bundle().apply { putLong("habit", habit.id!!) }
                    setOnDateClickedListener(object : OnDateClickedListener {
                        override fun onDateShortPress(date: LocalDate) { shortPresses++ }
                        override fun onDateLongPress(date: LocalDate) {
                            selected = date
                            habit.originalEntries.add(Entry(Timestamp.fromLocalDate(date), Entry.YES_MANUAL, "Changed through editor"))
                            habit.recompute()
                        }
                    })
                }
                editor.showNow(activity.supportFragmentManager, "accessibleHistoryTest")
                val chart: AndroidDataView = editor.dataView
                assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
                assertEquals(1, chart.dataOffset)
                assertTrue(chart.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)))
                assertTrue(chart.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
                val dialog = controller(chart).dialog!!
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                val list = dialog.findViewById<ListView>(R.id.chart_data_list)!!
                list.performItemClick(null, 0, list.adapter.getItemId(0))
                val timestamp = DateUtils.getTodayWithOffset().minus(31)
                assertEquals(timestamp.toLocalDate(), selected)
                assertEquals(0, shortPresses)
                chart.refreshChartAccessibility()
                assertTrue(list.adapter.getItem(0).toString().contains("Changed through editor"))
                dialog.dismiss()
                editor.dismissNow()
            }
        }
    }

    @Test
    fun testTargetFrequencyAndStreakDataIncludesExactValues() {
        val target = TargetChart(targetContext).apply {
            accessibilityUnit = "km"
            setLabels(listOf("Today", "Week"))
            setValues(listOf(1.234, 5.0))
            setTargets(listOf(4.0, 20.0))
        }
        assertEquals(2, controller(target).data.size())
        assertTrue(controller(target).data.row(0).contains("1.234 km"))
        assertTrue(controller(target).data.row(1).contains("20 km"))
        val month = DateUtils.getTodayWithOffset().toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val frequency = FrequencyChart(targetContext).apply {
            accessibilityUnit = "km"
            setIsNumerical(true)
            setFirstWeekday(Calendar.MONDAY)
            setFrequency(hashMapOf(Timestamp(month) to arrayOf(0, 0, 1234, 0, 0, 0, 0)))
        }
        assertEquals(7, controller(frequency).data.size())
        assertTrue(controller(frequency).data.row(0).contains("Monday"))
        assertTrue(controller(frequency).data.row(0).contains("1.234 km"))
        val today = DateUtils.getTodayWithOffset()
        val streak = StreakChart(targetContext).apply {
            setStreaks(listOf(Streak(today.minus(2), today)))
        }
        assertTrue(controller(streak).data.row(0).contains(targetContext.chartDate(today.minus(2))))
        assertTrue(controller(streak).data.row(0).contains("3 days"))
        streak.setStreaks(emptyList())
        assertTrue(streak.contentDescription.toString().contains(targetContext.getString(R.string.chart_no_data)))
    }

    @Test
    fun testEntryStatesNumbersAndCalendarDatesAreNotConflated() {
        val timestamp = Timestamp(1577923200000)
        for (value in listOf(1, 2, 4)) {
            assertTrue(targetContext.chartEntry(Entry(timestamp, value), true, "km").contains("0.00$value km"))
        }
        assertTrue(
            targetContext.chartEntry(Entry(timestamp, Entry.NUMERICAL_AUTO), true, "km")
                .contains(targetContext.getString(R.string.chart_completed_automatically))
        )
        assertTrue(
            targetContext.chartEntry(Entry(timestamp, Entry.SKIP), false, "")
                .contains(targetContext.getString(R.string.chart_skipped))
        )
        assertTrue(
            targetContext.chartEntry(Entry(timestamp, Entry.SKIP), true, "km")
                .contains(targetContext.getString(R.string.chart_skipped))
        )
        val originalZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val context = targetContext.createConfigurationContext(
                Configuration(targetContext.resources.configuration).apply { setLocale(Locale.FRANCE) }
            )
            assertTrue(context.chartDate(timestamp).contains("2 janvier 2020"))
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    @Test
    fun testDetailHistoryAndBarExposeValuesBeyondTheVisibleChart() {
        val habit = fixtures.createLongNumericalHabit()
        val today = DateUtils.getTodayWithOffset()
        habit.unit = "km"
        habit.originalEntries.add(Entry(today.minus(90), 1234, "Older note"))
        habit.recompute()
        prefs.barCardNumericalSpinnerPosition = 0
        ActivityScenario.launch<ShowHabitActivity>(IntentFactory().startShowHabitActivity(targetContext, habit)).use { scenario ->
            scenario.onActivity { activity ->
                val history = activity.findViewById<HistoryCardView>(R.id.historyCard)
                    .findViewById<AndroidDataView>(R.id.chart)
                val calendarData = controller(history).data
                assertTrue(calendarData.size() > 90)
                assertTrue(calendarData.row(90).contains("1.234 km"))
                assertTrue(calendarData.row(90).contains("Older note"))
                val bar = activity.findViewById<BarCardView>(R.id.barCard)
                    .findViewById<AndroidDataView>(R.id.chart)
                val barData = controller(bar).data
                assertTrue(barData.size() > 90)
                assertTrue(barData.row(90).contains("1.234 km"))
                assertTrue(barData.row(90).contains(activity.chartDate(today.minus(90))))
            }
        }
    }
}
