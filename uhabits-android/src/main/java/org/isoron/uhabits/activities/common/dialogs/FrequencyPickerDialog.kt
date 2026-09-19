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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.databinding.FrequencyPickerDialogBinding

class FrequencyPickerDialog(
    var freqNumerator: Int,
    var freqDenominator: Int
) : AppCompatDialogFragment() {
    private var _binding: FrequencyPickerDialogBinding? = null
    private val binding get() = _binding!!
    private var restoredFocusId = View.NO_ID
    private var restoredSelectionId = View.NO_ID

    var onFrequencyPicked: (num: Int, den: Int) -> Unit = { _, _ -> }

    init {
        arguments = Bundle().apply {
            putInt(NUMERATOR, freqNumerator)
            putInt(DENOMINATOR, freqDenominator)
        }
    }

    constructor() : this(1, 1)

    private val inputs
        get() = listOf(
            binding.everyXDaysTextView,
            binding.xTimesPerWeekTextView,
            binding.xTimesPerMonthTextView,
            binding.xTimesPerYDaysXTextView,
            binding.xTimesPerYDaysYTextView
        )

    private val radios
        get() = listOf(
            binding.everyDayRadioButton,
            binding.everyXDaysRadioButton,
            binding.xTimesPerWeekRadioButton,
            binding.xTimesPerMonthRadioButton,
            binding.xTimesPerYDaysRadioButton
        )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FrequencyPickerDialogBinding.inflate(LayoutInflater.from(requireActivity()))
        freqNumerator = requireArguments().getInt(NUMERATOR, 1)
        freqDenominator = requireArguments().getInt(DENOMINATOR, 1)
        (inputs + radios).forEach { it.isSaveEnabled = false }
        binding.root.isFocusableInTouchMode = true
        binding.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        binding.root.requestFocus()

        addBeforeAfterText(
            this.getString(R.string.every_x_days),
            binding.everyXDaysContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_week),
            binding.xTimesPerWeekContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_month),
            binding.xTimesPerMonthContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_y_days),
            binding.xTimesPerYDaysContainer
        )

        binding.everyDayRadioButton.setOnClickListener {
            check(binding.everyDayRadioButton)
            if (!it.requestFocus()) binding.root.requestFocus()
        }

        binding.everyXDaysRadioButton.setOnClickListener {
            check(binding.everyXDaysRadioButton)
            val everyXDaysTextView = binding.everyXDaysTextView
            selectInputField(everyXDaysTextView)
        }

        binding.everyXDaysTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.everyXDaysRadioButton)
        }

        binding.xTimesPerWeekRadioButton.setOnClickListener {
            check(binding.xTimesPerWeekRadioButton)
            selectInputField(binding.xTimesPerWeekTextView)
        }

        binding.xTimesPerWeekTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerWeekRadioButton)
        }

        binding.xTimesPerMonthRadioButton.setOnClickListener {
            check(binding.xTimesPerMonthRadioButton)
            selectInputField(binding.xTimesPerMonthTextView)
        }

        binding.xTimesPerMonthTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerMonthRadioButton)
        }

        binding.xTimesPerYDaysRadioButton.setOnClickListener {
            check(binding.xTimesPerYDaysRadioButton)
            selectInputField(binding.xTimesPerYDaysXTextView)
        }

        binding.xTimesPerYDaysXTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerYDaysRadioButton)
        }

        binding.xTimesPerYDaysYTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerYDaysRadioButton)
        }

        populateViews()
        if (savedInstanceState != null) {
            for (input in inputs) {
                input.setText(savedInstanceState.getString("input_${input.id}", input.text.toString()))
                input.error = savedInstanceState.getString("error_${input.id}")
            }
            restoredSelectionId = savedInstanceState.getInt(SELECTED, View.NO_ID)
            radios.firstOrNull { it.id == restoredSelectionId }?.let { check(it) }
            restoredFocusId = savedInstanceState.getInt(FOCUSED, View.NO_ID)
        }

        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onSaveClicked() }
        radios.firstOrNull { it.id == restoredSelectionId }?.let {
            check(it)
            if (!focusMatchesSelection(binding.root.findFocus()?.id ?: View.NO_ID)) binding.root.requestFocus()
        }
        if (restoredFocusId != View.NO_ID) {
            if (focusMatchesSelection(restoredFocusId)) {
                binding.root.findViewById<View>(restoredFocusId)?.requestFocus()
            } else {
                binding.root.requestFocus()
            }
        }
        restoredFocusId = View.NO_ID
        restoredSelectionId = View.NO_ID
    }

    private fun focusMatchesSelection(id: Int): Boolean = when (id) {
        R.id.everyXDaysTextView -> binding.everyXDaysRadioButton.isChecked
        R.id.xTimesPerWeekTextView -> binding.xTimesPerWeekRadioButton.isChecked
        R.id.xTimesPerMonthTextView -> binding.xTimesPerMonthRadioButton.isChecked
        R.id.xTimesPerYDaysXTextView, R.id.xTimesPerYDaysYTextView -> binding.xTimesPerYDaysRadioButton.isChecked
        else -> true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _binding?.let {
            outState.putInt(SELECTED, radios.firstOrNull { it.isChecked }?.id ?: R.id.everyDayRadioButton)
            outState.putInt(FOCUSED, binding.root.findFocus()?.id ?: View.NO_ID)
            for (input in inputs) {
                outState.putString("input_${input.id}", input.text.toString())
                outState.putString("error_${input.id}", input.error?.toString())
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun addBeforeAfterText(
        str: String,
        container: LinearLayout
    ) {
        val parts = str.split("%d")
        for (i in parts.indices) {
            container.addView(
                TextView(activity).apply { text = parts[i].trim() },
                2 * i + 1
            )
        }
    }

    private fun onSaveClicked() {
        inputs.forEach { it.error = null }
        val (numerator, denominator) = when {
            binding.everyDayRadioButton.isChecked -> 1 to 1
            binding.everyXDaysRadioButton.isChecked -> 1 to readPositiveInteger(binding.everyXDaysTextView)
            binding.xTimesPerWeekRadioButton.isChecked -> readPositiveInteger(binding.xTimesPerWeekTextView) to 7
            binding.xTimesPerMonthRadioButton.isChecked -> readPositiveInteger(binding.xTimesPerMonthTextView) to 30
            else -> readPositiveInteger(binding.xTimesPerYDaysXTextView) to readPositiveInteger(binding.xTimesPerYDaysYTextView)
        }
        if (numerator == null || denominator == null) {
            inputs.firstOrNull { it.error != null }?.requestFocus()
            return
        }
        if (numerator > denominator) {
            val input = when {
                binding.xTimesPerWeekRadioButton.isChecked -> binding.xTimesPerWeekTextView
                binding.xTimesPerMonthRadioButton.isChecked -> binding.xTimesPerMonthTextView
                else -> binding.xTimesPerYDaysXTextView
            }
            input.error = getString(R.string.frequency_not_more_than_days)
            input.requestFocus()
            return
        }
        val num = if (numerator == denominator) 1 else numerator
        val den = if (numerator == denominator) 1 else denominator
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putInt(NUMERATOR, num)
                putInt(DENOMINATOR, den)
            }
        )
        onFrequencyPicked(num, den)
        dismiss()
    }

    private fun readPositiveInteger(input: EditText): Int? {
        val text = input.text.toString()
        val value = parsePositiveFrequencyInteger(text)
        if (value == null) {
            input.error = if (text.isBlank()) {
                getString(R.string.validation_cannot_be_blank)
            } else {
                getString(R.string.frequency_positive_integer)
            }
            return null
        }
        return value
    }

    private fun check(view: RadioButton) {
        uncheckAll()
        view.isChecked = true
    }

    private fun populateViews() {
        uncheckAll()
        if (Frequency.isMonthlyInterval(freqDenominator)) {
            binding.xTimesPerMonthRadioButton.isChecked = true
            binding.xTimesPerMonthTextView.setText(freqNumerator.toString())
        } else {
            if (freqNumerator == 1) {
                if (freqDenominator == 1) {
                    binding.everyDayRadioButton.isChecked = true
                } else {
                    binding.everyXDaysRadioButton.isChecked = true
                    binding.everyXDaysTextView.setText(freqDenominator.toString())
                }
            } else {
                if (freqDenominator == 7) {
                    binding.xTimesPerWeekRadioButton.isChecked = true
                    binding.xTimesPerWeekTextView.setText(freqNumerator.toString())
                } else {
                    binding.xTimesPerYDaysRadioButton.isChecked = true
                    binding.xTimesPerYDaysXTextView.setText(freqNumerator.toString())
                    binding.xTimesPerYDaysYTextView.setText(freqDenominator.toString())
                }
            }
        }
    }

    private fun selectInputField(view: EditText) {
        view.requestFocus()
        view.setSelection(view.text.length)
    }

    private fun uncheckAll() {
        radios.forEach { it.isChecked = false }
    }

    companion object {
        const val REQUEST_KEY = "frequencyPickerResult"
        const val NUMERATOR = "numerator"
        const val DENOMINATOR = "denominator"
        private const val SELECTED = "selected"
        private const val FOCUSED = "focused"
    }
}
