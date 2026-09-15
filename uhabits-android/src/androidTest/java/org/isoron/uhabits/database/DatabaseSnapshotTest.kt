package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DatabaseSnapshotTest {
    private lateinit var directory: File
    private lateinit var db: SQLiteDatabase
    private lateinit var output: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.cacheDir, "snapshot-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdir())
        db = SQLiteDatabase.openOrCreateDatabase(File(directory, "live.db"), null)
        db.disableWriteAheadLogging()
        db.rawQuery("PRAGMA journal_mode=DELETE", null).use { assertTrue(it.moveToFirst()) }
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY, value TEXT)")
        db.execSQL("INSERT INTO records VALUES (1, 'before')")
        output = File(directory, "snapshot.db")
    }

    @After
    fun tearDown() {
        db.close()
        directory.deleteRecursively()
    }

    @Test
    fun excludesWriterOnSharedConnectionAndRoundTrips() {
        assertWriterExcluded(db)
    }

    @Test
    fun excludesWriterOnIndependentSQLiteConnection() {
        SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            assertWriterExcluded(it)
        }
    }

    @Test
    fun waitsForExistingWriterToCommitBeforeCopying() {
        val pool = Executors.newFixedThreadPool(2)
        val writing = CountDownLatch(1)
        val commit = CountDownLatch(1)
        try {
            val writer = pool.submit {
                db.beginTransaction()
                try {
                    db.execSQL("UPDATE records SET value='after'")
                    writing.countDown()
                    check(commit.await(5, TimeUnit.SECONDS))
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            assertTrue(writing.await(5, TimeUnit.SECONDS))
            val copied = CountDownLatch(1)
            val snapshot = pool.submit {
                DatabaseSnapshot.write(db, output)
                copied.countDown()
            }
            assertFalse(copied.await(150, TimeUnit.MILLISECONDS))
            commit.countDown()
            writer.get(5, TimeUnit.SECONDS)
            snapshot.get(5, TimeUnit.SECONDS)
            assertSnapshotValue("after")
        } finally {
            commit.countDown()
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun rejectsNestedTransactionsAndWalWithoutPublishing() {
        db.beginTransaction()
        try {
            try {
                DatabaseSnapshot.write(db, output)
                fail("A nested transaction must not be copied")
            } catch (_: IllegalStateException) {
                assertFalse(output.exists())
            }
        } finally {
            db.endTransaction()
        }
        assertTrue(db.enableWriteAheadLogging())
        try {
            DatabaseSnapshot.write(db, output)
            fail("WAL requires a different snapshot protocol")
        } catch (_: IOException) {
            assertFalse(output.exists())
        }
        assertFalse(db.inTransaction())
    }

    @Test
    fun releasesLockAfterCopyFailure() {
        try {
            DatabaseSnapshot.write(db, output) { _, _ -> throw IOException("disk full") }
            fail("Copy failure should propagate")
        } catch (_: IOException) {
            assertFalse(db.inTransaction())
        }
        db.execSQL("UPDATE records SET value='after'")
        DatabaseSnapshot.write(db, output)
        assertSnapshotValue("after")
    }

    @Test
    fun preservesAdditionalTablesColumnsAndVersion() {
        db.execSQL("ALTER TABLE records ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE records SET tags='health,daily'")
        db.execSQL("CREATE TABLE additional_data (record INTEGER, payload BLOB)")
        db.execSQL("INSERT INTO additional_data VALUES (1, X'0100FF')")
        db.version = 27
        DatabaseSnapshot.write(db, output)
        SQLiteDatabase.openDatabase(output.path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals(27, it.version)
            it.rawQuery("SELECT tags FROM records WHERE id=1", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("health,daily", cursor.getString(0))
            }
            it.rawQuery("SELECT payload FROM additional_data WHERE record=1", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(byteArrayOf(1, 0, -1).contentEquals(cursor.getBlob(0)))
            }
        }
    }

    private fun assertWriterExcluded(writerDatabase: SQLiteDatabase) {
        val pool = Executors.newSingleThreadExecutor()
        val attempted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        try {
            var writer: java.util.concurrent.Future<*>? = null
            DatabaseSnapshot.write(db, output) { source, destination ->
                writer = pool.submit {
                    attempted.countDown()
                    writerDatabase.execSQL("UPDATE records SET value='after'")
                    finished.countDown()
                }
                assertTrue(attempted.await(5, TimeUnit.SECONDS))
                assertFalse("Writer escaped snapshot lock", finished.await(150, TimeUnit.MILLISECONDS))
                source.inputStream().use { input ->
                    destination.outputStream().use { input.copyTo(it) }
                }
            }
            writer!!.get(5, TimeUnit.SECONDS)
            assertSnapshotValue("before")
            db.rawQuery("SELECT value FROM records", null).use {
                assertTrue(it.moveToFirst())
                assertEquals("after", it.getString(0))
            }
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun assertSnapshotValue(value: String) {
        SQLiteDatabase.openDatabase(output.path, null, SQLiteDatabase.OPEN_READONLY).use {
            it.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
            }
            it.rawQuery("SELECT value FROM records", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(value, cursor.getString(0))
            }
        }
    }
}
