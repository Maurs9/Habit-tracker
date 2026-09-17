package org.isoron.uhabits.activities.habits.list

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.HabitCardView
import org.isoron.uhabits.activities.settings.SettingsActivity
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.utils.StyledResources
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ThemeSwitchingTest : BaseAndroidTest() {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun pureBlackChangesRefreshExistingAndRecycledRowsInAutomaticAndExplicitDarkMode() {
        val originalNightMode = AppCompatDelegate.getDefaultNightMode()
        StyledResources.setFixedTheme(null)
        prefs.isFirstRun = false
        repeat(30) { index ->
            val habit = fixtures.createEmptyHabit().apply {
                if (index % 2 == 0) {
                    type = HabitType.NUMERICAL
                    targetValue = 10.0
                    unit = "ml"
                }
            }
            habitList.update(habit)
        }
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        try {
            for (theme in listOf(ThemeSwitcher.THEME_AUTOMATIC, ThemeSwitcher.THEME_DARK)) {
                prefs.theme = theme
                prefs.isPureBlackEnabled = false
                var activity = instrumentation.startActivitySync(
                    Intent(targetContext, ListHabitsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ) as ListHabitsActivity
                var settings: SettingsActivity? = null
                try {
                    instrumentation.waitForIdleSync()
                    assertCardBackgrounds(activity, Color.rgb(48, 48, 48))
                    for (black in listOf(true, false)) {
                        settings = awaitActivity<SettingsActivity> {
                            activity.startActivity(Intent(activity, SettingsActivity::class.java))
                        }
                        settings = awaitActivity<SettingsActivity>(settings) {
                            prefs.isPureBlackEnabled = black
                        }
                        val currentSettings = checkNotNull(settings)
                        activity = awaitActivity<ListHabitsActivity>(activity) { currentSettings.finish() }
                        settings = null
                        instrumentation.waitForIdleSync()
                        val expected = if (black) Color.BLACK else Color.rgb(48, 48, 48)
                        assertCardBackgrounds(activity, expected)
                        instrumentation.runOnMainSync {
                            activity.rootView.listView.scrollToPosition(activity.adapter.itemCount - 1)
                        }
                        instrumentation.waitForIdleSync()
                        assertCardBackgrounds(activity, expected)
                    }
                } finally {
                    instrumentation.runOnMainSync {
                        settings?.finish()
                        activity.finish()
                    }
                    instrumentation.waitForIdleSync()
                }
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(originalNightMode) }
            setTheme(R.style.AppBaseTheme)
        }
    }

    private inline fun <reified T : Activity> awaitActivity(
        excluded: Activity? = null,
        crossinline action: () -> Unit
    ): T {
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync { action() }
        val deadline = SystemClock.uptimeMillis() + 10000
        do {
            instrumentation.waitForIdleSync()
            var resumed: T? = null
            instrumentation.runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<T>()
                    .singleOrNull { it !== excluded && !it.isFinishing && !it.isDestroyed }
            }
            resumed?.let { return it }
            check(SystemClock.uptimeMillis() < deadline) {
                "Timed out waiting for a new resumed ${T::class.java.simpleName}"
            }
            Thread.sleep(20)
        } while (true)
    }

    private fun assertCardBackgrounds(activity: ListHabitsActivity, expected: Int) {
        instrumentation.runOnMainSync {
            val list = activity.rootView.listView
            assertEquals(expected, StyledResources(activity).getColor(R.attr.cardBgColor))
            val cards = (0 until list.childCount).map { list.getChildAt(it) }.filterIsInstance<HabitCardView>()
            assertTrue(cards.isNotEmpty())
            cards.forEach { card ->
                val background = card.getChildAt(0).background as RippleDrawable
                assertEquals(expected, (background.getDrawable(0) as ColorDrawable).color)
            }
        }
    }
}
