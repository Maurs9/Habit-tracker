package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.ChangeHabitSectionCommand
import org.isoron.uhabits.core.commands.DeleteSectionCommand
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SectionListTest(private val sqlite: Boolean) : BaseUnitTest() {
    private var database: Database? = null

    override fun setUp() {
        super.setUp()
        if (sqlite) {
            database = buildMemoryDatabase()
            modelFactory = SQLModelFactory(database!!)
            habitList = modelFactory.buildHabitList()
            sectionList = modelFactory.buildSectionList()
        }
    }

    override fun tearDown() {
        database?.close()
        super.tearDown()
    }

    @Test
    fun namesAreTrimmedUniqueAndLocaleIndependent() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            val morning = sectionList.add("  MORNING \t")
            assertEquals("MORNING", morning.name)
            assertEquals(morning, sectionList.getByName("\nmorning "))
            assertEquals(morning, sectionList.getById(morning.id))
            assertSame(sectionList, modelFactory.buildSectionList())
            for (invalid in listOf("", " \t\n", " morning ")) {
                assertThrows(IllegalArgumentException::class.java) { sectionList.add(invalid) }
            }
            assertEquals(1, sectionList.size())
            assertNull(sectionList.getByName("missing"))
            assertNull(sectionList.getById(9999))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun idsArePositiveNeverReusedAndOrderIsCompact() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        assertTrue(morning.id >= 1)
        assertTrue(evening.id > morning.id)
        sectionList.remove(evening)
        val night = sectionList.add("Night")
        assertTrue(night.id > evening.id)
        assertEquals(listOf(morning.id, night.id), sectionList.getAll().map { it.id })
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
        assertEquals(mapOf(morning.id to "Morning", night.id to "Night"), sectionList.nameMap())
    }

    @Test
    fun renameValidatesWithoutMutatingSnapshotsOrOtherSections() {
        val first = sectionList.add("Morning")
        val second = sectionList.add("Evening")
        val before = sectionList.getAll()
        for (invalid in listOf(" ", "\n", " EVENING ")) {
            assertThrows(IllegalArgumentException::class.java) { sectionList.rename(first, invalid) }
        }
        assertEquals(before, sectionList.getAll())
        sectionList.rename(first, " morning ")
        assertEquals("morning", sectionList.getById(first.id)!!.name)
        assertEquals("Morning", first.name)
        sectionList.rename(first, "Early")
        assertNull(sectionList.getByName("Morning"))
        assertEquals(second, sectionList.getById(second.id))
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
    }

    @Test
    fun moveRewritesPositionsAndRejectsUnknownIdsAndInvalidPositions() {
        val first = sectionList.add("Morning")
        val second = sectionList.add("Evening")
        val third = sectionList.add("Night")
        sectionList.move(third, 0)
        assertEquals(listOf(third.id, first.id, second.id), sectionList.getAll().map { it.id })
        sectionList.move(third, 2)
        assertEquals(listOf(first.id, second.id, third.id), sectionList.getAll().map { it.id })
        sectionList.move(second, 1)
        val before = sectionList.getAll()
        for (position in listOf(-1, 3)) {
            assertThrows(IllegalArgumentException::class.java) { sectionList.move(first, position) }
        }
        val unknown = Section(9999, "Unknown", 0)
        assertThrows(IllegalArgumentException::class.java) { sectionList.move(unknown, 0) }
        assertThrows(IllegalArgumentException::class.java) { sectionList.rename(unknown, "Other") }
        assertThrows(IllegalArgumentException::class.java) { sectionList.remove(unknown) }
        assertEquals(before, sectionList.getAll())
        assertEquals(listOf(0, 1, 2), sectionList.getAll().map { it.position })
        assertPersistentSections()
    }

    @Test
    fun removeClearsLiveAndPersistedReferencesIncludingArchivedHabits() {
        val first = sectionList.add("Morning")
        val deleted = sectionList.add("Evening")
        val last = sectionList.add("Night")
        val habit = addHabit(deleted.id).apply { isArchived = true }
        habitList.update(habit)
        val untouched = addHabit(first.id)
        val otherList = modelFactory.buildHabitList()
        val otherHabit = if (sqlite) {
            otherList.getById(habit.id!!)!!
        } else {
            modelFactory.buildHabit().apply { sectionId = deleted.id }.also { otherList.add(it) }
        }
        var changed = 0
        habitList.observable.addListener { changed++ }
        sectionList.remove(deleted)
        assertNull(habit.sectionId)
        assertNull(otherHabit.sectionId)
        assertTrue(changed > 0)
        assertTrue(habit.isArchived)
        assertEquals(first.id, untouched.sectionId)
        assertEquals(2, habitList.size())
        assertEquals(listOf(first.id, last.id), sectionList.getAll().map { it.id })
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
        assertPersistentSections()
        assertPersistentReference(habit, null)
    }

    @Test
    fun repairClearsOrphansButPreservesValidAssignments() {
        val valid = sectionList.add("Morning")
        sectionList.add("Evening")
        val orphan = addHabit(99999)
        val assigned = addHabit(valid.id)
        database?.execute("UPDATE sections SET position = position + 10")
        sectionList.repair()
        assertNull(orphan.sectionId)
        assertEquals(valid.id, assigned.sectionId)
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
        sectionList.repair()
        assertPersistentSections()
        assertPersistentReference(orphan, null)
    }

    @Test
    fun commandsPersistAssignmentReassignmentAndDeletion() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val habit = addHabit(null)
        ChangeHabitSectionCommand(habitList, listOf(habit), morning.id).run()
        assertPersistentReference(habit, morning.id)
        ChangeHabitSectionCommand(habitList, listOf(habit), evening.id).run()
        assertPersistentReference(habit, evening.id)
        DeleteSectionCommand(sectionList, habitList, evening.id).run()
        assertNull(habit.sectionId)
        assertPersistentReference(habit, null)
        assertPersistentSections()
    }

    @Test
    fun concurrentAddsKeepUniqueNamesIdsAndPositions() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val tasks = (0 until 24).map { index ->
                Callable { sectionList.add("Section $index") }
            }
            val sections = executor.invokeAll(tasks).map { it.get() }
            assertEquals(24, sections.map { it.id }.distinct().size)
            assertEquals((0 until 24).toList(), sectionList.getAll().map { it.position })
            val duplicates = executor.invokeAll(
                (0 until 8).map {
                    Callable {
                        try {
                            sectionList.add(" Duplicate ")
                            true
                        } catch (_: IllegalArgumentException) {
                            false
                        }
                    }
                }
            )
            assertEquals(1, duplicates.count { it.get() })
            assertPersistentSections()
        } finally {
            executor.shutdownNow()
        }
    }

    private fun addHabit(section: Long?): Habit = modelFactory.buildHabit().apply {
        sectionId = section
        habitList.add(this)
    }

    private fun assertPersistentSections() {
        val db = database ?: return
        assertEquals(sectionList.getAll(), SQLModelFactory(db).buildSectionList().getAll())
    }

    private fun assertPersistentReference(habit: Habit, expected: Long?) {
        assertEquals(expected, habit.sectionId)
        val db = database ?: return
        val reloaded = SQLModelFactory(db).buildHabitList().getById(habit.id!!)
        assertNotNull(reloaded)
        assertEquals(expected, reloaded.sectionId)
        (habitList as SQLiteHabitList).reload()
        assertSame(habit, habitList.getById(habit.id!!))
        assertEquals(expected, habit.sectionId)
        db.query("SELECT typeof(section_id) FROM habits WHERE id = ?", habit.id.toString()).use {
            assertTrue(it.moveToNext())
            assertEquals(if (expected == null) "null" else "integer", it.getString(0))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "sqlite={0}")
        fun backends() = listOf(arrayOf(false), arrayOf(true))
    }
}
