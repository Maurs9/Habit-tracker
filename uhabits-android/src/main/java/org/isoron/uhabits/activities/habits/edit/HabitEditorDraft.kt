package org.isoron.uhabits.activities.habits.edit

import org.isoron.uhabits.core.models.HabitTags
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.utils.parseFiniteNumber
import java.io.Serializable
import java.util.Locale

internal data class HabitEditorDraft(
    val name: String,
    val question: String,
    val notes: String,
    val type: Int,
    val unit: String,
    val target: String,
    val targetType: Int,
    val color: Int,
    val numerator: Int,
    val denominator: Int,
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderDays: Int,
    val tags: Set<String>,
    val sectionId: Long?
) : Serializable {
    fun normalized(locale: Locale) = copy(
        name = name.trim(),
        question = question.trim(),
        notes = notes.trim(),
        unit = unit.trim(),
        target = parseFiniteNumber(target, locale)?.toString() ?: target.trim(),
        numerator = if (numerator == denominator) 1 else numerator,
        denominator = if (numerator == denominator) 1 else denominator,
        reminderMinute = if (reminderHour < 0) -1 else reminderMinute,
        reminderDays = if (reminderHour < 0) WeekdayList.EVERY_DAY.toInteger() else reminderDays,
        tags = HabitTags.normalize(tags)
    )
}
