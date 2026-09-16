package org.isoron.uhabits.activities.habits.list

import android.content.Intent
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import dagger.Lazy
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.activities.habits.list.views.CheckmarkButtonViewFactory
import org.isoron.uhabits.activities.habits.list.views.CheckmarkPanelViewFactory
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.activities.habits.list.views.HabitCardListController
import org.isoron.uhabits.activities.habits.list.views.HabitCardListViewFactory
import org.isoron.uhabits.activities.habits.list.views.HabitCardView
import org.isoron.uhabits.activities.habits.list.views.HabitCardViewHolder
import org.isoron.uhabits.activities.habits.list.views.HabitCardViewFactory
import org.isoron.uhabits.activities.habits.list.views.NumberButtonViewFactory
import org.isoron.uhabits.activities.habits.list.views.NumberPanelViewFactory
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.tasks.SingleThreadTaskRunner
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.core.ui.screens.habits.list.HintListFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListHabitsRootViewTest : BaseAndroidTest() {
    private lateinit var root: ListHabitsRootView
    private lateinit var container: FrameLayout
    private lateinit var adapter: HabitCardListAdapter
    private lateinit var commands: CommandRunner
    private lateinit var testHabits: HabitList
    private val memoryFactory = MemoryModelFactory()

    @Test
    fun testSubtitleEmptyAndModelChanges() = withRoot {
        assertNull(root.tbar.subtitle)
        val habit = addHabit(Entry.NO)
        adapter.refresh()
        assertEquals("0 of 1 done", root.tbar.subtitle.toString())
        commands.run(CreateRepetitionCommand(testHabits, habit, day(0), Entry.YES_MANUAL, ""))
        assertEquals("1 of 1 done", root.tbar.subtitle.toString())
        commands.run(CreateRepetitionCommand(testHabits, habit, day(0), Entry.NO, ""))
        assertEquals("0 of 1 done", root.tbar.subtitle.toString())
        testHabits.removeAll()
        adapter.refresh()
        assertNull(root.tbar.subtitle)
    }

    @Test
    fun testSubtitlePreservesNumericalAndSkipSemantics() = withRoot {
        addHabit(Entry.YES_MANUAL)
        addHabit(Entry.SKIP)
        addHabit(Entry.NO)
        addHabit(Entry.UNKNOWN)
        addHabit(10000, numerical = true)
        addHabit(11000, numerical = true)
        addHabit(9999, numerical = true)
        addHabit(Entry.SKIP, numerical = true)
        addHabit(0, numerical = true, atMost = true)
        adapter.refresh()
        assertEquals("4 of 9 done", root.tbar.subtitle.toString())
    }

    @Test
    fun testGroupingHeadersNeverCountAsHabitsOrBecomeSelected() = withRoot {
        val morning = memoryFactory.buildSectionList().add("Morning")
        addHabit(Entry.YES_MANUAL).sectionId = morning.id
        addHabit(Entry.NO)
        adapter.groupBySection = true
        adapter.refresh()
        assertEquals(4, adapter.itemCount)
        assertEquals(2, adapter.habitCount)
        assertEquals("1 of 2 done", root.tbar.subtitle.toString())
        assertTrue(adapter.getItemId(0) < 0)
        assertEquals(-1L, adapter.getItemId(2))
        assertEquals(1, adapter.getItemViewType(0))
        assertNull(adapter.getItem(0))
        adapter.toggleSelection(0)
        assertTrue(adapter.isSelectionEmpty)
        adapter.toggleSelection(1)
        assertEquals(1, adapter.selected.size)
        adapter.toggleSelection(2)
        assertEquals(1, adapter.selected.size)
        assertFalse(adapter.isSameSection(1, 3))
        assertFalse(adapter.isSameSection(0, 1))
        val header = adapter.onCreateViewHolder(root.listView, 1)
        adapter.onBindViewHolder(header, 0)
        assertEquals(0, root.listView.TouchHelperCallback().getMovementFlags(root.listView, header))
        assertFalse(header.itemView.performClick())
        assertFalse(header.itemView.performLongClick())
        val cardHolder = adapter.onCreateViewHolder(root.listView, 0) as HabitCardViewHolder
        adapter.onBindViewHolder(cardHolder, 1)
        val selectionMenu: ListHabitsSelectionMenu = mock()
        val controller = HabitCardListController(adapter, mock(), Lazy { selectionMenu })
        controller.onItemClick(0)
        controller.onItemLongClick(0)
        controller.startDrag(0)
        controller.drop(0, 1)
        controller.drop(1, 3)
        assertEquals(1, adapter.selected.size)
        verifyNoInteractions(selectionMenu)
        adapter.clearSelection()
        adapter.groupBySection = false
        assertEquals(2, adapter.itemCount)
        assertEquals("1 of 2 done", root.tbar.subtitle.toString())
    }

    @Test
    fun testSubtitleFollowsFiltersAndSortsTagsIgnoringCase() = withRoot {
        addHabit(Entry.YES_MANUAL).tags = setOf("zebra", "Apple", "banana")
        addHabit(Entry.NO).tags = setOf("zebra", "Apple", "banana")
        addHabit(Entry.YES_MANUAL)
        prefs.selectedTags = setOf("zebra", "banana", "Apple")
        adapter.setFilter(HabitMatcher(requiredTags = prefs.selectedTags))
        adapter.refresh()
        assertEquals("1 of 2 done · Apple, banana, zebra", root.tbar.subtitle.toString())

        prefs.showCompleted = false
        adapter.setFilter(HabitMatcher(requiredTags = prefs.selectedTags, isCompletedAllowed = false))
        adapter.refresh()
        assertEquals("1 to go · Apple, banana, zebra", root.tbar.subtitle.toString())

        prefs.selectedTags = setOf("Missing")
        adapter.setFilter(HabitMatcher(requiredTags = prefs.selectedTags, isCompletedAllowed = false))
        adapter.refresh()
        assertEquals("0 to go · Missing", root.tbar.subtitle.toString())
        prefs.showCompleted = true
        adapter.refresh()
        assertEquals("0 of 0 done · Missing", root.tbar.subtitle.toString())

        prefs.selectedTags = emptySet()
        adapter.setFilter(HabitMatcher())
        adapter.refresh()
        assertEquals("2 of 3 done", root.tbar.subtitle.toString())
        prefs.showCompleted = false
        adapter.setFilter(HabitMatcher(isCompletedAllowed = false))
        adapter.refresh()
        assertEquals("1 to go", root.tbar.subtitle.toString())
    }

    @Test
    fun testSubtitleUpdatesOnReattachWithoutModelNotification() = withRoot {
        addHabit(Entry.YES_MANUAL)
        adapter.refresh()
        assertEquals("1 of 1 done", root.tbar.subtitle.toString())
        container.removeView(root)
        prefs.selectedTags = setOf("Morning")
        container.addView(root)
        assertEquals("1 of 1 done · Morning", root.tbar.subtitle.toString())
    }

    private fun addHabit(value: Int, numerical: Boolean = false, atMost: Boolean = false): Habit =
        memoryFactory.buildHabit().apply {
            if (numerical) {
                type = HabitType.NUMERICAL
                targetValue = 10.0
                targetType = if (atMost) NumericalHabitType.AT_MOST else NumericalHabitType.AT_LEAST
            }
            testHabits.add(this)
            originalEntries.add(Entry(day(0), value))
            recompute()
        }

    private fun withRoot(block: () -> Unit) {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val runner = SingleThreadTaskRunner()
                commands = CommandRunner(runner)
                testHabits = memoryFactory.buildHabitList()
                val cache = HabitCardListCache(testHabits, memoryFactory.buildSectionList(), commands, runner, mock())
                adapter = HabitCardListAdapter(cache, prefs, mock())
                val cardFactory = HabitCardViewFactory(
                    activity,
                    CheckmarkPanelViewFactory(activity, prefs, CheckmarkButtonViewFactory(activity, prefs)),
                    NumberPanelViewFactory(activity, prefs, NumberButtonViewFactory(activity, prefs)),
                    mock()
                )
                root = ListHabitsRootView(
                    activity,
                    HintListFactory(prefs),
                    prefs,
                    mock(),
                    runner,
                    adapter,
                    HabitCardListViewFactory(activity, adapter, cardFactory, Lazy { mock() })
                )
                container = FrameLayout(activity)
                activity.setContentView(container)
                container.addView(root)
                try {
                    block()
                } finally {
                    container.removeView(root)
                }
            }
        }
    }
}
