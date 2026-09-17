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
package org.isoron.uhabits.core.models

import javax.annotation.concurrent.ThreadSafe
import kotlin.math.min

@ThreadSafe
class StreakList {
    private val list = ArrayList<Streak>()

    @Synchronized
    fun getBest(limit: Int): List<Streak> {
        list.sortWith { s1: Streak, s2: Streak -> s2.compareLonger(s1) }
        return list.subList(0, min(list.size, limit)).apply {
            sortWith { s1: Streak, s2: Streak -> s2.compareNewer(s1) }
        }.toList()
    }

    @Synchronized
    fun recompute(
        computedEntries: EntryList,
        from: Timestamp,
        to: Timestamp,
        isNumerical: Boolean,
        targetValue: Double,
        targetType: NumericalHabitType
    ) {
        list.clear()
        val entries = computedEntries
            .getByInterval(from, to)
            .filter {
                val value = it.value
                if (value == Entry.SKIP) {
                    true
                } else if (isNumerical) {
                    value == Entry.NUMERICAL_AUTO ||
                        (
                            value >= 0 && value != Entry.SKIP && when (targetType) {
                                NumericalHabitType.AT_LEAST -> value / 1000.0 >= targetValue
                                NumericalHabitType.AT_MOST -> value / 1000.0 <= targetValue
                            }
                            )
                } else {
                    value > 0
                }
            }
            .toTypedArray()

        if (entries.isEmpty()) return

        var begin = entries[0].timestamp
        var end = begin
        var hasCompletion = !isNumerical || entries[0].value != Entry.SKIP
        for (i in 1 until entries.size) {
            val current = entries[i].timestamp
            if (current == begin.minus(1)) {
                begin = current
                hasCompletion = hasCompletion || entries[i].value != Entry.SKIP
            } else {
                if (hasCompletion) list.add(Streak(begin, end))
                begin = current
                end = current
                hasCompletion = !isNumerical || entries[i].value != Entry.SKIP
            }
        }
        if (hasCompletion) list.add(Streak(begin, end))
    }
}
