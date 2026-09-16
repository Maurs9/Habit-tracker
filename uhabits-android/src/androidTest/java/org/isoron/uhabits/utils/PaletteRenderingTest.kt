package org.isoron.uhabits.utils

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils.calculateContrast
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.core.ui.views.PureBlackTheme
import org.isoron.uhabits.core.ui.views.WidgetTheme
import org.isoron.uhabits.notifications.AndroidNotificationTray
import org.isoron.uhabits.notifications.RingtoneManager
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaletteRenderingTest : BaseAndroidTest() {
    @Test
    fun testSharedForegroundMeetsContrastForEveryPaletteFill() {
        for (theme in listOf(LightTheme(), DarkTheme(), PureBlackTheme(), WidgetTheme())) {
            for (index in 0 until PaletteColor.COUNT) {
                val fill = theme.color(index).toInt()
                val text = ColorUtils.contrastingTextColor(fill)
                assertEquals(if (theme is LightTheme) Color.WHITE else Color.rgb(33, 33, 33), text)
                assertTrue(calculateContrast(text, fill) >= 4.5)
            }
        }
    }

    @Test
    fun testAndroidGreyResourcesMatchCoreAndMeetContrast() {
        val styles = listOf(
            R.style.AppBaseTheme to LightTheme(),
            R.style.AppBaseThemeDark to DarkTheme(),
            R.style.AppBaseThemeDark_PureBlack to PureBlackTheme()
        )
        for ((style, theme) in styles) {
            setTheme(style)
            val resources = StyledResources(targetContext)
            val card = resources.getColor(R.attr.cardBgColor)
            val header = resources.getColor(R.attr.headerBackgroundColor)
            val text = resources.getColor(R.attr.contrast60)
            val mark = resources.getColor(R.attr.contrast40)
            assertEquals(theme.cardBackgroundColor.toInt(), card)
            assertEquals(theme.headerBackgroundColor.toInt(), header)
            assertEquals(theme.mediumContrastTextColor.toInt(), text)
            assertEquals(theme.inactiveMarkColor.toInt(), mark)
            assertTrue(calculateContrast(text, card) >= 4.5)
            assertTrue(calculateContrast(text, header) >= 4.5)
            assertTrue(calculateContrast(mark, card) >= 3.0)
        }
    }

    @Test
    fun testNonHabitToolbarUsesPrimaryWhileHabitToolbarKeepsItsColor() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
                val root = View(activity)
                for ((style, theme) in listOf(
                    R.style.AppBaseTheme to LightTheme(),
                    R.style.AppBaseThemeDark to DarkTheme(),
                    R.style.AppBaseThemeDark_PureBlack to PureBlackTheme()
                )) {
                    setTheme(style)
                    activity.setTheme(style)
                    val primary = StyledResources(activity).getColor(R.attr.colorPrimary)
                    root.setupToolbar(toolbar, "App", theme, displayHomeAsUpEnabled = false)
                    assertEquals(primary, (toolbar.background as ColorDrawable).color)
                    assertEquals(0, activity.supportActionBar!!.displayOptions and ActionBar.DISPLAY_HOME_AS_UP)
                    root.setupToolbar(toolbar, "Habit", PaletteColor(17), theme)
                    val expected = if (theme is LightTheme) theme.color(17).toInt() else primary
                    assertEquals(expected, (toolbar.background as ColorDrawable).color)
                    assertTrue(activity.supportActionBar!!.displayOptions and ActionBar.DISPLAY_HOME_AS_UP != 0)
                }
            }
        }
    }

    @Test
    fun testNotificationTintAlwaysUsesLightPalette() {
        val habit = fixtures.createEmptyHabit()
        val tray = AndroidNotificationTray(
            targetContext,
            appComponent.pendingIntentFactory,
            prefs,
            RingtoneManager(targetContext)
        )
        for (style in listOf(R.style.AppBaseTheme, R.style.AppBaseThemeDark, R.style.AppBaseThemeDark_PureBlack)) {
            setTheme(style)
            for (index in 0 until PaletteColor.COUNT) {
                habit.color = PaletteColor(index)
                val notification = tray.buildNotification(habit, 0, day(0), disableSound = true)
                assertEquals(LightTheme().color(index).toInt(), notification.color)
            }
        }
    }
}
