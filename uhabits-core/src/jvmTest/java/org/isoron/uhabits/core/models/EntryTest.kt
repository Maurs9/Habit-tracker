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

import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.core.models.Entry.Companion.nextToggleValue
import org.junit.Test
import kotlin.test.assertEquals

class EntryTest {
    @Test
    fun testFormattingDistinguishesNumericalValuesFromBooleanStates() {
        val timestamp = Timestamp(0)
        assertEquals("YES_AUTO", Entry(timestamp, YES_AUTO).formatValue(isNumerical = false))
        assertEquals("YES_MANUAL", Entry(timestamp, YES_MANUAL).formatValue(isNumerical = false))
        for (value in listOf(0, 1, 2, 1000)) {
            assertEquals(value.toString(), Entry(timestamp, value).formatValue(isNumerical = true))
        }
        assertEquals("YES_AUTO", Entry(timestamp, Entry.NUMERICAL_AUTO).formatValue(isNumerical = true))
        assertEquals("SKIP", Entry(timestamp, SKIP).formatValue(isNumerical = true))
        assertEquals("UNKNOWN", Entry(timestamp, UNKNOWN).formatValue(isNumerical = true))
    }

    @Test
    fun testNextValue() {
        check(
            mapOf(
                YES_AUTO to YES_MANUAL,
                YES_MANUAL to SKIP,
                SKIP to NO,
                NO to UNKNOWN,
                UNKNOWN to YES_MANUAL
            ),
            isSkipEnabled = true,
            areQuestionMarksEnabled = true
        )
        check(
            mapOf(
                YES_AUTO to YES_MANUAL,
                YES_MANUAL to NO,
                SKIP to NO,
                NO to UNKNOWN,
                UNKNOWN to YES_MANUAL
            ),
            isSkipEnabled = false,
            areQuestionMarksEnabled = true
        )
        check(
            mapOf(
                YES_AUTO to YES_MANUAL,
                YES_MANUAL to SKIP,
                SKIP to NO,
                NO to YES_MANUAL,
                UNKNOWN to YES_MANUAL
            ),
            isSkipEnabled = true,
            areQuestionMarksEnabled = false
        )
        check(
            mapOf(
                YES_AUTO to YES_MANUAL,
                YES_MANUAL to NO,
                SKIP to NO,
                NO to YES_MANUAL,
                UNKNOWN to YES_MANUAL
            ),
            isSkipEnabled = false,
            areQuestionMarksEnabled = false
        )
    }

    private fun check(
        map: Map<Int, Int>,
        isSkipEnabled: Boolean,
        areQuestionMarksEnabled: Boolean
    ) {
        for ((value, expected) in map) {
            assertEquals(
                nextToggleValue(
                    value = value,
                    isSkipEnabled = isSkipEnabled,
                    areQuestionMarksEnabled = areQuestionMarksEnabled
                ),
                expected
            )
        }
    }
}
