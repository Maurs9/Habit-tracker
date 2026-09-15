package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitTags

data class ChangeHabitTagsCommand(
    val habitList: HabitList,
    val selected: List<Habit>,
    val tags: Set<String>
) : Command {
    override fun run() {
        val normalized = HabitTags.normalize(tags)
        selected.forEach { it.tags = normalized }
        habitList.update(selected)
    }
}
