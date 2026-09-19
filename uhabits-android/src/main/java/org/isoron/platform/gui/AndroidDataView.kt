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

package org.isoron.platform.gui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Scroller
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.refreshChartAccessibility
import kotlin.math.abs
import kotlin.math.max

/**
 * An AndroidView that implements scrolling.
 */
class AndroidDataView(
    context: Context,
    attrs: AttributeSet? = null
) : AndroidView<DataView>(context, attrs),
    GestureDetector.OnGestureListener,
    ValueAnimator.AnimatorUpdateListener {

    private val detector = GestureDetector(context, this)
    private val scroller = Scroller(context, null, true)
    private val scrollAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        addUpdateListener(this@AndroidDataView)
    }

    var maxDataOffset: Int = 10000
    val dataOffset: Int get() = view?.dataOffset ?: 0

    init {
        isFocusable = true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isScrollable = maxDataOffset > 0
        if (dataOffset < maxDataOffset) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                    context.getString(R.string.chart_older)
                )
            )
        }
        if (dataOffset > 0) {
            info.addAction(
                AccessibilityNodeInfo.AccessibilityAction(
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
                    context.getString(R.string.chart_newer)
                )
            )
        }
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.isScrollable = maxDataOffset > 0
        event.fromIndex = dataOffset
        event.toIndex = dataOffset
        event.itemCount = maxDataOffset + 1
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = when (action) {
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> scrollByColumns(1)
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> scrollByColumns(-1)
        else -> super.performAccessibilityAction(action, arguments)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> scrollByColumns(1)
        KeyEvent.KEYCODE_DPAD_RIGHT -> scrollByColumns(-1)
        else -> super.onKeyDown(keyCode, event)
    }

    fun scrollByColumns(delta: Int): Boolean {
        if (!isEnabled) return false
        val v = view ?: return false
        val target = (v.dataOffset.toLong() + delta).coerceIn(0, maxDataOffset.toLong()).toInt()
        if (target == v.dataOffset) return false
        scrollAnimator.cancel()
        scroller.forceFinished(true)
        val columnWidth = (v.dataColumnWidth * canvas.innerDensity).toInt().coerceAtLeast(1)
        scroller.startScroll(0, 0, target * columnWidth, 0, 0)
        scroller.computeScrollOffset()
        v.dataOffset = target
        refreshChartAccessibility()
        invalidate()
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        contentDescription?.let { announceForAccessibility(it) }
        return true
    }

    override fun onTouchEvent(event: MotionEvent) = detector.onTouchEvent(event)
    override fun onDown(e: MotionEvent) = true
    override fun onShowPress(e: MotionEvent) = Unit

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return handleClick(e, true)
    }

    override fun onLongPress(e: MotionEvent) {
        handleClick(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        dx: Float,
        dy: Float
    ): Boolean {
        if (abs(dx) > abs(dy)) {
            val parent = parent
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        scroller.startScroll(
            scroller.currX,
            scroller.currY,
            -dx.toInt(),
            dy.toInt(),
            0
        )
        scroller.computeScrollOffset()
        clampScroller()
        updateDataOffset()
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        scroller.fling(
            scroller.currX,
            scroller.currY,
            velocityX.toInt() / 2,
            0,
            0,
            maxScrollX(),
            0,
            0
        )
        invalidate()
        scrollAnimator.duration = scroller.duration.toLong()
        scrollAnimator.start()
        return false
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        if (!scroller.isFinished) {
            scroller.computeScrollOffset()
            updateDataOffset()
        } else {
            scrollAnimator.cancel()
        }
    }

    fun resetDataOffset() {
        scroller.finalX = 0
        scroller.computeScrollOffset()
        updateDataOffset()
    }

    private fun maxScrollX(): Int {
        val v = view ?: return Integer.MAX_VALUE
        val columnWidth = (v.dataColumnWidth * canvas.innerDensity).toInt().coerceAtLeast(1)
        return (maxDataOffset.toLong() * columnWidth).coerceAtMost(Integer.MAX_VALUE.toLong()).toInt()
    }

    private fun clampScroller() {
        val clamped = scroller.currX.coerceIn(0, maxScrollX())
        if (clamped != scroller.currX) {
            scroller.forceFinished(true)
            scroller.startScroll(clamped, scroller.currY, 0, 0, 0)
            scroller.computeScrollOffset()
        }
    }

    private fun updateDataOffset() {
        view?.let { v ->
            var newDataOffset: Int =
                scroller.currX / (v.dataColumnWidth * canvas.innerDensity).toInt().coerceAtLeast(1)
            newDataOffset = max(0, newDataOffset).coerceAtMost(maxDataOffset)
            if (newDataOffset != v.dataOffset) {
                v.dataOffset = newDataOffset
                refreshChartAccessibility()
                postInvalidate()
            }
        }
    }

    private fun handleClick(e: MotionEvent, isSingleTap: Boolean = false): Boolean {
        val x: Float
        val y: Float
        try {
            val pointerId = e.getPointerId(0)
            x = e.getX(pointerId)
            y = e.getY(pointerId)
        } catch (ex: RuntimeException) {
            // Android often throws IllegalArgumentException here. Apparently,
            // the pointer id may become invalid shortly after calling
            // e.getPointerId.
            return false
        }
        if (isSingleTap) {
            view?.onClick(x / canvas.innerDensity, y / canvas.innerDensity)
        } else {
            view?.onLongClick(x / canvas.innerDensity, y / canvas.innerDensity)
        }
        return true
    }
}
