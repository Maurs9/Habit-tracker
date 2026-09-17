package org.isoron.uhabits.activities

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.automation.EditSettingActivity
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.tasks.StartupCoordinator
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@MediumTest
class StartupActivityTest : BaseAndroidTest() {
    private class QueueExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }

    @Test
    fun loadingPreventsPrematureContentAndCompletesAfterResume() = withStartup { worker ->
        ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNull(activity.findViewById<View>(R.id.habitSpinner))
                assertEquals(
                    activity.getString(R.string.startup_loading),
                    activity.findViewById<TextView>(R.id.startup_message).text.toString()
                )
            }
            worker.runAll()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<View>(R.id.habitSpinner))
                assertNull(activity.findViewById<View>(R.id.startup_message))
            }
        }
    }

    @Test
    fun recreationWhileLoadingReconnectsToTheSameInitialization() = withStartup { worker ->
        ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { assertNotNull(it.findViewById<View>(R.id.startup_message)) }
            worker.runAll()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertNotNull(it.findViewById<View>(R.id.habitSpinner)) }
        }
    }

    @Test
    fun freshEditorKeepsCreationDefaultsAfterLoadingAndRotation() = withStartup { worker ->
        val intent = Intent(targetContext, EditHabitActivity::class.java)
            .putExtra("habitType", HabitType.NUMERICAL.value)
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.recreate()
            worker.runAll()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(-1L, activity.habitId)
                assertEquals(HabitType.NUMERICAL, activity.habitType)
                assertEquals(1, activity.freqNum)
                assertEquals(1, activity.freqDen)
                assertEquals(-1, activity.reminderHour)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.typeOuterBox).visibility)
                activity.findViewById<EditText>(R.id.nameInput).setText("Read")
                activity.findViewById<EditText>(R.id.targetInput).setText("1")
                activity.findViewById<View>(R.id.buttonSave).performClick()
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun existingEditorDraftSurvivesLoadingRecreation() {
        val habit = habitList.getByPosition(0)
        val intent = Intent(targetContext, EditHabitActivity::class.java).putExtra("habitId", requireNotNull(habit.id))
        ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.nameInput).setText("Pending name")
                activity.freqNum = 3
                activity.freqDen = 7
            }
            withStartup { worker ->
                scenario.recreate()
                worker.runAll()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals(habit.id, activity.habitId)
                    assertEquals("Pending name", activity.findViewById<EditText>(R.id.nameInput).text.toString())
                    assertEquals(3, activity.freqNum)
                    assertEquals(7, activity.freqDen)
                }
            }
        }
    }

    @Test
    fun restoredFragmentsWaitUntilHistoryIsReady() {
        ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.supportFragmentManager.beginTransaction().add(Fragment(), "startup-probe").commitNow()
            }
            withStartup { worker ->
                scenario.recreate()
                scenario.onActivity {
                    assertNull(it.supportFragmentManager.findFragmentByTag("startup-probe"))
                    assertNotNull(it.findViewById<View>(R.id.startup_message))
                }
                worker.runAll()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity {
                    assertNotNull(it.supportFragmentManager.findFragmentByTag("startup-probe"))
                    assertNotNull(it.findViewById<View>(R.id.habitSpinner))
                }
            }
        }
    }

    @Test
    fun closedActivityDoesNotCreateContentAfterInitialization() = withStartup { worker ->
        lateinit var activity: EditSettingActivity
        ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
            scenario.onActivity { activity = it }
        }
        worker.runAll()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue(activity.isDestroyed)
            assertNull(activity.findViewById<View>(R.id.habitSpinner))
        }
    }

    @Test
    fun failedInitializationShowsRecoveryAndRetryCompletes() {
        var attempts = 0
        withStartup({ if (attempts++ == 0) throw IllegalStateException("history unavailable") }) { worker ->
            ActivityScenario.launch(EditSettingActivity::class.java).use { scenario ->
                worker.runAll()
                scenario.onActivity { activity ->
                    assertEquals(
                        activity.getString(R.string.startup_failed),
                        activity.findViewById<TextView>(R.id.startup_message).text.toString()
                    )
                    activity.findViewById<View>(R.id.startup_retry).performClick()
                }
                worker.runAll()
                scenario.onActivity { assertNotNull(it.findViewById<View>(R.id.habitSpinner)) }
                assertEquals(2, attempts)
            }
        }
    }

    private fun withStartup(initialize: () -> Unit = {}, block: (QueueExecutor) -> Unit) {
        val app = targetContext.applicationContext as HabitsApplication
        val original = app.startup
        val worker = QueueExecutor()
        val callbacks = Executor {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                it.run()
            } else {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(it)
            }
        }
        val startup = StartupCoordinator(worker, callbacks, initialize)
        val field = HabitsApplication::class.java.getDeclaredField("startup").apply { isAccessible = true }
        field.set(app, startup)
        try {
            startup.start()
            block(worker)
        } finally {
            field.set(app, original)
        }
    }
}
