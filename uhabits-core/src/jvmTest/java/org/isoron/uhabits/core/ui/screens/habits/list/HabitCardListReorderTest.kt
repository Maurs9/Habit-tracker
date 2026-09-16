package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache.ReorderSession
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitCardListReorderTest : BaseUnitTest() {
    private lateinit var cache: HabitCardListCache
    private lateinit var behavior: ListHabitsBehavior

    override fun setUp() {
        super.setUp()
        modelFactory = SQLModelFactory(buildMemoryDatabase())
        habitList = modelFactory.buildHabitList()
        sectionList = modelFactory.buildSectionList()
        cache = HabitCardListCache(habitList, sectionList, commandRunner, taskRunner, mock())
        cache.setCheckmarkCount(1)
        cache.setFilter(HabitMatcher(requiredTags = setOf("Visible")))
        behavior = behavior(taskRunner)
    }

    @Test
    fun downwardBacktrackingPersistsThePreview() {
        val (a, b, c, d) = createHabits()
        val drag = cache.startReorder(0)!!
        assertTrue(drag.move(0, 1))
        assertTrue(drag.move(1, 2))
        assertTrue(drag.move(2, 1))
        assertEquals(listOf(a, b, c, d), habitList.toList())
        finishAndReload(drag, b, listOf(b, a, c, d))
    }

    @Test
    fun upwardBacktrackingPersistsThePreview() {
        val (a, b, c, d) = createHabits()
        val drag = cache.startReorder(3)!!
        assertTrue(drag.move(3, 2))
        assertTrue(drag.move(2, 1))
        assertTrue(drag.move(1, 2))
        assertEquals(listOf(a, b, c, d), habitList.toList())
        finishAndReload(drag, c, listOf(a, b, d, c))
    }

    @Test
    fun backtrackingPastTheStartUsesTheFinalDirection() {
        val (a, b, c, d) = createHabits()
        val drag = cache.startReorder(1)!!
        assertTrue(drag.move(1, 3))
        assertTrue(drag.move(3, 0))
        finishAndReload(drag, a, listOf(b, a, c, d))

        val reverse = cache.startReorder(2)!!
        assertTrue(reverse.move(2, 0))
        assertTrue(reverse.move(0, 3))
        finishAndReload(reverse, d, listOf(b, a, d, c))
    }

    @Test
    fun movesToEitherEndPersistThePreview() {
        val (a, b, c, d) = createHabits()
        val down = cache.startReorder(0)!!
        assertTrue(down.move(0, 3))
        finishAndReload(down, d, listOf(b, c, d, a))

        val up = cache.startReorder(3)!!
        assertTrue(up.move(3, 0))
        finishAndReload(up, b, listOf(a, b, c, d))
    }

    @Test
    fun returningToTheOriginalPositionDoesNotPersistOrBecomeAnUnmovedLongPress() {
        val original = createHabits()
        for (moves in listOf(listOf(0 to 2, 2 to 0), listOf(3 to 1, 1 to 3))) {
            val drag = cache.startReorder(moves.first().first)!!
            moves.forEach { (from, to) -> assertTrue(drag.move(from, to)) }
            assertTrue(drag.hasMoved)
            finishAndReload(drag, null, original)
        }
    }

    @Test
    fun aLongPressWithoutMovementDoesNotProduceAReorder() {
        val original = createHabits()
        val drag = cache.startReorder(0)!!
        assertFalse(drag.move(0, 0))
        assertFalse(drag.hasMoved)
        finishAndReload(drag, null, original)
        assertNull(cache.startReorder(-1))
        assertNull(cache.startReorder(cache.itemCount))
        cache.primaryOrder = HabitList.Order.BY_NAME_ASC
        assertNull(cache.startReorder(0))
    }

    @Test
    fun filteredBacktrackingKeepsHiddenHabitsAndPersistsVisibleOrder() {
        val a = habit("A")
        val hidden1 = habit("Hidden 1", visible = false)
        val b = habit("B")
        val hidden2 = habit("Hidden 2", visible = false)
        val c = habit("C")
        val hidden3 = habit("Hidden 3", visible = false)
        val d = habit("D")
        cache.refreshAllHabits()

        val down = cache.startReorder(0)!!
        assertTrue(down.move(0, 1))
        assertTrue(down.move(1, 2))
        assertTrue(down.move(2, 1))
        finishAndReload(down, b, listOf(hidden1, b, a, hidden2, c, hidden3, d), listOf(b, a, c, d))

        val up = cache.startReorder(3)!!
        assertTrue(up.move(3, 2))
        assertTrue(up.move(2, 1))
        assertTrue(up.move(1, 2))
        finishAndReload(up, c, listOf(hidden1, b, a, hidden2, d, c, hidden3), listOf(b, a, d, c))
    }

    @Test
    fun groupedAndFilteredBacktrackingStaysWithinTheSection() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val a = habit("A", morning.id)
        val x = habit("X", evening.id)
        val b = habit("B", morning.id)
        val hidden = habit("Hidden", morning.id, visible = false)
        val c = habit("C", morning.id)
        val y = habit("Y", evening.id)
        val d = habit("D", morning.id)
        cache.groupBySection = true
        assertNull(cache.startReorder(0))
        val down = cache.startReorder(1)!!
        for (target in listOf(-1, 0, 5, 6, 99)) {
            assertFalse(down.move(1, target))
        }
        assertFalse(down.hasMoved)
        assertTrue(down.move(1, 2))
        assertTrue(down.move(2, 3))
        assertTrue(down.move(3, 2))
        finishAndReload(down, b, listOf(x, b, a, hidden, c, y, d), listOf(b, a, c, d, x, y))

        val up = cache.startReorder(4)!!
        assertTrue(up.move(4, 3))
        assertTrue(up.move(3, 2))
        assertTrue(up.move(2, 3))
        finishAndReload(up, c, listOf(x, b, a, hidden, d, c, y), listOf(b, a, d, c, x, y))
    }

    @Test
    fun successiveDragsCanFinishBeforeTheQueuedWritesRun() {
        val (a, b, c, d) = createHabits()
        val queued = mutableListOf<Task>()
        val delayed: TaskRunner = mock {
            on { execute(any()) } doAnswer { queued.add(it.getArgument(0)); Unit }
        }
        val delayedBehavior = behavior(delayed)
        val first = cache.startReorder(0)!!
        assertTrue(first.move(0, 2))
        assertTrue(first.move(2, 1))
        delayedBehavior.onReorderHabit(first.habit, first.finish()!!)

        val second = cache.startReorder(3)!!
        assertTrue(second.move(3, 1))
        assertNull(first.finish())
        assertFalse(first.move(1, 2))
        assertTrue(second.move(1, 2))
        delayedBehavior.onReorderHabit(second.habit, second.finish()!!)
        assertEquals(listOf(b, a, d, c), rows())
        assertEquals(listOf(a, b, c, d), habitList.toList())
        assertEquals(2, queued.size)
        queued.forEach { it.doInBackground(); it.onPostExecute() }
        assertReloadedOrder(listOf(b, a, d, c))
    }

    @Test
    fun changedRowsCancelTheDragAndRestorePersistedOrder() {
        val (a, b, c, d) = createHabits()
        val drag = cache.startReorder(3)!!
        assertTrue(drag.move(3, 1))
        habitList.remove(b)
        cache.remove(b.id!!)
        assertEquals(listOf(a, d, c), rows())

        assertNull(drag.finish())
        assertTrue(drag.hasMoved)
        assertFalse(drag.move(2, 1))
        assertEquals(listOf(a, c, d), rows())
        assertReloadedOrder(listOf(a, c, d))
    }

    @Test
    fun changingSortCancelsAnActivePreview() {
        val original = createHabits()
        val drag = cache.startReorder(3)!!
        assertTrue(drag.move(3, 1))
        cache.primaryOrder = HabitList.Order.BY_NAME_DESC

        assertFalse(drag.move(0, 1))
        assertNull(drag.finish())
        assertTrue(drag.hasMoved)
        assertEquals(original.reversed(), rows())
        cache.primaryOrder = HabitList.Order.BY_POSITION
        assertReloadedOrder(original)
    }

    private fun finishAndReload(
        drag: ReorderSession,
        target: Habit?,
        expectedAll: List<Habit>,
        expectedVisible: List<Habit> = expectedAll
    ) {
        assertEquals(expectedVisible, rows())
        assertEquals(target, drag.finish())
        if (target != null) behavior.onReorderHabit(drag.habit, target)
        assertNull(drag.finish())
        assertReloadedOrder(expectedAll)
        assertEquals(expectedVisible, rows())
    }

    private fun assertReloadedOrder(expected: List<Habit>) {
        val preview = (0 until cache.itemCount).map { cache.getItemByPosition(it)!!.itemId }
        assertEquals(expected, habitList.toList())
        (habitList as SQLiteHabitList).reload()
        cache.refreshAllHabits()
        assertEquals(expected, habitList.toList())
        assertEquals(expected.indices.toList(), habitList.map { it.position })
        assertEquals(preview, (0 until cache.itemCount).map { cache.getItemByPosition(it)!!.itemId })
    }

    private fun behavior(runner: TaskRunner) = ListHabitsBehavior(
        habitList,
        sectionList,
        mock(),
        runner,
        mock(),
        commandRunner,
        mock(),
        mock()
    )

    private fun createHabits() = "ABCD".map { habit(it.toString()) }.also { cache.refreshAllHabits() }

    private fun habit(name: String, sectionId: Long? = null, visible: Boolean = true) =
        modelFactory.buildHabit().apply {
            this.name = name
            this.sectionId = sectionId
            tags = if (visible) setOf("Visible") else emptySet()
            habitList.add(this)
        }

    private fun rows() = (0 until cache.itemCount).mapNotNull { cache.getHabitByPosition(it) }
}
