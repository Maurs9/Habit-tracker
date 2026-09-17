package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumericalDataRegressionTest : BaseUnitTest() {
    @Test
    fun threeThousandthsRoundTripsAsMeasurementNotSkip() {
        val db = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(db)
            val habits = factory.buildHabitList()
            val habit = factory.buildHabit().apply {
                type = HabitType.NUMERICAL
                targetValue = 0.003
            }
            habits.add(habit)
            val today = DateUtils.getTodayWithOffset()
            CreateRepetitionCommand(habits, habit, today, 3, "Measured").run()
            val restored = factory.buildHabitList().getById(habit.id!!)!!
            restored.recompute()
            assertEquals(Entry(today, 3, "Measured"), restored.originalEntries.get(today))
            assertEquals("3", restored.originalEntries.get(today).formatValue(true))
            assertTrue(restored.isCompletedToday())
            assertEquals(Score.compute(1.0, 0.0, 1.0), restored.scores[today].value)
            assertEquals(1, restored.streaks.getBest(1).single().length)
            assertEquals(3L, restored.computedEntries.getKnown().groupedSum(DateUtils.TruncateField.DAY, isNumerical = true).single().value)
        } finally {
            db.close()
        }
    }

    @Test
    fun numericalSkipsBridgeStreaksForBothTargetTypes() {
        val today = DateUtils.getTodayWithOffset()
        for (targetType in NumericalHabitType.entries) {
            val habit = modelFactory.buildHabit().apply {
                type = HabitType.NUMERICAL
                this.targetType = targetType
                targetValue = 5.0
            }
            habit.originalEntries.add(Entry(today, 5000))
            habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
            habit.originalEntries.add(Entry(today.minus(2), 5000))
            habit.recompute()
            assertEquals(listOf(Streak(today.minus(2), today)), habit.streaks.getBest(5))
            habit.originalEntries.clear()
            habit.originalEntries.add(Entry(today, Entry.SKIP))
            habit.recompute()
            assertTrue(habit.streaks.getBest(5).isEmpty())
        }
    }

    @Test
    fun yearlyTotalsUseLongAndSentinelsDoNotContribute() {
        val start = Timestamp.from(2025, 0, 1)
        val entries = (0 until 260).map { Entry(start.plus(it), 10_000_000) }
        assertEquals(
            listOf(EntryAggregate(start, 2_600_000_000L)),
            entries.groupedSum(DateUtils.TruncateField.YEAR, isNumerical = true)
        )
        val tiny = listOf(Entry.SKIP, Entry.UNKNOWN, Entry.NUMERICAL_AUTO, 3).mapIndexed { index, value ->
            Entry(start.plus(index), value)
        }
        assertEquals(3L, tiny.groupedSum(DateUtils.TruncateField.MONTH, isNumerical = true).single().value)
        val large = listOf(Entry(start, Int.MAX_VALUE), Entry(start.plus(1), Int.MAX_VALUE))
        assertEquals(4_294_967_294L, large.groupedSum(DateUtils.TruncateField.MONTH, isNumerical = true).single().value)
    }

    @Test
    fun weekdayFrequencyIgnoresUnknownsAndSkipsButCountsTinyMeasurements() {
        val day = Timestamp.from(2025, 0, 1)
        val entries = EntryList()
        entries.add(Entry(day, Entry.SKIP))
        entries.add(Entry(day.plus(7), Entry.UNKNOWN))
        entries.add(Entry(day.plus(14), 3))
        assertEquals(3, entries.computeWeekdayFrequency(true)[day]!![day.weekday])
    }

    @Test
    fun intervalsNearEpochDoNotShiftBeforeSupportedDates() {
        val habit = modelFactory.buildHabit().apply { frequency = Frequency(1, 3) }
        habit.originalEntries.add(Entry(Timestamp.ZERO.plus(1), Entry.YES_MANUAL))
        habit.originalEntries.add(Entry(Timestamp.ZERO.plus(2), Entry.YES_MANUAL))
        habit.recompute()
        assertTrue(habit.computedEntries.getKnown().all { it.timestamp >= Timestamp.ZERO })
        assertEquals(Entry.YES_MANUAL, habit.computedEntries.get(Timestamp.ZERO.plus(1)).value)
    }

    @Test
    fun boundedAutomaticEntriesNeverDiscardRecordedFutureMeasurements() {
        val today = DateUtils.getTodayWithOffset()
        val habit = modelFactory.buildHabit().apply {
            type = HabitType.NUMERICAL
            frequency = Frequency(1, Int.MAX_VALUE)
            targetValue = 1.0
        }
        val future = Entry(today.plus(100), 1000, "Future measurement")
        habit.originalEntries.add(Entry(today, 1000))
        habit.originalEntries.add(future)
        habit.recompute()
        assertEquals(future, habit.computedEntries.get(future.timestamp))
        assertTrue(habit.computedEntries.getKnown().filter { it.value == Entry.NUMERICAL_AUTO }.all { it.timestamp <= today.plus(30) })
    }
}
