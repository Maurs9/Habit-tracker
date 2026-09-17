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

package org.isoron.uhabits.core.models.sqlite

import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.Repository
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.EntryList
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.sqlite.records.EntryRecord

class SQLiteEntryList(database: Database) : EntryList() {
    val repository = Repository(EntryRecord::class.java, database)
    var habitId: Long? = null
    var isLoaded = false

    private fun loadRecords() {
        if (isLoaded) return
        val habitId = habitId ?: throw IllegalStateException("habitId must be set")
        val records = repository.findAll(
            "where habit = ? order by timestamp",
            habitId.toString()
        )
        for (rec in records) super.add(rec.toEntry())
        isLoaded = true
    }

    @Synchronized
    override fun get(timestamp: Timestamp): Entry {
        loadRecords()
        return super.get(timestamp)
    }

    @Synchronized
    override fun getByInterval(from: Timestamp, to: Timestamp): List<Entry> {
        loadRecords()
        return super.getByInterval(from, to)
    }

    @Synchronized
    override fun add(entry: Entry) {
        loadRecords()
        val habitId = habitId ?: throw IllegalStateException("habitId must be set")
        repository.execSQL(
            "INSERT OR REPLACE INTO repetitions (habit, timestamp, value, notes) VALUES (?, ?, ?, ?)",
            habitId,
            entry.timestamp.unixTime,
            entry.value,
            entry.notes
        )
        super.add(entry)
    }

    @Synchronized
    override fun getKnown(): List<Entry> {
        loadRecords()
        return super.getKnown()
    }

    @Synchronized
    internal fun reload() {
        super.clear()
        isLoaded = false
    }

    override fun recomputeFrom(
        originalEntries: EntryList,
        frequency: Frequency,
        isNumerical: Boolean,
        targetValue: Double,
        targetType: NumericalHabitType
    ) {
        throw UnsupportedOperationException()
    }

    @Synchronized
    override fun clear() {
        repository.execSQL(
            "delete from repetitions where habit = ?",
            habitId.toString()
        )
        super.clear()
    }
}
