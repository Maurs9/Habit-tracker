/*
 * Copyright (C) 2026 Loop Habit Tracker contributors
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

internal data class RecomputeScenario(
    val years: Int,
    val schedule: String,
    val frequency: Frequency,
    val kind: String,
    val skips: Boolean
) {
    val days = years * 365
    val to = Timestamp(1_735_689_600_000L)
    val from = to.minus(days - 1)
    val isNumerical = kind != "boolean"
    val targetType = if (kind == "at-most") NumericalHabitType.AT_MOST else NumericalHabitType.AT_LEAST
    val targetValue = 5.0 * frequency.denominator

    fun entries(): List<Entry> = List(days) { offset ->
        val value = when {
            skips && offset % 17 == 0 -> Entry.SKIP
            offset % 13 == 0 -> Entry.UNKNOWN
            isNumerical -> (offset * 37 % 11) * 1000
            offset % 7 < 4 -> Entry.YES_MANUAL
            else -> Entry.NO
        }
        Entry(to.minus(offset), value, if (offset % 29 == 0) "Fixture note" else "")
    }

    companion object {
        fun all(): List<RecomputeScenario> = buildList {
            for (years in listOf(1, 5, 10)) {
                for ((schedule, frequency) in listOf(
                    "daily" to Frequency.DAILY,
                    "weekly" to Frequency(3, 7),
                    "monthly" to Frequency(5, 30)
                )) {
                    for (kind in listOf("boolean", "at-least", "at-most")) {
                        for (skips in listOf(false, true)) {
                            add(RecomputeScenario(years, schedule, frequency, kind, skips))
                        }
                    }
                }
            }
        }
    }
}

internal class RecomputeFixture(val scenario: RecomputeScenario, entries: List<Entry> = scenario.entries()) {
    val original = EntryList().apply { entries.forEach { add(it) } }
    val computed = EntryList()
    val scores = ScoreList()
    val streaks = StreakList()

    init {
        recomputeAll()
    }

    fun recomputeEntries() {
        computed.recomputeFrom(original, scenario.frequency, scenario.isNumerical)
    }

    fun recomputeScores() {
        scores.recompute(
            scenario.frequency,
            scenario.isNumerical,
            scenario.targetType,
            scenario.targetValue,
            computed,
            scenario.from,
            scenario.to
        )
    }

    fun recomputeStreaks() {
        streaks.recompute(
            computed,
            scenario.from,
            scenario.to,
            scenario.isNumerical,
            scenario.targetValue,
            scenario.targetType
        )
    }

    fun recomputeAll() {
        recomputeEntries()
        recomputeScores()
        recomputeStreaks()
    }
}
