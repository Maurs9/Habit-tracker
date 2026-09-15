package org.isoron.uhabits.notifications

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SnoozeDelayPickerActivityTest : BaseAndroidTest() {
    @Test
    fun testInvalidAndDeletedHabitsFinishWithoutCrashing() {
        val habit = fixtures.createEmptyHabit()
        val deletedUri = Uri.parse(habit.uriString)
        habitList.remove(habit)
        for (uri in listOf(null, Uri.parse("content://org.isoron.uhabits/habit/invalid"), deletedUri)) {
            ActivityScenario.launch<SnoozeDelayPickerActivity>(pickerIntent(uri)).use { scenario ->
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            }
        }
    }

    @Test
    fun testCustomPickerSurvivesRecreationAndCancelClosesActivity() {
        val habit = fixtures.createEmptyHabit()
        habit.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        habitList.update(habit)
        val widgetPreferences = appComponent.widgetPreferences
        widgetPreferences.removeSnoozeTime(habit.id!!)
        ActivityScenario.launch<SnoozeDelayPickerActivity>(pickerIntent(Uri.parse(habit.uriString))).use { scenario ->
            onView(withText(R.string.interval_custom)).perform(click())
            scenario.recreate()
            onView(withId(R.id.time_picker_dialog)).check(matches(isDisplayed()))
            pressBack()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            assertEquals(0L, widgetPreferences.getSnoozeTime(habit.id!!))
        }
    }

    @Test
    fun testCustomPickerConfirmationStillSnoozesAfterRecreation() {
        val habit = fixtures.createEmptyHabit()
        habit.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        habitList.update(habit)
        val widgetPreferences = appComponent.widgetPreferences
        widgetPreferences.removeSnoozeTime(habit.id!!)
        try {
            ActivityScenario.launch<SnoozeDelayPickerActivity>(pickerIntent(Uri.parse(habit.uriString))).use { scenario ->
                onView(withText(R.string.interval_custom)).perform(click())
                scenario.recreate()
                onView(withId(R.id.done_button)).perform(click())
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
                assertTrue(widgetPreferences.getSnoozeTime(habit.id!!) > 0L)
            }
        } finally {
            widgetPreferences.removeSnoozeTime(habit.id!!)
        }
    }

    @Test
    fun testNewNotificationReplacesHabitInExistingPicker() {
        val first = fixtures.createEmptyHabit()
        val second = fixtures.createEmptyHabit()
        first.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        second.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
        habitList.update(listOf(first, second))
        val widgetPreferences = appComponent.widgetPreferences
        widgetPreferences.removeSnoozeTime(first.id!!)
        widgetPreferences.removeSnoozeTime(second.id!!)
        try {
            ActivityScenario.launch<SnoozeDelayPickerActivity>(pickerIntent(Uri.parse(first.uriString))).use { scenario ->
                onView(withText(R.string.interval_custom)).perform(click())
                targetContext.startActivity(pickerIntent(Uri.parse(second.uriString)))
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { assertEquals(Uri.parse(second.uriString), it.intent.data) }
                onView(withText(R.string.interval_15_minutes)).perform(click())
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertEquals(0L, widgetPreferences.getSnoozeTime(first.id!!))
                assertTrue(widgetPreferences.getSnoozeTime(second.id!!) > 0L)
            }
        } finally {
            widgetPreferences.removeSnoozeTime(first.id!!)
            widgetPreferences.removeSnoozeTime(second.id!!)
        }
    }

    @Test
    fun testHabitDeletedWhilePickerOpenIsNotSnoozed() {
        for (customTime in listOf(false, true)) {
            val habit = fixtures.createEmptyHabit()
            habit.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
            habitList.update(habit)
            val widgetPreferences = appComponent.widgetPreferences
            widgetPreferences.removeSnoozeTime(habit.id!!)
            ActivityScenario.launch<SnoozeDelayPickerActivity>(pickerIntent(Uri.parse(habit.uriString))).use { scenario ->
                if (customTime) onView(withText(R.string.interval_custom)).perform(click())
                scenario.onActivity { habitList.remove(habit) }
                if (customTime) {
                    onView(withId(R.id.done_button)).perform(click())
                } else {
                    onView(withText(R.string.interval_15_minutes)).perform(click())
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
                assertEquals(0L, widgetPreferences.getSnoozeTime(habit.id!!))
            }
        }
    }

    private fun pickerIntent(uri: Uri?) =
        Intent(targetContext, SnoozeDelayPickerActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
