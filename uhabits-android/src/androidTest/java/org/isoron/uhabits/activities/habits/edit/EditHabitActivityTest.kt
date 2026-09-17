package org.isoron.uhabits.activities.habits.edit

import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.TagPickerDialog
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EditHabitActivityTest : BaseAndroidTest() {
    @Test
    fun testDirectCreateDefaultsToYesNoWithOptionalFieldsFolded() {
        saveHabit(null) { activity ->
            assertEquals(HabitType.YES_NO, activity.habitType)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.typeOuterBox).visibility)
            assertTrue(activity.findViewById<MaterialButton>(R.id.typeYesNo).isChecked)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.frequencyOuterBox).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.targetOuterBox).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.moreGroup).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tagsPicker).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.sectionPicker).visibility)
            activity.findViewById<EditText>(R.id.nameInput).setText("Quick habit")
        }
        val saved = habitList.first { it.name == "Quick habit" }
        assertEquals(HabitType.YES_NO, saved.type)
        assertEquals(Frequency.DAILY, saved.frequency)
        assertEquals("", saved.question)
        assertEquals("", saved.description)
        assertTrue(saved.tags.isEmpty())
        assertNull(saved.sectionId)
        assertNull(saved.reminder)
    }

    @Test
    fun testToolbarUpConfirmsBeforeDiscardingUnsavedChanges() {
        val existing = fixtures.createEmptyHabit().apply {
            name = "Unchanged habit"
            tags = setOf("Original")
        }
        val intent = Intent(targetContext, EditHabitActivity::class.java).putExtra("habitId", existing.id)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText("Canceled draft")
                val picker = openTags(activity)
                addTag(picker, "Canceled tag")
                picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertTrue(activity.onSupportNavigateUp())
                assertFalse(activity.isFinishing)
                val discard = activity.supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG)
                    as DiscardHabitChangesDialog
                (discard.requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertTrue(activity.isFinishing)
            }
        }
        assertEquals("Unchanged habit", existing.name)
        assertEquals(setOf("Original"), existing.tags)
    }

    @Test
    fun testTypeToggleValidatesAndSavesNumericalFields() {
        saveHabit(null) { activity ->
            activity.findViewById<EditText>(R.id.nameInput).setText("Read chapters")
            activity.findViewById<View>(R.id.typeMeasurable).performClick()
            assertEquals(HabitType.NUMERICAL, activity.habitType)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.frequencyOuterBox).visibility)
            for (id in listOf(R.id.unitOuterBox, R.id.targetOuterBox, R.id.targetTypeOuterBox)) {
                assertEquals(View.VISIBLE, activity.findViewById<View>(id).visibility)
            }
            val target = activity.findViewById<EditText>(R.id.targetInput)
            target.keyListener = null
            target.setText("bad target")
            activity.findViewById<View>(R.id.buttonSave).performClick()
            assertFalse(activity.isFinishing)
            assertNotNull(target.error)
            target.setText("12.5")
            activity.findViewById<EditText>(R.id.unitInput).setText("chapters")
            activity.targetType = NumericalHabitType.AT_MOST
            activity.freqNum = 3
            activity.freqDen = 7
        }
        val saved = habitList.first { it.name == "Read chapters" }
        assertEquals(HabitType.NUMERICAL, saved.type)
        assertEquals(12.5, saved.targetValue)
        assertEquals("chapters", saved.unit)
        assertEquals(NumericalHabitType.AT_MOST, saved.targetType)
        assertEquals(Frequency(3, 7), saved.frequency)
    }

    @Test
    fun testToggleBackToYesNoRestoresFrequencyAndIgnoresHiddenTarget() {
        saveHabit(null) { activity ->
            activity.findViewById<EditText>(R.id.nameInput).setText("Three walks")
            activity.freqNum = 3
            activity.freqDen = 7
            activity.findViewById<View>(R.id.typeMeasurable).performClick()
            assertEquals(3, activity.freqNum)
            assertEquals(7, activity.freqDen)
            activity.findViewById<EditText>(R.id.targetInput).setText("")
            activity.findViewById<View>(R.id.buttonSave).performClick()
            assertFalse(activity.isFinishing)
            activity.findViewById<View>(R.id.typeYesNo).performClick()
            assertEquals(3, activity.freqNum)
            assertEquals(7, activity.freqDen)
            assertNull(activity.findViewById<EditText>(R.id.targetInput).error)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.unitOuterBox).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.frequencyOuterBox).visibility)
            assertEquals(
                activity.getString(R.string.example_question_boolean),
                activity.findViewById<EditText>(R.id.questionInput).hint.toString()
            )
        }
        val saved = habitList.first { it.name == "Three walks" }
        assertEquals(HabitType.YES_NO, saved.type)
        assertEquals(Frequency(3, 7), saved.frequency)
    }

    @Test
    fun testBothTypeDraftsAndOrganizationSurviveToggleAndRotation() {
        val section = appComponent.sectionList.add("Morning")
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                activity.freqNum = 3
                activity.freqDen = 7
                activity.findViewById<View>(R.id.typeMeasurable).performClick()
                activity.freqDen = 30
                activity.targetType = NumericalHabitType.AT_MOST
                activity.reminderHour = 9
                activity.reminderMin = 15
                activity.reminderDays = WeekdayList(31)
                activity.sectionId = section.id
                val picker = openTags(activity)
                addTag(picker, "Draft tag")
                picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                activity.findViewById<EditText>(R.id.nameInput).setText("Rotated draft")
                activity.findViewById<EditText>(R.id.unitInput).setText("kilometres")
                activity.findViewById<EditText>(R.id.targetInput).setText("14.5")
                activity.findViewById<View>(R.id.moreToggle).performClick()
                activity.findViewById<EditText>(R.id.questionInput).setText("Draft question?")
                activity.findViewById<EditText>(R.id.notesInput).setText("Draft notes")
                activity.findViewById<View>(R.id.moreToggle).performClick()
                activity.findViewById<View>(R.id.typeYesNo).performClick()
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(HabitType.YES_NO, activity.habitType)
                assertEquals(3, activity.freqNum)
                assertEquals(30, activity.freqDen)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.moreGroup).visibility)
                activity.findViewById<View>(R.id.typeMeasurable).performClick()
                assertEquals(3, activity.freqNum)
                assertEquals(30, activity.freqDen)
                assertEquals(NumericalHabitType.AT_MOST, activity.targetType)
                assertEquals(section.id, activity.sectionId)
                assertEquals("Morning", activity.findViewById<TextView>(R.id.sectionPicker).text.toString())
                assertEquals(setOf("Draft tag"), activity.tags)
                assertEquals(9, activity.reminderHour)
                assertEquals(15, activity.reminderMin)
                assertEquals(WeekdayList(31), activity.reminderDays)
                for ((id, text) in listOf(
                    R.id.nameInput to "Rotated draft",
                    R.id.unitInput to "kilometres",
                    R.id.targetInput to "14.5",
                    R.id.questionInput to "Draft question?",
                    R.id.notesInput to "Draft notes"
                )) {
                    assertEquals(text, activity.findViewById<EditText>(id).text.toString())
                }
                activity.findViewById<View>(R.id.moreToggle).performClick()
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(HabitType.NUMERICAL, activity.habitType)
                assertTrue(activity.findViewById<MaterialButton>(R.id.typeMeasurable).isChecked)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.moreGroup).visibility)
                activity.findViewById<View>(R.id.buttonSave).performClick()
            }
        }
        val saved = habitList.first { it.name == "Rotated draft" }
        assertEquals(Frequency(1, 30), saved.frequency)
        assertEquals(14.5, saved.targetValue)
        assertEquals(section.id, saved.sectionId)
        assertEquals(setOf("Draft tag"), saved.tags)
        assertEquals("Draft notes", saved.description)
    }

    @Test
    fun testExistingTypeIsHiddenAndMoreDefaultsToNonblankContent() {
        val existing = fixtures.createLongNumericalHabit()
        for ((question, notes, expanded) in listOf(
            Triple("", "", false),
            Triple("  ", "\n", false),
            Triple("How far?", "", true),
            Triple("", "Remember water", true)
        )) {
            existing.question = question
            existing.description = notes
            val intent = Intent(targetContext, EditHabitActivity::class.java).putExtra("habitId", existing.id)
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.typeOuterBox).visibility)
                    activity.findViewById<View>(R.id.typeYesNo).performClick()
                    assertEquals(HabitType.NUMERICAL, activity.habitType)
                    assertEquals(if (expanded) View.VISIBLE else View.GONE, activity.findViewById<View>(R.id.moreGroup).visibility)
                    activity.findViewById<View>(R.id.moreToggle).performClick()
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals(if (expanded) View.GONE else View.VISIBLE, activity.findViewById<View>(R.id.moreGroup).visibility)
                }
            }
        }
    }

    @Test
    fun testTypeButtonsAndTagPickerInflateAndTintAcrossThemes() {
        for ((theme, pureBlack) in listOf(
            ThemeSwitcher.THEME_LIGHT to false,
            ThemeSwitcher.THEME_DARK to false,
            ThemeSwitcher.THEME_DARK to true
        )) {
            prefs.theme = theme
            prefs.isPureBlackEnabled = pureBlack
            ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    val yesNo = activity.findViewById<MaterialButton>(R.id.typeYesNo)
                    assertEquals(activity.androidColor, yesNo.backgroundTintList!!.getColorForState(yesNo.drawableState, 0))
                    assertEquals(contrastingTextColor(activity.androidColor), yesNo.currentTextColor)
                    activity.findViewById<View>(R.id.typeMeasurable).performClick()
                    val measurable = activity.findViewById<MaterialButton>(R.id.typeMeasurable)
                    assertTrue(measurable.isChecked)
                    assertEquals(activity.androidColor, measurable.backgroundTintList!!.getColorForState(measurable.drawableState, 0))
                    val picker = openTags(activity)
                    addTag(picker, "Accessible label")
                    val checkbox = picker.findViewById<LinearLayout>(R.id.tagChoices)!!.children
                        .filterIsInstance<MaterialCheckBox>().single { it.text == "Accessible label" }
                    assertTrue(checkbox.isChecked)
                    assertTrue(checkbox.isFocusable)
                    picker.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                }
            }
        }
    }

    @Test
    fun testNewHabitUsesBlueThenRemembersSavedColorButNotEdits() {
        val existing = fixtures.createEmptyHabit().apply { color = PaletteColor(3) }
        saveHabit(null) { activity ->
            assertEquals(PaletteColor.DEFAULT, activity.color)
            activity.findViewById<EditText>(R.id.nameInput).setText("New blue habit")
            activity.color = PaletteColor(39)
        }
        assertEquals(39, prefs.getDefaultHabitColor(-1))
        assertEquals(PaletteColor(39), habitList.first { it.name == "New blue habit" }.color)
        saveHabit(existing.id) { activity ->
            assertEquals(PaletteColor(3), activity.color)
            activity.color = PaletteColor(12)
        }
        assertEquals(39, prefs.getDefaultHabitColor(-1))
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(PaletteColor(39), activity.color)
                activity.color = PaletteColor(1)
            }
        }
        assertEquals(39, prefs.getDefaultHabitColor(-1))
    }

    @Test
    fun testClampsRememberedColorAndPreservesUnsavedSelectionOnRecreation() {
        for ((stored, expected) in listOf(-10 to 0, 80 to 39)) {
            prefs.setDefaultHabitColor(stored)
            ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(PaletteColor(expected), activity.color)
                    activity.color = PaletteColor(20)
                }
                scenario.recreate()
                scenario.onActivity { activity -> assertEquals(PaletteColor(20), activity.color) }
            }
            assertEquals(stored, prefs.getDefaultHabitColor(-1))
        }
    }

    @Test
    fun testInvalidNewHabitDoesNotRememberColor() {
        prefs.setDefaultHabitColor(5)
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                activity.color = PaletteColor(39)
                activity.findViewById<EditText>(R.id.nameInput).setText("")
                activity.findViewById<android.view.View>(R.id.buttonSave).performClick()
                assertFalse(activity.isFinishing)
                assertEquals(5, prefs.getDefaultHabitColor(-1))
            }
        }
    }

    @Test
    fun testLoadsTagsAndPreservesPickerSelectionAndDraftOnRecreation() {
        val existing = fixtures.createEmptyHabit().apply {
            tags = setOf("Outdoors", "Health")
        }
        val draft = "  Evening routine "
        for (habitId in listOf(null, existing.id)) {
            val intent = Intent(targetContext, EditHabitActivity::class.java)
            if (habitId != null) intent.putExtra("habitId", habitId)
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(
                        if (habitId == null) "None" else HabitTags.formatInline(existing.tags),
                        activity.findViewById<TextView>(R.id.tagsPicker).text.toString()
                    )
                    val picker = openTags(activity)
                    addTag(picker, "  Focus  ")
                    picker.findViewById<EditText>(R.id.newTagInput)!!.setText(draft)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val picker = tagsDialog(activity)
                    assertEquals(draft, picker.findViewById<EditText>(R.id.newTagInput)!!.text.toString())
                    val focus = picker.findViewById<LinearLayout>(R.id.tagChoices)!!.children
                        .filterIsInstance<MaterialCheckBox>().single { it.text == "Focus" }
                    assertTrue(focus.isChecked)
                    picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val expected = (if (habitId == null) emptySet() else existing.tags) + setOf("Focus", "Evening routine")
                    assertEquals(expected, activity.tags)
                    assertEquals(HabitTags.formatInline(expected), activity.findViewById<TextView>(R.id.tagsPicker).text.toString())
                }
                assertEquals(setOf("Outdoors", "Health"), existing.tags)
            }
        }
    }

    @Test
    fun testCreatesEditsAndClearsTags() {
        val existing = fixtures.createEmptyHabit().apply { tags = setOf("Original") }
        for ((index, habitId) in listOf(null, existing.id, existing.id).withIndex()) {
            val name = "Tagged habit $index"
            val expected = if (index == 2) emptySet() else setOf("Focus", "Evening")
            saveHabit(habitId) { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText(name)
                val picker = openTags(activity)
                picker.findViewById<LinearLayout>(R.id.tagChoices)!!.children
                    .filterIsInstance<MaterialCheckBox>().forEach { it.isChecked = false }
                if (index != 2) {
                    addTag(picker, "  Focus  ")
                    addTag(picker, "focus")
                    addTag(picker, " Evening ")
                }
                picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            assertEquals(expected, habitList.first { it.name == name }.tags)
        }
    }

    @Test
    fun testPreservesSecondaryReminderTimesAndClearsAllWhenDisabled() {
        val existing = fixtures.createEmptyHabit().apply {
            reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
            replaceReminderTimes(listOf(510, 750, 1230))
        }
        val days = WeekdayList(31)
        saveHabit(existing.id) { activity ->
            activity.reminderHour = 9
            activity.reminderMin = 15
            activity.reminderDays = days
        }
        val edited = habitList.getById(existing.id!!)!!
        assertEquals(setOf(555, 750, 1230), edited.reminderTimes)
        assertEquals(setOf(750, 1230), edited.extraReminderTimes)
        assertEquals(days, edited.reminder!!.days)

        saveHabit(existing.id) { activity ->
            activity.reminderHour = -1
            activity.reminderMin = -1
        }
        val disabled = habitList.getById(existing.id!!)!!
        assertNull(disabled.reminder)
        assertTrue(disabled.reminderTimes.isEmpty())
        assertTrue(disabled.extraReminderTimes.isEmpty())
    }

    @Test
    fun testRotationKeepsSavedExtraRemindersAndUnsavedPrimaryDays() {
        val section = appComponent.sectionList.add("Evening")
        val existing = fixtures.createEmptyHabit().apply {
            reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
            replaceReminderTimes(listOf(510, 750, 1230))
            sectionId = section.id
            tags = setOf("Health")
        }
        val intent = Intent(targetContext, EditHabitActivity::class.java).putExtra("habitId", existing.id)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.reminderHour = 9
                activity.reminderMin = 15
                activity.reminderDays = WeekdayList(31)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.buttonSave).performClick()
            }
        }
        assertEquals(setOf(555, 750, 1230), existing.reminderTimes)
        assertEquals(WeekdayList(31), existing.reminder!!.days)
        assertEquals(section.id, existing.sectionId)
        assertEquals(setOf("Health"), existing.tags)
    }

    @Test
    fun testRestoresUnsavedTargetTypeForNewAndExistingHabits() {
        val existing = fixtures.createLongNumericalHabit()
        for (habitId in listOf(null, existing.id)) {
            val intent = Intent(targetContext, EditHabitActivity::class.java)
                .putExtra("habitType", HabitType.NUMERICAL.value)
            if (habitId != null) intent.putExtra("habitId", habitId)
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    activity.targetType = NumericalHabitType.AT_MOST
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals(NumericalHabitType.AT_MOST, activity.targetType)
                    assertEquals(
                        activity.getString(R.string.target_type_at_most),
                        activity.findViewById<android.widget.TextView>(R.id.targetTypePicker).text
                    )
                }
            }
        }
    }

    @Test
    fun testRejectsWhitespaceNameAndMalformedTargetWithoutClosing() {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
            .putExtra("habitType", HabitType.NUMERICAL.value)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val name = activity.findViewById<EditText>(R.id.nameInput)
                val target = activity.findViewById<EditText>(R.id.targetInput)
                name.setText("   ")
                // Bypass keyboard filtering to cover pasted/restored malformed values.
                target.keyListener = null
                target.setText("1.2.3")
                activity.findViewById<android.view.View>(R.id.buttonSave).performClick()
                assertNotNull(name.error)
                assertNotNull(target.error)
                assertFalse(activity.isFinishing)
                assertEquals("1.2.3", target.text.toString())
            }
        }
    }

    private fun openTags(activity: EditHabitActivity): AlertDialog {
        activity.findViewById<View>(R.id.tagsPicker).performClick()
        activity.supportFragmentManager.executePendingTransactions()
        return tagsDialog(activity)
    }

    private fun tagsDialog(activity: EditHabitActivity): AlertDialog =
        (activity.supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) as TagPickerDialog)
            .requireDialog() as AlertDialog

    private fun addTag(picker: AlertDialog, name: String) {
        picker.findViewById<EditText>(R.id.newTagInput)!!.setText(name)
        picker.findViewById<View>(R.id.addTagButton)!!.performClick()
    }

    private fun saveHabit(habitId: Long?, edit: (EditHabitActivity) -> Unit) {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
        if (habitId != null) intent.putExtra("habitId", habitId)
        val finished = CountDownLatch(1)
        val listener = object : CommandRunner.Listener {
            override fun onCommandFinished(command: Command) {
                finished.countDown()
            }
        }
        appComponent.commandRunner.addListener(listener)
        try {
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    edit(activity)
                    activity.findViewById<android.view.View>(R.id.buttonSave).performClick()
                }
                assertTrue("Habit save did not finish", finished.await(10, TimeUnit.SECONDS))
            }
        } finally {
            appComponent.commandRunner.removeListener(listener)
        }
    }
}
