package org.isoron.uhabits.activities.habits.list

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class HabitSectionDialogsTest : BaseAndroidTest() {
    @Test
    fun testBulkAssignmentAndNonePreserveTagsAndHistory() {
        val morning = appComponent.sectionList.add("Morning")
        val evening = appComponent.sectionList.add("Evening")
        val habits = List(3) { fixtures.createEmptyHabit() }
        habits[0].sectionId = evening.id
        habits[1].sectionId = morning.id
        habits.forEach { it.tags = setOf("Health") }
        habitList.update(habits)
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            lateinit var dialogs: HabitOrganizationDialogs
            scenario.onActivity { activity ->
                dialogs = HabitOrganizationDialogs(
                    activity,
                    habitList,
                    prefs,
                    appComponent.commandRunner,
                    taskRunner,
                    appComponent.sectionList
                )
                dialogs.editSection(habits)
            }
            onView(withText("Morning")).inRoot(isDialog()).perform(click())
            assertTrue(habits.all { it.sectionId == morning.id })
            assertTrue(habits.all { it.tags == setOf("Health") })
            scenario.onActivity { dialogs.editSection(habits) }
            onView(withText(R.string.section_none)).inRoot(isDialog()).perform(click())
            assertTrue(habits.all { it.sectionId == null })
            assertTrue(habits.all { it.tags == setOf("Health") })
        }
    }
}
