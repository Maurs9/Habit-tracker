package org.isoron.uhabits.activities.settings

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.SectionDialogs
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateSectionCommand
import org.isoron.uhabits.core.commands.DeleteSectionCommand
import org.isoron.uhabits.core.commands.MoveSectionCommand
import org.isoron.uhabits.core.commands.RenameSectionCommand
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.models.Section

class SectionsDialog : DialogFragment(), CommandRunner.Listener, ModelObservable.Listener {
    private val component
        get() = (requireContext().applicationContext as HabitsApplication).component
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var sectionDialogs: SectionDialogs
    private var sections = emptyList<Section>()
    private var actionDialog: AlertDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = builder()
        sectionDialogs = SectionDialogs(requireContext(), component.sectionList)
        adapter = object : ArrayAdapter<String>(builder.context, android.R.layout.simple_list_item_1, mutableListOf()) {
            override fun isEnabled(position: Int) = sections.isNotEmpty()
            override fun areAllItemsEnabled() = false
        }
        refresh()
        return builder.setTitle(R.string.sections)
            .setAdapter(adapter) { _, index -> sections.getOrNull(index)?.let { showActions(it.id) } }
            .setNeutralButton(R.string.section_add, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        component.commandRunner.addListener(this)
        component.sectionList.observable.addListener(this)
        val dialog = requireDialog() as AlertDialog
        // Keep the manager open while editing its children.
        dialog.listView.setOnItemClickListener { _, _, index, _ ->
            sections.getOrNull(index)?.let { showActions(it.id) }
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            sectionDialogs.promptName { name ->
                component.commandRunner.run(CreateSectionCommand(component.sectionList, name))
            }
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onStop() {
        component.commandRunner.removeListener(this)
        component.sectionList.observable.removeListener(this)
        super.onStop()
    }

    override fun onDestroyView() {
        actionDialog?.dismiss()
        actionDialog = null
        sectionDialogs.dismiss()
        super.onDestroyView()
    }

    override fun onCommandFinished(command: Command) = onModelChange()

    override fun onModelChange() {
        activity?.runOnUiThread {
            if (isAdded && dialog?.isShowing == true) refresh()
        }
    }

    private fun refresh() {
        sections = component.sectionList.getAll()
        adapter.clear()
        adapter.addAll(sections.map { it.name }.ifEmpty { listOf(getString(R.string.no_sections_yet)) })
        adapter.notifyDataSetChanged()
    }

    private fun showActions(id: Long) {
        val section = component.sectionList.getById(id) ?: return
        val actions = buildList {
            add(R.string.section_rename)
            if (section.position > 0) add(R.string.section_move_up)
            if (section.position < component.sectionList.size() - 1) add(R.string.section_move_down)
            add(R.string.section_delete)
        }
        actionDialog = builder().setTitle(section.name)
            .setItems(actions.map { getString(it) }.toTypedArray()) { _, index ->
                val current = component.sectionList.getById(id) ?: return@setItems
                when (actions[index]) {
                    R.string.section_rename -> sectionDialogs.promptName(current) { name ->
                        component.commandRunner.run(RenameSectionCommand(component.sectionList, id, name))
                    }
                    R.string.section_move_up -> move(current, -1)
                    R.string.section_move_down -> move(current, 1)
                    R.string.section_delete -> confirmDelete(current)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun move(section: Section, delta: Int) {
        component.commandRunner.run(MoveSectionCommand(component.sectionList, section.id, section.position + delta))
    }

    private fun confirmDelete(section: Section) {
        actionDialog = builder().setTitle(R.string.section_delete)
            .setMessage(getString(R.string.section_delete_confirm, section.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.section_delete) { _, _ ->
                if (component.sectionList.getById(section.id) != null) {
                    component.commandRunner.run(DeleteSectionCommand(component.sectionList, component.habitList, section.id))
                }
            }
            .show()
    }

    private fun builder() = MaterialAlertDialogBuilder(requireContext(), R.style.HabitControlsDialogTheme)
}
