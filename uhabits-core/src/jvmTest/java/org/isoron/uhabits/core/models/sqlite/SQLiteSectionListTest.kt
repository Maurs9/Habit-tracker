package org.isoron.uhabits.core.models.sqlite

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.DeleteSectionCommand
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.DatabaseOpener
import org.isoron.uhabits.core.io.LoopDBImporter
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Section
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SQLiteSectionListTest : BaseUnitTest() {
    private lateinit var database: Database

    override fun setUp() {
        super.setUp()
        database = buildMemoryDatabase()
        modelFactory = SQLModelFactory(database)
        habitList = modelFactory.buildHabitList()
        sectionList = modelFactory.buildSectionList()
    }

    override fun tearDown() {
        database.close()
        super.tearDown()
    }

    @Test
    fun removeAllClearsPersistedAndCachedSections() {
        sectionList.add("Morning")
        habitList.add(modelFactory.buildHabit())
        habitList.removeAll()
        assertTrue(sectionList.getAll().isEmpty())
        assertEquals(0, habitList.size())
        assertTrue(SQLModelFactory(database).buildSectionList().getAll().isEmpty())
    }

    @Test
    fun loadAndRepairCompactPersistedOrderDeterministically() {
        database.execute("INSERT INTO sections (id, name, position) VALUES (12, 'Evening', 90)")
        database.execute("INSERT INTO sections (id, name, position) VALUES (10, 'Morning', 90)")
        assertEquals(listOf(10L, 12L), sectionList.getAll().map { it.id })
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
        assertEquals(sectionList.getAll(), SQLModelFactory(database).buildSectionList().getAll())
        database.execute("UPDATE sections SET position = position + 10")
        sectionList.repair()
        assertEquals(listOf(0, 1), sectionList.getAll().map { it.position })
        assertEquals(sectionList.getAll(), SQLModelFactory(database).buildSectionList().getAll())
    }

    @Test
    fun deletionFailureRollsBackReferencesAndOrder() {
        val section = sectionList.add("Morning")
        sectionList.add("Evening")
        val habit = modelFactory.buildHabit().apply { sectionId = section.id }.also { habitList.add(it) }
        preventDeletion()
        assertThrows(RuntimeException::class.java) { sectionList.remove(section) }
        assertUnchanged(section, habit)
        assertFalse(database.inTransaction)
    }

    @Test
    fun deleteCommandFailureDoesNotDetachHabitsFromSurvivingSection() {
        val section = sectionList.add("Morning")
        val habit = modelFactory.buildHabit().apply { sectionId = section.id }.also { habitList.add(it) }
        preventDeletion()
        assertThrows(RuntimeException::class.java) {
            DeleteSectionCommand(sectionList, habitList, section.id).run()
        }
        assertUnchanged(section, habit)
        assertFalse(database.inTransaction)
    }

    @Test
    fun standaloneSectionMutationsUseDatabaseTransactionApi() {
        var transactions = 0
        val observedDatabase = object : Database by database {
            override fun beginTransaction() {
                transactions++
                database.beginTransaction()
            }
        }
        val sections = SQLModelFactory(observedDatabase).buildSectionList()
        val first = sections.add("Morning")
        val second = sections.add("Evening")
        sections.move(second, 0)
        sections.remove(first)
        sections.repair()
        assertEquals(3, transactions)
        assertFalse(database.inTransaction)
        assertEquals(listOf(second.id), sections.getAll().map { it.id })
    }

    @Test
    fun failedNestedDeleteRollsBackItsSavepointWithoutCommittingOuterWrites() {
        val section = sectionList.add("Morning")
        val habit = modelFactory.buildHabit().apply { sectionId = section.id }.also { habitList.add(it) }
        preventDeletion()
        database.beginTransaction()
        try {
            database.execute("UPDATE habits SET name = 'Outer change'")
            assertThrows(RuntimeException::class.java) { sectionList.remove(section) }
            assertTrue(database.inTransaction)
            assertEquals(section.id, modelFactory.buildHabitListRepository().find(habit.id!!)!!.sectionId)
            assertEquals("Outer change", modelFactory.buildHabitListRepository().find(habit.id!!)!!.name)
        } finally {
            database.endTransaction()
        }
        assertEquals("", modelFactory.buildHabitListRepository().find(habit.id!!)!!.name)
        assertUnchanged(section, habit)
    }

    @Test
    fun importAndRepairRespectOuterRollbackAndCachedModelsCanReload() {
        val existing = sectionList.add("Local")
        val habit = modelFactory.buildHabit().apply {
            uuid = "restored"
            sectionId = existing.id
        }.also { habitList.add(it) }
        val source = buildMemoryDatabase()
        source.execute("PRAGMA user_version=28")
        val sourceFactory = SQLModelFactory(source)
        val foreign = sourceFactory.buildSectionList().add("Imported")
        sourceFactory.buildHabitList().add(
            sourceFactory.buildHabit().apply {
                uuid = "restored"
                sectionId = foreign.id
            }
        )
        val opener = object : DatabaseOpener {
            override fun open(file: File): Database = source
        }
        val importer = LoopDBImporter(habitList, sectionList, modelFactory, opener, commandRunner, StandardLogging())
        database.beginTransaction()
        try {
            importer.importHabitsFromFile(File("unused-memory-database"))
            assertEquals(listOf("Local", "Imported"), sectionList.getAll().map { it.name })
            assertEquals(sectionList.getByName("Imported")!!.id, habit.sectionId)
        } finally {
            database.endTransaction()
        }
        (sectionList as SQLiteSectionList).reload()
        (habitList as SQLiteHabitList).reload()
        assertEquals(listOf(existing), sectionList.getAll())
        assertEquals(existing.id, habit.sectionId)
        assertEquals(1, habitList.size())
        assertNull(sectionList.getByName("Imported"))
    }

    private fun preventDeletion() {
        database.execute(
            "CREATE TRIGGER prevent_section_delete BEFORE DELETE ON sections BEGIN SELECT RAISE(ABORT, 'stop'); END"
        )
    }

    private fun assertUnchanged(section: Section, habit: Habit) {
        assertEquals(section, sectionList.getById(section.id))
        assertEquals(section.id, habit.sectionId)
        val reloaded = SQLModelFactory(database)
        assertEquals(sectionList.getAll(), reloaded.buildSectionList().getAll())
        assertEquals(section.id, reloaded.buildHabitList().getById(habit.id!!)!!.sectionId)
    }
}
