package org.isoron.uhabits.activities.intro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.isoron.uhabits.utils.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@RunWith(AndroidJUnit4::class)
@SmallTest
class OnboardingContrastTest {
    @Test
    fun everyOnboardingBackgroundHasReadableBodyText() {
        for (background in intArrayOf(0xFF194673.toInt(), 0xFFFFA726.toInt(), 0xFF9575CD.toInt())) {
            val foreground = ColorUtils.contrastingTextColor(background)
            val lighter = max(luminance(background), luminance(foreground))
            val darker = min(luminance(background), luminance(foreground))
            assertTrue((lighter + 0.05) / (darker + 0.05) >= 4.5)
        }
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((color ushr shift) and 255) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
    }
}
