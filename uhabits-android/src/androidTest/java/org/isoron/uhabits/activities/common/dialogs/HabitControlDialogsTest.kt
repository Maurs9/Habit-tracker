package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
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
                    val fragment = ColorPickerDialogFactory(activity).create(PaletteColor(39), theme)
                    fragment.setListener { selected = it }
                    fragment.showNow(activity.supportFragmentManager, "palette")
                    val grid = fragment.requireDialog().findViewById<GridLayout>(R.id.color_picker)
                    assertEquals(PaletteColor.COUNT, grid.childCount)
                    val last = grid.getChildAt(39) as MaterialButton
                    assertTrue(last.isChecked)
                    assertTrue(last.contentDescription.isNotBlank())
                    last.performClick()
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
    }
}
