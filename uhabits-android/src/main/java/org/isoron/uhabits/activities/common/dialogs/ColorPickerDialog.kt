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
package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.callbacks.OnColorPickedCallback
import org.isoron.uhabits.utils.dp

/**
 * Dialog that allows the user to choose a color.
 */
class ColorPickerDialog : AppCompatDialogFragment() {
    private var onPicked: OnColorPickedCallback? = null

    fun setListener(callback: OnColorPickedCallback) {
        onPicked = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
        val context = builder.context
        val colors = requireArguments().getIntArray("colors")!!
        val selected = requireArguments().getInt("selected")
        val names = resources.getStringArray(R.array.habit_color_names)
        val grid = GridLayout(context).apply {
            id = R.id.color_picker
            columnCount = 4
            val padding = dp(12f).toInt()
            setPadding(padding, 0, padding, padding)
        }
        colors.forEachIndexed { index, color ->
            val foreground = if (ColorUtils.calculateLuminance(color) > 0.179) Color.BLACK else Color.WHITE
            val swatch = MaterialButton(context).apply {
                layoutParams = GridLayout.LayoutParams(
                    GridLayout.spec(index / 4),
                    GridLayout.spec(index % 4, 1f)
                ).apply {
                    width = 0
                    height = dp(56f).toInt()
                    val margin = dp(4f).toInt()
                    setMargins(margin, 0, margin, 0)
                }
                minWidth = 0
                minimumWidth = 0
                setPadding(0, 0, 0, 0)
                cornerRadius = dp(24f).toInt()
                backgroundTintList = ColorStateList.valueOf(color)
                setTextColor(foreground)
                isCheckable = true
                isChecked = index == selected
                text = if (isChecked) "✓" else ""
                contentDescription = names[index]
                setOnClickListener {
                    onPicked?.onColorPicked(PaletteColor(index))
                    dismiss()
                }
            }
            grid.addView(swatch)
        }
        val scroll = object : ScrollView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val maximumHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
                super.onMeasure(
                    widthMeasureSpec,
                    View.MeasureSpec.makeMeasureSpec(maximumHeight, View.MeasureSpec.AT_MOST)
                )
            }
        }.apply { addView(grid) }
        return builder.setTitle(R.string.color_picker_default_title)
            .setView(scroll)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}
