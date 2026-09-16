/*
 * Copyright (C) 2026 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list.views

import android.graphics.Paint
import android.view.View
import androidx.core.graphics.ColorUtils
import org.isoron.uhabits.R
import org.isoron.uhabits.core.utils.ColorContrast
import org.isoron.uhabits.utils.sres

fun View.todayTintPaint() = Paint().apply {
    color = sres.getColor(R.attr.contrast100)
    alpha = (255 * sres.getFloat(R.attr.todayHighlightAlpha)).toInt()
}

class TodayHighlight(private val view: View) {
    val paint = view.todayTintPaint()
    private val normalBackground = ColorUtils.compositeColors(paint.color, view.sres.getColor(R.attr.cardBgColor))
    private val selectedBackground = ColorUtils.compositeColors(paint.color, view.sres.getColor(R.attr.highlightedBackgroundColor))
    private val colors = mutableMapOf<Triple<Int, Int, Double>, Int>()

    val backgroundColor: Int
        get() = if (view.isSelected) selectedBackground else normalBackground

    fun foreground(color: Int, minimum: Double = 4.5): Int =
        colors.getOrPut(Triple(color, backgroundColor, minimum)) {
            // Leave room for the one-channel rounding difference between Skia and ColorUtils.
            ColorContrast.ensureContrast(color, backgroundColor, minimum + 0.1)
        }
}
