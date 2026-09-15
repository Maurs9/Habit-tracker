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
            "#D32F2F", //  0 red
            "#E64A19", //  1 deep orange
            "#F57C00", //  2 orange
            "#FF8F00", //  3 amber
            "#F9A825", //  4 yellow
            "#AFB42B", //  5 lime
            "#7CB342", //  6 light green
            "#388E3C", //  7 green
            "#00897B", //  8 teal
            "#00ACC1", //  9 cyan
            "#039BE5", // 10 light blue
            "#1976D2", // 11 blue
            "#303F9F", // 12 indigo
            "#5E35B1", // 13 deep purple
            "#8E24AA", // 14 purple
            "#D81B60", // 15 pink
            "#5D4037", // 16 brown
            "#303030", // 17 dark grey
            "#757575", // 18 grey
            "#aaaaaa", // 19 light grey
            "#B71C1C", // 20 crimson
            "#AD1457", // 21 rose
            "#C43D3D", // 22 coral
            "#BF360C", // 23 rust
            "#A65D16", // 24 copper
            "#886900", // 25 ochre
            "#6C7214", // 26 olive
            "#557A30", // 27 moss
            "#1B5E20", // 28 forest
            "#006B44", // 29 emerald
            "#00695C", // 30 lagoon
            "#006978", // 31 ocean
            "#156082", // 32 denim
            "#0D47A1", // 33 royal blue
            "#3949AB", // 34 iris
            "#6A1B9A", // 35 violet
            "#7B1F72", // 36 plum
            "#9E3E68", // 37 mulberry
            "#795548", // 38 cocoa
            "#455A64" // 39 slate
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
