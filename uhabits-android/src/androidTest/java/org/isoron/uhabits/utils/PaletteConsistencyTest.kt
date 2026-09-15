package org.isoron.uhabits.utils

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaletteConsistencyTest : BaseAndroidTest() {
    @Test
    fun testAllCuratedColorsAreAvailableInResourcesThemesAndExport() {
        val light = targetContext.resources.getIntArray(R.array.lightPalette)
        val dark = targetContext.resources.getIntArray(R.array.darkPalette)
        val widget = targetContext.resources.getIntArray(R.array.transparentWidgetPalette)
        val names = targetContext.resources.getStringArray(R.array.habit_color_names)
        for (array in listOf(light, dark, widget)) assertEquals(PaletteColor.COUNT, array.size)
        assertEquals(PaletteColor.COUNT, names.size)
        for (index in 0 until PaletteColor.COUNT) {
            val color = PaletteColor(index)
            assertEquals(LightTheme().color(color).toInt(), light[index])
            assertEquals(DarkTheme().color(color).toInt(), dark[index])
            assertEquals(Color.parseColor(color.toCsvColor()), color.toFixedAndroidColor())
            assertTrue(names[index].isNotBlank())
            if (index >= 20) assertEquals(light[index], widget[index])
        }
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            val palette = StyledResources(targetContext).getPalette()
            for (index in palette.indices) assertEquals(PaletteColor(index), palette[index].toPaletteColor(targetContext))
        }
    }
}
