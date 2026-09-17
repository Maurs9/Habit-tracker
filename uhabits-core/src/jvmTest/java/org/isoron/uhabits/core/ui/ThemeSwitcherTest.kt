package org.isoron.uhabits.core.ui

import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.Theme
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThemeSwitcherTest {
    @Test
    fun resolvesExplicitAndAutomaticThemesWithoutApplyingStyles() {
        for (user in listOf(ThemeSwitcher.THEME_AUTOMATIC, ThemeSwitcher.THEME_LIGHT, ThemeSwitcher.THEME_DARK)) {
            for (system in listOf(ThemeSwitcher.THEME_LIGHT, ThemeSwitcher.THEME_DARK)) {
                for (black in listOf(false, true)) {
                    val preferences: Preferences = mock {
                        on { theme } doReturn user
                        on { isPureBlackEnabled } doReturn black
                    }
                    val switcher = TestSwitcher(preferences, system)
                    val night = user == ThemeSwitcher.THEME_DARK ||
                        user == ThemeSwitcher.THEME_AUTOMATIC && system == ThemeSwitcher.THEME_DARK
                    val expected = when {
                        !night -> ThemeSwitcher.Variant.LIGHT
                        black -> ThemeSwitcher.Variant.PURE_BLACK
                        else -> ThemeSwitcher.Variant.DARK
                    }
                    assertEquals(expected, switcher.themeVariant)
                    val resolved = switcher.resolveTheme()
                    assertNull(switcher.currentTheme)
                    switcher.apply()
                    assertEquals(resolved.javaClass, switcher.currentTheme!!.javaClass)
                }
            }
        }
    }

    private class TestSwitcher(preferences: Preferences, private val system: Int) : ThemeSwitcher(preferences) {
        override var currentTheme: Theme? = null
        override fun getSystemTheme() = system
        override fun applyDarkTheme() {
            currentTheme = DarkTheme()
        }
        override fun applyLightTheme() {
            currentTheme = LightTheme()
        }
        override fun applyPureBlackTheme() {
            currentTheme = PureBlackTheme()
        }
    }
}
