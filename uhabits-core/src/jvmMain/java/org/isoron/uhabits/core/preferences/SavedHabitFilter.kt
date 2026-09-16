package org.isoron.uhabits.core.preferences

import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitTags
import java.net.URLDecoder
import java.net.URLEncoder

data class SavedHabitFilter(
    val name: String,
    val tags: Set<String>,
    val showArchived: Boolean,
    val showCompleted: Boolean,
    val primaryOrder: HabitList.Order,
    val secondaryOrder: HabitList.Order,
    val groupBySection: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "A saved filter needs a name" }
    }

    fun encode(): String = listOf(
        URLEncoder.encode(name, "UTF-8"),
        URLEncoder.encode(HabitTags.format(tags), "UTF-8"),
        showArchived.toString(),
        showCompleted.toString(),
        primaryOrder.name,
        secondaryOrder.name,
        groupBySection.toString()
    ).joinToString("\t")

    companion object {
        fun decode(record: String): SavedHabitFilter {
            val fields = record.split('\t')
            require(fields.size in 6..7) { "Invalid saved filter" }
            return SavedHabitFilter(
                name = URLDecoder.decode(fields[0], "UTF-8"),
                tags = HabitTags.parse(URLDecoder.decode(fields[1], "UTF-8")),
                showArchived = fields[2].toBooleanStrict(),
                showCompleted = fields[3].toBooleanStrict(),
                primaryOrder = HabitList.Order.valueOf(fields[4]),
                secondaryOrder = HabitList.Order.valueOf(fields[5]),
                groupBySection = fields.getOrNull(6)?.toBooleanStrict() ?: false
            )
        }
    }
}
