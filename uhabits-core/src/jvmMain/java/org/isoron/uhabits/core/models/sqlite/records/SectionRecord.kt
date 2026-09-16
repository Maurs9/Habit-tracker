package org.isoron.uhabits.core.models.sqlite.records

import org.isoron.uhabits.core.database.Column
import org.isoron.uhabits.core.database.Table
import org.isoron.uhabits.core.models.Section

@Table(name = "sections")
class SectionRecord {
    @field:Column
    var id: Long? = null

    @field:Column
    var name: String? = null

    @field:Column
    var position: Int? = null

    fun copyFrom(section: Section) {
        id = section.id
        name = section.name
        position = section.position
    }

    fun toSection(): Section {
        val section = Section(requireNotNull(id), requireNotNull(name), requireNotNull(position))
        require(section.id >= 1 && section.name.isNotBlank()) { "Invalid section metadata" }
        return section
    }
}
