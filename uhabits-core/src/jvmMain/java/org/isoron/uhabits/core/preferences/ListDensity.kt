package org.isoron.uhabits.core.preferences

enum class ListDensity(
    val rowHeightDp: Int,
    val rowGapDp: Int,
    val titleTextSizeSp: Float,
    val ringSizeDp: Int,
    val ringThicknessDp: Float,
    val checkmarkIconSizeSp: Float,
    val numberTextSizeSp: Float,
    val unitTextSizeSp: Float,
    val sectionTopDp: Int,
    val firstSectionTopDp: Int,
    val sectionBottomDp: Int,
    val sectionTextSizeSp: Float
) {
    COMPACT(
        rowHeightDp = 40,
        rowGapDp = 1,
        titleTextSizeSp = 14.5f,
        ringSizeDp = 13,
        ringThicknessDp = 2.5f,
        checkmarkIconSizeSp = 12.5f,
        numberTextSizeSp = 13.0f,
        unitTextSizeSp = 11.0f,
        sectionTopDp = 16,
        firstSectionTopDp = 6,
        sectionBottomDp = 3,
        sectionTextSizeSp = 11.0f
    ),
    STANDARD(
        rowHeightDp = 48,
        rowGapDp = 3,
        titleTextSizeSp = 16.0f,
        ringSizeDp = 15,
        ringThicknessDp = 3.0f,
        checkmarkIconSizeSp = 14.0f,
        numberTextSizeSp = 14.0f,
        unitTextSizeSp = 12.0f,
        sectionTopDp = 28,
        firstSectionTopDp = 12,
        sectionBottomDp = 6,
        sectionTextSizeSp = 12.0f
    ),
    LARGE(
        rowHeightDp = 64,
        rowGapDp = 4,
        titleTextSizeSp = 18.5f,
        ringSizeDp = 19,
        ringThicknessDp = 4.0f,
        checkmarkIconSizeSp = 18.0f,
        numberTextSizeSp = 17.0f,
        unitTextSizeSp = 13.5f,
        sectionTopDp = 32,
        firstSectionTopDp = 16,
        sectionBottomDp = 8,
        sectionTextSizeSp = 13.5f
    );

    companion object {
        @JvmField
        val SPACIOUS = LARGE
    }
}
