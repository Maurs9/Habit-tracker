package org.isoron.uhabits.activities.common.dialogs

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Section
import org.isoron.uhabits.core.models.SectionList

class SectionDialogs(private val context: Context, private val sections: SectionList) {
    private var activeDialog: AlertDialog? = null

    fun selectSection(sectionId: Long?, mixed: Boolean = false, onSelected: (Long?) -> Unit) {
        val choices = sections.getAll()
        val labels = listOf(context.getString(R.string.section_none)) +
            choices.map { it.name } + context.getString(R.string.section_new)
        val selected = if (mixed) -1 else choices.indexOfFirst { it.id == sectionId } + 1
        show(
            builder().setTitle(R.string.habit_section)
                .setSingleChoiceItems(labels.toTypedArray(), selected) { dialog, index ->
                    dialog.dismiss()
                    when (index) {
                        0 -> onSelected(null)
                        labels.lastIndex -> promptNewSection { onSelected(it.id) }
                        else -> onSelected(sections.getById(choices[index - 1].id)?.id)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        )
    }

    fun promptNewSection(onCreated: (Section) -> Unit) {
        promptName { name -> onCreated(sections.add(name)) }
    }

    fun promptName(section: Section? = null, onName: (String) -> Unit) {
        val dialogBuilder = builder()
        val input = EditText(dialogBuilder.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
            hint = context.getString(R.string.section_name_hint)
            setText(section?.name.orEmpty())
            setSelection(text.length)
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val dialog = dialogBuilder
            .setTitle(if (section == null) R.string.section_add else R.string.section_rename)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                val duplicate = sections.getByName(name)
                when {
                    name.isEmpty() -> input.error = context.getString(R.string.validation_cannot_be_blank)
                    duplicate != null && duplicate.id != section?.id ->
                        input.error = context.getString(R.string.section_name_exists)
                    else -> {
                        onName(name)
                        dialog.dismiss()
                    }
                }
            }
        }
        show(dialog)
    }

    fun dismiss() {
        activeDialog?.dismiss()
        activeDialog = null
    }

    private fun show(dialog: AlertDialog) {
        dismiss()
        activeDialog = dialog
        dialog.show()
    }

    private fun builder() = MaterialAlertDialogBuilder(context, R.style.HabitControlsDialogTheme)
}
