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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.ColorWheelView
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.callbacks.OnColorPickedCallback
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import org.isoron.uhabits.utils.dp
import kotlin.math.ceil

/**
 * Color selection using a wheel or a named, scrollable swatch list.
 */
class ColorPickerDialog : AppCompatDialogFragment() {
    private var onPicked: OnColorPickedCallback? = null
    private var currentSelection = 0
    private var showingList = false
    private var list: ListView? = null

    fun setListener(callback: OnColorPickedCallback) {
        onPicked = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
        val context = builder.context
        val colors = requireArguments().getIntArray("colors")!!
        currentSelection = (savedInstanceState ?: requireArguments()).getInt(SELECTED).coerceIn(0, colors.size - 1)
        showingList = savedInstanceState?.getBoolean(SHOWING_LIST) ?: false
        val names = resources.getStringArray(R.array.habit_color_names)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = dp(16f).toInt()
            setPadding(pad, dp(8f).toInt(), pad, dp(8f).toInt())
        }

        // Live Preview Badge
        val previewBadge = TextView(context).apply {
            id = R.id.colorPickerPreview
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
            previewBadge.text = names[index]
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
                list?.setItemChecked(index, true)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val paletteList = object : ListView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val maximum = dp(280f).toInt()
                val available = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                    maximum
                } else {
                    MeasureSpec.getSize(heightMeasureSpec).coerceAtMost(maximum)
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST))
            }
        }.apply {
            id = R.id.colorPickerList
            choiceMode = ListView.CHOICE_MODE_SINGLE
            adapter = object : BaseAdapter() {
                override fun getCount() = colors.size
                override fun getItem(position: Int) = names[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun hasStableIds() = true

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val row = convertView as? CheckedTextView ?: (
                        LayoutInflater.from(context)
                            .inflate(android.R.layout.simple_list_item_single_choice, parent, false) as CheckedTextView
                        ).apply {
                        minHeight = ceil(dp(48f)).toInt()
                        isSingleLine = false
                        layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        compoundDrawablePadding = dp(12f).toInt()
                    }
                    row.text = names[position]
                    row.isChecked = position == currentSelection
                    val swatch = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colors[position])
                        setStroke(row.dp(1f).toInt(), contrastingTextColor(colors[position]))
                        setBounds(0, 0, row.dp(24f).toInt(), row.dp(24f).toInt())
                    }
                    row.setCompoundDrawablesRelative(swatch, null, null, null)
                    return row
                }
            }
            setItemChecked(currentSelection, true)
            setSelection(savedInstanceState?.getInt(LIST_POSITION, currentSelection) ?: currentSelection)
            setOnItemClickListener { _, _, position, _ -> colorWheel.select(position) }
        }
        list = paletteList
        val modeToggle = LayoutInflater.from(context)
            .inflate(R.layout.color_picker_mode_button, container, false) as MaterialButton
        fun updateMode() {
            previewBadge.visibility = if (showingList) View.GONE else View.VISIBLE
            colorWheel.visibility = if (showingList) View.GONE else View.VISIBLE
            paletteList.visibility = if (showingList) View.VISIBLE else View.GONE
            modeToggle.setText(if (showingList) R.string.color_picker_show_wheel else R.string.color_picker_show_list)
        }
        modeToggle.setOnClickListener {
            showingList = !showingList
            updateMode()
            if (showingList) paletteList.setSelection(currentSelection)
        }
        updateMode()

        container.addView(previewBadge)
        container.addView(modeToggle)
        container.addView(colorWheel)
        container.addView(paletteList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

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
        outState.putBoolean(SHOWING_LIST, showingList)
        outState.putInt(LIST_POSITION, list?.firstVisiblePosition ?: currentSelection)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        list = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "colorPickerResult"
        const val SELECTED = "selected"
        private const val SHOWING_LIST = "showingList"
        private const val LIST_POSITION = "listPosition"
    }
}
