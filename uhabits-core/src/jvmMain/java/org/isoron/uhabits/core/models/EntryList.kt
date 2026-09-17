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

import org.isoron.uhabits.core.models.Entry.Companion.NUMERICAL_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.core.utils.DateUtils
import java.util.ArrayList
import java.util.Calendar
import javax.annotation.concurrent.ThreadSafe
import kotlin.collections.set
import kotlin.math.max

@ThreadSafe
open class EntryList {

    private val entriesByTimestamp: HashMap<Timestamp, Entry> = HashMap()

    /**
     * Returns the entry corresponding to the given timestamp. If no entry with such timestamp
     * has been previously added, returns Entry(timestamp, UNKNOWN).
     */
    @Synchronized
    open fun get(timestamp: Timestamp): Entry {
        return entriesByTimestamp[timestamp] ?: Entry(timestamp, UNKNOWN)
    }

    /**
     * Returns one entry for each day in the given interval. The first element corresponds to the
     * newest entry, and the last element corresponds to the oldest. The interval endpoints are
     * included.
     */
    @Synchronized
    open fun getByInterval(from: Timestamp, to: Timestamp): List<Entry> {
        val result = mutableListOf<Entry>()
        if (from.isNewerThan(to)) return result
        var current = to
        while (current >= from) {
            result.add(get(current))
            if (current == from) break
            current = current.minus(1)
        }
        return result
    }

    /**
     * Adds the given entry to the list. If another entry with the same timestamp already exists,
     * replaces it.
     */
    @Synchronized
    open fun add(entry: Entry) {
        entriesByTimestamp[entry.timestamp] = entry
    }

    /**
     * Returns all entries whose values are known, sorted by timestamp. The first element
     * corresponds to the newest entry, and the last element corresponds to the oldest.
     */
    @Synchronized
    open fun getKnown(): List<Entry> {
        return entriesByTimestamp.values.sortedByDescending { it.timestamp }
    }

    /**
     * Replaces all entries in this list by entries computed automatically from another list.
     *
     * For boolean habits, this function creates additional entries (with value YES_AUTO) according
     * to the frequency of the habit. Non-daily numerical habits use NUMERICAL_AUTO for rest days,
     * preserving all originally recorded measurements.
     */
    @Synchronized
    open fun recomputeFrom(
        originalEntries: EntryList,
        frequency: Frequency,
        isNumerical: Boolean,
        targetValue: Double = 0.0,
        targetType: NumericalHabitType = NumericalHabitType.AT_LEAST
    ) {
        frequency.validate()
        clear()
        val original = originalEntries.getKnown()
        if (isNumerical && frequency.numerator == frequency.denominator) {
            original.forEach { add(it) }
        } else if (isNumerical) {
            val intervals = buildNumericalIntervals(frequency, original, targetValue, targetType)
            snapIntervalsTogether(intervals)
            val computed = buildNumericalEntriesFromInterval(original, intervals)
            computed.filter { it.value != UNKNOWN || it.notes.isNotEmpty() }.forEach { add(it) }
        } else {
            val intervals = buildIntervals(frequency, original)
            snapIntervalsTogether(intervals)
            val computed = buildEntriesFromInterval(original, intervals)
            computed.filter { it.value != UNKNOWN || it.notes.isNotEmpty() }.forEach { add(it) }
        }
    }

    /**
     * Removes all known entries.
     */
    @Synchronized
    open fun clear() {
        entriesByTimestamp.clear()
    }

