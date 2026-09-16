package org.isoron.uhabits.core.models

import java.lang.ref.WeakReference
import java.util.Locale

abstract class SectionList {
    val observable = ModelObservable()
    private val habitLists = mutableListOf<WeakReference<HabitList>>()

    abstract fun add(name: String): Section
    abstract fun getAll(): List<Section>
    abstract fun getById(id: Long): Section?
    abstract fun getByName(name: String): Section?
    abstract fun rename(section: Section, name: String)
    abstract fun move(section: Section, newPosition: Int)
    abstract fun remove(section: Section)
    abstract fun size(): Int
    abstract fun repair()

    fun nameMap(): Map<Long, String> = getAll().associate { it.id to it.name }

    @Synchronized
    internal fun registerHabitList(habits: HabitList) {
        habitLists.removeAll { it.get() == null }
        habitLists.add(WeakReference(habits))
    }

    protected fun validateName(name: String, id: Long? = null): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Section name must not be blank" }
        val existing = getByName(trimmed)
        require(existing == null || existing.id == id) { "Section name already exists" }
        return trimmed
    }

    protected fun clearInvalidHabitReferences() {
        val ids = getAll().map { it.id }.toSet()
        val lists = synchronized(this) { habitLists.mapNotNull { it.get() } }
        for (habits in lists) {
            val changed = habits.filter { it.sectionId != null && it.sectionId !in ids }
            if (changed.isEmpty()) continue
            changed.forEach { it.sectionId = null }
            habits.update(changed)
            changed.forEach { it.observable.notifyListeners() }
        }
    }

    companion object {
        fun normalizeName(name: String): String = name.trim().lowercase(Locale.ROOT)
    }
}
