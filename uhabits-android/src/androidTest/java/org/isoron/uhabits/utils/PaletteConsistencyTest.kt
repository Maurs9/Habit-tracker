package org.isoron.uhabits.utils

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaletteConsistencyTest : BaseAndroidTest() {
    @Test
    fun testAllCuratedColorsHaveNamesAndMatchExport() {
        val names = targetContext.resources.getStringArray(R.array.habit_color_names)
        assertEquals(40, names.size)
        for (index in 0 until PaletteColor.COUNT) {
            val color = PaletteColor(index)
            assertEquals(Color.parseColor(color.toCsvColor()), color.toFixedAndroidColor())
            assertTrue(names[index].isNotBlank())
        }
    }
}
