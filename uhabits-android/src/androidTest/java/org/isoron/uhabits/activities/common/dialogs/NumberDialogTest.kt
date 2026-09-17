package org.isoron.uhabits.activities.common.dialogs

import android.content.Intent
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class NumberDialogTest : BaseAndroidTest() {
    @Test
    fun testSavingAThousandthPreservesItsMeasurementAndNotes() {
        ActivityScenario.launch<EditHabitActivity>(Intent(targetContext, EditHabitActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                var result: Pair<Int, String>? = null
                val fragment = NumberDialog().apply {
                    arguments = EntryDialogFragment.arguments(habitList.getByPosition(0), day(0), android.graphics.Color.BLUE).apply {
                        putDouble("value", 0.001)
                        putString("notes", "Measured")
                    }
                    onToggle = { value, notes -> result = (value * 1000).roundToInt() to notes }
                }
                fragment.showNow(activity.supportFragmentManager, "number")
                val dialog = fragment.requireDialog()
                assertEquals("0.001", dialog.findViewById<EditText>(R.id.value).text.toString())
                fragment.save()
                assertEquals(1 to "Measured", result)
                assertFalse(dialog.isShowing)
            }
        }
    }
}
