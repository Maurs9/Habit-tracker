package org.isoron.uhabits.activities.habits.list.views

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.core.models.Timestamp
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class EntryAccessibilityTest : BaseAndroidTest() {
    @Test
    fun testDateIsLocalizedWithoutShiftingToThePreviousDay() {
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val configuration = Configuration(targetContext.resources.configuration).apply {
                setLocale(Locale.FRANCE)
            }
            val context = targetContext.createConfigurationContext(configuration)
            val description = context.entryDescription("Lire", Timestamp(1577923200000), "Completed", "")
            assertTrue(description.contains("2 janvier 2020"))
            assertFalse(description.contains("1 janvier"))
        } finally {
            TimeZone.setDefault(previousZone)
        }
    }
}
