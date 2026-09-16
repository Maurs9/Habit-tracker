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

import org.isoron.uhabits.core.models.Score.Companion.compute
import java.util.ArrayList
import java.util.HashMap
import javax.annotation.concurrent.ThreadSafe
import kotlin.math.max
import kotlin.math.min

@ThreadSafe
class ScoreList {

    private val map = HashMap<Timestamp, Score>()

    /**
     * Returns the score for a given day. If the timestamp given happens before the first
     * repetition of the habit or after the last computed score, returns a score with value zero.
     */
    @Synchronized
    operator fun get(timestamp: Timestamp): Score {
        return map[timestamp] ?: Score(timestamp, 0.0)
    }

    /**
     * Returns the list of scores that fall within the given interval.
     *
     * There is exactly one score per day in the interval. The endpoints of the interval are
     * included. The list is ordered by timestamp (decreasing). That is, the first score
     * corresponds to the newest timestamp, and the last score corresponds to the oldest timestamp.
     */
    @Synchronized
    fun getByInterval(
        fromTimestamp: Timestamp,
        toTimestamp: Timestamp
    ): List<Score> {
        val result: MutableList<Score> = ArrayList()
        if (fromTimestamp.isNewerThan(toTimestamp)) return result
        var current = toTimestamp
        while (!current.isOlderThan(fromTimestamp)) {
            result.add(get(current))
            current = current.minus(1)
        }
        return result
    }

    /**
     * Recomputes all scores between the provided [from] and [to] timestamps.
     */
    @Synchronized
    fun recompute(
        frequency: Frequency,
        isNumerical: Boolean,
        numericalHabitType: NumericalHabitType,
        targetValue: Double,
        computedEntries: EntryList,
        from: Timestamp,
        to: Timestamp
    ) {
        map.clear()
        var rollingSum = 0.0
        var numerator = frequency.numerator
        var denominator = frequency.denominator
        val freq = frequency.toDouble()
        val values = computedEntries.getByInterval(from, to).map { it.value }.toIntArray()
        val isAtMost = numericalHabitType == NumericalHabitType.AT_MOST

        // For non-daily boolean habits and multi-frequency numerical habits, we double the numerator
        // and denominator to smooth out irregular repetition schedules across different days of the week.
        if ((!isNumerical || frequency.numerator > 1) && freq < 1.0) {
            numerator *= 2
            denominator *= 2
        }

        val dayScores = DoubleArray(values.size)
        for (i in values.indices) {
            val v = values[i]
            dayScores[i] = when {
                v == Entry.SKIP || v == Entry.UNKNOWN || (isNumerical && v == Entry.NUMERICAL_AUTO) -> 0.0
                !isNumerical -> if (v == Entry.YES_MANUAL) 1.0 else 0.0
                !isAtMost -> if (targetValue > 0) min(1.0, max(0.0, v / 1000.0) / targetValue) else 1.0
                else -> {
                    val norm = v / 1000.0
                    if (targetValue > 0) {
                        (1.0 - ((norm - targetValue) / targetValue)).coerceIn(0.0, 1.0)
                    } else {
                        if (norm > 0) 0.0 else 1.0
                    }
                }
            }
        }

        var previousValue = if (isNumerical && isAtMost) 1.0 else 0.0
        for (i in values.indices) {
            val offset = values.size - i - 1
            rollingSum += dayScores[offset]
            if (offset + denominator < values.size) {
                rollingSum -= dayScores[offset + denominator]
            }

            if (values[offset] != Entry.SKIP) {
                val percentageCompleted = min(1.0, rollingSum / numerator)
                previousValue = compute(freq, previousValue, percentageCompleted)
            }
            val timestamp = from.plus(i)
            map[timestamp] = Score(timestamp, previousValue)
        }
    }
}
