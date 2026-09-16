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

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HabitsCSVExporterTest : BaseUnitTest() {
    private lateinit var baseDir: File

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        habitList.add(fixtures.createShortHabit())
        habitList.add(fixtures.createEmptyHabit())
        baseDir = Files.createTempDirectory("csv").toFile()
        baseDir.deleteOnExit()
    }

    @Test
    fun testNumericalExportSeparatesMeasurementsAndAutomaticDays() {
        habitList.removeAll()
        val today = DateUtils.getTodayWithOffset()
        val habit = modelFactory.buildHabit().apply {
            name = "Measure"
            type = HabitType.NUMERICAL
            targetValue = 5.0
            frequency = Frequency(1, 3)
            originalEntries.add(Entry(today.minus(2), 5000))
            originalEntries.add(Entry(today.minus(1), 1))
            recompute()
        }
        habitList.add(habit)
        val filename = HabitsCSVExporter(habitList, sectionList, listOf(habit), baseDir).writeArchive()
        try {
            ZipFile(filename).use { archive ->
                for (path in listOf("Checkmarks.csv", "001 Measure/Checkmarks.csv")) {
                    val values = archive.getInputStream(archive.getEntry(path)).bufferedReader().use { reader ->
                        reader.readLines().drop(1).map { it.split(',')[1] }
                    }
                    assertEquals(listOf("YES_AUTO", "1", "5000"), values, path)
                }
            }
        } finally {
            File(filename).delete()
        }
    }

    @Test
    fun testSectionNamesAreQuotedInArchive() {
        val section = sectionList.add("Morning, \"quiet\"\ntime")
        val habit = habitList.getByPosition(0)
        habit.sectionId = section.id
        habitList.update(habit)
        val filename = HabitsCSVExporter(habitList, sectionList, habitList.toList(), baseDir).writeArchive()
        try {
            ZipFile(filename).use { archive ->
                val csv = archive.getInputStream(archive.getEntry("Habits.csv")).bufferedReader().use { it.readText() }
                assertTrue(csv.contains("Tags,Section,ReminderTimes"))
                assertTrue(csv.contains("\"Morning, \"\"quiet\"\"\ntime\""))
            }
        } finally {
            File(filename).delete()
        }
    }

    @Test
    @Throws(IOException::class)
    fun testExportCSV() {
        val selected: MutableList<Habit> = LinkedList()
        for (h in habitList) selected.add(h)
        val exporter = HabitsCSVExporter(
            habitList,
            sectionList,
            selected,
            baseDir
        )
        val filename = exporter.writeArchive()
        assertAbsolutePathExists(filename)
        val archive = File(filename)
        unzip(archive)
        val filesToCheck = arrayOf(
            "001 Meditate/Checkmarks.csv",
            "001 Meditate/Scores.csv",
            "002 Wake up early/Checkmarks.csv",
            "002 Wake up early/Scores.csv",
            "Checkmarks.csv",
            "Habits.csv",
            "Scores.csv"
        )

        for (file in filesToCheck) {
            assertPathExists(file)
            assertFileAndReferenceAreEqual(file)
        }
    }

    @Throws(IOException::class)
    private fun unzip(file: File) {
        ZipFile(file).use { zip ->
            val e = zip.entries()
            while (e.hasMoreElements()) {
                val entry = e.nextElement()
                val out = File(baseDir, entry.name)
                out.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun assertPathExists(s: String) {
        assertAbsolutePathExists(String.format("%s/%s", baseDir.absolutePath, s))
    }

    private fun assertAbsolutePathExists(s: String) {
        val file = File(s)
        assertTrue(
            String.format("File %s should exist", file.absolutePath)
        ) { file.exists() }
    }

    private fun assertFileAndReferenceAreEqual(s: String) {
        val assetFilename = String.format("csv_export/%s", s)
        val actualFile = File(String.format("%s/%s", baseDir.absolutePath, s))
        val expectedFile = File.createTempFile("asset", "")
        expectedFile.deleteOnExit()
        copyAssetToFile(assetFilename, expectedFile)
        val actualContents = actualFile.readText()
        val expectedContents = expectedFile.readText()
        if (actualContents != expectedContents) {
            File("build/failed", assetFilename).apply {
                parentFile.mkdirs()
                writeText(actualContents)
            }
        }
        assertEquals(expectedContents, actualContents, "content mismatch for $s")
    }
}
