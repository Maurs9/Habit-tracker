package org.isoron.uhabits.notifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlarmManager
import android.app.Dialog
import android.content.Context.ALARM_SERVICE
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.ChangeReminderTimesCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.formatTime
import java.util.Calendar

class ReminderTimesDialog : DialogFragment() {
    private val times = sortedSetOf<Int>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var error: TextView
    private lateinit var empty: TextView
    private var editingTime = -1
    private var saveRequested = false
    private val notificationPermission = registerForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            component.reminderScheduler.scheduleAll()
            finishSaving()
        } else {
            Toast.makeText(requireContext(), R.string.reminder_times_notifications_disabled, Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    private val component
        get() = (requireContext().applicationContext as HabitsApplication).component

    private val habitId: Long
        get() = requireArguments().getLong(HABIT_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val draft = savedInstanceState?.getIntArray(DRAFT)
        if (draft != null) {
            times.addAll(draft.toList())
        } else {
            component.habitList.getById(habitId)?.let { times.addAll(it.reminderTimes) }
        }
        editingTime = savedInstanceState?.getInt(EDITING_TIME, -1) ?: -1
        saveRequested = savedInstanceState?.getBoolean(SAVE_REQUESTED, false) ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
        }
        val habit = component.habitList.getById(habitId)
        content.addView(
            TextView(context).apply {
                text = getString(R.string.reminder_times_shared_days, weekdayLabel(habit))
            }
        )
        content.addView(TextView(context).apply { setText(R.string.reminder_times_snooze_policy) })
        empty = TextView(context).apply { setText(R.string.reminder_times_empty) }
        content.addView(empty)
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, mutableListOf())
        val list = ListView(context).apply {
            adapter = this@ReminderTimesDialog.adapter
            setOnItemClickListener { _, _, position, _ -> openTimePicker(times.elementAt(position)) }
        }
        content.addView(list, LinearLayout.LayoutParams(-1, (240 * resources.displayMetrics.density).toInt()))
        error = TextView(context).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            visibility = View.GONE
        }
        content.addView(error)
        refreshTimes()
        return MaterialAlertDialogBuilder(context, R.style.HabitControlsDialogTheme)
            .setTitle(R.string.reminder_times_title)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.reminder_times_add, null)
            .setPositiveButton(R.string.save, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        if (saveRequested && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(requireContext(), POST_NOTIFICATIONS) == PERMISSION_GRANTED
            )
        ) {
            finishSaving()
            return
        }
        val dialog = requireDialog() as AlertDialog
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { openTimePicker(-1) }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = !saveRequested
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !saveRequested
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (saveRequested) return@setOnClickListener
            val habit = component.habitList.getById(habitId)
            when {
                habit == null -> showError(R.string.reminder_habit_unavailable)
                times.any { it !in 0 until 24 * 60 } -> showError(R.string.reminder_times_invalid)
                times.isNotEmpty() && habit.reminder?.days?.isEmpty == true ->
                    showError(R.string.reminder_times_no_weekdays)
                else -> {
                    saveRequested = true
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = false
                    component.commandRunner.run(ChangeReminderTimesCommand(component.habitList, habitId, times))
                    if (times.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        checkSelfPermission(requireContext(), POST_NOTIFICATIONS) != PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(POST_NOTIFICATIONS)
                    } else {
                        finishSaving()
                    }
                }
            }
        }
        (childFragmentManager.findFragmentByTag(TIME_PICKER) as? MaterialTimePicker)?.let { bindTimePicker(it) }
        if (component.habitList.getById(habitId) == null) showError(R.string.reminder_habit_unavailable)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(DRAFT, times.toIntArray())
        outState.putInt(EDITING_TIME, editingTime)
        outState.putBoolean(SAVE_REQUESTED, saveRequested)
        super.onSaveInstanceState(outState)
    }

    private fun openTimePicker(time: Int) {
        if (saveRequested) return
        if (childFragmentManager.findFragmentByTag(TIME_PICKER) != null) return
        editingTime = time
        val initial = if (time >= 0) time else 8 * 60
        val picker = MaterialTimePicker.Builder()
            .setTheme(R.style.HabitControlsTimePickerTheme)
            .setTitleText(R.string.reminder_times_title)
            .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(initial / 60)
            .setMinute(initial % 60)
            .setNegativeButtonText(if (time >= 0) R.string.reminder_times_remove else android.R.string.cancel)
            .build()
        bindTimePicker(picker)
        picker.show(childFragmentManager, TIME_PICKER)
    }

    private fun bindTimePicker(picker: MaterialTimePicker) {
        picker.clearOnPositiveButtonClickListeners()
        picker.clearOnNegativeButtonClickListeners()
        picker.addOnPositiveButtonClickListener {
            val minute = picker.hour * 60 + picker.minute
            when {
                minute !in 0 until 24 * 60 -> showError(R.string.reminder_times_invalid)
                minute != editingTime && minute in times -> showError(R.string.reminder_times_duplicate)
                else -> {
                    times.remove(editingTime)
                    times.add(minute)
                    refreshTimes()
                }
            }
        }
        picker.addOnNegativeButtonClickListener {
            if (editingTime >= 0) {
                times.remove(editingTime)
                refreshTimes()
            }
        }
    }

    private fun refreshTimes() {
        adapter.clear()
        adapter.addAll(times.map { formatTime(requireContext(), it / 60, it % 60) })
        empty.visibility = if (times.isEmpty()) View.VISIBLE else View.GONE
        error.visibility = View.GONE
    }

    private fun showError(message: Int) {
        error.setText(message)
        error.visibility = View.VISIBLE
    }

    private fun finishSaving() {
        val alarmManager = requireContext().getSystemService(ALARM_SERVICE) as AlarmManager
        if (times.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Toast.makeText(requireContext(), R.string.reminder_times_exact_alarms_disabled, Toast.LENGTH_LONG).show()
        }
        dismiss()
    }

    private fun weekdayLabel(habit: Habit?): String {
        val names = DateUtils.getShortWeekdayNames(Calendar.SATURDAY)
        val selected = habit?.reminder?.days?.toArray() ?: BooleanArray(7) { true }
        return names.filterIndexed { index, _ -> selected[index] }.joinToString(", ")
    }

    companion object {
        private const val HABIT_ID = "habitId"
        private const val DRAFT = "reminderTimes"
        private const val EDITING_TIME = "editingTime"
        private const val TIME_PICKER = "reminderTimePicker"
        private const val SAVE_REQUESTED = "saveRequested"

        fun newInstance(habitId: Long) = ReminderTimesDialog().apply {
            arguments = Bundle().apply { putLong(HABIT_ID, habitId) }
        }
    }
}
