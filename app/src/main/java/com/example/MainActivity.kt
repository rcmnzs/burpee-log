package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.preferences.AppTheme
import com.example.burpeelog.data.preferences.LocalePreferencesManager
import com.example.burpeelog.ui.MainScreen
import com.example.ui.theme.BurpeeLogTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localizedContext = LocalePreferencesManager.applyLocaleContext(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BurpeeLogApplication
        setContent {
            val appTheme by app.themePreferences.temaFlow.collectAsStateWithLifecycle(initialValue = AppTheme.ESCURO)
            val isDark = when (appTheme) {
                AppTheme.ESCURO -> true
                AppTheme.CLARO -> false
                AppTheme.SEGUIR_SISTEMA -> isSystemInDarkTheme()
            }
            BurpeeLogTheme(darkTheme = isDark) {
                MainScreen()
            }
        }
    }
}
