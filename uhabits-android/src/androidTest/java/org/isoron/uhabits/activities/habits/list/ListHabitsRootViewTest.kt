package org.isoron.uhabits.activities.habits.list

import android.content.Intent
import android.view.KeyEvent
import android.view.MenuInflater
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import dagger.Lazy
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.activities.habits.list.views.CheckmarkButtonViewFactory
import org.isoron.uhabits.activities.habits.list.views.CheckmarkPanelViewFactory
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.activities.habits.list.views.HabitCardListController
import org.isoron.uhabits.activities.habits.list.views.HabitCardListView
import org.isoron.uhabits.activities.habits.list.views.HabitCardListViewFactory
import org.isoron.uhabits.activities.habits.list.views.HabitCardView
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
import org.isoron.uhabits.core.ui.screens.habits.list.HabitListEmptyState
import org.isoron.uhabits.core.ui.screens.habits.list.HintListFactory
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListHabitsRootViewTest : BaseAndroidTest() {
    private lateinit var root: ListHabitsRootView
    private lateinit var container: FrameLayout
    private lateinit var adapter: HabitCardListAdapter
    private lateinit var commands: CommandRunner
    private lateinit var testHabits: HabitList
    private lateinit var controller: HabitCardListController
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
        val cardHolder = adapter.onCreateViewHolder(root.listView, 0)
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

    @Test
    fun testDragBacktrackingPersistsThePreviewInBothDirections() = withRoot {
        val (a, b, c, d) = prepareReorder()
        val callback = root.listView.TouchHelperCallback()
        callback.onSelectedChanged(holderAt(0), ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 0, 1)
        move(callback, 1, 2)
        move(callback, 2, 1)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        assertReorderPersisted(listOf(b, a, c, d))

        callback.onSelectedChanged(holderAt(3), ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 3, 2)
        move(callback, 2, 1)
        move(callback, 1, 2)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        assertReorderPersisted(listOf(b, a, d, c))
        assertTrue(adapter.isSelectionEmpty)
    }

    @Test
    fun testDragReturningToStartDoesNotReorderOrSelect() = withRoot {
        val original = prepareReorder()
        val callback = root.listView.TouchHelperCallback()
        val holder = holderAt(0)
        callback.onSelectedChanged(holder, ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 0, 2)
        move(callback, 2, 0)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        callback.clearView(root.listView, holder)
        assertTrue(adapter.isSelectionEmpty)
        assertReorderPersisted(original)
    }

    @Test
    fun testLongPressWithoutMovementStillSelectsExactlyOnce() = withRoot {
        val original = prepareReorder()
        val callback = root.listView.TouchHelperCallback()
        val holder = holderAt(0)
        callback.onSelectedChanged(holder, ItemTouchHelper.ACTION_STATE_DRAG)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        callback.clearView(root.listView, holder)
        callback.clearView(root.listView, holder)
        assertEquals(listOf(original[0]), adapter.selected.toList())
        assertReorderPersisted(original)
    }

    @Test
    fun testDelayedClearViewDoesNotFinishTheNextDrag() = withRoot {
        val (a, b, c, d) = prepareReorder()
        val callback = root.listView.TouchHelperCallback()
        val first = holderAt(0)
        callback.onSelectedChanged(first, ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 0, 2)
        move(callback, 2, 1)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        assertReorderPersisted(listOf(b, a, c, d))

        val second = holderAt(3)
        callback.onSelectedChanged(second, ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 3, 1)
        callback.clearView(root.listView, first)
        assertTrue(adapter.isSelectionEmpty)
        move(callback, 1, 2)
        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        callback.clearView(root.listView, second)
        callback.clearView(root.listView, first)
        assertTrue(adapter.isSelectionEmpty)
        assertReorderPersisted(listOf(b, a, d, c))
    }

    @Test
    fun testChangedRowsCancelDragWithoutSelecting() = withRoot {
        val (a, b, c, d) = prepareReorder()
        val callback = root.listView.TouchHelperCallback()
        val holder = holderAt(3)
        callback.onSelectedChanged(holder, ItemTouchHelper.ACTION_STATE_DRAG)
        move(callback, 3, 1)
        testHabits.remove(b)
        adapter.performRemove(listOf(b))

        callback.onSelectedChanged(null, ItemTouchHelper.ACTION_STATE_IDLE)
        callback.clearView(root.listView, holder)
        assertTrue(adapter.isSelectionEmpty)
        assertReorderPersisted(listOf(a, c, d))
    }

    @Test
    fun testAccessibilityMovesPersistAndUpdateBoundaryActions() = withRoot {
        val (a, b, c, d) = prepareReorder()
        val first = holderAt(0).itemView
        assertEquals(setOf(R.id.actionMoveHabitDown), moveActions(first))
        assertFalse(first.performAccessibilityAction(R.id.actionMoveHabitUp, null))
        assertTrue(first.performAccessibilityAction(R.id.actionMoveHabitDown, null))
        layoutList()
        assertReorderPersisted(listOf(b, a, c, d))
        assertEquals(setOf(R.id.actionMoveHabitUp, R.id.actionMoveHabitDown), moveActions(holderAt(1).itemView))
        assertTrue(holderAt(1).itemView.performAccessibilityAction(R.id.actionMoveHabitUp, null))
        layoutList()
        assertReorderPersisted(listOf(a, b, c, d))
        assertTrue(adapter.isSelectionEmpty)
    }

    @Test
    fun testKeyboardMovesRequireControlAndManualSort() = withRoot {
        val (a, b, c, d) = prepareReorder()
        val card = holderAt(0).itemView
        val down = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, KeyEvent.META_CTRL_ON)
        assertTrue(card.onKeyDown(down.keyCode, down))
        layoutList()
        assertReorderPersisted(listOf(b, a, c, d))
        holderAt(1).itemView.onKeyDown(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)
        )
        assertReorderPersisted(listOf(b, a, c, d))
        adapter.primaryOrder = HabitList.Order.BY_NAME_ASC
        layoutList()
        val sorted = testHabits.toList()
        assertTrue(moveActions(holderAt(0).itemView).isEmpty())
        assertFalse(holderAt(0).itemView.performAccessibilityAction(R.id.actionMoveHabitDown, null))
        holderAt(0).itemView.onKeyDown(down.keyCode, down)
        assertEquals(sorted, testHabits.toList())
    }

    @Test
    fun testAccessibleMovesProtectSectionsFiltersAndMultipleSelection() = withRoot {
        val morning = memoryFactory.buildSectionList().add("Morning")
        val evening = memoryFactory.buildSectionList().add("Evening")
        val a = addHabit(Entry.NO).apply {
            sectionId = morning.id
            tags = setOf("Visible")
        }
        val hidden = addHabit(Entry.NO).apply { sectionId = morning.id }
        val b = addHabit(Entry.NO).apply {
            sectionId = morning.id
            tags = setOf("Visible")
        }
        val c = addHabit(Entry.NO).apply {
            sectionId = evening.id
            tags = setOf("Visible")
        }
        adapter.setFilter(HabitMatcher(requiredTags = setOf("Visible")))
        adapter.primaryOrder = HabitList.Order.BY_POSITION
        adapter.groupBySection = true
        adapter.refresh()
        root.listView.itemAnimator = null
        layoutList()
        assertFalse(adapter.canMoveHabit(0, 1))
        assertFalse(adapter.canMoveHabit(1, -1))
        assertFalse(adapter.canMoveHabit(2, 1))
        assertFalse(controller.moveHabit(2, 2))
        assertTrue(holderAt(1).itemView.performAccessibilityAction(R.id.actionMoveHabitDown, null))
        layoutList()
        assertEquals(listOf(hidden, b, a, c), testHabits.toList())
        adapter.refresh()
        assertEquals(listOf(b, a, c), (0 until adapter.itemCount).mapNotNull { adapter.getItem(it) })
        adapter.toggleSelection(1)
        assertTrue(adapter.canMoveHabit(1, 1))
        assertFalse(adapter.canMoveHabit(2, -1))
        adapter.toggleSelection(2)
        assertFalse(adapter.canMoveHabit(1, 1))
        assertFalse(controller.moveHabit(1, 1))
    }

    @Test
    fun testSelectionMenuOffersRepeatedMovesWithoutEndingSelection() = withRoot {
        val (a, b, c, d) = prepareReorder()
        adapter.toggleSelection(0)
        val selection = ListHabitsSelectionMenu(
            root.context,
            adapter,
            commands,
            prefs,
            mock(),
            Lazy { controller },
            mock(),
            mock()
        )
        val menu = PopupMenu(root.context, root).menu
        val mode: ActionMode = mock()
        selection.onCreateActionMode(mode, menu)
        selection.onPrepareActionMode(mode, menu)
        assertTrue(menu.findItem(R.id.actionMoveHabitUp).isVisible)
        assertFalse(menu.findItem(R.id.actionMoveHabitUp).isEnabled)
        assertTrue(menu.findItem(R.id.actionMoveHabitDown).isEnabled)
        selection.onActionItemClicked(mode, menu.findItem(R.id.actionMoveHabitDown))
        layoutList()
        assertReorderPersisted(listOf(b, a, c, d))
        selection.onPrepareActionMode(mode, menu)
        assertTrue(menu.findItem(R.id.actionMoveHabitUp).isEnabled)
        selection.onActionItemClicked(mode, menu.findItem(R.id.actionMoveHabitUp))
        layoutList()
        assertReorderPersisted(listOf(a, b, c, d))
        assertEquals(listOf(a), adapter.selected.toList())
        verify(mode, never()).finish()
        adapter.primaryOrder = HabitList.Order.BY_NAME_ASC
        selection.onPrepareActionMode(mode, menu)
        assertFalse(menu.findItem(R.id.actionMoveHabitDown).isVisible)
    }

    @Test
    fun testAccessibleDateNavigationRebindsVisibleHabitDates() = withRoot {
        prepareReorder()
        assertTrue(root.header.buttonCount > 0)
        assertTrue(root.header.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        assertEquals(1, root.listView.dataOffset)
        val card = holderAt(0).itemView as HabitCardView
        assertEquals(day(1), card.checkmarkPanel.buttons[0].timestamp)
        assertTrue(root.header.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null))
        assertEquals(0, root.listView.dataOffset)
        assertEquals(day(0), card.checkmarkPanel.buttons[0].timestamp)
    }

    @Test
    fun testArchivedOnlyAndFilteredEmptyStatesOfferRelevantRecovery() = withRoot {
        addHabit(Entry.NO).isArchived = true
        adapter.refresh()
        var recovery: HabitListEmptyState? = null
        root.onEmptyAction = { recovery = it }
        assertEquals(root.context.getString(R.string.list_only_archived), root.llEmpty.textTextView.text.toString())
        root.llEmpty.recoveryButton.performClick()
        assertEquals(HabitListEmptyState.ARCHIVED, recovery)
        adapter.setFilter(HabitMatcher(requiredTags = setOf("Missing")))
        adapter.refresh()
        assertEquals(root.context.getString(R.string.list_filtered_empty), root.llEmpty.textTextView.text.toString())
        root.llEmpty.recoveryButton.performClick()
        assertEquals(HabitListEmptyState.FILTERED, recovery)
    }

    @Test
    fun testSortMenuExposesExactlyOneCheckedKeyAndItsDirection() = withRoot {
        val renderer = ListHabitsMenu(root.context, prefs, mock(), mock(), mock())
        val menu = PopupMenu(root.context, root).menu
        val sortIds = listOf(R.id.actionSortManual, R.id.actionSortName, R.id.actionSortColor, R.id.actionSortScore, R.id.actionSortStatus)
        val expected = mapOf(
            HabitList.Order.BY_POSITION to (R.id.actionSortManual to R.string.manually),
            HabitList.Order.BY_NAME_ASC to (R.id.actionSortName to R.string.sort_name_ascending),
            HabitList.Order.BY_NAME_DESC to (R.id.actionSortName to R.string.sort_name_descending),
            HabitList.Order.BY_COLOR_ASC to (R.id.actionSortColor to R.string.sort_color_ascending),
            HabitList.Order.BY_COLOR_DESC to (R.id.actionSortColor to R.string.sort_color_descending),
            HabitList.Order.BY_SCORE_ASC to (R.id.actionSortScore to R.string.sort_score_highest),
            HabitList.Order.BY_SCORE_DESC to (R.id.actionSortScore to R.string.sort_score_lowest),
            HabitList.Order.BY_STATUS_ASC to (R.id.actionSortStatus to R.string.sort_status_ascending),
            HabitList.Order.BY_STATUS_DESC to (R.id.actionSortStatus to R.string.sort_status_descending)
        )
        expected.forEach { (order, item) ->
            prefs.defaultPrimaryOrder = order
            renderer.onCreate(MenuInflater(root.context), menu)
            assertEquals(listOf(item.first), sortIds.filter { menu.findItem(it).isChecked })
            assertTrue(sortIds.all { menu.findItem(it).isCheckable })
            assertEquals(root.context.getString(item.second), menu.findItem(item.first).title.toString())
        }
    }

    private fun moveActions(view: View): Set<Int> {
        val info = AccessibilityNodeInfo.obtain()
        return try {
            view.onInitializeAccessibilityNodeInfo(info)
            info.actionList.map { it.id }.filter {
                it == R.id.actionMoveHabitUp || it == R.id.actionMoveHabitDown
            }.toSet()
        } finally {
            info.recycle()
        }
    }

    private fun prepareReorder(): List<Habit> {
        val habits = List(4) { addHabit(Entry.NO) }
        adapter.primaryOrder = HabitList.Order.BY_POSITION
        adapter.refresh()
        root.listView.itemAnimator = null
        layoutList()
        return habits
    }

    private fun layoutList() {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 800, 1600)
    }

    private fun holderAt(position: Int): RecyclerView.ViewHolder =
        root.listView.findViewHolderForAdapterPosition(position)!!

    private fun move(callback: HabitCardListView.TouchHelperCallback, from: Int, to: Int) {
        assertTrue(callback.onMove(root.listView, holderAt(from), holderAt(to)))
        layoutList()
    }

    private fun assertReorderPersisted(expected: List<Habit>) {
        assertEquals(expected, testHabits.toList())
        assertEquals(expected, (0 until adapter.itemCount).map { adapter.getItem(it) })
        adapter.refresh()
        assertEquals(expected, (0 until adapter.itemCount).map { adapter.getItem(it) })
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
                val behavior = ListHabitsBehavior(
                    testHabits,
                    memoryFactory.buildSectionList(),
                    mock(),
                    runner,
                    mock(),
                    commands,
                    prefs,
                    mock()
                )
                val selectionMenu: ListHabitsSelectionMenu = mock()
                controller = HabitCardListController(adapter, behavior, Lazy { selectionMenu })
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
                    HabitCardListViewFactory(activity, adapter, cardFactory, Lazy { controller })
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
