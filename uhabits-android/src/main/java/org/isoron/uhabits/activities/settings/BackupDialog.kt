package org.isoron.uhabits.activities.settings

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.isoron.uhabits.R
import org.isoron.uhabits.database.BackupPreview
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.TimeZone

object BackupDialog {
    fun show(
        context: Context,
        preview: BackupPreview?,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ): AlertDialog {
        val message = if (preview == null) {
            context.getString(R.string.backup_import_other_preview)
        } else {
            val number = NumberFormat.getIntegerInstance()
            val date = preview.latestEntryTimestamp?.let {
                DateFormat.getDateInstance(DateFormat.MEDIUM).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(it))
            } ?: context.getString(R.string.backup_no_entries)
            context.getString(
                R.string.backup_preview_details,
                number.format(preview.habitCount),
                number.format(preview.entryCount),
                date,
                number.format(preview.version)
            )
        }
        return AlertDialog.Builder(context)
            .setTitle(R.string.backup_preview_title)
            .setMessage(message + "\n\n" + context.getString(R.string.backup_import_warning))
            .setPositiveButton(R.string.backup_confirm_import) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }
}
