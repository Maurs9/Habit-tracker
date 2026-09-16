package org.isoron.uhabits.core.models.sqlite

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.core.utils.DateUtils
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SQLiteHabitReloadTest : BaseUnitTest() {
    @Test
    fun restoresCommittedHistoryAndCachedIdentityAfterRollback() {
        val database = buildMemoryDatabase()
        try {
            val factory = SQLModelFactory(database)
            val habits = factory.buildHabitList() as SQLiteHabitList
            val habit = factory.buildHabit().apply { name = "Original habit" }
            habits.add(habit)
            val today = DateUtils.getTodayWithOffset()
            habit.originalEntries.add(Entry(today, Entry.YES_MANUAL, "Original note"))
            habit.originalEntries.add(Entry(today.minus(1), Entry.YES_MANUAL))
            habit.originalEntries.add(Entry(today.minus(2), Entry.NO))
            habit.recompute()
            val id = habit.id!!
            val entries = habit.originalEntries.getKnown()
            val scores = habit.scores.getByInterval(today.minus(2), today)
            val streaks = habit.streaks.getBest(5)
            assertTrue(habit.scores[today].value > 0)

            val incomplete = habits.getFiltered(HabitMatcher(isCompletedAllowed = false))
            val cache = HabitCardListCache(habits, factory.buildSectionList(), commandRunner, taskRunner, StandardLogging())
            cache.setCheckmarkCount(3)
            cache.refreshAllHabits()
            assertSame(habit, cache.getHabitByPosition(0))
            val listener: ModelObservable.Listener = mock()
            habit.observable.addListener(listener)

            database.beginTransaction()
            try {
                habit.name = "Uncommitted edit"
                habit.reminder = Reminder(8, 0, WeekdayList.EVERY_DAY)
                habits.update(habit)
                habit.originalEntries.add(Entry(today, Entry.NO, "Uncommitted note"))
                habit.originalEntries.add(Entry(today.minus(5), Entry.YES_MANUAL))
                habit.recompute()
                habits.add(factory.buildHabit().apply { name = "Uncommitted habit" })
                cache.refreshAllHabits()
                assertEquals(2, cache.habitCount)
            } finally {
                database.endTransaction()
            }

            habits.reload()
            cache.refreshAllHabits()

            val restored = habits.getById(id)!!
            verify(listener).onModelChange()
            assertSame(habit, restored)
            assertEquals("Original habit", restored.name)
            assertNull(restored.reminder)
            assertEquals(entries, restored.originalEntries.getKnown())
            assertEquals(Entry.YES_MANUAL, restored.computedEntries.get(today).value)
            assertEquals(scores, restored.scores.getByInterval(today.minus(2), today))
            assertEquals(streaks, restored.streaks.getBest(5))
            assertTrue(restored.isCompletedToday())
            assertEquals(0, incomplete.size())
            assertEquals(1, cache.habitCount)
            assertSame(restored, cache.getHabitByPosition(0))
            assertEquals(restored.scores[today].value, cache.getScore(id))
            assertEquals(Entry.YES_MANUAL, cache.getCheckmarks(id)[0])
            assertEquals("Original note", cache.getNotes(id)[0])
        } finally {
            database.close()
        }
    }
}
