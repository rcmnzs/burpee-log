package com.example.burpeelog.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.IntervaloCalculator
import com.example.burpeelog.service.WorkoutPhase

@Composable
fun WorkoutScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BurpeeLogApplication
    val sessionManager = app.workoutSessionManager
    val activeState by sessionManager.state.collectAsStateWithLifecycle()

    when (activeState.phase) {
        WorkoutPhase.COUNTDOWN, WorkoutPhase.RUNNING, WorkoutPhase.PAUSED -> {
            WorkoutActiveScreen(
                onNavigateToSummary = { repsFeitas, repsPrevistas, duracaoSegundos, tipo, padrao ->
                    // O sessionManager transiciona para FINISHED ou STOPPED_EARLY
                },
                onCancelWorkout = {
                    sessionManager.reset()
                },
                modifier = modifier
            )
        }
        WorkoutPhase.FINISHED, WorkoutPhase.STOPPED_EARLY -> {
            WorkoutSummaryScreen(
                tipo = activeState.tipo,
                padrao = activeState.padrao,
                repsFeitasInicial = activeState.repsCompletadas,
                repsPrevistas = activeState.totalRepsPrevistas,
                duracaoSegundos = activeState.elapsedSeconds,
                onFinish = {
                    sessionManager.reset()
                },
                modifier = modifier
            )
        }
        WorkoutPhase.IDLE -> {
            WorkoutConfigContent(
                onIniciarTreino = { tipo, padrao ->
                    sessionManager.startWorkout(tipo, padrao)
                },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutConfigContent(
    onIniciarTreino: (TipoExercicio, List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutConfigViewModel? = run {
        val app = LocalContext.current.applicationContext as? BurpeeLogApplication
        if (app != null) {
            viewModel(
                factory = WorkoutConfigViewModel.Factory(
                    repository = app.repository,
                    patternPreferences = app.patternPreferences
                )
            )
        } else null
    }
) {
    val uiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: WorkoutConfigUiState()
    var editingIndexForDialog by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Inicia o treino independente da resposta para nunca bloquear o usuário
        viewModel?.iniciarTreino()
        onIniciarTreino(uiState.tipoSelecionado, uiState.padraoIntervalos)
    }

    fun dispararInicioTreino() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val temPermissao = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!temPermissao) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel?.iniciarTreino()
        onIniciarTreino(uiState.tipoSelecionado, uiState.padraoIntervalos)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_workout_config"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Configurar Sessão",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Busy Dad Program • $DURACAO_MINUTOS minutos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Botão Iniciar fixo na parte inferior
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { dispararInicioTreino() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_iniciar"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar (${uiState.repsPrevistas} reps previstas)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Duração Fixa do Busy Dad Program (Informativo apenas)
            item {
                DuracaoFixaInfoCard()
            }

            // 2. Seletor do Tipo (Six-Count ou Navy Seal com sugestão automática de alternância)
            item {
                SeletorTipoExercicio(
                    tipoSelecionado = uiState.tipoSelecionado,
                    tipoSugerido = uiState.tipoSugerido,
                    ultimaSessao = uiState.ultimaSessao,
                    onTipoSelecionado = { viewModel?.selecionarTipo(it) }
                )
            }

            // 3. Resumo em Tempo Real e Prévia do Ciclo
            item {
                ResumoPreviaCard(
                    repsPrevistas = uiState.repsPrevistas,
                    previaCiclo = uiState.previaCiclo,
                    tamanhoCicloMinutos = uiState.padraoIntervalos.size
                )
            }

            // 4. Cabeçalho da Lista de Intervalos
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Padrão de Intervalos por Minuto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Ciclo de ${uiState.padraoIntervalos.size} min (repete até 20 min)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { viewModel?.adicionarMinuto() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_adicionar_minuto"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Adicionar Minuto",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. Itens da Lista de Intervalos
            itemsIndexed(
                items = uiState.padraoIntervalos,
                key = { index, _ -> "intervalo_item_$index" }
            ) { index, intervaloSegundos ->
                ItemIntervaloCard(
                    index = index,
                    totalItens = uiState.padraoIntervalos.size,
                    intervaloSegundos = intervaloSegundos,
                    onAlterarIntervalo = { novoValor -> viewModel?.alterarIntervalo(index, novoValor) },
                    onMoverParaCima = { viewModel?.moverMinutoParaCima(index) },
                    onMoverParaBaixo = { viewModel?.moverMinutoParaBaixo(index) },
                    onRemover = { viewModel?.removerMinuto(index) },
                    onAbrirEdicaoManual = { editingIndexForDialog = index }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Diálogo de Edição Manual de Segundos
    editingIndexForDialog?.let { idx ->
        if (idx in uiState.padraoIntervalos.indices) {
            DialogAjusteSegundos(
                minutoNumero = idx + 1,
                valorAtual = uiState.padraoIntervalos[idx],
                onConfirmar = { novoValor ->
                    viewModel?.alterarIntervalo(idx, novoValor)
                    editingIndexForDialog = null
                },
                onDismiss = { editingIndexForDialog = null }
            )
        }
    }
}

/**
 * Card exibindo a duração fixa oficial de 20 minutos.
 */
@Composable
private fun DuracaoFixaInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_duracao_fixa"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Duração Fixa: $DURACAO_MINUTOS Minutos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PADRÃO",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "Regra Busy Dad Program: sessões de 20 min sem duração editável.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Seletor de Tipo de Exercício com alternância inteligente baseada na última sessão.
 */
@Composable
private fun SeletorTipoExercicio(
    tipoSelecionado: TipoExercicio,
    tipoSugerido: TipoExercicio,
    ultimaSessao: com.example.burpeelog.data.model.Sessao?,
    onTipoSelecionado: (TipoExercicio) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tipo de Exercício",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            val textoSugestao = when {
                ultimaSessao == null -> "Sugestão inicial"
                else -> "Alternância da última (${ultimaSessao.tipo.titulo.substringBefore(" ")})"
            }
            Text(
                text = textoSugestao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TipoExercicioCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_tipo_six_count"),
                tipo = TipoExercicio.SIX_COUNT,
                selecionado = tipoSelecionado == TipoExercicio.SIX_COUNT,
                isSugerido = tipoSugerido == TipoExercicio.SIX_COUNT,
                onClick = { onTipoSelecionado(TipoExercicio.SIX_COUNT) }
            )

            TipoExercicioCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_tipo_navy_seal"),
                tipo = TipoExercicio.NAVY_SEAL,
                selecionado = tipoSelecionado == TipoExercicio.NAVY_SEAL,
                isSugerido = tipoSugerido == TipoExercicio.NAVY_SEAL,
                onClick = { onTipoSelecionado(TipoExercicio.NAVY_SEAL) }
            )
        }
    }
}

@Composable
private fun TipoExercicioCard(
    modifier: Modifier = Modifier,
    tipo: TipoExercicio,
    selecionado: Boolean,
    isSugerido: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selecionado) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val backgroundColor = if (selecionado) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    OutlinedCard(
        modifier = modifier.clickable { onClick() },
        border = BorderStroke(if (selecionado) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (tipo == TipoExercicio.SIX_COUNT) "Six-Count" else "Navy Seal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSugerido) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SUGERIDO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = if (tipo == TipoExercicio.SIX_COUNT) "6 tempos • 1 flexão" else "Militar • 3 flexões",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card exibindo o total previsto de reps e a prévia formatada do ciclo.
 */
@Composable
private fun ResumoPreviaCard(
    repsPrevistas: Int,
    previaCiclo: String,
    tamanhoCicloMinutos: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_resumo_previa"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL PREVISTO (20 MIN)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$repsPrevistas reps",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("txt_total_reps_previstas")
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val mediaPorMinuto = String.format("%.1f", repsPrevistas / 20.0)
                    Text(
                        text = "RITMO MÉDIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$mediaPorMinuto /min",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Prévia do Ciclo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prévia do Ciclo ($tamanhoCicloMinutos ${if (tamanhoCicloMinutos == 1) "minuto" else "minutos"}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = previaCiclo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("txt_previa_ciclo")
                )
            }
        }
    }
}

/**
 * Item individual que representa 1 minuto do ciclo de intervalos.
 */
@Composable
private fun ItemIntervaloCard(
    index: Int,
    totalItens: Int,
    intervaloSegundos: Int,
    onAlterarIntervalo: (Int) -> Unit,
    onMoverParaCima: () -> Unit,
    onMoverParaBaixo: () -> Unit,
    onRemover: () -> Unit,
    onAbrirEdicaoManual: () -> Unit
) {
    val repsNoMinuto = IntervaloCalculator.calcularRepsNoMinuto(intervaloSegundos)
    val instantes = IntervaloCalculator.calcularInstantesDoMinuto(intervaloSegundos)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_intervalo_item_$index"),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Minuto ${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$repsNoMinuto ${if (repsNoMinuto == 1) "rep" else "reps"} (em ${instantes.joinToString("s, ")}s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoverParaCima,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_mover_cima_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Mover para cima",
                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = onMoverParaBaixo,
                        enabled = index < totalItens - 1,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_mover_baixo_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Mover para baixo",
                            tint = if (index < totalItens - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = onRemover,
                        enabled = totalItens > 1,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_remover_item_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover minuto",
                            tint = if (totalItens > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val canFitInOneRow = maxWidth >= 300.dp
                if (canFitInOneRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onAlterarIntervalo(intervaloSegundos - 5) },
                            enabled = intervaloSegundos > IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_minus_5_$index"),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-5s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        FilledTonalButton(
                            onClick = { onAlterarIntervalo(intervaloSegundos - 1) },
                            enabled = intervaloSegundos > IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_minus_1_$index"),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-1s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Surface(
                            onClick = onAbrirEdicaoManual,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_valor_intervalo_$index")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${intervaloSegundos}s",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar valor",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { onAlterarIntervalo(intervaloSegundos + 1) },
                            enabled = intervaloSegundos < IntervaloCalculator.INTERVALO_MAX_SEGUNDOS,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_plus_1_$index"),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+1s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        FilledTonalButton(
                            onClick = { onAlterarIntervalo(intervaloSegundos + 5) },
                            enabled = intervaloSegundos < IntervaloCalculator.INTERVALO_MAX_SEGUNDOS,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_plus_5_$index"),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onAlterarIntervalo(intervaloSegundos - 5) },
                                enabled = intervaloSegundos > IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_minus_5_$index"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("-5s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                            Surface(
                                onClick = onAbrirEdicaoManual,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_valor_intervalo_$index")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${intervaloSegundos}s",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }

                            FilledTonalButton(
                                onClick = { onAlterarIntervalo(intervaloSegundos + 5) },
                                enabled = intervaloSegundos < IntervaloCalculator.INTERVALO_MAX_SEGUNDOS,
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_plus_5_$index"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("+5s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onAlterarIntervalo(intervaloSegundos - 1) },
                                enabled = intervaloSegundos > IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_minus_1_$index"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("-1s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                            FilledTonalButton(
                                onClick = { onAlterarIntervalo(intervaloSegundos + 1) },
                                enabled = intervaloSegundos < IntervaloCalculator.INTERVALO_MAX_SEGUNDOS,
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_plus_1_$index"),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("+1s", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogAjusteSegundos(
    minutoNumero: Int,
    valorAtual: Int,
    onConfirmar: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textoInput by remember { mutableStateOf(valorAtual.toString()) }
    val presets = listOf(10, 15, 20, 25, 30, 40, 60)

    val valorParsed = textoInput.toIntOrNull()
    val isValido = valorParsed != null && valorParsed in IntervaloCalculator.INTERVALO_MIN_SEGUNDOS..IntervaloCalculator.INTERVALO_MAX_SEGUNDOS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Intervalo do Minuto $minutoNumero", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Defina o tempo em segundos (entre 2 e 60 s):",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = textoInput,
                    onValueChange = { textoInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Segundos") },
                    suffix = { Text("s") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValido,
                    supportingText = {
                        if (!isValido) {
                            Text("Digite um número entre 2 e 60")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_segundos_dialog")
                )

                Text(
                    text = "Valores rápidos:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(4).forEach { preset ->
                        SuggestionChip(
                            onClick = { textoInput = preset.toString() },
                            label = { Text("${preset}s") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.drop(4).forEach { preset ->
                        SuggestionChip(
                            onClick = { textoInput = preset.toString() },
                            label = { Text("${preset}s") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valorParsed != null && isValido) {
                        onConfirmar(valorParsed)
                    }
                },
                enabled = isValido,
                modifier = Modifier.testTag("btn_confirmar_segundos")
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(name = "WorkoutScreen - Font 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "WorkoutScreen - 360dp", widthDp = 360, showBackground = true)
@Composable
private fun WorkoutScreenPreview() {
    MaterialTheme {
        WorkoutConfigContent(
            onIniciarTreino = { _, _ -> }
        )
    }
}

