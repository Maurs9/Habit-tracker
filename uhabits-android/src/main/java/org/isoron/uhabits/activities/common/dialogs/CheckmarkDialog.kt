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
import android.view.View.GONE
import android.view.View.VISIBLE
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.EntryDialogFragment.Companion.DRAFT_NOTES
import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.InterfaceUtils.getFontAwesome
import org.isoron.uhabits.utils.sres

class CheckmarkDialog : EntryDialogFragment() {
    var onToggle: ((Int, String) -> Unit)? = null
    private lateinit var view: CheckmarkPopupBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefs = appComponent.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        bindContext(view, numerical = false)
        val color = requireArguments().getInt("color")
        arrayOf(view.yesBtn, view.skipBtn).forEach {
            it.setTextColor(color)
        }
        arrayOf(view.noBtn, view.unknownBtn).forEach {
            it.setTextColor(view.root.sres.getColor(R.attr.contrast60))
        }
        arrayOf(view.yesBtn, view.noBtn, view.skipBtn, view.unknownBtn).forEach {
            it.typeface = getFontAwesome(requireContext())
        }
        view.yesBtn.setEntryActionAccessibility(R.string.entry_action_complete)
        view.noBtn.setEntryActionAccessibility(R.string.entry_action_not_complete)
        view.skipBtn.setEntryActionAccessibility(R.string.entry_action_skip)
        view.unknownBtn.setEntryActionAccessibility(R.string.entry_action_clear)
        val value = requireArguments().getInt("value")
        view.yesBtn.isSelected = value == YES_MANUAL || value == YES_AUTO
        view.noBtn.isSelected = value == NO
        view.skipBtn.isSelected = value == SKIP
        view.unknownBtn.isSelected = value == UNKNOWN
        view.notes.setText(savedInstanceState?.getString(DRAFT_NOTES) ?: requireArguments().getString("notes").orEmpty())
        if (!prefs.isSkipEnabled) view.skipBtn.visibility = GONE
        if (!prefs.areQuestionMarksEnabled) view.unknownBtn.visibility = GONE
        view.booleanButtons.visibility = VISIBLE
        val dialog = Dialog(requireContext())
        dialog.setContentView(view.root)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        fun onClick(v: Int) {
            val notes = view.notes.text.toString().trim()
            val callback = onToggle?.let { { it(v, notes) } }
            if (submitEntry(v, notes, callback)) {
                dismiss()
            } else {
                view.notes.error = getString(R.string.entry_habit_unavailable)
                view.notes.requestFocus()
            }
        }
        view.yesBtn.setOnClickListener { onClick(YES_MANUAL) }
        view.noBtn.setOnClickListener { onClick(NO) }
        view.skipBtn.setOnClickListener { onClick(SKIP) }
        view.unknownBtn.setOnClickListener { onClick(UNKNOWN) }
        view.notes.setOnEditorActionListener { v, actionId, event ->
            onClick(requireArguments().getInt("value"))
            true
        }

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::view.isInitialized) outState.putString(DRAFT_NOTES, view.notes.text.toString())
    }

    override fun onDestroyView() {
        onToggle = null
        super.onDestroyView()
    }
}
