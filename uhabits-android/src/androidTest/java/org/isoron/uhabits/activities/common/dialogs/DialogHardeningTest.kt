package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogHardeningTest : BaseAndroidTest() {
    @Test
    fun testCheckmarkActionsExposeLabelsAndButtonsAndExecuteTheirStates() {
        prefs.isSkipEnabled = true
        prefs.areQuestionMarksEnabled = true
        launchEditor().use { scenario ->
            for ((id, label, expected) in listOf(
                Triple(R.id.yesBtn, R.string.entry_action_complete, Entry.YES_MANUAL),
                Triple(R.id.noBtn, R.string.entry_action_not_complete, Entry.NO),
                Triple(R.id.skipBtn, R.string.entry_action_skip, Entry.SKIP),
                Triple(R.id.unknownBtn, R.string.entry_action_clear, Entry.UNKNOWN)
            )) {
                var result: Pair<Int, String>? = null
                scenario.onActivity { activity ->
                    val fragment = CheckmarkDialog().apply {
                        arguments = EntryDialogFragment.arguments(habitList.getByPosition(0), day(0), android.graphics.Color.BLUE).apply {
                            putInt("value", Entry.NO)
                            putString("notes", "Keep my note")
                        }
                        onToggle = { value, notes -> result = value to notes }
                    }
                    fragment.showNow(activity.supportFragmentManager, "checkmark")
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("checkmark") as CheckmarkDialog
                    assertButtonAction(fragment.requireDialog().findViewById(id), label)
                    assertEquals(expected to "Keep my note", result)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    @Test
    fun testNumberActionsExposeLabelsAndButtonsAndPreserveNotes() {
        prefs.isSkipEnabled = true
        prefs.areQuestionMarksEnabled = true
        launchEditor().use { scenario ->
            for ((id, label, expected) in listOf(
                Triple(R.id.saveBtn, R.string.save, 2.5),
                Triple(R.id.skipBtnNumber, R.string.entry_action_skip, Entry.SKIP / 1000.0),
                Triple(R.id.unknownBtnNumber, R.string.entry_action_clear, Entry.UNKNOWN / 1000.0)
            )) {
                var result: Pair<Double, String>? = null
                scenario.onActivity { activity ->
                    val fragment = NumberDialog().apply {
                        arguments = EntryDialogFragment.arguments(habitList.getByPosition(0), day(0), android.graphics.Color.BLUE).apply {
                            putDouble("value", 2.5)
                            putString("notes", "Keep my note")
                        }
                        onToggle = { value, notes -> result = value to notes }
                    }
                    fragment.showNow(activity.supportFragmentManager, "number")
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("number") as NumberDialog
                    assertButtonAction(fragment.requireDialog().findViewById(id), label)
                    assertEquals(expected to "Keep my note", result)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    @Test
    fun testFrequencyInputsRetainKeyboardFocusAndAcceptDigits() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(false)
        try {
            launchEditor().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = FrequencyPickerDialog(1, 1)
                    fragment.showNow(activity.supportFragmentManager, "frequency")
                    val dialog = fragment.requireDialog()
                    assertTrue(dialog.findViewById<RadioButton>(R.id.everyDayRadioButton).isChecked)
                    for ((inputId, radioId) in listOf(
                        R.id.everyXDaysTextView to R.id.everyXDaysRadioButton,
                        R.id.xTimesPerWeekTextView to R.id.xTimesPerWeekRadioButton,
                        R.id.xTimesPerMonthTextView to R.id.xTimesPerMonthRadioButton,
                        R.id.xTimesPerYDaysXTextView to R.id.xTimesPerYDaysRadioButton,
                        R.id.xTimesPerYDaysYTextView to R.id.xTimesPerYDaysRadioButton
                    )) {
                        val input = dialog.findViewById<EditText>(inputId)
                        input.setText("")
                        assertTrue(input.requestFocus())
                        assertTrue(input.hasFocus())
                        assertTrue(dialog.findViewById<RadioButton>(radioId).isChecked)
                        input.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_4))
                        input.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_4))
                        assertEquals("4", input.text.toString())
                    }
                    dialog.findViewById<View>(R.id.everyXDaysRadioButton).performClick()
                    assertTrue(dialog.findViewById<EditText>(R.id.everyXDaysTextView).hasFocus())
                }
            }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    @Test
    fun testDailySelectionSurvivesRecreationWithStaleInputFocus() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(true)
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = FrequencyPickerDialog(3, 7)
                fragment.showNow(activity.supportFragmentManager, "frequencyPicker")
                val dialog = fragment.requireDialog()
                val input = dialog.findViewById<EditText>(R.id.xTimesPerWeekTextView)
                input.setText("4")
                assertTrue(input.requestFocus())
                dialog.findViewById<View>(R.id.everyDayRadioButton).performClick()
                assertFalse(input.hasFocus())
                assertTrue(dialog.findViewById<RadioButton>(R.id.everyDayRadioButton).isChecked)

                // Reproduce a snapshot from an older version that left the weekly input focused.
                input.requestFocus()
                dialog.findViewById<RadioButton>(R.id.xTimesPerWeekRadioButton).isChecked = false
                dialog.findViewById<RadioButton>(R.id.everyDayRadioButton).isChecked = true
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                var result: Pair<Int, Int>? = null
                activity.supportFragmentManager.setFragmentResultListener(
                    FrequencyPickerDialog.REQUEST_KEY,
                    activity
                ) { _, data ->
                    result = data.getInt(FrequencyPickerDialog.NUMERATOR) to
                        data.getInt(FrequencyPickerDialog.DENOMINATOR)
                }
                val fragment = activity.supportFragmentManager.findFragmentByTag("frequencyPicker") as FrequencyPickerDialog
                val dialog = fragment.requireDialog() as AlertDialog
                assertTrue(dialog.findViewById<RadioButton>(R.id.everyDayRadioButton)!!.isChecked)
                assertFalse(dialog.findViewById<EditText>(R.id.xTimesPerWeekTextView)!!.hasFocus())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(1 to 1, result)
            }
        }
    }

    @Test
    fun testFrequencyRejectsInvalidDaysWithoutClosingOrLosingInput() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                var result: Pair<Int, Int>? = null
                val fragment = FrequencyPickerDialog(3, 7).apply {
                    onFrequencyPicked = { num, den -> result = num to den }
                }
                fragment.showNow(activity.supportFragmentManager, "frequency")
                val dialog = fragment.requireDialog() as AlertDialog
                dialog.findViewById<View>(R.id.everyXDaysRadioButton)!!.performClick()
                val input = dialog.findViewById<EditText>(R.id.everyXDaysTextView)!!
                input.keyListener = null
                input.filters = emptyArray()
                for (text in listOf("", "0", "-1", "oops", "1.5", "999999999999")) {
                    input.setText(text)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    assertTrue(dialog.isShowing)
                    assertNull(result)
                    assertNotNull(input.error)
                    assertEquals(text, input.text.toString())
                }
                input.setText("1")
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(1 to 1, result)
                assertFalse(dialog.isShowing)
            }
        }
    }

    @Test
    fun testFrequencyRejectsTooManyTimesButNormalizesEqualCountsToDaily() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                for ((radioId, inputId, days) in listOf(
                    Triple(R.id.xTimesPerWeekRadioButton, R.id.xTimesPerWeekTextView, 7),
                    Triple(R.id.xTimesPerMonthRadioButton, R.id.xTimesPerMonthTextView, 30),
                    Triple(R.id.xTimesPerYDaysRadioButton, R.id.xTimesPerYDaysXTextView, 14)
                )) {
                    var result: Pair<Int, Int>? = null
                    val fragment = FrequencyPickerDialog(3, 7).apply {
                        onFrequencyPicked = { num, den -> result = num to den }
                    }
                    fragment.showNow(activity.supportFragmentManager, "frequency")
                    val dialog = fragment.requireDialog() as AlertDialog
                    dialog.findViewById<View>(radioId)!!.performClick()
                    val input = dialog.findViewById<EditText>(inputId)!!
                    input.setText((days + 1).toString())
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    assertNull(result)
                    assertEquals(activity.getString(R.string.frequency_not_more_than_days), input.error.toString())
                    assertTrue(dialog.isShowing)
                    input.setText(days.toString())
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    assertEquals(1 to 1, result)
                    assertFalse(dialog.isShowing)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    @Test
    fun testFrequencyPreservesOutOfRangeInputAndAcceptsTheModelLimit() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                var result: Pair<Int, Int>? = null
                val fragment = FrequencyPickerDialog(1, 1).apply {
                    onFrequencyPicked = { num, den -> result = num to den }
                }
                fragment.showNow(activity.supportFragmentManager, "frequency")
                val dialog = fragment.requireDialog() as AlertDialog
                dialog.findViewById<View>(R.id.everyXDaysRadioButton)!!.performClick()
                val input = dialog.findViewById<EditText>(R.id.everyXDaysTextView)!!
                val limit = Frequency.MAX_DENOMINATOR
                val invalid = (limit.toLong() + 1).toString()
                input.setText(invalid)
                assertEquals(invalid, input.text.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertNull(result)
                assertTrue(dialog.isShowing)
                assertEquals(activity.getString(R.string.frequency_positive_integer), input.error.toString())
                input.setText(limit.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(1 to limit, result)
            }
        }
    }

    private fun assertButtonAction(button: View, label: Int) {
        assertTrue(button.isAttachedToWindow)
        val node = button.createAccessibilityNodeInfo()
        assertEquals(button.context.getString(label), node.contentDescription?.toString())
        assertEquals(Button::class.java.name, node.className.toString())
        assertTrue(node.isClickable)
        assertTrue(button.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
    }

    private fun launchEditor() =
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java))
}
