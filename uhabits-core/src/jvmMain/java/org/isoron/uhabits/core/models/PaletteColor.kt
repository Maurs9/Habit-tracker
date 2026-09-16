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
            "#D32F2F", //  0 Red (Vibrant)
            "#A65050", //  1 Red (Muted)
            "#B71C1C", //  2 Red (Deep)
            "#C43E12", //  3 Coral (Vibrant)
            "#A6563D", //  4 Coral (Muted)
            "#8F2809", //  5 Coral (Deep)
            "#AE5800", //  6 Orange (Vibrant)
            "#96602A", //  7 Orange (Muted)
            "#8A3300", //  8 Orange (Deep)
            "#9C6800", //  9 Amber (Vibrant)
            "#8C6A2E", // 10 Amber (Muted)
            "#7A4A00", // 11 Amber (Deep)
            "#827000", // 12 Yellow (Vibrant)
            "#7C7038", // 13 Yellow (Muted)
            "#665500", // 14 Yellow (Deep)
            "#657400", // 15 Lime (Vibrant)
            "#6A6B45", // 16 Lime (Muted)
            "#4A4D00", // 17 Lime (Deep)
            "#2E7D32", // 18 Green (Vibrant)
            "#4A764D", // 19 Green (Muted)
            "#1B5E20", // 20 Green (Deep)
            "#00796B", // 21 Teal (Vibrant)
            "#34766F", // 22 Teal (Muted)
            "#004D40", // 23 Teal (Deep)
            "#007B88", // 24 Cyan (Vibrant)
            "#3A7078", // 25 Cyan (Muted)
            "#006064", // 26 Cyan (Deep)
            "#1565C0", // 27 Blue (Vibrant)
            "#3E6FA0", // 28 Blue (Muted)
            "#0D47A1", // 29 Blue (Deep)
            "#7B1FA2", // 30 Purple (Vibrant)
            "#7D4894", // 31 Purple (Muted)
            "#4A148C", // 32 Purple (Deep)
            "#C2185B", // 33 Pink (Vibrant)
            "#A6446B", // 34 Pink (Muted)
            "#880E4F", // 35 Pink (Deep)
            "#6E6E6E", // 36 Gray
            "#4F4F4F", // 37 Dark Gray
            "#455A64", // 38 Slate
            "#212121" // 39 Charcoal
        )[paletteIndex]
    }

    fun compareTo(other: PaletteColor): Int {
        return paletteIndex.compareTo(other.paletteIndex)
    }

    companion object {
        // Persisted slots are append-only: values may be tuned, but never reorder indexes.
        const val COUNT = 40
        val DEFAULT = PaletteColor(27)
    }
}
