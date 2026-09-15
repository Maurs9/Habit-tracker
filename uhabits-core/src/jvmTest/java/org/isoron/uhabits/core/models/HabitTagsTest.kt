package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitTagsTest : BaseUnitTest() {
    @Test
    fun normalizesWhitespaceAndDuplicateNames() {
        assertEquals(setOf("Health", "Morning routine"), HabitTags.parse(" Health \nhealth\n\nMorning routine "))
    }

    @Test
    fun matchesAllTagsWithoutChangingOtherFilters() {
        val habit = modelFactory.buildHabit().apply { tags = setOf("Health", "Morning") }
        assertTrue(HabitMatcher(requiredTags = setOf("health", "morning")).matches(habit))
        assertFalse(HabitMatcher(requiredTags = setOf("health", "work")).matches(habit))
        habit.isArchived = true
        assertFalse(HabitMatcher(requiredTags = setOf("Health")).matches(habit))
        assertTrue(HabitMatcher(isArchivedAllowed = true, requiredTags = setOf("Health")).matches(habit))
    }

    @Test
    fun copiesTagsWithHabitAttributes() {
        val original = modelFactory.buildHabit().apply { tags = setOf("Health", "Morning") }
        val copy = modelFactory.buildHabit()
        copy.copyFrom(original)
        assertEquals(original.tags, copy.tags)
        assertEquals(original, copy)
        assertEquals(original.hashCode(), copy.hashCode())
    }

    @Test
    fun persistsTagsInDatabase() {
        val database = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(database)
            val list = factory.buildHabitList()
            val original = factory.buildHabit().apply {
                name = "Read"
                tags = setOf("Learning", "Books, articles")
            }
            list.add(original)
            val reloaded = SQLModelFactory(database).buildHabitList().getById(original.id!!)
            assertEquals(original.tags, reloaded!!.tags)
        } finally {
            database.close()
        }
    }
}
