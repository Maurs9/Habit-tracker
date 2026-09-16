package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.SectionList

data class CreateSectionCommand(val sectionList: SectionList, val name: String) : SectionCommand {
    override fun run() {
        sectionList.add(name)
    }
}
