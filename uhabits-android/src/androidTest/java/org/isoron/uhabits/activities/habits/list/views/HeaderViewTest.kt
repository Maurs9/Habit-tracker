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

import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
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

    companion object {
        const val PATH = "habits/list/HeaderView/"
    }
}
