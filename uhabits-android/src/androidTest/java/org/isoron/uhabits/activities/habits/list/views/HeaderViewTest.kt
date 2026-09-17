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

import android.content.res.Configuration
import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.text.DateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@MediumTest
class HeaderViewTest : BaseViewTest() {
    private lateinit var view: HeaderView

    @Before
    override fun setUp() {
        super.setUp()
        prefs = mock()
        view = HeaderView(targetContext, prefs, mock())
        view.buttonCount = 5
        measureView(view, dpToPixels(600), dpToPixels(48))
    }

    @Test
    @Throws(Exception::class)
    fun testRender() {
        whenever(prefs.isCheckmarkSequenceReversed).thenReturn(false)
        assertRenders(view, PATH + "render.png")
        verify(prefs).isCheckmarkSequenceReversed
        verifyNoMoreInteractions(prefs)
    }

    @Test
    @Throws(Exception::class)
    fun testRender_reverse() {
        doReturn(true).whenever(prefs).isCheckmarkSequenceReversed
        assertRenders(view, PATH + "render_reverse.png")
        verify(prefs).isCheckmarkSequenceReversed
        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun testTodayTintFollowsOffsetReversalAndRtlInEveryTheme() {
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            view = HeaderView(targetContext, prefs, mock()).apply { buttonCount = 5 }
            measureView(view, dpToPixels(600), dpToPixels(48))
            val cellWidth = view.dim(R.dimen.checkmarkWidth).toInt()
            val inset = view.dp(3f).toInt()
            val background = view.sres.getColor(R.attr.headerBackgroundColor)
            val tint = ColorUtils.compositeColors(view.todayTintPaint().color, background)
            for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
                view.layoutDirection = direction
                for (reversed in listOf(false, true)) {
                    whenever(prefs.isCheckmarkSequenceReversed).thenReturn(reversed)
                    view.reset()
                    val left = view.width - inset - cellWidth * if (reversed) 1 else view.buttonCount
                    val todayLeft = if (direction == View.LAYOUT_DIRECTION_RTL) view.width - left - cellWidth else left
                    renderView(view).let { bitmap ->
                        for (x in 0 until bitmap.width) {
                            val expected = if (x in todayLeft until todayLeft + cellWidth) tint else background
                            val actual = bitmap.getPixel(x, 1)
                            assertEquals(Color.alpha(expected), Color.alpha(actual))
                            assertTrue(abs(Color.red(expected) - Color.red(actual)) <= 1)
                            assertTrue(abs(Color.green(expected) - Color.green(actual)) <= 1)
                            assertTrue(abs(Color.blue(expected) - Color.blue(actual)) <= 1)
                        }
                        bitmap.recycle()
                    }
                    view.setScrollDirection(1)
                    val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
                    view.onScroll(null, event, -3f * cellWidth, 0f)
                    event.recycle()
                    assertEquals(3, view.dataOffset)
                    renderView(view).let { bitmap ->
                        for (x in 0 until bitmap.width) assertEquals(background, bitmap.getPixel(x, 1))
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun testAccessibleDateActionsHaveLabelsAndRespectBothBounds() {
        view.setMaxDataOffset(2)
        assertEquals(
            mapOf(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD to targetContext.getString(R.string.habit_list_earlier_dates)),
            scrollActions()
        )
        assertTrue(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertEquals(1, view.dataOffset)
        assertEquals(2, scrollActions().size)
        assertTrue(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertEquals(2, view.dataOffset)
        assertEquals(
            mapOf(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD to targetContext.getString(R.string.habit_list_later_dates)),
            scrollActions()
        )
        assertFalse(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertTrue(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
        assertTrue(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
        assertFalse(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
    }

    @Test
    fun testDateRangeIsLocalizedAndUsesHabitDatesRatherThanLocalInstants() {
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val configuration = Configuration(targetContext.resources.configuration).apply { setLocale(Locale.FRANCE) }
            val context = targetContext.createConfigurationContext(configuration)
            view = HeaderView(context, prefs, mock()).apply {
                buttonCount = 5
                setMaxDataOffset(10)
            }
            assertTrue(view.scrollByColumns(2))
            val format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.FRANCE).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val today = DateUtils.getTodayWithOffset()
            val expected = context.getString(
                R.string.habit_list_date_range,
                format.format(today.minus(6).toJavaDate()),
                format.format(today.minus(2).toJavaDate())
            )
            assertEquals(expected, view.contentDescription.toString())
            val info = AccessibilityNodeInfo.obtain()
            view.onInitializeAccessibilityNodeInfo(info)
            assertEquals(expected, info.contentDescription.toString())
            info.recycle()
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }

    @Test
    fun testKeyboardDateNavigationFollowsReversalAndRtl() {
        view.setMaxDataOffset(2)
        for (rtl in listOf(false, true)) {
            view.layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            for (reversed in listOf(false, true)) {
                whenever(prefs.isCheckmarkSequenceReversed).thenReturn(reversed)
                view.onCheckmarkSequenceChanged()
                view.reset()
                val older = if (rtl == reversed) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                val newer = if (older == KeyEvent.KEYCODE_DPAD_LEFT) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                assertTrue(view.onKeyDown(older, KeyEvent(KeyEvent.ACTION_DOWN, older)))
                assertEquals(1, view.dataOffset)
                assertTrue(view.onKeyDown(newer, KeyEvent(KeyEvent.ACTION_DOWN, newer)))
                assertEquals(0, view.dataOffset)
            }
        }
    }

    private fun scrollActions(): Map<Int, String> {
        val info = AccessibilityNodeInfo.obtain()
        return try {
            view.onInitializeAccessibilityNodeInfo(info)
            info.actionList.filter {
                it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }.associate { it.id to it.label.toString() }
        } finally {
            info.recycle()
        }
    }

    companion object {
        const val PATH = "habits/list/HeaderView/"
    }
}
