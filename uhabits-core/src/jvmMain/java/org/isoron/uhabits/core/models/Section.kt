package org.isoron.uhabits.core.models

/** Immutable snapshot; changes must go through [SectionList]. */
data class Section(val id: Long, val name: String, val position: Int)
