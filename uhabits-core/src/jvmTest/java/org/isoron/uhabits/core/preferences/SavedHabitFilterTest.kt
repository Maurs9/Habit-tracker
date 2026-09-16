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
            HabitList.Order.BY_COLOR_ASC,
            groupBySection = true
        )
        assertEquals(filter, SavedHabitFilter.decode(filter.encode()))
    }

    @Test
    fun rejectsMalformedData() {
        assertFailsWith<IllegalArgumentException> { SavedHabitFilter.decode("broken") }
        assertFailsWith<IllegalArgumentException> {
            SavedHabitFilter.decode("Morning\t\tfalse\ttrue\tBY_POSITION\tBY_NAME_ASC\tyes")
        }
        assertFailsWith<IllegalArgumentException> {
            SavedHabitFilter.decode("Morning\t\tfalse\ttrue\tBY_POSITION\tBY_NAME_ASC\ttrue\textra")
        }
    }

    @Test
    fun decodesLegacySixFieldFiltersWithoutGrouping() {
        val legacy = "Morning\tHealth\tfalse\ttrue\tBY_POSITION\tBY_NAME_ASC"
        val decoded = SavedHabitFilter.decode(legacy)
        assertEquals(false, decoded.groupBySection)
        assertEquals(decoded, SavedHabitFilter.decode(decoded.encode()))
        assertEquals(7, decoded.encode().split('\t').size)
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
                HabitList.Order.BY_NAME_ASC,
                groupBySection = true
            )
            prefs.selectedTags = filter.tags
            prefs.groupBySection = true
            prefs.savedHabitFilters = listOf(filter)
            val restored = Preferences(PropertiesStorage(file))
            assertEquals(filter.tags, restored.selectedTags)
            assertEquals(true, restored.groupBySection)
            assertEquals(listOf(filter), restored.savedHabitFilters)
        } finally {
            file.delete()
        }
    }
}
