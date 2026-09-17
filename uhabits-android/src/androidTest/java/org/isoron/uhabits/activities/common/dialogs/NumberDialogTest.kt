package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class NumberDialogTest : BaseAndroidTest() {
    @Test
    fun testSavingAThousandthPreservesItsMeasurementAndNotes() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                var result: Pair<Int, String>? = null
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habitList.getByPosition(0), day(0), android.graphics.Color.BLUE).apply {
                        putDouble("value", 0.001)
                        putString("notes", "Measured")
                    }
                    onToggle = { value, notes -> result = (value * 1000).roundToInt() to notes }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                val dialog = fragment.requireDialog()
                assertEquals("0.001", dialog.findViewById<EditText>(R.id.value).text.toString())
                fragment.save()
                assertEquals(1 to "Measured", result)
                assertFalse(dialog.isShowing)
            }
        }
    }

    @Test
    fun testThreeThousandthsSurvivesRecreationAndIsNotASkip() {
        val habit = habitList.getByPosition(0).apply {
            type = HabitType.NUMERICAL
            targetValue = 0.003
        }
        habitList.update(habit)
        prefs.isSkipEnabled = false
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habit, day(2), android.graphics.Color.BLUE).apply {
                        putDouble("value", 0.003)
                        putString("notes", "A measurement, not a skip")
                    }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                assertEquals(View.GONE, fragment.requireDialog().findViewById<View>(R.id.skipBtnNumber).visibility)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("number") as NumberDialog
                assertEquals("0.003", fragment.requireDialog().findViewById<EditText>(R.id.value).text.toString())
                fragment.save()
                val entry = habit.originalEntries.get(day(2))
                assertEquals(3, entry.value)
                assertTrue(entry.value != Entry.SKIP)
                assertEquals("A measurement, not a skip", entry.notes)
                assertEquals(Entry.UNKNOWN, habit.originalEntries.get(day(0)).value)
            }
        }
    }

    @Test
    fun testSkipAndClearUseSentinelsAndRetainNotes() {
        val habit = habitList.getByPosition(0).apply { type = HabitType.NUMERICAL }
        habitList.update(habit)
        prefs.isSkipEnabled = true
        prefs.areQuestionMarksEnabled = true
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                for ((button, expected) in listOf(R.id.skipBtnNumber to Entry.SKIP, R.id.unknownBtnNumber to Entry.UNKNOWN)) {
                    val fragment = NumberDialog().apply {
                        arguments = EntryDialogFragment.arguments(habit, day(1), android.graphics.Color.BLUE).apply {
                            putDouble("value", 0.003)
                            putString("notes", "Retained note")
                        }
                    }
                    fragment.showNow(activity.supportFragmentManager, "number")
                    fragment.requireDialog().findViewById<View>(button).performClick()
                    assertEquals(expected, habit.originalEntries.get(day(1)).value)
                    assertEquals("Retained note", habit.originalEntries.get(day(1)).notes)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    @Test
    fun testNegativeSentinelInputAndOverflowAreRejectedButMaximumMeasurementSaves() {
        val habit = habitList.getByPosition(0).apply { type = HabitType.NUMERICAL }
        habitList.update(habit)
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habit, day(1), android.graphics.Color.BLUE).apply {
                        putDouble("value", 0.0)
                        putString("notes", "Keep this draft")
                    }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                val dialog = fragment.requireDialog()
                val input = dialog.findViewById<EditText>(R.id.value)
                input.keyListener = null
                for (text in listOf((Entry.SKIP / 1000.0).toString(), "2147483.648", "NaN", "1.2.3")) {
                    input.setText(text)
                    fragment.save()
                    assertTrue(dialog.isShowing)
                    assertNotNull(input.error)
                    assertEquals(Entry.UNKNOWN, habit.originalEntries.get(day(1)).value)
                }
                input.setText("2147483.647")
                fragment.save()
                assertEquals(Int.MAX_VALUE, habit.originalEntries.get(day(1)).value)
                assertEquals("Keep this draft", habit.originalEntries.get(day(1)).notes)
            }
        }
    }
}
