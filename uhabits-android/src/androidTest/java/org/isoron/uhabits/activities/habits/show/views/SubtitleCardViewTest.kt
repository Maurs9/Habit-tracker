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
package org.isoron.uhabits.activities.habits.show.views

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList.Companion.EVERY_DAY
import org.isoron.uhabits.core.ui.screens.habits.show.views.SubtitleCardState
import org.isoron.uhabits.core.ui.views.LightTheme
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SubtitleCardViewTest : BaseViewTest() {
    val PATH = "habits/show/SubtitleCard/"
    private lateinit var view: SubtitleCardView

    @Before
    override fun setUp() {
        super.setUp()
        view = LayoutInflater
            .from(targetContext)
            .inflate(R.layout.show_habit, null)
            .findViewById(R.id.subtitleCard)
        view.setState(
            SubtitleCardState(
                color = PaletteColor(7),
                frequency = Frequency(3, 7),
                isNumerical = false,
                question = "Did you meditate this morning?",
                reminder = Reminder(8, 30, EVERY_DAY),
                theme = LightTheme()
            )
        )
        measureView(view, 800f, 200f)
    }

    @Test
    fun testRender() {
        assertRenders(view, PATH + "render.png")
    }

    @Test
    fun testOrganizationRowIsGoneByDefault() {
        assertEquals(View.GONE, view.findViewById<View>(R.id.organizationRow).visibility)
    }

    @Test
    fun testRender_tags() {
        view.setState(
            SubtitleCardState(
                color = PaletteColor(7),
                frequency = Frequency(3, 7),
                isNumerical = false,
                question = "Did you meditate this morning?",
                reminder = Reminder(8, 30, EVERY_DAY),
                theme = LightTheme(),
                sectionName = "Morning",
                tags = setOf("Wellbeing", "Health")
            )
        )
        measureView(view, 800f, 200f)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.organizationRow).visibility)
        assertEquals("Health, Wellbeing", view.findViewById<TextView>(R.id.tagsLabel).text.toString())
        assertRenders(view, PATH + "render_tags.png")
    }

    @Test
    fun testOrganizationVisibilityResetsWhenStateChanges() {
        val state = SubtitleCardState(
            color = PaletteColor(7),
            frequency = Frequency.DAILY,
            isNumerical = false,
            question = "",
            reminder = null,
            theme = LightTheme()
        )
        view.setState(state.copy(tags = setOf("Health")))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.tagsIcon).visibility)
        assertEquals(View.GONE, view.findViewById<View>(R.id.sectionLabel).visibility)
        view.setState(state.copy(sectionName = "Morning"))
        assertEquals(View.GONE, view.findViewById<View>(R.id.tagsLabel).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.sectionIcon).visibility)
        view.setState(state)
        assertEquals(View.GONE, view.findViewById<View>(R.id.organizationRow).visibility)
    }
}
