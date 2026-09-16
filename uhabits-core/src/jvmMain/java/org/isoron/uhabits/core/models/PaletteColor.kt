/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.core.models

data class PaletteColor(val paletteIndex: Int) {
    init {
        require(paletteIndex in 0 until COUNT) { "Invalid palette index: $paletteIndex" }
    }

    fun toCsvColor(): String {
        return arrayOf(
            "#F44336", //  0 Red (Vibrant)
            "#EF9A9A", //  1 Red (Soft)
            "#B71C1C", //  2 Red (Deep)
            "#FF5722", //  3 Coral (Vibrant)
            "#FFAB91", //  4 Coral (Soft)
            "#BF360C", //  5 Coral (Deep)
            "#FF9800", //  6 Orange (Vibrant)
            "#FFCC80", //  7 Orange (Soft)
            "#E65100", //  8 Orange (Deep)
            "#FFC107", //  9 Amber (Vibrant)
            "#FFE082", // 10 Amber (Soft)
            "#FF8F00", // 11 Amber (Deep)
            "#FDD835", // 12 Yellow (Vibrant)
            "#FFF59D", // 13 Yellow (Soft)
            "#F57F17", // 14 Yellow (Deep)
            "#C0CA33", // 15 Lime (Vibrant)
            "#E6EE9C", // 16 Lime (Soft)
            "#827717", // 17 Lime (Deep)
            "#4CAF50", // 18 Green (Vibrant)
            "#A5D6A7", // 19 Green (Soft)
            "#1B5E20", // 20 Green (Deep)
            "#009688", // 21 Teal (Vibrant)
            "#80CBC4", // 22 Teal (Soft)
            "#004D40", // 23 Teal (Deep)
            "#00BCD4", // 24 Cyan (Vibrant)
            "#80DEEA", // 25 Cyan (Soft)
            "#006064", // 26 Cyan (Deep)
            "#2196F3", // 27 Blue (Vibrant)
            "#90CAF9", // 28 Blue (Soft)
            "#0D47A1", // 29 Blue (Deep)
            "#9C27B0", // 30 Purple (Vibrant)
            "#CE93D8", // 31 Purple (Soft)
            "#4A148C", // 32 Purple (Deep)
            "#E91E63", // 33 Pink (Vibrant)
            "#F48FB1", // 34 Pink (Soft)
            "#880E4F", // 35 Pink (Deep)
            "#BDBDBD", // 36 Light Gray
            "#757575", // 37 Gray
            "#455A64", // 38 Dark Slate
            "#212121"  // 39 Charcoal
        )[paletteIndex]
    }

    fun compareTo(other: PaletteColor): Int {
        return paletteIndex.compareTo(other.paletteIndex)
    }

    companion object {
        // Persisted indexes are append-only: never reorder or replace existing colors.
        const val COUNT = 40
    }
}
