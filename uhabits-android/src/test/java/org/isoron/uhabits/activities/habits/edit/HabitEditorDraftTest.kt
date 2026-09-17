package org.isoron.uhabits.activities.habits.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Locale

class HabitEditorDraftTest {
    private val original = HabitEditorDraft(
        name = "Read", question = "", notes = "", type = 1, unit = "pages", target = "1", targetType = 0,
        color = 33, numerator = 1, denominator = 1, reminderHour = -1, reminderMinute = -1,
        reminderDays = 127, tags = setOf("Evening"), sectionId = null
    )

    @Test
    fun normalizesEquivalentTextNumbersFrequencyAndDisabledReminders() {
        val equivalent = original.copy(
            name = " Read ",
            unit = " pages ",
            target = "1,00",
            numerator = 7,
            denominator = 7,
            reminderMinute = 30,
            reminderDays = 31,
            tags = setOf(" Evening ")
        )
        assertEquals(original.normalized(Locale.US), equivalent.normalized(Locale.GERMANY))
    }

    @Test
    fun detectsEveryEditableFieldAndInvalidNumberDrafts() {
        val changes = listOf(
            original.copy(name = "Walk"),
            original.copy(question = "How many?"),
            original.copy(notes = "New note"),
            original.copy(type = 0),
            original.copy(unit = "chapters"),
            original.copy(target = "2"),
            original.copy(targetType = 1),
            original.copy(target = "unfinished"),
            original.copy(color = 42),
            original.copy(numerator = 3, denominator = 7),
            original.copy(denominator = 7),
            original.copy(reminderHour = 8, reminderMinute = 30),
            original.copy(tags = setOf("Morning")),
            original.copy(sectionId = 5)
        )
        for (change in changes) {
            assertNotEquals(change.toString(), original.normalized(Locale.US), change.normalized(Locale.US))
        }
        val reminded = original.copy(reminderHour = 8, reminderMinute = 30)
        assertNotEquals(reminded.normalized(Locale.US), reminded.copy(reminderMinute = 45).normalized(Locale.US))
        assertNotEquals(reminded.normalized(Locale.US), reminded.copy(reminderDays = 31).normalized(Locale.US))
    }

    @Test
    fun baselineSurvivesSavedStateSerialization() {
        val draft = original.normalized(Locale.US)
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(draft) }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        assertEquals(draft, restored)
    }
}
