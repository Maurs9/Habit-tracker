package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.ArchiveHabitsCommand
import org.isoron.uhabits.core.commands.ChangeHabitSectionCommand
import org.isoron.uhabits.core.commands.ChangeHabitTagsCommand
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.commands.DeleteSectionCommand
import org.isoron.uhabits.core.commands.MoveSectionCommand
import org.isoron.uhabits.core.commands.RenameSectionCommand
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache.ListItem
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitCardListCacheGroupingTest : BaseUnitTest() {
    private lateinit var cache: HabitCardListCache
    private val today get() = DateUtils.getTodayWithOffset()
    private val notifications = mutableListOf<String>()

    override fun setUp() {
        super.setUp()
        habitList.removeAll()
        cache = HabitCardListCache(habitList, sectionList, commandRunner, taskRunner, mock())
        cache.setCheckmarkCount(10)
        cache.groupBySection = true
        cache.onAttached()
        val mirror = mutableListOf<Long>()
        cache.setListener(object : HabitCardListCache.Listener {
            override fun onItemInserted(position: Int) {
                mirror.add(position, cache.getItemByPosition(position)!!.itemId)
                check("insert")
            }
            override fun onItemRemoved(position: Int) {
                mirror.removeAt(position)
                check("remove")
            }
            override fun onItemMoved(oldPosition: Int, newPosition: Int) {
                mirror.add(newPosition, mirror.removeAt(oldPosition))
                check("move")
            }
            override fun onItemChanged(position: Int) {
                assertEquals(mirror[position], cache.getItemByPosition(position)!!.itemId)
                check("change")
            }
            override fun onRefreshFinished() = check("refresh")
            private fun check(event: String) {
                notifications.add(event)
                assertEquals(mirror, items().map { it.itemId })
                assertEquals(mirror.size, cache.itemCount)
                assertEquals(mirror.size, mirror.toSet().size)
            }
        })
    }

    override fun tearDown() {
        cache.onDetached()
        super.tearDown()
    }

    @Test
    fun ordersHeadersAndRowsWithOtherLastAndUniqueNegativeIds() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        sectionList.add("Empty")
        val other = habit("Other")
        val e = habit("Evening habit", evening.id)
        val m = habit("Morning habit", morning.id)
        val orphan = habit("Orphan", 999)
        cache.refreshAllHabits()
        assertEquals(listOf(morning.id, evening.id, null), headers().map { it.sectionId })
        assertEquals(listOf(m, e, orphan, other), rows())
        assertEquals(listOf(-morning.id - 1, -evening.id - 1, -1L), headers().map { it.itemId })
        assertTrue(headers().all { it.itemId < 0 })
        assertEquals(7, cache.itemCount)
        assertEquals(4, cache.habitCount)
        assertNull(cache.getHabitByPosition(0))
        assertNull(cache.getHabitByPosition(-1))
        assertNull(cache.getItemByPosition(cache.itemCount))
    }

    @Test
    fun emptyAndAllUnsectionedListsStayFlat() {
        sectionList.add("Empty")
        cache.refreshAllHabits()
        assertEquals(0, cache.itemCount)
        val a = habit("A")
        val b = habit("B", 999)
        cache.refreshAllHabits()
        assertEquals(listOf(a, b), rows())
        assertTrue(headers().isEmpty())
        cache.groupBySection = false
        assertEquals(2, cache.itemCount)
    }

    @Test
    fun groupingStablyPartitionsEveryPrimaryAndSecondaryOrder() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        for ((index, id) in listOf(evening.id, morning.id, null, morning.id, evening.id, null).withIndex()) {
            habit(listOf("Zebra", "Alpha", "Pear")[index % 3], id, if (index % 2 == 0) Entry.YES_MANUAL else Entry.NO)
                .color = PaletteColor(index % 2)
        }
        for (primary in HabitList.Order.values()) {
            for (secondary in HabitList.Order.values()) {
                cache.secondaryOrder = secondary
                cache.primaryOrder = primary
                val sorted = habitList.toList()
                assertEquals(
                    listOf(morning.id, evening.id, null).flatMap { id -> sorted.filter { it.sectionId == id } },
                    rows()
                )
            }
        }
        cache.groupBySection = false
        assertEquals(habitList.toList(), rows())
        assertTrue(headers().isEmpty())
        cache.groupBySection = true
        assertEquals(3, headers().size)
    }

    @Test
    fun repetitionUpdatesHeaderAndHabitCountsWithNumericalAndSkipRules() {
        val morning = sectionList.add("Morning")
        val a = habit("Yes", morning.id, Entry.YES_MANUAL)
        habit("Skip", morning.id, Entry.SKIP)
        habit("No", morning.id, Entry.NO)
        val number = habit("Number", morning.id, 10000).apply {
            type = HabitType.NUMERICAL
            targetValue = 10.0
        }
        habit("Other", null, Entry.YES_MANUAL)
        cache.refreshAllHabits()
        assertEquals(listOf(3, 1), headers().map { it.done })
        assertEquals(4, cache.completedTodayCount())
        commandRunner.run(CreateRepetitionCommand(habitList, a, today, Entry.NO, ""))
        assertEquals(2, headers()[0].done)
        assertEquals(3, cache.completedTodayCount())
        commandRunner.run(CreateRepetitionCommand(habitList, number, today, 9000, ""))
        assertEquals(1, headers()[0].done)
        assertEquals(2, cache.completedTodayCount())
        assertEquals(5, cache.habitCount)
    }

    @Test
    fun movesAndDeletesDropEmptyHeadersWithConsistentNotifications() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val a = habit("A", morning.id)
        val b = habit("B", evening.id)
        val c = habit("C")
        cache.refreshAllHabits()
        commandRunner.run(ChangeHabitSectionCommand(habitList, listOf(a), evening.id))
        assertEquals(listOf(evening.id, null), headers().map { it.sectionId })
        assertEquals(listOf(a, b, c), rows())
        commandRunner.run(DeleteHabitsCommand(habitList, listOf(c)))
        assertEquals(listOf(evening.id), headers().map { it.sectionId })
        commandRunner.run(DeleteSectionCommand(sectionList, habitList, evening.id))
        assertTrue(headers().isEmpty())
        assertEquals(listOf(a, b), rows())
        assertTrue(rows().all { it.sectionId == null })
    }

    @Test
    fun renameAndMoveSectionUpdateHeaders() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        habit("A", morning.id)
        habit("B", evening.id)
        cache.refreshAllHabits()
        commandRunner.run(RenameSectionCommand(sectionList, morning.id, "Before work"))
        assertEquals("Before work", headers().first().name)
        commandRunner.run(MoveSectionCommand(sectionList, evening.id, 0))
        assertEquals(listOf(evening.id, morning.id), headers().map { it.sectionId })
    }

    @Test
    fun filteringCountsOnlyVisibleRowsAndRemovesEmptyGroups() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val done = habit("Done", morning.id, Entry.YES_MANUAL)
        val pending = habit("Pending", morning.id, Entry.NO)
        val other = habit("Evening", evening.id, Entry.NO)
        listOf(done, pending, other).forEach { it.tags = setOf("Health") }
        cache.setFilter(HabitMatcher(requiredTags = setOf("Health"), isCompletedAllowed = false))
        cache.refreshAllHabits()
        assertEquals(listOf(pending, other), rows())
        assertEquals(listOf(1, 1), headers().map { it.total })
        assertEquals(0, cache.completedTodayCount())
        commandRunner.run(CreateRepetitionCommand(habitList, pending, today, Entry.YES_MANUAL, ""))
        assertEquals(listOf(evening.id), headers().map { it.sectionId })
        commandRunner.run(ChangeHabitTagsCommand(habitList, listOf(other), emptySet()))
        assertTrue(items().isEmpty())
        cache.setFilter(HabitMatcher())
        cache.refreshAllHabits()
        commandRunner.run(ArchiveHabitsCommand(habitList, listOf(done, pending)))
        assertEquals(listOf(evening.id), headers().map { it.sectionId })
        assertEquals(1, cache.habitCount)
    }

    @Test
    fun directRemovalRegroupsAndUpdatesCountsWithoutChangingModels() {
        val section = sectionList.add("Morning")
        val a = habit("A", section.id, Entry.YES_MANUAL)
        val b = habit("B", section.id, Entry.NO)
        val other = habit("C")
        cache.refreshAllHabits()
        cache.remove(a.id!!)
        assertEquals(0, headers()[0].done)
        assertEquals(1, headers()[0].total)
        cache.remove(b.id!!)
        assertTrue(headers().isEmpty())
        assertEquals(listOf(other), rows())
        assertEquals(3, habitList.size())
        cache.refreshAllHabits()
        assertEquals(3, cache.habitCount)
    }

    @Test
    fun reorderRejectsHeadersAndCrossSectionRowsButAllowsWithinSection() {
        val section = sectionList.add("Morning")
        val a = habit("A", section.id)
        val b = habit("B", section.id)
        val other = habit("C")
        cache.refreshAllHabits()
        val initial = items()
        notifications.clear()
        for ((from, to) in listOf(0 to 1, 1 to 0, 1 to 4, 4 to 2, -1 to 2, 2 to 99)) {
            cache.reorder(from, to)
            assertFalse(cache.isSameSection(from, to))
        }
        assertEquals(initial, items())
        assertTrue(notifications.isEmpty())
        cache.reorder(1, 2)
        assertEquals(listOf(b, a, other), rows())
        assertEquals(listOf("move"), notifications)
        cache.groupBySection = false
        cache.reorder(0, 2)
        assertEquals(listOf(b, other, a), rows())
    }

    @Test
    fun supersededAndCancelledFetchesCannotPublishStaleSnapshots() {
        val section = sectionList.add("Morning")
        val a = habit("A", section.id, Entry.NO)
        val b = habit("B", section.id, Entry.NO)
        val queued = mutableListOf<Task>()
        val delayed: TaskRunner = mock {
            on { execute(any()) } doAnswer { queued.add(it.getArgument(0)); Unit }
        }
        val delayedCache = HabitCardListCache(habitList, sectionList, commandRunner, delayed, mock())
        delayedCache.setCheckmarkCount(1)
        delayedCache.groupBySection = true
        queued.removeAt(0).apply { doInBackground(); onPostExecute() }
        delayedCache.refreshHabit(a.id!!)
        val stale = queued.removeAt(0)
        stale.doInBackground()
        a.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        a.recompute()
        b.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        b.recompute()
        delayedCache.refreshHabit(b.id!!)
        stale.onPostExecute()
        assertEquals(0, delayedCache.completedTodayCount())
        queued.removeAt(0).apply { doInBackground(); onPostExecute() }
        assertEquals(2, delayedCache.completedTodayCount())
        delayedCache.groupBySection = false
        val cancelled = queued.removeAt(0)
        cancelled.doInBackground()
        delayedCache.cancelTasks()
        cancelled.onPostExecute()
        assertEquals(3, delayedCache.itemCount)
    }

    private fun habit(name: String, section: Long? = null, value: Int = Entry.NO): Habit =
        modelFactory.buildHabit().apply {
            this.name = name
            sectionId = section
            habitList.add(this)
            originalEntries.add(Entry(today, value))
            recompute()
        }

    private fun items() = (0 until cache.itemCount).map { cache.getItemByPosition(it)!! }
    private fun headers() = items().filterIsInstance<ListItem.Header>()
    private fun rows() = items().filterIsInstance<ListItem.Row>().map { it.habit }
}
