package org.isoron.uhabits

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitsBackupAgentTest : BaseAndroidTest() {
    @Test
    fun backupFilenameSelectsTheActualDefaultPreferences() {
        val key = "backup-filename-test-${UUID.randomUUID()}"
        val preferences = PreferenceManager.getDefaultSharedPreferences(targetContext)
        try {
            assertTrue(preferences.edit().putString(key, "saved").commit())
            val backedUp = targetContext.getSharedPreferences(
                HabitsBackupAgent.preferencesFilename(targetContext),
                Context.MODE_PRIVATE
            )
            assertEquals("saved", backedUp.getString(key, null))
        } finally {
            preferences.edit().remove(key).commit()
        }
    }
}
