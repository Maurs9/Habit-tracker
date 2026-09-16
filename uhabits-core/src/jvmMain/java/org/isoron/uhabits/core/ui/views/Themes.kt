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
    open val headerTextColor = Color(0x9E9E9E)
    open val highContrastTextColor = Color(0x202020)
    open val itemBackgroundColor = Color(0xffffff)
    open val lowContrastTextColor = Color(0xe0e0e0)
    open val mediumContrastTextColor = Color(0x9E9E9E)
    open val statusBarBackgroundColor = Color(0x333333)
    open val toolbarBackgroundColor = Color(0xf4f4f4)
    open val toolbarColor = Color(0xffffff)

    fun color(paletteColor: PaletteColor): Color {
        return color(paletteColor.paletteIndex)
    }

    open fun color(paletteIndex: Int): Color {
        return when (paletteIndex) {
            0 -> Color(0xF44336)
            1 -> Color(0xEF9A9A)
            2 -> Color(0xB71C1C)
            3 -> Color(0xFF5722)
            4 -> Color(0xFFAB91)
            5 -> Color(0xBF360C)
            6 -> Color(0xFF9800)
            7 -> Color(0xFFCC80)
            8 -> Color(0xE65100)
            9 -> Color(0xFFC107)
            10 -> Color(0xFFE082)
            11 -> Color(0xFF8F00)
            12 -> Color(0xFDD835)
            13 -> Color(0xFFF59D)
            14 -> Color(0xF57F17)
            15 -> Color(0xC0CA33)
            16 -> Color(0xE6EE9C)
            17 -> Color(0x827717)
            18 -> Color(0x4CAF50)
            19 -> Color(0xA5D6A7)
            20 -> Color(0x1B5E20)
            21 -> Color(0x009688)
            22 -> Color(0x80CBC4)
            23 -> Color(0x004D40)
            24 -> Color(0x00BCD4)
            25 -> Color(0x80DEEA)
            26 -> Color(0x006064)
            27 -> Color(0x2196F3)
            28 -> Color(0x90CAF9)
            29 -> Color(0x0D47A1)
            30 -> Color(0x9C27B0)
            31 -> Color(0xCE93D8)
            32 -> Color(0x4A148C)
            33 -> Color(0xE91E63)
            34 -> Color(0xF48FB1)
            35 -> Color(0x880E4F)
            36 -> Color(0xBDBDBD)
            37 -> Color(0x757575)
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
    override val headerBorderColor = Color(0xcccccc)
    override val headerTextColor = Color(0x9E9E9E)
    override val highContrastTextColor = Color(0xF5F5F5)
    override val itemBackgroundColor = Color(0xffffff)
    override val lowContrastTextColor = Color(0x424242)
    override val mediumContrastTextColor = Color(0x9E9E9E)
    override val statusBarBackgroundColor = Color(0x333333)
    override val toolbarBackgroundColor = Color(0xf4f4f4)
    override val toolbarColor = Color(0xffffff)

    override fun color(paletteIndex: Int): Color {
        return when (paletteIndex) {
            0 -> Color(0xEF5350)
            1 -> Color(0xFFCDD2)
            2 -> Color(0xE57373)
            3 -> Color(0xFF7043)
            4 -> Color(0xFFCCBC)
            5 -> Color(0xFF8A65)
            6 -> Color(0xFFA726)
            7 -> Color(0xFFE0B2)
            8 -> Color(0xFFB74D)
            9 -> Color(0xFFCA28)
            10 -> Color(0xFFECB3)
            11 -> Color(0xFFD54F)
            12 -> Color(0xFFEE58)
            13 -> Color(0xFFF9C4)
            14 -> Color(0xFFF176)
            15 -> Color(0xD4E157)
            16 -> Color(0xF0F4C3)
            17 -> Color(0xDCE775)
            18 -> Color(0x66BB6A)
            19 -> Color(0xC8E6C9)
            20 -> Color(0x81C784)
            21 -> Color(0x26A69A)
            22 -> Color(0xB2DFDB)
            23 -> Color(0x4DB6AC)
            24 -> Color(0x26C6DA)
            25 -> Color(0xB2EBF2)
            26 -> Color(0x4DD0E1)
            27 -> Color(0x42A5F5)
            28 -> Color(0xBBDEFB)
            29 -> Color(0x64B5F6)
            30 -> Color(0xAB47BC)
            31 -> Color(0xE1BEE7)
            32 -> Color(0xBA68C8)
            33 -> Color(0xEC407A)
            34 -> Color(0xF8BBD0)
            35 -> Color(0xF06292)
            36 -> Color(0xF5F5F5)
            37 -> Color(0xBDBDBD)
            38 -> Color(0x78909C)
            39 -> Color(0x424242)
            else -> Color(0xFFFFFF)
        }
    }
}

class PureBlackTheme : DarkTheme() {
    override val appBackgroundColor = Color(0x000000)
    override val cardBackgroundColor = Color(0x000000)
    override val lowContrastTextColor = Color(0x212121)
}

class WidgetTheme : LightTheme() {
    override val cardBackgroundColor = Color.TRANSPARENT
    override val highContrastTextColor = Color.WHITE
    override val mediumContrastTextColor = Color.WHITE.withAlpha(0.50)
    override val lowContrastTextColor = Color.WHITE.withAlpha(0.10)
}
