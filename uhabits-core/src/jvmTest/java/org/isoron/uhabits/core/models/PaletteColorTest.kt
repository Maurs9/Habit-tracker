package org.isoron.uhabits.core.models

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PaletteColorTest {
    @Test
    fun keepsEveryThemeColorAndIndex() {
        val light = LightTheme()
        val dark = DarkTheme()
        val black = PureBlackTheme()
        val widget = WidgetTheme()

        for (index in 0 until PaletteColor.COUNT) {
            val color = PaletteColor(index)
            val expectedCsv = color.toCsvColor()
            val expectedLight = Color(expectedCsv.removePrefix("#").toInt(16))
            assertEquals(expectedLight, light.color(color))
            assertEquals(expectedLight, widget.color(color))
            assertNotEquals(Color.BLACK, light.color(color))
            assertNotEquals(Color.WHITE, dark.color(color))
            assertEquals(dark.color(color), black.color(color))
        }
    }

    @Test
    fun includesFortyColorsAcrossAllThemesAndExport() {
        assertEquals(40, PaletteColor.COUNT)
        val light = LightTheme()
        val dark = DarkTheme()

        assertEquals(PaletteColor.COUNT, (0 until PaletteColor.COUNT).map { light.color(it) }.toSet().size)
        assertEquals(PaletteColor.COUNT, (0 until PaletteColor.COUNT).map { dark.color(it) }.toSet().size)
    }

    @Test
    fun rejectsIndexesOutsideCuratedPalette() {
        assertFailsWith<IllegalArgumentException> { PaletteColor(-1) }
        assertFailsWith<IllegalArgumentException> { PaletteColor(PaletteColor.COUNT) }
    }
}
