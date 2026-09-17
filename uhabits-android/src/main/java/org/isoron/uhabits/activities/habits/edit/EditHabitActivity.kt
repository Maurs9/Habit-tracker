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

package org.isoron.uhabits.activities.habits.edit

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.format.DateFormat
import android.text.method.DigitsKeyListener
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.HabitsActivity
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialog
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialogFactory
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.common.dialogs.SectionDialogs
import org.isoron.uhabits.activities.common.dialogs.TagPickerDialog
import org.isoron.uhabits.activities.common.dialogs.WeekdayPickerDialog
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.utils.formatEditableNumber
import org.isoron.uhabits.core.utils.parseFiniteNumber
import org.isoron.uhabits.databinding.ActivityEditHabitBinding
import org.isoron.uhabits.utils.ColorUtils.contrastingTextColor
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.isoron.uhabits.utils.formatTime
import org.isoron.uhabits.utils.toFormattedString

fun formatFrequency(freqNum: Int, freqDen: Int, resources: Resources) = when {
    freqNum == 1 && (freqDen == 30 || freqDen == 31) -> resources.getString(R.string.every_month)
    freqDen == 30 || freqDen == 31 -> resources.getString(R.string.x_times_per_month, freqNum)
    freqNum == 1 && freqDen == 1 -> resources.getString(R.string.every_day)
    freqNum == 1 && freqDen == 7 -> resources.getString(R.string.every_week)
    freqNum == 1 && freqDen > 1 -> resources.getString(R.string.every_x_days, freqDen)
    freqDen == 7 -> resources.getString(R.string.x_times_per_week, freqNum)
    else -> resources.getString(R.string.x_times_per_y_days, freqNum, freqDen)
}

class EditHabitActivity : HabitsActivity() {

    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private lateinit var binding: ActivityEditHabitBinding
    private lateinit var commandRunner: CommandRunner
    private lateinit var sectionDialogs: SectionDialogs

    var habitId = -1L
    var sectionId: Long? = null
    var tags: Set<String> = emptySet()
    lateinit var habitType: HabitType
    var unit = ""
    var color = PaletteColor.DEFAULT
    var androidColor = 0
    var freqNum = 1
    var freqDen = 1
    var reminderHour = -1
    var reminderMin = -1
    var reminderDays: WeekdayList = WeekdayList.EVERY_DAY
    var targetType = NumericalHabitType.AT_LEAST
    private var validatedTarget = 0.0
    private var moreExpanded = false
    private lateinit var initialDraft: HabitEditorDraft

    protected override fun onCreateReady(state: Bundle?) {
        val component = (application as HabitsApplication).component
        sectionDialogs = SectionDialogs(this, component.sectionList)
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()

        binding = ActivityEditHabitBinding.inflate(layoutInflater)
        binding.root.applyRootViewInsets()
        binding.toolbar.applyToolbarInsets()
        setContentView(binding.root)
        binding.targetInput.keyListener = DigitsKeyListener.getInstance(resources.configuration.locales[0], false, true)

        if (intent.hasExtra("habitId")) {
            binding.toolbar.title = getString(R.string.edit_habit)
            habitId = intent.getLongExtra("habitId", -1)
            val habit = component.habitList.getById(habitId)!!
            habitType = habit.type
            moreExpanded = habit.question.isNotBlank() || habit.description.isNotBlank()
            sectionId = habit.sectionId
            color = habit.color
            freqNum = habit.frequency.numerator
            freqDen = habit.frequency.denominator
            targetType = habit.targetType
            habit.reminder?.let {
                reminderHour = it.hour
                reminderMin = it.minute
                reminderDays = it.days
            }
            binding.nameInput.setText(habit.name)
            binding.questionInput.setText(habit.question)
            binding.notesInput.setText(habit.description)
            tags = habit.tags
            binding.unitInput.setText(habit.unit)
            binding.targetInput.setText(formatEditableNumber(habit.targetValue, resources.configuration.locales[0]))
        } else {
            color = PaletteColor(
                component.preferences.getDefaultHabitColor(PaletteColor.DEFAULT.paletteIndex)
                    .coerceIn(0, PaletteColor.COUNT - 1)
            )
            habitType = HabitType.fromInt(intent.getIntExtra("habitType", HabitType.YES_NO.value))
        }

        @Suppress("DEPRECATION")
        val restoredInitialDraft = state?.getSerializable(INITIAL_DRAFT) as? HabitEditorDraft
        initialDraft = restoredInitialDraft ?: currentDraft()

        if (state != null) {
            habitId = state.getLong("habitId")
            sectionId = state.getLong("sectionId", -1).takeIf { it >= 0 }
            tags = HabitTags.normalize(state.getStringArrayList("tags").orEmpty())
            habitType = HabitType.fromInt(state.getInt("habitType"))
            color = PaletteColor(state.getInt("paletteColor"))
            freqNum = state.getInt("freqNum")
            freqDen = state.getInt("freqDen")
            reminderHour = state.getInt("reminderHour")
            reminderMin = state.getInt("reminderMin")
            reminderDays = WeekdayList(state.getInt("reminderDays"))
            targetType = NumericalHabitType.fromInt(state.getInt("targetType", targetType.value))
            moreExpanded = state.getBoolean("moreExpanded", moreExpanded)
        }

        updateColors()
        applyHabitType()
        binding.typeOuterBox.visibility = if (habitId < 0) View.VISIBLE else View.GONE
        binding.typeToggle.check(if (habitType == HabitType.YES_NO) R.id.typeYesNo else R.id.typeMeasurable)
        binding.typeToggle.addOnButtonCheckedListener { _, id, checked ->
            if (checked && habitId < 0) {
                val newType = if (id == R.id.typeMeasurable) HabitType.NUMERICAL else HabitType.YES_NO
                if (newType != habitType) {
                    habitType = newType
                    applyHabitType()
                    populateFrequency()
                }
            }
        }
        populateMoreOptions()
        binding.moreToggle.setOnClickListener {
            moreExpanded = !moreExpanded
            populateMoreOptions()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.elevation = 10.0f

        val colorPickerDialogFactory = ColorPickerDialogFactory(this)
        supportFragmentManager.setFragmentResultListener(ColorPickerDialog.REQUEST_KEY, this) { _, result ->
            color = PaletteColor(result.getInt(ColorPickerDialog.SELECTED))
            updateColors()
        }
        binding.colorButton.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("colorPicker") == null) {
                colorPickerDialogFactory.create(color, themeSwitcher.currentTheme)
                    .show(supportFragmentManager, "colorPicker")
            }
        }

