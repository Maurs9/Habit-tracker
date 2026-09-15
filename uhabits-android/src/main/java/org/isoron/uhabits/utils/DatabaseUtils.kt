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
package org.isoron.uhabits.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.isoron.uhabits.HabitsApplication.Companion.isTestMode
import org.isoron.uhabits.HabitsDatabaseOpener
import org.isoron.uhabits.core.DATABASE_FILENAME
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.utils.DateFormats.Companion.getBackupDateFormat
import org.isoron.uhabits.core.utils.DateUtils.Companion.getLocalTime
import org.isoron.uhabits.database.BackupStatus
import org.isoron.uhabits.database.BackupValidator
import org.isoron.uhabits.database.DatabaseSnapshot
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.UUID

object DatabaseUtils {
    private var opener: HabitsDatabaseOpener? = null

    @JvmStatic
    fun getDatabaseFile(context: Context): File {
        return context.getDatabasePath(databaseFilename)
    }

    private val databaseFilename: String
        get() {
            var databaseFilename: String = DATABASE_FILENAME
            if (isTestMode()) databaseFilename = "test.db"
            return databaseFilename
        }

    fun initializeDatabase(context: Context?) {
        opener = HabitsDatabaseOpener(
            context!!,
            databaseFilename,
            DATABASE_VERSION
        )
    }

    @JvmStatic
    @Synchronized
    @Throws(IOException::class)
    fun saveDatabaseCopy(context: Context, dir: File): String {
        var temporary: File? = null
        val status = BackupStatus(context)
        try {
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Backup directory unavailable")
            val dateFormat: SimpleDateFormat = getBackupDateFormat()
            val date = dateFormat.format(getLocalTime())
            val destination = File(dir, "Loop Habits Backup $date ${UUID.randomUUID()}.db")
            temporary = File.createTempFile(".loop-backup-", ".partial", dir)
            val db = openDatabase()
            check(File(db.path).canonicalFile == getDatabaseFile(context).canonicalFile)
            DatabaseSnapshot.write(db, temporary)
            BackupValidator.inspect(temporary)
            if (!temporary.renameTo(destination)) throw IOException("Could not publish backup")
            status.succeeded(destination.absolutePath)
            Log.i("DatabaseUtils", "Backup published: ${destination.name}")
            return destination.absolutePath
        } catch (e: Exception) {
            status.failed(e)
            Log.e("DatabaseUtils", "Backup failed; previous backups retained", e)
            throw IOException("Could not create a verified database backup", e)
        } finally {
            temporary?.let {
                if (it.exists() && !it.delete()) Log.w("DatabaseUtils", "Could not remove ${it.name}")
            }
        }
    }

    fun openDatabase(): SQLiteDatabase {
        checkNotNull(opener)
        return opener!!.writableDatabase
    }
}
