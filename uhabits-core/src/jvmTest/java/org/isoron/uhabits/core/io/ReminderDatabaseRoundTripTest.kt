package org.isoron.uhabits.core.io

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.database.Repository
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.models.sqlite.records.HabitRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReminderDatabaseRoundTripTest : BaseUnitTest() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun testMigration27PreservesLegacyReminderAndTags() {
        val file = temporaryFolder.newFile()
        val db = databaseOpener.open(file)
        try {
            db.execute("PRAGMA user_version=8")
            MigrationHelper(db).migrateTo(26)
            db.execute("PRAGMA user_version=26")
            db.execute(
                """INSERT INTO habits
                (name, description, question, freq_num, freq_den, color, position,
                reminder_hour, reminder_min, reminder_days, archived, type, target_type,
                target_value, unit, uuid, tags)
                VALUES ('Legacy', '', '', 1, 1, 3, 0, 8, 30, 62, 0, 0, 0, 0, '', 'legacy', 'Health')"""
            )
            MigrationHelper(db).migrateTo(27)
            db.execute("PRAGMA user_version=27")
            assertEquals(27, db.version)
            MigrationHelper(db).migrateTo(28)
            val record = Repository(HabitRecord::class.java, db).findAll("")[0]
            val habit = modelFactory.buildHabit()
            record.copyTo(habit)
            assertEquals(Reminder(8, 30, WeekdayList(62)), habit.reminder)
            assertTrue(habit.extraReminderTimes.isEmpty())
            assertEquals(setOf("Health"), habit.tags)
        } finally {
            db.close()
        }
    }

    @Test
    fun testDatabaseExportImportAndUuidUpdateRetainAllTimes() {
        val file = temporaryFolder.newFile()
        val db = databaseOpener.open(file)
        try {
            db.execute("PRAGMA user_version=8")
            MigrationHelper(db).migrateTo(DATABASE_VERSION)
            db.execute("PRAGMA user_version=$DATABASE_VERSION")
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            val original = factory.buildHabit().apply {
                uuid = "multiple-reminders"
                tags = setOf("Health")
                reminder = Reminder(12, 30, WeekdayList(62))
                extraReminderTimes = setOf(480, 1200)
            }
            habits.add(original)
            val reopened = factory.buildHabitList().getByUUID(original.uuid)!!
            assertEquals(original.reminderTimes, reopened.reminderTimes)
            assertEquals(original.reminder, reopened.reminder)
        } finally {
            db.close()
        }
        val importer = LoopDBImporter(habitList, sectionList, modelFactory, databaseOpener, commandRunner, StandardLogging())
        assertTrue(importer.canHandle(file))
        importer.importHabitsFromFile(file)
        val imported = habitList.getByUUID("multiple-reminders")!!
        assertEquals(setOf(480, 750, 1200), imported.reminderTimes)
        assertEquals(setOf("Health"), imported.tags)
        imported.replaceReminderTimes(listOf(360))
        importer.importHabitsFromFile(file)
        assertEquals(1, habitList.size())
        assertEquals(setOf(480, 750, 1200), imported.reminderTimes)
        assertEquals(Reminder(12, 30, WeekdayList(62)), imported.reminder)
    }
}
