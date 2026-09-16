package org.isoron.uhabits.core.io

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SectionImportTest(private val sqlite: Boolean) : BaseUnitTest() {
    private var database: Database? = null
    private lateinit var file: File

    override fun setUp() {
        super.setUp()
        file = File("section-import-${UUID.randomUUID()}.db")
        if (sqlite) {
            database = buildMemoryDatabase()
            modelFactory = SQLModelFactory(database!!)
            habitList = modelFactory.buildHabitList()
            sectionList = modelFactory.buildSectionList()
        }
    }

    override fun tearDown() {
        database?.close()
        file.delete()
        super.tearDown()
    }

    @Test
    fun mergeUsesNormalizedNamesKeepsLocalOrderAndUpdatesUuidReferences() {
        val existing = sectionList.add(" evening ")
        val local = sectionList.add("Local")
        val unrelated = modelFactory.buildHabit().apply {
            uuid = "local-habit"
            sectionId = local.id
        }.also { habitList.add(it) }
        createBackup()
        val importer = importer()
        assertTrue(importer.canHandle(file))
        importer.importHabitsFromFile(file)
        assertEquals(listOf("evening", "Local", "Morning"), sectionList.getAll().map { it.name })
        val restored = habitList.getByUUID("foreign-habit")!!
        assertEquals(existing.id, restored.sectionId)
        assertEquals(local.id, unrelated.sectionId)
        assertEquals(setOf("Daily"), restored.tags)
        assertEquals(setOf(480, 1200), restored.reminderTimes)
        assertEquals("Kept note", restored.originalEntries.get(timestamp(2015, 0, 20)).notes)
        restored.sectionId = local.id
        habitList.update(restored)
        importer.importHabitsFromFile(file)
        assertEquals(3, sectionList.size())
        assertEquals(2, habitList.size())
        assertEquals(existing.id, restored.sectionId)
        assertEquals(listOf(0, 1, 2), sectionList.getAll().map { it.position })
        assertPersisted()
    }

    @Test
    fun orphanReferencesBecomeUnsectionedAndRepairLocalOrphans() {
        createBackup()
        val source = databaseOpener.open(file)
        try {
            source.execute("UPDATE habits SET section_id = 99999")
        } finally {
            source.close()
        }
        val localOrphan = modelFactory.buildHabit().apply {
            sectionId = 9999
        }.also { habitList.add(it) }
        importer().importHabitsFromFile(file)
        assertNull(habitList.getByUUID("foreign-habit")!!.sectionId)
        assertNull(localOrphan.sectionId)
        assertEquals(listOf("Morning", "EVENING"), sectionList.getAll().map { it.name })
        assertPersisted()
    }

    @Test
    fun invalidSectionMetadataFailsBeforeModifyingLocalModels() {
        val local = sectionList.add("Local")
        val mutations = listOf(
            "UPDATE sections SET name = '   '",
            "UPDATE sections SET position = 'invalid'",
            "UPDATE sections SET position = 1.5",
            "UPDATE sections SET position = 2147483648",
            "UPDATE habits SET section_id = 'invalid'",
            "UPDATE habits SET section_id = 1.5"
        )
        for (mutation in mutations) {
            createBackup()
            val source = databaseOpener.open(file)
            try {
                source.execute(mutation)
            } finally {
                source.close()
            }
            assertThrows(IllegalArgumentException::class.java) { importer().importHabitsFromFile(file) }
            assertEquals(listOf(local), sectionList.getAll())
            assertEquals(0, habitList.size())
            assertTrue(file.delete())
        }
    }

    @Test
    fun duplicateNormalizedForeignNamesMergeToOneLocalSection() {
        createBackup()
        val source = databaseOpener.open(file)
        try {
            source.execute("INSERT INTO sections (id, name, position) VALUES (99, ' evening ', 2)")
            source.execute("UPDATE habits SET section_id = 99")
        } finally {
            source.close()
        }
        importer().importHabitsFromFile(file)
        assertEquals(listOf("Morning", "EVENING"), sectionList.getAll().map { it.name })
        assertEquals(sectionList.getByName("Evening")!!.id, habitList.getByUUID("foreign-habit")!!.sectionId)
        assertPersisted()
    }

    @Test
    fun versions25Through27ImportWithoutSectionsAndKeepLocalSections() {
        val existing = sectionList.add("Local")
        for (version in 25..27) {
            val source = databaseOpener.open(file)
            try {
                source.execute("PRAGMA user_version=8")
                MigrationHelper(source).migrateTo(version)
                source.execute("PRAGMA user_version=$version")
                source.execute(
                    """INSERT INTO habits
                    (name, description, question, freq_num, freq_den, color, position,
                    archived, type, target_type, target_value, unit, uuid)
                    VALUES ('Legacy', '', '', 1, 1, 3, 0, 0, 0, 0, 0, '', ?)""",
                    "legacy-$version"
                )
            } finally {
                source.close()
            }
            val importer = importer()
            assertTrue(importer.canHandle(file))
            importer.importHabitsFromFile(file)
            assertNull(habitList.getByUUID("legacy-$version")!!.sectionId)
            assertEquals(listOf(existing), sectionList.getAll())
            assertTrue(file.delete())
        }
        assertEquals(3, habitList.size())
        assertPersisted()
    }

    private fun createBackup() {
        val source = databaseOpener.open(file)
        try {
            source.execute("PRAGMA user_version=8")
            MigrationHelper(source).migrateTo(DATABASE_VERSION)
            source.execute("PRAGMA user_version=$DATABASE_VERSION")
            val factory = SQLModelFactory(source)
            factory.buildSectionList().add("Morning")
            val evening = factory.buildSectionList().add(" EVENING ")
            val habit = factory.buildHabit().apply {
                uuid = "foreign-habit"
                name = "Read"
                sectionId = evening.id
                tags = setOf("Daily")
                reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
                extraReminderTimes = setOf(1200)
            }
            factory.buildHabitList().add(habit)
            habit.originalEntries.add(Entry(timestamp(2015, 0, 20), Entry.YES_MANUAL, "Kept note"))
        } finally {
            source.close()
        }
    }

    private fun importer() =
        LoopDBImporter(habitList, sectionList, modelFactory, databaseOpener, commandRunner, StandardLogging())

    private fun assertPersisted() {
        val db = database ?: return
        val factory = SQLModelFactory(db)
        assertEquals(sectionList.getAll(), factory.buildSectionList().getAll())
        assertEquals(habitList.toList(), factory.buildHabitList().toList())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "sqlite={0}")
        fun backends() = listOf(arrayOf(false), arrayOf(true))
    }
}
