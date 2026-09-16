package org.isoron.uhabits.core.models

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteContrastTest {
    @Test
    fun everySlotMeetsTextContrastInEveryThemeAndOnFills() {
        val light = LightTheme()
        val dark = DarkTheme()
        val black = PureBlackTheme()
        val widget = WidgetTheme()
        for (index in 0 until PaletteColor.COUNT) {
            assertContrast(light.color(index), Color(0xFAFAFA), 4.5, "light slot $index")
            assertContrast(Color.WHITE, light.color(index), 4.5, "toolbar slot $index")
            assertContrast(dark.color(index), Color(0x303030), 4.5, "dark slot $index")
            assertContrast(black.color(index), Color.BLACK, 4.5, "pure-black slot $index")
            assertContrast(widget.color(index), Color(0x303030), 4.5, "widget slot $index")
            assertContrast(Color(0x212121), widget.color(index), 4.5, "widget fill slot $index")
        }
    }

    @Test
    fun everyHueKeepsDeepDarkerAndMutedLessChromatic() {
        for (theme in listOf(LightTheme(), DarkTheme())) {
            for (hue in 0 until 12) {
                val vibrant = theme.color(hue * 3)
                val muted = theme.color(hue * 3 + 1)
                val deep = theme.color(hue * 3 + 2)
                val label = "${theme.javaClass.simpleName} hue $hue"
                assertTrue(luminance(deep) < luminance(vibrant), "$label: Deep must be darker than Vibrant")
                assertTrue(chroma(muted) < chroma(vibrant), "$label: Muted must have lower OKLab chroma")
            }
        }
    }

    @Test
    fun textGreysAndInactiveMarksMeetContrast() {
        for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme(), WidgetTheme())) {
            val card = cardBackground(theme)
            val label = theme.javaClass.simpleName
            assertContrast(theme.mediumContrastTextColor, card, 4.5, "$label medium text")
            assertContrast(theme.headerTextColor, theme.headerBackgroundColor, 4.5, "$label header text")
            assertContrast(theme.inactiveMarkColor, card, 3.0, "$label inactive marks")
        }
    }

    @Test
    fun themeSurfacesMatchTheirAndroidCounterparts() {
        val dark = DarkTheme()
        assertEquals(Color(0x303030), dark.itemBackgroundColor)
        assertEquals(Color(0x101010), dark.toolbarBackgroundColor)
        assertEquals(Color.BLACK, dark.statusBarBackgroundColor)
        assertEquals(Color(0x424242), dark.headerBorderColor)
        val black = PureBlackTheme()
        assertEquals(Color.BLACK, black.cardBackgroundColor)
        assertEquals(Color.BLACK, black.headerBackgroundColor)
        assertEquals(Color.BLACK, black.itemBackgroundColor)
        assertEquals(Color.BLACK, black.toolbarBackgroundColor)
    }

    private fun cardBackground(theme: Theme) =
        if (theme is WidgetTheme) Color(0x303030) else theme.cardBackgroundColor

    private fun assertContrast(foreground: Color, background: Color, minimum: Double, label: String) {
        // Composite translucent widget text onto its actual Android card before linearizing.
        val opaque = Color(
            foreground.red * foreground.alpha + background.red * (1 - foreground.alpha),
            foreground.green * foreground.alpha + background.green * (1 - foreground.alpha),
            foreground.blue * foreground.alpha + background.blue * (1 - foreground.alpha),
            1.0
        )
        val a = luminance(opaque)
        val b = luminance(background)
        val ratio = (max(a, b) + 0.05) / (min(a, b) + 0.05)
        assertTrue(ratio >= minimum, "$label: contrast $ratio must be >= $minimum")
    }

    private fun linear(channel: Double) =
        if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color) =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun chroma(color: Color): Double {
        // OKLab separates perceptual chroma from the lightness change between tone rings.
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b).pow(1.0 / 3)
        val m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b).pow(1.0 / 3)
        val s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b).pow(1.0 / 3)
        return hypot(
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        )
    }
}
