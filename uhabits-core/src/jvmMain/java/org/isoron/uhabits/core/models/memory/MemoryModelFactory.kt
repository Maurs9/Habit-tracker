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
package org.isoron.uhabits.core.models.memory

import org.isoron.uhabits.core.models.EntryList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.ScoreList
import org.isoron.uhabits.core.models.StreakList

class MemoryModelFactory : ModelFactory {
    private val sections = MemorySectionList()
    override fun buildComputedEntries() = EntryList()
    override fun buildOriginalEntries() = EntryList()
    override fun buildHabitList() = MemoryHabitList().also { sections.registerHabitList(it) }
    override fun buildSectionList() = sections
    override fun buildScoreList() = ScoreList()
    override fun buildStreakList() = StreakList()
    override fun buildHabitListRepository() = throw NotImplementedError()
    override fun buildSectionListRepository() = throw NotImplementedError()
    override fun buildRepetitionListRepository() = throw NotImplementedError()
}
