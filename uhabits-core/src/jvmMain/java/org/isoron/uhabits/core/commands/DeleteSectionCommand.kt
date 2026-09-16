package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.SectionList

data class DeleteSectionCommand(
    val sectionList: SectionList,
    val habitList: HabitList,
    val id: Long
) : SectionCommand {
    override fun run() {
        val section = requireNotNull(sectionList.getById(id)) { "Section not found: $id" }
        val affected = habitList.filter { it.sectionId == id }
        // Persist deletion before touching live references, so a failed delete leaves them intact.
        sectionList.remove(section)
        affected.forEach { it.sectionId = null }
        habitList.update(affected)
    }
}
