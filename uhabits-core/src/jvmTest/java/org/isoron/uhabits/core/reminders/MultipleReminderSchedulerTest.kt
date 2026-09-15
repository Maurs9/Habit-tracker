package org.isoron.uhabits.core.reminders

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.BulkSkipCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.ChangeReminderTimesCommand
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.preferences.PropertiesStorage
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.util.TimeZone

class MultipleReminderSchedulerTest : BaseUnitTest() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    private lateinit var habit: Habit
    private lateinit var scheduler: ReminderScheduler
    private lateinit var prefs: WidgetPreferences
    private lateinit var system: RecordingScheduler

    override fun setUp() {
        super.setUp()
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("UTC"))
        setNow("2026-09-14T07:00:00Z")
        habit = fixtures.createEmptyHabit()
        habit.replaceReminderTimes(listOf(480, 720, 1200))
        habitList.add(habit)
        prefs = WidgetPreferences(PropertiesStorage(temporaryFolder.newFile()))
        system = RecordingScheduler()
        scheduler = ReminderScheduler(commandRunner, habitList, system, prefs)
        scheduler.startListening()
    }

    override fun tearDown() {
        scheduler.stopListening()
        DateUtils.setFixedTimeZone(null)
        super.tearDown()
    }

    @Test
    fun testEachDailyTimeRollsForwardWithoutAccumulatingAlarms() {
        scheduler.scheduleAll()
        assertRegular("2026-09-14T08:00:00Z")
        fire("2026-09-14T08:00:00Z")
        assertRegular("2026-09-14T12:00:00Z")
        fire("2026-09-14T12:00:00Z")
        assertRegular("2026-09-14T20:00:00Z")
        fire("2026-09-14T20:00:00Z")
        assertRegular("2026-09-15T08:00:00Z")
    }

    @Test
    fun testBulkSkipReschedulesOnlySelectedHabitsAndClearsTheirSnooze() {
        val numerical = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_MOST).apply {
            replaceReminderTimes(listOf(480, 720))
            habitList.add(this)
        }
        val untouched = fixtures.createEmptyHabit().apply {
            replaceReminderTimes(listOf(480, 1200))
            habitList.add(this)
        }
        scheduler.scheduleAll()
        scheduler.snoozeAtTime(habit, instant("2026-09-14T09:00:00Z"))
        val cancellations = system.cancelCount
        val today = DateUtils.getTodayWithOffset()
        commandRunner.run(BulkSkipCommand(habitList, listOf(habit, numerical), today, today))
        assertEquals(cancellations + 2, system.cancelCount)
        assertEquals(
            mapOf(
                (habit.id!! to false) to instant("2026-09-15T08:00:00Z"),
                (numerical.id!! to false) to instant("2026-09-15T08:00:00Z"),
                (untouched.id!! to false) to instant("2026-09-14T08:00:00Z")
            ),
            system.alarms
        )
        assertEquals(0L, prefs.getSnoozeTime(habit.id!!))
    }

    @Test
    fun testChangingTagsDoesNotCancelReminders() {
        scheduler.scheduleAll()
        val cancellations = system.cancelCount
        commandRunner.run(ChangeHabitTagsCommand(habitList, listOf(habit), setOf("Health")))
        assertEquals(cancellations, system.cancelCount)
        assertRegular("2026-09-14T08:00:00Z")
    }

    @Test
    fun testWeekdaysApplyToAllTimes() {
        habit.reminder = Reminder(8, 0, WeekdayList(4))
        setNow("2026-09-15T07:00:00Z")
        scheduler.scheduleAll()
        assertRegular("2026-09-21T08:00:00Z")
    }

    @Test
    fun testStaleDeliveryAfterWeekdayChangeIsIgnored() {
        habit.reminder = Reminder(8, 0, WeekdayList(4))
        setNow("2026-09-20T08:00:00Z")
        assertFalse(scheduler.onReminderFired(habit, instant("2026-09-20T08:00:00Z"), false))
        assertRegular("2026-09-21T08:00:00Z")
    }

    @Test
    fun testCancelledAndRepeatedSnoozeDeliveriesAreIgnored() {
        scheduler.snoozeAtTime(habit, instant("2026-09-14T09:00:00Z"))
        fire("2026-09-14T09:00:00Z", snoozed = true)
        assertFalse(scheduler.onReminderFired(habit, instant("2026-09-14T09:00:00Z"), true))
        habit.isArchived = true
        assertFalse(scheduler.onReminderFired(habit, instant("2026-09-14T12:00:00Z"), false))
        assertTrue(system.alarms.isEmpty())
    }

    @Test
    fun testSkippedNumericalHabitDoesNotRemindAgainToday() {
        habit.type = HabitType.NUMERICAL
        habit.targetType = NumericalHabitType.AT_MOST
        habit.originalEntries.add(Entry(DateUtils.getTodayWithOffset(), Entry.SKIP))
        habit.recompute()
        scheduler.scheduleAll()
        assertRegular("2026-09-15T08:00:00Z")
    }

    @Test
    fun testRemovingTimeCancelsPendingAlarmAndRejectsStaleDelivery() {
        scheduler.scheduleAll()
        commandRunner.run(ChangeReminderTimesCommand(habitList, habit.id!!, listOf(720, 1200)))
        assertRegular("2026-09-14T12:00:00Z")
        assertFalse(scheduler.onReminderFired(habit, instant("2026-09-14T08:00:00Z"), false))
        assertRegular("2026-09-14T12:00:00Z")
    }

    @Test
    fun testSnoozePausesAllTimesAndResumesAfterSingleSnoozeAlarm() {
        scheduler.scheduleAll()
        scheduler.snoozeAtTime(habit, instant("2026-09-14T09:00:00Z"))
        assertEquals(mapOf((habit.id!! to true) to instant("2026-09-14T09:00:00Z")), system.alarms)
        setNow("2026-09-14T08:00:00Z")
        assertFalse(scheduler.onReminderFired(habit, instant("2026-09-14T08:00:00Z"), false))
        fire("2026-09-14T09:00:00Z", snoozed = true)
        assertEquals(0L, prefs.getSnoozeTime(habit.id!!))
        assertRegular("2026-09-14T12:00:00Z")
    }

    @Test
    fun testSnoozeSurvivesSchedulerRecreationAndTimezoneChange() {
        scheduler.snoozeAtTime(habit, instant("2026-09-14T09:00:00Z"))
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("America/New_York"))
        setNow("2026-09-14T07:00:00Z")
        ReminderScheduler(commandRunner, habitList, system, prefs).scheduleAll()
        assertEquals(instant("2026-09-14T09:00:00Z"), system.alarms[habit.id!! to true])
        fire("2026-09-14T09:00:00Z", snoozed = true)
        assertRegular("2026-09-14T12:00:00Z")
    }

    @Test
    fun testStartupAfterSnoozeDeadlinePreservesDeliveryExactlyOnce() {
        val file = temporaryFolder.newFile()
        val originalPrefs = WidgetPreferences(PropertiesStorage(file))
        val previous = ReminderScheduler(commandRunner, habitList, system, originalPrefs)
        val deadline = instant("2026-09-14T09:00:00Z")
        previous.snoozeAtTime(habit, deadline)

        setNow("2026-09-14T09:02:00Z")
        system.alarms.clear()
        val restoredPrefs = WidgetPreferences(PropertiesStorage(file))
        val restored = ReminderScheduler(commandRunner, habitList, system, restoredPrefs)
        restored.scheduleAll()

        assertEquals(deadline, restoredPrefs.getSnoozeTime(habit.id!!))
        assertEquals(mapOf((habit.id!! to true) to deadline), system.alarms)
        assertTrue(restored.onReminderFired(habit, deadline, snoozed = true))
        assertEquals(0L, restoredPrefs.getSnoozeTime(habit.id!!))
        assertRegular("2026-09-14T12:00:00Z")
        assertFalse(restored.onReminderFired(habit, deadline, snoozed = true))
    }

    @Test
    fun testCompletionSkipsRemainingTimesAndArchiveAndDeleteCancel() {
        scheduler.scheduleAll()
        habit.originalEntries.add(Entry(DateUtils.getTodayWithOffset(), Entry.YES_MANUAL))
        habit.recompute()
        scheduler.scheduleAll()
        assertRegular("2026-09-15T08:00:00Z")
        habit.isArchived = true
        scheduler.scheduleAll()
        assertTrue(system.alarms.isEmpty())
        habit.isArchived = false
        scheduler.scheduleAll()
        commandRunner.run(DeleteHabitsCommand(habitList, listOf(habit)))
        assertTrue(system.alarms.isEmpty())
        assertTrue(prefs.scheduledReminderHabitIds.isEmpty())
    }

    @Test
    fun testRemovedHabitIsCancelledAfterSchedulerRecreation() {
        scheduler.scheduleAll()
        habitList.remove(habit)
        ReminderScheduler(commandRunner, habitList, system, prefs).scheduleAll()
        assertTrue(system.alarms.isEmpty())
    }

    @Test
    fun testDisablingRemindersClearsSnoozeAndAlarms() {
        scheduler.snoozeAtTime(habit, instant("2026-09-14T09:00:00Z"))
        commandRunner.run(ChangeReminderTimesCommand(habitList, habit.id!!, emptyList()))
        assertTrue(system.alarms.isEmpty())
        assertEquals(0L, prefs.getSnoozeTime(habit.id!!))
    }

    @Test
    fun testSpringDstGapUsesNextValidWallTime() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("America/New_York"))
        setNow("2026-03-08T05:00:00Z")
        habit.replaceReminderTimes(listOf(150))
        scheduler.scheduleAll()
        assertRegular("2026-03-08T07:30:00Z")
        fire("2026-03-08T07:30:00Z")
        assertRegular("2026-03-09T06:30:00Z")
    }

    @Test
    fun testFallDstOverlapDoesNotRepeatSameDailyTime() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("America/New_York"))
        setNow("2026-11-01T04:00:00Z")
        habit.replaceReminderTimes(listOf(90))
        scheduler.scheduleAll()
        assertRegular("2026-11-01T05:30:00Z")
        fire("2026-11-01T05:30:00Z")
        assertRegular("2026-11-02T06:30:00Z")
    }

    @Test
    fun testAll1440TimesStillUseOneAlarmAndNeverSchedulePastTime() {
        habit.replaceReminderTimes((0 until 1440).toList())
        setNow("2026-09-14T00:00:30Z")
        scheduler.scheduleAll()
        assertRegular("2026-09-14T00:01:00Z")
    }

    private fun assertRegular(expected: String) {
        assertEquals(mapOf((habit.id!! to false) to instant(expected)), system.alarms)
    }

    private fun fire(time: String, snoozed: Boolean = false) {
        setNow(time)
        assertTrue(scheduler.onReminderFired(habit, instant(time), snoozed))
    }

    private fun setNow(time: String) {
        DateUtils.setFixedLocalTime(DateUtils.removeTimezone(instant(time)))
    }

    private fun instant(value: String): Long = Instant.parse(value).toEpochMilli()

    private class RecordingScheduler : ReminderScheduler.SystemScheduler {
        val alarms = mutableMapOf<Pair<Long, Boolean>, Long>()
        var cancelCount = 0
        override fun scheduleShowReminder(reminderTime: Long, habit: Habit, timestamp: Long): ReminderScheduler.SchedulerResult {
            alarms[habit.id!! to false] = reminderTime
            return ReminderScheduler.SchedulerResult.OK
        }
        override fun scheduleSnoozedReminder(reminderTime: Long, habit: Habit, timestamp: Long): ReminderScheduler.SchedulerResult {
            alarms[habit.id!! to true] = reminderTime
            return ReminderScheduler.SchedulerResult.OK
        }
        override fun cancelReminders(habitId: Long) {
            cancelCount++
            alarms.remove(habitId to false)
            alarms.remove(habitId to true)
        }
        override fun scheduleWidgetUpdate(updateTime: Long) = ReminderScheduler.SchedulerResult.OK
        override fun log(componentName: String, msg: String) = Unit
    }
}
