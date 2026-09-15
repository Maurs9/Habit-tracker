package org.isoron.uhabits.core.models

import java.util.Locale

object HabitTags {
    fun parse(text: String): Set<String> = normalize(text.lines())

    fun normalize(tags: Iterable<String>): Set<String> =
        tags.flatMap { it.lines() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toSet()

    fun format(tags: Iterable<String>): String =
        normalize(tags).sortedBy { it.lowercase(Locale.ROOT) }.joinToString("\n")

    fun containsAll(tags: Set<String>, required: Set<String>): Boolean {
        val names = tags.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        return required.all { it.lowercase(Locale.ROOT) in names }
    }
}
