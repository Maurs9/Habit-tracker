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
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.callbacks.OnColorPickedCallback
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import org.isoron.uhabits.utils.dp

/**
 * Dialog that allows the user to choose a color using a 3-ring Donut Color Wheel.
 */
class ColorPickerDialog : AppCompatDialogFragment() {
    private var onPicked: OnColorPickedCallback? = null
    private var currentSelection = 0

    fun setListener(callback: OnColorPickedCallback) {
        onPicked = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
        val context = builder.context
        val colors = requireArguments().getIntArray("colors")!!
        currentSelection = (savedInstanceState ?: requireArguments()).getInt(SELECTED).coerceIn(0, colors.size - 1)
        val names = resources.getStringArray(R.array.habit_color_names)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = dp(16f).toInt()
            setPadding(pad, dp(8f).toInt(), pad, dp(8f).toInt())
        }

        // Live Preview Badge
        val previewBadge = TextView(context).apply {
            gravity = Gravity.CENTER
            val hp = dp(16f).toInt()
            val vp = dp(8f).toInt()
            setPadding(hp, vp, hp, vp)
            textSize = 15f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12f).toInt()
            }
        }

        fun updateBadge(index: Int) {
            val color = colors[index]
            val textColor = contrastingTextColor(color)
            previewBadge.text = names.getOrElse(index) { "Color" }
            previewBadge.setTextColor(textColor)
            val corner = previewBadge.dp(20f)
            val strokeW = previewBadge.dp(1.5f).toInt()
            previewBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = corner
                setColor(color)
                setStroke(strokeW, (textColor and 0x00FFFFFF) or (80 shl 24))
            }
        }

        updateBadge(currentSelection)

        val colorWheel = ColorWheelView(context).apply {
            id = R.id.color_picker
            this.colors = colors
            this.colorNames = names
            this.selectedIndex = currentSelection
            this.onColorSelected = { index ->
                currentSelection = index
                updateBadge(index)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(previewBadge)
        container.addView(colorWheel)

        return builder.setTitle(R.string.color_picker_default_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply { putInt(SELECTED, currentSelection) }
                )
                onPicked?.onColorPicked(PaletteColor(currentSelection))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED, currentSelection)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val REQUEST_KEY = "colorPickerResult"
        const val SELECTED = "selected"
    }
}
