package com.example.burpeelog.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.service.WorkoutActiveState
import com.example.burpeelog.service.WorkoutPhase
import com.example.burpeelog.service.WorkoutSessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutActiveScreen(
    onNavigateToSummary: (repsFeitas: Int, repsPrevistas: Int, duracaoSegundos: Int, tipo: com.example.burpeelog.data.model.TipoExercicio, padrao: List<Int>) -> Unit,
    onCancelWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BurpeeLogApplication
    val sessionManager: WorkoutSessionManager = app.workoutSessionManager
    val state by sessionManager.state.collectAsStateWithLifecycle()

    var showEncerrarDialog by remember { mutableStateOf(false) }
    var isDiscarding by remember { mutableStateOf(false) }

    // Manter a tela ligada enquanto o treino estiver ativo nesta tela
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Interceptar botão voltar físico do Android para exibir confirmação
    BackHandler(enabled = state.phase == WorkoutPhase.RUNNING || state.phase == WorkoutPhase.PAUSED) {
        showEncerrarDialog = true
    }

    // Ao concluir os 20 minutos, ir automaticamente para a tela de Resumo
    LaunchedEffect(state.phase) {
        if (!isDiscarding && (state.phase == WorkoutPhase.FINISHED || state.phase == WorkoutPhase.COMPLETED)) {
            onNavigateToSummary(
                state.repsFeitasCalculadas.takeIf { it > 0 } ?: state.repsCompletadas,
                state.totalRepsPrevistas,
                state.tempoDecorridoSegundos.takeIf { it > 0 } ?: state.elapsedSeconds,
                state.tipo,
                state.padrao
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_workout_active"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.phase == WorkoutPhase.PAUSED) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.phase == WorkoutPhase.PAUSED) "Treino Pausado" else "Treino em Andamento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Text(
                        text = state.tipo.titulo.substringBefore(" "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.phase) {
                WorkoutPhase.COUNTDOWN -> {
                    // Contagem regressiva de 5 segundos
                    CountdownView(countdown = state.countdownSeconds, tipoTitulo = state.tipo.titulo)
                }
                WorkoutPhase.RUNNING, WorkoutPhase.PAUSED, WorkoutPhase.FINISHED, WorkoutPhase.COMPLETED, WorkoutPhase.STOPPED_EARLY -> {
                    // Tela principal do treino ativo
                    ActiveWorkoutView(
                        state = state,
                        onPauseToggle = {
                            if (state.phase == WorkoutPhase.RUNNING) {
                                sessionManager.pauseWorkout()
                            } else {
                                sessionManager.resumeWorkout()
                            }
                        },
                        onEncerrarClick = { showEncerrarDialog = true }
                    )
                }
                WorkoutPhase.IDLE -> {
                    // Estado ocioso (caso tenha sido resetado)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum treino em andamento", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Diálogo de confirmação de encerramento com três opções
    if (showEncerrarDialog) {
        val tempoDecorrido = state.tempoDecorridoSegundos.takeIf { it > 0 } ?: state.elapsedSeconds
        val repsFeitas = state.repsFeitasCalculadas.takeIf { it > 0 } ?: state.currentRepDisplay
        val minutos = tempoDecorrido / 60
        val segundos = tempoDecorrido % 60

        AlertDialog(
            onDismissRequest = { showEncerrarDialog = false },
            modifier = Modifier.testTag("dialog_encerrar_treino"),
            icon = {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Encerrar Treino",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempo realizado: ${minutos}m ${segundos}s (${state.tempoFormatado} restantes)."
                    )
                    Text(
                        text = "Repetições feitas: $repsFeitas de ${state.totalRepsPrevistas} reps."
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escolha como deseja prosseguir com a sessão:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Opção 1: Salvar parcial
                    Button(
                        onClick = {
                            showEncerrarDialog = false
                            sessionManager.finalizarSessao(isEarly = true)
                            val finalState = sessionManager.state.value
                            onNavigateToSummary(
                                finalState.repsFeitasCalculadas,
                                finalState.totalRepsPrevistas,
                                finalState.tempoDecorridoSegundos,
                                finalState.tipo,
                                finalState.padrao
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_salvar_parcial")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salvar parcial")
                    }

                    // Opção 2: Descartar treino
                    OutlinedButton(
                        onClick = {
                            isDiscarding = true
                            showEncerrarDialog = false
                            sessionManager.finalizarSessao(isEarly = false)
                            onCancelWorkout()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_descartar_treino")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descartar treino")
                    }

                    // Opção 3: Continuar treinando
                    TextButton(
                        onClick = { showEncerrarDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_continuar_treinando")
                    ) {
                        Text("Continuar treinando")
                    }
                }
            },
            dismissButton = null
        )
    }
}

/**
 * Visualização da Contagem Regressiva de 5 segundos.
 */
@Composable
private fun CountdownView(countdown: Int, tipoTitulo: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PREPARE-SE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tipoTitulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Círculo grande com o número 5..1
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = countdown, label = "countdown_anim") { count ->
                    Text(
                        text = "$count",
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("txt_countdown_number")
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "A primeira repetição começa no segundo 0!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Visualização do Treino Ativo com cronômetro grande, Rep X de Y, indicador de próxima rep e controles.
 */
@Composable
private fun ActiveWorkoutView(
    state: WorkoutActiveState,
    onPauseToggle: () -> Unit,
    onEncerrarClick: () -> Unit
) {
    val animatedOverallProgress by animateFloatAsState(
        targetValue = state.progressOverall,
        label = "overall_progress_anim"
    )

    val animatedRepProgress by animateFloatAsState(
        targetValue = state.nextRepProgress,
        label = "next_rep_progress_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Barra de progresso geral dos 20 minutos
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Minuto ${state.currentMinute} de $DURACAO_MINUTOS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(state.progressOverall * 100).toInt()}% concluído",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedOverallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("progress_overall_20min"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        // Bloco do Cronômetro Regressivo de 20 minutos (mm:ss)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("card_cronometro_principal"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TEMPO RESTANTE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Cronômetro regressivo mm:ss (legível, mantendo hierarquia com o timer de repetições em destaque)
                Text(
                    text = state.tempoFormatado,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (state.phase == WorkoutPhase.PAUSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.testTag("txt_cronometro_grande")
                )

                if (state.phase == WorkoutPhase.PAUSED) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PAUSADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Bloco de Destaque da Próxima Repetição e Contagem de Repetições
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_rep_counter"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header da Contagem de Repetições
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONTAGEM DE REPETIÇÕES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Rep ${state.currentRepDisplay} de ${state.totalRepsPrevistas}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("txt_rep_x_de_y")
                        )
                    }
                    Text(
                        text = if (state.secondsUntilNextRep > 0) {
                            "Próxima rep em ${state.secondsUntilNextRep}s"
                        } else {
                            "Última repetição!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("txt_proxima_rep_em")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Elemento em destaque máximo: Timer regressivo da próxima repetição (~dobro do tamanho anterior de 80.dp -> 164.dp)
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .testTag("indicator_next_rep_progress"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedRepProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${state.secondsUntilNextRep}s",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "PRÓXIMA REP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Botões de Ação: Pausar/Retomar e Encerrar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Botão Pausar / Retomar
            Button(
                onClick = onPauseToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("btn_pausar_retomar"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.phase == WorkoutPhase.PAUSED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (state.phase == WorkoutPhase.PAUSED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (state.phase == WorkoutPhase.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.phase == WorkoutPhase.PAUSED) "Retomar" else "Pausar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botão Encerrar
            OutlinedButton(
                onClick = onEncerrarClick,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("btn_encerrar"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Encerrar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
