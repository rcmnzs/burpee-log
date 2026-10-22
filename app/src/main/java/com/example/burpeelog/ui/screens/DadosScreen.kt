package com.example.burpeelog.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import com.example.burpeelog.ui.components.BurpeeSwitch
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.burpeelog.data.preferences.AutoBackupState
import com.example.burpeelog.domain.AutoBackupManager
import com.example.burpeelog.domain.BackupExecutionResult
import com.example.burpeelog.domain.BackupFrequency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.preferences.AppLanguage
import com.example.burpeelog.data.preferences.AppTheme
import com.example.burpeelog.domain.BackupManager
import com.example.burpeelog.domain.BackupParseResult
import com.example.burpeelog.domain.BackupPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DadosScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BurpeeLogApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()

    val todasSessoes by repository.allSessoes.collectAsStateWithLifecycle(emptyList())
    val temaAtual by app.themePreferences.temaFlow.collectAsStateWithLifecycle(initialValue = AppTheme.ESCURO)
    val idiomaAtual by app.localePreferences.idiomaFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.AUTO)

    var previewBackup by remember { mutableStateOf<BackupPreview?>(null) }
    var isRestaurandoBackup by remember { mutableStateOf(false) }
    var isExportandoBackup by remember { mutableStateOf(false) }
    val autoBackupState by app.autoBackupPreferences.stateFlow.collectAsStateWithLifecycle(initialValue = AutoBackupState())
    var isFazendoBackupAgora by remember { mutableStateOf(false) }

    // Launcher do Storage Access Framework para selecionar a pasta de backup automático
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val docFolder = DocumentFile.fromTreeUri(context, uri)
                val folderName = docFolder?.name ?: uri.lastPathSegment ?: "Backup"
                scope.launch {
                    app.autoBackupPreferences.setBackupFolder(uri.toString(), folderName)
                }
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_auto_backup_folder_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Launcher de permissão para POST_NOTIFICATIONS (Android 13+)
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        scope.launch {
            if (isGranted) {
                app.autoBackupPreferences.setReminderEnabled(true)
            } else {
                app.autoBackupPreferences.setReminderEnabled(false)
                Toast.makeText(
                    context,
                    context.getString(R.string.permission_notification_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Launcher do Storage Access Framework para criação do arquivo JSON de backup
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isExportandoBackup = true
                    val jsonString = BackupManager.gerarBackupJson(
                        sessoes = todasSessoes,
                        appVersion = BuildConfig.VERSION_NAME
                    )
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        }
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_backup_export_success),
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_backup_export_error),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isExportandoBackup = false
                }
            }
        }
    }

    // Launcher do Storage Access Framework para seleção do arquivo JSON de backup
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    var isTooLarge = false
                    val maxBytes = BackupManager.MAX_BACKUP_SIZE_BYTES
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val buffer = ByteArray(8192)
                            val baos = ByteArrayOutputStream()
                            var totalRead = 0L
                            var read: Int
                            while (stream.read(buffer).also { read = it } != -1) {
                                totalRead += read
                                if (totalRead > maxBytes) {
                                    isTooLarge = true
                                    return@use null
                                }
                                baos.write(buffer, 0, read)
                            }
                            baos.toString(Charsets.UTF_8.name())
                        }
                    }

                    if (isTooLarge) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_backup_too_large),
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (jsonString.isNullOrBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_backup_invalid_json),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        when (val resultado = BackupManager.processarBackupJson(jsonString, todasSessoes)) {
                            is BackupParseResult.Sucesso -> {
                                previewBackup = resultado.preview
                            }
                            is BackupParseResult.ArquivoMuitoGrande -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_backup_too_large),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is BackupParseResult.JsonInvalido -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_backup_invalid_json),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is BackupParseResult.VersaoNaoSuportada -> {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.toast_backup_unsupported_version,
                                        resultado.versaoEncontrada
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            is BackupParseResult.FormatoInvalido -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_backup_invalid_json),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_backup_read_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_dados"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_and_data_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_voltar_dados")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Seção 0: Backup Completo (JSON)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_backup"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.backup_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.backup_card_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.backup_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (todasSessoes.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_backup_no_sessions),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val defaultNome = BackupManager.gerarNomeArquivoBackup()
                                    createDocumentLauncher.launch(defaultNome)
                                }
                            },
                            enabled = !isExportandoBackup,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_fazer_backup"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isExportandoBackup) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = stringResource(R.string.btn_fazer_backup),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "application/*", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_restaurar_backup"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_restaurar_backup),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Aviso se a pasta de backup não está mais acessível
                    if (autoBackupState.lastBackupError != null) {
                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("banner_erro_backup")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = autoBackupState.lastBackupError
                                        ?: stringResource(R.string.backup_folder_error_banner),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Escolha da pasta de backup
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openFolderLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_escolher_pasta_backup"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_choose_backup_folder),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = if (!autoBackupState.folderName.isNullOrBlank()) {
                                stringResource(R.string.backup_folder_selected, autoBackupState.folderName ?: "")
                            } else {
                                stringResource(R.string.backup_no_folder_selected)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!autoBackupState.folderName.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (!autoBackupState.folderName.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("text_pasta_backup")
                        )
                    }

                    // Seletor de Frequência
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.backup_freq_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("segmented_button_frequencia")
                        ) {
                            val opcoes = listOf(
                                BackupFrequency.WEEKLY,
                                BackupFrequency.MONTHLY
                            )
                            opcoes.forEachIndexed { index, freq ->
                                val isSelected = autoBackupState.frequency == freq
                                SegmentedButton(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            app.autoBackupPreferences.setFrequency(freq)
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = opcoes.size),
                                    modifier = Modifier.testTag(
                                        when (freq) {
                                            BackupFrequency.WEEKLY -> "btn_freq_weekly"
                                            BackupFrequency.MONTHLY -> "btn_freq_monthly"
                                        }
                                    )
                                ) {
                                    Text(
                                        text = stringResource(freq.labelRes),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Interruptor: Backup Automático
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.backup_switch_auto),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.backup_switch_auto_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BurpeeSwitch(
                            checked = autoBackupState.autoBackupEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    app.autoBackupPreferences.setAutoBackupEnabled(checked)
                                }
                            },
                            enabled = !autoBackupState.folderUri.isNullOrBlank(),
                            modifier = Modifier.testTag("switch_backup_automatico")
                        )
                    }

                    // Interruptor: Lembrete de Backup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.backup_switch_reminder),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.backup_switch_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BurpeeSwitch(
                            checked = autoBackupState.reminderEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            scope.launch {
                                                app.autoBackupPreferences.setReminderEnabled(true)
                                            }
                                        } else {
                                            requestNotificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    } else {
                                        scope.launch {
                                            app.autoBackupPreferences.setReminderEnabled(true)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        app.autoBackupPreferences.setReminderEnabled(false)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("switch_lembrete_backup")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Último backup e botão "Fazer backup agora"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ultimoBackupTexto = if (autoBackupState.lastBackupTimestamp > 0L) {
                            val dataHora = Instant.ofEpochMilli(autoBackupState.lastBackupTimestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
                            stringResource(R.string.backup_last_backup, dataHora)
                        } else {
                            stringResource(R.string.backup_never_backed_up)
                        }

                        Text(
                            text = ultimoBackupTexto,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("text_ultimo_backup")
                        )

                        Button(
                            onClick = {
                                val folderUriString = autoBackupState.folderUri
                                if (folderUriString.isNullOrBlank()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.backup_no_folder_selected),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (todasSessoes.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_backup_no_sessions),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    scope.launch {
                                        try {
                                            isFazendoBackupAgora = true
                                            val uri = Uri.parse(folderUriString)
                                            val result = AutoBackupManager.executarBackupNaPasta(
                                                context = context,
                                                folderUri = uri,
                                                sessoes = todasSessoes,
                                                appVersion = BuildConfig.VERSION_NAME
                                            )
                                            when (result) {
                                                is BackupExecutionResult.Success -> {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.toast_auto_backup_success),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                is BackupExecutionResult.Error -> {
                                                    Toast.makeText(
                                                        context,
                                                        result.message,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                else -> {}
                                            }
                                        } finally {
                                            isFazendoBackupAgora = false
                                        }
                                    }
                                }
                            },
                            enabled = !autoBackupState.folderUri.isNullOrBlank() && !isFazendoBackupAgora,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_backup_agora"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isFazendoBackupAgora) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.btn_backup_now),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Seção 1: Aparência e Tema
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_configuracao_tema"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.appearance_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.appearance_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("segmented_button_tema")
                    ) {
                        val opcoes = listOf(
                            AppTheme.ESCURO,
                            AppTheme.CLARO,
                            AppTheme.SEGUIR_SISTEMA
                        )
                        opcoes.forEachIndexed { index, tema ->
                            SegmentedButton(
                                selected = temaAtual == tema,
                                onClick = {
                                    scope.launch {
                                        app.themePreferences.salvarTema(tema)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = opcoes.size),
                                modifier = Modifier.testTag("btn_tema_${tema.name.lowercase()}")
                            ) {
                                Text(text = stringResource(tema.labelRes), maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Seção 2: Idioma (Language)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_idioma"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.language_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.language_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("segmented_button_idioma")
                    ) {
                        val opcoes = listOf(
                            AppLanguage.AUTO,
                            AppLanguage.ENGLISH,
                            AppLanguage.PORTUGUESE
                        )
                        opcoes.forEachIndexed { index, idioma ->
                            val isSelected = idiomaAtual == idioma
                            SegmentedButton(
                                selected = isSelected,
                                onClick = {
                                    scope.launch {
                                        app.localePreferences.salvarIdioma(
                                            idioma = idioma,
                                            activity = context as? Activity
                                        )
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = opcoes.size),
                                modifier = Modifier.testTag(
                                    when (idioma) {
                                        AppLanguage.AUTO -> "btn_idioma_auto"
                                        AppLanguage.ENGLISH -> "btn_idioma_en"
                                        AppLanguage.PORTUGUESE -> "btn_idioma_pt_br"
                                    }
                                )
                            ) {
                                Text(
                                    text = when (idioma) {
                                        AppLanguage.AUTO -> stringResource(R.string.language_auto)
                                        AppLanguage.ENGLISH -> stringResource(R.string.language_en)
                                        AppLanguage.PORTUGUESE -> stringResource(R.string.language_pt_br)
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Text(
                            text = stringResource(R.string.language_restart_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de Prévia de Backup
    previewBackup?.let { preview ->
        DialogPreviaBackup(
            preview = preview,
            isRestaurando = isRestaurandoBackup,
            onConfirmar = {
                if (!isRestaurandoBackup) {
                    isRestaurandoBackup = true
                    scope.launch {
                        try {
                            repository.importarSessoes(preview.sessoesParaAdicionar)
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_backup_restore_success, preview.sessoesParaAdicionar.size),
                                Toast.LENGTH_LONG
                            ).show()
                            previewBackup = null
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_backup_restore_error),
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isRestaurandoBackup = false
                        }
                    }
                }
            },
            onCancelar = { previewBackup = null }
        )
    }
}

/**
 * Diálogo com a prévia detalhada da restauração de backup JSON:
 * - Sessões lidas
 * - Sessões a serem adicionadas
 * - Sessões ignoradas por duplicidade
 * - Sessões ignoradas por erro/invalidação
 * - Lista de erros detalhados
 * - Botões Confirmar e Cancelar
 */
