package org.isoron.uhabits.activities.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.tasks.ExportCSVTask
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.database.AutoBackup
import org.isoron.uhabits.database.BackupPreview
import org.isoron.uhabits.tasks.ExportDBTask
import org.isoron.uhabits.tasks.ImportDataTask
import org.isoron.uhabits.tasks.ImportPreviewTask
import java.io.File
import java.io.IOException
import java.util.UUID

/** Keeps pending documents and previews across rotation, without retaining an Activity. */
class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val component = (application as HabitsApplication).component
    val state = MutableLiveData<State>(State.Idle)
    private var workDirectory: File? = null
    private var pendingImport: File? = null
    private var pendingExport: File? = null
    private var cleared = false

    sealed class State {
        data object Idle : State()
        data class Busy(val message: Int) : State()
        data class Preview(val details: BackupPreview?) : State()
        data class LocalBackups(val files: List<File>) : State()
        data class ExportReady(val filename: String, val database: Boolean) : State()
        data object ChoosingDestination : State()
        data class Message(val message: Int, val failed: Boolean) : State()
        data class ImportedWithWarning(val message: Int) : State()
    }

    fun showLocalBackups() {
        if (state.value != State.Idle) return
        state.value = State.Busy(R.string.backup_finding)
        runTask(R.string.backup_import_read_failed, {
            AutoBackup(context).availableBackups()
        }) { state.value = State.LocalBackups(it) }
    }

    fun selectLocalBackup(file: File) {
        val current = state.value as? State.LocalBackups ?: return
        if (file !in current.files) return
        state.value = State.Idle
        importDocument(Uri.fromFile(file))
    }

    fun importDocument(uri: Uri) {
        if (state.value != State.Idle) return
        state.value = State.Busy(R.string.backup_reading)
        runTask(R.string.backup_import_read_failed, {
            val directory = newWorkDirectory()
            val file = File(directory, "import.db")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Document provider returned no input stream")
            pendingImport = file
            file
        }) { file ->
            component.taskRunner.execute(
                ImportPreviewTask(component.genericImporter, file) { result ->
                    if (cleared) {
                        cleanup()
                    } else {
                        result.fold(
                            onSuccess = { state.value = State.Preview(it) },
                            onFailure = { finish(R.string.backup_invalid_file) }
                        )
                    }
                }
            )
        }
    }

    fun confirmImport() {
        if (state.value !is State.Preview) return
        val file = pendingImport ?: return
        state.value = State.Busy(R.string.backup_importing)
        component.taskRunner.execute(
            ImportDataTask(component.genericImporter, component.modelFactory, file, component.habitList) { result ->
                val refreshActions = mutableListOf<Pair<String, () -> Unit>>(
                    "habit list" to { component.habitCardListCache.refreshAllHabits() }
                )
                if (result == ImportDataTask.SUCCESS) {
                    refreshActions.add("widgets" to { component.widgetUpdater.updateWidgets() })
                    refreshActions.add("reminders" to { component.reminderScheduler.scheduleAll() })
                }
                completeImport(result, refreshActions)
            }
        )
    }

    internal fun completeImport(result: Int, refreshActions: List<Pair<String, () -> Unit>>) {
        var refreshFailed = false
        for ((name, refresh) in refreshActions) {
            try {
                refresh()
            } catch (e: Exception) {
                refreshFailed = true
                Log.e("BackupViewModel", "Could not refresh $name after import", e)
            }
        }
        when {
            result != ImportDataTask.SUCCESS -> finish(R.string.backup_import_failed)
            refreshFailed -> {
                cleanup()
                if (!cleared) state.value = State.ImportedWithWarning(R.string.backup_import_refresh_failed)
            }
            else -> finish(R.string.habits_imported, failed = false)
        }
    }

    fun exportDatabase() {
        if (state.value != State.Idle) return
        state.value = State.Busy(R.string.backup_creating)
        component.taskRunner.execute(
            ExportDBTask(context, AndroidDirFinder(context)) { filename ->
                if (filename == null) {
                    finish(R.string.backup_create_failed)
                } else {
                    exportReady(File(filename), true)
                }
            }
        )
    }

    fun exportCSV() {
        if (state.value != State.Idle) return
        state.value = State.Busy(R.string.backup_exporting_csv)
        runTask(R.string.backup_export_failed, {
            newWorkDirectory() to component.habitList.toList()
        }) { (directory, selectedHabits) ->
            val habits = component.habitList
            component.taskRunner.execute(
                ExportCSVTask(habits, component.sectionList, selectedHabits, directory) { filename ->
                    if (filename == null) {
                        finish(R.string.backup_export_failed)
                    } else {
                        exportReady(File(filename), false)
                    }
                }
            )
        }
    }

    fun backupNow() {
        if (state.value != State.Idle) return
        state.value = State.Busy(R.string.backup_creating)
        runTask(R.string.backup_create_failed, {
            if (!AutoBackup(context).run(force = true)) throw IOException("Automatic backup failed")
        }) { finish(R.string.backup_created, failed = false) }
    }

    fun choosingDestination() {
        if (state.value is State.ExportReady) state.value = State.ChoosingDestination
    }

    fun saveDocument(uri: Uri?) {
        if (uri == null) {
            cancel()
            return
        }
        val file = pendingExport
        if (file == null) {
            finish(R.string.backup_export_interrupted)
            return
        }
        state.value = State.Busy(R.string.backup_saving)
        runTask(R.string.backup_export_failed, {
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
                output.flush()
            } ?: throw IOException("Document provider returned no output stream")
        }) { finish(R.string.backup_exported, failed = false) }
    }

    fun cancel() {
        if (state.value is State.Busy) return
        cleanup()
        state.value = State.Idle
    }

    fun consumeMessage() {
        if (state.value is State.Message || state.value is State.ImportedWithWarning) state.value = State.Idle
    }

    fun documentPickerFailed() = finish(R.string.backup_picker_unavailable)

    private fun exportReady(file: File, database: Boolean) {
        if (cleared) {
            cleanup()
            return
        }
        pendingExport = file
        state.value = State.ExportReady(file.name, database)
    }

    private fun newWorkDirectory(): File {
        val directory = File(context.cacheDir, "loop-transfer-${UUID.randomUUID()}")
        if (!directory.mkdir()) throw IOException("Transfer storage unavailable")
        workDirectory = directory
        return directory
    }

    private fun <T> runTask(error: Int, work: () -> T, onSuccess: (T) -> Unit) {
        component.taskRunner.execute(object : Task {
            private var result: Result<T>? = null

            override fun doInBackground() {
                result = try {
                    Result.success(work())
                } catch (e: Exception) {
                    Log.e("BackupViewModel", "Transfer failed", e)
                    Result.failure(e)
                }
            }

            override fun onPostExecute() {
                if (cleared) {
                    cleanup()
                } else {
                    result!!.fold(onSuccess, { finish(error) })
                }
            }
        })
    }

    private fun finish(message: Int, failed: Boolean = true) {
        cleanup()
        if (!cleared) state.value = State.Message(message, failed)
    }

    private fun cleanup() {
        workDirectory?.let {
            if (!it.deleteRecursively()) Log.w("BackupViewModel", "Could not remove transfer files")
        }
        workDirectory = null
        pendingImport = null
        pendingExport = null
    }

    override fun onCleared() {
        cleared = true
        if (state.value !is State.Busy) cleanup()
        super.onCleared()
    }
}
