package org.isoron.uhabits.activities.habits.edit

import android.content.Intent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorDiscardChangesTest : BaseAndroidTest() {
    @Test
    fun unchangedNewAndExistingEditorsExitImmediatelyThroughUpAndBack() {
        val habit = fixtures.createEmptyHabit().apply { name = "Read" }
        for (habitId in listOf(null, habit.id)) {
            for (systemBack in listOf(false, true)) {
                launchEditor(habitId).use { scenario ->
                    scenario.onActivity { activity ->
                        activity.findViewById<View>(R.id.moreToggle).performClick()
                        exit(activity, systemBack)
                        assertTrue(activity.isFinishing)
                        assertNull(activity.supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG))
                    }
                }
            }
        }
    }

    @Test
    fun bothExitPathsOfferKeepEditingAndDiscardWithoutSaving() {
        val habit = fixtures.createEmptyHabit().apply { name = "Read" }
        for (systemBack in listOf(false, true)) {
            launchEditor(habit.id).use { scenario ->
                scenario.onActivity { activity ->
                    val name = activity.findViewById<EditText>(R.id.nameInput)
                    name.setText("Unsaved draft")
                    exit(activity, systemBack)
                    assertFalse(activity.isFinishing)
                    val dialog = discardDialog(activity)
                    assertEquals(activity.getString(R.string.editor_keep_editing), dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                    activity.supportFragmentManager.executePendingTransactions()
                    assertEquals("Unsaved draft", name.text.toString())
                    assertEquals("Read", habit.name)
                    exit(activity, systemBack)
                    discardDialog(activity).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    assertTrue(activity.isFinishing)
                    assertEquals("Read", habit.name)
                }
            }
        }
    }

    @Test
    fun confirmationAndOriginalBaselineSurviveRecreationAndRevertingClearsDirtyState() {
        val habit = fixtures.createEmptyHabit().apply { name = "Read" }
        launchEditor(habit.id).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText("Unsaved draft")
                activity.onSupportNavigateUp()
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val dialog = discardDialog(activity)
                assertTrue(dialog.isShowing)
                assertEquals("Unsaved draft", activity.findViewById<EditText>(R.id.nameInput).text.toString())
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                activity.findViewById<EditText>(R.id.nameInput).setText(" Read ")
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue(activity.isFinishing)
                assertNull(activity.supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG))
            }
        }
    }

    @Test
    fun selectionOnlyChangesRemainDirtyAfterRecreation() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity -> activity.color = PaletteColor(42) }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(PaletteColor(42), activity.color)
                activity.onBackPressedDispatcher.onBackPressed()
                assertFalse(activity.isFinishing)
                discardDialog(activity).cancel()
                activity.supportFragmentManager.executePendingTransactions()
                assertEquals(PaletteColor(42), activity.color)
                assertFalse(activity.isFinishing)
            }
        }
    }

    @Test
    fun saveExitsWithoutDiscardPrompt() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText("Saved habit")
                activity.findViewById<View>(R.id.buttonSave).performClick()
                assertTrue(activity.isFinishing)
                assertNull(activity.supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG))
            }
        }
    }

    private fun exit(activity: EditHabitActivity, systemBack: Boolean) {
        if (systemBack) activity.onBackPressedDispatcher.onBackPressed() else activity.onSupportNavigateUp()
    }

    private fun discardDialog(activity: EditHabitActivity) =
        (activity.supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG) as DiscardHabitChangesDialog)
            .requireDialog() as AlertDialog

    private fun launchEditor(habitId: Long? = null): ActivityScenario<EditHabitActivity> {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
        if (habitId != null) intent.putExtra("habitId", habitId)
        return ActivityScenario.launch(intent)
    }
}
