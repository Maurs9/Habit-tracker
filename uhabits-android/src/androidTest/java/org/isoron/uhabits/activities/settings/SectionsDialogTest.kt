package org.isoron.uhabits.activities.settings

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
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
class SectionsDialogTest : BaseAndroidTest() {
    @Test
    fun testAddRenameMoveDeleteAndRecreation() {
        val morning = appComponent.sectionList.add("Morning")
        appComponent.sectionList.add("Evening")
        val habit = habitList.getByPosition(0).apply { sectionId = morning.id }
        habitList.update(habit)
        openManager().use { scenario ->
            onView(withText("Evening")).inRoot(isDialog()).perform(click())
            onView(withText(R.string.section_move_up)).inRoot(isDialog()).perform(click())
            assertEquals(listOf("Evening", "Morning"), appComponent.sectionList.getAll().map { it.name })
            scenario.recreate()
            onView(withText("Evening")).inRoot(isDialog()).perform(click())
            onView(withText(R.string.section_rename)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.section_name_hint)).perform(replaceText("After work"))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            onView(withText("After work")).inRoot(isDialog()).perform(click())
            onView(withText(R.string.section_move_down)).inRoot(isDialog()).perform(click())
            assertEquals(listOf("Morning", "After work"), appComponent.sectionList.getAll().map { it.name })
            onView(withText(R.string.section_add)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.section_name_hint)).perform(replaceText("Weekend"))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            assertEquals(3, appComponent.sectionList.size())
            onView(withText("Morning")).inRoot(isDialog()).perform(click())
            onView(withText(R.string.section_delete)).inRoot(isDialog()).perform(click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            assertNull(habit.sectionId)
            assertEquals(listOf("After work", "Weekend"), appComponent.sectionList.getAll().map { it.name })
            scenario.recreate()
            scenario.onActivity { activity ->
                val manager = activity.supportFragmentManager.findFragmentByTag("sections") as SectionsDialog
                val list = (manager.requireDialog() as AlertDialog).listView
                assertEquals(2, list.adapter.count)
                appComponent.sectionList.rename(appComponent.sectionList.getAll().first(), "Updated")
                assertEquals("Updated", list.adapter.getItem(0))
            }
        }
    }

    @Test
    fun testEmptyManagerStaysOpenAndDeleteCancelKeepsAssignment() {
        openManager().use { scenario ->
            scenario.onActivity { activity ->
                val manager = activity.supportFragmentManager.findFragmentByTag("sections") as SectionsDialog
                val list = (manager.requireDialog() as AlertDialog).listView
                assertEquals(targetContext.getString(R.string.no_sections_yet), list.adapter.getItem(0))
                assertFalse(list.adapter.isEnabled(0))
            }
            onView(withText(R.string.section_add)).inRoot(isDialog()).perform(click())
            onView(withHint(R.string.section_name_hint)).perform(replaceText("Morning"))
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            val section = appComponent.sectionList.getAll().single()
            val habit = habitList.getByPosition(0).apply { sectionId = section.id }
            habitList.update(habit)
            onView(withText("Morning")).inRoot(isDialog()).perform(click())
            onView(withText(R.string.section_delete)).inRoot(isDialog()).perform(click())
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
            assertEquals(section.id, habit.sectionId)
            assertEquals(1, appComponent.sectionList.size())
        }
    }

    private fun openManager(): ActivityScenario<SettingsActivity> =
        ActivityScenario.launch<SettingsActivity>(Intent(targetContext, SettingsActivity::class.java)).also { scenario ->
            scenario.onActivity { activity ->
                SectionsDialog().showNow(activity.supportFragmentManager, "sections")
            }
        }
}
