package org.isoron.uhabits.core.models

import java.util.Locale

object ReminderTimes {
    fun normalize(times: Collection<Int>): Set<Int> {
        require(times.all { it in 0 until 24 * 60 }) { "Reminder time must be a minute of the day" }
        return times.toSortedSet()
    }

    fun parse(value: String): Set<Int> =
        if (value.isBlank()) emptySet() else normalize(value.split(",").map { it.trim().toInt() })

    fun format(times: Collection<Int>): String = normalize(times).joinToString(",")

    fun formatClock(times: Collection<Int>): String = normalize(times).joinToString(";") {
        String.format(Locale.ROOT, "%02d:%02d", it / 60, it % 60)
    }
}
