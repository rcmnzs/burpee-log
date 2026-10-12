package com.example.burpeelog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.META_SEMANAL_MINUTOS
import com.example.burpeelog.data.model.META_SEMANAL_SESSOES
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.DashboardCalculator
import com.example.burpeelog.domain.DayHeatmapData
import com.example.burpeelog.domain.HeatmapMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

// 5 cores do Heatmap em tons de verde (estilo GitHub Dark)
private val HeatmapLevel0 = Color(0xFF1E222A) // Sem treino / cinza escuro
private val HeatmapLevel1 = Color(0xFF0E4429) // Até 25% do recorde
private val HeatmapLevel2 = Color(0xFF006D32) // Até 50% do recorde
private val HeatmapLevel3 = Color(0xFF26A641) // Até 75% do recorde
private val HeatmapLevel4 = Color(0xFF39D353) // Acima de 75% do recorde

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIniciarTreino: () -> Unit = {},
    onAbrirDados: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? BurpeeLogApplication
    val sessoes by (app?.repository?.allSessoes ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())

    val hoje = remember { LocalDate.now() }
    val anoAtual = hoje.year

    // Estado do Heatmap
    var modoHeatmap by remember { mutableStateOf(HeatmapMode.AMBOS) }
    var diaSelecionado by remember { mutableStateOf<DayHeatmapData?>(null) }

    // Cálculos de Resumo
    val minutosSemana = remember(sessoes, hoje) {
        DashboardCalculator.calcularMinutosSemanaAtual(sessoes, hoje)
    }
    val progressoSemana = (minutosSemana.toFloat() / META_SEMANAL_MINUTOS.toFloat()).coerceIn(0f, 1f)
    val sessoesSemana = minutosSemana / DURACAO_MINUTOS

    val streakSemanas = remember(sessoes, hoje) {
        DashboardCalculator.calcularStreakSemanas(sessoes, hoje)
    }

    val recordeSixCount = remember(sessoes) {
        DashboardCalculator.calcularRecordeSessao(sessoes, TipoExercicio.SIX_COUNT)
    }
    val recordeNavySeal = remember(sessoes) {
        DashboardCalculator.calcularRecordeSessao(sessoes, TipoExercicio.NAVY_SEAL)
    }

    val repsAnoSix = remember(sessoes, anoAtual) {
        DashboardCalculator.calcularTotalRepsNoAno(sessoes, anoAtual, TipoExercicio.SIX_COUNT)
    }
    val repsAnoNavy = remember(sessoes, anoAtual) {
        DashboardCalculator.calcularTotalRepsNoAno(sessoes, anoAtual, TipoExercicio.NAVY_SEAL)
    }

    // Estrutura das 53 semanas do Heatmap (últimos 12 meses)
    val semanasHeatmap = remember(sessoes, hoje) {
        gerarSemanasHeatmap(sessoes, hoje)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_home_dashboard"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Burpee Log",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Busy Dad Program • Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onAbrirDados,
                        modifier = Modifier.testTag("btn_abrir_dados")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações e Dados",
                            tint = MaterialTheme.colorScheme.onBackground
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Botão Grande "Iniciar Treino"
            Button(
                onClick = onIniciarTreino,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_iniciar_treino_dashboard"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Treino (20 min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. CARDS DE RESUMO (Acima do Mapa)
            // Card 2.1: Minutos da semana atual de 80
            CardMetaSemanal(
                minutosSemana = minutosSemana,
                sessoesSemana = sessoesSemana,
                progresso = progressoSemana
            )

            // Grid de Cards com Streak, Total no Ano e Recordes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sequência de semanas consecutivas (Streak)
                CardStreak(
                    streakSemanas = streakSemanas,
                    modifier = Modifier.weight(1f)
                )

                // Total de reps no ano por tipo
                CardTotalAno(
                    ano = anoAtual,
                    repsSix = repsAnoSix,
                    repsNavy = repsAnoNavy,
                    modifier = Modifier.weight(1f)
                )
            }

            // Card 2.3: Recorde de reps em uma sessão por tipo
            CardRecordes(
                recordeSix = recordeSixCount,
                recordeNavy = recordeNavySeal
            )

            // 3. MAPA DE CALOR (Estilo GitHub Contribution)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_heatmap_container"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "MAPA DE REPETIÇÕES (12 MESES)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    // Seletor Segmentado: "Six-Count", "Navy Seal", "Ambos"
                    SeletorModoHeatmap(
                        modoAtual = modoHeatmap,
                        onModoSelecionado = { modoHeatmap = it }
                    )

                    // Grade do Mapa de Calor com Scroll Horizontal iniciando na semana atual
                    HeatmapGrid(
                        semanas = semanasHeatmap,
                        modo = modoHeatmap,
                        recordeSix = recordeSixCount,
                        recordeNavy = recordeNavySeal,
                        diaSelecionado = diaSelecionado,
                        onDiaClicado = { diaSelecionado = it }
                    )

                    // Card de Detalhes do Dia Selecionado ao Tocar
                    diaSelecionado?.let { dia ->
                        DetalhesDiaSelecionadoCard(
                            dia = dia,
                            recordeSix = recordeSixCount,
                            recordeNavy = recordeNavySeal
                        )
                    }

                    // Legenda do Mapa de Calor
                    LegendaHeatmap(
                        modo = modoHeatmap,
                        recordeSix = recordeSixCount,
                        recordeNavy = recordeNavySeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Card de Meta Semanal (Segunda a Domingo).
 */
@Composable
private fun CardMetaSemanal(
    minutosSemana: Int,
    sessoesSemana: Int,
    progresso: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_meta_semanal_dashboard"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Semana Atual (Seg a Dom)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$minutosSemana / $META_SEMANAL_MINUTOS min ($sessoesSemana de $META_SEMANAL_SESSOES sessões)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(progresso * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progresso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("progress_minutos_semana"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Card exibindo a sequência de semanas consecutivas que bateram 80 min.
 */
@Composable
private fun CardStreak(
    streakSemanas: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.testTag("card_streak_semanas"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SEQUÊNCIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "$streakSemanas ${if (streakSemanas == 1) "semana" else "semanas"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Metas de 80 min seguidas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Card exibindo o total de reps no ano corrente por tipo.
 */
@Composable
private fun CardTotalAno(
    ano: Int,
    repsSix: Int,
    repsNavy: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.testTag("card_total_ano"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TOTAL EM $ano",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "6-Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "$repsSix", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(text = "Navy Seal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "$repsNavy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Card exibindo o recorde histórico em uma única sessão de 20 min por tipo.
 */
@Composable
private fun CardRecordes(
    recordeSix: Int,
    recordeNavy: Int
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_recordes_sessao"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recordes em 1 Sessão",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Máximo de reps em 20 min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "6-Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = if (recordeSix > 0) "$recordeSix reps" else "-", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Navy Seal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = if (recordeNavy > 0) "$recordeNavy reps" else "-", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Seletor segmentado para o Heatmap: Six-Count, Navy Seal, Ambos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorModoHeatmap(
    modoAtual: HeatmapMode,
    onModoSelecionado: (HeatmapMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("segmented_modo_heatmap")
    ) {
        HeatmapMode.entries.forEachIndexed { index, modo ->
            SegmentedButton(
                selected = modoAtual == modo,
                onClick = { onModoSelecionado(modo) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = HeatmapMode.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = modo.rotulo,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val MESES_3_LETRAS = arrayOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

/**
 * Grade do Mapa de Calor com rótulos de dias à esquerda e scroll horizontal
 * iniciando na semana atual (lado direito).
 */
@Composable
private fun HeatmapGrid(
    semanas: List<List<DayHeatmapData>>,
    modo: HeatmapMode,
    recordeSix: Int,
    recordeNavy: Int,
    diaSelecionado: DayHeatmapData?,
    onDiaClicado: (DayHeatmapData) -> Unit
) {
    val scrollState = rememberScrollState()

    // Rola para a semana atual automaticamente ao carregar
    LaunchedEffect(semanas.size) {
        if (semanas.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Rótulos de dias da semana (Segunda a Domingo) à esquerda
        Column(
            modifier = Modifier.padding(top = 21.dp, end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val diasRotulos = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
            diasRotulos.forEachIndexed { idx, label ->
                Box(
                    modifier = Modifier.size(width = 28.dp, height = 14.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (idx % 2 == 0) { // Mostrar Seg, Qua, Sex, Dom para manter limpo
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }

        // Colunas de semanas com rolagem horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .testTag("scroll_heatmap_semanas"),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            var ultimoMesRotulado: String? = null

            semanas.forEachIndexed { _, semana ->
                val primeiraDataSemana = semana.firstOrNull()?.date
                val mesNome = if (primeiraDataSemana != null) MESES_3_LETRAS[primeiraDataSemana.month.ordinal] else ""

                val deveMostrarMes = mesNome.isNotEmpty() && mesNome != ultimoMesRotulado && primeiraDataSemana?.dayOfMonth != null && primeiraDataSemana.dayOfMonth <= 7
                if (deveMostrarMes) {
                    ultimoMesRotulado = mesNome
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Rótulo do Mês no topo da coluna
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (deveMostrarMes) {
                            Text(
                                text = mesNome,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                            )
                        }
                    }

                    // 7 Quadradinhos arredondados (Segunda a Domingo)
                    semana.forEach { dia ->
                        val nivel = DashboardCalculator.calcularNivel(
                            repsSix = dia.repsSixCount,
                            repsNavy = dia.repsNavySeal,
                            recordeSix = recordeSix,
                            recordeNavy = recordeNavy,
                            modo = modo
                        )

                        val cor = when (nivel) {
                            1 -> HeatmapLevel1
                            2 -> HeatmapLevel2
                            3 -> HeatmapLevel3
                            4 -> HeatmapLevel4
                            else -> HeatmapLevel0
                        }

                        val isSelected = diaSelecionado?.date == dia.date

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cor)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                    } else Modifier
                                )
                                .clickable { onDiaClicado(dia) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card informativo exibido ao tocar em um quadradinho do heatmap.
 */
@Composable
private fun DetalhesDiaSelecionadoCard(
    dia: DayHeatmapData,
    recordeSix: Int,
    recordeNavy: Int
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")) }
    val dataTexto = dia.date.format(formatter).replaceFirstChar { it.uppercase() }

    val pctSix = if (recordeSix > 0) ((dia.repsSixCount.toFloat() / recordeSix.toFloat()) * 100).toInt() else 0
    val pctNavy = if (recordeNavy > 0) ((dia.repsNavySeal.toFloat() / recordeNavy.toFloat()) * 100).toInt() else 0

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_detalhes_dia_selecionado"),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dataTexto,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (dia.totalReps > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Six-Count: ${dia.repsSixCount} reps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (recordeSix > 0) "$pctSix% do recorde ($recordeSix)" else "Sem recorde",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Navy Seal: ${dia.repsNavySeal} reps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (recordeNavy > 0) "$pctNavy% do recorde ($recordeNavy)" else "Sem recorde",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Text(
                    text = "Nenhum treino realizado neste dia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Legenda "Menos ... Mais" e faixas calculadas em reps.
 */
@Composable
private fun LegendaHeatmap(
    modo: HeatmapMode,
    recordeSix: Int,
    recordeNavy: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Menos",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))

            listOf(
                HeatmapLevel0,
                HeatmapLevel1,
                HeatmapLevel2,
                HeatmapLevel3,
                HeatmapLevel4
            ).forEach { cor ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(cor)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }

            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Mais",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Faixas de reps correspondentes no modo ativo
        val faixasTexto = DashboardCalculator.gerarFaixasLegenda(modo, recordeSix, recordeNavy)
        Text(
            text = "Faixas de reps: $faixasTexto",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Gera a grade de 53 semanas (colunas de 7 dias de Segunda a Domingo) para o Heatmap.
 */
private fun gerarSemanasHeatmap(
    sessoes: List<Sessao>,
    hoje: LocalDate
): List<List<DayHeatmapData>> {
    // Mapa acumulado por dia para busca rápida
    val mapaRepsPorDia = HashMap<LocalDate, Pair<Int, Int>>() // Pair(repsSix, repsNavy)
    for (sessao in sessoes) {
        val data = DashboardCalculator.timestampToLocalDate(sessao.dataHora)
        val atual = mapaRepsPorDia[data] ?: Pair(0, 0)
        val novo = if (sessao.tipo == TipoExercicio.SIX_COUNT) {
            Pair(atual.first + sessao.repsFeitas, atual.second)
        } else {
            Pair(atual.first, atual.second + sessao.repsFeitas)
        }
        mapaRepsPorDia[data] = novo
    }

    val segundaSemanaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val inicioSegunda = segundaSemanaAtual.minusWeeks(52) // 52 semanas atrás

    val semanas = ArrayList<List<DayHeatmapData>>(53)
    for (w in 0..52) {
        val segundaDaSemana = inicioSegunda.plusWeeks(w.toLong())
        val diasSemana = ArrayList<DayHeatmapData>(7)
        for (d in 0..6) {
            val dataDia = segundaDaSemana.plusDays(d.toLong())
            val reps = mapaRepsPorDia[dataDia] ?: Pair(0, 0)
            diasSemana.add(
                DayHeatmapData(
                    date = dataDia,
                    repsSixCount = reps.first,
                    repsNavySeal = reps.second
                )
            )
        }
        semanas.add(diasSemana)
    }

    return semanas
}

@Preview(name = "HomeScreen - Font 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "HomeScreen - 360dp", widthDp = 360, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
