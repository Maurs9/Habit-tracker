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

package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.models.PaletteColor

abstract class Theme {
    open val appBackgroundColor = Color(0xf4f4f4)
    open val cardBackgroundColor = Color(0xFAFAFA)
    open val headerBackgroundColor = Color(0xeeeeee)
    open val headerBorderColor = Color(0xcccccc)
    open val headerTextColor = Color(0x666666)
    open val highContrastTextColor = Color(0x202020)
    open val inactiveMarkColor = Color(0x8A8A8A)
    open val itemBackgroundColor = Color(0xffffff)
    open val lowContrastTextColor = Color(0xe0e0e0)
    open val mediumContrastTextColor = Color(0x666666)
    open val statusBarBackgroundColor = Color(0x333333)
    open val toolbarBackgroundColor = Color(0xf4f4f4)
    open val toolbarColor = Color(0xffffff)

    fun color(paletteColor: PaletteColor): Color {
        return color(paletteColor.paletteIndex)
    }

    open fun color(paletteIndex: Int): Color {
        return when (paletteIndex) {
            // Sector 0: Red
            0 -> Color(0xC6110D)
            1 -> Color(0xE12D2E)
            2 -> Color(0xE85A58)
            3 -> Color(0xF18A8B)
            // Sector 1: Orange
            4 -> Color(0xD65E15)
            5 -> Color(0xEB7423)
            6 -> Color(0xF09452)
            7 -> Color(0xF7B481)
            // Sector 2: Yellow / Gold
            8 -> Color(0xC29B00)
            9 -> Color(0xDBB40E)
            10 -> Color(0xE5CA45)
            11 -> Color(0xEFE080)
            // Sector 3: Lime
            12 -> Color(0x7EA810)
            13 -> Color(0x97CC18)
            14 -> Color(0xB0DC42)
            15 -> Color(0xCBEE72)
            // Sector 4: Bright Green
            16 -> Color(0x289B0C)
            17 -> Color(0x3CCC12)
            18 -> Color(0x64D942)
            19 -> Color(0x8FE678)
            // Sector 5: Forest / Emerald
            20 -> Color(0x1B7528)
            21 -> Color(0x2EA53A)
            22 -> Color(0x4DC25B)
            23 -> Color(0x77DE84)
            // Sector 6: Teal / Mint
            24 -> Color(0x0E9656)
            25 -> Color(0x14C471)
            26 -> Color(0x44D994)
            27 -> Color(0x78ECB7)
            // Sector 7: Cyan / Turquoise
            28 -> Color(0x0C8B8E)
            29 -> Color(0x10B8BB)
            30 -> Color(0x3FD1D4)
            31 -> Color(0x7AE3E5)
            // Sector 8: Blue
            32 -> Color(0x12549C)
            33 -> Color(0x1976D2)
            34 -> Color(0x4797E6)
            35 -> Color(0x7EB8F2)
            // Sector 9: Indigo
            36 -> Color(0x310E94)
            37 -> Color(0x4D1BC7)
            38 -> Color(0x7346DC)
            39 -> Color(0x9E7CF0)
            // Sector 10: Purple / Violet
            40 -> Color(0x680C8C)
            41 -> Color(0x8C19B8)
            42 -> Color(0xAF42D8)
            43 -> Color(0xCF76F0)
            // Sector 11: Magenta / Pink
            44 -> Color(0xB80056)
            45 -> Color(0xE2006F)
            46 -> Color(0xEC3E94)
            47 -> Color(0xF47DB8)
            // Center Neutrals / Greys
            48 -> Color(0x9E9E9E)
            49 -> Color(0x616161)
            50 -> Color(0x455A64)
            51 -> Color(0x212121)
            else -> Color(0x000000)
        }
    }

    val checkmarkButtonSize = 48.0
    val smallTextSize = 10.0
    val regularTextSize = 17.0
}

open class LightTheme : Theme()

