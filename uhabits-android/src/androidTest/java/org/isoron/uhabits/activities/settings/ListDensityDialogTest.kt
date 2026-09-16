package org.isoron.uhabits.activities.settings

import android.content.Intent
import android.widget.RadioButton
import androidx.preference.Preference
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.ListDensity
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListDensityDialogTest : BaseAndroidTest() {
    @Test
    fun testSaveCancelRotationAndPreferenceSummary() {
        ActivityScenario.launch<SettingsActivity>(Intent(targetContext, SettingsActivity::class.java)).use { scenario ->
            openSelector(scenario)
            onView(withId(R.id.list_density_compact)).inRoot(isDialog()).perform(scrollTo(), click())
            assertEquals(ListDensity.STANDARD, prefs.listDensity)
            scenario.recreate()
            scenario.onActivity { activity ->
                val dialog = activity.supportFragmentManager.findFragmentByTag(ListDensityDialog.TAG) as ListDensityDialog
                assertTrue(dialog.requireDialog().findViewById<RadioButton>(R.id.list_density_compact).isChecked)
            }
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            assertEquals(ListDensity.COMPACT, prefs.listDensity)
            scenario.onActivity { activity ->
                val settings = activity.supportFragmentManager.fragments.filterIsInstance<SettingsFragment>().single()
                assertEquals(
                    activity.getString(R.string.list_density_compact),
                    settings.findPreference<Preference>("pref_list_density")!!.summary
                )
            }
            openSelector(scenario)
            onView(withId(R.id.list_density_spacious)).inRoot(isDialog()).perform(scrollTo(), click())
            onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
            assertEquals(ListDensity.COMPACT, prefs.listDensity)
            openSelector(scenario)
            onView(withId(R.id.list_density_spacious)).inRoot(isDialog()).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            assertEquals(ListDensity.SPACIOUS, prefs.listDensity)
            scenario.recreate()
            openSelector(scenario)
            scenario.onActivity { activity ->
                val dialog = activity.supportFragmentManager.findFragmentByTag(ListDensityDialog.TAG) as ListDensityDialog
                assertTrue(dialog.requireDialog().findViewById<RadioButton>(R.id.list_density_spacious).isChecked)
            }
        }
    }

    @Test
    fun testSelectorInDarkAndPureBlackThemes() {
        for (pureBlack in listOf(false, true)) {
            prefs.theme = ThemeSwitcher.THEME_DARK
            prefs.isPureBlackEnabled = pureBlack
            setTheme(if (pureBlack) R.style.AppBaseThemeDark_PureBlack else R.style.AppBaseThemeDark)
            ActivityScenario.launch<SettingsActivity>(Intent(targetContext, SettingsActivity::class.java)).use { scenario ->
                openSelector(scenario)
                for (id in listOf(R.id.list_density_spacious, R.id.list_density_compact, R.id.list_density_standard)) {
                    onView(withId(id)).inRoot(isDialog()).perform(scrollTo(), click())
                }
                onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
            }
        }
    }

    private fun openSelector(scenario: ActivityScenario<SettingsActivity>) {
        scenario.onActivity { activity ->
            val settings = activity.supportFragmentManager.fragments.filterIsInstance<SettingsFragment>().single()
            val preference = checkNotNull(settings.findPreference<Preference>("pref_list_density"))
            assertTrue(settings.onPreferenceTreeClick(preference))
            activity.supportFragmentManager.executePendingTransactions()
        }
    }
}
