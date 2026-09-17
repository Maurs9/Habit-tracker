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
package org.isoron.uhabits.core.ui.widgets

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.io.Logger
import org.isoron.uhabits.core.io.Logging
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Entry.Companion.nextToggleValue
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.utils.DateUtils.Companion.getTodayWithOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WidgetBehaviorTest : BaseUnitTest() {
    private lateinit var notificationTray: NotificationTray
    private lateinit var preferences: Preferences
    private lateinit var behavior: WidgetBehavior
    private lateinit var habit: Habit
    private lateinit var today: Timestamp
    private lateinit var logging: Logging

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        habit = fixtures.createEmptyHabit().apply { targetValue = 2.0 }.also(habitList::add)
        notificationTray = mock()
        preferences = mock()
        logging = StandardLogging()
        behavior = WidgetBehavior(habitList, commandRunner, notificationTray, preferences, logging)
        today = getTodayWithOffset()
    }

    @Test
    fun testOnAddRepetition() {
        setEntry(Entry.NO, "Keep notes")
        behavior.onAddRepetition(habit, today)
        assertEntry(Entry.YES_MANUAL, "Keep notes")
        verify(notificationTray).cancel(habit)
    }

    @Test
    fun testOnRemoveRepetition() {
        setEntry(Entry.YES_MANUAL, "Keep notes")
        behavior.onRemoveRepetition(habit, today)
        assertEntry(Entry.NO, "Keep notes")
        verify(notificationTray).cancel(habit)
    }

    @Test
    fun testOnToggleRepetition() {
        for (skipEnabled in listOf(true, false)) for (questionMarks in listOf(true, false)) for (
        currentValue in listOf(Entry.NO, Entry.YES_MANUAL, Entry.YES_AUTO, Entry.SKIP, Entry.UNKNOWN)
        ) {
            whenever(preferences.isSkipEnabled).thenReturn(skipEnabled)
            whenever(preferences.areQuestionMarksEnabled).thenReturn(questionMarks)
            val nextValue: Int = nextToggleValue(
                currentValue,
                isSkipEnabled = skipEnabled,
                areQuestionMarksEnabled = questionMarks
            )
            setEntry(currentValue, "Keep notes")
            behavior.onToggleRepetition(habit, today)
            assertEntry(nextValue, "Keep notes")
        }
    }

    @Test
    fun testOnIncrement() {
        habit.type = HabitType.NUMERICAL
        setEntry(500, "Keep notes")
        behavior.onIncrement(habit, today, 100)
        assertEntry(600, "Keep notes")
        verify(notificationTray).cancel(habit)
    }

    @Test
    fun testAdjustmentsNormalizeAllNonMeasurementStates() {
        habit.type = HabitType.NUMERICAL
        habit.targetValue = 1.0
        for (status in listOf(Entry.UNKNOWN, Entry.SKIP, Entry.NUMERICAL_AUTO)) {
            setEntry(if (status == Entry.NUMERICAL_AUTO) Entry.UNKNOWN else status, "Keep notes")
            habit.computedEntries.add(Entry(today, status))
            behavior.onIncrement(habit, today, 1000)
            assertEntry(1000, "Keep notes")
            assertTrue(habit.isCompletedToday())

            setEntry(if (status == Entry.NUMERICAL_AUTO) Entry.UNKNOWN else status, "Keep notes")
            habit.computedEntries.add(Entry(today, status))
            behavior.onDecrement(habit, today, 1000)
            assertEntry(0, "Keep notes")
        }
    }

    @Test
    fun testAdjustmentsPreserveSmallMeasurements() {
        habit.type = HabitType.NUMERICAL
        for (value in listOf(1, 2, 3, 4)) {
            setEntry(value, "Measured")
            behavior.onIncrement(habit, today, 100)
            assertEntry(value + 100, "Measured")
        }
    }

    @Test
    fun testLegacyNegativeAdjustmentIsNotUsedAsAMeasurement() {
        habit.type = HabitType.NUMERICAL
        setEntry(-1000, "Keep notes")
        behavior.onIncrement(habit, today, 1000)
        assertEntry(1000, "Keep notes")
    }

    @Test
    fun testOnDecrement() {
        habit.type = HabitType.NUMERICAL
        setEntry(500, "Keep notes")
        behavior.onDecrement(habit, today, 100)
        assertEntry(400, "Keep notes")
        verify(notificationTray).cancel(habit)
    }

    @Test
    fun testQueuedIncrementsAndDecrementsReadLatestValues() {
        habit.type = HabitType.NUMERICAL
        setEntry(0, "Keep notes")
        val queue = useQueuedCommands()
        behavior.onIncrement(habit, today, 1000)
        behavior.onIncrement(habit, today, 1000)
        assertEntry(0, "Keep notes")
        queue.drain()
        assertEntry(2000, "Keep notes")

        behavior.onDecrement(habit, today, 1000)
        behavior.onDecrement(habit, today, 1000)
        queue.drain()
        assertEntry(0, "Keep notes")
    }

    @Test
    fun testQueuedTogglesReadLatestValue() {
        setEntry(Entry.NO, "Keep notes")
        val queue = useQueuedCommands()
        behavior.onToggleRepetition(habit, today)
        behavior.onToggleRepetition(habit, today)
        queue.drain()
        assertEntry(Entry.NO, "Keep notes")
    }

    @Test
    fun testAllWidgetActionsPreserveNotesWrittenByEarlierQueuedCommands() {
        val actions: List<() -> Unit> = listOf(
            { behavior.onAddRepetition(habit, today) },
            { behavior.onRemoveRepetition(habit, today) },
            { behavior.onToggleRepetition(habit, today) },
            { behavior.onIncrement(habit, today, 1000) },
            { behavior.onDecrement(habit, today, 1000) }
        )
        for ((index, action) in actions.withIndex()) {
            habit.type = if (index >= 3) HabitType.NUMERICAL else HabitType.YES_NO
            setEntry(Entry.NO, "Old notes")
            val queue = useQueuedCommands()
            commandRunner.run(CreateRepetitionCommand(habitList, habit, today, Entry.NO, "New notes"))
            action()
            queue.drain()
            assertEquals("New notes", habit.originalEntries.get(today).notes)
        }
    }

    @Test
    fun testAdjustmentsSaturateWithoutOverflowOrNegativeMeasurements() {
        habit.type = HabitType.NUMERICAL
        setEntry(Int.MAX_VALUE - 100, "Keep notes")
        behavior.onIncrement(habit, today, 1000)
        assertEntry(Int.MAX_VALUE, "Keep notes")
        behavior.onIncrement(habit, today, Int.MAX_VALUE)
        assertEntry(Int.MAX_VALUE, "Keep notes")
        behavior.onDecrement(habit, today, Int.MAX_VALUE)
        assertEntry(0, "Keep notes")
        behavior.onDecrement(habit, today, Int.MAX_VALUE)
        assertEntry(0, "Keep notes")
    }

    @Test
    fun testNegativeAdjustmentAmountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { behavior.onIncrement(habit, today, -1) }
        assertThrows(IllegalArgumentException::class.java) { behavior.onDecrement(habit, today, Int.MIN_VALUE) }
    }

    @Test
    fun testQueuedUpdateToDeletedHabitIsLoggedAndCancelled() {
        setEntry(Entry.NO, "Keep notes")
        val logger: Logger = mock()
        logging = mock()
        whenever(logging.getLogger("UpdateRepetitionCommand")).thenReturn(logger)
        val queue = useQueuedCommands()
        behavior.onAddRepetition(habit, today)
        habitList.remove(habit)
        queue.drain()
        assertEntry(Entry.NO, "Keep notes")
        verify(logger).info("Cancelled repetition update for deleted habit=${habit.id}")
    }

    @Test
    fun testQueuedUpdateDoesNotSwallowOtherFailures() {
        setEntry(Entry.NO, "Keep notes")
        val queue = useQueuedCommands()
        behavior.onAddRepetition(habit, today)
        val failure = IllegalStateException("Database read failed")
        doThrow(failure).whenever(habitList).getById(habit.id!!)
        assertSame(failure, assertThrows(IllegalStateException::class.java) { queue.drain() })
        assertEntry(Entry.NO, "Keep notes")
    }

    private fun setEntry(value: Int, notes: String) {
        habit.originalEntries.add(Entry(today, value, notes))
        habit.recompute()
    }

    private fun assertEntry(value: Int, notes: String) {
        assertEquals(Entry(today, value, notes), habit.originalEntries.get(today))
    }

    private fun useQueuedCommands(): QueuedTaskRunner {
        val queue = QueuedTaskRunner()
        commandRunner = CommandRunner(queue)
        behavior = WidgetBehavior(habitList, commandRunner, notificationTray, preferences, logging)
        return queue
    }

    private class QueuedTaskRunner : TaskRunner {
        private val tasks = ArrayDeque<Task>()
        override val activeTaskCount: Int get() = tasks.size
        override fun addListener(listener: TaskRunner.Listener) = Unit
        override fun removeListener(listener: TaskRunner.Listener) = Unit
        override fun publishProgress(task: Task, progress: Int) = Unit
        override fun execute(task: Task) {
            tasks.addLast(task)
        }
        fun drain() {
            while (tasks.isNotEmpty()) {
                val task = tasks.removeFirst()
                task.doInBackground()
                task.onPostExecute()
            }
        }
    }
}
