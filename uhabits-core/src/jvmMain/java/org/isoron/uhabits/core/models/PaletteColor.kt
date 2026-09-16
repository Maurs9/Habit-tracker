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
            // Sector 0: Red
            "#C6110D", //  0 Deep Red
            "#E12D2E", //  1 Vibrant Red
            "#E85A58", //  2 Soft Red
            "#F18A8B", //  3 Light Red
            // Sector 1: Orange
            "#D65E15", //  4 Deep Orange
            "#EB7423", //  5 Vibrant Orange
            "#F09452", //  6 Soft Orange
            "#F7B481", //  7 Light Orange
            // Sector 2: Yellow / Gold
            "#C29B00", //  8 Deep Amber
            "#DBB40E", //  9 Vibrant Amber
            "#E5CA45", // 10 Soft Amber
            "#EFE080", // 11 Light Amber
            // Sector 3: Lime
            "#7EA810", // 12 Deep Lime
            "#97CC18", // 13 Vibrant Lime
            "#B0DC42", // 14 Soft Lime
            "#CBEE72", // 15 Light Lime
            // Sector 4: Bright Green
            "#289B0C", // 16 Deep Green
            "#3CCC12", // 17 Vibrant Green
            "#64D942", // 18 Soft Green
            "#8FE678", // 19 Light Green
            // Sector 5: Forest / Emerald
            "#1B7528", // 20 Deep Emerald
            "#2EA53A", // 21 Vibrant Emerald
            "#4DC25B", // 22 Soft Emerald
            "#77DE84", // 23 Light Emerald
            // Sector 6: Teal / Mint
            "#0E9656", // 24 Deep Teal
            "#14C471", // 25 Vibrant Teal
            "#44D994", // 26 Soft Teal
            "#78ECB7", // 27 Light Teal
            // Sector 7: Cyan / Turquoise
            "#0C8B8E", // 28 Deep Cyan
            "#10B8BB", // 29 Vibrant Cyan
            "#3FD1D4", // 30 Soft Cyan
            "#7AE3E5", // 31 Light Cyan
            // Sector 8: Blue
            "#12549C", // 32 Deep Blue
            "#1976D2", // 33 Vibrant Blue
            "#4797E6", // 34 Soft Blue
            "#7EB8F2", // 35 Light Blue
            // Sector 9: Indigo
            "#310E94", // 36 Deep Indigo
            "#4D1BC7", // 37 Vibrant Indigo
            "#7346DC", // 38 Soft Indigo
            "#9E7CF0", // 39 Light Indigo
            // Sector 10: Purple / Violet
            "#680C8C", // 40 Deep Purple
            "#8C19B8", // 41 Vibrant Purple
            "#AF42D8", // 42 Soft Purple
            "#CF76F0", // 43 Light Purple
            // Sector 11: Magenta / Pink
            "#B80056", // 44 Deep Magenta
            "#E2006F", // 45 Vibrant Magenta
            "#EC3E94", // 46 Soft Magenta
            "#F47DB8", // 47 Light Magenta
            // Center Neutrals / Greys
            "#9E9E9E", // 48 Light Gray
            "#616161", // 49 Medium Gray
            "#455A64", // 50 Slate
            "#212121"  // 51 Charcoal
        )[paletteIndex]
    }

    fun compareTo(other: PaletteColor): Int {
        return paletteIndex.compareTo(other.paletteIndex)
    }

    companion object {
        // Persisted slots are append-only: values may be tuned, but never reorder indexes.
        const val COUNT = 52
        val DEFAULT = PaletteColor(33)
    }
}
