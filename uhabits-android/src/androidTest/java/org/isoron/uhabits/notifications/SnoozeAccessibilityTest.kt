package org.isoron.uhabits.notifications

import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.datetimepicker.HapticFeedbackController
import com.android.datetimepicker.time.RadialPickerLayout
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnoozeAccessibilityTest : BaseAndroidTest() {
    @Test
    fun test24HourActionsAdvanceAndWrapAcrossEveryHour() {
        withPicker(is24Hour = true) { picker ->
            picker.setCurrentItemShowing(TimePickerDialog.HOUR_INDEX, false)
            for (hour in 0..23) {
                picker.setTime(hour, 37)
                assertTrue(picker.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
                assertEquals((hour + 1) % 24, picker.hours)
                assertEquals(37, picker.minutes)
                picker.setTime(hour, 37)
                assertTrue(picker.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
                assertEquals((hour + 23) % 24, picker.hours)
                assertEquals(37, picker.minutes)
            }
        }
    }

    @Test
    fun test12HourActionsKeepExistingClockWrapping() {
        withPicker(is24Hour = false) { picker ->
            picker.setCurrentItemShowing(TimePickerDialog.HOUR_INDEX, false)
            for ((hour, action, expected) in listOf(
                Triple(11, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, 12),
                Triple(12, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, 1),
                Triple(1, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, 12),
                Triple(3, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, 2)
            )) {
                picker.setTime(hour, 37)
                assertTrue(picker.performAccessibilityAction(action, null))
                assertEquals(expected, picker.hours)
                assertEquals(37, picker.minutes)
            }
        }
    }

    @Test
    fun testMinuteActionsKeepFiveMinuteSteppingAndWrapping() {
        for (is24Hour in listOf(false, true)) {
            withPicker(is24Hour) { picker ->
                picker.setCurrentItemShowing(TimePickerDialog.MINUTE_INDEX, false)
                for ((minute, action, expected) in listOf(
                    Triple(13, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, 15),
                    Triple(13, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, 10),
                    Triple(55, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, 0),
                    Triple(0, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, 55)
                )) {
                    picker.setTime(14, minute)
                    assertTrue(picker.performAccessibilityAction(action, null))
                    assertEquals(expected, picker.minutes)
                    assertEquals(14, picker.hours)
                }
            }
        }
    }

    private fun withPicker(is24Hour: Boolean, block: (RadialPickerLayout) -> Unit) {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val picker = RadialPickerLayout(activity, null)
                picker.initialize(activity, HapticFeedbackController(activity), 14, 37, is24Hour)
                picker.setOnValueSelectedListener { index, value, _ ->
                    assertEquals(if (index == TimePickerDialog.HOUR_INDEX) picker.hours else picker.minutes, value)
                }
                block(picker)
            }
        }
    }
}
