package org.isoron.uhabits.core.utils

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object ColorContrast {
    fun ratio(foreground: Int, background: Int): Double {
        require(foreground ushr 24 == 255 && background ushr 24 == 255) { "Contrast requires opaque colors" }
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    fun ensureContrast(foreground: Int, background: Int, minimum: Double): Int {
        require(minimum in 1.0..21.0) { "Contrast target must be between 1 and 21" }
        if (ratio(foreground, background) >= minimum) return foreground
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val endpoint = if (ratio(black, background) >= ratio(white, background)) black else white
        require(ratio(endpoint, background) >= minimum) { "Contrast target cannot be reached on this background" }

        var lower = 0.0
        var upper = 1.0
        var result = endpoint
        repeat(12) {
            val weight = (lower + upper) / 2
            val candidate = blend(foreground, endpoint, weight)
            if (ratio(candidate, background) >= minimum) {
                upper = weight
                result = candidate
            } else {
                lower = weight
            }
        }
        return result
    }

    private fun luminance(color: Int): Double {
        fun linear(shift: Int): Double {
            val channel = ((color shr shift) and 255) / 255.0
            return if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }

    private fun blend(from: Int, to: Int, weight: Double): Int {
        fun channel(shift: Int): Int {
            val start = (from shr shift) and 255
            val end = (to shr shift) and 255
            return (start + (end - start) * weight).roundToInt() shl shift
        }
        return (0xFF shl 24) or channel(16) or channel(8) or channel(0)
    }
}
