package com.example.burpeelog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.burpeelog.domain.BackupFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.autoBackupDataStore: DataStore<Preferences> by preferencesDataStore(name = "auto_backup_preferences")

data class AutoBackupState(
    val folderUri: String? = null,
    val folderName: String? = null,
    val frequency: BackupFrequency = BackupFrequency.MONTHLY,
    val autoBackupEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupSignature: Long = 0L,
    val lastReminderDate: String? = null,
    val lastBackupError: String? = null
)

class AutoBackupPreferences(private val context: Context) {

    companion object {
        val KEY_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val KEY_FOLDER_NAME = stringPreferencesKey("auto_backup_folder_name")
        val KEY_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("auto_backup_reminder_enabled")
        val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("auto_backup_last_timestamp")
        val KEY_LAST_BACKUP_SIGNATURE = longPreferencesKey("auto_backup_last_signature")
        val KEY_LAST_REMINDER_DATE = stringPreferencesKey("auto_backup_last_reminder_date")
        val KEY_LAST_BACKUP_ERROR = stringPreferencesKey("auto_backup_last_error")
    }

    val stateFlow: Flow<AutoBackupState> = context.autoBackupDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val folderUri = prefs[KEY_FOLDER_URI]
            val folderName = prefs[KEY_FOLDER_NAME]
            val freqName = prefs[KEY_FREQUENCY]
            val autoEnabled = prefs[KEY_AUTO_BACKUP_ENABLED] ?: false
            val reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: false
            val lastTimestamp = prefs[KEY_LAST_BACKUP_TIMESTAMP] ?: 0L
            val lastSignature = prefs[KEY_LAST_BACKUP_SIGNATURE] ?: 0L
            val lastReminder = prefs[KEY_LAST_REMINDER_DATE]
            val lastError = prefs[KEY_LAST_BACKUP_ERROR]

            AutoBackupState(
                folderUri = folderUri,
                folderName = folderName,
                frequency = BackupFrequency.fromName(freqName),
                autoBackupEnabled = autoEnabled && !folderUri.isNullOrBlank(),
                reminderEnabled = reminderEnabled,
                lastBackupTimestamp = lastTimestamp,
                lastBackupSignature = lastSignature,
                lastReminderDate = lastReminder,
                lastBackupError = lastError
            )
        }

    suspend fun setBackupFolder(uri: String, name: String) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_FOLDER_URI] = uri
            prefs[KEY_FOLDER_NAME] = name
            prefs.remove(KEY_LAST_BACKUP_ERROR) // Clear prior error when user chooses a new folder
        }
    }

    suspend fun setFrequency(frequency: BackupFrequency) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_FREQUENCY] = frequency.name
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_AUTO_BACKUP_ENABLED] = enabled
            if (enabled) {
                prefs.remove(KEY_LAST_BACKUP_ERROR)
            }
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun recordBackupSuccess(timestamp: Long, signature: Long) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_TIMESTAMP] = timestamp
            prefs[KEY_LAST_BACKUP_SIGNATURE] = signature
            prefs.remove(KEY_LAST_BACKUP_ERROR)
        }
    }

    suspend fun recordBackupError(errorMessage: String) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_ERROR] = errorMessage
            prefs[KEY_AUTO_BACKUP_ENABLED] = false // Disable auto-backup on error
        }
    }

    suspend fun clearBackupError() {
        context.autoBackupDataStore.edit { prefs ->
            prefs.remove(KEY_LAST_BACKUP_ERROR)
        }
    }

    suspend fun recordReminderSent(date: LocalDate) {
        context.autoBackupDataStore.edit { prefs ->
            prefs[KEY_LAST_REMINDER_DATE] = date.toString()
        }
    }
}
