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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.utils.DatabaseUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AutoBackupTest : BaseAndroidTest() {
    private lateinit var directory: File
    private val now = System.currentTimeMillis()

    @Before
    fun prepareBackupDirectory() {
        directory = File(targetContext.cacheDir, "backup-test-${UUID.randomUUID()}")
        assertTrue(directory.mkdir())
        targetContext.getSharedPreferences("backup_status", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun removeBackupDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun testPrunesOnlyBackupsAfterSuccessfulPublicationAndEnforcesCount() {
        repeat(8) { oldBackup(it) }
        val unrelated = File(directory, "unrelated.txt").apply { writeText("keep") }
        val partial = File(directory, ".loop-backup-owned.partial").apply { writeText("keep") }
        val autoBackup = AutoBackup(targetContext, { directory }, clock = { now })
        assertTrue(autoBackup.run(keep = 3))
        assertEquals(3, directory.listFiles()!!.count { it.extension == "db" })
        assertEquals(3, autoBackup.availableBackups().size)
        assertTrue(unrelated.exists())
        assertTrue(partial.exists())
        assertEquals(now, BackupStatus(targetContext).lastSuccess)
    }

    @Test
    fun testFailureRetainsAllBackupsAndLastSuccess() {
        repeat(6) { oldBackup(it) }
        val original = directory.list()!!.toSet()
        val success = BackupStatus(targetContext).lastSuccess
        val autoBackup = AutoBackup(
            targetContext,
            { directory },
            snapshot = { _, _ -> throw IOException("disk full") },
            clock = { now }
        )
        assertFalse(autoBackup.run(keep = 2))
        assertEquals(original, directory.list()!!.toSet())
        assertEquals(success, BackupStatus(targetContext).lastSuccess)
        assertEquals(now, BackupStatus(targetContext).lastFailure)
    }

    @Test
    fun testCorruptRecentFileDoesNotSuppressBackup() {
        File(directory, "Loop Habits Backup 2026-09-15 120000.db").apply {
            writeText("not a database")
            setLastModified(now)
        }
        assertTrue(AutoBackup(targetContext, { directory }, clock = { now }).run(keep = 1))
        val output = directory.listFiles()!!.single()
        assertEquals(habitList.size().toLong(), BackupValidator.inspect(output).habitCount)
        assertEquals(0L, BackupStatus(targetContext).lastFailure)
    }

    @Test
    fun testFreshValidBackupIsReusedAndStillEnforcesCount() {
        repeat(6) { oldBackup(it) }
        val recent = oldBackup(10).apply { setLastModified(now - 1000) }
        val autoBackup = AutoBackup(
            targetContext,
            { directory },
            snapshot = { _, _ -> throw AssertionError("A fresh verified backup exists") },
            clock = { now }
        )
        assertTrue(autoBackup.run(keep = 2))
        assertEquals(2, directory.listFiles()!!.size)
        assertTrue(recent.exists())
    }

    @Test
    fun testUnavailableDirectoryDoesNotCrash() {
        assertFalse(AutoBackup(targetContext, { null }).run())
        assertTrue(BackupStatus(targetContext).lastFailure > 0)
    }

    @Test
    fun testMissingDirectoryIsRecreated() {
        assertTrue(directory.delete())
        assertTrue(AutoBackup(targetContext, { directory }).run())
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test
    fun testSameSecondExportsNeverOverwrite() {
        val first = DatabaseUtils.saveDatabaseCopy(targetContext, directory)
        val bytes = File(first).readBytes()
        val second = DatabaseUtils.saveDatabaseCopy(targetContext, directory)
        assertFalse(first == second)
        assertTrue(bytes.contentEquals(File(first).readBytes()))
        assertFalse(directory.listFiles()!!.any { it.name.endsWith(".partial") })
    }

    private fun oldBackup(index: Int): File {
        return File(DatabaseUtils.saveDatabaseCopy(targetContext, directory)).apply {
            assertTrue(setLastModified(now - (index + 2) * DateUtils.DAY_LENGTH))
        }
    }
}
