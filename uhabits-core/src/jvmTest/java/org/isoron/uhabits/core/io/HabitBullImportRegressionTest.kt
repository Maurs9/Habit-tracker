package org.isoron.uhabits.core.io

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitBullImportRegressionTest : BaseUnitTest() {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun numericalTypeAndAllOneValuesSurviveReloadRegardlessOfRowOrder() {
        for (values in listOf(listOf(1, 2, 1), listOf(2, 1, 1), listOf(1, 1, 2))) {
            val db = buildMemoryDatabase()
            try {
                val factory = SQLModelFactory(db)
                val habits = factory.buildHabitList()
                val rows = values.mapIndexed { index, value -> "Count,,Health,2025-01-0${index + 1},$value,Note $index" }
                val importer = HabitBullCSVImporter(habits, factory, StandardLogging())
                importer.importHabitsFromFile(csv(rows.joinToString("\n")))
                val restored = factory.buildHabitList().getByPosition(0)
                assertEquals(HabitType.NUMERICAL, restored.type)
                for ((index, value) in values.withIndex()) {
                    assertEquals(
                        Entry(Timestamp.from(2025, 0, index + 1), value * 1000, "Note $index"),
                        restored.originalEntries.get(Timestamp.from(2025, 0, index + 1))
                    )
                }
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun booleanRowsRemainBooleanAndReadersHandleEmptyFiles() {
        val importer = HabitBullCSVImporter(habitList, modelFactory, StandardLogging())
        assertFalse(importer.canHandle(folder.newFile()))
        importer.importHabitsFromFile(csv("Read,,Study,2025-01-01,1,\nRead,,Study,2025-01-02,0,"))
        val habit = habitList.getByPosition(0)
        assertEquals(HabitType.YES_NO, habit.type)
        assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(Timestamp.from(2025, 0, 1)).value)
        assertEquals(Entry.NO, habit.originalEntries.get(Timestamp.from(2025, 0, 2)).value)
    }

    @Test
    fun strictDatesHonorLocaleAndDoNotOverrideEarlierSuccessfulFormats() {
        val previous = Locale.getDefault()
        try {
            for ((locale, input, expected) in listOf(
                Triple(Locale.UK, "31/12/2024", Timestamp.from(2024, 11, 31)),
                Triple(Locale.UK, "04/11/2024", Timestamp.from(2024, 10, 4)),
                Triple(Locale.US, "04/11/2024", Timestamp.from(2024, 3, 11)),
                Triple(Locale.GERMANY, "2024-12-31", Timestamp.from(2024, 11, 31))
            )) {
                Locale.setDefault(locale)
                habitList.removeAll()
                HabitBullCSVImporter(habitList, modelFactory, StandardLogging())
                    .importHabitsFromFile(csv("Read,,Study,$input,1,"))
                assertEquals(expected, habitList.getByPosition(0).originalEntries.getKnown().single().timestamp)
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun malformedDatesAndMeasurementsFailBeforeAnyHabitIsCreated() {
        val importer = HabitBullCSVImporter(habitList, modelFactory, StandardLogging())
        val badRows = listOf(
            "Read,,Study,2025-02-30,1,",
            "Read,,Study,2025-01-01junk,1,",
            "Read,,Study,2025-01-01,-1,",
            "Read,,Study,2025-01-01,2147483647,",
            "Read,,Study,2025-01-01,1.5,",
            "Read,,Study"
        )
        for (badRow in badRows) {
            val file = csv("Valid,,Study,2025-01-01,1,\n$badRow")
            assertFailsWith<IllegalArgumentException> { importer.importHabitsFromFile(file) }
            assertTrue(habitList.isEmpty)
        }
    }

    private fun csv(rows: String): File = folder.newFile().apply {
        writeText("HabitName,HabitDescription,HabitCategory,CalendarDate,Value,CommentText\n$rows\n")
    }
}
