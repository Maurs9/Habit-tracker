package org.isoron.uhabits.core.ui

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.BulkSkipCommand
import org.isoron.uhabits.core.commands.ChangeHabitSectionCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.DeleteSectionCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class NotificationReminderLifecycleTest : BaseUnitTest() {
    @Test
    fun testBulkSkipCancelsSelectedNotificationsEvenWhenNotTrackedInMemory() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val selected = listOf(reminderHabit(), reminderHabit())
        val untouched = reminderHabit()
        val today = DateUtils.getTodayWithOffset()
        val time = DateUtils.getUtcTime()
        tray.show(untouched, today, time)
        clearInvocations(system)
        tray.startListening()
        try {
            commandRunner.run(BulkSkipCommand(habitList, selected, today, today))
            for (habit in selected) verify(system).removeNotification(habit.id!!.toInt())
            verify(system, never()).removeNotification(untouched.id!!.toInt())
            tray.onNotificationsChanged()
            verify(system).showNotification(untouched, untouched.id!!.toInt(), today, time)
        } finally {
            tray.stopListening()
        }
    }

    @Test
    fun testHistoricalBulkSkipAndTagsLeaveCurrentNotificationAlone() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        val today = DateUtils.getTodayWithOffset()
        tray.show(habit, today, DateUtils.getUtcTime())
        clearInvocations(system)
        tray.startListening()
        try {
            commandRunner.run(BulkSkipCommand(habitList, listOf(habit), today.minus(1), today.minus(1)))
            commandRunner.run(ChangeHabitTagsCommand(habitList, listOf(habit), setOf("Health")))
            verifyNoInteractions(system)
        } finally {
            tray.stopListening()
        }
    }

    @Test
    fun testSectionChangesLeaveCurrentNotificationAlone() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        val section = sectionList.add("Morning")
        val today = DateUtils.getTodayWithOffset()
        val time = DateUtils.getUtcTime()
        tray.show(habit, today, time)
        clearInvocations(system)
        tray.startListening()
        try {
            commandRunner.run(ChangeHabitSectionCommand(habitList, listOf(habit), section.id))
            commandRunner.run(DeleteSectionCommand(sectionList, habitList, section.id))
            verifyNoInteractions(system)
            tray.onNotificationsChanged()
            verify(system).showNotification(habit, habit.id!!.toInt(), today, time)
        } finally {
            tray.stopListening()
        }
    }

    @Test
    fun testBulkSkipCancelsNumericalRemindersForBothTargetTypes() {
        for (targetType in NumericalHabitType.entries) {
            val system: NotificationTray.SystemTray = mock()
            val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
            val habit = reminderHabit().apply {
                type = HabitType.NUMERICAL
                this.targetType = targetType
                targetValue = 10.0
            }
            val today = DateUtils.getTodayWithOffset()
            tray.show(habit, today, DateUtils.getUtcTime())
            val command = BulkSkipCommand(habitList, listOf(habit), today, today)
            command.run()
            tray.onCommandFinished(command)
            verify(system).removeNotification(habit.id!!.toInt())
            clearInvocations(system)
            tray.onNotificationsChanged()
            verifyNoInteractions(system)
        }
    }

    @Test
    fun testMutableHabitChangesCannotLeaveCancelledNotificationActive() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        tray.show(habit, DateUtils.getTodayWithOffset(), DateUtils.getUtcTime())
        habit.extraReminderTimes = setOf(720, 1200)
        tray.cancel(habit)
        clearInvocations(system)
        tray.onNotificationsChanged()
        verifyNoInteractions(system)
    }

    @Test
    fun testQueuedNotificationDoesNotReappearAfterCancellation() {
        val queued: TaskRunner = mock()
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(queued, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        tray.show(habit, DateUtils.getTodayWithOffset(), DateUtils.getUtcTime())
        val task = argumentCaptor<Task>().apply { verify(queued).execute(capture()) }.firstValue
        tray.cancel(habit)
        task.doInBackground()
        task.onPostExecute()
        verify(system, never()).showNotification(any(), any(), any(), any())
    }

    @Test
    fun testArchivingCancelsActiveNotification() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        tray.show(habit, DateUtils.getTodayWithOffset(), DateUtils.getUtcTime())
        habit.isArchived = true
        tray.onCommandFinished(mock<Command>())
        verify(system).removeNotification(habit.id!!.toInt())
        clearInvocations(system)
        tray.onNotificationsChanged()
        verifyNoInteractions(system)
    }

    @Test
    fun testExplicitSnoozeCanDeliverOnUnselectedWeekday() {
        val system: NotificationTray.SystemTray = mock()
        val tray = NotificationTray(taskRunner, commandRunner, mock<Preferences>(), system)
        val habit = reminderHabit()
        val today = DateUtils.getTodayWithOffset()
        habit.reminder = Reminder(8, 0, WeekdayList(127 xor (1 shl today.weekday)))
        val time = DateUtils.getUtcTime()
        tray.show(habit, today, time)
        verify(system, never()).showNotification(any(), any(), any(), any())
        tray.show(habit, today, time, snoozed = true)
        verify(system).showNotification(habit, habit.id!!.toInt(), today, time)
    }

    private fun reminderHabit(): Habit = fixtures.createEmptyHabit().apply {
        reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        habitList.add(this)
    }
}
