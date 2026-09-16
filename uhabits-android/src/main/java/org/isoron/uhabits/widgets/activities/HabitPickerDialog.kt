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

package org.isoron.uhabits.widgets.activities

import android.app.Activity
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.SectionList
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.widgets.WidgetUpdater

class BooleanHabitPickerDialog : HabitPickerDialog() {
    override fun shouldHideNumerical() = true
    override fun getEmptyMessage() = R.string.no_boolean_habits
}

class NumericalHabitPickerDialog : HabitPickerDialog() {
    override fun shouldHideBoolean() = true
    override fun getEmptyMessage() = R.string.no_numerical_habits
}

open class HabitPickerDialog : Activity() {

    private var widgetId = 0
    private lateinit var habitList: HabitList
    private lateinit var sectionList: SectionList
    private lateinit var widgetPreferences: WidgetPreferences
    private lateinit var widgetUpdater: WidgetUpdater
    private lateinit var listView: ListView
    private lateinit var saveButton: Button
    private var habitIds = emptyList<Long>()
    private val checkedIds = mutableSetOf<Long>()
    private var selectionChanged = false

    protected open fun shouldHideNumerical() = false
    protected open fun shouldHideBoolean() = false
    protected open fun getEmptyMessage() = R.string.no_habits

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val component = (applicationContext as HabitsApplication).component
        AndroidThemeSwitcher(this, component.preferences).apply()
        habitList = component.habitList
        sectionList = component.sectionList
        widgetPreferences = component.widgetPreferences
        widgetUpdater = component.widgetUpdater
        widgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (widgetId == INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.widget_configure_activity)
        listView = findViewById(R.id.listView)
        saveButton = findViewById(R.id.buttonSave)
        checkedIds.addAll(savedInstanceState?.getLongArray(STATE_CHECKED_IDS)?.toList().orEmpty())
        selectionChanged = savedInstanceState?.getBoolean(STATE_SELECTION_CHANGED) ?: false

        with(listView) {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            // Restore our stable ids, not ListView's saved adapter positions.
            isSaveEnabled = false
            emptyView = findViewById<TextView>(R.id.message).apply { setText(getEmptyMessage()) }
            setOnItemClickListener { _, _, position, _ ->
                val id = habitIds[position]
                if (isItemChecked(position)) checkedIds.add(id) else checkedIds.remove(id)
                updateSaveButton()
            }
        }
        saveButton.setOnClickListener { confirm(habitIds.filter { it in checkedIds }) }
        findViewById<Button>(R.id.buttonCancel).setOnClickListener { finish() }
        refreshHabits()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray(STATE_CHECKED_IDS, checkedIds.toLongArray())
        outState.putBoolean(STATE_SELECTION_CHANGED, selectionChanged)
        super.onSaveInstanceState(outState)
    }

    private fun isEligible(habit: Habit): Boolean =
        !habit.isArchived &&
            !(habit.isNumerical && shouldHideNumerical()) &&
            !(!habit.isNumerical && shouldHideBoolean())

    private fun refreshHabits() {
        val habits = habitList.filter { isEligible(it) }
        habitIds = habits.map { it.id!! }
        if (checkedIds.retainAll(habitIds.toSet())) selectionChanged = true
        listView.adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            habits.map { it.name }
        ) {
            override fun getItemId(position: Int) = habitIds[position]
            override fun hasStableIds() = true
        }
        applyChecks()
        findViewById<TextView>(R.id.widgetPickerError).apply {
            setText(R.string.widget_selection_changed)
            visibility = if (selectionChanged) View.VISIBLE else View.GONE
        }

        val sections = sectionList.getAll()
        findViewById<View>(R.id.sectionShortcuts).visibility =
            if (sections.isEmpty()) View.GONE else View.VISIBLE
        val shortcuts = findViewById<LinearLayout>(R.id.sectionShortcutButtons)
        shortcuts.removeAllViews()
        for (section in sections) {
            shortcuts.addView(
                Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                    text = section.name
                    isAllCaps = false
                    contentDescription = getString(R.string.widget_select_section, section.name)
                    isEnabled = habits.any { it.sectionId == section.id }
                    setOnClickListener {
                        for (id in habitIds) {
                            val habit = habitList.getById(id) ?: continue
                            if (isEligible(habit) && habit.sectionId == section.id) checkedIds.add(id)
                        }
                        applyChecks()
                    }
                }
            )
        }
    }

    private fun applyChecks() {
        listView.clearChoices()
        habitIds.forEachIndexed { position, id -> listView.setItemChecked(position, id in checkedIds) }
        updateSaveButton()
    }

    private fun updateSaveButton() {
        saveButton.isEnabled = checkedIds.isNotEmpty()
    }

    fun confirm(selectedIds: List<Long>) {
        if (selectedIds.isEmpty()) return
        if (selectedIds.any { id -> habitList.getById(id)?.let { isEligible(it) } != true }) {
            selectionChanged = true
            refreshHabits()
            return
        }
        widgetPreferences.addWidget(widgetId, selectedIds.toLongArray())
        widgetUpdater.updateWidgets()
        setResult(
            RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_APPWIDGET_ID, widgetId)
            }
        )
        finish()
    }

    companion object {
        private const val STATE_CHECKED_IDS = "checkedHabitIds"
        private const val STATE_SELECTION_CHANGED = "selectionChanged"
    }
}
