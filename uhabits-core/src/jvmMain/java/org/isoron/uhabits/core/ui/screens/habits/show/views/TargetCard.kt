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

package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Timestamp
import org.isoron.uhabits.core.models.groupedSum
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.core.utils.DateUtils
import java.time.YearMonth
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

data class TargetCardState(
    val color: PaletteColor,
    val values: List<Double> = listOf(),
    val targets: List<Double> = listOf(),
    val intervals: List<Int> = listOf(),
    val theme: Theme
)

class TargetCardPresenter {
    companion object {
        fun buildState(
            habit: Habit,
            firstWeekday: Int,
            theme: Theme
        ): TargetCardState {
            val today = DateUtils.getTodayWithOffset()
            val cal = today.toCalendar()
            val year = cal[Calendar.YEAR]
            val month = cal[Calendar.MONTH] + 1
            val quarterStart = YearMonth.of(year, (month - 1) / 3 * 3 + 1)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysInQuarter = (0L..2L).sumOf { quarterStart.plusMonths(it).lengthOfMonth() }
            val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            val denominator = habit.frequency.denominator
            val monthly = Frequency.isMonthlyInterval(denominator)
            val periodTarget = habit.targetValue * habit.frequency.numerator
            val dailyTarget = periodTarget / denominator
            val periods = buildList {
                if (denominator <= 1) add(Period(1, DateUtils.TruncateField.DAY, 1))
                if (denominator <= 7) add(Period(7, DateUtils.TruncateField.WEEK_NUMBER, 7))
                add(Period(30, DateUtils.TruncateField.MONTH, daysInMonth, 1))
                add(Period(91, DateUtils.TruncateField.QUARTER, daysInQuarter, 3))
                add(Period(365, DateUtils.TruncateField.YEAR, daysInYear, 12))
            }

            val starts = periods.map { DateUtils.truncate(it.field, today.unixTime, firstWeekday) }
            val oldest = Timestamp(max(0L, min(today.unixTime, starts.minOrNull() ?: today.unixTime)))
            val entries = habit.computedEntries.getByInterval(oldest, today)
            val skipped = entries.filter { it.value == Entry.SKIP }
            val values = periods.map { period ->
                val value = entries.groupedSum(period.field, firstWeekday, habit.isNumerical).firstOrNull()?.value
                (value?.toDouble() ?: 0.0) / 1000.0
            }
            val targets = periods.mapIndexed { index, period ->
                val target = if (monthly) periodTarget * period.months else dailyTarget * period.days
                val skippedTarget = skipped.asSequence().filter { it.timestamp.unixTime >= starts[index] }.sumOf {
                    if (monthly) {
                        val date = it.timestamp.toLocalDate()
                        periodTarget / YearMonth.of(date.year, date.month).lengthOfMonth()
                    } else {
                        dailyTarget
                    }
                }
                max(0.0, target - skippedTarget)
            }

            return TargetCardState(
                color = habit.color,
                values = values,
                targets = targets,
                intervals = periods.map { it.interval },
                theme = theme
            )
        }

        private data class Period(
            val interval: Int,
            val field: DateUtils.TruncateField,
            val days: Int,
            val months: Int = 0
        )
    }
}
