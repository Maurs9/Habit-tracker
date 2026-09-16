package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitControlDialogsTest : BaseAndroidTest() {
    @Test
    fun testClearingNumberUnsetsEntryWhenQuestionMarksAreDisabled() {
        prefs.areQuestionMarksEnabled = false
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val results = mutableListOf<Pair<Double, String>>()
                val fragment = numberDialog().apply {
                    onToggle = { value, notes -> results.add(value to notes) }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                val dialog = fragment.requireDialog()
                assertEquals(View.GONE, dialog.findViewById<View>(R.id.unknownBtnNumber).visibility)
                dialog.findViewById<EditText>(R.id.value).text.clear()
                dialog.findViewById<EditText>(R.id.notes).setText("Preserve my note")
                fragment.save()
                assertEquals(listOf(Entry.UNKNOWN / 1000.0 to "Preserve my note"), results)
                assertFalse(dialog.isShowing)
            }
        }
    }

    @Test
    fun testMalformedNumberKeepsDialogAndNotesThenAllowsZero() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                var result: Pair<Double, String>? = null
                val fragment = numberDialog().apply {
                    onToggle = { value, notes -> result = value to notes }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                val dialog = fragment.requireDialog()
                val value = dialog.findViewById<EditText>(R.id.value)
                val notes = dialog.findViewById<EditText>(R.id.notes)
                value.keyListener = null
                value.setText("12oops")
                notes.setText("Keep this note")
                fragment.save()
                assertNull(result)
                assertTrue(dialog.isShowing)
                assertNotNull(value.error)
                assertEquals("Keep this note", notes.text.toString())
                value.setText("0")
                fragment.save()
                assertEquals(0.0 to "Keep this note", result)
            }
        }
    }

    @Test
    fun testSpecialButtonsPreserveNotesWithoutParsingTheInput() {
        prefs.isSkipEnabled = true
        prefs.areQuestionMarksEnabled = true
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                for ((buttonId, expected) in listOf(R.id.skipBtnNumber to Entry.SKIP, R.id.unknownBtnNumber to Entry.UNKNOWN)) {
                    var result: Pair<Double, String>? = null
                    val fragment = numberDialog().apply {
                        onToggle = { value, notes -> result = value to notes }
                    }
                    fragment.showNow(activity.supportFragmentManager, "number")
                    val dialog = fragment.requireDialog()
                    val value = dialog.findViewById<EditText>(R.id.value)
                    assertEquals("0.001", value.text.toString())
                    value.keyListener = null
                    value.setText("invalid")
                    dialog.findViewById<View>(buttonId).performClick()
                    assertEquals(expected / 1000.0 to "Original notes", result)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    @Test
    fun testLastCuratedColorIsSelectableInEveryTheme() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme())) {
                    var selected: PaletteColor? = null
                    val fragment = ColorPickerDialogFactory(activity).create(PaletteColor.DEFAULT, theme)
                    fragment.setListener { selected = it }
                    fragment.showNow(activity.supportFragmentManager, "palette")
                    val dialog = fragment.requireDialog()
                    val wheel = dialog.findViewById<ColorWheelView>(R.id.color_picker)
                    assertEquals(40, wheel.colors.size)
                    assertEquals(PaletteColor.DEFAULT.paletteIndex, wheel.selectedIndex)
                    assertTrue(wheel.colorNames[39].isNotBlank())
                    wheel.select(39)
                    assertEquals(39, wheel.selectedIndex)
                    assertNull(selected)
                    dialog.findViewById<View>(android.R.id.button1).performClick()
                    assertEquals(PaletteColor(39), selected)
                    activity.supportFragmentManager.executePendingTransactions()
                }
            }
        }
    }

    private fun numberDialog() = NumberDialog().apply {
        arguments = Bundle().apply {
            putInt("color", android.graphics.Color.BLUE)
            putDouble("value", 0.001)
            putString("notes", "Original notes")
        }

        @Test
        fun testWheelExposesAllColorsAsSelectableVirtualViews() {
            ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = ColorPickerDialogFactory(activity).create(PaletteColor.DEFAULT, LightTheme())
                    var picked: PaletteColor? = null
                    fragment.setListener { picked = it }
                    fragment.showNow(activity.supportFragmentManager, "accessiblePalette")
                    val dialog = fragment.requireDialog()
                    val wheel = dialog.findViewById<ColorWheelView>(R.id.color_picker)
                    wheel.measure(
                        View.MeasureSpec.makeMeasureSpec(560, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(560, View.MeasureSpec.EXACTLY)
                    )
                    wheel.layout(0, 0, 560, 560)
                    val provider = ViewCompat.getAccessibilityNodeProvider(wheel)!!
                    assertEquals(40, provider.createAccessibilityNodeInfo(ExploreByTouchHelper.HOST_ID)!!.childCount)
                    for (index in 0 until PaletteColor.COUNT) {
                        val node = provider.createAccessibilityNodeInfo(index)!!
                        assertEquals(wheel.colorNames[index], node.contentDescription)
                        assertTrue(node.isCheckable)
                        assertEquals(wheel.selectedIndex == index, node.isChecked)
                        val bounds = Rect()
                        node.getBoundsInParent(bounds)
                        assertFalse(bounds.isEmpty)
                        assertTrue(Rect(0, 0, wheel.width, wheel.height).contains(bounds))
                        assertTrue(provider.performAction(index, AccessibilityNodeInfoCompat.ACTION_CLICK, null))
                        assertEquals(index, wheel.selectedIndex)
                        assertTrue(provider.createAccessibilityNodeInfo(index)!!.isChecked)
                    }
                    assertNull(picked)
                    assertTrue(provider.performAction(38, AccessibilityNodeInfoCompat.ACTION_FOCUS, null))
                    wheel.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    wheel.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    assertEquals(38, wheel.selectedIndex)
                    dialog.findViewById<View>(android.R.id.button1).performClick()
                    assertEquals(PaletteColor(38), picked)
                }
            }
        }

        @Test
        fun testWheelTouchUsesSelectionCallbackAndCancelDoesNotCommit() {
            ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = ColorPickerDialogFactory(activity).create(PaletteColor.DEFAULT, LightTheme())
                    var picked: PaletteColor? = null
                    fragment.setListener { picked = it }
                    fragment.showNow(activity.supportFragmentManager, "touchPalette")
                    val dialog = fragment.requireDialog()
                    val wheel = dialog.findViewById<ColorWheelView>(R.id.color_picker)
                    wheel.layout(0, 0, 560, 560)
                    val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 300f, 300f, 0)
                    try {
                        assertTrue(wheel.onTouchEvent(event))
                        assertEquals(39, wheel.selectedIndex)
                    } finally {
                        event.recycle()
                    }
                    dialog.findViewById<View>(android.R.id.button2).performClick()
                    assertNull(picked)
                }
            }
        }
    }
}
