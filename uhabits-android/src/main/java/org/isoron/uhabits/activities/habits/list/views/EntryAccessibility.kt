package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Timestamp
import java.text.DateFormat
import java.util.TimeZone

internal fun Context.entryDescription(
    habitName: String,
    timestamp: Timestamp?,
    state: String,
    notes: String
): String {
    val date = timestamp?.let {
        DateFormat.getDateInstance(DateFormat.FULL, resources.configuration.locales[0]).apply {
            // Habit timestamps encode calendar dates in UTC, not instants in the device timezone.
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(it.toJavaDate())
    }.orEmpty()
    val description = if (habitName.isEmpty() && date.isEmpty()) {
        state
    } else {
        getString(R.string.habit_entry_description, habitName, date, state)
    }
    return if (notes.isBlank()) description else getString(R.string.habit_entry_with_notes, description, notes)
}
