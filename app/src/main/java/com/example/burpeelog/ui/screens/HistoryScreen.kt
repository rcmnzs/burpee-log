package com.example.burpeelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.example.burpeelog.ui.components.BurpeeHistoryLineChart
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = run {
        val app = LocalContext.current.applicationContext as BurpeeLogApplication
        viewModel(factory = HistoryViewModel.Factory(app.repository))
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                actions = {
                    IconButton(
                        onClick = { showDialogAdicionarPassada = true },
                        modifier = Modifier.testTag("btn_adicionar_sessao_passada_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar treino passado",
                            tint = MaterialTheme.colorScheme.primary
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
                text = { Text("Treino Passado") },
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
                onFiltroChange = { viewModel.definirFiltro(it) }
            )

            // Gráfico de linha com reps feitas por sessão (Six-Count e Navy Seal, filtrável 30d, 90d, tudo)
            if (uiState.todasSessoes.isNotEmpty()) {
                BurpeeHistoryLineChart(
                    sessoes = uiState.todasSessoes,
                    periodoSelecionado = uiState.periodoGrafico,
                    onPeriodoChange = { viewModel.definirPeriodoGrafico(it) }
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

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
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
                viewModel.excluirSessao(sessao)
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
                viewModel.atualizarSessao(sessaoAtualizada)
                sessaoParaEditar = null
            },
            onDismiss = { sessaoParaEditar = null }
        )
    }

    // Diálogo para Adicionar Manualmente Sessão Passada
    if (showDialogAdicionarPassada) {
        DialogAdicionarSessaoPassada(
            onAdicionar = { dataHora, tipo, reps, obs ->
                viewModel.adicionarSessaoPassada(dataHora, tipo, reps, obs)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filtroSelecionado == null,
            onClick = { onFiltroChange(null) },
            label = { Text("Todos ($totalGeral)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("chip_filtro_todos")
        )

        FilterChip(
            selected = filtroSelecionado == TipoExercicio.SIX_COUNT,
            onClick = { onFiltroChange(TipoExercicio.SIX_COUNT) },
            label = { Text("Six-Count ($totalSixCount)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("chip_filtro_six_count")
        )

        FilterChip(
            selected = filtroSelecionado == TipoExercicio.NAVY_SEAL,
            onClick = { onFiltroChange(TipoExercicio.NAVY_SEAL) },
            label = { Text("Navy Seal ($totalNavySeal)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("chip_filtro_navy_seal")
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
    val dataFormatada = remember(sessao.dataHora) { formatarDataHora(sessao.dataHora) }

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

                    Column {
                        Text(
                            text = sessao.tipo.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dataFormatada,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selo "importada" se aplicável
                if (sessao.importada) {
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
                        Text(
                            text = "$DURACAO_MINUTOS min",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Observação se houver
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
                    modifier = Modifier.testTag("btn_editar_sessao_${sessao.id}")
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
                    modifier = Modifier.testTag("btn_excluir_sessao_${sessao.id}")
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
    val dataTexto = remember(sessao.dataHora) { formatarDataHora(sessao.dataHora) }

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
            Text(text = "Excluir Sessão?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "Tem certeza de que deseja excluir a sessão de ${sessao.tipo.titulo} (${sessao.repsFeitas} reps) realizada em $dataTexto? Esta ação não pode ser desfeita."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirmar_exclusao")
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancelar_exclusao")
            ) {
                Text("Cancelar")
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

    val repsValidas = repsTexto.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Editar Sessão", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Seletor de tipo
                Text(text = "Tipo de Exercício:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { tipo = TipoExercicio.SIX_COUNT },
                        label = { Text("Six-Count") },
                        border = if (tipo == TipoExercicio.SIX_COUNT)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier.weight(1f)
                    )
                    SuggestionChip(
                        onClick = { tipo = TipoExercicio.NAVY_SEAL },
                        label = { Text("Navy Seal") },
                        border = if (tipo == TipoExercicio.NAVY_SEAL)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo Reps Feitas
                OutlinedTextField(
                    value = repsTexto,
                    onValueChange = { novo -> repsTexto = novo.filter { it.isDigit() } },
                    label = { Text("Repetições Feitas") },
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
                    onValueChange = { observacao = it },
                    label = { Text("Observação (opcional)") },
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
                    val atualizada = sessao.copy(
                        tipo = tipo,
                        repsFeitas = reps,
                        observacao = observacao.trim().ifEmpty { null }
                    )
                    onSalvar(atualizada)
                },
                enabled = repsValidas,
                modifier = Modifier.testTag("btn_salvar_edicao")
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para adicionar manualmente uma sessão passada (data, tipo, reps feitas).
 */
@Composable
private fun DialogAdicionarSessaoPassada(
    onAdicionar: (dataHora: Long, tipo: TipoExercicio, repsFeitas: Int, observacao: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var tipo by remember { mutableStateOf(TipoExercicio.SIX_COUNT) }
    var repsTexto by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }

    // Seleção de data relativa fácil e intuitiva
    var offsetDias by remember { mutableIntStateOf(0) } // 0 = Hoje, 1 = Ontem, etc.

    val dataHoraEscolhida = remember(offsetDias) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -offsetDias)
        calendar.timeInMillis
    }

    val repsValidas = repsTexto.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Adicionar Sessão Passada", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Seletor de Data
                Text(text = "Data do Treino:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "Hoje",
                        1 to "Ontem",
                        2 to "2 dias atrás",
                        3 to "3 dias atrás"
                    ).forEach { (dias, rotulo) ->
                        SuggestionChip(
                            onClick = { offsetDias = dias },
                            label = { Text(rotulo, fontSize = 11.sp) },
                            border = if (offsetDias == dias)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null,
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (offsetDias == dias)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                // Data selecionada formatada
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.forLanguageTag("pt-BR")).format(Date(dataHoraEscolhida)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Seletor de Tipo
                Text(text = "Tipo de Exercício:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { tipo = TipoExercicio.SIX_COUNT },
                        label = { Text("Six-Count") },
                        border = if (tipo == TipoExercicio.SIX_COUNT)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_adicionar_six_count")
                    )
                    SuggestionChip(
                        onClick = { tipo = TipoExercicio.NAVY_SEAL },
                        label = { Text("Navy Seal") },
                        border = if (tipo == TipoExercicio.NAVY_SEAL)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_adicionar_navy_seal")
                    )
                }

                // Campo Reps Feitas
                OutlinedTextField(
                    value = repsTexto,
                    onValueChange = { novo -> repsTexto = novo.filter { it.isDigit() } },
                    label = { Text("Repetições Feitas *") },
                    placeholder = { Text("Ex.: 45") },
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
                    onValueChange = { observacao = it },
                    label = { Text("Observação (opcional)") },
                    placeholder = { Text("Ex.: Treino registrado após o parque") },
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
                    onAdicionar(dataHoraEscolhida, tipo, reps, observacao)
                },
                enabled = repsValidas,
                modifier = Modifier.testTag("btn_confirmar_adicionar_passada")
            ) {
                Text("Adicionar Sessão")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
                Text("Adicionar Sessão Passada", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatarDataHora(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
    return sdf.format(Date(timestamp))
}
