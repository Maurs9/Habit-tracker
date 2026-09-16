package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.SectionList

data class RenameSectionCommand(val sectionList: SectionList, val id: Long, val name: String) : SectionCommand {
    override fun run() {
        sectionList.rename(requireNotNull(sectionList.getById(id)) { "Section not found: $id" }, name)
    }
}
