package org.isoron.uhabits.core.models.memory

import org.isoron.uhabits.core.models.Section
import org.isoron.uhabits.core.models.SectionList

class MemorySectionList : SectionList() {
    private val sections = mutableListOf<Section>()
    private var nextId = 1L

    @Synchronized
    override fun add(name: String): Section {
        val section = Section(nextId, validateName(name), sections.size)
        nextId++
        sections.add(section)
        observable.notifyListeners()
        return section
    }

    @Synchronized
    override fun getAll(): List<Section> = sections.toList()

    @Synchronized
    override fun getById(id: Long): Section? = sections.find { it.id == id }

    @Synchronized
    override fun getByName(name: String): Section? =
        sections.find { normalizeName(it.name) == normalizeName(name) }

    @Synchronized
    override fun rename(section: Section, name: String) {
        val index = indexOf(section)
        sections[index] = sections[index].copy(name = validateName(name, section.id))
        observable.notifyListeners()
    }

    @Synchronized
    override fun move(section: Section, newPosition: Int) {
        val index = indexOf(section)
        require(newPosition in sections.indices) { "Invalid section position" }
        sections.add(newPosition, sections.removeAt(index))
        rebuildOrder()
        observable.notifyListeners()
    }

    override fun remove(section: Section) {
        synchronized(this) {
            sections.removeAt(indexOf(section))
            rebuildOrder()
        }
        clearInvalidHabitReferences()
        observable.notifyListeners()
    }

    @Synchronized
    override fun size(): Int = sections.size

    override fun repair() {
        synchronized(this) { rebuildOrder() }
        clearInvalidHabitReferences()
        observable.notifyListeners()
    }

    private fun indexOf(section: Section): Int {
        val index = sections.indexOfFirst { it.id == section.id }
        require(index >= 0) { "Section not found: ${section.id}" }
        return index
    }

    private fun rebuildOrder() {
        sections.indices.forEach { sections[it] = sections[it].copy(position = it) }
    }
}
