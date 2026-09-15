package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * API 28-compatible snapshot of a rollback-journal database.
 *
 * BEGIN EXCLUSIVE waits for existing writers and excludes subsequent writers, including
 * connections outside the app's task runner. The transaction itself must never write:
 * uncommitted pages in a rollback journal are not a standalone backup.
 */
object DatabaseSnapshot {
    fun write(
        db: SQLiteDatabase,
        destination: File,
        copy: (File, File) -> Unit = ::copyAndSync
    ) {
        check(!db.inTransaction()) { "Cannot back up an uncommitted transaction" }
        require(File(db.path).canonicalFile != destination.canonicalFile)
        db.beginTransaction()
        try {
            val mode = db.rawQuery("PRAGMA journal_mode", null).use {
                check(it.moveToFirst())
                it.getString(0)
            }
            if (mode.lowercase(Locale.ROOT) !in setOf("delete", "truncate", "persist")) {
                throw IOException("Backup requires rollback journaling; found $mode")
            }
            copy(File(db.path), destination)
        } finally {
            db.endTransaction()
        }
    }

    private fun copyAndSync(source: File, destination: File) {
        source.inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }
}