        populateFrequency()
        supportFragmentManager.setFragmentResultListener(FrequencyPickerDialog.REQUEST_KEY, this) { _, result ->
            freqNum = result.getInt(FrequencyPickerDialog.NUMERATOR)
            freqDen = result.getInt(FrequencyPickerDialog.DENOMINATOR)
            populateFrequency()
        }
        binding.booleanFrequencyPicker.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("frequencyPicker") == null) {
                FrequencyPickerDialog(freqNum, freqDen).show(supportFragmentManager, "frequencyPicker")
            }
        }

        populateTargetType()
        binding.targetTypePicker.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this, R.style.HabitControlsDialogTheme)
            val choices = arrayOf(getString(R.string.target_type_at_least), getString(R.string.target_type_at_most))
            builder.setTitle(R.string.target_type)
            builder.setSingleChoiceItems(choices, targetType.value) { dialog, which ->
                targetType = when (which) {
                    0 -> NumericalHabitType.AT_LEAST
                    else -> NumericalHabitType.AT_MOST
                }
                populateTargetType()
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.dismissCurrentAndShow()
        }

        populateReminder()
        populateTags()
        supportFragmentManager.setFragmentResultListener(TagPickerDialog.REQUEST_KEY, this) { _, result ->
            if (result.getLong(TagPickerDialog.HABIT_ID, -1) == habitId) {
                tags = HabitTags.normalize(result.getStringArrayList(TagPickerDialog.TAGS).orEmpty())
                populateTags()
            }
        }
        binding.tagsPicker.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(TagPickerDialog.FRAGMENT_TAG) == null) {
                TagPickerDialog.newInstance(habitId, tags, HabitTags.union(component.habitList, tags))
                    .show(supportFragmentManager, TagPickerDialog.FRAGMENT_TAG)
            }
        }
        populateSection()
        binding.sectionPicker.setOnClickListener {
            sectionDialogs.selectSection(sectionId) { id ->
                sectionId = id
                populateSection()
            }
        }
        binding.reminderTimePicker.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("timePicker") != null) return@setOnClickListener
            val currentHour = if (reminderHour >= 0) reminderHour else 8
            val currentMin = if (reminderMin >= 0) reminderMin else 0
            val is24HourMode = DateFormat.is24HourFormat(this)
            val dialog = MaterialTimePicker.Builder()
                .setTheme(R.style.HabitControlsTimePickerTheme)
                .setTitleText(R.string.reminder)
                .setTimeFormat(if (is24HourMode) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(currentHour)
                .setMinute(currentMin)
                .setNegativeButtonText(R.string.clear)
                .build()
            bindReminderTimePicker(dialog)
            dialog.show(supportFragmentManager, "timePicker")
        }

        supportFragmentManager.setFragmentResultListener(WeekdayPickerDialog.REQUEST_KEY, this) { _, result ->
            reminderDays = WeekdayList(result.getInt(WeekdayPickerDialog.DAYS))
            if (reminderDays.isEmpty) reminderDays = WeekdayList.EVERY_DAY
            populateReminder()
        }
        binding.reminderDatePicker.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag("dayPicker") == null) {
                WeekdayPickerDialog().apply {
                    setSelectedDays(reminderDays)
                }.show(supportFragmentManager, "dayPicker")
            }
        }

        binding.buttonSave.setOnClickListener {
            if (validate()) save()
        }
        supportFragmentManager.setFragmentResultListener(DiscardHabitChangesDialog.REQUEST_KEY, this) { _, _ -> finish() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = requestExit()
            }
        )
    }

    protected override fun onStartReady() {
        (supportFragmentManager.findFragmentByTag("timePicker") as? MaterialTimePicker)?.let { bindReminderTimePicker(it) }
    }

    private fun bindReminderTimePicker(dialog: MaterialTimePicker) {
        dialog.clearOnPositiveButtonClickListeners()
        dialog.clearOnNegativeButtonClickListeners()
        dialog.addOnPositiveButtonClickListener {
            reminderHour = dialog.hour
            reminderMin = dialog.minute
            populateReminder()
        }
        dialog.addOnNegativeButtonClickListener {
            reminderHour = -1
            reminderMin = -1
            reminderDays = WeekdayList.EVERY_DAY
            populateReminder()
        }
    }

    private fun save() {
        val component = (application as HabitsApplication).component
        val habit = component.modelFactory.buildHabit()

        var original: Habit? = null
        if (habitId >= 0) {
            original = component.habitList.getById(habitId)!!
            habit.copyFrom(original)
        }

        habit.name = binding.nameInput.text.trim().toString()
        habit.question = binding.questionInput.text.trim().toString()
        habit.description = binding.notesInput.text.trim().toString()
        habit.tags = tags
        habit.sectionId = sectionId?.let { component.sectionList.getById(it)?.id }
        habit.color = color
        if (reminderHour >= 0) {
            habit.reminder = Reminder(reminderHour, reminderMin, reminderDays)
        } else {
            habit.replaceReminderTimes(emptyList())
        }

        habit.frequency = Frequency(freqNum, freqDen)
        if (habitType == HabitType.NUMERICAL) {
            habit.targetValue = validatedTarget
            habit.targetType = targetType
            habit.unit = binding.unitInput.text.trim().toString()
        }
        habit.type = habitType

        val command = if (habitId >= 0) {
            EditHabitCommand(
                component.habitList,
                habitId,
                habit
            )
        } else {
            CreateHabitCommand(
                component.modelFactory,
                component.habitList,
                habit
            )
        }
        component.commandRunner.run(command)
        if (habitId < 0) component.preferences.setDefaultHabitColor(color.paletteIndex)
        finish()
    }

    private fun validate(): Boolean {
        var isValid = true
        binding.nameInput.error = null
        binding.targetInput.error = null
        if (binding.nameInput.text.isBlank()) {
            binding.nameInput.error = getFormattedValidationError(R.string.validation_cannot_be_blank)
            isValid = false
        }
        if (habitType == HabitType.NUMERICAL) {
            val text = binding.targetInput.text.toString()
            val target = parseFiniteNumber(text, resources.configuration.locales[0])
            val error = when {
                text.isBlank() -> R.string.validation_cannot_be_blank
                target == null -> R.string.habit_number_invalid
                target < 0 -> R.string.habit_number_nonnegative
                else -> null
            }
            if (error != null) {
                binding.targetInput.error = getString(error)
                isValid = false
            } else {
                validatedTarget = target!!
            }
        }
        if (!isValid) {
            if (binding.nameInput.error != null) binding.nameInput.requestFocus() else binding.targetInput.requestFocus()
        }
        return isValid
    }

    private fun populateReminder() {
        if (reminderHour < 0) {
            binding.reminderTimePicker.text = getString(R.string.reminder_off)
            binding.reminderDatePicker.visibility = View.GONE
            binding.reminderDivider.visibility = View.GONE
        } else {
            val time = formatTime(this, reminderHour, reminderMin)
            binding.reminderTimePicker.text = time
            binding.reminderDatePicker.visibility = View.VISIBLE
            binding.reminderDivider.visibility = View.VISIBLE
            binding.reminderDatePicker.text = reminderDays.toFormattedString(this)
        }
    }

    private fun populateTags() {
        binding.tagsPicker.text = HabitTags.formatInline(tags).ifEmpty { getString(R.string.habit_tags_none) }
    }

    private fun applyHabitType() {
        val numerical = habitType == HabitType.NUMERICAL
        binding.unitOuterBox.visibility = if (numerical) View.VISIBLE else View.GONE
        binding.targetOuterBox.visibility = binding.unitOuterBox.visibility
        binding.targetTypeOuterBox.visibility = binding.unitOuterBox.visibility
        binding.frequencyOuterBox.visibility = View.VISIBLE
        binding.nameInput.setHint(if (numerical) R.string.measurable_short_example else R.string.yes_or_no_short_example)
        binding.questionInput.setHint(if (numerical) R.string.measurable_question_example else R.string.example_question_boolean)
        if (!numerical) binding.targetInput.error = null
    }

    private fun populateMoreOptions() {
        if (!moreExpanded && binding.moreGroup.hasFocus()) binding.moreToggle.requestFocus()
        binding.moreGroup.visibility = if (moreExpanded) View.VISIBLE else View.GONE
        binding.moreToggle.setText(if (moreExpanded) R.string.fewer_options else R.string.more_options)
    }

    private fun populateSection() {
        val sections = (application as HabitsApplication).component.sectionList
        val section = sectionId?.let { sections.getById(it) }
        sectionId = section?.id
        binding.sectionPicker.text = section?.name ?: getString(R.string.section_none)
    }

    protected override fun onDestroyReady() {
        sectionDialogs.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        requestExit()
        return true
    }

    private fun requestExit() {
        if (!isContentReady || currentDraft() == initialDraft) {
            finish()
        } else if (supportFragmentManager.findFragmentByTag(DiscardHabitChangesDialog.TAG) == null) {
            DiscardHabitChangesDialog().showNow(supportFragmentManager, DiscardHabitChangesDialog.TAG)
        }
    }

    private fun currentDraft(): HabitEditorDraft {
        val sections = (application as HabitsApplication).component.sectionList
        return HabitEditorDraft(
            name = binding.nameInput.text.toString(),
            question = binding.questionInput.text.toString(),
            notes = binding.notesInput.text.toString(),
            type = habitType.value,
            unit = binding.unitInput.text.toString(),
            target = binding.targetInput.text.toString(),
            targetType = targetType.value,
            color = color.paletteIndex,
            numerator = freqNum,
            denominator = freqDen,
            reminderHour = reminderHour,
            reminderMinute = reminderMin,
            reminderDays = reminderDays.toInteger(),
            tags = tags,
            sectionId = sectionId?.takeIf { sections.getById(it) != null }
        ).normalized(resources.configuration.locales[0])
    }

    @SuppressLint("StringFormatMatches")
    private fun populateFrequency() {
        binding.booleanFrequencyPicker.text = formatFrequency(freqNum, freqDen, resources)
    }

    private fun populateTargetType() {
        binding.targetTypePicker.text = when (targetType) {
            NumericalHabitType.AT_MOST -> getString(R.string.target_type_at_most)
            else -> getString(R.string.target_type_at_least)
        }
    }

    private fun updateColors() {
        androidColor = themeSwitcher.currentTheme.color(color).toInt()
        binding.colorButton.backgroundTintList = ColorStateList.valueOf(androidColor)
        binding.colorButton.contentDescription = getString(
            R.string.habit_color_description,
            resources.getStringArray(R.array.habit_color_names)[color.paletteIndex]
        )
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        for (button in listOf(binding.typeYesNo, binding.typeMeasurable)) {
            button.backgroundTintList = ColorStateList(states, intArrayOf(androidColor, Color.TRANSPARENT))
            button.setTextColor(
                ColorStateList(states, intArrayOf(contrastingTextColor(androidColor), themeSwitcher.currentTheme.highContrastTextColor.toInt()))
            )
            button.strokeColor = ColorStateList.valueOf(androidColor)
        }
        if (!themeSwitcher.isNightMode) {
            window.statusBarColor = androidColor
            binding.toolbar.setBackgroundColor(androidColor)
        }
    }

    private fun getFormattedValidationError(@StringRes resId: Int): Spanned {
        val html = "<font color=#FFFFFF>${getString(resId)}</font>"
        return Html.fromHtml(html)
    }

    protected override fun onSaveInstanceStateReady(state: Bundle) {
        with(state) {
            putSerializable(INITIAL_DRAFT, initialDraft)
            putLong("habitId", habitId)
            putLong("sectionId", sectionId ?: -1)
            putStringArrayList("tags", ArrayList(tags))
            putInt("habitType", habitType.value)
            putInt("paletteColor", color.paletteIndex)
            putInt("androidColor", androidColor)
            putInt("freqNum", freqNum)
            putInt("freqDen", freqDen)
            putInt("reminderHour", reminderHour)
            putInt("reminderMin", reminderMin)
            putInt("reminderDays", reminderDays.toInteger())
            putInt("targetType", targetType.value)
            putBoolean("moreExpanded", moreExpanded)
        }
    }

    companion object {
        private const val INITIAL_DRAFT = "initialHabitDraft"
    }
}
