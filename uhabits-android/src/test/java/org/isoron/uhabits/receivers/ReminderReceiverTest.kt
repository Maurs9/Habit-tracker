package org.isoron.uhabits.receivers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.isoron.uhabits.BaseAndroidJVMTest
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.tasks.StartupCoordinator
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ReminderReceiverTest : BaseAndroidJVMTest() {
    @Test
    fun testSnoozedBroadcastPreservesSnoozeFlag() {
        assertDispatch(ReminderReceiver.ACTION_SHOW_SNOOZED_REMINDER, snoozed = true)
    }

    @Test
    fun testRegularBroadcastDoesNotSetSnoozeFlag() {
        assertDispatch(ReminderReceiver.ACTION_SHOW_REMINDER, snoozed = false)
    }

    private fun assertDispatch(action: String, snoozed: Boolean) {
        val context: Context = mock()
        val application: HabitsApplication = mock()
        val component: HabitsApplicationComponent = mock()
        val startup: StartupCoordinator = mock()
        val habits: HabitList = mock()
        val controller: ReminderController = mock()
        val intent: Intent = mock()
        val uri: Uri = mock()
        val habit = fixtures.createEmptyHabit().apply { id = 1L }
        val timestamp = DateUtils.getTodayWithOffset()
        val reminderTime = timestamp.unixTime + 8 * 60 * 60 * 1000 + 15 * 60 * 1000
        whenever(context.applicationContext).thenReturn(application)
        whenever(application.component).thenReturn(component)
        whenever(application.startup).thenReturn(startup)
        whenever(startup.state).thenReturn(StartupCoordinator.State.Ready)
        whenever(component.habitList).thenReturn(habits)
        whenever(component.reminderController).thenReturn(controller)
        whenever(intent.action).thenReturn(action)
        whenever(intent.data).thenReturn(uri)
        whenever(uri.lastPathSegment).thenReturn(habit.id.toString())
        whenever(habits.getById(habit.id!!)).thenReturn(habit)
        whenever(intent.getLongExtra(eq("timestamp"), any())).thenReturn(timestamp.unixTime)
        whenever(intent.getLongExtra(eq("reminderTime"), any())).thenReturn(reminderTime)
        try {
            mockStatic(Log::class.java).use {
                ReminderReceiver().onReceive(context, intent)
            }
            verify(controller).onShowReminder(habit, timestamp, reminderTime, snoozed)
            verifyNoMoreInteractions(controller)
        } finally {
            ReminderReceiver.clearLastReceivedIntent()
        }
    }
}
