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
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.list.HabitListEmptyState
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.getFontAwesome
import org.isoron.uhabits.utils.sp
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.str

class EmptyListView(context: Context) : LinearLayout(context) {
    var textTextView: TextView
    var iconTextView: TextView
    val recoveryButton = Button(context).apply {
        minHeight = dp(48f).toInt()
        visibility = GONE
    }

    init {
        orientation = VERTICAL
        gravity = CENTER
        visibility = View.GONE

        iconTextView = TextView(context).apply {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            text = str(R.string.fa_star_half_o)
            typeface = getFontAwesome()
            textSize = sp(40.0f)
            gravity = CENTER
            setTextColor(sres.getColor(R.attr.contrast60))
        }

        addView(
            iconTextView,
            MATCH_PARENT,
            WRAP_CONTENT
        )

        textTextView = TextView(context).apply {
            text = str(R.string.no_habits_found)
            gravity = CENTER
            setPadding(0, dp(20.0f).toInt(), 0, 0)
            setTextColor(sres.getColor(R.attr.contrast60))
        }
        addView(
            textTextView,
            MATCH_PARENT,
            WRAP_CONTENT
        )
        addView(recoveryButton, WRAP_CONTENT, WRAP_CONTENT)
    }

    fun showState(state: HabitListEmptyState, onRecover: () -> Unit) {
        if (state == HabitListEmptyState.NONE) {
            hide()
            return
        }
        visibility = VISIBLE
        val (message, action) = when (state) {
            HabitListEmptyState.NO_HABITS -> R.string.no_habits_found to R.string.add_habit
            HabitListEmptyState.ARCHIVED -> R.string.list_only_archived to R.string.list_show_archived
            HabitListEmptyState.ENTERED -> R.string.list_entries_hidden to R.string.list_show_all_entries
            HabitListEmptyState.COMPLETED -> R.string.list_completed_hidden to R.string.list_show_completed
            else -> R.string.list_filtered_empty to R.string.list_clear_filters
        }
        iconTextView.text = str(R.string.fa_star_half_o)
        textTextView.setText(message)
        recoveryButton.setText(action)
        recoveryButton.setOnClickListener { onRecover() }
        recoveryButton.visibility = VISIBLE
    }

    fun showDone() {
        visibility = VISIBLE
        iconTextView.text = str(R.string.fa_umbrella_beach)
        textTextView.text = str(R.string.no_habits_left_to_do)
    }

    fun showEmpty() {
        visibility = VISIBLE
        iconTextView.text = str(R.string.fa_star_half_o)
        textTextView.text = str(R.string.no_habits_found)
    }

    fun showFiltered() {
        visibility = VISIBLE
        iconTextView.text = str(R.string.fa_star_half_o)
        textTextView.text = str(R.string.no_habits_match_tags)
    }

    fun hide() {
        visibility = GONE
    }
}
