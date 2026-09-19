/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.core.database.migrations

import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.DatabaseOpener
import org.isoron.uhabits.core.database.JdbcDatabase
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.sql.DriverManager
import kotlin.test.assertEquals

class Version30Test {
    @get:Rule
    val folder = TemporaryFolder()

    private val opener = object : DatabaseOpener {
        override fun open(file: File): Database =
            JdbcDatabase(DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}"))
    }

    @Test
    fun legacyPaletteSlotsAreRemappedByHueFamilyAndTone() {
        assertEquals(listOf(33, 45, 50, 1, 9, 24), migrate(listOf(27, 33, 38, 0, 12, 23)))
    }

    @Test
    fun databasesAlreadyOnTheNewPaletteAreLeftUnchanged() {
        assertEquals(listOf(27, 45), migrate(listOf(27, 45)))
    }

    private fun migrate(colors: List<Int>): List<Int> {
        val file = folder.newFile()
        val db = opener.open(file)
        try {
            db.execute("PRAGMA user_version=8")
            MigrationHelper(db).migrateTo(29)
            db.execute("PRAGMA user_version=29")
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            for (color in colors) {
                habits.add(factory.buildHabit().apply { this.color = PaletteColor(color) })
            }
            MigrationHelper(db).migrateTo(30)
            return SQLModelFactory(db).buildHabitList().map { it.color.paletteIndex }
        } finally {
            db.close()
        }
    }
}
