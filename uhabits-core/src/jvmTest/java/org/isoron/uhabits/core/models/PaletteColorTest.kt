package org.isoron.uhabits.core.models

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PaletteColorTest {
    @Test
    fun keepsEveryOriginalThemeColorAndIndex() {
        val light = listOf(
            0xD32F2F, 0xE64A19, 0xF57C00, 0xFF8F00, 0xF9A825,
            0xAFB42B, 0x7CB342, 0x388E3C, 0x00897B, 0x00ACC1,
            0x039BE5, 0x1976D2, 0x303F9F, 0x5E35B1, 0x8E24AA,
            0xD81B60, 0x5D4037, 0x424242, 0x757575, 0x9E9E9E
        )
        val dark = listOf(
            0xEF9A9A, 0xFFAB91, 0xFFCC80, 0xFFECB3, 0xFFF59D,
            0xE6EE9C, 0xC5E1A5, 0x69F0AE, 0x80CBC4, 0x80DEEA,
            0x81D4FA, 0x64B5F6, 0x9FA8DA, 0xB39DDB, 0xCE93D8,
            0xF48FB1, 0xBCAAA4, 0xF5F5F5, 0xE0E0E0, 0x9E9E9E
        )
        for (index in 0 until 20) {
            assertEquals(Color(light[index]), LightTheme().color(index))
            assertEquals(Color(dark[index]), DarkTheme().color(index))
            val widget = when (index) {
                12 -> 0x6275F0
                17 -> 0x757575
                else -> light[index]
            }
            assertEquals(Color(widget), WidgetTheme().color(index))
        }
    }

    @Test
    fun keepsEveryOriginalExportColorAndIndex() {
        val original = listOf(
            "#D32F2F", "#E64A19", "#F57C00", "#FF8F00", "#F9A825",
            "#AFB42B", "#7CB342", "#388E3C", "#00897B", "#00ACC1",
            "#039BE5", "#1976D2", "#303F9F", "#5E35B1", "#8E24AA",
            "#D81B60", "#5D4037", "#303030", "#757575", "#aaaaaa"
        )
        original.forEachIndexed { index, color ->
            assertEquals(color, PaletteColor(index).toCsvColor())
        }
    }

    @Test
    fun includesFortyColorsAcrossAllThemesAndExport() {
        assertEquals(40, PaletteColor.COUNT)
        val light = LightTheme()
        val dark = DarkTheme()
        val black = PureBlackTheme()
        val widget = WidgetTheme()
        for (index in 0 until PaletteColor.COUNT) {
            val color = PaletteColor(index)
            assertNotEquals(Color.BLACK, light.color(color))
            assertNotEquals(Color.WHITE, dark.color(color))
            assertNotEquals(Color.BLACK, widget.color(color))
            assertEquals(dark.color(color), black.color(color))
            if (index >= 20) {
                assertEquals(Color(color.toCsvColor().removePrefix("#").toInt(16)), light.color(color))
                assertEquals(light.color(color), widget.color(color))
            }
        }
        assertEquals(PaletteColor.COUNT, (0 until PaletteColor.COUNT).map { light.color(it) }.toSet().size)
        assertEquals(PaletteColor.COUNT, (0 until PaletteColor.COUNT).map { dark.color(it) }.toSet().size)
    }

    @Test
    fun rejectsIndexesOutsideCuratedPalette() {
        assertFailsWith<IllegalArgumentException> { PaletteColor(-1) }
        assertFailsWith<IllegalArgumentException> { PaletteColor(PaletteColor.COUNT) }
    }

    @Test
    fun appendedColorsMeetTextContrastOnThemeSurfaces() {
        fun luminance(color: Color): Double {
            fun linear(value: Double) =
                if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
            return linear(color.red) * 0.2126 + linear(color.green) * 0.7152 + linear(color.blue) * 0.0722
        }
        for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme())) {
            for (index in 20 until PaletteColor.COUNT) {
                for (background in listOf(theme.appBackgroundColor, theme.cardBackgroundColor)) {
                    val levels = listOf(luminance(theme.color(index)), luminance(background)).sorted()
                    val ratio = (levels[1] + 0.05) / (levels[0] + 0.05)
                    assertTrue(ratio >= 4.5, "${theme.javaClass.simpleName} color $index: $ratio")
                }
            }
        }
    }
}