    /**
     * Returns the total number of successful entries for each month, grouped by day of week.
     * <p>
     * The checkmarks are returned in a HashMap. The key is the timestamp for
     * the first day of the month, at midnight (00:00). The value is an integer
     * array with 7 entries. The first entry contains the total number of
     * successful checkmarks during the specified month that occurred on a Saturday. The
     * second entry corresponds to Sunday, and so on. If there are no
     * successful checkmarks during a certain month, the value is null.
     *
     * @return total number of checkmarks by month versus day of week
     */
    @Synchronized
    fun computeWeekdayFrequency(isNumerical: Boolean): HashMap<Timestamp, Array<Int>> {
        val entries = getKnown()
        val map = hashMapOf<Timestamp, Array<Int>>()
        for ((originalTimestamp, value) in entries) {
            val weekday = originalTimestamp.weekday
            val truncatedTimestamp = Timestamp(
                originalTimestamp.toCalendar().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }.timeInMillis
            )

            var list = map[truncatedTimestamp]
            if (list == null) {
                list = arrayOf(0, 0, 0, 0, 0, 0, 0)
                map[truncatedTimestamp] = list
            }

            if (isNumerical) {
                if (value >= 0) list[weekday] += value
            } else if (value == YES_MANUAL) {
                list[weekday] += 1
            }
        }
        return map
    }

    data class Interval(val begin: Timestamp, val center: Timestamp, val end: Timestamp) {
        val length: Int
            get() = begin.daysUntil(end) + 1
    }

    companion object {
        /**
         * Converts a list of intervals into a list of entries. Entries that fall outside of any
         * interval receive value UNKNOWN. Entries that fall within an interval but do not appear
         * in [original] receive value YES_AUTO. Entries provided in [original] are copied over.
         *
         * The intervals should be sorted by timestamp. The first element in the list should
         * correspond to the newest interval.
         */
        fun buildEntriesFromInterval(
            original: List<Entry>,
            intervals: List<Interval>
        ): List<Entry> {
            val result = buildAutomaticEntries(original, intervals, YES_AUTO)
            original.forEach { entry ->
                val value = if (
                    result[entry.timestamp]?.value != YES_AUTO ||
                    entry.value == SKIP ||
                    entry.value == YES_MANUAL
                ) {
                    entry.value
                } else {
                    YES_AUTO
                }
                result[entry.timestamp] = Entry(entry.timestamp, value, entry.notes)
            }
            return result.values.sortedByDescending { it.timestamp }
        }

        /**
         * Starting from the second newest interval, this function tries to slide the
         * intervals backwards into the past, so that gaps are eliminated and
         * streaks are maximized.
         *
         * The intervals should be sorted by timestamp. The first element in the list should
         * correspond to the newest interval.
         */
        fun snapIntervalsTogether(intervals: ArrayList<Interval>) {
            for (i in 1 until intervals.size) {
                val curr = intervals[i]
                val next = intervals[i - 1]
                val gapNextToCurrent = (curr.end.unixTime - next.begin.unixTime) / Timestamp.DAY_LENGTH
                val gapCenterToEnd = curr.center.daysUntil(curr.end)
                if (gapNextToCurrent >= 0) {
                    val shift = minOf(
                        gapCenterToEnd.toLong(),
                        gapNextToCurrent + 1,
                        curr.begin.unixTime / Timestamp.DAY_LENGTH
                    ).toInt()
                    intervals[i] = Interval(
                        curr.begin.minus(shift),
                        curr.center,
                        curr.end.minus(shift)
                    )
                }
            }
        }

        fun buildIntervals(
            freq: Frequency,
            entries: List<Entry>
        ): ArrayList<Interval> {
            val filtered = entries.filter { it.value == YES_MANUAL }
            val num = freq.numerator
            val den = freq.denominator
            val intervals = arrayListOf<Interval>()
            for (i in num - 1 until filtered.size) {
                val (begin, _) = filtered[i]
                val (center, _) = filtered[i - num + 1]
                var size = den
                if (den == 30 || den == 31) {
                    val beginDate = begin.toLocalDate()
                    size = if (beginDate.day == beginDate.monthLength) {
                        beginDate.plus(1).monthLength
                    } else {
                        beginDate.monthLength
                    }
                }
                if (begin.daysUntil(center) < size) {
                    val end = begin.plus(size - 1)
                    intervals.add(Interval(begin, center, end))
                }
            }
            return intervals
        }

        fun buildNumericalIntervals(
            freq: Frequency,
            entries: List<Entry>,
            targetValue: Double,
            targetType: NumericalHabitType
        ): ArrayList<Interval> {
            val filtered = entries.filter { entry ->
                entry.value >= 0 && entry.value != SKIP && when (targetType) {
                    NumericalHabitType.AT_LEAST -> entry.value / 1000.0 >= targetValue
                    NumericalHabitType.AT_MOST -> entry.value / 1000.0 <= targetValue
                }
            }
            val num = freq.numerator
            val den = freq.denominator
            val intervals = arrayListOf<Interval>()
            for (i in num - 1 until filtered.size) {
                val (begin, _) = filtered[i]
                val (center, _) = filtered[i - num + 1]
                var size = den
                if (den == 30 || den == 31) {
                    val beginDate = begin.toLocalDate()
                    size = if (beginDate.day == beginDate.monthLength) {
                        beginDate.plus(1).monthLength
                    } else {
                        beginDate.monthLength
                    }
                }
                if (begin.daysUntil(center) < size) {
                    val end = begin.plus(size - 1)
                    intervals.add(Interval(begin, center, end))
                }
            }
            return intervals
        }

        fun buildNumericalEntriesFromInterval(
            original: List<Entry>,
            intervals: ArrayList<Interval>
        ): ArrayList<Entry> {
            val result = buildAutomaticEntries(original, intervals, NUMERICAL_AUTO)
            original.forEach { result[it.timestamp] = it }
            return ArrayList(result.values.sortedByDescending { it.timestamp })
        }

        private fun buildAutomaticEntries(
            original: List<Entry>,
            intervals: List<Interval>,
            automaticValue: Int
        ): HashMap<Timestamp, Entry> {
            val result = hashMapOf<Timestamp, Entry>()
            if (original.isEmpty()) return result

            // Match the score/streak horizon without allocating billions of future rest days.
            // Recorded future entries are copied separately and never truncated.
            val horizon = DateUtils.getTodayWithOffset().plus(30)
            val ranges = intervals.mapNotNull {
                val begin = maxOf(it.begin, Timestamp.ZERO)
                val end = minOf(it.end, horizon)
                if (begin <= end) begin to end else null
            }.sortedBy { it.first }
            val oldest = original.minOf { it.timestamp }
            val newest = original.maxOf { it.timestamp }
            val from = minOf(oldest, ranges.firstOrNull()?.first ?: oldest)
            val to = minOf(horizon, maxOf(newest, ranges.maxOfOrNull { it.second } ?: newest))
            var current = to
            while (current >= from) {
                result[current] = Entry(current, UNKNOWN)
                if (current == from) break
                current = current.minus(1)
            }

            var coveredThrough: Timestamp? = null
            for ((begin, end) in ranges) {
                current = maxOf(begin, coveredThrough?.plus(1) ?: begin)
                while (current <= end) {
                    result[current] = Entry(current, automaticValue)
                    if (current == end) break
                    current = current.plus(1)
                }
                coveredThrough = maxOf(coveredThrough ?: end, end)
            }
            return result
        }
    }
}

