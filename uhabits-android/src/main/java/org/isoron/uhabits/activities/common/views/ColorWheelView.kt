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
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.RadioButton
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3-ring Donut Color Wheel containing:
 * - 12 radial hue sectors (30 degrees each)
 * - 3 concentric tone rings: Outer (Vibrant), Middle (Muted), Inner (Deep)
 * - Center neutral area with 4 swatches (Gray, Dark Gray, Slate, Charcoal)
 * Total: 40 colors.
 */
class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var colors: IntArray = IntArray(PaletteColor.COUNT) { Color.GRAY }
        set(value) {
            require(value.size == PaletteColor.COUNT)
            field = value
            invalidate()
        }

    var colorNames: Array<String> = resources.getStringArray(R.array.habit_color_names)
        set(value) {
            require(value.size == PaletteColor.COUNT && value.all { it.isNotBlank() })
            field = value
            accessibilityHelper.invalidateRoot()
        }

    var selectedIndex: Int = 0
        set(value) {
            if (value in colors.indices && field != value) {
                val previous = field
                field = value
                accessibilityHelper.invalidateVirtualView(previous)
                accessibilityHelper.invalidateVirtualView(value)
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

    private val accessibilityHelper = object : ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int = indexAt(x, y)

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            virtualViewIds.addAll(colors.indices)
        }

        override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
            node.contentDescription = colorNames[virtualViewId]
            node.className = RadioButton::class.java.name
            node.isCheckable = true
            node.isChecked = virtualViewId == selectedIndex
            node.isSelected = virtualViewId == selectedIndex
            node.isEnabled = isEnabled
            node.isClickable = true
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            node.setBoundsInParent(swatchBounds(virtualViewId))
        }

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            if (!isEnabled || virtualViewId !in colors.indices || action != AccessibilityNodeInfoCompat.ACTION_CLICK) {
                return false
            }
            select(virtualViewId)
            sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
            return true
        }

        override fun onVirtualViewKeyboardFocusChanged(virtualViewId: Int, hasFocus: Boolean) {
            invalidate()
        }
    }

    init {
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
    }

    fun select(index: Int) {
        require(index in colors.indices)
        if (selectedIndex == index) return
        selectedIndex = index
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onColorSelected?.invoke(index)
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        accessibilityHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        accessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
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
        if (colors.size < PaletteColor.COUNT) return

        val cx = width / 2f
        val cy = height / 2f
        val padding = 12 * density
        val outerRadius = (min(width, height) / 2f) - padding
        if (outerRadius <= 0) return

        val innerRadius = outerRadius * 0.32f
        val ringWidth = (outerRadius - innerRadius) / 4f
        val gap = 1.0f // angle gap in degrees

        // 1. Draw 12 hue sectors x 4 concentric rings (48 colors)
        var selectedPath: Path? = null
        var selectedColor = Color.BLACK
        var selectedCenterX = cx
        var selectedCenterY = cy

        for (hue in 0 until 12) {
            val startAngle = -90f + hue * 30f + gap / 2f
            val sweepAngle = 30f - gap

            for (ring in 0 until 4) {
                // Ring 0: Outermost (Deep)
                // Ring 1: Vibrant
                // Ring 2: Soft
                // Ring 3: Innermost (Light)
                val colorIndex = hue * 4 + ring
                val (rIn, rOut) = when (ring) {
                    0 -> Pair(innerRadius + 3 * ringWidth, outerRadius)
                    1 -> Pair(innerRadius + 2 * ringWidth, innerRadius + 3 * ringWidth)
                    2 -> Pair(innerRadius + ringWidth, innerRadius + 2 * ringWidth)
                    else -> Pair(innerRadius, innerRadius + ringWidth)
                }

                val path = buildArcPath(cx, cy, rIn, rOut, startAngle, sweepAngle)

                fillPaint.color = colors[colorIndex]
                canvas.drawPath(path, fillPaint)

                // Divider line between segments
                strokePaint.color = Color.argb(60, 255, 255, 255)
                strokePaint.strokeWidth = 1f * density
                canvas.drawPath(path, strokePaint)
                if (colorIndex == accessibilityHelper.keyboardFocusedVirtualViewId && colorIndex != selectedIndex) {
                    strokePaint.color = contrastingTextColor(colors[colorIndex])
                    strokePaint.strokeWidth = 2f * density
                    canvas.drawPath(path, strokePaint)
                }

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

        // 2. Draw Center Neutral Swatches (indices 48..51)
        val neutralRadius = innerRadius * 0.34f
        val neutralOffset = innerRadius * 0.46f
        val neutralCenters = arrayOf(
            Pair(cx - neutralOffset, cy - neutralOffset), // 48: Light Gray (top-left)
            Pair(cx + neutralOffset, cy - neutralOffset), // 49: Medium Gray (top-right)
            Pair(cx - neutralOffset, cy + neutralOffset), // 50: Slate (bottom-left)
            Pair(cx + neutralOffset, cy + neutralOffset)  // 51: Charcoal (bottom-right)
        )

        for (i in 0 until 4) {
            val colorIndex = 48 + i
            val (nx, ny) = neutralCenters[i]

            fillPaint.color = colors[colorIndex]
            canvas.drawCircle(nx, ny, neutralRadius, fillPaint)

            // Outline for neutral circle
            strokePaint.color = Color.argb(60, 0, 0, 0)
            strokePaint.strokeWidth = 1f * density
            canvas.drawCircle(nx, ny, neutralRadius, strokePaint)
            if (colorIndex == accessibilityHelper.keyboardFocusedVirtualViewId && colorIndex != selectedIndex) {
                strokePaint.color = contrastingTextColor(colors[colorIndex])
                strokePaint.strokeWidth = 2f * density
                canvas.drawCircle(nx, ny, neutralRadius, strokePaint)
            }

            if (colorIndex == selectedIndex) {
                selectedColor = colors[colorIndex]
                selectedCenterX = nx
                selectedCenterY = ny

                // Bold selection border around circle
                strokePaint.color = contrastingTextColor(selectedColor)
                strokePaint.strokeWidth = 3f * density
                canvas.drawCircle(nx, ny, neutralRadius, strokePaint)
            }
        }

        // 3. Highlight Selected Segment on the Wheel
        if (selectedPath != null) {
            strokePaint.color = contrastingTextColor(selectedColor)
            strokePaint.strokeWidth = 3.5f * density
            canvas.drawPath(selectedPath, strokePaint)
        }

        // 4. Draw Checkmark in Selected Swatch
        textPaint.color = contrastingTextColor(selectedColor)
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

    private fun swatchBounds(index: Int): Rect {
        val cx = width / 2f
        val cy = height / 2f
        val outerRadius = (min(width, height) / 2f - 12 * density).coerceAtLeast(1f)
        val innerRadius = outerRadius * 0.32f
        val bounds = RectF()
        if (index < 48) {
            val ringWidth = (outerRadius - innerRadius) / 4f
            val ring = index % 4
            val hue = index / 4
            val (rIn, rOut) = when (ring) {
                0 -> Pair(innerRadius + 3 * ringWidth, outerRadius)
                1 -> Pair(innerRadius + 2 * ringWidth, innerRadius + 3 * ringWidth)
                2 -> Pair(innerRadius + ringWidth, innerRadius + 2 * ringWidth)
                else -> Pair(innerRadius, innerRadius + ringWidth)
            }
            buildArcPath(cx, cy, rIn, rOut, -90f + hue * 30f + 0.5f, 29f)
                .computeBounds(bounds, true)
        } else {
            val neutralRadius = innerRadius * 0.34f
            val offset = innerRadius * 0.46f
            val nx = cx + if ((index - 48) % 2 == 0) -offset else offset
            val ny = cy + if ((index - 48) < 2) -offset else offset
            bounds.set(nx - neutralRadius, ny - neutralRadius, nx + neutralRadius, ny + neutralRadius)
        }
        return Rect().also { bounds.roundOut(it) }
    }

    private fun indexAt(x: Float, y: Float): Int {
        val outerRadius = min(width, height) / 2f - 12 * density
        if (outerRadius <= 0 || x < 0 || y < 0 || x >= width || y >= height) return ExploreByTouchHelper.INVALID_ID
        val innerRadius = outerRadius * 0.32f
        val ringWidth = (outerRadius - innerRadius) / 4f
        val dx = x - width / 2f
        val dy = y - height / 2f
        val dist = sqrt(dx * dx + dy * dy)
        return if (dist < innerRadius) {
            when {
                dx < 0 && dy < 0 -> 48
                dx >= 0 && dy < 0 -> 49
                dx < 0 && dy >= 0 -> 50
                else -> 51
            }
        } else if (dist <= outerRadius + 16 * density) {
            val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val angle = (degrees + 450f) % 360f
            val hue = (angle / 30f).toInt().coerceIn(0, 11)
            val ring = when {
                dist >= innerRadius + 3 * ringWidth -> 0
                dist >= innerRadius + 2 * ringWidth -> 1
                dist >= innerRadius + ringWidth -> 2
                else -> 3
            }
            hue * 4 + ring
        } else {
            ExploreByTouchHelper.INVALID_ID
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val tappedIndex = indexAt(event.x, event.y)
                if (tappedIndex == ExploreByTouchHelper.INVALID_ID) return super.onTouchEvent(event)
                select(tappedIndex)
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
