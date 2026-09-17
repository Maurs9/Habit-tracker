package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseViewTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.core.preferences.Preferences
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonPanelReuseTest : BaseViewTest() {
    @Test
    fun sameCountRebindsAllocateNoControlsAndKeepEveryChild() {
        prefs.isCheckmarkSequenceReversed = false
        val panel = CountingPanel(targetContext, prefs)
        panel.buttonCount = 5
        val controls = panel.buttons.toList()
        val initialBindings = panel.bindings
        repeat(100) { panel.buttonCount = 5 }
        assertEquals(5, panel.allocations)
        assertEquals(initialBindings + 100, panel.bindings)
        controls.forEachIndexed { index, view ->
            assertSame(view, panel.buttons[index])
            assertSame(view, panel.getChildAt(index))
        }
    }

    @Test
    fun sameCountRebindKeepsKeyboardFocusOnTheSameControl() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(false)
        try {
            ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
                scenario.onActivity { activity ->
                    val panel = CountingPanel(activity, prefs).apply { buttonCount = 3 }
                    activity.setContentView(panel)
                    measureView(panel, dpToPixels(144), dpToPixels(48))
                    val focused = panel.buttons[1]
                    assertTrue(focused.requestFocus())
                    repeat(100) { panel.buttonCount = 3 }
                    assertSame(focused, activity.currentFocus)
                    assertEquals(3, panel.allocations)
                }
            }
        } finally {
            instrumentation.setInTouchMode(true)
        }
    }

    @Test
    fun growthShrinkReversalAndDensityReuseSurvivingControls() {
        prefs.isCheckmarkSequenceReversed = false
        val panel = CountingPanel(targetContext, prefs)
        panel.buttonCount = 3
        val first = panel.buttons.toList()
        panel.buttonCount = 5
        assertEquals(5, panel.allocations)
        first.forEachIndexed { index, view -> assertSame(view, panel.buttons[index]) }
        panel.buttonCount = 2
        assertEquals(2, panel.childCount)
        assertEquals(5, panel.allocations)
        prefs.isCheckmarkSequenceReversed = true
        panel.onCheckmarkSequenceChanged()
        assertSame(first[1], panel.getChildAt(0))
        assertSame(first[0], panel.getChildAt(1))
        assertEquals(5, panel.allocations)
        panel.buttonHeight = dpToPixels(64).toInt()
        measureView(panel, dpToPixels(96), dpToPixels(64))
        assertEquals(dpToPixels(64).toInt(), panel.height)
        assertSame(first[0], panel.buttons[0])
        panel.buttonCount = 0
        assertEquals(0, panel.childCount)
        panel.buttonCount = 2
        assertEquals(7, panel.allocations)
    }

    private class CountingPanel(context: Context, preferences: Preferences) : ButtonPanelView<View>(context, preferences) {
        var allocations = 0
        var bindings = 0
        override fun createButton() = View(context).also {
            it.isFocusable = true
            it.minimumWidth = context.resources.getDimensionPixelSize(R.dimen.checkmarkWidth)
            allocations++
        }
        override fun setupButtons() {
            bindings++
        }
    }
}
