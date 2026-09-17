package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.HabitTags

enum class HabitListEmptyState {
    NONE,
    NO_HABITS,
    ARCHIVED,
    FILTERED,
    ENTERED,
    COMPLETED;

    companion object {
        fun classify(habits: List<Habit>, matcher: HabitMatcher): HabitListEmptyState {
            if (habits.isEmpty()) return NO_HABITS
            if (habits.any(matcher::matches)) return NONE
            val tagged = habits.filter { HabitTags.containsAll(it.tags, matcher.requiredTags) }
            if (tagged.isEmpty()) return FILTERED
            if (!matcher.isArchivedAllowed && tagged.all { it.isArchived }) return ARCHIVED
            val withoutStatus = matcher.copy(isCompletedAllowed = true, isEnteredAllowed = true)
            if (tagged.none(withoutStatus::matches)) return FILTERED
            if (!matcher.isEnteredAllowed) return ENTERED
            if (!matcher.isCompletedAllowed) return COMPLETED
            return FILTERED
        }
    }
}
