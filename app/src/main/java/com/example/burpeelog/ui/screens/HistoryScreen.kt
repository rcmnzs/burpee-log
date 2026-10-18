package com.example.burpeelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.DuracaoValidator
import com.example.burpeelog.ui.components.BurpeeHistoryLineChart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel? = run {
        val app = LocalContext.current.applicationContext as? BurpeeLogApplication
        if (app != null) {
            viewModel(factory = HistoryViewModel.Factory(app.repository))
        } else null
    }
) {
    val uiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: HistoryUiState()

    var sessaoParaExcluir by remember { mutableStateOf<Sessao?>(null) }
    var sessaoParaEditar by remember { mutableStateOf<Sessao?>(null) }
    var showDialogAdicionarPassada by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_history"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Histórico de Treinos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${uiState.totalGeral} ${if (uiState.totalGeral == 1) "sessão registrada" else "sessões registradas"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialogAdicionarPassada = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.btn_log_past_workout)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_adicionar_sessao")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filtro por Tipo de Exercício
            FiltroTipoBar(
                filtroSelecionado = uiState.filtroSelecionado,
                totalGeral = uiState.totalGeral,
                totalSixCount = uiState.totalSixCount,
                totalNavySeal = uiState.totalNavySeal,
                onFiltroChange = { viewModel?.definirFiltro(it) }
            )

            // Gráfico de linha com reps feitas por sessão (Six-Count e Navy Seal, filtrável 30d, 90d, tudo)
            if (uiState.todasSessoes.isNotEmpty()) {
                BurpeeHistoryLineChart(
                    sessoes = uiState.todasSessoes,
                    periodoSelecionado = uiState.periodoGrafico,
                    onPeriodoChange = { viewModel?.definirPeriodoGrafico(it) }
                )
            }

            // Lista de Sessões ou Estado Vazio
            if (uiState.sessoes.isEmpty()) {
                EmptyHistoryView(
                    filtroAtivo = uiState.filtroSelecionado != null,
                    onAdicionarClick = { showDialogAdicionarPassada = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_history_sessoes"),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.sessoes,
                        key = { it.id }
                    ) { sessao ->
                        SessaoItemCard(
                            sessao = sessao,
                            onEditar = { sessaoParaEditar = sessao },
                            onExcluir = { sessaoParaExcluir = sessao }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Confirmação para Excluir Sessão
    sessaoParaExcluir?.let { sessao ->
        DialogConfirmarExclusao(
            sessao = sessao,
            onConfirmar = {
                viewModel?.excluirSessao(sessao)
                sessaoParaExcluir = null
            },
            onDismiss = { sessaoParaExcluir = null }
        )
    }

    // Diálogo para Editar Sessão
    sessaoParaEditar?.let { sessao ->
        DialogEditarSessao(
            sessao = sessao,
            onSalvar = { sessaoAtualizada ->
                viewModel?.atualizarSessao(sessaoAtualizada)
                sessaoParaEditar = null
            },
            onDismiss = { sessaoParaEditar = null }
        )
    }

    // Diálogo para Adicionar Manualmente Sessão Passada
    if (showDialogAdicionarPassada) {
        DialogAdicionarSessaoPassada(
            onAdicionar = { dataHora, tipo, reps, obs, duracaoSegundos ->
                viewModel?.adicionarSessaoPassada(dataHora, tipo, reps, obs, duracaoSegundos)
                showDialogAdicionarPassada = false
            },
            onDismiss = { showDialogAdicionarPassada = false }
        )
    }
}

/**
 * Barra de chips para filtrar por tipo de exercício.
 */
@Composable
private fun FiltroTipoBar(
    filtroSelecionado: TipoExercicio?,
    totalGeral: Int,
    totalSixCount: Int,
    totalNavySeal: Int,
    onFiltroChange: (TipoExercicio?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = filtroSelecionado == null,
            onClick = { onFiltroChange(null) },
            label = {
                Text(
                    text = "Todos ($totalGeral)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("chip_filtro_todos")
        )

        FilterChip(
            selected = filtroSelecionado == TipoExercicio.SIX_COUNT,
            onClick = { onFiltroChange(TipoExercicio.SIX_COUNT) },
            label = {
                Text(
                    text = "Six-Count ($totalSixCount)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("chip_filtro_six_count")
        )

        FilterChip(
            selected = filtroSelecionado == TipoExercicio.NAVY_SEAL,
            onClick = { onFiltroChange(TipoExercicio.NAVY_SEAL) },
            label = {
                Text(
                    text = "Navy Seal ($totalNavySeal)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .testTag("chip_filtro_navy_seal")
        )
    }
}

/**
 * Item de sessão no histórico com tipo, reps feitas, data, selo "importada", editar e excluir.
 */
@Composable
private fun SessaoItemCard(
    sessao: Sessao,
    onEditar: () -> Unit,
    onExcluir: () -> Unit
) {
    val dataFormatada = remember(sessao.dataHora, sessao.importada) {
        if (sessao.importada) {
            formatarDataApenas(sessao.dataHora)
        } else {
            formatarDataHora(sessao.dataHora)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_sessao_${sessao.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha superior: Tipo do exercício, data e selo "importada"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (sessao.tipo == TipoExercicio.SIX_COUNT)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (sessao.tipo == TipoExercicio.SIX_COUNT)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sessao.tipo.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dataFormatada,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Selo "importada" se aplicável
                if (sessao.importada) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("badge_importada_${sessao.id}")
                    ) {
                        Text(
                            text = "IMPORTADA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Linha de métricas: Reps feitas em destaque e duração
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${sessao.repsFeitas}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "reps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (sessao.repsPrevistas != null) {
                        Text(
                            text = "Meta: ${sessao.repsPrevistas} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val minutos = sessao.duracaoSegundos / 60
                        val segundos = sessao.duracaoSegundos % 60
                        val duracaoFormatada = "%02d:%02d".format(minutos, segundos)
                        Text(
                            text = duracaoFormatada,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Observação se houver (só ocupa espaço se houver observação)
            if (!sessao.observacao.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sessao.observacao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ações: Editar e Excluir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEditar,
                    modifier = Modifier.size(36.dp).testTag("btn_editar_sessao_${sessao.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar sessão",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onExcluir,
                    modifier = Modifier.size(36.dp).testTag("btn_excluir_sessao_${sessao.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir sessão",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de confirmação para exclusão de sessão.
 */
@Composable
private fun DialogConfirmarExclusao(
    sessao: Sessao,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    val dataTexto = remember(sessao.dataHora, sessao.importada) {
        if (sessao.importada) formatarDataApenas(sessao.dataHora) else formatarDataHora(sessao.dataHora)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = stringResource(R.string.dialog_delete_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = stringResource(
                    R.string.dialog_delete_confirm_msg,
                    stringResource(sessao.tipo.titleRes),
                    sessao.repsFeitas,
                    dataTexto
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirmar_exclusao")
            ) {
                Text(stringResource(R.string.btn_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancelar_exclusao")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * Diálogo para editar uma sessão já existente.
 */
@Composable
private fun DialogEditarSessao(
    sessao: Sessao,
    onSalvar: (Sessao) -> Unit,
    onDismiss: () -> Unit
) {
    var tipo by remember { mutableStateOf(sessao.tipo) }
    var repsTexto by remember { mutableStateOf(sessao.repsFeitas.toString()) }
    var observacao by remember { mutableStateOf(sessao.observacao ?: "") }

    var minutosTexto by remember { mutableStateOf((sessao.duracaoSegundos / 60).toString()) }
    var segundosTexto by remember { mutableStateOf("%02d".format(sessao.duracaoSegundos % 60)) }

    val repsValidas = repsTexto.toIntOrNull()?.let { it in 1..5000 } == true

    val validacaoDuracao = DuracaoValidator.validar(minutosTexto, segundosTexto)
    val duracaoValida = validacaoDuracao.ehValido
    val erroDuracaoMensagem = validacaoDuracao.mensagemErro
    val totalDuracaoSegundos = validacaoDuracao.totalSegundos

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.edit_session_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Seletor de tipo estilo chip
                Text(text = stringResource(R.string.field_exercise_type), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tipo == TipoExercicio.SIX_COUNT,
                        onClick = { tipo = TipoExercicio.SIX_COUNT },
                        label = { Text(stringResource(TipoExercicio.SIX_COUNT.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        border = BorderStroke(
                            1.dp,
                            if (tipo == TipoExercicio.SIX_COUNT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = tipo == TipoExercicio.NAVY_SEAL,
                        onClick = { tipo = TipoExercicio.NAVY_SEAL },
                        label = { Text(stringResource(TipoExercicio.NAVY_SEAL.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        border = BorderStroke(
                            1.dp,
                            if (tipo == TipoExercicio.NAVY_SEAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo Duração (Minutos e Segundos)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.field_duration_label), style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minutosTexto,
                            onValueChange = { novo ->
                                minutosTexto = novo.filter { it.isDigit() }.take(3)
                            },
                            label = { Text(stringResource(R.string.minutes_short)) },
                            placeholder = { Text("20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = !duracaoValida,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_editar_duracao_minutos")
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = segundosTexto,
                            onValueChange = { novo ->
                                segundosTexto = novo.filter { it.isDigit() }.take(2)
                            },
                            label = { Text(stringResource(R.string.seconds_short)) },
                            placeholder = { Text("00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = !duracaoValida,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_editar_duracao_segundos")
                        )
                    }
                    if (!duracaoValida && erroDuracaoMensagem != null) {
                        Text(
                            text = erroDuracaoMensagem,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Campo Reps Feitas
                OutlinedTextField(
                    value = repsTexto,
                    onValueChange = { novo -> repsTexto = novo.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.field_reps_completed)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = !repsValidas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_editar_reps")
                )

                // Campo Observação
                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it.take(500) },
                    label = { Text(stringResource(R.string.field_notes_optional)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_editar_observacao")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reps = repsTexto.toIntOrNull() ?: return@Button
                    if (!duracaoValida) return@Button
                    val atualizada = sessao.copy(
                        tipo = tipo,
                        repsFeitas = reps,
                        duracaoSegundos = totalDuracaoSegundos,
                        observacao = observacao.trim().ifEmpty { null }
                    )
                    onSalvar(atualizada)
                },
                enabled = repsValidas && duracaoValida,
                modifier = Modifier.testTag("btn_salvar_edicao")
            ) {
                Text(stringResource(R.string.btn_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

enum class OpcaoDataRelativa(val labelRes: Int) {
    HOJE(R.string.chip_today),
    ONTEM(R.string.chip_yesterday),
    OUTRA(R.string.choose_another_date)
}

/**
 * Diálogo para adicionar manualmente uma sessão passada (data, tipo, reps feitas).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DialogAdicionarSessaoPassada(
    onAdicionar: (dataHora: Long, tipo: TipoExercicio, repsFeitas: Int, observacao: String?, duracaoSegundos: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tipo by remember { mutableStateOf(TipoExercicio.SIX_COUNT) }
    var repsTexto by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }

    var minutosTexto by remember { mutableStateOf("20") }
    var segundosTexto by remember { mutableStateOf("00") }

    var opcaoData by remember { mutableStateOf(OpcaoDataRelativa.HOJE) }
    var dataHoraEscolhida by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val repsValidas = repsTexto.toIntOrNull()?.let { it in 1..5000 } == true

    val validacaoDuracao = DuracaoValidator.validar(minutosTexto, segundosTexto)
    val duracaoValida = validacaoDuracao.ehValido
    val erroDuracaoMensagem = validacaoDuracao.mensagemErro
    val totalDuracaoSegundos = validacaoDuracao.totalSegundos

    // Material3 DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dataHoraEscolhida
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                            val localCal = Calendar.getInstance().apply {
                                set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dataHoraEscolhida = localCal.timeInMillis
                            opcaoData = OpcaoDataRelativa.OUTRA
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_add_past_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Seletor de Data
                Text(text = stringResource(R.string.workout_date_label), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpcaoDataRelativa.entries.forEach { opcao ->
                        val isSelected = opcaoData == opcao
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                opcaoData = opcao
                                if (opcao == OpcaoDataRelativa.HOJE) {
                                    dataHoraEscolhida = System.currentTimeMillis()
                                } else if (opcao == OpcaoDataRelativa.ONTEM) {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                    dataHoraEscolhida = cal.timeInMillis
                                } else if (opcao == OpcaoDataRelativa.OUTRA) {
                                    showDatePicker = true
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(opcao.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Data selecionada formatada com acionamento do DatePicker ao tocar
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_escolher_outra_data")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(Date(dataHoraEscolhida)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.choose_another_date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.choose_another_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Seletor de Tipo com estilo chip e contorno
                Text(text = stringResource(R.string.field_exercise_type), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tipo == TipoExercicio.SIX_COUNT,
                        onClick = { tipo = TipoExercicio.SIX_COUNT },
                        label = { Text(stringResource(TipoExercicio.SIX_COUNT.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        border = BorderStroke(
                            1.dp,
                            if (tipo == TipoExercicio.SIX_COUNT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_adicionar_six_count")
                    )

                    FilterChip(
                        selected = tipo == TipoExercicio.NAVY_SEAL,
                        onClick = { tipo = TipoExercicio.NAVY_SEAL },
                        label = { Text(stringResource(TipoExercicio.NAVY_SEAL.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        border = BorderStroke(
                            1.dp,
                            if (tipo == TipoExercicio.NAVY_SEAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_adicionar_navy_seal")
                    )
                }

                // Campo Duração (Minutos e Segundos)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.field_duration_label), style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minutosTexto,
                            onValueChange = { novo ->
                                minutosTexto = novo.filter { it.isDigit() }.take(3)
                            },
                            label = { Text(stringResource(R.string.minutes_short)) },
                            placeholder = { Text("20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = !duracaoValida,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_adicionar_duracao_minutos")
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = segundosTexto,
                            onValueChange = { novo ->
                                segundosTexto = novo.filter { it.isDigit() }.take(2)
                            },
                            label = { Text(stringResource(R.string.seconds_short)) },
                            placeholder = { Text("00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = !duracaoValida,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_adicionar_duracao_segundos")
                        )
                    }
                    if (!duracaoValida && erroDuracaoMensagem != null) {
                        Text(
                            text = erroDuracaoMensagem,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Campo Reps Feitas
                OutlinedTextField(
                    value = repsTexto,
                    onValueChange = { novo -> repsTexto = novo.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.field_reps_completed_required)) },
                    placeholder = { Text(stringResource(R.string.placeholder_reps_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = repsTexto.isNotEmpty() && !repsValidas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_adicionar_reps")
                )

                // Campo Observação
                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it.take(500) },
                    label = { Text(stringResource(R.string.field_notes_optional)) },
                    placeholder = { Text(stringResource(R.string.placeholder_notes_example)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_adicionar_observacao")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reps = repsTexto.toIntOrNull() ?: return@Button
                    if (!duracaoValida) return@Button
                    onAdicionar(
                        dataHoraEscolhida,
                        tipo,
                        reps,
                        observacao.trim().ifEmpty { null },
                        totalDuracaoSegundos
                    )
                },
                enabled = repsValidas && duracaoValida,
                modifier = Modifier.testTag("btn_confirmar_adicionar_passada")
            ) {
                Text(stringResource(R.string.btn_add_session))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * Estado vazio quando não há treinos no histórico.
 */
@Composable
private fun EmptyHistoryView(
    filtroAtivo: Boolean,
    onAdicionarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .testTag("empty_state_history"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (filtroAtivo) "Nenhuma Sessão com Este Filtro" else "Nenhum Treino Registrado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (filtroAtivo) "Tente selecionar outro filtro ou adicione uma sessão passada."
                else "Conclua uma sessão de 20 minutos na aba Treino ou adicione uma sessão passada manualmente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAdicionarClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.testTag("btn_empty_adicionar_passada")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_add_past_session), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatarDataHora(timestamp: Long): String {
    val df = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
    return df.format(Date(timestamp))
}

private fun formatarDataApenas(timestamp: Long): String {
    val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
    return df.format(Date(timestamp))
}

@Preview(name = "HistoryScreen - Font 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "HistoryScreen - 360dp", widthDp = 360, showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    MaterialTheme {
        HistoryScreen()
    }
}
