package com.example.burpeelog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AppTheme(val rotulo: String) {
    ESCURO("Escuro"),
    CLARO("Claro"),
    SEGUIR_SISTEMA("Automático")
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme_mode")
    }

    val temaFlow: Flow<AppTheme> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val nome = preferences[THEME_KEY]
            when (nome) {
                AppTheme.CLARO.name -> AppTheme.CLARO
                AppTheme.SEGUIR_SISTEMA.name -> AppTheme.SEGUIR_SISTEMA
                else -> AppTheme.ESCURO // Padrão é ESCURO
            }
        }

    suspend fun salvarTema(novoTema: AppTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = novoTema.name
        }
    }
}
