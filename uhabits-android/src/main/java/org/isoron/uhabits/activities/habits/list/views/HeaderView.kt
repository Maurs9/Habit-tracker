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

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Parcelable
import android.text.TextPaint
import android.view.View.MeasureSpec.EXACTLY
import android.view.accessibility.AccessibilityNodeInfo
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ScrollableChart
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.isRTL
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.toMeasureSpec
import java.text.DateFormat
import java.util.GregorianCalendar
import java.util.TimeZone

class HeaderView(
    context: Context,
    val prefs: Preferences,
    val midnightTimer: MidnightTimer
) : ScrollableChart(context),
    Preferences.Listener,
    MidnightTimer.MidnightListener {

    private var drawer = Drawer()

    var buttonCount: Int = 0
        set(value) {
            field = value
            updateAccessibilityDescription()
            requestLayout()
        }

    init {
        setScrollerBucketSize(dim(R.dimen.checkmarkWidth).toInt())
        setBackgroundColor(sres.getColor(R.attr.headerBackgroundColor))
        elevation = dp(2.0f)
        updateAccessibilityDescription()
    }

    override fun atMidnight() {
        post {
            updateAccessibilityDescription()
            invalidate()
        }
    }

    override fun onCheckmarkSequenceChanged() {
        updateScrollDirection()
        postInvalidate()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        if (isAttachedToWindow) updateScrollDirection()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateScrollDirection()
        updateAccessibilityDescription()
        prefs.addListener(this)
        midnightTimer.addListener(this)
    }

    override fun onDetachedFromWindow() {
        midnightTimer.removeListener(this)
        prefs.removeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = dim(R.dimen.checkmarkHeight)
        setMeasuredDimension(widthMeasureSpec, height.toMeasureSpec(EXACTLY))
    }

    override fun onDataOffsetChanged() {
        updateAccessibilityDescription()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        updateAccessibilityDescription()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        val actions = listOf(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD to R.string.habit_list_earlier_dates,
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD to R.string.habit_list_later_dates
        )
        for ((action, label) in actions) {
            if (info.actionList.any { it.id == action }) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction(action, context.getString(label)))
            }
        }
    }

    private fun updateAccessibilityDescription() {
        if (buttonCount == 0) {
            contentDescription = context.getString(R.string.habit_list_dates)
            return
        }
        val newest = DateUtils.getTodayWithOffset().minus(dataOffset)
        val oldest = newest.minus(buttonCount - 1)
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM, resources.configuration.locales[0]).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        contentDescription = context.getString(
            R.string.habit_list_date_range,
            format.format(oldest.toJavaDate()),
            format.format(newest.toJavaDate())
        )
    }

    private fun updateScrollDirection() {
        var direction = -1
        if (prefs.isCheckmarkSequenceReversed) direction *= -1
        if (isRTL()) direction *= -1
        setScrollDirection(direction)
    }

    private inner class Drawer {
        private val rect = RectF()
        private val regularTextColor = sres.getColor(R.attr.contrast60)
        private val todayTextColor = sres.getColor(R.attr.contrast100)
        private val todayPaint = todayTintPaint()
        private val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = dim(R.dimen.tinyTextSize)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            color = regularTextColor
        }

        fun draw(canvas: Canvas) {
            val day = DateUtils.getStartOfTodayCalendarWithOffset()
            val width = dim(R.dimen.checkmarkWidth)
            val height = dim(R.dimen.checkmarkHeight)
            val isReversed = prefs.isCheckmarkSequenceReversed

            day.add(GregorianCalendar.DAY_OF_MONTH, -dataOffset)
            val em = paint.measureText("m")

            repeat(buttonCount) { index ->
                rect.set(0f, 0f, width, height)
                rect.offset(canvas.width.toFloat() - dp(3.0f), 0f)

                if (isReversed) {
                    rect.offset(-(index + 1) * width, 0f)
                } else {
                    rect.offset((index - buttonCount) * width, 0f)
                }

                if (isRTL()) {
                    rect.set(
                        canvas.width - rect.right,
                        rect.top,
                        canvas.width - rect.left,
                        rect.bottom
                    )
                }

                val isToday = index == 0 && dataOffset == 0
                if (isToday) canvas.drawRect(rect, todayPaint)
                paint.color = if (isToday) todayTextColor else regularTextColor
                val y1 = rect.centerY() - 0.25 * em
                val y2 = rect.centerY() + 1.25 * em
                val lines = DateUtils.formatHeaderDate(day).uppercase().split("\n")
                canvas.drawText(lines[0], rect.centerX(), y1.toFloat(), paint)
                canvas.drawText(lines[1], rect.centerX(), y2.toFloat(), paint)
                day.add(GregorianCalendar.DAY_OF_MONTH, -1)
            }
        }
    }
}
