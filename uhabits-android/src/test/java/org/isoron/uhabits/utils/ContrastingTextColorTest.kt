package org.isoron.uhabits.utils

import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ContrastingTextColorTest {
    @Test
    fun everyThemePaletteFillGetsNormalTextContrast() {
        for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme())) {
            for (index in 0 until PaletteColor.COUNT) {
                val color = theme.color(index)
                val luminance = 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
                assertNormalTextContrast(luminance)
            }
        }
    }

    @Test
    fun foregroundChoiceMeetsNormalTextContrastAcrossLuminanceRange() {
        for (step in 0..10000) assertNormalTextContrast(step / 10000.0)
    }

    private fun assertNormalTextContrast(luminance: Double) {
        val foreground = ColorUtils.contrastingTextColorForLuminance(luminance)
        val contrast = if (foreground == -0x1000000) (luminance + 0.05) / 0.05 else 1.05 / (luminance + 0.05)
        assertTrue("Luminance $luminance has contrast $contrast", contrast >= 4.5)
    }

    private fun linear(channel: Double) =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
