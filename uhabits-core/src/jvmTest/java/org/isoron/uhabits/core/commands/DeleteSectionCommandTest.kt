package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.BaseUnitTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeleteSectionCommandTest : BaseUnitTest() {
    @Test
    fun deletesSectionWithoutDeletingHabitsOrTheirData() {
        val morning = sectionList.add("Morning")
        val evening = sectionList.add("Evening")
        val first = modelFactory.buildHabit().apply { sectionId = morning.id }
        val second = modelFactory.buildHabit().apply { sectionId = evening.id }
        habitList.add(first)
        habitList.add(second)
        val before = first.copy()
        DeleteSectionCommand(sectionList, habitList, morning.id).run()
        assertEquals(before.copy(sectionId = null), first)
        assertEquals(evening.id, second.sectionId)
        assertEquals(2, habitList.size())
        assertNull(sectionList.getById(morning.id))
        assertEquals(0, sectionList.getById(evening.id)!!.position)
        verify(habitList).update(listOf(first))
    }

    @Test
    fun createRenameMoveCommandsValidateAndNotify() {
        var notifications = 0
        sectionList.observable.addListener { notifications++ }
        CreateSectionCommand(sectionList, "Morning").run()
        CreateSectionCommand(sectionList, "Evening").run()
        val morning = sectionList.getByName("Morning")!!
        val evening = sectionList.getByName("Evening")!!
        RenameSectionCommand(sectionList, morning.id, " Early ").run()
        MoveSectionCommand(sectionList, evening.id, 0).run()
        assertEquals(listOf("Evening", "Early"), sectionList.getAll().map { it.name })
        assertEquals(4, notifications)
        assertThrows(IllegalArgumentException::class.java) { CreateSectionCommand(sectionList, "EARLY").run() }
        assertThrows(IllegalArgumentException::class.java) { RenameSectionCommand(sectionList, 9999, "Late").run() }
        assertThrows(IllegalArgumentException::class.java) { MoveSectionCommand(sectionList, 9999, 0).run() }
        assertThrows(IllegalArgumentException::class.java) { DeleteSectionCommand(sectionList, habitList, 9999).run() }
        assertEquals(4, notifications)
    }
}
