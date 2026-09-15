package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.database.JdbcDatabase
import org.isoron.uhabits.core.database.MigrationHelper
import org.junit.Test
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagMigrationTest {
    @Test
    fun upgradesExistingHabitsWithoutChangingTheirData() {
        val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        val database = JdbcDatabase(connection)
        try {
            database.execute("PRAGMA user_version=8")
            val helper = MigrationHelper(database)
            helper.migrateTo(25)
            database.execute("PRAGMA user_version=25")
            database.execute("INSERT INTO habits (name, description) VALUES (?, ?)", "Read", "Keep my notes")
            helper.migrateTo(26)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT name, description, tags FROM habits").use { rows ->
                    assertTrue(rows.next())
                    assertEquals("Read", rows.getString("name"))
                    assertEquals("Keep my notes", rows.getString("description"))
                    assertEquals("", rows.getString("tags"))
                }
            }
        } finally {
            database.close()
        }
    }
}
