package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.isoron.uhabits.utils.dismissCurrentDialog
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryDialogDraftTest : BaseAndroidTest() {
    @Test
    fun numericalContextAndRawDraftSurvivePauseAndRotationAndSaveToOriginalDate() {
        val habit = habitList.getByPosition(0).apply {
            name = "Walk"
            type = HabitType.NUMERICAL
            unit = "km"
            targetType = NumericalHabitType.AT_MOST
            targetValue = 20.0
        }
        habitList.update(habit)
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habit, day(4), Color.BLUE).apply {
                        putDouble("value", 1.0)
                        putString("notes", "Original")
                    }
                    onToggle = { _, _ -> fail("A recreated dialog must not invoke its old activity callback") }
                }
                fragment.dismissCurrentAndShow(activity.supportFragmentManager, "draft")
                val dialog = fragment.requireDialog()
                assertTrue(dialog.findViewById<TextView>(R.id.entryContext).text.contains("Walk"))
                assertTrue(dialog.findViewById<TextView>(R.id.entryDetails).text.contains("km"))
                assertTrue(dialog.findViewById<TextView>(R.id.entryDetails).text.contains("Limit"))
                dialog.findViewById<EditText>(R.id.value).setText("12.500")
                dialog.findViewById<EditText>(R.id.notes).setText("First line\nSecond line")
                dismissCurrentDialog(preserveEntryDrafts = true)
                assertTrue(dialog.isShowing)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            scenario.onActivity { activity ->
                val restored = activity.supportFragmentManager.findFragmentByTag("draft") as NumberDialog
                val dialog = restored.requireDialog()
                assertEquals("12.500", dialog.findViewById<EditText>(R.id.value).text.toString())
                assertEquals("First line\nSecond line", dialog.findViewById<EditText>(R.id.notes).text.toString())
                assertEquals(day(4).unixTime, restored.requireArguments().getLong(EntryDialogFragment.DATE))
                restored.save()
                assertEquals(12500, habit.originalEntries.get(day(4)).value)
                assertEquals("First line\nSecond line", habit.originalEntries.get(day(4)).notes)
                assertEquals(Entry.UNKNOWN, habit.originalEntries.get(day(0)).value)
            }
        }
    }

    @Test
    fun booleanDraftRestoresSelectionAndNotesAndResolvesSubmissionWithoutCallback() {
        val habit = habitList.getByPosition(0).apply { name = "Read" }
        habitList.update(habit)
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = CheckmarkDialog().apply {
                    arguments = EntryDialogFragment.arguments(habit, day(2), Color.BLUE).apply {
                        putInt("value", Entry.NO)
                        putString("notes", "")
                    }
                }
                fragment.showNow(activity.supportFragmentManager, "draft")
                fragment.requireDialog().findViewById<EditText>(R.id.notes).setText("Keep this note")
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val restored = activity.supportFragmentManager.findFragmentByTag("draft") as CheckmarkDialog
                val dialog = restored.requireDialog()
                assertTrue(dialog.findViewById<View>(R.id.noBtn).isSelected)
                assertEquals("Keep this note", dialog.findViewById<EditText>(R.id.notes).text.toString())
                dialog.findViewById<View>(R.id.yesBtn).performClick()
                assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(day(2)).value)
                assertEquals("Keep this note", habit.originalEntries.get(day(2)).notes)
            }
        }
    }

    @Test
    fun missingHabitCannotSaveAndKeepsDraftForRecovery() {
        val habit = habitList.getByPosition(0)
        launch().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habit, day(1), Color.BLUE).apply {
                        putDouble("value", 7.0)
                        putString("notes", "Keep me")
                    }
                    onToggle = { _, _ -> fail("Deleted habit callback must not execute") }
                }
                fragment.showNow(activity.supportFragmentManager, "draft")
                habitList.remove(habit)
                fragment.save()
                val dialog = fragment.requireDialog()
                assertTrue(dialog.isShowing)
                assertEquals("Keep me", dialog.findViewById<EditText>(R.id.notes).text.toString())
                assertEquals(
                    activity.getString(R.string.entry_habit_unavailable),
                    dialog.findViewById<EditText>(R.id.value).error.toString()
                )
                fragment.dismiss()
            }
        }
    }

    private fun launch(): ActivityScenario<EditHabitActivity> =
        ActivityScenario.launch(Intent(targetContext, EditHabitActivity::class.java))
}
