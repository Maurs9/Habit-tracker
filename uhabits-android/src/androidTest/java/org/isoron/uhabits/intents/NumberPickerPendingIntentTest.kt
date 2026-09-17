package org.isoron.uhabits.intents

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.utils.DateUtils
import org.isoron.uhabits.notifications.AndroidNotificationTray
import org.isoron.uhabits.notifications.RingtoneManager
import org.isoron.uhabits.widgets.CheckmarkWidget
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class NumberPickerPendingIntentTest : BaseAndroidTest() {
    @Test
    fun testWidgetRefreshAndNotificationReshowKeepTheirOwnDates() {
        prefs.isFirstRun = false
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val habit = fixtures.createEmptyHabit().apply {
            type = HabitType.NUMERICAL
            targetValue = 1.0
            reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        }
        val today = DateUtils.getTodayWithOffset()
        val yesterday = today.minus(1)
        val factory = appComponent.pendingIntentFactory
        val tray = AndroidNotificationTray(targetContext, factory, prefs, RingtoneManager(targetContext))
        val oldNotification = tray.buildNotification(habit, yesterday.unixTime, yesterday, disableSound = true)
        val oldEntryIntent = oldNotification.actions.first().actionIntent
        val widgetIntent = requireNotNull(CheckmarkWidget(targetContext, 42, habit).getOnClickPendingIntent(targetContext))
        val currentEntryIntent = factory.showNumberPicker(habit, today)
        val template = factory.showNumberPickerTemplate()
        try {
            assertFalse(oldEntryIntent == widgetIntent)
            assertFalse(oldEntryIntent == currentEntryIntent)
            assertFalse(widgetIntent == template)
            assertFalse(currentEntryIntent == template)
            assertEquals(widgetIntent, factory.showNumberPickerFromWidget(habit, today))
            assertEquals(oldEntryIntent, factory.showNumberPicker(habit, yesterday))

            val activity = instrumentation.startActivitySync(
                Intent(targetContext, ListHabitsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ) as ListHabitsActivity
            try {
                instrumentation.waitForIdleSync()
                assertDeliveredDate(oldEntryIntent, habit.id!!, yesterday)
                // Rebuilding an old notification must not rewrite the current widget's action.
                tray.buildNotification(habit, yesterday.unixTime, yesterday, disableSound = true)
                assertDeliveredDate(widgetIntent, habit.id!!, today)
                assertDeliveredDate(currentEntryIntent, habit.id!!, today)
            } finally {
                instrumentation.runOnMainSync { activity.finish() }
                instrumentation.waitForIdleSync()
            }
        } finally {
            oldEntryIntent.cancel()
            widgetIntent.cancel()
            currentEntryIntent.cancel()
            template.cancel()
            oldNotification.actions.last().actionIntent.cancel()
        }
    }

    private fun assertDeliveredDate(pendingIntent: PendingIntent, habitId: Long, expected: Timestamp) {
        val result = AtomicReference<Intent>()
        val delivered = CountDownLatch(1)
        pendingIntent.send(
            targetContext,
            0,
            null,
            { _, intent, _, _, _ ->
                result.set(intent)
                delivered.countDown()
            },
            Handler(Looper.getMainLooper())
        )
        assertTrue("PendingIntent was not delivered", delivered.await(10, TimeUnit.SECONDS))
        assertEquals(habitId, result.get().getLongExtra("habit", -1))
        assertEquals(expected.unixTime, result.get().getLongExtra("timestamp", -1))
    }
}
