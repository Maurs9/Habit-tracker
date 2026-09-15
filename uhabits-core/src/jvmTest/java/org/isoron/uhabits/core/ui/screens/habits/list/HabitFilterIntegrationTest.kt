package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.preferences.PropertiesStorage
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitFilterIntegrationTest {
    @Test
    fun savesAndRestoresTagsVisibilityAndOrdering() {
        val file = File.createTempFile("habit-filter", ".properties")
        try {
            val preferences = Preferences(PropertiesStorage(file))
            val adapter = TestAdapter()
            val behavior = ListHabitsMenuBehavior(mock(), adapter, preferences, mock())
            behavior.onFilterTags(setOf("Morning", "Health"))
            behavior.onToggleShowArchived()
            behavior.onToggleShowCompleted()
            behavior.onSortByName()
            val saved = behavior.currentFilter("Morning routine")
            preferences.savedHabitFilters = listOf(saved)

            val restoredPreferences = Preferences(PropertiesStorage(file))
            val restoredAdapter = TestAdapter()
            val restored = ListHabitsMenuBehavior(mock(), restoredAdapter, restoredPreferences, mock())
            restored.onApplySavedFilter(restoredPreferences.savedHabitFilters.single())

            assertEquals(setOf("Morning", "Health"), restoredAdapter.matcher.requiredTags)
            assertTrue(restoredAdapter.matcher.isArchivedAllowed)
            assertFalse(restoredAdapter.matcher.isCompletedAllowed)
            assertEquals(HabitList.Order.BY_NAME_ASC, restoredAdapter.primaryOrder)

            restored.onFilterTags(emptySet())
            assertTrue(restoredAdapter.matcher.requiredTags.isEmpty())
            assertTrue(restoredAdapter.matcher.isArchivedAllowed)
            assertFalse(restoredAdapter.matcher.isCompletedAllowed)
        } finally {
            file.delete()
        }
    }

    private class TestAdapter : ListHabitsMenuBehavior.Adapter {
        var matcher = HabitMatcher()
        override var primaryOrder = HabitList.Order.BY_POSITION
        override var secondaryOrder = HabitList.Order.BY_NAME_ASC
        override fun refresh() = Unit
        override fun setFilter(matcher: HabitMatcher) {
            this.matcher = matcher
        }
    }
}
