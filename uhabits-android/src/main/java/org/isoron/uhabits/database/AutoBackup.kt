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
package org.isoron.uhabits.database

import android.content.Context
import android.util.Log
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.DatabaseUtils
import java.io.File
import java.io.IOException

class AutoBackup(
    private val context: Context,
    private val directory: () -> File? = { AndroidDirFinder(context).getFilesDir("Backups") },
    private val snapshot: (Context, File) -> String = DatabaseUtils::saveDatabaseCopy,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun availableBackups(): List<File> {
        val basedir = directory() ?: throw IOException("Backup storage unavailable")
        return if (basedir.exists()) listBackupFiles(basedir) else emptyList()
    }

    fun run(keep: Int = 5, force: Boolean = false): Boolean = synchronized(lock) {
        require(keep > 0)
        val status = BackupStatus(context)
        try {
            val basedir = directory() ?: throw IOException("Backup storage unavailable")
            if (!basedir.isDirectory && !basedir.mkdirs()) throw IOException("Backup storage unavailable")
            val files = listBackupFiles(basedir)
            val now = clock()
            // Modification dates alone do not establish success: interrupted/corrupt copies
            // from older releases must not suppress a new verified backup.
            val fresh = if (force) {
                null
            } else {
                files.firstOrNull { file ->
                    val age = now - file.lastModified()
                    file.absolutePath == status.lastSuccessFile &&
                        age in 0 until DateUtils.DAY_LENGTH && isValid(file)
                }
            }
            if (fresh != null) {
                prune(basedir, fresh, keep)
                return@synchronized true
            }
            val output = File(snapshot(context, basedir))
            BackupValidator.inspect(output)
            status.succeeded(output.absolutePath, now)
            // Keep the newly published backup even when another file has a future timestamp.
            prune(basedir, output, keep)
            true
        } catch (e: Exception) {
            status.failed(e, clock())
            Log.e("AutoBackup", "Automatic backup failed", e)
            false
        }
    }

    private fun prune(directory: File, newest: File, keep: Int) {
        val older = listBackupFiles(directory).filter { it != newest }
        for (file in older.drop(keep - 1)) {
            if (!file.delete()) throw IOException("Could not remove old backup: ${file.name}")
        }
    }

    private fun isValid(file: File): Boolean = try {
        BackupValidator.inspect(file)
        true
    } catch (e: Exception) {
        Log.w("AutoBackup", "Ignoring invalid backup: ${file.name}", e)
        false
    }

    private fun listBackupFiles(directory: File): List<File> {
        val files = directory.listFiles() ?: throw IOException("Could not list backup directory")
        return files.filter { it.isFile && backupName.matches(it.name) }
            .sortedByDescending { it.lastModified() }
    }

    companion object {
        private val lock = Any()
        private val backupName = Regex(
            """Loop Habits Backup \d{4}-\d{2}-\d{2} \d{6}(?: [a-f0-9-]{36})?\.db"""
        )
    }
}
