package org.isoron.uhabits.notifications

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.show.ShowHabitActivity
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.intents.IntentFactory
import org.isoron.uhabits.utils.formatTime
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ReminderTimesDialogTest : BaseAndroidTest() {
    @Test
    fun testPendingPickerAndDraftSurviveRecreation() {
        val habit = habitAtNoon()
        openEditor(habit).use { scenario ->
            onView(withText(R.string.reminder_times_add)).perform(click())
            scenario.recreate()
            confirmTimePicker()
            scenario.recreate()
            onView(withText(R.string.save)).inRoot(isDialog()).perform(click())
            assertEquals(setOf(480, 720), habit.reminderTimes)
            assertEquals(Reminder(12, 0, WeekdayList(62)), habit.reminder)
        }
    }

    @Test
    fun testCancelDoesNotSaveDraft() {
        val habit = habitAtNoon()
        openEditor(habit).use {
            onView(withText(R.string.reminder_times_add)).perform(click())
            confirmTimePicker()
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click())
            assertEquals(setOf(720), habit.reminderTimes)
        }
    }

    @Test
    fun testDuplicateTimeShowsErrorWithoutDuplicatingSchedule() {
        val habit = habitAtNoon()
        habit.replaceReminderTimes(listOf(480, 720))
        habitList.update(habit)
        openEditor(habit).use {
            onView(withText(R.string.reminder_times_add)).perform(click())
            confirmTimePicker()
            onView(withText(R.string.reminder_times_duplicate)).check(matches(isDisplayed()))
            onView(withText(R.string.save)).inRoot(isDialog()).perform(click())
            assertEquals(setOf(480, 720), habit.reminderTimes)
        }
    }

    @Test
    fun testRemovingLastTimeDisablesAllReminders() {
        val habit = habitAtNoon()
        openEditor(habit).use {
            onView(withText(formatTime(targetContext, 12, 0))).inRoot(isDialog()).perform(click())
            onView(withText(R.string.reminder_times_remove)).perform(click())
            onView(withText(R.string.reminder_times_empty)).check(matches(isDisplayed()))
            onView(withText(R.string.save)).inRoot(isDialog()).perform(click())
            assertNull(habit.reminder)
            assertTrue(habit.extraReminderTimes.isEmpty())
        }
    }

    private fun habitAtNoon(): Habit = fixtures.createEmptyHabit().apply {
        reminder = Reminder(12, 0, WeekdayList(62))
        habitList.update(this)
    }

    private fun openEditor(habit: Habit): ActivityScenario<ShowHabitActivity> {
        val scenario = ActivityScenario.launch<ShowHabitActivity>(
            IntentFactory().startShowHabitActivity(targetContext, habit)
        )
        scenario.onActivity {
            ReminderTimesDialog.newInstance(habit.id!!).show(it.supportFragmentManager, "reminderTimes")
        }
        return scenario
    }

    private fun confirmTimePicker() {
        onView(withId(com.google.android.material.R.id.material_timepicker_ok_button)).perform(click())
    }
}
