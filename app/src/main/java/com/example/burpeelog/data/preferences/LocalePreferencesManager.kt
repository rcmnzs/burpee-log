package com.example.burpeelog.data.preferences

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

enum class AppLanguage(val code: String, val labelRes: Int, val displayName: String) {
    AUTO("", R.string.language_auto, "Automático"),
    ENGLISH("en", R.string.language_en, "English"),
    PORTUGUESE("pt-BR", R.string.language_pt_br, "Português (BR)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when {
                code.isNullOrBlank() -> AUTO
                code.equals("en", ignoreCase = true) -> ENGLISH
                code.startsWith("pt", ignoreCase = true) -> PORTUGUESE
                else -> ENGLISH // Fallback para inglês quando idioma não suportado
            }
        }
    }
}

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "locale_preferences")

class LocalePreferencesManager(private val context: Context) {

    companion object {
        val LOCALE_KEY = stringPreferencesKey("app_language_code")
        private const val PREFS_NAME = "locale_backup_prefs"
        private const val PREF_KEY = "cached_locale_code"

        fun getSavedLocaleCodeSync(context: Context): String {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sp.getString(PREF_KEY, "") ?: ""
        }

        fun applyLocaleContext(context: Context): Context {
            val savedCode = getSavedLocaleCodeSync(context)
            if (savedCode.isBlank()) {
                return context
            }
            val targetLocale = if (savedCode == "pt-BR" || savedCode == "pt") Locale("pt", "BR") else Locale(savedCode)
            Locale.setDefault(targetLocale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(targetLocale)
            return context.createConfigurationContext(config)
        }
    }

    val idiomaFlow: Flow<AppLanguage> = context.localeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val code = preferences[LOCALE_KEY]
            AppLanguage.fromCode(code)
        }

    suspend fun salvarIdioma(idioma: AppLanguage, activity: Activity? = null) {
        val codeToSave = if (idioma == AppLanguage.AUTO) "" else idioma.code

        context.localeDataStore.edit { preferences ->
            if (codeToSave.isEmpty()) {
                preferences.remove(LOCALE_KEY)
            } else {
                preferences[LOCALE_KEY] = codeToSave
            }
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_KEY, codeToSave)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (idioma == AppLanguage.AUTO) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(idioma.code)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idioma.code))
            }
        } else {
            val localeListCompat = if (idioma == AppLanguage.AUTO) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(idioma.code)
            }
            AppCompatDelegate.setApplicationLocales(localeListCompat)
            activity?.recreate()
        }
    }
}
