package org.isoron.uhabits.core.utils

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberParserTest {
    @Test
    fun parsesCompleteLocalizedNumbers() {
        assertEquals(0.0, parseFiniteNumber(" 0 ", Locale.US))
        assertEquals(0.001, parseFiniteNumber("0.001", Locale.US))
        assertEquals(1234.5, parseFiniteNumber("1,234.5", Locale.US))
        assertEquals(1234.5, parseFiniteNumber("1.234,5", Locale.GERMANY))
        assertEquals(12.5, parseFiniteNumber("12,5", Locale.FRANCE))
        assertEquals(12.5, parseFiniteNumber("١٢٫٥", Locale("ar", "EG")))
        assertEquals(-1.5, parseFiniteNumber("-1.5", Locale.US))
    }

    @Test
    fun rejectsInvalidOrPartialInput() {
        listOf("", " ", ".", "NaN", "Infinity", "∞", "1.2.3", "1,2", "1,,000", "1,234,56", "12abc", "1e3", "9".repeat(400))
            .forEach { assertNull(parseFiniteNumber(it, Locale.US), it) }
        assertNull(parseFiniteNumber("1,234.5", Locale.GERMANY))
        assertNull(parseFiniteNumber("1.23", Locale.GERMANY))
    }

    @Test
    fun editableNumbersRoundTripWithoutLosingZeroOrSmallValues() {
        listOf(Locale.US, Locale.GERMANY, Locale.FRANCE, Locale("ar", "EG")).forEach { locale ->
            listOf(0.0, 0.001, 2.5, 1234.567, Int.MAX_VALUE / 1000.0).forEach { number ->
                assertEquals(number, parseFiniteNumber(formatEditableNumber(number, locale), locale))
            }
        }
    }
}
