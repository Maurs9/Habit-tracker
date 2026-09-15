package org.isoron.uhabits.notifications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.automation.FireSettingReceiver
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.receivers.ReminderReceiver
import org.isoron.uhabits.receivers.WidgetReceiver
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SnoozeNotificationTest : BaseAndroidTest() {
    @Test
    fun testSnoozePendingIntentTargetsPickerAndKeepsHabitIdentity() {
        val habit = fixtures.createEmptyHabit()
        val otherHabit = fixtures.createEmptyHabit()
        val factory = appComponent.pendingIntentFactory
        val pendingIntent = factory.snoozeNotification(habit)
        try {
            val expected = PendingIntent.getActivity(
                targetContext,
                0,
                Intent(targetContext, SnoozeDelayPickerActivity::class.java).apply {
                    action = ReminderReceiver.ACTION_SNOOZE_REMINDER
                    data = Uri.parse(habit.uriString)
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            assertEquals(expected, pendingIntent)
            assertEquals(pendingIntent, factory.snoozeNotification(habit))
            val otherPendingIntent = factory.snoozeNotification(otherHabit)
            try {
                assertFalse(pendingIntent == otherPendingIntent)
            } finally {
                otherPendingIntent.cancel()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertTrue(pendingIntent.isActivity)
                assertTrue(pendingIntent.isImmutable)
            }
        } finally {
            pendingIntent.cancel()
        }
    }

    @Test
    fun testBooleanAndNumericalNotificationsAlwaysOfferSnooze() {
        val factory = appComponent.pendingIntentFactory
        val tray = AndroidNotificationTray(targetContext, factory, prefs, RingtoneManager(targetContext))
        for (habit in listOf(fixtures.createEmptyHabit(), fixtures.createLongNumericalHabit())) {
            val notification = tray.buildNotification(habit, 0, Timestamp.ZERO, disableSound = true)
            val snooze = notification.actions.last()
            assertEquals(targetContext.getString(R.string.snooze), snooze.title.toString())
            assertEquals(factory.snoozeNotification(habit), snooze.actionIntent)
            val wearableSnooze = NotificationCompat.WearableExtender(notification).actions.last()
            assertEquals(snooze.actionIntent, wearableSnooze.actionIntent)
            snooze.actionIntent.cancel()
        }
    }

    @Test
    fun testRegularAndSnoozedAlarmIdentitiesAreDistinctAndCancelledTogether() {
        val habit = fixtures.createEmptyHabit()
        val other = fixtures.createEmptyHabit()
        val factory = appComponent.pendingIntentFactory
        val regular = factory.showReminder(habit, 100L, 0L)
        val snoozed = factory.showSnoozedReminder(habit, 200L, 0L)
        val picker = factory.snoozeNotification(habit)
        val otherAlarm = factory.showReminder(other, 100L, 0L)
        try {
            assertFalse(regular == snoozed)
            assertFalse(regular == picker)
            assertFalse(snoozed == picker)
            assertFalse(regular == otherAlarm)
            appComponent.intentScheduler!!.cancelReminders(habit.id!!)
            assertNull(factory.existingReminder(habit.id!!, false))
            assertNull(factory.existingReminder(habit.id!!, true))
            assertEquals(otherAlarm, factory.existingReminder(other.id!!, false))
        } finally {
            regular.cancel()
            snoozed.cancel()
            picker.cancel()
            otherAlarm.cancel()
        }
    }

    @Test
    fun testManifestPreservesPublicAutomationAndPrivatePicker() {
        val manager = targetContext.packageManager
        val widget = manager.getReceiverInfo(ComponentName(targetContext, WidgetReceiver::class.java), 0)
        assertTrue(widget.exported)
        assertNull(widget.permission)
        val tasker = manager.getReceiverInfo(ComponentName(targetContext, FireSettingReceiver::class.java), 0)
        assertTrue(tasker.exported)
        val picker = manager.getActivityInfo(ComponentName(targetContext, SnoozeDelayPickerActivity::class.java), 0)
        assertFalse(picker.exported)
        val reminders = manager.getReceiverInfo(ComponentName(targetContext, ReminderReceiver::class.java), 0)
        assertFalse(reminders.exported)
    }
}
