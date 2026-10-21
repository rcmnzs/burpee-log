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

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.burpeelog.domain.AutoBackupManager
import com.example.burpeelog.service.BackupNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var abrirDadosDireto by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        val localizedContext = LocalePreferencesManager.applyLocaleContext(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getStringExtra(BackupNotificationHelper.EXTRA_DESTINATION) == BackupNotificationHelper.DESTINATION_DADOS) {
            abrirDadosDireto = true
        }

        // On app open: check if backup is overdue and execute auto-backup or reminder if due
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AutoBackupManager.verificarEExecutar(applicationContext)
            } catch (_: Exception) {
                // Safeguard against any unexpected failure during startup
            }
        }

        val app = application as BurpeeLogApplication
        setContent {
            val appTheme by app.themePreferences.temaFlow.collectAsStateWithLifecycle(initialValue = AppTheme.ESCURO)
            val isDark = when (appTheme) {
                AppTheme.ESCURO -> true
                AppTheme.CLARO -> false
                AppTheme.SEGUIR_SISTEMA -> isSystemInDarkTheme()
            }
            BurpeeLogTheme(darkTheme = isDark) {
                MainScreen(
                    initialMostrandoDados = abrirDadosDireto,
                    onResetAbrirDados = { abrirDadosDireto = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra(BackupNotificationHelper.EXTRA_DESTINATION) == BackupNotificationHelper.DESTINATION_DADOS) {
            abrirDadosDireto = true
        }
    }
}
