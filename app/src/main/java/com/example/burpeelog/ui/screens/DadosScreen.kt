package com.example.burpeelog.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.preferences.AppLanguage
import com.example.burpeelog.data.preferences.AppTheme
import com.example.burpeelog.domain.BackupManager
import com.example.burpeelog.domain.BackupParseResult
import com.example.burpeelog.domain.BackupPreview
import com.example.burpeelog.domain.CsvImportPreview
import com.example.burpeelog.domain.CsvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

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

    var previewImportacao by remember { mutableStateOf<CsvImportPreview?>(null) }
    var isImportando by remember { mutableStateOf(false) }
    var isExportando by remember { mutableStateOf(false) }

    var previewBackup by remember { mutableStateOf<BackupPreview?>(null) }
    var isRestaurandoBackup by remember { mutableStateOf(false) }
    var isExportandoBackup by remember { mutableStateOf(false) }

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

    // Launcher do Storage Access Framework para selecionar arquivo CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val conteudo = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val maxChars = CsvManager.MAX_CSV_SIZE_BYTES
                            val buffer = CharArray(4096)
                            val sb = StringBuilder()
                            val reader = stream.bufferedReader()
                            var totalRead = 0
                            var read: Int
                            while (reader.read(buffer).also { read = it } != -1) {
                                totalRead += read
                                if (totalRead > maxChars) {
                                    return@use null
                                }
                                sb.append(buffer, 0, read)
                            }
                            sb.toString()
                        }
                    }
                    if (conteudo == null) {
                        Toast.makeText(context, context.getString(R.string.toast_csv_too_large), Toast.LENGTH_LONG).show()
                    } else if (conteudo.isNotBlank()) {
                        val preview = CsvManager.processarCsvImportacao(conteudo, todasSessoes)
                        previewImportacao = preview
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_csv_empty), Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_csv_read_error), Toast.LENGTH_SHORT).show()
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
            // Seção 0: Idioma (Language)
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

            // Seção: Backup Completo (JSON)
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
                }
            }

            // Seção 2: Importar CSV
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_importar_csv"),
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.import_csv_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.import_csv_header),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.import_csv_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_importar_csv"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_import_csv),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Seção 3: Exportar CSV
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_exportar_csv"),
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
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.export_csv_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.export_csv_count, todasSessoes.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.export_csv_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            if (!isExportando) {
                                isExportando = true
                                scope.launch {
                                    exportarECompartilharCsv(context, todasSessoes)
                                    isExportando = false
                                }
                            }
                        },
                        enabled = !isExportando && todasSessoes.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_exportar_csv"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isExportando) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_export_csv),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo de Prévia de Importação
    previewImportacao?.let { preview ->
        DialogPreviaImportacao(
            preview = preview,
            isImportando = isImportando,
            onConfirmar = {
                if (!isImportando) {
                    isImportando = true
                    scope.launch {
                        try {
                            repository.importarSessoes(preview.sessoesParaCriar)
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_import_success, preview.sessoesParaCriar.size),
                                Toast.LENGTH_LONG
                            ).show()
                            previewImportacao = null
                        } catch (_: Exception) {
                            Toast.makeText(context, context.getString(R.string.toast_import_db_error), Toast.LENGTH_LONG).show()
                        } finally {
                            isImportando = false
                        }
                    }
                }
            },
            onCancelar = { previewImportacao = null }
        )
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

/**
 * Diálogo com a prévia detalhada da importação:
 * - Linhas lidas
 * - Sessões a serem criadas
 * - Sessões ignoradas por duplicidade
 * - Primeiras 5 linhas
 * - Lista de linhas com erro (com número da linha)
 * - Botões Confirmar e Cancelar
 */
@Composable
private fun DialogPreviaImportacao(
    preview: CsvImportPreview,
    isImportando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isImportando) onCancelar() },
        title = {
            Text(text = stringResource(R.string.import_preview_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Métricas da leitura
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.import_preview_lines_read), style = MaterialTheme.typography.bodySmall)
                            Text("${preview.totalLinhasLidas}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.import_preview_sessions_create), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("${preview.totalSessoesCriadas}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.import_preview_duplicates), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${preview.totalDuplicadas}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Amostra das primeiras linhas
                if (preview.primeirasLinhas.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.import_preview_first_lines, preview.primeirasLinhas.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        preview.primeirasLinhas.forEach { linha ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "L${linha.numeroLinha}: ${linha.dataFormatada}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${stringResource(R.string.six_count_short)}: ${linha.sixCounts} · ${stringResource(R.string.navy_seal_short)}: ${linha.navySeals}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Linhas com erro
                if (preview.erros.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.import_preview_error_lines, preview.erros.size),
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
                                    text = stringResource(R.string.import_preview_row_error_format, erro.numeroLinha, erro.mensagem),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (preview.sessoesParaCriar.isEmpty()) {
                    Text(
                        text = stringResource(R.string.import_preview_no_valid_sessions),
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
                enabled = !isImportando && preview.sessoesParaCriar.isNotEmpty(),
                modifier = Modifier.testTag("btn_confirmar_importacao")
            ) {
                if (isImportando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.saving_progress))
                } else {
                    Text(stringResource(R.string.btn_confirm_count, preview.sessoesParaCriar.size))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                enabled = !isImportando,
                modifier = Modifier.testTag("btn_cancelar_importacao")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * Gera o arquivo CSV e aciona a folha de compartilhamento nativa do Android.
 */
private suspend fun exportarECompartilharCsv(
    context: Context,
    sessoes: List<com.example.burpeelog.data.model.Sessao>
) {
    withContext(Dispatchers.IO) {
        try {
            val conteudoCsv = CsvManager.gerarCsvExportacao(sessoes)
            val exportDir = File(context.cacheDir, "csv_exports").apply { mkdirs() }
            exportDir.listFiles()?.forEach { it.delete() }
            val cacheFile = File(exportDir, "burpee_log_export.csv")
            cacheFile.writeText(conteudoCsv)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.toast_export_error), Toast.LENGTH_LONG).show()
            }
        }
    }
}
