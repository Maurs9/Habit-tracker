package org.isoron.uhabits.activities.settings

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.BaseAndroidTest
import org.isoron.uhabits.R
import org.isoron.uhabits.tasks.ImportDataTask
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class BackupViewModelTest : BaseAndroidTest() {
    @Test
    fun successfulImportWithRefreshFailureShowsWarningAndAttemptsRemainingRefreshes() = onMain { model ->
        val attempted = mutableListOf<String>()
        val actions: List<Pair<String, () -> Unit>> = listOf(
            "habit list" to {
                attempted += "habit list"
                throw IOException("Refresh failed")
            },
            "widgets" to { attempted += "widgets" },
            "reminders" to { attempted += "reminders" }
        )
        model.completeImport(ImportDataTask.SUCCESS, actions)
        assertEquals(listOf("habit list", "widgets", "reminders"), attempted)
        assertEquals(
            BackupViewModel.State.ImportedWithWarning(R.string.backup_import_refresh_failed),
            model.state.value
        )
        model.consumeMessage()
        assertEquals(BackupViewModel.State.Idle, model.state.value)
    }

    @Test
    fun successfulImportAndRefreshShowsSuccess() = onMain { model ->
        model.completeImport(ImportDataTask.SUCCESS, listOf("habit list" to {}))
        assertEquals(BackupViewModel.State.Message(R.string.habits_imported, false), model.state.value)
    }

    @Test
    fun failedImportIsNotReportedAsImportedWithWarning() = onMain { model ->
        model.completeImport(
            ImportDataTask.FAILED,
            listOf("habit list" to { throw IOException("Refresh failed") })
        )
        assertEquals(BackupViewModel.State.Message(R.string.backup_import_failed, true), model.state.value)
    }

    @Test
    fun fatalRefreshErrorIsNotConvertedToARecoverableWarning() = onMain { model ->
        var propagated = false
        try {
            model.completeImport(
                ImportDataTask.SUCCESS,
                listOf("habit list" to { throw FatalRefreshError() })
            )
        } catch (_: FatalRefreshError) {
            propagated = true
        }
        assertTrue(propagated)
        assertEquals(BackupViewModel.State.Idle, model.state.value)
    }

    private fun onMain(test: (BackupViewModel) -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            test(BackupViewModel(targetContext.applicationContext as Application))
        }
    }

    private class FatalRefreshError : Error()
}
