package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.JdbcDatabase
import org.isoron.uhabits.core.database.MigrationHelper
import org.junit.Test
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SectionMigrationTest {
    @Test
    fun migration28PreservesExistingDataAndLeavesHabitsUnsectioned() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val db = JdbcDatabase(connection)
        try {
            db.execute("PRAGMA user_version=8")
            MigrationHelper(db).migrateTo(27)
            db.execute("PRAGMA user_version=27")
            db.execute(
                "INSERT INTO habits (name, description, tags, reminder_times) VALUES (?, ?, ?, ?)",
                "Read",
                "My notes",
                "Daily",
                "600;1200"
            )
            MigrationHelper(db).migrateTo(28)
            assertEquals(28, DATABASE_VERSION)
            db.query("SELECT name, description, tags, reminder_times, section_id FROM habits").use {
                assertTrue(it.moveToNext())
                assertEquals("Read", it.getString(0))
                assertEquals("My notes", it.getString(1))
                assertEquals("Daily", it.getString(2))
                assertEquals("600;1200", it.getString(3))
                assertNull(it.getLong(4))
                assertFalse(it.moveToNext())
            }
            db.query("SELECT id, name, position FROM sections").use { assertFalse(it.moveToNext()) }
        } finally {
            db.close()
        }
    }
}
