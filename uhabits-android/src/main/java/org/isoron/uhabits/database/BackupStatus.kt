package org.isoron.uhabits.database

import android.content.Context

class BackupStatus(context: Context) {
    private val preferences = context.getSharedPreferences("backup_status", Context.MODE_PRIVATE)

    val lastSuccess: Long
        get() = preferences.getLong("last_success", 0)
    val lastFailure: Long
        get() = preferences.getLong("last_failure", 0)
    val lastSuccessFile: String?
        get() = preferences.getString("last_success_file", null)
    val failureReason: String?
        get() = preferences.getString("failure_reason", null)

    fun succeeded(file: String, time: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong("last_success", time)
            .putString("last_success_file", file)
            .remove("last_failure")
            .remove("failure_reason")
            .apply()
    }

    fun failed(error: Exception, time: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong("last_failure", time)
            .putString("failure_reason", error.javaClass.simpleName)
            .apply()
    }
}
