package com.example.burpeelog

import android.app.Application
import com.example.burpeelog.data.database.AppDatabase
import com.example.burpeelog.data.repository.SessaoRepository
import com.example.burpeelog.data.repository.SessaoRepositoryImpl

class BurpeeLogApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SessaoRepository by lazy { SessaoRepositoryImpl(database.sessaoDao()) }
    val patternPreferences: com.example.burpeelog.data.preferences.PatternPreferences by lazy {
        com.example.burpeelog.data.preferences.PatternPreferences(this)
    }
    val themePreferences: com.example.burpeelog.data.preferences.ThemePreferences by lazy {
        com.example.burpeelog.data.preferences.ThemePreferences(this)
    }
    val localePreferences: com.example.burpeelog.data.preferences.LocalePreferencesManager by lazy {
        com.example.burpeelog.data.preferences.LocalePreferencesManager(this)
    }
    val workoutSessionManager: com.example.burpeelog.service.WorkoutSessionManager by lazy {
        com.example.burpeelog.service.WorkoutSessionManager(this)
    }
    val autoBackupPreferences: com.example.burpeelog.data.preferences.AutoBackupPreferences by lazy {
        com.example.burpeelog.data.preferences.AutoBackupPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            com.example.burpeelog.service.AutoBackupWorker.scheduleDailyCheck(this)
        } catch (_: Exception) {
            // Safe fallback if WorkManager initialization differs in test environments
        }
    }
}
