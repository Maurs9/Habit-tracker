package org.isoron.uhabits.core.preferences

enum class ListDensity(
    val rowHeightDp: Int,
    val rowGapDp: Int,
    val sectionTopDp: Int,
    val firstSectionTopDp: Int,
    val sectionBottomDp: Int
) {
    COMPACT(48, 1, 20, 8, 4)
}
