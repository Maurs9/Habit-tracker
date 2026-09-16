/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.activities.common.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3-ring Donut Color Wheel containing:
 * - 12 radial hue sectors (30 degrees each)
 * - 3 concentric tone rings: Outer (Vibrant), Middle (Soft), Inner (Deep)
 * - Center neutral area with 4 swatches (Light Gray, Gray, Dark Slate, Charcoal)
 * Total: 40 colors.
 */
class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var colors: IntArray = IntArray(40) { Color.GRAY }
        set(value) {
            field = value
            invalidate()
        }

    var selectedIndex: Int = 0
        set(value) {
            if (value in 0 until 40 && field != value) {
                field = value
                invalidate()
            }
        }

    var onColorSelected: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = (280 * density).toInt()
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredSize, widthSize)
            else -> desiredSize
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredSize, heightSize)
            else -> desiredSize
        }
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (colors.size < 40) return

        val cx = width / 2f
        val cy = height / 2f
        val padding = 12 * density
        val outerRadius = (min(width, height) / 2f) - padding
        if (outerRadius <= 0) return

        val innerRadius = outerRadius * 0.38f
        val ringWidth = (outerRadius - innerRadius) / 3f
        val gap = 1.2f // angle gap in degrees

        // 1. Draw 12 hue sectors x 3 concentric rings (36 colors)
        var selectedPath: Path? = null
        var selectedColor = Color.BLACK
        var selectedCenterX = cx
        var selectedCenterY = cy

        for (hue in 0 until 12) {
            val startAngle = -90f + hue * 30f + gap / 2f
            val sweepAngle = 30f - gap

            for (ring in 0 until 3) {
                // Ring 0: Outer (Vibrant) -> palette index = hue * 3 + 0
                // Ring 1: Middle (Soft)    -> palette index = hue * 3 + 1
                // Ring 2: Inner (Deep)     -> palette index = hue * 3 + 2
                val colorIndex = hue * 3 + ring
                val (rIn, rOut) = when (ring) {
                    0 -> Pair(innerRadius + 2 * ringWidth, outerRadius) // Outer
                    1 -> Pair(innerRadius + ringWidth, innerRadius + 2 * ringWidth) // Middle
                    else -> Pair(innerRadius, innerRadius + ringWidth) // Inner
                }

                val path = buildArcPath(cx, cy, rIn, rOut, startAngle, sweepAngle)

                fillPaint.color = colors[colorIndex]
                canvas.drawPath(path, fillPaint)

                // Divider line between segments
                strokePaint.color = Color.argb(40, 255, 255, 255)
                strokePaint.strokeWidth = 1f * density
                canvas.drawPath(path, strokePaint)

                if (colorIndex == selectedIndex) {
                    selectedPath = path
                    selectedColor = colors[colorIndex]
                    val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                    val midR = (rIn + rOut) / 2f
                    selectedCenterX = (cx + midR * cos(midAngleRad)).toFloat()
                    selectedCenterY = (cy + midR * sin(midAngleRad)).toFloat()
                }
            }
        }

        // 2. Draw Center Neutral Swatches (indices 36..39)
        val neutralRadius = innerRadius * 0.35f
        val neutralOffset = innerRadius * 0.48f
        val neutralCenters = arrayOf(
            Pair(cx - neutralOffset, cy - neutralOffset), // 36: Light Gray (top-left)
            Pair(cx + neutralOffset, cy - neutralOffset), // 37: Gray (top-right)
            Pair(cx - neutralOffset, cy + neutralOffset), // 38: Dark Slate (bottom-left)
            Pair(cx + neutralOffset, cy + neutralOffset)  // 39: Charcoal (bottom-right)
        )

        for (i in 0 until 4) {
            val colorIndex = 36 + i
            val (nx, ny) = neutralCenters[i]

            fillPaint.color = colors[colorIndex]
            canvas.drawCircle(nx, ny, neutralRadius, fillPaint)

            // Outline for neutral circle
            strokePaint.color = Color.argb(60, 0, 0, 0)
            strokePaint.strokeWidth = 1f * density
            canvas.drawCircle(nx, ny, neutralRadius, strokePaint)

            if (colorIndex == selectedIndex) {
                selectedColor = colors[colorIndex]
                selectedCenterX = nx
                selectedCenterY = ny

                // Bold selection border around circle
                val isDark = ColorUtils.calculateLuminance(selectedColor) < 0.35
                strokePaint.color = if (isDark) Color.WHITE else Color.BLACK
                strokePaint.strokeWidth = 3f * density
                canvas.drawCircle(nx, ny, neutralRadius, strokePaint)
            }
        }

        // 3. Highlight Selected Segment on the Wheel
        if (selectedPath != null) {
            val isDark = ColorUtils.calculateLuminance(selectedColor) < 0.35
            strokePaint.color = if (isDark) Color.WHITE else Color.BLACK
            strokePaint.strokeWidth = 3.5f * density
            canvas.drawPath(selectedPath, strokePaint)
        }

        // 4. Draw Checkmark in Selected Swatch
        val isDark = ColorUtils.calculateLuminance(selectedColor) < 0.35
        textPaint.color = if (isDark) Color.WHITE else Color.BLACK
        textPaint.textSize = 14 * density
        val textY = selectedCenterY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("✓", selectedCenterX, textY, textPaint)
    }

    private fun buildArcPath(
        cx: Float,
        cy: Float,
        rIn: Float,
        rOut: Float,
        startAngle: Float,
        sweepAngle: Float
    ): Path {
        val path = Path()
        val outerRect = RectF(cx - rOut, cy - rOut, cx + rOut, cy + rOut)
        val innerRect = RectF(cx - rIn, cy - rIn, cx + rIn, cy + rIn)

        path.arcTo(outerRect, startAngle, sweepAngle)
        val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble())
        path.lineTo(
            (cx + rIn * cos(endAngleRad)).toFloat(),
            (cy + rIn * sin(endAngleRad)).toFloat()
        )
        path.arcTo(innerRect, startAngle + sweepAngle, -sweepAngle)
        path.close()
        return path
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val cx = width / 2f
                val cy = height / 2f
                val padding = 12 * density
                val outerRadius = (min(width, height) / 2f) - padding
                val innerRadius = outerRadius * 0.38f
                val ringWidth = (outerRadius - innerRadius) / 3f

                val dx = event.x - cx
                val dy = event.y - cy
                val dist = sqrt(dx * dx + dy * dy)

                val tappedIndex = if (dist < innerRadius) {
                    // Touch in center neutral hub
                    when {
                        dx < 0 && dy < 0 -> 36 // Light Gray
                        dx >= 0 && dy < 0 -> 37 // Gray
                        dx < 0 && dy >= 0 -> 38 // Dark Slate
                        else -> 39             // Charcoal
                    }
                } else if (dist <= outerRadius + 16 * density) {
                    // Touch in wheel
                    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    var normAngle = deg + 90f
                    if (normAngle < 0f) normAngle += 360f
                    if (normAngle >= 360f) normAngle -= 360f

                    val hue = (normAngle / 30f).toInt().coerceIn(0, 11)
                    val ring = when {
                        dist >= innerRadius + 2 * ringWidth -> 0 // Outer (Vibrant)
                        dist >= innerRadius + ringWidth -> 1     // Middle (Soft)
                        else -> 2                                // Inner (Deep)
                    }
                    hue * 3 + ring
                } else {
                    return super.onTouchEvent(event)
                }

                if (tappedIndex != selectedIndex) {
                    selectedIndex = tappedIndex
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onColorSelected?.invoke(selectedIndex)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
