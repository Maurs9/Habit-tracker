package org.isoron.uhabits.activities.habits.edit

import android.content.Intent
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class EditHabitSectionTest : BaseAndroidTest() {
    @Test
    fun testExistingAssignmentAndNoneSurviveRecreationAndSave() {
        val section = appComponent.sectionList.add("Morning")
        val habit = habitList.getByPosition(0).apply { sectionId = section.id }
        habitList.update(habit)
        val intent = Intent(targetContext, EditHabitActivity::class.java).putExtra("habitId", habit.id)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(section.id, activity.sectionId)
                assertEquals("Morning", activity.findViewById<TextView>(R.id.sectionPicker).text.toString())
            }
            onView(withId(R.id.sectionPicker)).perform(scrollTo(), click())
            onView(withText(R.string.section_none)).inRoot(isDialog()).perform(click())
            scenario.recreate()
            scenario.onActivity { activity ->
                assertNull(activity.sectionId)
                assertEquals("None", activity.findViewById<TextView>(R.id.sectionPicker).text.toString())
            }
            onView(withId(R.id.buttonSave)).perform(click())
            assertNull(habit.sectionId)
        }
    }

    @Test
    fun testNewSectionValidationAndAbandonedHabitKeepsEmptySection() {
        appComponent.sectionList.add("Morning")
        val count = habitList.size()
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            onView(withId(R.id.sectionPicker)).perform(scrollTo(), click())
            onView(withText(R.string.section_new)).inRoot(isDialog()).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.section_name_hint))
                .check(matches(hasErrorText(targetContext.getString(R.string.validation_cannot_be_blank))))
            onView(withHint(R.string.section_name_hint)).perform(replaceText(" morning "))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.section_name_hint))
                .check(matches(hasErrorText(targetContext.getString(R.string.section_name_exists))))
            onView(withHint(R.string.section_name_hint)).perform(replaceText(" Evening "))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            val evening = appComponent.sectionList.getByName("Evening")!!
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(evening.id, activity.sectionId)
                assertEquals("Evening", activity.findViewById<TextView>(R.id.sectionPicker).text.toString())
                assertEquals("", activity.findViewById<EditText>(R.id.nameInput).text.toString())
                activity.finish()
            }
        }
        assertEquals(count, habitList.size())
        assertEquals(2, appComponent.sectionList.size())
    }
}
