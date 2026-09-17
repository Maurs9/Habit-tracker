package org.isoron.uhabits.activities.habits.edit

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R

class DiscardHabitChangesDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
            .setTitle(R.string.editor_discard_title)
            .setMessage(R.string.editor_discard_message)
            .setPositiveButton(R.string.editor_discard_changes) { _, _ ->
                parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
            }
            .setNegativeButton(R.string.editor_keep_editing, null)
            .create()

    companion object {
        const val TAG = "discardHabitChanges"
        const val REQUEST_KEY = "discardHabitChangesResult"
    }
}
