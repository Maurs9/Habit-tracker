package org.isoron.uhabits.core.utils

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ColorContrastTest {
    @Test
    fun highlightedPaletteAndGreysMeetContrastIncludingSelectedCards() {
        val themes = listOf(LightTheme(), DarkTheme(), PureBlackTheme())
        val tints = listOf(0x424242, 0xF5F5F5, 0xEEEEEE)
        val alphas = listOf(15, 40, 51)
        val selectedBackgrounds = listOf(0xF5F5F5, 0x424242, 0x000000)
        for ((index, theme) in themes.withIndex()) {
            for (base in listOf(rgb(theme.cardBackgroundColor), opaque(selectedBackgrounds[index]))) {
                val background = composite(tints[index], base, alphas[index])
                val foregrounds = (0 until PaletteColor.COUNT).map { rgb(theme.color(it)) } +
                    listOf(rgb(theme.mediumContrastTextColor), rgb(theme.inactiveMarkColor))
                for (foreground in foregrounds) {
                    val adjusted = ColorContrast.ensureContrast(foreground, background, 4.6)
                    assertAtLeast(adjusted, background, 4.6)
                    for (delta in -1..1) assertAtLeast(adjusted, shifted(background, delta), 4.5)
                    if (ratio(foreground, background) >= 4.6) assertEquals(foreground, adjusted)
                }
                val inactive = ColorContrast.ensureContrast(rgb(theme.inactiveMarkColor), background, 3.1)
                assertAtLeast(inactive, background, 3.1)
                for (delta in -1..1) assertAtLeast(inactive, shifted(background, delta), 3.0)
            }
        }
    }

    @Test
    fun darkTodayTintRemainsVisibleOnNormalSelectedAndHeaderBackgrounds() {
        val themes = listOf(DarkTheme(), PureBlackTheme())
        val tints = listOf(0xF5F5F5, 0xEEEEEE)
        val alphas = listOf(40, 51)
        val selectedBackgrounds = listOf(0x424242, 0x000000)
        for ((index, theme) in themes.withIndex()) {
            val backgrounds = listOf(
                rgb(theme.cardBackgroundColor),
                rgb(theme.headerBackgroundColor),
                opaque(selectedBackgrounds[index])
            )
            for (base in backgrounds) {
                val highlighted = composite(tints[index], base, alphas[index])
                assertAtLeast(highlighted, base, 1.4)
            }
        }
    }

    @Test
    fun unchangedColorsAreReturnedExactlyAndAdjustmentIsIdempotent() {
        assertEquals(opaque(0x123456), ColorContrast.ensureContrast(opaque(0x123456), opaque(0xFFFFFF), 4.5))
        for (background in listOf(0xFFFFFF, 0xEEEEEE, 0x777777, 0x303030, 0x000000)) {
            val adjusted = ColorContrast.ensureContrast(opaque(background), opaque(background), 4.5)
            assertAtLeast(adjusted, opaque(background), 4.5)
            assertEquals(adjusted, ColorContrast.ensureContrast(adjusted, opaque(background), 4.5))
        }
    }

    @Test
    fun ratiosUseWcagLuminanceRatherThanWeightedSrgb() {
        assertEquals(21.0, ColorContrast.ratio(opaque(0xFFFFFF), opaque(0x000000)), 0.000001)
        assertEquals(1.0, ColorContrast.ratio(opaque(0x303030), opaque(0x303030)), 0.000001)
        assertEquals(ratio(opaque(0xFF0000), opaque(0xFFFFFF)), ColorContrast.ratio(opaque(0xFF0000), opaque(0xFFFFFF)), 0.000001)
    }

    @Test
    fun invalidInputsAndUnreachableTargetsAreRejected() {
        assertFailsWith<IllegalArgumentException> { ColorContrast.ensureContrast(0, opaque(0), 4.5) }
        assertFailsWith<IllegalArgumentException> { ColorContrast.ensureContrast(opaque(0), 0, 4.5) }
        assertFailsWith<IllegalArgumentException> { ColorContrast.ensureContrast(opaque(0), opaque(0), Double.NaN) }
        assertFailsWith<IllegalArgumentException> { ColorContrast.ensureContrast(opaque(0), opaque(0), 0.0) }
        assertFailsWith<IllegalArgumentException> { ColorContrast.ensureContrast(opaque(0x777777), opaque(0x777777), 21.0) }
    }

    private fun assertAtLeast(foreground: Int, background: Int, minimum: Double) {
        assertTrue(
            ratio(foreground, background) >= minimum,
            "${foreground.toUInt().toString(16)} on ${background.toUInt().toString(16)}: ${ratio(foreground, background)} < $minimum"
        )
    }

    private fun opaque(rgb: Int) = rgb or (0xFF shl 24)

    private fun rgb(color: Color) = opaque(
        ((color.red * 255).toInt() shl 16) or
            ((color.green * 255).toInt() shl 8) or
            (color.blue * 255).toInt()
    )

    private fun composite(foreground: Int, background: Int, alpha: Int): Int {
        var result = opaque(0)
        for (shift in listOf(16, 8, 0)) {
            val channel = (((foreground shr shift) and 255) * alpha + ((background shr shift) and 255) * (255 - alpha)) / 255
            result = result or (channel shl shift)
        }
        return result
    }

    private fun shifted(color: Int, delta: Int): Int {
        var result = opaque(0)
        for (shift in listOf(16, 8, 0)) {
            result = result or ((((color shr shift) and 255) + delta).coerceIn(0, 255) shl shift)
        }
        return result
    }

    private fun ratio(foreground: Int, background: Int): Double {
        fun luminance(color: Int): Double {
            fun channel(shift: Int): Double {
                val value = ((color shr shift) and 255) / 255.0
                return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}
