package org.isoron.uhabits.tasks

import android.util.Log
import org.isoron.uhabits.core.io.GenericImporter
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.database.BackupPreview
import org.isoron.uhabits.database.BackupValidator
import java.io.File
import java.io.IOException

class ImportPreviewTask(
    private val importer: GenericImporter,
    private val file: File,
    private val listener: (Result<BackupPreview?>) -> Unit
) : Task {
    private var result: Result<BackupPreview?>? = null

    override fun doInBackground() {
        result = try {
            val preview = if (BackupValidator.isLoopDatabase(file)) {
                BackupValidator.inspect(file)
            } else {
                if (!importer.canHandle(file)) throw IOException("Unrecognized import file")
                null
            }
            Result.success(preview)
        } catch (e: Exception) {
            Log.e("ImportPreviewTask", "Cannot preview import", e)
            Result.failure(e)
        }
    }

    override fun onPostExecute() = listener(result!!)
}
