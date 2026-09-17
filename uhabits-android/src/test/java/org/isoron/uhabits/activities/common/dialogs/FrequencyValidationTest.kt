package org.isoron.uhabits.activities.common.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrequencyValidationTest {
    @Test
    fun acceptsPositiveWholeNumbersAndLocalizedDigits() {
        assertEquals(1, parsePositiveFrequencyInteger("1"))
        assertEquals(30, parsePositiveFrequencyInteger(" 30 "))
        assertEquals(12, parsePositiveFrequencyInteger("١٢"))
        assertEquals(10000, parsePositiveFrequencyInteger("10000"))
        assertEquals(Int.MAX_VALUE, parsePositiveFrequencyInteger(Int.MAX_VALUE.toString()))
    }

    @Test
    fun rejectsEmptyNonpositiveFractionalAndMalformedInput() {
        for (text in listOf("", " ", "0", "-1", "1.5", "3days")) {
            assertNull(text, parsePositiveFrequencyInteger(text))
        }
    }

    @Test
    fun rejectsOverflowWithoutThrowing() {
        assertNull(parsePositiveFrequencyInteger("2147483648"))
        assertNull(parsePositiveFrequencyInteger("999999999999999"))
    }
}
