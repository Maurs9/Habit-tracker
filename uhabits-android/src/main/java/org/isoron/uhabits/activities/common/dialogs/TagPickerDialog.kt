package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.databinding.DialogTagPickerBinding

class TagPickerDialog : DialogFragment() {
    private var viewBinding: DialogTagPickerBinding? = null
    private val binding get() = checkNotNull(viewBinding)
    private var all = emptySet<String>()
    private val selected = linkedSetOf<String>()
    private var draft = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = savedInstanceState ?: requireArguments()
        val initialSelection = HabitTags.normalize(state.getStringArrayList(TAGS).orEmpty())
        all = HabitTags.union(emptyList(), state.getStringArrayList(ALL).orEmpty() + initialSelection)
        selected.addAll(all.filter { HabitTags.containsAll(initialSelection, setOf(it)) })
        draft = state.getString(DRAFT).orEmpty()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
        viewBinding = DialogTagPickerBinding.inflate(LayoutInflater.from(builder.context))
        binding.newTagInput.setText(draft)
        binding.addTagButton.setOnClickListener { addTag() }
        binding.newTagInput.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                addTag()
                true
            } else {
                false
            }
        }
        renderChoices()
        return builder
            .setTitle(R.string.habit_tags)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (binding.newTagInput.text.isNotBlank() && !addTag()) return@setOnClickListener
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putStringArrayList(TAGS, ArrayList(all.filter { it in selected }))
                    putLong(HABIT_ID, requireArguments().getLong(HABIT_ID, -1))
                }
            )
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(TAGS, ArrayList(selected))
        outState.putStringArrayList(ALL, ArrayList(all))
        outState.putString(DRAFT, viewBinding?.newTagInput?.text?.toString() ?: draft)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }

    private fun addTag(): Boolean {
        val input = binding.newTagInput
        val name = input.text.toString().trim()
        val error = when {
            name.isEmpty() -> R.string.validation_cannot_be_blank
            name.contains('\n') || name.contains('\r') -> R.string.tag_name_single_line
            else -> null
        }
        if (error != null) {
            input.error = getString(error)
            input.requestFocus()
            return false
        }
        val tag = all.firstOrNull { it.equals(name, ignoreCase = true) } ?: name
        all = HabitTags.union(emptyList(), all + tag)
        selected.add(tag)
        input.setText("")
        input.error = null
        renderChoices()
        return true
    }

    private fun renderChoices() {
        binding.emptyTags.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        binding.tagChoices.removeAllViews()
        for (tag in all) {
            binding.tagChoices.addView(
                MaterialCheckBox(binding.root.context).apply {
                    text = tag
                    isChecked = tag in selected
                    isSaveEnabled = false
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) selected.add(tag) else selected.remove(tag)
                    }
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            )
        }
    }

    companion object {
        const val REQUEST_KEY = "tagPickerResult"
        const val FRAGMENT_TAG = "tagPicker"
        const val TAGS = "tags"
        const val HABIT_ID = "habitId"
        private const val ALL = "allTags"
        private const val DRAFT = "newTagDraft"

        fun newInstance(habitId: Long, selected: Set<String>, all: Set<String>) = TagPickerDialog().apply {
            arguments = Bundle().apply {
                putLong(HABIT_ID, habitId)
                putStringArrayList(TAGS, ArrayList(selected))
                putStringArrayList(ALL, ArrayList(all))
            }
        }
    }
}
