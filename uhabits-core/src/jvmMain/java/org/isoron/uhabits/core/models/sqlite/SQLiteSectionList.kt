package org.isoron.uhabits.core.models.sqlite

import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.Section
import org.isoron.uhabits.core.models.SectionList
import org.isoron.uhabits.core.models.sqlite.records.SectionRecord

class SQLiteSectionList(modelFactory: ModelFactory) : SectionList() {
    private val repository = modelFactory.buildSectionListRepository()
    private var sections: List<Section>? = null

    @Synchronized
    override fun add(name: String): Section {
        val record = SectionRecord().apply {
            this.name = validateName(name)
            position = size()
        }
        repository.save(record)
        val section = record.toSection()
        sections = getAll() + section
        observable.notifyListeners()
        return section
    }

    @Synchronized
    override fun getAll(): List<Section> {
        if (sections == null) {
            val loaded = repository.findAll("ORDER BY position, id").map { it.toSection() }
            require(loaded.map { normalizeName(it.name) }.distinct().size == loaded.size) {
                "Duplicate section names"
            }
            if (loaded.withIndex().any { (position, section) -> section.position != position }) {
                transaction { rebuildOrder(loaded) }
            }
            sections = loaded.mapIndexed { position, section -> section.copy(position = position) }
        }
        return sections!!.toList()
    }

    @Synchronized
    override fun getById(id: Long): Section? = getAll().find { it.id == id }

    @Synchronized
    override fun getByName(name: String): Section? =
        getAll().find { normalizeName(it.name) == normalizeName(name) }

    @Synchronized
    override fun rename(section: Section, name: String) {
        val current = requireSection(section)
        val replacement = current.copy(name = validateName(name, section.id))
        repository.save(SectionRecord().apply { copyFrom(replacement) })
        sections = getAll().map { if (it.id == section.id) replacement else it }
        observable.notifyListeners()
    }

    @Synchronized
    override fun move(section: Section, newPosition: Int) {
        val current = requireSection(section)
        val reordered = getAll().toMutableList()
        require(newPosition in reordered.indices) { "Invalid section position" }
        reordered.remove(current)
        reordered.add(newPosition, current)
        transaction { rebuildOrder(reordered) }
        sections = reordered.mapIndexed { position, item -> item.copy(position = position) }
        observable.notifyListeners()
    }

    override fun remove(section: Section) {
        synchronized(this) {
            requireSection(section)
            val remaining = getAll().filter { it.id != section.id }
            transaction {
                repository.execSQL("UPDATE habits SET section_id = NULL WHERE section_id = ?", section.id)
                repository.execSQL("DELETE FROM sections WHERE id = ?", section.id)
                rebuildOrder(remaining)
            }
            sections = remaining.mapIndexed { position, item -> item.copy(position = position) }
        }
        clearInvalidHabitReferences()
        observable.notifyListeners()
    }

    @Synchronized
    override fun size(): Int = getAll().size

    override fun repair() {
        synchronized(this) {
            sections = null
            val current = getAll()
            transaction {
                repository.execSQL(
                    "UPDATE habits SET section_id = NULL " +
                        "WHERE section_id IS NOT NULL AND section_id NOT IN (SELECT id FROM sections)"
                )
                rebuildOrder(current)
            }
            sections = current.mapIndexed { position, item -> item.copy(position = position) }
        }
        clearInvalidHabitReferences()
        observable.notifyListeners()
    }

    @Synchronized
    fun reload() {
        sections = null
        getAll()
        observable.notifyListeners()
    }

    private fun requireSection(section: Section): Section =
        requireNotNull(getById(section.id)) { "Section not found: ${section.id}" }

    private fun rebuildOrder(ordered: List<Section>) {
        ordered.forEachIndexed { position, section ->
            if (section.position != position) {
                repository.execSQL("UPDATE sections SET position = ? WHERE id = ?", position, section.id)
            }
        }
    }

    private fun transaction(action: () -> Unit) {
        repository.executeAsTransaction(allowNesting = true) { action() }
    }
}
