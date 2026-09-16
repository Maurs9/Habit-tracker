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
package org.isoron.uhabits.acceptance.steps

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.core.models.PaletteColor
import com.google.android.material.R as MaterialR

object EditHabitSteps {
    fun clickSave() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonSave)).perform(ViewActions.click())
    }

    fun pickFrequency() {
        Espresso.onView(ViewMatchers.withId(R.id.boolean_frequency_picker))
            .perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withText("SAVE")).perform(ViewActions.click())
    }

    fun pickMonthFrequency() {
        Espresso.onView(ViewMatchers.withId(R.id.boolean_frequency_picker))
            .perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.xTimesPerMonthRadioButton))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.xTimesPerMonthTextView))
            .perform(ViewActions.replaceText("1"))
        Espresso.onView(ViewMatchers.withText("SAVE")).perform(ViewActions.click())
    }

    fun pickDailyFrequency() {
        Espresso.onView(ViewMatchers.withId(R.id.boolean_frequency_picker))
            .perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.everyDayRadioButton))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("SAVE")).perform(ViewActions.click())
    }

    fun pickColor(color: Int) {
        require(color in 1..PaletteColor.COUNT) { "Color number must be between 1 and ${PaletteColor.COUNT}" }
        Espresso.onView(ViewMatchers.withId(R.id.colorButton)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.color_picker)).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> = ViewMatchers.isAssignableFrom(ColorWheelView::class.java)
            override fun getDescription() = "Select color $color"
            override fun perform(uiController: UiController, view: View) {
                (view as ColorWheelView).select(color - 1)
                uiController.loopMainThreadUntilIdle()
            }
        })
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
    }

    fun typeName(name: String) {
        typeTextWithId(R.id.nameInput, name)
    }

    fun typeQuestion(name: String) {
        expandMoreOptions()
        typeTextWithId(R.id.questionInput, name)
    }

    fun typeDescription(description: String) {
        expandMoreOptions()
        typeTextWithId(R.id.notesInput, description)
    }

    fun expandMoreOptions() {
        var expanded = false
        Espresso.onView(ViewMatchers.withId(R.id.moreGroup)).check { view, exception ->
            if (exception != null) throw exception
            expanded = view.visibility == View.VISIBLE
        }
        if (!expanded) {
            Espresso.onView(ViewMatchers.withId(R.id.moreToggle)).perform(ViewActions.scrollTo(), ViewActions.click())
        }
    }

    fun selectMeasurable() {
        Espresso.onView(ViewMatchers.withId(R.id.typeMeasurable)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun typeUnit(unit: String) = typeTextWithId(R.id.unitInput, unit)

    fun typeTarget(target: String) = typeTextWithId(R.id.targetInput, target)

    fun setReminder() {
        Espresso.onView(ViewMatchers.withId(R.id.reminderTimePicker)).perform(ViewActions.scrollTo(), ViewActions.click())
        Espresso.onView(ViewMatchers.withId(MaterialR.id.material_timepicker_ok_button))
            .perform(ViewActions.click())
    }

    fun clickReminderDays() {
        Espresso.onView(ViewMatchers.withId(R.id.reminderDatePicker)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun unselectAllDays() {
        Espresso.onView(ViewMatchers.withText("Saturday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Sunday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Monday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Tuesday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Wednesday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Thursday")).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText("Friday")).perform(ViewActions.click())
    }

    private fun typeTextWithId(id: Int, name: String) {
        Espresso.onView(ViewMatchers.withId(id)).perform(
            ViewActions.scrollTo(),
            ViewActions.clearText(),
            ViewActions.typeText(name),
            ViewActions.closeSoftKeyboard()
        )
    }
}
