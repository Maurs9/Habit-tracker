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
package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.preferences.SavedHabitFilter
import org.isoron.uhabits.core.ui.ThemeSwitcher
import javax.inject.Inject

class ListHabitsMenuBehavior @Inject constructor(
    private val screen: Screen,
    private val adapter: Adapter,
    private val preferences: Preferences,
    private val themeSwitcher: ThemeSwitcher
) {
    private var showCompleted: Boolean
    private var showArchived: Boolean

    fun onCreateHabit() {
        screen.showCreateHabitScreen()
    }

    fun onViewFAQ() {
        screen.showFAQScreen()
    }

    fun onViewAbout() {
        screen.showAboutScreen()
    }

    fun onViewSettings() {
        screen.showSettingsScreen()
    }

    fun onToggleShowArchived() {
        showArchived = !showArchived
        preferences.showArchived = showArchived
        updateAdapterFilter()
    }

    fun onToggleShowCompleted() {
        showCompleted = !showCompleted
        preferences.showCompleted = showCompleted
        updateAdapterFilter()
    }

    fun onSortByManually() {
        adapter.primaryOrder = HabitList.Order.BY_POSITION
    }

    fun onToggleGroupBySection() {
        val enabled = !preferences.groupBySection
        preferences.groupBySection = enabled
        adapter.groupBySection = enabled
    }

    fun onSortByColor() {
        onSortToggleBy(HabitList.Order.BY_COLOR_ASC, HabitList.Order.BY_COLOR_DESC)
    }

    fun onSortByScore() {
        onSortToggleBy(HabitList.Order.BY_SCORE_DESC, HabitList.Order.BY_SCORE_ASC)
    }

    fun onSortByName() {
        onSortToggleBy(HabitList.Order.BY_NAME_ASC, HabitList.Order.BY_NAME_DESC)
    }

    fun onSortByStatus() {
        onSortToggleBy(HabitList.Order.BY_STATUS_ASC, HabitList.Order.BY_STATUS_DESC)
    }

    private fun onSortToggleBy(defaultOrder: HabitList.Order, reversedOrder: HabitList.Order) {
        if (adapter.primaryOrder != defaultOrder) {
            if (adapter.primaryOrder != reversedOrder) {
                adapter.secondaryOrder = adapter.primaryOrder
            }
            adapter.primaryOrder = defaultOrder
        } else {
            adapter.primaryOrder = reversedOrder
        }
    }

    fun onToggleNightMode() {
        themeSwitcher.toggleNightMode()
        screen.applyTheme()
    }

    fun onPreferencesChanged() {
        showCompleted = preferences.showCompleted
        showArchived = preferences.showArchived
        adapter.groupBySection = preferences.groupBySection
        updateAdapterFilter()
    }

    fun onFilterTags(tags: Set<String>) {
        preferences.selectedTags = HabitTags.normalize(tags)
        updateAdapterFilter()
    }

    fun onClearFilters() {
        showArchived = true
        showCompleted = true
        preferences.showArchived = true
        preferences.showCompleted = true
        preferences.selectedTags = emptySet()
        updateAdapterFilter()
    }

    fun currentFilter(name: String) = SavedHabitFilter(
        name.trim(),
        preferences.selectedTags,
        showArchived,
        showCompleted,
        adapter.primaryOrder,
        adapter.secondaryOrder,
        preferences.groupBySection
    )

    fun onApplySavedFilter(filter: SavedHabitFilter) {
        preferences.showArchived = filter.showArchived
        preferences.showCompleted = filter.showCompleted
        preferences.selectedTags = filter.tags
        preferences.groupBySection = filter.groupBySection
        adapter.primaryOrder = filter.primaryOrder
        adapter.secondaryOrder = filter.secondaryOrder
        onPreferencesChanged()
    }

    private fun updateAdapterFilter() {
        if (preferences.areQuestionMarksEnabled) {
            adapter.setFilter(
                HabitMatcher(
                    isArchivedAllowed = showArchived,
                    isEnteredAllowed = showCompleted,
                    requiredTags = preferences.selectedTags
                )
            )
        } else {
            adapter.setFilter(
                HabitMatcher(
                    isArchivedAllowed = showArchived,
                    isCompletedAllowed = showCompleted,
                    requiredTags = preferences.selectedTags
                )
            )
        }
        adapter.refresh()
    }

    interface Adapter {
        fun refresh()
        fun setFilter(matcher: HabitMatcher)
        var primaryOrder: HabitList.Order
        var secondaryOrder: HabitList.Order
        var groupBySection: Boolean
    }

    interface Screen {
        fun applyTheme()
        fun showAboutScreen()
        fun showFAQScreen()
        fun showSettingsScreen()
        fun showCreateHabitScreen()
    }

    init {
        showCompleted = preferences.showCompleted
        showArchived = preferences.showArchived
        updateAdapterFilter()
    }
}
