package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.SectionList

data class MoveSectionCommand(val sectionList: SectionList, val id: Long, val newPosition: Int) : SectionCommand {
    override fun run() {
        sectionList.move(requireNotNull(sectionList.getById(id)) { "Section not found: $id" }, newPosition)
    }
}
