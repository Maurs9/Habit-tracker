package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList

data class ChangeHabitSectionCommand(
    val habitList: HabitList,
    val selected: List<Habit>,
    val sectionId: Long?
) : SectionCommand {
    override fun run() {
        require(sectionId == null || sectionId >= 1) { "Invalid section id" }
        selected.forEach { it.sectionId = sectionId }
        habitList.update(selected)
    }
}
