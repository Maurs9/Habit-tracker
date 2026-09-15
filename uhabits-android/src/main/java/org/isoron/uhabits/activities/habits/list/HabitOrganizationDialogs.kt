package org.isoron.uhabits.activities.habits.list

import android.content.Context
import android.text.InputType
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.BulkSkipCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.preferences.SavedHabitFilter
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsMenuBehavior
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.inject.ActivityScope
import java.util.Locale
import javax.inject.Inject

@ActivityScope
class HabitOrganizationDialogs @Inject constructor(
    @ActivityContext context: Context,
    private val habits: HabitList,
    private val preferences: Preferences,
    private val commandRunner: CommandRunner,
    private val taskRunner: TaskRunner
) {
    private val activity = context as AppCompatActivity

    fun editTags(habit: Habit) {
        val input = textInput(multiline = true).apply {
            hint = activity.getString(R.string.habit_tags_hint)
            setText(HabitTags.format(habit.tags))
        }
        dialogBuilder()
            .setTitle(R.string.habit_tags)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                commandRunner.run(
                    ChangeHabitTagsCommand(habits, listOf(habit), HabitTags.parse(input.text.toString()))
                )
            }
            .show()
    }

    fun filterTags(behavior: ListHabitsMenuBehavior) {
        val tags = HabitTags.normalize(habits.flatMap { it.tags } + preferences.selectedTags)
            .sortedBy { it.lowercase(Locale.ROOT) }
        if (tags.isEmpty()) {
            dialogBuilder()
                .setTitle(R.string.filter_tags)
                .setMessage(R.string.no_tags_yet)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val checked = tags.map { tag ->
            HabitTags.containsAll(preferences.selectedTags, setOf(tag))
        }.toBooleanArray()
        dialogBuilder()
            .setTitle(R.string.filter_tags_all)
            .setMultiChoiceItems(tags.toTypedArray(), checked) { _, index, selected ->
                checked[index] = selected
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.clear_tag_filter) { _, _ ->
                behavior.onFilterTags(emptySet())
                activity.invalidateOptionsMenu()
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                behavior.onFilterTags(tags.filterIndexed { index, _ -> checked[index] }.toSet())
                activity.invalidateOptionsMenu()
            }
            .show()
    }

    fun saveFilter(behavior: ListHabitsMenuBehavior) {
        val existing = readSavedFilters() ?: return
        val input = textInput(multiline = false).apply {
            hint = activity.getString(R.string.filter_name)
        }
        val dialog = dialogBuilder()
            .setTitle(R.string.save_filter)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                when {
                    name.isEmpty() -> input.error = activity.getString(R.string.validation_cannot_be_blank)
                    existing.any { it.name.equals(name, ignoreCase = true) } ->
                        input.error = activity.getString(R.string.filter_name_exists)
                    else -> {
                        preferences.savedHabitFilters = existing + behavior.currentFilter(name)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    fun savedFilters(behavior: ListHabitsMenuBehavior) {
        val filters = readSavedFilters() ?: return
        if (filters.isEmpty()) {
            dialogBuilder()
                .setTitle(R.string.saved_filters)
                .setMessage(R.string.no_saved_filters)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        dialogBuilder()
            .setTitle(R.string.saved_filters)
            .setItems(filters.map { it.name }.toTypedArray()) { _, index ->
                behavior.onApplySavedFilter(filters[index])
                activity.invalidateOptionsMenu()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.delete_saved_filter) { _, _ ->
                dialogBuilder()
                    .setTitle(R.string.delete_saved_filter)
                    .setItems(filters.map { it.name }.toTypedArray()) { _, index ->
                        val filter = filters[index]
                        dialogBuilder()
                            .setTitle(R.string.delete_saved_filter)
                            .setMessage(filter.name)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                preferences.savedHabitFilters = filters.filter { it != filter }
                            }
                            .show()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            .show()
    }

    fun bulkSkip(selected: List<Habit>) {
        val today = DateUtils.getTodayWithOffset().unixTime
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTheme(R.style.HabitOrganizationCalendarTheme)
            .setTitleText(R.string.bulk_skip)
            .setSelection(Pair(today, today))
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(Timestamp.DAY_LENGTH)
                    .setValidator(DateValidatorPointForward.from(Timestamp.DAY_LENGTH))
                    .build()
            )
            .build()
        picker.arguments?.putLongArray(BULK_HABITS, selected.mapNotNull { it.id }.toLongArray())
        attachBulkSkipListener(picker)
        picker.show(activity.supportFragmentManager, BULK_PICKER)
    }

    fun restorePendingDialogs() {
        val picker = activity.supportFragmentManager.findFragmentByTag(BULK_PICKER)
        if (picker is MaterialDatePicker<*>) attachBulkSkipListener(picker)
    }

    private fun attachBulkSkipListener(picker: MaterialDatePicker<*>) {
        picker.clearOnPositiveButtonClickListeners()
        picker.addOnPositiveButtonClickListener { selection ->
            val range = selection as? Pair<*, *>
            val from = range?.first as? Long
            val to = range?.second as? Long
            val selected = picker.arguments?.getLongArray(BULK_HABITS)
                ?.asSequence()?.mapNotNull { habits.getById(it) }?.toList().orEmpty()
            if (from == null || to == null || from < Timestamp.DAY_LENGTH || to < from || selected.isEmpty()) {
                showError(R.string.bulk_skip_unavailable)
            } else {
                previewBulkSkip(BulkSkipCommand(habits, selected, Timestamp(from), Timestamp(to)))
            }
        }
    }

    private fun previewBulkSkip(command: BulkSkipCommand) {
        taskRunner.execute(object : Task {
            private var eligible = 0L

            override fun doInBackground() {
                eligible = command.countEligibleEntries()
            }

            override fun onPostExecute() {
                if (activity.isFinishing || activity.isDestroyed) return
                val message = if (eligible == 0L) {
                    activity.getString(R.string.bulk_skip_empty)
                } else {
                    activity.getString(
                        R.string.bulk_skip_preview,
                        eligible,
                        command.from.toDialogDateString(),
                        command.to.toDialogDateString()
                    )
                }
                val dialog = dialogBuilder()
                    .setTitle(R.string.bulk_skip)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.cancel, null)
                if (eligible > 0) {
                    dialog.setPositiveButton(R.string.apply_bulk_skip) { _, _ ->
                        commandRunner.run(command)
                    }
                }
                dialog.show()
            }
        })
    }

    private fun readSavedFilters(): List<SavedHabitFilter>? = try {
        preferences.savedHabitFilters
    } catch (e: IllegalArgumentException) {
        Log.e("HabitOrganization", "Cannot read saved filters", e)
        dialogBuilder()
            .setTitle(R.string.saved_filters)
            .setMessage(R.string.saved_filters_invalid)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.reset_saved_filters) { _, _ ->
                preferences.savedHabitFilters = emptyList()
            }
            .show()
        null
    }

    private fun textInput(multiline: Boolean) = EditText(activity).apply {
        inputType = InputType.TYPE_CLASS_TEXT or if (multiline) {
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        } else {
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
        if (multiline) minLines = 3
    }

    private fun showError(message: Int) {
        Log.w("HabitOrganization", activity.getString(message))
        dialogBuilder()
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        private const val BULK_PICKER = "bulk-skip-dates"
        private const val BULK_HABITS = "bulk-skip-habit-ids"
    }

    private fun dialogBuilder() = MaterialAlertDialogBuilder(activity, R.style.HabitControlsDialogTheme)
}
