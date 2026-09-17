package org.isoron.uhabits.automation

import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class EditSettingRootViewTest : BaseAndroidTest() {
    @Test
    fun emptyListExplainsRecoveryAndCannotSave() {
        habitList.removeAll()
        withRoot { root, saved ->
            assertFalse(root.findViewById<Button>(R.id.buttonSave).isEnabled)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.emptyMessage).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.habitField).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.actionField).visibility)
            root.findViewById<Button>(R.id.buttonSave).performClick()
            assertTrue(saved.isEmpty())
        }
    }

    @Test
    fun validSelectionSavesTheChosenAction() = withRoot { root, saved ->
        val habit = habitList.getByPosition(0)
        root.findViewById<Spinner>(R.id.actionSpinner).setSelection(2)
        root.findViewById<Button>(R.id.buttonSave).performClick()
        assertEquals(listOf(habit to ACTION_TOGGLE), saved)
    }

    @Test
    fun restoredSelectionRetainsItsActionAfterSpinnerCallback() {
        val habit = fixtures.createEmptyHabit()
        withRoot(SettingUtils.Arguments(ACTION_UNCHECK, habit)) { root, saved ->
            val spinner = root.findViewById<Spinner>(R.id.habitSpinner)
            spinner.onItemSelectedListener!!.onItemSelected(
                spinner,
                spinner.selectedView,
                spinner.selectedItemPosition,
                spinner.selectedItemId
            )
            root.findViewById<Button>(R.id.buttonSave).performClick()
            assertEquals(listOf(habit to ACTION_UNCHECK), saved)
        }
    }

    @Test
    fun numericalSelectionRetainsDecrementAction() {
        val habit = fixtures.createLongNumericalHabit()
        withRoot(SettingUtils.Arguments(ACTION_DECREMENT, habit)) { root, saved ->
            assertEquals(2, root.findViewById<Spinner>(R.id.actionSpinner).count)
            root.findViewById<Button>(R.id.buttonSave).performClick()
            assertEquals(listOf(habit to ACTION_DECREMENT), saved)
        }
    }

    @Test
    fun removingSelectedHabitDoesNotSaveAnotherHabit() {
        val selected = habitList.getByPosition(0)
        fixtures.createEmptyHabit().apply { name = "Read" }
        withRoot { root, saved ->
            habitList.remove(selected)
            root.findViewById<Button>(R.id.buttonSave).performClick()
            assertTrue(saved.isEmpty())
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.selectionError).visibility)
            assertEquals(
                targetContext.getString(R.string.automation_habit_unavailable),
                root.findViewById<TextView>(R.id.selectionError).text.toString()
            )
        }
    }

    @Test
    fun removingLastHabitShowsEmptyState() = withRoot { root, saved ->
        habitList.removeAll()
        root.findViewById<Button>(R.id.buttonSave).performClick()
        assertTrue(saved.isEmpty())
        assertFalse(root.findViewById<Button>(R.id.buttonSave).isEnabled)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.emptyMessage).visibility)
    }

    @Test
    fun reorderingUnderlyingListDoesNotChangeSelectedIdentity() {
        val selected = habitList.getByPosition(0)
        val other = fixtures.createEmptyHabit()
        withRoot { root, saved ->
            habitList.reorder(selected, other)
            root.findViewById<Button>(R.id.buttonSave).performClick()
            assertEquals(selected.id, saved.single().first.id)
        }
    }

    private fun withRoot(
        args: SettingUtils.Arguments? = null,
        block: (EditSettingRootView, MutableList<Pair<Habit, Int>>) -> Unit
    ) {
        ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
            val saved = mutableListOf<Pair<Habit, Int>>()
            lateinit var root: EditSettingRootView
            scenario.onActivity { activity ->
                root = EditSettingRootView(
                    activity,
                    habitList,
                    { habit, action -> saved.add(habit to action) },
                    args
                )
                activity.setContentView(root)
            }
            scenario.onActivity { block(root, saved) }
        }
    }
}
