package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.ReminderTimes
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Locale

data class BackupPreview(
    val version: Int,
    val habitCount: Long,
    val entryCount: Long,
    val latestEntryTimestamp: Long?
)

object BackupValidator {
    fun isSQLite(file: File): Boolean = file.inputStream().use {
        val header = ByteArray(16)
        it.read(header) == 16 && header.contentEquals("SQLite format 3\u0000".toByteArray())
    }

    fun isLoopDatabase(file: File): Boolean {
        if (!isSQLite(file)) return false
        return openReadOnly(file).use { db ->
            db.rawQuery(
                "SELECT count(*) FROM sqlite_master WHERE type='table' " +
                    "AND lower(name) IN ('habits', 'repetitions')",
                null
            ).use {
                it.moveToFirst()
                it.getInt(0) == 2
            }
        }
    }

    fun inspect(file: File): BackupPreview {
        if (!isSQLite(file)) throw IOException("Not a SQLite database")
        return openReadOnly(file).use { db ->
            val version = db.version
            if (version !in 9..DATABASE_VERSION) {
                throw IOException("Unsupported backup version: $version")
            }
            db.rawQuery("PRAGMA integrity_check", null).use {
                if (!it.moveToFirst() || it.getString(0) != "ok" || it.moveToNext()) {
                    throw IOException("Backup integrity check failed")
                }
            }
            val habits = mutableSetOf(
                "id", "name", "description", "freq_num", "freq_den", "color",
                "position", "archived", "highlight", "reminder_hour", "reminder_min"
            )
            if (version >= 11) habits.add("reminder_days")
            if (version >= 16) habits.add("type")
            if (version >= 18) habits.addAll(listOf("target_type", "target_value", "unit"))
            if (version >= 23) habits.add("question")
            if (version >= 24) habits.add("uuid")
            if (version >= 26) habits.add("tags")
            if (version >= 27) habits.add("reminder_times")
            val entries = mutableSetOf("id", "habit", "timestamp")
            if (version >= 16) entries.add("value")
            if (version >= 25) entries.add("notes")
            requireColumns(db, "Habits", habits)
            requireColumns(db, "Repetitions", entries)
            validateMetadata(db, version)
            val habitCount = count(db, "Habits")
            val entryCount = count(db, "Repetitions")
            val latest = db.rawQuery("SELECT max(timestamp) FROM Repetitions", null).use {
                it.moveToFirst()
                if (it.isNull(0)) null else it.getLong(0)
            }
            BackupPreview(version, habitCount, entryCount, latest)
        }
    }

    private fun count(db: SQLiteDatabase, table: String): Long =
        db.rawQuery("SELECT count(*) FROM $table", null).use {
            it.moveToFirst()
            it.getLong(0)
        }

    private fun validateMetadata(db: SQLiteDatabase, version: Int) {
        val invalidHabit = mutableListOf(
            "name IS NULL",
            "description IS NULL",
            "color IS NULL",
            "position IS NULL",
            "freq_num IS NULL",
            "freq_den IS NULL",
            "freq_num <= 0",
            "freq_den <= 0"
        )
        for (column in listOf("id", "color", "position", "freq_num", "freq_den")) {
            invalidHabit.add("typeof($column) != 'integer'")
        }
        // Earlier backups store Android ARGB values, converted by migration 14.
        if (version >= 14) invalidHabit.add("color < 0 OR color >= ${PaletteColor.COUNT}")
        if (version >= 16) invalidHabit.addAll(listOf("type IS NULL", "type NOT IN (0, 1)"))
        if (version >= 18) {
            invalidHabit.addAll(
                listOf(
                    "target_type IS NULL",
                    "target_value IS NULL",
                    "unit IS NULL",
                    "target_type NOT IN (0, 1)",
                    "typeof(target_value) NOT IN ('real', 'integer')"
                )
            )
        }
        if (version >= 23) invalidHabit.add("question IS NULL")
        if (version >= 24) invalidHabit.addAll(listOf("uuid IS NULL", "uuid = ''"))
        if (version >= 26) invalidHabit.add("tags IS NULL")
        if (version >= 27) invalidHabit.add("typeof(reminder_times) != 'text'")
        invalidHabit.addAll(
            listOf(
                "(reminder_hour IS NULL) != (reminder_min IS NULL)",
                "(reminder_hour IS NOT NULL AND (typeof(reminder_hour) != 'integer' " +
                    "OR reminder_hour NOT BETWEEN 0 AND 23))",
                "(reminder_min IS NOT NULL AND (typeof(reminder_min) != 'integer' " +
                    "OR reminder_min NOT BETWEEN 0 AND 59))"
            )
        )
        if (version >= 11) {
            invalidHabit.add(
                "(reminder_hour IS NOT NULL AND (typeof(reminder_days) != 'integer' " +
                    "OR reminder_days NOT BETWEEN 0 AND 127))"
            )
        }
        requireNoRows(db, "SELECT 1 FROM Habits WHERE ${invalidHabit.joinToString(" OR ")} LIMIT 1")
        if (version >= 27) validateReminderTimes(db)
        if (version >= 24) {
            requireNoRows(db, "SELECT uuid FROM Habits GROUP BY uuid HAVING count(*) > 1 LIMIT 1")
        }
        requireNoRows(db, "SELECT 1 FROM Repetitions WHERE timestamp < 0 LIMIT 1")
        if (version >= 22) {
            requireNoRows(
                db,
                "SELECT 1 FROM Repetitions WHERE habit IS NULL OR timestamp IS NULL OR value IS NULL " +
                    "OR typeof(habit) != 'integer' OR typeof(timestamp) != 'integer' " +
                    "OR typeof(value) != 'integer' OR habit NOT IN (SELECT id FROM Habits) LIMIT 1"
            )
            requireNoRows(
                db,
                "SELECT habit FROM Repetitions GROUP BY habit, timestamp HAVING count(*) > 1 LIMIT 1"
            )
        }
    }

    private fun validateReminderTimes(db: SQLiteDatabase) {
        db.rawQuery("SELECT reminder_hour, reminder_times FROM Habits", null).use {
            while (it.moveToNext()) {
                val times = try {
                    ReminderTimes.parse(it.getString(1))
                } catch (e: IllegalArgumentException) {
                    throw IOException("Invalid reminder times", e)
                }
                if (it.isNull(0) && times.isNotEmpty()) {
                    throw IOException("Secondary reminder times require an enabled primary reminder")
                }
            }
        }
    }

    private fun requireNoRows(db: SQLiteDatabase, query: String) {
        db.rawQuery(query, null).use {
            if (it.moveToFirst()) throw IOException("Invalid habit or entry metadata")
        }
    }

    private fun requireColumns(db: SQLiteDatabase, table: String, expected: Set<String>) {
        val actual = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($table)", null).use {
            while (it.moveToNext()) actual.add(it.getString(1).lowercase(Locale.ROOT))
        }
        if (!actual.containsAll(expected)) throw IOException("Invalid backup schema: $table")
        db.rawQuery("SELECT type FROM sqlite_master WHERE lower(name)=lower(?)", arrayOf(table)).use {
            if (!it.moveToFirst() || it.getString(0) != "table") {
                throw IOException("Invalid backup table: $table")
            }
        }
    }

    private fun openReadOnly(file: File): SQLiteDatabase {
        RandomAccessFile(file, "r").use {
            it.seek(18)
            if (it.read() != 1 || it.read() != 1) {
                throw IOException("Backup is incomplete or requires a write-ahead log")
            }
        }
        return SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
            // The default corruption handler deletes files. Inspection must never modify its input.
            { }
        )
    }
}
