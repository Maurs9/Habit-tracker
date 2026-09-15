package org.isoron.uhabits.core.preferences

import org.isoron.uhabits.core.models.HabitList
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavedHabitFilterTest {
    @Test
    fun roundTripsNamesTagsAndSorting() {
        val filter = SavedHabitFilter(
            "Morning & evening\tfilter",
            setOf("Health", "Work + life"),
            true,
            false,
            HabitList.Order.BY_NAME_DESC,
            HabitList.Order.BY_COLOR_ASC
        )
        assertEquals(filter, SavedHabitFilter.decode(filter.encode()))
    }

    @Test
    fun rejectsMalformedData() {
        assertFailsWith<IllegalArgumentException> { SavedHabitFilter.decode("broken") }
    }

    @Test
    fun persistsSavedAndActiveFilters() {
        val file = File.createTempFile("saved-filters", ".properties")
        try {
            val prefs = Preferences(PropertiesStorage(file))
            val filter = SavedHabitFilter(
                "Morning",
                setOf("Health"),
                false,
                true,
                HabitList.Order.BY_POSITION,
                HabitList.Order.BY_NAME_ASC
            )
            prefs.selectedTags = filter.tags
            prefs.savedHabitFilters = listOf(filter)
            val restored = Preferences(PropertiesStorage(file))
            assertEquals(filter.tags, restored.selectedTags)
            assertEquals(listOf(filter), restored.savedHabitFilters)
        } finally {
            file.delete()
        }
    }
}
