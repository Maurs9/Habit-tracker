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

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.utils.PaletteUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val PATH = "habits/list/CheckmarkPanelView"

@RunWith(AndroidJUnit4::class)
@MediumTest
class EntryPanelViewTest : BaseViewTest() {

    private lateinit var view: CheckmarkPanelView

    @Before
    override fun setUp() {
        super.setUp()
        val checkmarks = intArrayOf(
            YES_MANUAL,
            YES_MANUAL,
            YES_AUTO,
            NO,
            NO,
            NO,
            YES_MANUAL
        )

        view = component.getCheckmarkPanelViewFactory().create().apply {
            values = checkmarks
            buttonCount = 4
            color = PaletteUtils.getAndroidTestColor(7)
        }
        view.onAttachedToWindow()
        measureView(view, dpToPixels(200), dpToPixels(200))
    }

    @After
    public override fun tearDown() {
        view.onDetachedFromWindow()
        super.tearDown()
    }

    @Test
    fun testRender() {
        assertRenders(view, "$PATH/render.png")
    }

    @Test
    fun testTodayFlagFollowsOffset() {
        for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
            view.layoutDirection = direction
            for (reversed in listOf(false, true)) {
                prefs.isCheckmarkSequenceReversed = reversed
                view.dataOffset = 0
                assertEquals(listOf(true, false, false, false), view.buttons.map { it.isToday })
                assertSame(view.buttons[0], view.getChildAt(if (reversed) 3 else 0))
                view.dataOffset = 3
                assertTrue(view.buttons.none { it.isToday })
                view.dataOffset = 0
                assertEquals(listOf(true, false, false, false), view.buttons.map { it.isToday })
            }
        }
    }

    @Test
    fun testToggle() {
        val timestamps = mutableListOf<Timestamp>()
        view.onToggle = { t, _, _ -> timestamps.add(t) }
        view.buttons[0].performLongClick()
        view.buttons[2].performLongClick()
        view.buttons[3].performLongClick()
        assertThat(timestamps, equalTo(listOf(day(0), day(2), day(3))))
    }

    @Test
    fun testToggle_withOffset() {
        val timestamps = mutableListOf<Timestamp>()
        view.dataOffset = 3
        view.onToggle = { t, _, _ -> timestamps += t }
        view.buttons[0].performLongClick()
        view.buttons[2].performLongClick()
        view.buttons[3].performLongClick()
        assertThat(timestamps, equalTo(listOf(day(3), day(5), day(6))))
    }

    @Test
    fun testAccessibilityDatesSurviveOffsetReversalAndRebind() {
        view.habitName = "Read"
        view.dataOffset = 2
        prefs.isCheckmarkSequenceReversed = true
        assertSame(view.buttons.last(), view.getChildAt(0))
        view.buttons.forEachIndexed { index, button ->
            assertEquals(day(index + 2), button.timestamp)
            assertEquals("Read", button.habitName)
        }
        view.habitName = "Walk"
        view.dataOffset = 0
        val info = AccessibilityNodeInfo.obtain()
        view.buttons[0].onInitializeAccessibilityNodeInfo(info)
        assertTrue(info.contentDescription.toString().contains("Walk"))
        assertFalse(info.contentDescription.toString().contains("Read"))
        assertEquals(day(0), view.buttons[0].timestamp)
        info.recycle()
    }
}