/**
 * Given a list of entries, truncates the timestamp of each entry (according to the field given),
 * groups the entries according to this truncated timestamp, then creates a new entry (t,v) for
 * each group, where t is the truncated timestamp and v is the sum of the values of all entries in
 * the group.
 *
 * For numerical habits, non-positive entry values are converted to zero. For boolean habits, each
 * YES_MANUAL value is converted to 1000 and all other values are converted to zero.
 *
 * Skips, unknown values, and automatic rest days contribute zero.
 *
 * The returned list is sorted by timestamp, with the newest entry coming first and the oldest entry
 * coming last. If the original list has gaps in it (for example, weeks or months without any
 * entries), then the list produced by this method will also have gaps.
 *
 * The argument [firstWeekday] is only relevant when truncating by week.
 */
fun List<Entry>.groupedSum(
    truncateField: DateUtils.TruncateField,
    firstWeekday: Int = Calendar.SATURDAY,
    isNumerical: Boolean
): List<EntryAggregate> {
    return this.map { (timestamp, value) ->
        if (isNumerical) {
            EntryAggregate(timestamp, max(0, value).toLong())
        } else {
            EntryAggregate(timestamp, if (value == YES_MANUAL) 1000L else 0L)
        }
    }.groupBy { entry ->
        entry.timestamp.truncate(
            truncateField,
            firstWeekday
        )
    }.entries.map { (timestamp, entries) ->
        EntryAggregate(timestamp, entries.sumOf { it.value })
    }.sortedBy { (timestamp, _) ->
        -timestamp.unixTime
    }
}

/**
 * Counts the number of days with vaLue SKIP in the given period.
 */
fun List<Entry>.countSkippedDays(
    truncateField: DateUtils.TruncateField,
    firstWeekday: Int = Calendar.SATURDAY
): List<Entry> {
    return this.map { (timestamp, value) ->
        if (value == SKIP) {
            Entry(timestamp, 1)
        } else {
            Entry(timestamp, 0)
        }
    }.groupBy { entry ->
        entry.timestamp.truncate(
            truncateField,
            firstWeekday
        )
    }.entries.map { (timestamp, entries) ->
        Entry(timestamp, entries.sumOf { it.value })
    }.sortedBy { (timestamp, _) ->
        -timestamp.unixTime
    }
}