@Composable
private fun DialogPreviaBackup(
    preview: BackupPreview,
    isRestaurando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isRestaurando) onCancelar() },
        title = {
            Text(text = stringResource(R.string.backup_preview_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Métricas do backup
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.backup_preview_sessions_read), style = MaterialTheme.typography.bodySmall)
                            Text("${preview.totalLidas}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.backup_preview_sessions_add), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("${preview.totalParaAdicionar}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.backup_preview_duplicates), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${preview.totalDuplicadas}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (preview.totalErros > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.backup_preview_errors), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("${preview.totalErros}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Lista de erros (se houver)
                if (preview.erros.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.backup_preview_error_lines, preview.erros.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        preview.erros.forEach { erro ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.backup_preview_error_format, erro.indice, erro.mensagem),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (preview.sessoesParaAdicionar.isEmpty()) {
                    Text(
                        text = stringResource(R.string.backup_preview_no_sessions_to_add),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !isRestaurando && preview.sessoesParaAdicionar.isNotEmpty(),
                modifier = Modifier.testTag("btn_confirmar_restauracao")
            ) {
                if (isRestaurando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.saving_progress))
                } else {
                    Text(stringResource(R.string.btn_confirm_count, preview.sessoesParaAdicionar.size))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                enabled = !isRestaurando,
                modifier = Modifier.testTag("btn_cancelar_restauracao")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

