package org.isoron.uhabits.activities.habits.list

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.activities.habits.list.views.HabitCardView
import org.isoron.uhabits.activities.habits.list.views.SectionHeaderView
import org.isoron.uhabits.core.preferences.ListDensity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ListDensityIntegrationTest : BaseViewTest() {
    @Test
    fun testSettingRebindsRowsAndHeadersAndSurvivesRecreation() {
        prefs.isFirstRun = false
        prefs.groupBySection = true
        val section = appComponent.sectionList.add("Morning")
        val habit = habitList.getByPosition(0).apply {
            name = "Read"
            sectionId = section.id
        }
        habitList.update(habit)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // This activity consumes its launch intent, which breaks ActivityScenario's intent matching.
        var activity = instrumentation.startActivitySync(
            Intent(targetContext, ListHabitsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ) as ListHabitsActivity
        try {
            for (density in ListDensity.entries + ListDensity.STANDARD) {
                instrumentation.runOnMainSync { prefs.listDensity = density }
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    val list = activity.rootView.listView
                    val header = checkNotNull(list.findViewHolderForAdapterPosition(0)).itemView as SectionHeaderView
                    val card = checkNotNull(list.findViewHolderForAdapterPosition(1)).itemView as HabitCardView
                    assertEquals(density, header.listDensity)
                    assertEquals(dpToPixels(density.firstSectionTopDp).toInt(), header.paddingTop)
                    assertEquals(density, card.listDensity)
                    assertEquals(dpToPixels(density.rowHeightDp).toInt(), card.checkmarkPanel.height)
                    assertSame(habit, card.habit)
                }
            }
            instrumentation.runOnMainSync { prefs.listDensity = ListDensity.SPACIOUS }
            val monitor = instrumentation.addMonitor(ListHabitsActivity::class.java.name, null, false)
            try {
                instrumentation.runOnMainSync { activity.recreate() }
                activity = checkNotNull(instrumentation.waitForMonitorWithTimeout(monitor, 5000)) as ListHabitsActivity
            } finally {
                instrumentation.removeMonitor(monitor)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val card = checkNotNull(activity.rootView.listView.findViewHolderForAdapterPosition(1)).itemView as HabitCardView
                assertEquals(ListDensity.SPACIOUS, card.listDensity)
                activity.adapter.groupBySection = false
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val card = checkNotNull(activity.rootView.listView.findViewHolderForAdapterPosition(0)).itemView as HabitCardView
                assertEquals(ListDensity.SPACIOUS, card.listDensity)
                assertEquals(dpToPixels(64).toInt(), card.checkmarkPanel.height)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}
