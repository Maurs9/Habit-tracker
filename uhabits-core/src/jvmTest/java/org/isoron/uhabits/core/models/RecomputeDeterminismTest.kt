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

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecomputeDeterminismTest {
    @Test
    fun testFullHistoryIsIndependentOfInsertionOrderAndRepeatedCalls() {
        for (scenario in RecomputeScenario.all()) {
            val reference = RecomputeFixture(scenario)
            val shuffled = RecomputeFixture(scenario, scenario.entries().shuffled(Random(42)))
            val expectedEntries = reference.computed.getKnown()
            val expectedScores = reference.scores.getByInterval(scenario.from, scenario.to)
            val expectedStreaks = reference.streaks.getBest(scenario.days)
            repeat(2) {
                shuffled.recomputeAll()
                assertEquals(expectedEntries, shuffled.computed.getKnown(), scenario.toString())
                assertEquals(
                    expectedScores,
                    shuffled.scores.getByInterval(scenario.from, scenario.to),
                    scenario.toString()
                )
                assertEquals(expectedStreaks, shuffled.streaks.getBest(scenario.days), scenario.toString())
            }
            if (scenario.isNumerical) {
                val originalEntries = reference.original.getKnown()
                for (entry in originalEntries) {
                    assertEquals(entry, reference.computed.get(entry.timestamp), scenario.toString())
                }
                val originalDates = originalEntries.map { it.timestamp }.toSet()
                assertTrue(
                    expectedEntries.filter { it.timestamp !in originalDates }
                        .all { it.value == Entry.NUMERICAL_AUTO },
                    scenario.toString()
                )
                if (scenario.frequency == Frequency.DAILY) {
                    assertEquals(originalEntries, expectedEntries, scenario.toString())
                }
            }
            assertEquals(scenario.days, expectedScores.size)
            assertEquals(scenario.from, expectedScores.last().timestamp)
        }
    }
}
