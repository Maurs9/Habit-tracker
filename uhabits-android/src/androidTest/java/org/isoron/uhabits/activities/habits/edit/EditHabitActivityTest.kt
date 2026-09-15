package org.isoron.uhabits.activities.habits.edit

import android.content.Intent
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EditHabitActivityTest : BaseAndroidTest() {
    @Test
    fun testLoadsTagsAndPreservesUnnormalizedDraftOnRecreation() {
        val existing = fixtures.createEmptyHabit().apply {
            tags = setOf("Outdoors", "Health")
        }
        val draft = "  Focus  \nfocus\n\nEvening "
        for (habitId in listOf(null, existing.id)) {
            val intent = Intent(targetContext, EditHabitActivity::class.java)
            if (habitId != null) intent.putExtra("habitId", habitId)
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    val input = activity.findViewById<EditText>(R.id.tagsInput)
                    assertEquals(if (habitId == null) "" else HabitTags.format(existing.tags), input.text.toString())
                    input.setText(draft)
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val input = activity.findViewById<EditText>(R.id.tagsInput)
                    assertEquals(draft, input.text.toString())
                    input.setText("")
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals("", activity.findViewById<EditText>(R.id.tagsInput).text.toString())
                }
            }
        }
    }

    @Test
    fun testCreatesEditsAndClearsTags() {
        val existing = fixtures.createEmptyHabit().apply { tags = setOf("Original") }
        for ((index, habitId) in listOf(null, existing.id, existing.id).withIndex()) {
            val name = "Tagged habit $index"
            val draft = if (index == 2) " \n " else "  Focus  \nfocus\n\nEvening "
            val expected = if (index == 2) emptySet() else setOf("Focus", "Evening")
            saveHabit(habitId) { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText(name)
                activity.findViewById<EditText>(R.id.tagsInput).setText(draft)
            }
            assertEquals(expected, habitList.first { it.name == name }.tags)
        }
    }

    @Test
    fun testPreservesSecondaryReminderTimesAndClearsAllWhenDisabled() {
        val existing = fixtures.createEmptyHabit().apply {
            reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
            replaceReminderTimes(listOf(510, 750, 1230))
        }
        val days = WeekdayList(31)
        saveHabit(existing.id) { activity ->
            activity.reminderHour = 9
            activity.reminderMin = 15
            activity.reminderDays = days
        }
        val edited = habitList.getById(existing.id!!)!!
        assertEquals(setOf(555, 750, 1230), edited.reminderTimes)
        assertEquals(setOf(750, 1230), edited.extraReminderTimes)
        assertEquals(days, edited.reminder!!.days)

        saveHabit(existing.id) { activity ->
            activity.reminderHour = -1
            activity.reminderMin = -1
        }
        val disabled = habitList.getById(existing.id!!)!!
        assertNull(disabled.reminder)
        assertTrue(disabled.reminderTimes.isEmpty())
        assertTrue(disabled.extraReminderTimes.isEmpty())
    }

    @Test
    fun testRestoresUnsavedTargetTypeForNewAndExistingHabits() {
        val existing = fixtures.createLongNumericalHabit()
        for (habitId in listOf(null, existing.id)) {
            val intent = Intent(targetContext, EditHabitActivity::class.java)
                .putExtra("habitType", HabitType.NUMERICAL.value)
            if (habitId != null) intent.putExtra("habitId", habitId)
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    activity.targetType = NumericalHabitType.AT_MOST
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals(NumericalHabitType.AT_MOST, activity.targetType)
                    assertEquals(
                        activity.getString(R.string.target_type_at_most),
                        activity.findViewById<android.widget.TextView>(R.id.targetTypePicker).text
                    )
                }
            }
        }
    }

    @Test
    fun testRejectsWhitespaceNameAndMalformedTargetWithoutClosing() {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
            .putExtra("habitType", HabitType.NUMERICAL.value)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val name = activity.findViewById<EditText>(R.id.nameInput)
                val target = activity.findViewById<EditText>(R.id.targetInput)
                name.setText("   ")
                // Bypass keyboard filtering to cover pasted/restored malformed values.
                target.keyListener = null
                target.setText("1.2.3")
                activity.findViewById<android.view.View>(R.id.buttonSave).performClick()
                assertNotNull(name.error)
                assertNotNull(target.error)
                assertFalse(activity.isFinishing)
                assertEquals("1.2.3", target.text.toString())
            }
        }
    }

    private fun saveHabit(habitId: Long?, edit: (EditHabitActivity) -> Unit) {
        val intent = Intent(targetContext, EditHabitActivity::class.java)
        if (habitId != null) intent.putExtra("habitId", habitId)
        val finished = CountDownLatch(1)
        val listener = object : CommandRunner.Listener {
            override fun onCommandFinished(command: Command) {
                finished.countDown()
            }
        }
        appComponent.commandRunner.addListener(listener)
        try {
            ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    edit(activity)
                    activity.findViewById<android.view.View>(R.id.buttonSave).performClick()
                }
                assertTrue("Habit save did not finish", finished.await(10, TimeUnit.SECONDS))
            }
        } finally {
            appComponent.commandRunner.removeListener(listener)
        }
    }
}
