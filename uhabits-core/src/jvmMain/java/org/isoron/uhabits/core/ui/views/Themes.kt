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
            0 -> Color(0xD32F2F)
            1 -> Color(0xA65050)
            2 -> Color(0xB71C1C)
            3 -> Color(0xC43E12)
            4 -> Color(0xA6563D)
            5 -> Color(0x8F2809)
            6 -> Color(0xAE5800)
            7 -> Color(0x96602A)
            8 -> Color(0x8A3300)
            9 -> Color(0x9C6800)
            10 -> Color(0x8C6A2E)
            11 -> Color(0x7A4A00)
            12 -> Color(0x827000)
            13 -> Color(0x7C7038)
            14 -> Color(0x665500)
            15 -> Color(0x657400)
            16 -> Color(0x6A6B45)
            17 -> Color(0x4A4D00)
            18 -> Color(0x2E7D32)
            19 -> Color(0x4A764D)
            20 -> Color(0x1B5E20)
            21 -> Color(0x00796B)
            22 -> Color(0x34766F)
            23 -> Color(0x004D40)
            24 -> Color(0x007B88)
            25 -> Color(0x3A7078)
            26 -> Color(0x006064)
            27 -> Color(0x1565C0)
            28 -> Color(0x3E6FA0)
            29 -> Color(0x0D47A1)
            30 -> Color(0x7B1FA2)
            31 -> Color(0x7D4894)
            32 -> Color(0x4A148C)
            33 -> Color(0xC2185B)
            34 -> Color(0xA6446B)
            35 -> Color(0x880E4F)
            36 -> Color(0x6E6E6E)
            37 -> Color(0x4F4F4F)
            38 -> Color(0x455A64)
            39 -> Color(0x212121)
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
            0 -> Color(0xFF8A80)
            1 -> Color(0xD8AAAA)
            2 -> Color(0xFF6B6B)
            3 -> Color(0xFF8A65)
            4 -> Color(0xE1B3A5)
            5 -> Color(0xFF7043)
            6 -> Color(0xFFB74D)
            7 -> Color(0xE1C59B)
            8 -> Color(0xFB8C00)
            9 -> Color(0xFFD54F)
            10 -> Color(0xE1D09D)
            11 -> Color(0xFFB300)
            12 -> Color(0xFFF176)
            13 -> Color(0xE1DCAB)
            14 -> Color(0xE6D200)
            15 -> Color(0xDCE775)
            16 -> Color(0xD4D8AB)
            17 -> Color(0xAFB42B)
            18 -> Color(0x81C784)
            19 -> Color(0xB0CBB1)
            20 -> Color(0x4CAF50)
            21 -> Color(0x64C8BC)
            22 -> Color(0x9BC5C1)
            23 -> Color(0x4DB6AC)
            24 -> Color(0x4DD0E1)
            25 -> Color(0x9BCFD6)
            26 -> Color(0x26C6DA)
            27 -> Color(0x64B5F6)
            28 -> Color(0xA4C4DE)
            29 -> Color(0x42A5F5)
            30 -> Color(0xEA80FC)
            31 -> Color(0xC8A9CE)
            32 -> Color(0xC682D1)
            33 -> Color(0xFF80AB)
            34 -> Color(0xDBA4B6)
            35 -> Color(0xF1709B)
            36 -> Color(0x9E9E9E)
            37 -> Color(0xBDBDBD)
            38 -> Color(0x90A4AE)
            39 -> Color(0xEEEEEE)
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
