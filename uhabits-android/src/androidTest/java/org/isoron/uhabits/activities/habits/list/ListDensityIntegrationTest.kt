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
    fun testListAlwaysUsesCompactDensity() {
        prefs.isFirstRun = false
        prefs.groupBySection = true
        val section = appComponent.sectionList.add("Morning")
        val habit = habitList.getByPosition(0).apply {
            name = "Read"
            sectionId = section.id
        }
        habitList.update(habit)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(targetContext, ListHabitsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ) as ListHabitsActivity
        try {
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val list = activity.rootView.listView
                val header = checkNotNull(list.findViewHolderForAdapterPosition(0)).itemView as SectionHeaderView
                val card = checkNotNull(list.findViewHolderForAdapterPosition(1)).itemView as HabitCardView
                assertEquals(ListDensity.COMPACT, header.listDensity)
                assertEquals(dpToPixels(ListDensity.COMPACT.firstSectionTopDp).toInt(), header.paddingTop)
                assertEquals(ListDensity.COMPACT, card.listDensity)
                assertEquals(dpToPixels(ListDensity.COMPACT.rowHeightDp).toInt(), card.checkmarkPanel.height)
                assertSame(habit, card.habit)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }
}
