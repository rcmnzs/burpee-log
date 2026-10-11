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
    val workoutSessionManager: com.example.burpeelog.service.WorkoutSessionManager by lazy {
        com.example.burpeelog.service.WorkoutSessionManager(this)
    }
}
