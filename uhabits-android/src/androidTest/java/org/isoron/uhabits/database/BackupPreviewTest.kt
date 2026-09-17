package org.isoron.uhabits.database

import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.io.AbstractImporter
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.isoron.uhabits.tasks.ExportDBTask
import org.isoron.uhabits.tasks.ImportDataTask
import org.isoron.uhabits.tasks.ImportPreviewTask
import org.isoron.uhabits.utils.DatabaseUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupPreviewTest : BaseAndroidTest() {
    private lateinit var directory: File

    @Before
    fun createDirectory() {
        directory = File(targetContext.cacheDir, "preview-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdir())
    }

    @After
    fun removeDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun previewIsReadOnlyAndContainsCountsAndEntryDate() {
        val habit = fixtures.createEmptyHabit()
        habit.originalEntries.add(Entry(day(0), Entry.YES_MANUAL, "Remember this"))
        val file = backup()
        val before = file.readBytes()
        val preview = BackupValidator.inspect(file)
        assertEquals(DATABASE_VERSION, preview.version)
        assertEquals(habitList.size().toLong(), preview.habitCount)
        assertEquals(1L, preview.entryCount)
        assertEquals(day(0).unixTime, preview.latestEntryTimestamp)
        assertTrue(before.contentEquals(file.readBytes()))
        assertFalse(File(file.path + "-journal").exists())
        assertFalse(File(file.path + "-wal").exists())
    }

    @Test
    fun previewDoesNotImportWithoutConfirmation() {
        val habit = fixtures.createLongHabit()
        val file = backup()
        val before = file.readBytes()
        val uuid = habit.uuid
        habitList.remove(habit)
        val count = habitList.size()
        var succeeded = false
        val previewTask = ImportPreviewTask(appComponent.genericImporter, file) {
            succeeded = it.isSuccess
        }
        previewTask.doInBackground()
        previewTask.onPostExecute()
        assertTrue(succeeded)
        assertEquals(count, habitList.size())
        assertNull(habitList.getByUUID(uuid))
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test
    fun snapshotImportRoundTripsHabitsEntriesAndNotesWithoutRemovingOtherHabits() {
        val habit = fixtures.createEmptyHabit()
        habit.tags = setOf("health", "daily")
        habit.color = PaletteColor(PaletteColor.COUNT - 1)
        habit.reminder = Reminder(8, 15, WeekdayList(0b1111100))
        habit.extraReminderTimes = setOf(12 * 60 + 30, 20 * 60 + 45)
        habitList.update(listOf(habit))
        habit.originalEntries.add(Entry(day(0), Entry.YES_MANUAL, "Snapshot note"))
        val uuid = habit.uuid
        val name = habit.name
        val tags = habit.tags
        val color = habit.color
        val reminder = habit.reminder
        val reminderTimes = habit.reminderTimes
        val extraReminderTimes = habit.extraReminderTimes
        val file = backup()
        habitList.remove(habit)
        val other = fixtures.createEmptyHabit()
        var result = 0
        val task = ImportDataTask(
            appComponent.genericImporter,
            modelFactory,
            file,
            habitList
        ) { result = it }
        task.doInBackground()
        task.onPostExecute()
        assertEquals(ImportDataTask.SUCCESS, result)
        (habitList as SQLiteHabitList).reload()
        val restored = habitList.getByUUID(uuid)!!
        assertEquals(name, restored.name)
        assertEquals(tags, restored.tags)
        assertEquals(color, restored.color)
        assertEquals(reminder, restored.reminder)
        assertEquals(reminderTimes, restored.reminderTimes)
        assertEquals(extraReminderTimes, restored.extraReminderTimes)
        assertEquals("Snapshot note", restored.originalEntries.get(day(0)).notes)
        assertEquals(Entry.YES_MANUAL, restored.originalEntries.get(day(0)).value)
        assertNotNull(habitList.getByUUID(other.uuid))
    }

    @Test
    fun invalidVersionAndSchemaAreRejectedWithoutChangingInputOrLiveDatabase() {
        val count = habitList.size()
        val newer = backup()
        SQLiteDatabase.openDatabase(newer.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.version = DATABASE_VERSION + 1
        }
        assertRejectedUnchanged(newer)
        val wrongSchema = backup()
        SQLiteDatabase.openDatabase(wrongSchema.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("ALTER TABLE Repetitions RENAME TO wrong_table")
        }
        assertRejectedUnchanged(wrongSchema)
        assertEquals(count, habitList.size())
    }

    @Test
    fun corruptMetadataIsRejectedBeforeImportAndExistingDataIsRetained() {
        val file = backup()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("UPDATE Habits SET freq_den=0")
        }
        assertRejectedUnchanged(file)
        val count = habitList.size()
        var result = 0
        val task = ImportDataTask(
            appComponent.genericImporter,
            modelFactory,
            file,
            habitList
        ) { result = it }
        task.doInBackground()
        task.onPostExecute()
        assertEquals(ImportDataTask.FAILED, result)
        assertEquals(count, habitList.size())
        assertFalse(DatabaseUtils.openDatabase().inTransaction())
    }

    @Test
    fun malformedInputIsNotDeletedByCorruptionHandler() {
        val file = File(directory, "corrupt.db")
        val bytes = "SQLite format 3\u0000".toByteArray() + ByteArray(100)
        bytes[18] = 1
        bytes[19] = 1
        file.writeBytes(bytes)
        assertRejectedUnchanged(file)
    }

    @Test
    fun recognizesPriorSchemaWithoutMigratingPreview() {
        for (version in 25..27) {
            val file = File(directory, "legacy-$version.db")
            SQLiteDatabase.openOrCreateDatabase(file, null).use {
                it.disableWriteAheadLogging()
                it.version = 8
                MigrationHelper(AndroidDatabase(it, file)).migrateTo(version)
                it.version = version
            }
            val bytes = file.readBytes()
            val preview = BackupValidator.inspect(file)
            assertEquals(version, preview.version)
            assertEquals(0L, preview.habitCount)
            assertNull(preview.latestEntryTimestamp)
            assertTrue(bytes.contentEquals(file.readBytes()))
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.disableWriteAheadLogging()
                it.version = DATABASE_VERSION
            }
            assertRejectedUnchanged(file)
        }
    }

    @Test
    fun currentVersionRecognizesSectionsAndAllowsOrphansWithoutChangingInput() {
        val section = appComponent.sectionList.add("Morning")
        val habit = fixtures.createEmptyHabit()
        habit.sectionId = section.id
        habitList.update(habit)
        val file = backup()
        val bytes = file.readBytes()
        assertTrue(BackupValidator.isLoopDatabase(file))
        assertEquals(DATABASE_VERSION, BackupValidator.inspect(file).version)
        assertTrue(bytes.contentEquals(file.readBytes()))
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("UPDATE Habits SET section_id = 999999")
        }
        val orphanBytes = file.readBytes()
        assertEquals(DATABASE_VERSION, BackupValidator.inspect(file).version)
        assertTrue(orphanBytes.contentEquals(file.readBytes()))
    }

    @Test
    fun version28RequiresSectionTableColumnsAndHabitReferenceColumn() {
        val mutations = listOf(
            "DROP TABLE Sections",
            "ALTER TABLE Sections RENAME COLUMN position TO incorrect_position",
            "ALTER TABLE Habits RENAME COLUMN section_id TO incorrect_section_id",
            "DROP TABLE Sections; CREATE VIEW Sections AS SELECT 1 AS id, 'Morning' AS name, 0 AS position"
        )
        for (mutation in mutations) {
            val file = backup()
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                mutation.split(";").forEach { db.execSQL(it) }
            }
            assertRejectedUnchanged(file)
        }
    }

    @Test
    fun version28RejectsInvalidSectionMetadataReadOnly() {
        appComponent.sectionList.add("Morning")
        fixtures.createEmptyHabit()
        val mutations = listOf(
            "UPDATE Habits SET section_id = 'not-an-id'",
            "UPDATE Habits SET section_id = 1.5",
            "UPDATE Sections SET name = ''",
            "UPDATE Sections SET name = char(9) || char(10) || char(160)",
            "UPDATE Sections SET name = x'1234'",
            "UPDATE Sections SET position = 'first'",
            "UPDATE Sections SET position = 1.5",
            "UPDATE Sections SET position = 2147483648",
            "UPDATE Sections SET id = 0"
        )
        for (mutation in mutations) {
            val file = backup()
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL(mutation)
            }
            assertRejectedUnchanged(file)
        }
    }

    @Test
    fun sectionsMergeByNameKeepLocalOrderAndRoundTripReferences() {
        val sections = appComponent.sectionList
        val morning = sections.add("Morning")
        val evening = sections.add("Evening")
        val habit = fixtures.createEmptyHabit()
        habit.sectionId = evening.id
        habitList.update(habit)
        val file = backup()
        sections.remove(morning)
        sections.remove(evening)
        val localEvening = sections.add(" evening ")
        sections.add("Local")
        repeat(2) {
            var result = 0
            val task = ImportDataTask(appComponent.genericImporter, modelFactory, file, habitList) { result = it }
            task.doInBackground()
            task.onPostExecute()
            assertEquals(ImportDataTask.SUCCESS, result)
            assertFalse(DatabaseUtils.openDatabase().inTransaction())
            assertEquals(listOf("evening", "Local", "Morning"), sections.getAll().map { it.name })
            assertEquals(localEvening.id, habit.sectionId)
            assertEquals(localEvening.id, habitList.getByUUID(habit.uuid)!!.sectionId)
        }
        (habitList as SQLiteHabitList).reload()
        assertEquals(localEvening.id, habit.sectionId)
        assertEquals(sections.getAll(), SQLModelFactory((modelFactory as SQLModelFactory).database).buildSectionList().getAll())
    }

    @Test
    fun importedOrphanIsRepairedInLoadedAndPersistedHabits() {
        val habit = fixtures.createEmptyHabit()
        val file = backup()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("UPDATE Habits SET section_id = 999999")
        }
        var result = 0
        val task = ImportDataTask(appComponent.genericImporter, modelFactory, file, habitList) { result = it }
        task.doInBackground()
        task.onPostExecute()
        assertEquals(ImportDataTask.SUCCESS, result)
        assertNull(habit.sectionId)
        (habitList as SQLiteHabitList).reload()
        assertNull(habitList.getById(habit.id!!)!!.sectionId)
    }

    @Test
    fun validatesCurrentPaletteBoundsWithoutRejectingLegacyArgbColors() {
        val current = backup()
        SQLiteDatabase.openDatabase(current.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.disableWriteAheadLogging()
            it.execSQL("UPDATE Habits SET color = ${PaletteColor.COUNT - 1}")
        }
        assertEquals(habitList.size().toLong(), BackupValidator.inspect(current).habitCount)
        SQLiteDatabase.openDatabase(current.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.disableWriteAheadLogging()
            it.execSQL("UPDATE Habits SET color = ${PaletteColor.COUNT}")
        }
        assertRejectedUnchanged(current)

        val legacy = File(directory, "legacy-argb.db")
        SQLiteDatabase.openOrCreateDatabase(legacy, null).use {
            it.disableWriteAheadLogging()
            it.version = 8
            MigrationHelper(AndroidDatabase(it, legacy)).migrateTo(13)
            it.version = 13
            it.execSQL(
                "INSERT INTO Habits (name, description, freq_num, freq_den, color, position, archived, highlight) " +
                    "VALUES ('Legacy', '', 1, 1, -2937041, 0, 0, 0)"
            )
        }
        assertEquals(1L, BackupValidator.inspect(legacy).habitCount)
    }

    @Test
    fun rejectsInvalidReminderMetadataWithoutChangingInput() {
        for (value in listOf("1440", "not-a-time", "600,")) {
            val file = backup()
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.disableWriteAheadLogging()
                it.execSQL(
                    "UPDATE Habits SET reminder_hour=8, reminder_min=0, reminder_days=127, reminder_times=?",
                    arrayOf(value)
                )
            }
            assertRejectedUnchanged(file)
        }
        val disabled = backup()
        SQLiteDatabase.openDatabase(disabled.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.disableWriteAheadLogging()
            it.execSQL("UPDATE Habits SET reminder_times='600'")
        }
        assertRejectedUnchanged(disabled)
    }

    @Test
    fun exportFailureRemovesPartialFileAndPreservesPreviousBackup() {
        val previous = backup()
        val before = directory.list()!!.toSet()
        val db = (modelFactory as SQLModelFactory).database
        db.beginTransaction()
        try {
            try {
                DatabaseUtils.saveDatabaseCopy(targetContext, directory)
                fail("Backing up an uncommitted transaction should fail")
            } catch (_: IOException) {
                assertEquals(before, directory.list()!!.toSet())
                assertTrue(previous.exists())
            }
        } finally {
            db.endTransaction()
        }
    }

    @Test
    fun failedImportRollsBackPartialWritesAndReloadsCachedModels() {
        val habit = fixtures.createEmptyHabit()
        val sections = appComponent.sectionList
        val originalSection = sections.add("Original")
        habit.sectionId = originalSection.id
        habitList.update(habit)
        habit.originalEntries.add(Entry(day(0), Entry.YES_MANUAL, "Original note"))
        habit.recompute()
        val originalScore = habit.scores[day(0)].value
        val id = habit.id!!
        val originalName = habit.name
        val originalCount = habitList.size()
        val file = File(directory, "partial-import.csv").apply { writeText("import fixture") }
        val importer = appComponent.genericImporter
        val originalImporters = importer.importers
        importer.importers = listOf(object : AbstractImporter() {
            override fun canHandle(file: File) = true

            override fun importHabitsFromFile(file: File) {
                habit.name = "Partial update"
                habit.sectionId = sections.add("Partial section").id
                habitList.update(listOf(habit))
                habit.originalEntries.add(Entry(day(0), Entry.NO, "Partial note"))
                fixtures.createEmptyHabit()
                sections.repair()
                throw IOException("Interrupted import")
            }
        })
        try {
            var result = 0
            val task = ImportDataTask(importer, modelFactory, file, habitList) { result = it }
            task.doInBackground()
            task.onPostExecute()
            assertEquals(ImportDataTask.FAILED, result)
            assertFalse(DatabaseUtils.openDatabase().inTransaction())
            assertEquals(originalCount, habitList.size())
            val restored = habitList.getById(id)!!
            assertSame(habit, restored)
            assertEquals(originalName, restored.name)
            assertEquals(originalSection.id, restored.sectionId)
            assertEquals(listOf(originalSection), sections.getAll())
            assertEquals(Entry.YES_MANUAL, restored.originalEntries.get(day(0)).value)
            assertEquals("Original note", restored.originalEntries.get(day(0)).notes)
            assertEquals(Entry.YES_MANUAL, restored.computedEntries.get(day(0)).value)
            assertEquals(originalScore, restored.scores[day(0)].value)
            assertTrue(restored.isCompletedToday())
        } finally {
            importer.importers = originalImporters
        }
    }

    @Test
    fun exportTaskReportsFailureWithoutThrowingOrAdvancingLastSuccess() {
        val previous = backup()
        val previousBytes = previous.readBytes()
        val lastSuccess = BackupStatus(targetContext).lastSuccess
        val storageContext = object : ContextWrapper(targetContext) {
            override fun getExternalFilesDirs(type: String?): Array<File> = arrayOf(directory)
        }
        var notified = false
        var filename: String? = "not notified"
        val task = ExportDBTask(targetContext, AndroidDirFinder(storageContext)) {
            notified = true
            filename = it
        }
        val db = DatabaseUtils.openDatabase()
        db.beginTransaction()
        try {
            task.doInBackground()
        } finally {
            db.endTransaction()
        }
        assertFalse(notified)
        task.onPostExecute()
        assertTrue(notified)
        assertNull(filename)
        assertEquals(lastSuccess, BackupStatus(targetContext).lastSuccess)
        assertTrue(BackupStatus(targetContext).lastFailure > 0)
        assertTrue(previousBytes.contentEquals(previous.readBytes()))
        assertTrue(File(directory, "Backups").listFiles()!!.isEmpty())
    }

    private fun assertRejectedUnchanged(file: File) {
        val bytes = file.readBytes()
        try {
            BackupValidator.inspect(file)
            fail("Invalid backup was accepted")
        } catch (_: Exception) {
            assertTrue(file.exists())
            assertTrue(bytes.contentEquals(file.readBytes()))
        }
    }

    private fun backup(): File = File(DatabaseUtils.saveDatabaseCopy(targetContext, directory))
}