open class DarkTheme : Theme() {
    override val appBackgroundColor = Color(0x212121)
    override val cardBackgroundColor = Color(0x303030)
    override val headerBackgroundColor = Color(0x212121)
    override val headerBorderColor = Color(0x424242)
    override val headerTextColor = Color(0x9E9E9E)
    override val highContrastTextColor = Color(0xF5F5F5)
    override val inactiveMarkColor = Color(0x808080)
    override val itemBackgroundColor = Color(0x303030)
    override val lowContrastTextColor = Color(0x424242)
    override val mediumContrastTextColor = Color(0x9E9E9E)
    override val statusBarBackgroundColor = Color(0x000000)
    override val toolbarBackgroundColor = Color(0x101010)
    override val toolbarColor = Color(0xffffff)

    override fun color(paletteIndex: Int): Color {
        return when (paletteIndex) {
            // Sector 0: Red
            0 -> Color(0xFF5252)
            1 -> Color(0xFF6E6E)
            2 -> Color(0xFF8A80)
            3 -> Color(0xFFB4AB)
            // Sector 1: Orange
            4 -> Color(0xFF6E40)
            5 -> Color(0xFF8A65)
            6 -> Color(0xFFAB91)
            7 -> Color(0xFFCCBC)
            // Sector 2: Yellow / Gold
            8 -> Color(0xFFD740)
            9 -> Color(0xFFE082)
            10 -> Color(0xFFF176)
            11 -> Color(0xFFF9C4)
            // Sector 3: Lime
            12 -> Color(0xB2FF59)
            13 -> Color(0xC6FF00)
            14 -> Color(0xDCE775)
            15 -> Color(0xE8F5E9)
            // Sector 4: Bright Green
            16 -> Color(0x69F0AE)
            17 -> Color(0x81C784)
            18 -> Color(0xA5D6A7)
            19 -> Color(0xC8E6C9)
            // Sector 5: Forest / Emerald
            20 -> Color(0x00E676)
            21 -> Color(0x4CAF50)
            22 -> Color(0x66BB6A)
            23 -> Color(0x80E27E)
            // Sector 6: Teal / Mint
            24 -> Color(0x1DE9B6)
            25 -> Color(0x4DB6AC)
            26 -> Color(0x80CBC4)
            27 -> Color(0xB2DFDB)
            // Sector 7: Cyan / Turquoise
            28 -> Color(0x00E5FF)
            29 -> Color(0x4DD0E1)
            30 -> Color(0x80DEEA)
            31 -> Color(0xB2EBF2)
            // Sector 8: Blue
            32 -> Color(0x448AFF)
            33 -> Color(0x64B5F6)
            34 -> Color(0x90CAF9)
            35 -> Color(0xBBDEFB)
            // Sector 9: Indigo
            36 -> Color(0x8052FF)
            37 -> Color(0x9575CD)
            38 -> Color(0xB39DDB)
            39 -> Color(0xD1C4E9)
            // Sector 10: Purple / Violet
            40 -> Color(0xE040FB)
            41 -> Color(0xBA68C8)
            42 -> Color(0xCE93D8)
            43 -> Color(0xE1BEE7)
            // Sector 11: Magenta / Pink
            44 -> Color(0xFF4081)
            45 -> Color(0xFF80AB)
            46 -> Color(0xF48FB1)
            47 -> Color(0xF8BBD0)
            // Center Neutrals / Greys
            48 -> Color(0xBDBDBD)
            49 -> Color(0x9E9E9E)
            50 -> Color(0x90A4AE)
            51 -> Color(0xEEEEEE)
            else -> Color(0xFFFFFF)
        }
    }
}

class PureBlackTheme : DarkTheme() {
    override val appBackgroundColor = Color(0x000000)
    override val cardBackgroundColor = Color(0x000000)
    override val headerBackgroundColor = Color(0x000000)
    override val inactiveMarkColor = Color(0x666666)
    override val itemBackgroundColor = Color(0x000000)
    override val lowContrastTextColor = Color(0x212121)
    override val toolbarBackgroundColor = Color(0x000000)
}

class WidgetTheme : DarkTheme() {
    override val cardBackgroundColor = Color.TRANSPARENT
    override val highContrastTextColor = Color.WHITE
    override val inactiveMarkColor = Color.WHITE.withAlpha(0.50)
    override val mediumContrastTextColor = Color.WHITE.withAlpha(0.50)
    override val lowContrastTextColor = Color.WHITE.withAlpha(0.10)
}
