package org.isoron.uhabits.activities.common.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.utils.formatEditableNumber
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.currentDialogFragment
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.TimeZone

abstract class EntryDialogFragment : AppCompatDialogFragment() {
    protected val appComponent
        get() = (requireActivity().application as HabitsApplication).component

    protected fun bindContext(view: CheckmarkPopupBinding, numerical: Boolean) {
        val habit = currentHabit()
        val date = arguments?.takeIf { it.containsKey(DATE) }?.getLong(DATE)?.let(::Timestamp)
        view.entryContext.text = if (habit == null || date == null) {
            getString(R.string.entry_habit_unavailable)
        } else {
            val format = DateFormat.getDateInstance(DateFormat.FULL, resources.configuration.locales[0]).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            getString(R.string.entry_context, habit.name, format.format(date.toJavaDate()))
        }
        if (numerical && habit != null) {
            val valueLabel = if (habit.unit.isBlank()) {
                getString(R.string.entry_value_without_unit)
            } else {
                getString(R.string.entry_value_label, habit.unit)
            }
            val target = formatEditableNumber(habit.targetValue, resources.configuration.locales[0])
            val measurement = if (habit.unit.isBlank()) target else getString(R.string.habit_entry_value, target, habit.unit)
            view.entryDetails.text = getString(
                if (habit.targetType == NumericalHabitType.AT_MOST) {
                    R.string.entry_target_at_most
                } else {
                    R.string.entry_target_at_least
                },
                measurement
            )
            view.entryDetails.visibility = View.VISIBLE
            view.entryDetails.labelFor = R.id.value
            view.value.hint = valueLabel
        }
    }

    protected fun submitEntry(value: Int, notes: String, callback: (() -> Unit)?): Boolean {
        val habit = currentHabit() ?: return false
        val date = arguments?.takeIf { it.containsKey(DATE) }?.getLong(DATE)?.let(::Timestamp) ?: return false
        // Live callbacks retain optional feedback; restored fragments rebind submission by stable identity.
        if (callback != null) {
            callback()
        } else {
            appComponent.commandRunner.run(CreateRepetitionCommand(appComponent.habitList, habit, date, value, notes))
        }
        return true
    }

    private fun currentHabit(): Habit? =
        arguments?.takeIf { it.containsKey(HABIT) }?.getLong(HABIT)?.let { appComponent.habitList.getById(it) }

    override fun onStart() {
        super.onStart()
        currentDialogFragment = WeakReference(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (currentDialogFragment.get() === this) currentDialogFragment = WeakReference(null)
        super.onDismiss(dialog)
    }

    companion object {
        const val HABIT = "entryHabitId"
        const val DATE = "entryTimestamp"
        const val DRAFT_NOTES = "draftNotes"
        const val DRAFT_VALUE = "draftValue"

        fun arguments(habit: Habit, timestamp: Timestamp, color: Int): Bundle = Bundle().apply {
            putLong(HABIT, requireNotNull(habit.id))
            putLong(DATE, timestamp.unixTime)
            putInt("color", color)
        }
    }
}
