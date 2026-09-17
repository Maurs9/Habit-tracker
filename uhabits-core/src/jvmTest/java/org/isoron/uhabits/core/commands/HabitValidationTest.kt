package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HabitValidationTest : BaseUnitTest() {
    @Test
    fun frequencyValidatesBeforeNormalizing() {
        for ((numerator, denominator) in listOf(0 to 0, 0 to 1, 2 to 1, 1 to 0, 1 to -1, -1 to Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { Frequency(numerator, denominator) }
        }
        assertEquals(Frequency.DAILY, Frequency(7, 7))
        val habit = modelFactory.buildHabit().apply { frequency = Frequency(1, Frequency.MAX_DENOMINATOR) }
        habit.recompute()
        assertTrue(habit.scores[DateUtils.getTodayWithOffset()].value.isFinite())
    }

    @Test
    fun extremeFrequenciesRemainSafeBeforeAndAfterEntriesAndReload() {
        val db = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            val today = DateUtils.getTodayWithOffset()
            for (type in HabitType.entries) {
                for (frequency in listOf(Frequency(1, 1_073_741_824), Frequency(2, Int.MAX_VALUE))) {
                    val model = factory.buildHabit().apply {
                        this.type = type
                        this.frequency = frequency
                        targetValue = 1.0
                    }
                    CreateHabitCommand(factory, habits, model).run()
                    val habit = habits.getByUUID(model.uuid)!!
                    val value = if (habit.isNumerical) 1000 else Entry.YES_MANUAL
                    CreateRepetitionCommand(habits, habit, today, value, "").run()
                    CreateRepetitionCommand(habits, habit, today.minus(1), value, "").run()
                    val restored = factory.buildHabitList().getById(habit.id!!)!!
                    restored.recompute()
                    assertEquals(frequency, restored.frequency)
                    assertEquals(2, restored.originalEntries.getKnown().size)
                    assertTrue(restored.scores[today].value.isFinite())
                    assertTrue(restored.computedEntries.getKnown().all { it.timestamp <= today.plus(30) })
                    assertTrue(
                        restored.computedEntries.getKnown().size <= today.unixTime / Timestamp.DAY_LENGTH + 31
                    )
                }
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun loadingExistingRecordsDoesNotApplyNewWriteValidation() {
        val db = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            val habit = factory.buildHabit().apply {
                type = HabitType.NUMERICAL
                targetValue = 1.0
            }
            habits.add(habit)
            db.execute("UPDATE habits SET target_value = -1 WHERE id = ?", habit.id!!)
            val restored = factory.buildHabitList().getById(habit.id!!)!!
            assertEquals(-1.0, restored.targetValue)
            restored.recompute()
            assertTrue(restored.scores[DateUtils.getTodayWithOffset()].value.isFinite())
            assertFailsWith<IllegalArgumentException> { habits.update(restored) }
        } finally {
            db.close()
        }
    }

    @Test
    fun invalidMutableMetadataNeverReachesStoredHabits() {
        val db = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            val original = factory.buildHabit().apply { name = "Original" }
            habits.add(original)
            val modified = factory.buildHabit().apply {
                name = "Invalid edit"
                frequency = Frequency(1, 7).also { it.denominator = 0 }
            }
            assertFailsWith<IllegalArgumentException> { CreateHabitCommand(factory, habits, modified).run() }
            assertFailsWith<IllegalArgumentException> { EditHabitCommand(habits, original.id!!, modified).run() }
            assertEquals(1, habits.size())
            assertEquals("Original", original.name)
            assertEquals(Frequency.DAILY, factory.buildHabitList().getById(original.id!!)!!.frequency)
            modified.frequency = Frequency.DAILY
            modified.type = HabitType.NUMERICAL
            for (target in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0)) {
                modified.targetValue = target
                assertFailsWith<IllegalArgumentException> { EditHabitCommand(habits, original.id!!, modified).run() }
                assertFailsWith<IllegalArgumentException> { habits.add(modified) }
            }
            assertEquals("Original", factory.buildHabitList().getById(original.id!!)!!.name)
        } finally {
            db.close()
        }
    }
}
