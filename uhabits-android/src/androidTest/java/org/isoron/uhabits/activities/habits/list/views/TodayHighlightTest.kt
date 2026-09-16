package org.isoron.uhabits.activities.habits.list.views

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.utils.sres
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@MediumTest
class TodayHighlightTest : BaseViewTest() {
    @Test
    fun testEveryPaletteForegroundContrastsWithRenderedTodayCells() {
        val themes = listOf(
            R.style.AppBaseTheme to LightTheme(),
            R.style.AppBaseThemeDark to DarkTheme(),
            R.style.AppBaseThemeDark_PureBlack to PureBlackTheme()
        )
        for ((style, theme) in themes) {
            setTheme(style)
            val checkmark = CheckmarkButtonView(targetContext, prefs).apply {
                isToday = true
                value = Entry.YES_MANUAL
            }
            val number = NumberButtonView(targetContext, prefs).apply {
                isToday = true
                threshold = 0.5
                value = 1.0
                units = "km"
            }
            for (selected in listOf(false, true)) {
                for (index in 0 until PaletteColor.COUNT) {
                    val color = theme.color(index).toInt()
                    checkmark.color = color
                    number.color = color
                    for (button in listOf(checkmark, number)) {
                        prepareButton(button, selected)
                        val expected = TodayHighlight(button).foreground(color)
                        renderView(button).let {
                            assertTrue(ColorUtils.calculateContrast(expected, it.getPixel(1, 1)) >= 4.5)
                            assertContainsColor(it, expected)
                            it.recycle()
                        }
                    }
                }
            }
            checkmark.isToday = false
            number.isToday = false
            for (button in listOf(checkmark, number)) {
                prepareButton(button, false)
                renderView(button).let {
                    assertContainsColor(it, checkmark.color)
                    it.recycle()
                }
            }
        }
    }

    @Test
    fun testInactiveAndSecondaryForegroundsKeepContrastInEveryTheme() {
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            val button = NumberButtonView(targetContext, prefs).apply {
                isToday = true
                value = 0.25
                threshold = 0.5
                units = "km"
            }
            for (selected in listOf(false, true)) {
                prepareButton(button, selected)
                val highlight = TodayHighlight(button)
                renderView(button).let {
                    for (attr in listOf(R.attr.contrast40, R.attr.contrast60)) {
                        val color = highlight.foreground(button.sres.getColor(attr))
                        assertTrue(ColorUtils.calculateContrast(color, it.getPixel(1, 1)) >= 4.5)
                    }
                    assertContainsColor(it, highlight.foreground(button.sres.getColor(R.attr.contrast60)))
                    it.recycle()
                }
                val inactive = highlight.foreground(button.sres.getColor(R.attr.contrast40), 3.0)
                assertTrue(ColorUtils.calculateContrast(inactive, highlight.backgroundColor) >= 3.0)
            }
        }
    }

    @Test
    fun testButtonsShareTintAndClearItOnRebindInEveryTheme() {
        val themes = listOf(
            R.style.AppBaseTheme to 0.06f,
            R.style.AppBaseThemeDark to 0.16f,
            R.style.AppBaseThemeDark_PureBlack to 0.20f
        )
        for ((style, alpha) in themes) {
            setTheme(style)
            val checkmark = CheckmarkButtonView(targetContext, prefs)
            val number = NumberButtonView(targetContext, prefs)
            for (button in listOf(checkmark, number)) {
                val background = button.sres.getColor(R.attr.cardBgColor)
                button.setBackgroundColor(background)
                measureView(button, dpToPixels(48), dpToPixels(48))
                val paint = button.todayTintPaint()
                assertEquals((255 * alpha).toInt(), paint.alpha)
                assertEquals(button.sres.getColor(R.attr.contrast100) and 0xFFFFFF, paint.color and 0xFFFFFF)
                assertFalse(checkmark.isToday)
                assertFalse(number.isToday)
                renderView(button).let {
                    assertEquals(background, it.getPixel(1, 1))
                    it.recycle()
                }
                checkmark.isToday = true
                number.isToday = true
                renderView(button).let {
                    val expected = ColorUtils.compositeColors(paint.color, background)
                    assertCompositeColor(expected, it.getPixel(1, 1))
                    assertCompositeColor(expected, it.getPixel(it.width - 2, it.height - 2))
                    if (style != R.style.AppBaseTheme) {
                        assertTrue(ColorUtils.calculateContrast(it.getPixel(1, 1), background) >= 1.4)
                    }
                    it.recycle()
                }
                checkmark.isToday = false
                number.isToday = false
                renderView(button).let {
                    assertEquals(background, it.getPixel(1, 1))
                    it.recycle()
                }
            }
        }
    }

    private fun assertCompositeColor(expected: Int, actual: Int) {
        assertEquals(Color.alpha(expected), Color.alpha(actual))
        // Skia and ColorUtils can differ by one channel level when rounding alpha.
        assertTrue(abs(Color.red(expected) - Color.red(actual)) <= 1)
        assertTrue(abs(Color.green(expected) - Color.green(actual)) <= 1)
        assertTrue(abs(Color.blue(expected) - Color.blue(actual)) <= 1)
    }

    private fun prepareButton(button: View, selected: Boolean) {
        button.isSelected = selected
        val background = if (selected) R.attr.highlightedBackgroundColor else R.attr.cardBgColor
        button.setBackgroundColor(button.sres.getColor(background))
        measureView(button, dpToPixels(48), dpToPixels(48))
    }

    private fun assertContainsColor(bitmap: Bitmap, color: Int) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue("Rendered text must use the contrast-adjusted foreground", pixels.any { it == color })
    }
}
