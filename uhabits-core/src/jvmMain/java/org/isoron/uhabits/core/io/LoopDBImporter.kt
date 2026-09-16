/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
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
package org.isoron.uhabits.core.io

import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.DatabaseOpener
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.database.Repository
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.SectionList
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.sqlite.records.EntryRecord
import org.isoron.uhabits.core.models.sqlite.records.HabitRecord
import org.isoron.uhabits.core.models.sqlite.records.SectionRecord
import org.isoron.uhabits.core.utils.isSQLite3File
import java.io.File
import javax.inject.Inject

/**
 * Class that imports data from database files exported by Loop Habit Tracker.
 */
class LoopDBImporter
@Inject constructor(
    @AppScope val habitList: HabitList,
    @AppScope val sectionList: SectionList,
    @AppScope val modelFactory: ModelFactory,
    @AppScope val opener: DatabaseOpener,
    @AppScope val runner: CommandRunner,
    @AppScope logging: Logging
) : AbstractImporter() {

    private val logger = logging.getLogger("LoopDBImporter")

    override fun canHandle(file: File): Boolean {
        if (!file.isSQLite3File()) return false
        val db = opener.open(file)
        return try {
            canHandleDatabase(db)
        } finally {
            db.close()
        }
    }

    private fun canHandleDatabase(db: Database): Boolean {
        var canHandle = true
        val c = db.query(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND lower(name) IN ('habits', 'repetitions')"
        )
        try {
            if (!c.moveToNext() || c.getInt(0) != 2) {
                logger.error("Cannot handle file: tables not found")
                canHandle = false
            }
        } finally {
            c.close()
        }
        if (db.version > DATABASE_VERSION) {
            logger.error("Cannot handle file: incompatible version: ${db.version} > $DATABASE_VERSION")
            canHandle = false
        }
        return canHandle
    }

    override fun importHabitsFromFile(file: File) {
        val db = opener.open(file)
        try {
            importDatabase(db)
        } finally {
            db.close()
        }
    }

    private fun importDatabase(db: Database) {
        val helper = MigrationHelper(db)
        helper.migrateTo(DATABASE_VERSION)

        val habitsRepository = Repository(HabitRecord::class.java, db)
        val entryRepository = Repository(EntryRecord::class.java, db)
        validateSectionMetadata(db)
        val foreignSections = Repository(SectionRecord::class.java, db)
            .findAll("ORDER BY position, id").map { it.toSection() }
        val sectionIds = foreignSections.associate { section ->
            section.id to (sectionList.getByName(section.name) ?: sectionList.add(section.name)).id
        }

        for (habitRecord in habitsRepository.findAll("order by position")) {
            var habit = habitList.getByUUID(habitRecord.uuid)
            val entryRecords = entryRepository.findAll("where habit = ?", habitRecord.id.toString())
            habitRecord.sectionId = sectionIds[habitRecord.sectionId]

            if (habit == null) {
                habit = modelFactory.buildHabit()
                habitRecord.id = null
                habitRecord.copyTo(habit)
                CreateHabitCommand(modelFactory, habitList, habit).run()
            } else {
                val modified = modelFactory.buildHabit()
                habitRecord.id = habit.id
                habitRecord.copyTo(modified)
                EditHabitCommand(habitList, habit.id!!, modified).run()
            }

            // Reload saved version of the habit
            habit = habitList.getByUUID(habitRecord.uuid)!!
            val entries = habit.originalEntries

            // Import entries
            for (r in entryRecords) {
                val t = Timestamp(r.timestamp!!)
                val (_, value, notes) = entries.get(t)
                if (value != r.value || notes != r.notes) {
                    entries.add(Entry(t, r.value!!, r.notes ?: ""))
                }
            }
            habit.recompute()
        }
        habitList.resort()
        sectionList.repair()
    }

    private fun validateSectionMetadata(db: Database) {
        val invalidRows = listOf(
            "SELECT 1 FROM habits WHERE section_id IS NOT NULL AND typeof(section_id) != 'integer' LIMIT 1",
            "SELECT 1 FROM sections WHERE typeof(id) != 'integer' OR id < 1 OR typeof(name) != 'text' " +
                "OR typeof(position) != 'integer' OR position NOT BETWEEN -2147483648 AND 2147483647 LIMIT 1",
            "SELECT id FROM sections GROUP BY id HAVING count(*) > 1 LIMIT 1"
        )
        for (query in invalidRows) {
            db.query(query).use { require(!it.moveToNext()) { "Invalid section metadata" } }
        }
    }
}
