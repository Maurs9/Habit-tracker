package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.ColorUtils.calculateContrast
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
class ColorPickerRefinementsTest : BaseAndroidTest() {
    @Test
    fun actualPreviewBadgeMeetsNormalTextContrastForEveryColorAndTheme() {
        for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme())) {
            prefs.theme = if (theme is DarkTheme) ThemeSwitcher.THEME_DARK else ThemeSwitcher.THEME_LIGHT
            prefs.isPureBlackEnabled = theme is PureBlackTheme
            launchEditor().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = ColorPickerDialogFactory(activity).create(PaletteColor.DEFAULT, theme)
                    fragment.showNow(activity.supportFragmentManager, "palette")
                    val dialog = fragment.requireDialog()
                    val wheel = dialog.findViewById<ColorWheelView>(R.id.color_picker)
                    val preview = dialog.findViewById<TextView>(R.id.colorPickerPreview)
                    for (index in 0 until PaletteColor.COUNT) {
                        wheel.select(index)
                        val fill = (preview.background as GradientDrawable).color!!.defaultColor
                        assertEquals(theme.color(index).toInt(), fill)
                        assertEquals(contrastingTextColor(fill), preview.currentTextColor)
                        assertTrue(
                            "${theme.javaClass.simpleName}, slot $index",
                            calculateContrast(preview.currentTextColor, fill) >= 4.5
                        )
                    }
                }
            }
        }
    }

    @Test
    fun everyNamedListRowHasAtLeast48dpAndSelectsExactlyItsColor() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                var picked: PaletteColor? = null
                val fragment = ColorPickerDialogFactory(activity).create(PaletteColor.DEFAULT, LightTheme()).apply {
                    setListener { picked = it }
                }
                fragment.showNow(activity.supportFragmentManager, "palette")
                val dialog = fragment.requireDialog() as AlertDialog
                dialog.findViewById<View>(R.id.colorPickerModeToggle)!!.performClick()
                val list = dialog.findViewById<ListView>(R.id.colorPickerList)!!
                val wheel = dialog.findViewById<ColorWheelView>(R.id.color_picker)!!
                val density = activity.resources.displayMetrics.density
                val minimum = ceil(48 * density).toInt()
                assertEquals(View.VISIBLE, list.visibility)
                assertEquals(View.GONE, wheel.visibility)
                assertEquals(PaletteColor.COUNT, list.adapter.count)
                for (index in 0 until PaletteColor.COUNT) {
                    val row = list.adapter.getView(index, null, list)
                    row.measure(
                        View.MeasureSpec.makeMeasureSpec((240 * density).toInt(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec((1000 * density).toInt(), View.MeasureSpec.AT_MOST)
                    )
                    assertTrue(row.measuredWidth >= minimum)
                    assertTrue(row.measuredHeight >= minimum)
                    assertEquals(activity.resources.getStringArray(R.array.habit_color_names)[index], (row as TextView).text)
                    list.performItemClick(row, index, list.adapter.getItemId(index))
                    assertEquals(index, wheel.selectedIndex)
                    assertTrue(list.isItemChecked(index))
                    assertNull(picked)
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals(PaletteColor(PaletteColor.COUNT - 1), picked)
            }
        }
    }

    @Test
    fun listModeAndPendingSelectionSurviveRecreationWithCancelAndConfirm() {
        for (confirm in listOf(false, true)) {
            launchEditor().use { scenario ->
                scenario.onActivity { activity ->
                    activity.findViewById<View>(R.id.colorButton).performClick()
                    activity.supportFragmentManager.executePendingTransactions()
                    val dialog = picker(activity)
                    dialog.findViewById<View>(R.id.colorPickerModeToggle)!!.performClick()
                    val list = dialog.findViewById<ListView>(R.id.colorPickerList)!!
                    list.performItemClick(list.adapter.getView(51, null, list), 51, 51)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val dialog = picker(activity)
                    val list = dialog.findViewById<ListView>(R.id.colorPickerList)!!
                    assertEquals(View.VISIBLE, list.visibility)
                    assertTrue(list.isItemChecked(51))
                    assertEquals(PaletteColor.DEFAULT, activity.color)
                    dialog.getButton(if (confirm) AlertDialog.BUTTON_POSITIVE else AlertDialog.BUTTON_NEGATIVE).performClick()
                    assertEquals(if (confirm) PaletteColor(51) else PaletteColor.DEFAULT, activity.color)
                }
            }
        }
    }

    @Test
    fun editorColorButtonMeets48dpAtCompactWidth() {
        launchEditor().use { scenario ->
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                val root = activity.findViewById<View>(android.R.id.content)
                root.measure(
                    View.MeasureSpec.makeMeasureSpec((320 * density).toInt(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec((640 * density).toInt(), View.MeasureSpec.EXACTLY)
                )
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                val button = activity.findViewById<View>(R.id.colorButton)
                val minimum = ceil(48 * density).toInt()
                assertTrue(button.width >= minimum)
                assertTrue(button.height >= minimum)
            }
        }
    }

    private fun picker(activity: EditHabitActivity) =
        (activity.supportFragmentManager.findFragmentByTag("colorPicker") as ColorPickerDialog).requireDialog() as AlertDialog

    private fun launchEditor() =
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java))
}
