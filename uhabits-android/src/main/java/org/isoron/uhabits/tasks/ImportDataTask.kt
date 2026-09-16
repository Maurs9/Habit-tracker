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
package org.isoron.uhabits.tasks

import android.util.Log
import org.isoron.uhabits.core.io.GenericImporter
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.database.BackupValidator
import java.io.File

class ImportDataTask(
    private val importer: GenericImporter,
    modelFactory: ModelFactory,
    private val file: File,
    private val habitList: HabitList,
    private val listener: Listener
) : Task {
    private var result = 0
    private val modelFactory: SQLModelFactory = modelFactory as SQLModelFactory
    override fun doInBackground() {
        synchronized(habitList) {
            importData()
        }
    }

    private fun importData() {
        var transactionStarted = false
        try {
            if (BackupValidator.isLoopDatabase(file)) BackupValidator.inspect(file)
            modelFactory.database.beginTransaction()
            transactionStarted = true
            if (importer.canHandle(file)) {
                importer.importHabitsFromFile(file)
                result = SUCCESS
                modelFactory.database.setTransactionSuccessful()
            } else {
                result = NOT_RECOGNIZED
            }
        } catch (e: Exception) {
            result = FAILED
            Log.e("ImportDataTask", "Import failed", e)
        } finally {
            if (transactionStarted) {
                try {
                    modelFactory.database.endTransaction()
                } catch (e: Exception) {
                    result = FAILED
                    Log.e("ImportDataTask", "Could not finish import transaction", e)
                }
            }
            if (result != SUCCESS) {
                modelFactory.buildSectionList().reload()
                (habitList as? SQLiteHabitList)?.reload()
            }
        }
    }

    override fun onPostExecute() {
        listener.onImportDataFinished(result)
    }

    fun interface Listener {
        fun onImportDataFinished(result: Int)
    }

    companion object {
        const val FAILED = 3
        const val NOT_RECOGNIZED = 2
        const val SUCCESS = 1
    }
}
