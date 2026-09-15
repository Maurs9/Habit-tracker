package org.isoron.uhabits.core.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

/**
 * Parses a complete, finite, locale-formatted decimal, rejecting malformed grouping.
 * NumberFormat alone accepts a valid prefix and silently ignores trailing input.
 */
fun parseFiniteNumber(text: String, locale: Locale = Locale.getDefault()): Double? {
    val input = text.trim()
    if (input.isEmpty()) return null
    val format = NumberFormat.getNumberInstance(locale) as DecimalFormat
    val symbols = format.decimalFormatSymbols
    val unsigned = if (input.startsWith(format.negativePrefix)) {
        input.removePrefix(format.negativePrefix).removeSuffix(format.negativeSuffix)
    } else {
        input
    }
    val parts = unsigned.split(symbols.decimalSeparator)
    if (parts.size > 2) return null
    val integer = parts[0]
    val fraction = parts.getOrElse(1) { "" }
    if (fraction.any { !it.isDigit() }) return null
    val groups = integer.split(symbols.groupingSeparator)
    if (groups.any { group -> group.any { !it.isDigit() } }) return null
    if (groups.size > 1) {
        val size = format.groupingSize
        if (size <= 0 || groups[0].length !in 1..size) return null
        if (groups.drop(1).any { it.length != size }) return null
    }
    if (integer.isEmpty() && fraction.isEmpty()) return null
    val position = ParsePosition(0)
    val value = format.parse(input, position)?.toDouble() ?: return null
    return value.takeIf { position.index == input.length && position.errorIndex < 0 && it.isFinite() }
}

fun formatEditableNumber(value: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = 340
    }.format(value)
