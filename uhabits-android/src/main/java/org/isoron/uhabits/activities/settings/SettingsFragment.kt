/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.activities.settings

import android.app.backup.BackupManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.RESULT_BUG_REPORT
import org.isoron.uhabits.activities.habits.list.RESULT_REPAIR_DB
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.NotificationTray
import org.isoron.uhabits.core.utils.DateUtils.Companion.getLongWeekdayNames
import org.isoron.uhabits.database.BackupStatus
import org.isoron.uhabits.notifications.AndroidNotificationTray.Companion.createAndroidNotificationChannel
import org.isoron.uhabits.notifications.RingtoneManager
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.showMessage
import org.isoron.uhabits.utils.startActivitySafely
import org.isoron.uhabits.widgets.WidgetUpdater
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var sharedPrefs: SharedPreferences? = null
    private var ringtoneManager: RingtoneManager? = null
    private lateinit var prefs: Preferences
    private var widgetUpdater: WidgetUpdater? = null
    private lateinit var backupModel: BackupViewModel
    private var previewDialog: AlertDialog? = null
    private var errorDialog: AlertDialog? = null
    private var localBackupsDialog: AlertDialog? = null
    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) backupModel.importDocument(it)
    }
    private val exportDatabase = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { backupModel.saveDocument(it) }
    private val exportCSV = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { backupModel.saveDocument(it) }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RINGTONE_REQUEST_CODE) {
            ringtoneManager!!.update(data)
            updateRingtoneDescription()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val appContext = requireContext().applicationContext
        if (appContext is HabitsApplication) {
            prefs = appContext.component.preferences
            widgetUpdater = appContext.component.widgetUpdater
        }
        backupModel = ViewModelProvider(this)[BackupViewModel::class.java]
        requirePreference<Preference>("exportDB").summary = getString(R.string.backup_database_summary)
        requirePreference<Preference>("importData").summary = getString(R.string.backup_import_summary)
        val category = requirePreference<PreferenceCategory>("databaseCategory")
        category.addPreference(
            Preference(requireContext()).apply {
                key = "backupStatus"
                title = getString(R.string.backup_status_title)
                isIconSpaceReserved = false
                order = -1
            }
        )
        category.addPreference(
            Preference(requireContext()).apply {
                key = "localBackups"
                title = getString(R.string.backup_local_title)
                summary = getString(R.string.backup_local_summary)
                isIconSpaceReserved = false
            }
        )
        setResultOnPreferenceClick("repairDB", RESULT_REPAIR_DB)
        setResultOnPreferenceClick("bugReport", RESULT_BUG_REPORT)
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        // NOP
    }

    override fun onPause() {
        sharedPrefs!!.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sr = StyledResources(context!!)
        view.setBackgroundColor(sr.getColor(R.attr.contrast0))
        super.onViewCreated(view, savedInstanceState)
        backupModel.state.observe(viewLifecycleOwner) { state ->
            val idle = state is BackupViewModel.State.Idle || state is BackupViewModel.State.Message
            for (key in listOf("exportDB", "exportCSV", "importData", "backupStatus", "localBackups")) {
                requirePreference<Preference>(key).isEnabled = idle
            }
            updateBackupStatus()
            when (state) {
                is BackupViewModel.State.Busy ->
                    requirePreference<Preference>("backupStatus").summary = getString(state.message)
                is BackupViewModel.State.Preview -> {
                    if (previewDialog == null) {
                        previewDialog = BackupDialog.show(
                            requireContext(),
                            state.details,
                            onConfirm = {
                                previewDialog = null
                                backupModel.confirmImport()
                            },
                            onCancel = {
                                previewDialog = null
                                backupModel.cancel()
                            }
                        )
                    }
                }
                is BackupViewModel.State.LocalBackups -> {
                    if (localBackupsDialog == null) {
                        val builder = AlertDialog.Builder(requireContext())
                            .setTitle(R.string.backup_local_title)
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                localBackupsDialog = null
                                backupModel.cancel()
                            }
                            .setOnCancelListener {
                                localBackupsDialog = null
                                backupModel.cancel()
                            }
                        if (state.files.isEmpty()) {
                            builder.setMessage(R.string.backup_local_empty)
                        } else {
                            val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                            val labels = state.files.map {
                                getString(R.string.backup_file_modified, dateFormat.format(Date(it.lastModified())))
                            }.toTypedArray()
                            builder.setItems(labels) { _, index ->
                                localBackupsDialog = null
                                backupModel.selectLocalBackup(state.files[index])
                            }
                        }
                        localBackupsDialog = builder.show()
                    }
                }
                is BackupViewModel.State.ExportReady -> {
                    backupModel.choosingDestination()
                    try {
                        if (state.database) {
                            exportDatabase.launch(state.filename)
                        } else {
                            exportCSV.launch(state.filename)
                        }
                    } catch (e: RuntimeException) {
                        Log.e("SettingsFragment", "Could not launch document picker", e)
                        backupModel.documentPickerFailed()
                    }
                }
                is BackupViewModel.State.ImportedWithWarning ->
                    showFeedback(R.string.backup_import_warning_title, state.message)
                is BackupViewModel.State.Message -> {
                    if (state.failed) {
                        showFeedback(R.string.backup_action_failed, state.message)
                    } else {
                        requireActivity().showMessage(getString(state.message))
                        backupModel.consumeMessage()
                    }
                }
                else -> Unit
            }
        }
    }

    private fun showFeedback(title: Int, message: Int) {
        if (errorDialog != null) return
        errorDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                errorDialog = null
                backupModel.consumeMessage()
            }
            .setOnCancelListener {
                errorDialog = null
                backupModel.consumeMessage()
            }
            .show()
    }

    override fun onDestroyView() {
        previewDialog?.dismiss()
        previewDialog = null
        errorDialog?.dismiss()
        errorDialog = null
        localBackupsDialog?.dismiss()
        localBackupsDialog = null
        super.onDestroyView()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key ?: return false
        when (key) {
            "importData" -> {
                try {
                    importDocument.launch(arrayOf("*/*"))
                } catch (e: RuntimeException) {
                    Log.e("SettingsFragment", "Could not launch document picker", e)
                    backupModel.documentPickerFailed()
                }
                return true
            }
            "exportDB" -> {
                backupModel.exportDatabase()
                return true
            }
            "exportCSV" -> {
                backupModel.exportCSV()
                return true
            }
            "backupStatus" -> {
                backupModel.backupNow()
                return true
            }
            "localBackups" -> {
                backupModel.showLocalBackups()
                return true
            }
            "reminderSound" -> {
                showRingtonePicker()
                return true
            }
            "reminderCustomize" -> {
                createAndroidNotificationChannel(requireContext())
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationTray.REMINDERS_CHANNEL_ID)
                startActivity(intent)
                return true
            }
            "rateApp" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playStoreURL)))
                activity?.startActivitySafely(intent)
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onResume() {
        super.onResume()
        ringtoneManager = RingtoneManager(requireActivity())
        sharedPrefs = preferenceManager.sharedPreferences
        sharedPrefs!!.registerOnSharedPreferenceChangeListener(this)
        if (!prefs.isDeveloper) {
            val devCategory = requirePreference<PreferenceCategory>("devCategory")
            devCategory.isVisible = false
        }
        updateWeekdayPreference()
        updateBackupStatus()

        requirePreference<Preference>("reminderSound").isVisible = false
    }

    private fun updateBackupStatus() {
        val status = BackupStatus(requireContext())
        val lastSuccess = if (status.lastSuccess == 0L) {
            getString(R.string.backup_status_never)
        } else {
            getString(
                R.string.backup_status_success,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(status.lastSuccess))
            )
        }
        requirePreference<Preference>("backupStatus").summary = if (status.lastFailure > 0) {
            lastSuccess + "\n" + getString(R.string.backup_status_failure)
        } else {
            lastSuccess
        }
        val state = backupModel.state.value
        if (state is BackupViewModel.State.Busy) {
            requirePreference<Preference>("backupStatus").summary = getString(state.message)
        }
    }

    private fun updateWeekdayPreference() {
        val weekdayPref = requirePreference<ListPreference>("pref_first_weekday")
        val currentFirstWeekday = prefs.firstWeekday.daysSinceSunday + 1
        val dayNames = getLongWeekdayNames(Calendar.SATURDAY)
        val dayValues = arrayOf("7", "1", "2", "3", "4", "5", "6")
        weekdayPref.entries = dayNames
        weekdayPref.entryValues = dayValues
        weekdayPref.setDefaultValue(currentFirstWeekday.toString())
        weekdayPref.summary = dayNames[currentFirstWeekday % 7]
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?
    ) {
        if (key == "pref_widget_opacity" && widgetUpdater != null) {
            Log.d("SettingsFragment", "updating widgets")
            widgetUpdater!!.updateWidgets()
        }
        BackupManager.dataChanged("org.isoron.uhabits")
        updateWeekdayPreference()
    }

    private fun setResultOnPreferenceClick(key: String, result: Int) {
        val pref = requirePreference<Preference>(key)
        pref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                requireActivity().setResult(result)
                requireActivity().finish()
                true
            }
    }

    private fun showRingtonePicker() {
        val existingRingtoneUri = ringtoneManager!!.getURI()
        val defaultRingtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
            android.media.RingtoneManager.TYPE_NOTIFICATION
        )
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            defaultRingtoneUri
        )
        intent.putExtra(
            android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            existingRingtoneUri
        )
        startActivityForResult(intent, RINGTONE_REQUEST_CODE)
    }

    private fun updateRingtoneDescription() {
        val ringtoneName = ringtoneManager!!.getName() ?: return
        val ringtonePreference = requirePreference<Preference>("reminderSound")
        ringtonePreference.summary = ringtoneName
    }

    private inline fun <reified T : Preference> requirePreference(key: String): T {
        val preference = checkNotNull(findPreference<Preference>(key)) {
            "Missing preference: $key"
        }
        check(preference is T) {
            "Invalid preference type for $key: expected ${T::class.java.simpleName}, " +
                "found ${preference.javaClass.simpleName}"
        }
        return preference
    }

    companion object {
        private const val RINGTONE_REQUEST_CODE = 1
    }
}
