/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.io.Logging
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitList.Order
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.SectionList
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.utils.DateUtils.Companion.getTodayWithOffset
import javax.inject.Inject

/**
 * A HabitCardListCache fetches and keeps a cache of all the data necessary to
 * render a HabitCardListView.
 *
 *
 * This is needed since performing database lookups during scrolling can make
 * the ListView very slow. It also registers itself as an observer of the
 * models, in order to update itself automatically.
 *
 *
 * Note that this class is singleton-scoped, therefore it is shared among all
 * activities.
 */
@AppScope
class HabitCardListCache @Inject constructor(
    private val allHabits: HabitList,
    private val sectionList: SectionList,
    private val commandRunner: CommandRunner,
    taskRunner: TaskRunner,
    @Suppress("UNUSED_PARAMETER") logging: Logging
) : CommandRunner.Listener {

    sealed class ListItem {
        abstract val itemId: Long

        data class Header(
            val sectionId: Long?,
            val name: String,
            val done: Int,
            val total: Int,
            val collapsed: Boolean = false,
            val isFirstSection: Boolean = false
        ) : ListItem() {
            override val itemId: Long get() = -(sectionId ?: 0) - 1
        }

        data class Row(val habit: Habit) : ListItem() {
            override val itemId: Long get() = habit.id!!
        }
    }

    private var checkmarkCount = 0
    private var currentFetchTask: Task? = null
    private var listener: Listener
    private val data: CacheData
    private var filteredHabits: HabitList
    private var matcher = HabitMatcher()
    private val taskRunner: TaskRunner

    @Synchronized
    fun cancelTasks() {
        currentFetchTask?.cancel()
    }

    @Synchronized
    fun getCheckmarks(habitId: Long): IntArray {
        return data.checkmarks[habitId]!!
    }

    @Synchronized
    fun getNotes(habitId: Long): Array<String> {
        return data.notes[habitId]!!
    }

    @Synchronized
    fun hasNoHabit(): Boolean {
        return allHabits.isEmpty
    }

    @Synchronized
    fun emptyState(): HabitListEmptyState = data.emptyState

    @Synchronized
    fun completedTodayCount(): Int = data.items.filterIsInstance<ListItem.Row>().count {
        data.isCompleted(it.habit)
    }

    /**
     * Returns the habits that occupies a certain position on the list.
     *
     * @param position the position of the habit
     * @return the habit at given position or null if position is invalid
     */
    @Synchronized
    fun getHabitByPosition(position: Int): Habit? {
        return (getItemByPosition(position) as? ListItem.Row)?.habit
    }

    @Synchronized
    fun getItemByPosition(position: Int): ListItem? = data.items.getOrNull(position)

    @get:Synchronized
    val itemCount: Int
        get() = data.items.size

    @get:Synchronized
    val habitCount: Int
        get() = data.items.count { it is ListItem.Row }

    @get:Synchronized
    @set:Synchronized
    var groupBySection: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            refreshAllHabits()
        }

    @get:Synchronized
    @set:Synchronized
    var primaryOrder: Order
        get() = filteredHabits.primaryOrder
        set(order) {
            allHabits.primaryOrder = order
            filteredHabits.primaryOrder = order
            refreshAllHabits()
        }

    @get:Synchronized
    @set:Synchronized
    var secondaryOrder: Order
        get() = filteredHabits.secondaryOrder
        set(order) {
            allHabits.secondaryOrder = order
            filteredHabits.secondaryOrder = order
            refreshAllHabits()
        }

    @Synchronized
    fun getScore(habitId: Long): Double {
        return data.scores[habitId]!!
    }

    @Synchronized
    fun onAttached() {
        refreshAllHabits()
        commandRunner.addListener(this)
    }

    @Synchronized
    override fun onCommandFinished(command: Command) {
        if (command is CreateRepetitionCommand) {
            command.habit.id?.let { refreshHabit(it) }
        } else {
            refreshAllHabits()
        }
    }

    @Synchronized
    fun onDetached() {
        commandRunner.removeListener(this)
    }

    @Synchronized
    fun refreshAllHabits() {
        if (currentFetchTask != null) currentFetchTask!!.cancel()
        val task = RefreshTask()
        currentFetchTask = task
        taskRunner.execute(task)
    }

    @Synchronized
    fun refreshHabit(id: Long) {
        // A superseded partial refresh must not lose another habit's changed entries.
        val task = if (currentFetchTask == null) RefreshTask(id) else RefreshTask()
        currentFetchTask?.cancel()
        currentFetchTask = task
        taskRunner.execute(task)
    }

    @Synchronized
    fun remove(id: Long) {
        val remaining = data.items.filterIsInstance<ListItem.Row>().map { it.habit }.filter { it.id != id }
        if (remaining.size == habitCount) return
        val next = CacheData()
        next.referenceDate = data.referenceDate
        next.checkmarks.putAll(data.checkmarks)
        next.notes.putAll(data.notes)
        next.scores.putAll(data.scores)
        next.fetchItems(remaining)
        applyData(next)
    }

    @Synchronized
    fun reorder(from: Int, to: Int) {
        if (from == to || !isSameSection(from, to)) return
        val row = data.items.removeAt(from)
        data.items.add(to, row)
        listener.onItemMoved(from, to)
    }

    @Synchronized
    fun startReorder(position: Int): ReorderSession? {
        if (primaryOrder != Order.BY_POSITION) return null
        val habit = getHabitByPosition(position) ?: return null
        return ReorderSession(habit, data.items.toList())
    }

    inner class ReorderSession internal constructor(
        val habit: Habit,
        private val originalItems: List<ListItem>
    ) {
        private val originalOtherIds = originalItems.map { it.itemId }.filter { it != habit.id }
        private val originalGrouping = groupBySection
        private val originalFilter = filteredHabits
        var hasMoved = false
            private set
        private var finished = false

        fun move(from: Int, to: Int): Boolean = synchronized(this@HabitCardListCache) {
            if (finished) return false
            if (!isSnapshotValid()) {
                cancel()
                return false
            }
            if (from == to || getHabitByPosition(from) != habit || !isSameSection(from, to)) {
                return false
            }
            hasMoved = true
            reorder(from, to)
            true
        }

        fun finish(): Habit? = synchronized(this@HabitCardListCache) {
            if (finished) return null
            finished = true
            if (!hasMoved) return null
            if (!isSnapshotValid()) {
                cancel()
                return null
            }
            val position = data.items.indexOfFirst { it is ListItem.Row && it.habit == habit }
            // Persistence still uses the pre-drag order, not the last row crossed while dragging.
            val target = (originalItems.getOrNull(position) as? ListItem.Row)?.habit
            val targetPosition = data.items.indexOfFirst { it.itemId == target?.id }
            if (target == null || !isSameSection(position, targetPosition)) {
                cancel()
                return null
            }
            target.takeUnless { it == habit }
        }

        private fun isSnapshotValid(): Boolean =
            primaryOrder == Order.BY_POSITION &&
                groupBySection == originalGrouping &&
                filteredHabits === originalFilter &&
                data.items.size == originalItems.size &&
                data.items.map { it.itemId }.filter { it != habit.id } == originalOtherIds

        private fun cancel() {
            finished = true
            if (hasMoved) refreshAllHabits()
        }
    }

    @Synchronized
    fun isSameSection(from: Int, to: Int): Boolean {
        val first = getHabitByPosition(from) ?: return false
        val second = getHabitByPosition(to) ?: return false
        if (!groupBySection) return true
        fun section(habit: Habit) = habit.sectionId?.let { sectionList.getById(it)?.id }
        return section(first) == section(second)
    }

    @Synchronized
    fun setCheckmarkCount(checkmarkCount: Int) {
        this.checkmarkCount = checkmarkCount
    }

    @Synchronized
    fun setFilter(matcher: HabitMatcher) {
        this.matcher = matcher
        filteredHabits = allHabits.getFiltered(matcher)
    }

    @Synchronized
    fun setListener(listener: Listener) {
        this.listener = listener
    }

    /**
     * Interface definition for a callback to be invoked when the data on the
     * cache has been modified.
     */
    interface Listener {
        fun onItemChanged(position: Int) {}
        fun onItemInserted(position: Int) {}
        fun onItemMoved(oldPosition: Int, newPosition: Int) {}
        fun onItemRemoved(position: Int) {}
        fun onRefreshFinished() {}
    }

    private inner class CacheData {
        var referenceDate: Timestamp? = null
        var emptyState = HabitListEmptyState.NO_HABITS
        val items = mutableListOf<ListItem>()
        val checkmarks = hashMapOf<Long?, IntArray>()
        val scores = hashMapOf<Long?, Double>()
        val notes = hashMapOf<Long?, Array<String>>()

        fun isCompleted(habit: Habit) =
            checkmarks[habit.id]?.firstOrNull()?.let { habit.isCompleted(it) } == true

        fun fetchItems(habits: List<Habit>) {
            emptyState = if (habits.isNotEmpty()) {
                HabitListEmptyState.NONE
            } else {
                HabitListEmptyState.classify(allHabits.toList(), matcher)
            }
            val sections = sectionList.getAll()
            val knownIds = sections.map { it.id }.toSet()
            if (!groupBySection || habits.none { it.sectionId in knownIds }) {
                items.addAll(habits.map { ListItem.Row(it) })
                return
            }
            val groups = habits.groupBy { it.sectionId.takeIf { id -> id in knownIds } }
            fun append(id: Long?, name: String) {
                val rows = groups[id].orEmpty()
                if (rows.isEmpty()) return
                items.add(
                    ListItem.Header(
                        id,
                        name,
                        rows.count { isCompleted(it) },
                        rows.size,
                        isFirstSection = items.isEmpty()
                    )
                )
                items.addAll(rows.map { ListItem.Row(it) })
            }
            sections.forEach { append(it.id, it.name) }
            append(null, "")
        }
    }

    @Synchronized
    private fun applyData(next: CacheData) {
        val dateChanged = data.referenceDate != next.referenceDate
        data.referenceDate = next.referenceDate
        data.emptyState = next.emptyState
        val ids = next.items.map { it.itemId }.toSet()
        var position = 0
        while (position < data.items.size) {
            val item = data.items[position]
            if (item.itemId in ids) {
                position++
                continue
            }
            data.items.removeAt(position)
            if (item is ListItem.Row) {
                data.checkmarks.remove(item.itemId)
                data.notes.remove(item.itemId)
                data.scores.remove(item.itemId)
            }
            listener.onItemRemoved(position)
        }
        next.items.forEachIndexed { index, item ->
            val oldPosition = data.items.indexOfFirst { it.itemId == item.itemId }
            val oldItem = data.items.getOrNull(oldPosition)
            val changed = when (item) {
                is ListItem.Header -> item != oldItem
                is ListItem.Row -> {
                    val id = item.itemId
                    dateChanged || data.scores[id] != next.scores[id] ||
                        !data.checkmarks[id].contentEquals(next.checkmarks[id]) ||
                        !data.notes[id].contentEquals(next.notes[id])
                }
            }
            if (item is ListItem.Row) {
                data.checkmarks[item.itemId] = next.checkmarks[item.itemId]!!
                data.notes[item.itemId] = next.notes[item.itemId]!!
                data.scores[item.itemId] = next.scores[item.itemId]!!
            }
            if (oldPosition < 0) {
                data.items.add(index, item)
                listener.onItemInserted(index)
            } else {
                data.items.removeAt(oldPosition)
                data.items.add(index, item)
                if (oldPosition != index) listener.onItemMoved(oldPosition, index)
                if (changed) listener.onItemChanged(index)
            }
        }
    }

    private inner class RefreshTask(private val targetId: Long? = null) : Task {
        private val newData = CacheData()

        @Volatile private var isCancelled = false

        override fun cancel() {
            isCancelled = true
        }

        override fun isCanceled() = isCancelled

        override fun doInBackground() {
            val today = getTodayWithOffset()
            val habits = filteredHabits.filter { it.id != null }
            val canReuseEntries = synchronized(this@HabitCardListCache) {
                newData.referenceDate = today
                newData.scores.putAll(data.scores)
                newData.checkmarks.putAll(data.checkmarks)
                newData.notes.putAll(data.notes)
                data.referenceDate == today
            }
            val dateFrom = today.minus(checkmarkCount - 1)
            for (habit in habits) {
                if (isCancelled) return
                if (canReuseEntries && targetId != null && targetId != habit.id && newData.checkmarks.containsKey(habit.id)) continue
                newData.scores[habit.id] = habit.scores[today].value
                val entries = habit.computedEntries.getByInterval(dateFrom, today)
                newData.checkmarks[habit.id] = entries.map { it.value }.toIntArray()
                newData.notes[habit.id] = entries.map { it.notes }.toTypedArray()
            }
            newData.fetchItems(habits)
        }

        override fun onPostExecute() {
            synchronized(this@HabitCardListCache) {
                if (isCancelled || currentFetchTask !== this) return
                // Publish the complete snapshot on the UI thread; each notification sees
                // exactly the item count and positions that RecyclerView expects.
                applyData(newData)
                currentFetchTask = null
                listener.onRefreshFinished()
            }
        }
    }

    init {
        filteredHabits = allHabits
        this.taskRunner = taskRunner
        listener = object : Listener {}
        data = CacheData()
    }
}
