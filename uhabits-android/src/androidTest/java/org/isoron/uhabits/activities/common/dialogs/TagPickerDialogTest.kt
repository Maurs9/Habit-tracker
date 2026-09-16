package org.isoron.uhabits.activities.common.dialogs

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.checkbox.MaterialCheckBox
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.HabitOrganizationDialogs
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.activities.habits.show.ShowHabitActivity
import org.isoron.uhabits.intents.IntentFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TagPickerDialogTest : BaseAndroidTest() {
    @Test
    fun testDetailResultSurvivesRotationAndChangesOnlyItsHabit() {
        val habit = fixtures.createEmptyHabit().apply { tags = setOf("Health") }
        val other = fixtures.createEmptyHabit().apply { tags = setOf("Work") }
        ActivityScenario.launch<ShowHabitActivity>(IntentFactory().startShowHabitActivity(targetContext, habit)).use { scenario ->
            scenario.onActivity { activity ->
                activity.Screen().showTagsDialog()
                activity.supportFragmentManager.executePendingTransactions()
                val dialog = (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
                    .requireDialog() as AlertDialog
                add(dialog, "health")
                add(dialog, "  Evening ")
                add(dialog, "Unselected draft")
                val choices = choices(dialog)
                assertEquals(1, choices.count { it.text == "Health" })
                choices.single { it.text == "Health" }.isChecked = false
                choices.single { it.text == "Unselected draft" }.isChecked = false
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val dialog = (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
                    .requireDialog() as AlertDialog
                assertFalse(choices(dialog).single { it.text == "Health" }.isChecked)
                assertFalse(choices(dialog).single { it.text == "Unselected draft" }.isChecked)
                assertTrue(choices(dialog).single { it.text == "Evening" }.isChecked)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(setOf("Evening"), habit.tags)
            assertEquals(setOf("Work"), other.tags)
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("Evening", activity.findViewById<TextView>(R.id.tagsLabel).text.toString())
            }
        }
    }

    @Test
    fun testListResultRestoresListenerAndCancelDoesNotMutate() {
        val habit = fixtures.createEmptyHabit().apply { tags = setOf("Original") }
        ActivityScenario.launch(ListHabitsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                HabitOrganizationDialogs(
                    activity,
                    habitList,
                    prefs,
                    appComponent.commandRunner,
                    appComponent.taskRunner,
                    appComponent.sectionList
                ).editTags(habit)
                activity.supportFragmentManager.executePendingTransactions()
                val dialog = (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
                    .requireDialog() as AlertDialog
                add(dialog, "Canceled")
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                activity.supportFragmentManager.executePendingTransactions()
            }
            assertEquals(setOf("Original"), habit.tags)
            scenario.onActivity { activity ->
                HabitOrganizationDialogs(
                    activity,
                    habitList,
                    prefs,
                    appComponent.commandRunner,
                    appComponent.taskRunner,
                    appComponent.sectionList
                ).editTags(habit)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val dialog = (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
                    .requireDialog() as AlertDialog
                choices(dialog).forEach { it.isChecked = false }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            assertTrue(habit.tags.isEmpty())
        }
    }

    @Test
    fun testEmptyValidationAndLongLocalizedNames() {
        val habit = fixtures.createEmptyHabit()
        ActivityScenario.launch<ShowHabitActivity>(IntentFactory().startShowHabitActivity(targetContext, habit)).use { scenario ->
            scenario.onActivity { activity ->
                activity.Screen().showTagsDialog()
                activity.supportFragmentManager.executePendingTransactions()
                val dialog = (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
                    .requireDialog() as AlertDialog
                assertEquals(View.VISIBLE, dialog.findViewById<View>(R.id.emptyTags)!!.visibility)
                val input = dialog.findViewById<EditText>(R.id.newTagInput)!!
                add(dialog, "   ")
                assertEquals(activity.getString(R.string.validation_cannot_be_blank), input.error.toString())
                assertTrue(dialog.isShowing)
                input.keyListener = null
                add(dialog, "First\nSecond")
                assertEquals(activity.getString(R.string.tag_name_single_line), input.error.toString())
                val longName = "健康と集中力 ".repeat(20).trim()
                add(dialog, longName)
                assertNull(input.error)
                assertEquals(longName, choices(dialog).single().text.toString())
                assertTrue(choices(dialog).single().isChecked)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(setOf(longName), habit.tags)
            }
        }
    }

    private fun choices(dialog: AlertDialog) =
        dialog.findViewById<LinearLayout>(R.id.tagChoices)!!.children.filterIsInstance<MaterialCheckBox>().toList()

    private fun add(dialog: AlertDialog, name: String) {
        dialog.findViewById<EditText>(R.id.newTagInput)!!.setText(name)
        dialog.findViewById<View>(R.id.addTagButton)!!.performClick()
    }
}
