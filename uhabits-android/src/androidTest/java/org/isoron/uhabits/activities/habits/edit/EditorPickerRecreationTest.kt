package org.isoron.uhabits.activities.habits.edit

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.descendants
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.timepicker.MaterialTimePicker
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialog
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.common.dialogs.WeekdayPickerDialog
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorPickerRecreationTest : BaseAndroidTest() {
    @Test
    fun testPendingColorSurvivesRecreationAndOnlyConfirmationUpdatesEditor() {
        for (confirm in listOf(false, true)) {
            launchEditor().use { scenario ->
                scenario.onActivity { activity ->
                    openPicker(activity, R.id.colorButton)
                    val picker = activity.supportFragmentManager.findFragmentByTag("colorPicker") as ColorPickerDialog
                    picker.requireDialog().findViewById<ColorWheelView>(R.id.color_picker).select(42)
                    assertEquals(PaletteColor.DEFAULT, activity.color)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val picker = activity.supportFragmentManager.findFragmentByTag("colorPicker") as ColorPickerDialog
                    val dialog = picker.requireDialog() as AlertDialog
                    assertTrue(dialog.isShowing)
                    assertEquals(42, dialog.findViewById<ColorWheelView>(R.id.color_picker)!!.selectedIndex)
                    assertEquals(PaletteColor.DEFAULT, activity.color)
                    dialog.getButton(if (confirm) AlertDialog.BUTTON_POSITIVE else AlertDialog.BUTTON_NEGATIVE).performClick()
                    assertEquals(if (confirm) PaletteColor(42) else PaletteColor.DEFAULT, activity.color)
                }
            }
        }
    }

    @Test
    fun testPendingCustomFrequencySurvivesRecreationAndOnlyConfirmationUpdatesEditor() {
        for (confirm in listOf(false, true)) {
            launchEditor().use { scenario ->
                scenario.onActivity { activity ->
                    openPicker(activity, R.id.boolean_frequency_picker)
                    val dialog = frequencyDialog(activity)
                    dialog.findViewById<View>(R.id.xTimesPerYDaysRadioButton)!!.performClick()
                    dialog.findViewById<EditText>(R.id.xTimesPerYDaysXTextView)!!.setText("4")
                    dialog.findViewById<EditText>(R.id.xTimesPerYDaysYTextView)!!.setText("13")
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val dialog = frequencyDialog(activity)
                    assertTrue(dialog.isShowing)
                    assertTrue(dialog.findViewById<RadioButton>(R.id.xTimesPerYDaysRadioButton)!!.isChecked)
                    assertEquals("4", dialog.findViewById<EditText>(R.id.xTimesPerYDaysXTextView)!!.text.toString())
                    assertEquals("13", dialog.findViewById<EditText>(R.id.xTimesPerYDaysYTextView)!!.text.toString())
                    assertEquals(1, activity.freqNum)
                    assertEquals(1, activity.freqDen)
                    if (confirm) dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick() else dialog.cancel()
                    assertEquals(if (confirm) 4 else 1, activity.freqNum)
                    assertEquals(if (confirm) 13 else 1, activity.freqDen)
                }
            }
        }
    }

    @Test
    fun testBlankFrequencyDraftAndErrorSurviveRecreationAndCanBeCorrected() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                openPicker(activity, R.id.boolean_frequency_picker)
                val dialog = frequencyDialog(activity)
                dialog.findViewById<View>(R.id.everyXDaysRadioButton)!!.performClick()
                dialog.findViewById<EditText>(R.id.everyXDaysTextView)!!.setText("")
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val dialog = frequencyDialog(activity)
                val input = dialog.findViewById<EditText>(R.id.everyXDaysTextView)!!
                assertEquals("", input.text.toString())
                assertNotNull(input.error)
                assertTrue(dialog.findViewById<RadioButton>(R.id.everyXDaysRadioButton)!!.isChecked)
                input.setText("5")
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(1, activity.freqNum)
                assertEquals(5, activity.freqDen)
            }
        }
    }

    @Test
    fun testPendingWeekdaysSurviveRecreationAndOnlyConfirmationUpdatesEditor() {
        val habit = fixtures.createEmptyHabit().apply {
            reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
        }
        val expected = WeekdayList(booleanArrayOf(false, true, true, true, true, true, true))
        for (confirm in listOf(false, true)) {
            launchEditor(habit.id).use { scenario ->
                scenario.onActivity { activity ->
                    openPicker(activity, R.id.reminderDatePicker)
                    val picker = activity.supportFragmentManager.findFragmentByTag("dayPicker") as WeekdayPickerDialog
                    val dialog = picker.requireDialog() as AlertDialog
                    dialog.listView.performItemClick(null, 0, dialog.listView.adapter.getItemId(0))
                    assertFalse(dialog.listView.isItemChecked(0))
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val picker = activity.supportFragmentManager.findFragmentByTag("dayPicker") as WeekdayPickerDialog
                    val dialog = picker.requireDialog() as AlertDialog
                    assertTrue(dialog.isShowing)
                    assertFalse(dialog.listView.isItemChecked(0))
                    assertEquals(WeekdayList.EVERY_DAY, activity.reminderDays)
                    dialog.getButton(if (confirm) AlertDialog.BUTTON_POSITIVE else AlertDialog.BUTTON_NEGATIVE).performClick()
                    assertEquals(if (confirm) expected else WeekdayList.EVERY_DAY, activity.reminderDays)
                }
            }
        }
    }

    @Test
    fun testPendingReminderTimeSurvivesRecreationWithSaveClearAndCancelReconnected() {
        val habit = fixtures.createEmptyHabit().apply {
            reminder = Reminder(8, 30, WeekdayList(31))
        }
        for (action in listOf("save", "clear", "cancel")) {
            launchEditor(habit.id).use { scenario ->
                scenario.onActivity { activity ->
                    openPicker(activity, R.id.reminderTimePicker)
                    val picker = timePicker(activity)
                    picker.requireDialog().findViewById<View>(
                        com.google.android.material.R.id.material_timepicker_mode_button
                    ).performClick()
                    for ((id, value) in listOf(
                        com.google.android.material.R.id.material_hour_text_input to "10",
                        com.google.android.material.R.id.material_minute_text_input to "45"
                    )) {
                        picker.requireDialog().findViewById<ViewGroup>(id).descendants
                            .filterIsInstance<EditText>().single().setText(value)
                    }
                    assertEquals(10, picker.hour)
                    assertEquals(45, picker.minute)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val picker = timePicker(activity)
                    assertTrue(picker.requireDialog().isShowing)
                    assertEquals(10, picker.hour)
                    assertEquals(45, picker.minute)
                    assertEquals(8, activity.reminderHour)
                    assertEquals(30, activity.reminderMin)
                    when (action) {
                        "save" -> picker.requireDialog().findViewById<View>(
                            com.google.android.material.R.id.material_timepicker_ok_button
                        ).performClick()
                        "clear" -> picker.requireDialog().findViewById<View>(
                            com.google.android.material.R.id.material_timepicker_cancel_button
                        ).performClick()
                        else -> picker.requireDialog().cancel()
                    }
                    assertEquals(if (action == "save") 10 else if (action == "clear") -1 else 8, activity.reminderHour)
                    assertEquals(if (action == "save") 45 else if (action == "clear") -1 else 30, activity.reminderMin)
                    assertEquals(if (action == "clear") WeekdayList.EVERY_DAY else WeekdayList(31), activity.reminderDays)
                }
            }
        }
    }

    private fun frequencyDialog(activity: EditHabitActivity) =
        (activity.supportFragmentManager.findFragmentByTag("frequencyPicker") as FrequencyPickerDialog).requireDialog() as AlertDialog

    private fun timePicker(activity: EditHabitActivity) =
        activity.supportFragmentManager.findFragmentByTag("timePicker") as MaterialTimePicker

    private fun openPicker(activity: EditHabitActivity, viewId: Int) {
        activity.findViewById<View>(viewId).performClick()
        activity.supportFragmentManager.executePendingTransactions()
    }

    private fun launchEditor(habitId: Long? = null): ActivityScenario<EditHabitActivity> {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
        if (habitId != null) intent.putExtra("habitId", habitId)
        return ActivityScenario.launch(intent)
    }
}
