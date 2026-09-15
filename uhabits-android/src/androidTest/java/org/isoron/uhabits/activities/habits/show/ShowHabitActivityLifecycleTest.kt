package org.isoron.uhabits.activities.habits.show

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.intents.IntentFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ShowHabitActivityLifecycleTest : BaseAndroidTest() {
    @Test
    fun testDestroyCancelsActivityScope() {
        val habit = fixtures.createEmptyHabit()
        val intent = IntentFactory().startShowHabitActivity(targetContext, habit)
        lateinit var job: Job
        ActivityScenario.launch<ShowHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val field = ShowHabitActivity::class.java.getDeclaredField("scope")
                field.isAccessible = true
                val scope = field.get(activity) as CoroutineScope
                job = scope.coroutineContext[Job]!!
                assertTrue(job.isActive)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertTrue(job.isCancelled)
        }
    }
}
