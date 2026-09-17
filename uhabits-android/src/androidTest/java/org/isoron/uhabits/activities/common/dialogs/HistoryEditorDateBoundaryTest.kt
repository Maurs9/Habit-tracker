package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.activities.common.views.chartDate
import org.isoron.uhabits.activities.common.views.chartSummary
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.ui.views.HistoryChart
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryEditorDateBoundaryTest : BaseAndroidTest() {
    @After
    fun restoreClock() {
        DateUtils.setFixedLocalTime(null)
    }

    @Test
    fun resumeUpdatesCalendarAnchorWithoutMovingStoredEntries() {
        val habit = habitList.getByPosition(0)
        val originalDay = DateUtils.getTodayWithOffset()
        habit.originalEntries.add(Entry(originalDay, Entry.YES_MANUAL, "Completed"))
        habit.recompute()
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                HistoryEditorDialog().apply {
                    arguments = Bundle().apply { putLong("habit", habit.id!!) }
                }.showNow(activity.supportFragmentManager, "historyEditor")
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            DateUtils.setFixedLocalTime(originalDay.plus(1).unixTime)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                val editor = activity.supportFragmentManager.findFragmentByTag("historyEditor") as HistoryEditorDialog
                val chart = editor.dataView.view as HistoryChart
                assertEquals(originalDay.plus(1).toLocalDate(), chart.today)
                assertEquals(listOf(HistoryChart.Square.OFF, HistoryChart.Square.ON), chart.series.take(2))
                assertTrue(editor.dataView.chartSummary().contains(activity.chartDate(originalDay.plus(1))))
                assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(originalDay).value)
                assertEquals(Entry.UNKNOWN, habit.originalEntries.get(originalDay.plus(1)).value)
            }
        }
    }

    @Test
    fun resumeAfterClockMovesBackClampsRetainedCalendarOffset() {
        val habit = habitList.getByPosition(0)
        val originalDay = DateUtils.getTodayWithOffset()
        var originalMaximum = 0
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val editor = HistoryEditorDialog().apply {
                    arguments = Bundle().apply { putLong("habit", habit.id!!) }
                }
                editor.showNow(activity.supportFragmentManager, "historyEditor")
                originalMaximum = editor.dataView.maxDataOffset
                assertTrue(editor.dataView.scrollByColumns(editor.dataView.maxDataOffset))
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            DateUtils.setFixedLocalTime(originalDay.minus(14).unixTime)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                val editor = activity.supportFragmentManager.findFragmentByTag("historyEditor") as HistoryEditorDialog
                val currentDay = originalDay.minus(14)
                assertEquals(originalMaximum - 2, editor.dataView.maxDataOffset)
                assertEquals(editor.dataView.maxDataOffset, editor.dataView.dataOffset)
                assertEquals(currentDay.toLocalDate(), (editor.dataView.view as HistoryChart).today)
                assertTrue(editor.dataView.scrollByColumns(-1))
            }
        }
    }
}
