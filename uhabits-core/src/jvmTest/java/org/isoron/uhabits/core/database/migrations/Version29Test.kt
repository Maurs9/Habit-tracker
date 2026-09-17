package org.isoron.uhabits.core.database.migrations

import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.DatabaseOpener
import org.isoron.uhabits.core.database.JdbcDatabase
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.io.LoopDBImporter
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.tasks.SingleThreadTaskRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertEquals

class Version29Test {
    @get:Rule
    val folder = TemporaryFolder()
    private val day = Timestamp.from(2025, 0, 1)
    private val opener = object : DatabaseOpener {
        override fun open(file: File): Database =
            JdbcDatabase(DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}"))
    }

    @Test
    fun migrationPreservesLegacySkipsForBothHabitTypesAndTheirNotes() {
        val file = backup(28)
        val db = opener.open(file)
        try {
            MigrationHelper(db).migrateTo(29)
            val habits = SQLModelFactory(db).buildHabitList()
            for (habit in habits) {
                assertEquals(Entry(day, Entry.SKIP, "Legacy skip"), habit.originalEntries.get(day))
                assertEquals(Entry(day.minus(1), 2, "Unchanged"), habit.originalEntries.get(day.minus(1)))
                assertEquals(Entry(day.minus(2), Entry.UNKNOWN), habit.originalEntries.get(day.minus(2)))
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun oldImportsConvertSkipsButCurrentImportsPreserveTinyMeasurements() {
        for (version in listOf(28, DATABASE_VERSION)) {
            val file = backup(version)
            val factory = MemoryModelFactory()
            val habits = factory.buildHabitList()
            val importer = LoopDBImporter(
                habits,
                factory.buildSectionList(),
                factory,
                opener,
                CommandRunner(SingleThreadTaskRunner()),
                StandardLogging()
            )
            repeat(2) {
                importer.importHabitsFromFile(file)
                assertEquals(2, habits.size())
                for (habit in habits) {
                    assertEquals(if (version < 29) Entry.SKIP else 3, habit.originalEntries.get(day).value)
                    assertEquals("Legacy skip", habit.originalEntries.get(day).notes)
                }
            }
            val db = opener.open(file)
            try {
                assertEquals(DATABASE_VERSION, db.version)
            } finally {
                db.close()
            }
        }
    }

    private fun backup(version: Int): File {
        val file = folder.newFile()
        val db = opener.open(file)
        try {
            db.execute("PRAGMA user_version=8")
            MigrationHelper(db).migrateTo(version)
            db.execute("PRAGMA user_version=$version")
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            for (type in HabitType.entries) {
                val habit = factory.buildHabit().apply { this.type = type }
                habits.add(habit)
                habit.originalEntries.add(Entry(day, 3, "Legacy skip"))
                habit.originalEntries.add(Entry(day.minus(1), 2, "Unchanged"))
                habit.originalEntries.add(Entry(day.minus(2), Entry.UNKNOWN))
            }
        } finally {
            db.close()
        }
        return file
    }
}
