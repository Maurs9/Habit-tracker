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

import com.opencsv.CSVReader
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.Timestamp
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Class that imports data from HabitBull CSV files.
 */
class HabitBullCSVImporter
@Inject constructor(
    private val habitList: HabitList,
    private val modelFactory: ModelFactory,
    logging: Logging
) : AbstractImporter() {

    private val logger = logging.getLogger("HabitBullCSVImporter")

    override fun canHandle(file: File): Boolean {
        return BufferedReader(FileReader(file)).use {
            it.readLine()?.startsWith("HabitName,HabitDescription,HabitCategory") == true
        }
    }

    override fun importHabitsFromFile(file: File) {
        val rows = CSVReader(FileReader(file)).use { reader ->
            require(reader.readNext()?.take(3) == listOf("HabitName", "HabitDescription", "HabitCategory")) {
                "Unrecognized HabitBull header"
            }
            reader.map { cols ->
                require(cols.size >= 6) { "Incomplete HabitBull row" }
                val value = cols[4].toIntOrNull()
                require(value != null && value in 0..Int.MAX_VALUE / 1000) {
                    "Invalid HabitBull measurement: ${cols[4]}"
                }
                ImportRow(cols[0], cols[1], parseTimestamp(cols[3]), value, cols[5])
            }
        }
        for ((name, entries) in rows.groupBy { it.name }) {
            val numerical = entries.any { it.value > 1 }
            val habit = modelFactory.buildHabit().apply {
                this.name = name
                description = entries.first().description
                frequency = Frequency.DAILY
                type = if (numerical) HabitType.NUMERICAL else HabitType.YES_NO
            }
            habitList.add(habit)
            logger.info("Creating habit: $name")
            for (row in entries) {
                val value = if (numerical) row.value * 1000 else if (row.value == 1) Entry.YES_MANUAL else Entry.NO
                habit.originalEntries.add(Entry(row.timestamp, value, row.notes))
            }
            habit.recompute()
        }
        habitList.resort()
    }

    private fun parseTimestamp(rawValue: String): Timestamp {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            DateFormat.getDateInstance(DateFormat.SHORT),
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        )
        for (fmt in formats) {
            fmt.isLenient = false
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            val position = ParsePosition(0)
            val date = fmt.parse(rawValue, position)
            if (date != null && position.index == rawValue.length && position.errorIndex < 0) {
                return Timestamp(date.time)
            }
        }
        throw IllegalArgumentException("Unrecognized date format: $rawValue")
    }

    private data class ImportRow(
        val name: String,
        val description: String,
        val timestamp: Timestamp,
        val value: Int,
        val notes: String
    )
}
