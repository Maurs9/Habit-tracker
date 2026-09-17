package org.isoron.uhabits.widgets

import android.view.View
import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.chartDate
import org.isoron.uhabits.activities.common.views.chartPercent
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetAccessibilityTest : BaseViewTest() {
    @Test
    fun testEveryWidgetPublishesItsHabitActionAndDataOnTheRemoteButton() {
        setTheme(R.style.WidgetTheme)
        val habit = fixtures.createLongHabit()
        val numerical = fixtures.createLongNumericalHabit()
        val cases = listOf(
            CheckmarkWidget(targetContext, 0, habit) to (habit.name to R.string.chart_widget_toggle),
            HistoryWidget(targetContext, 0, habit) to (habit.name to R.string.calendar),
            ScoreWidget(targetContext, 0, habit) to (habit.name to R.string.score),
            FrequencyWidget(targetContext, 0, habit, prefs.firstWeekdayInt) to (habit.name to R.string.frequency),
            StreakWidget(targetContext, 0, habit) to (habit.name to R.string.best_streaks),
            TargetWidget(targetContext, 0, numerical) to (numerical.name to R.string.target)
        )
        for ((widget, expected) in cases) {
            val view = convertToView(widget, 400, 400)
            val button = view.findViewById<Button>(R.id.button)
            val description = button.createAccessibilityNodeInfo().contentDescription.toString()
            assertTrue(description, description.contains(expected.first))
            assertTrue(description, description.contains(targetContext.getString(expected.second)))
            assertFalse(description, description.contains(targetContext.getString(R.string.chart_no_data)))
            assertTrue(button.isClickable)
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.findViewById<View>(R.id.imageView).importantForAccessibility)
            if (widget !is CheckmarkWidget) {
                assertTrue(description.contains(targetContext.getString(R.string.chart_widget_open)))
            }
        }
    }

    @Test
    fun testStackItemsKeepDistinctNamesAndMixedStackEditorAction() {
        setTheme(R.style.WidgetTheme)
        val first = fixtures.createLongHabit().apply { name = "First habit" }
        val second = fixtures.createLongNumericalHabit().apply { name = "Second habit" }
        for (habit in listOf(first, second)) {
            val widget = CheckmarkWidget(targetContext, 0, habit, stacked = true).apply {
                opensEntryEditor = true
            }
            val view = convertToView(widget, 200, 200)
            val description = view.findViewById<Button>(R.id.button).contentDescription.toString()
            assertTrue(description.contains(habit.name))
            assertTrue(description.contains(targetContext.getString(R.string.chart_widget_edit)))
            assertFalse(description.contains(targetContext.getString(R.string.chart_widget_toggle)))
        }
    }

    @Test
    fun testCheckmarkStateAndStatisticsRefreshWithTheBitmap() {
        setTheme(R.style.WidgetTheme)
        val habit = fixtures.createLongHabit()
        val today = DateUtils.getTodayWithOffset()
        val widget = CheckmarkWidget(targetContext, 0, habit)
        habit.originalEntries.add(Entry(today, Entry.NO))
        habit.recompute()
        val before = convertToView(widget, 200, 200).findViewById<Button>(R.id.button).contentDescription.toString()
        assertTrue(before.contains(targetContext.getString(R.string.chart_not_completed)))
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL, "From accessibility"))
        habit.recompute()
        val after = convertToView(widget, 200, 200).findViewById<Button>(R.id.button).contentDescription.toString()
        assertTrue(after.contains(targetContext.getString(R.string.chart_completed)))
        assertTrue(after.contains(targetContext.chartDate(today)))
        assertTrue(after.contains("From accessibility"))
        assertTrue(after.contains(targetContext.chartPercent(habit.scores[today].value)))
        assertFalse(before == after)
    }
}
