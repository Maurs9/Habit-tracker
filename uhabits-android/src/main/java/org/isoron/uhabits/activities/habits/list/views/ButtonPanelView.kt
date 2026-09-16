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
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.widget.LinearLayout
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.toMeasureSpec

abstract class ButtonPanelView<T : View>(
    context: Context,
    val preferences: Preferences
) : LinearLayout(context),
    Preferences.Listener {

    var buttonCount = 0
        set(value) {
            field = value
            inflateButtons()
        }

    var dataOffset = 0
        set(value) {
            field = value
            setupButtons()
        }

    var listDensity = ListDensity.STANDARD
        set(value) {
            field = value
            buttonHeight = dp(value.rowHeightDp.toFloat()).toInt()
            applyListDensity(value)
        }

    var buttonHeight = dim(R.dimen.checkmarkHeight).toInt()
        set(value) {
            require(value >= dp(36f).toInt()) { "Entry buttons must remain at least 36dp tall" }
            if (field == value) return
            field = value
            requestLayout()
        }

    protected open fun applyListDensity(density: ListDensity) {}

    var buttons = mutableListOf<T>()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onCheckmarkSequenceChanged() {
        inflateButtons()
    }

    @Synchronized
    protected fun inflateButtons() {
        val reverse = preferences.isCheckmarkSequenceReversed

        buttons.clear()
        repeat(buttonCount) { buttons.add(createButton()) }

        removeAllViews()
        if (reverse) {
            buttons.reversed().forEach { addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)) }
        } else {
            buttons.forEach { addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)) }
        }
        setupButtons()
        requestLayout()
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        preferences.addListener(this)
    }

    public override fun onDetachedFromWindow() {
        preferences.removeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val buttonWidth = dim(R.dimen.checkmarkWidth)
        val width = (buttonWidth * buttonCount)
        super.onMeasure(
            width.toMeasureSpec(EXACTLY),
            buttonHeight.toFloat().toMeasureSpec(EXACTLY)
        )
    }

    protected abstract fun setupButtons()
    protected abstract fun createButton(): T
}
