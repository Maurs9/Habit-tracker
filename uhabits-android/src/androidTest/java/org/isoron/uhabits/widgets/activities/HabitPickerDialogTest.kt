package org.isoron.uhabits.widgets.activities

import android.app.Activity
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.CoreMatchers.anything
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class HabitPickerDialogTest : BaseAndroidTest() {
    @Test
    fun testTwoChecksSaveAllIdsInListOrderAndReturnOk() {
        val first = habitList.getByPosition(0)
        val second = createHabit("Second")
        openPicker().use { scenario ->
            checkHabit(scenario, second)
            checkHabit(scenario, first)
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertEquals(2, activity.findViewById<ListView>(R.id.listView).checkedItemCount)
                assertTrue(storedIds().isEmpty())
            }
            onView(withId(R.id.buttonSave)).perform(click())
            assertEquals(listOf(first.id, second.id), storedIds())
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            assertEquals(WIDGET_ID, scenario.result.resultData.getIntExtra(EXTRA_APPWIDGET_ID, 0))
        }
    }

    @Test
    fun testSaveDisabledWithNoChecksAndAfterUnchecking() {
        val habit = habitList.getByPosition(0)
        openPicker().use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
            }
            checkHabit(scenario, habit)
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
            }
            checkHabit(scenario, habit)
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                activity.confirm(emptyList())
                assertFalse(activity.isFinishing)
                assertTrue(storedIds().isEmpty())
            }
        }
    }

    @Test
    fun testCheckedIdsSurviveRecreationAndReordering() {
        val first = habitList.getByPosition(0)
        val second = createHabit("Same name")
        val third = createHabit("Same name")
        openPicker().use { scenario ->
            checkHabit(scenario, second)
            checkHabit(scenario, third)
            scenario.onActivity {
                habitList.reorder(third, first)
                habitList.remove(first)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(listOf(third.id, second.id), checkedIds(activity))
                assertTrue(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                assertTrue(storedIds().isEmpty())
            }
            onView(withId(R.id.buttonSave)).perform(click())
            assertEquals(listOf(third.id, second.id), storedIds())
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        }
    }

    @Test
    fun testSectionShortcutsAreAdditiveAndRespectEveryPickerFilter() {
        val morning = appComponent.sectionList.add("Morning")
        val empty = appComponent.sectionList.add("Empty")
        val evening = appComponent.sectionList.add("Evening")
        val boolean = createHabit("Boolean", sectionId = morning.id)
        val numerical = createHabit("Numerical", HabitType.NUMERICAL, morning.id)
        val archived = createHabit("Archived", sectionId = morning.id).apply { isArchived = true }
        habitList.update(archived)
        val outsideBoolean = createHabit("Outside boolean", sectionId = evening.id)
        val outsideNumerical = createHabit("Outside numerical", HabitType.NUMERICAL, evening.id)
        val unsectioned = createHabit("Unsectioned")
        val cases = listOf(
            Triple(HabitPickerDialog::class.java, listOf(boolean, numerical), outsideBoolean),
            Triple(BooleanHabitPickerDialog::class.java, listOf(boolean), outsideBoolean),
            Triple(NumericalHabitPickerDialog::class.java, listOf(numerical), outsideNumerical)
        )
        for ((picker, expected, outside) in cases) {
            openPicker(picker).use { scenario ->
                checkHabit(scenario, outside)
                scenario.onActivity { activity ->
                    val list = activity.findViewById<ListView>(R.id.listView)
                    assertFalse((0 until list.count).any { list.adapter.getItemId(it) == archived.id })
                    val shortcuts = activity.findViewById<LinearLayout>(R.id.sectionShortcutButtons)
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.sectionShortcuts).visibility)
                    assertEquals(
                        listOf(morning.name, empty.name, evening.name),
                        (0 until shortcuts.childCount).map {
                            (shortcuts.getChildAt(it) as Button).text.toString()
                        }
                    )
                    assertFalse(shortcuts.getChildAt(1).isEnabled)
                    assertEquals(
                        activity.getString(R.string.widget_select_section, morning.name),
                        shortcuts.getChildAt(0).contentDescription
                    )
                    shortcuts.getChildAt(0).performClick()
                    shortcuts.getChildAt(0).performClick()
                    assertEquals((expected + outside).map { it.id }.toSet(), checkedIds(activity).toSet())
                    assertFalse(unsectioned.id in checkedIds(activity))
                    assertTrue(storedIds().isEmpty())
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals((expected + outside).map { it.id }.toSet(), checkedIds(activity).toSet())
                }
                onView(withId(R.id.buttonSave)).perform(click())
                assertEquals((expected + outside).map { it.id }, storedIds())
                assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
            }
            appComponent.widgetPreferences.removeWidget(WIDGET_ID)
        }
    }

    @Test
    fun testShortcutIgnoresHabitArchivedAfterOpening() {
        val section = appComponent.sectionList.add("Morning")
        val habit = createHabit("Newly archived", sectionId = section.id)
        openPicker().use { scenario ->
            scenario.onActivity { activity ->
                habit.isArchived = true
                habitList.update(habit)
                activity.findViewById<LinearLayout>(R.id.sectionShortcutButtons).getChildAt(0).performClick()
                assertTrue(checkedIds(activity).isEmpty())
                assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
            }
        }
    }

    @Test
    fun testNoSectionsHidesShortcuts() {
        openPicker().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.findViewById<View>(R.id.sectionShortcuts).visibility)
                assertEquals(ListView.CHOICE_MODE_MULTIPLE, activity.findViewById<ListView>(R.id.listView).choiceMode)
            }
        }
    }

    @Test
    fun testCancelDiscardsDraftAndKeepsExistingWidget() {
        val existing = habitList.getByPosition(0)
        val draft = createHabit("Draft")
        appComponent.widgetPreferences.addWidget(WIDGET_ID, longArrayOf(existing.id!!))
        openPicker().use { scenario ->
            checkHabit(scenario, draft)
            scenario.recreate()
            onView(withId(R.id.buttonCancel)).perform(click())
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertEquals(listOf(existing.id), storedIds())
        }
    }

    @Test
    fun testMissingSelectionRequiresExplicitReviewBeforeSavingRemainingIds() {
        val first = habitList.getByPosition(0)
        val second = createHabit("Second")
        openPicker().use { scenario ->
            checkHabit(scenario, first)
            checkHabit(scenario, second)
            scenario.onActivity { habitList.remove(first) }
            onView(withId(R.id.buttonSave)).perform(click())
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertTrue(storedIds().isEmpty())
                assertEquals(listOf(second.id), checkedIds(activity))
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.widgetPickerError).visibility)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.widgetPickerError).visibility)
            }
            onView(withId(R.id.buttonSave)).perform(click())
            assertEquals(listOf(second.id), storedIds())
            assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        }
    }

    @Test
    fun testAllSelectedHabitsUnavailableDisablesSaveWithoutSuccess() {
        val habit = habitList.getByPosition(0)
        openPicker().use { scenario ->
            checkHabit(scenario, habit)
            scenario.onActivity {
                habit.isArchived = true
                habitList.update(habit)
            }
            onView(withId(R.id.buttonSave)).perform(click())
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertTrue(storedIds().isEmpty())
                assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.widgetPickerError).visibility)
                assertEquals(activity.getString(R.string.no_habits), activity.findViewById<TextView>(R.id.message).text)
            }
            onView(withId(R.id.buttonCancel)).perform(click())
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }

    @Test
    fun testDeletedCheckedIdDoesNotSelectReplacementOnRecreation() {
        val original = habitList.getByPosition(0)
        openPicker().use { scenario ->
            checkHabit(scenario, original)
            scenario.onActivity {
                createHabit("Replacement")
                habitList.remove(original)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertTrue(checkedIds(activity).isEmpty())
                assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.widgetPickerError).visibility)
                assertTrue(storedIds().isEmpty())
            }
        }
    }

    @Test
    fun testEmptyMessagesKeepSubclassRestrictions() {
        habitList.removeAll()
        val cases = listOf(
            HabitPickerDialog::class.java to R.string.no_habits,
            BooleanHabitPickerDialog::class.java to R.string.no_boolean_habits,
            NumericalHabitPickerDialog::class.java to R.string.no_numerical_habits
        )
        for ((picker, message) in cases) {
            openPicker(picker).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.message).visibility)
                    assertEquals(activity.getString(message), activity.findViewById<TextView>(R.id.message).text)
                    assertFalse(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                }
            }
        }
    }

    @Test
    fun testNativeCheckedStateSurvivesRecreationInEveryTheme() {
        val habit = habitList.getByPosition(0)
        for ((theme, pureBlack) in listOf(
            ThemeSwitcher.THEME_LIGHT to false,
            ThemeSwitcher.THEME_DARK to false,
            ThemeSwitcher.THEME_DARK to true
        )) {
            prefs.theme = theme
            prefs.isPureBlackEnabled = pureBlack
            openPicker().use { scenario ->
                checkHabit(scenario, habit)
                scenario.recreate()
                scenario.onActivity { activity ->
                    val list = activity.findViewById<ListView>(R.id.listView)
                    assertEquals(listOf(habit.id), checkedIds(activity))
                    val row = list.getChildAt(0) as CheckedTextView
                    assertEquals(habit.name, row.text.toString())
                    assertTrue(row.isChecked)
                    assertTrue(activity.findViewById<Button>(R.id.buttonSave).isEnabled)
                }
            }
        }
    }

    @Test
    fun testMissingWidgetIdCancelsWithoutWritingPreferences() {
        ActivityScenario.launchActivityForResult<HabitPickerDialog>(
            Intent(targetContext, HabitPickerDialog::class.java)
        ).use { scenario ->
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertTrue(appComponent.widgetPreferences.getHabitIdsFromWidgetId(0).isEmpty())
        }
    }

    private fun openPicker(
        picker: Class<out HabitPickerDialog> = HabitPickerDialog::class.java
    ): ActivityScenario<HabitPickerDialog> = ActivityScenario.launchActivityForResult(
        Intent(targetContext, picker).putExtra(EXTRA_APPWIDGET_ID, WIDGET_ID)
    )

    private fun createHabit(name: String, type: HabitType = HabitType.YES_NO, sectionId: Long? = null): Habit =
        fixtures.createEmptyHabit().apply {
            this.name = name
            this.type = type
            this.sectionId = sectionId
            habitList.update(this)
        }

    private fun checkHabit(scenario: ActivityScenario<HabitPickerDialog>, habit: Habit) {
        var position = -1
        scenario.onActivity { activity ->
            val adapter = activity.findViewById<ListView>(R.id.listView).adapter
            position = (0 until adapter.count).first { adapter.getItemId(it) == habit.id }
        }
        onData(anything()).inAdapterView(withId(R.id.listView)).atPosition(position).perform(click())
    }

    private fun checkedIds(activity: HabitPickerDialog): List<Long> {
        val list = activity.findViewById<ListView>(R.id.listView)
        return (0 until list.count).filter { list.isItemChecked(it) }.map { list.adapter.getItemId(it) }
    }

    private fun storedIds() = appComponent.widgetPreferences.getHabitIdsFromWidgetId(WIDGET_ID).toList()

    companion object {
        private const val WIDGET_ID = 8008
    }
}
