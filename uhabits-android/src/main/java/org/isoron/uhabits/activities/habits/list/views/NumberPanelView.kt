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
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.ActivityContext
import javax.inject.Inject

class NumberPanelViewFactory
@Inject constructor(
    @ActivityContext val context: Context,
    val preferences: Preferences,
    val buttonFactory: NumberButtonViewFactory
) {
    fun create() = NumberPanelView(context, preferences, buttonFactory)
}

class NumberPanelView(
    @ActivityContext context: Context,
    preferences: Preferences,
    private val buttonFactory: NumberButtonViewFactory
) : ButtonPanelView<NumberButtonView>(context, preferences) {

    var habitName = ""
        set(value) {
            field = value
            setupButtons()
        }

    var values = DoubleArray(0)
        set(values) {
            field = values
            setupButtons()
        }

    var targetType = NumericalHabitType.AT_LEAST
        set(value) {
            field = value
            setupButtons()
        }

    var threshold = 0.0
        set(value) {
            field = value
            setupButtons()
        }

    var color = 0
        set(value) {
            field = value
            setupButtons()
        }

    var units = ""
        set(value) {
            field = value
            setupButtons()
        }

    var notes = arrayOf<String>()
        set(values) {
            field = values
            setupButtons()
        }

    var onEdit: (Timestamp) -> Unit = { _ -> }
        set(value) {
            field = value
            setupButtons()
        }

    override fun createButton() = buttonFactory.create().apply {
        listDensity = this@NumberPanelView.listDensity
    }

    override fun applyListDensity(density: ListDensity) {
        for (button in buttons) {
            button.listDensity = density
        }
    }

    @Synchronized
    override fun setupButtons() {
        val today = DateUtils.getTodayWithOffset()

        buttons.forEachIndexed { index, button ->
            val timestamp = today.minus(index + dataOffset)
            button.habitName = habitName
            button.timestamp = timestamp
            button.isToday = index + dataOffset == 0
            button.value = when {
                index + dataOffset < values.size -> values[index + dataOffset]
                else -> Entry.UNKNOWN.toDouble() / 1000
            }
            button.notes = when {
                index + dataOffset < notes.size -> notes[index + dataOffset]
                else -> ""
            }
            button.color = color
            button.targetType = targetType
            button.threshold = threshold
            button.units = units
            button.onEdit = { onEdit(timestamp) }
        }
    }
}
