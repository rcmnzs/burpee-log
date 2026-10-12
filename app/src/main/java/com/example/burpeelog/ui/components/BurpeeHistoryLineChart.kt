package com.example.burpeelog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.ChartPeriodFilter
import com.example.burpeelog.domain.ChartPoint
import com.example.burpeelog.domain.HistoryChartCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BurpeeHistoryLineChart(
    sessoes: List<Sessao>,
    periodoSelecionado: ChartPeriodFilter,
    onPeriodoChange: (ChartPeriodFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessoesFiltradas = remember(sessoes, periodoSelecionado) {
        HistoryChartCalculator.filtrarSessoesPorPeriodo(sessoes, periodoSelecionado)
    }

    val pontosSixCount = remember(sessoesFiltradas) {
        HistoryChartCalculator.prepararPontosGrafico(sessoesFiltradas, TipoExercicio.SIX_COUNT)
    }

    val pontosNavySeal = remember(sessoesFiltradas) {
        HistoryChartCalculator.prepararPontosGrafico(sessoesFiltradas, TipoExercicio.NAVY_SEAL)
    }

    val maxReps = remember(pontosSixCount, pontosNavySeal) {
        HistoryChartCalculator.calcularMaxReps(pontosSixCount, pontosNavySeal)
    }

    var pontoSelecionado by remember { mutableStateOf<ChartPoint?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_history_chart"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho do Gráfico com Filtros 30 dias / 90 dias / Tudo
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Linha do Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Evolução por Sessão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Reps por sessão de 20 min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Legenda de cores em linha própria abaixo do título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendaItem(
                        cor = Color(0xFF22C55E), // Verde Six-Count
                        nome = "Six-Count"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendaItem(
                        cor = Color(0xFF38BDF8), // Azul/Ciano Navy Seal
                        nome = "Navy Seal"
                    )
                }
            }

            // Barra de Filtro de Período
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartPeriodFilter.values().forEach { periodo ->
                    val isSelected = periodo == periodoSelecionado
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            pontoSelecionado = null
                            onPeriodoChange(periodo)
                        },
                        label = {
                            Text(
                                text = periodo.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("chip_chart_filter_${periodo.name.lowercase()}")
                    )
                }
            }

            // Exibição do Canvas do Gráfico ou Estado Vazio
            if (pontosSixCount.isEmpty() && pontosNavySeal.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma sessão registrada no período selecionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Área interativa do gráfico
                val corSix = Color(0xFF22C55E)
                val corNavy = Color(0xFF38BDF8)
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    // Canvas de desenho das linhas e pontos
                    val hitTargets = remember { mutableListOf<Pair<Offset, ChartPoint>>() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pontosSixCount, pontosNavySeal, maxReps) {
                                detectTapGestures { tapOffset ->
                                    // Localizar o ponto mais próximo do toque
                                    val closest = hitTargets.minByOrNull { (pointOffset, _) ->
                                        val dx = pointOffset.x - tapOffset.x
                                        val dy = pointOffset.y - tapOffset.y
                                        dx * dx + dy * dy
                                    }
                                    if (closest != null) {
                                        val distSq = (closest.first.x - tapOffset.x) * (closest.first.x - tapOffset.x) +
                                                (closest.first.y - tapOffset.y) * (closest.first.y - tapOffset.y)
                                        if (distSq <= 40f * 40f) { // Raio de toque permissivo
                                            pontoSelecionado = closest.second
                                        } else {
                                            pontoSelecionado = null
                                        }
                                    }
                                }
                            }
                    ) {
                        hitTargets.clear()
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paddingLeft = 36f
                        val paddingRight = 16f
                        val paddingTop = 16f
                        val paddingBottom = 24f

                        val chartWidth = canvasWidth - paddingLeft - paddingRight
                        val chartHeight = canvasHeight - paddingTop - paddingBottom

                        // 1. Desenhar Linhas de Grade Horizontais (0%, 50%, 100%)
                        val numSteps = 4
                        for (i in 0..numSteps) {
                            val y = paddingTop + chartHeight * (i.toFloat() / numSteps)
                            val valor = (maxReps * (1f - (i.toFloat() / numSteps))).toInt()

                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(canvasWidth - paddingRight, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }

                        // Função para calcular coordenadas X e Y
                        fun calcularCoordenadas(
                            pontos: List<ChartPoint>,
                            cor: Color
                        ) {
                            if (pontos.isEmpty()) return

                            val coords = mutableListOf<Offset>()

                            if (pontos.size == 1) {
                                val x = paddingLeft + chartWidth / 2f
                                val y = paddingTop + chartHeight * (1f - (pontos[0].reps.toFloat() / maxReps.toFloat()).coerceIn(0f, 1f))
                                coords.add(Offset(x, y))
                            } else {
                                for (i in pontos.indices) {
                                    val t = i.toFloat() / (pontos.size - 1)
                                    val x = paddingLeft + chartWidth * t
                                    val y = paddingTop + chartHeight * (1f - (pontos[i].reps.toFloat() / maxReps.toFloat()).coerceIn(0f, 1f))
                                    coords.add(Offset(x, y))
                                }
                            }

                            // Desenhar linha contínua conectando os pontos
                            if (coords.size > 1) {
                                val path = Path().apply {
                                    moveTo(coords[0].x, coords[0].y)
                                    for (i in 1 until coords.size) {
                                        lineTo(coords[i].x, coords[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = cor,
                                    style = Stroke(
                                        width = 3.5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Desenhar marcadores circulares nos pontos
                            for (i in coords.indices) {
                                val c = coords[i]
                                val p = pontos[i]
                                hitTargets.add(c to p)

                                val isHighlighted = pontoSelecionado?.sessaoId == p.sessaoId && pontoSelecionado?.tipo == p.tipo
                                val raio = if (isHighlighted) 7.dp.toPx() else 4.5.dp.toPx()

                                // Círculo externo
                                drawCircle(
                                    color = cor,
                                    radius = raio,
                                    center = c
                                )
                                // Miolo branco para contraste visual
                                drawCircle(
                                    color = Color.White,
                                    radius = raio * 0.45f,
                                    center = c
                                )
                            }
                        }

                        // Desenhar séries: Six-Count e Navy Seal
                        calcularCoordenadas(pontosSixCount, corSix)
                        calcularCoordenadas(pontosNavySeal, corNavy)
                    }

                    // Marcador de escala máxima no canto superior esquerdo
                    Text(
                        text = "$maxReps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    // Marcador de zero no canto inferior esquerdo
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 4.dp)
                    )
                }

                // Card de Detalhe do Ponto Selecionado ao Tocar
                pontoSelecionado?.let { ponto ->
                    DetalhePontoCard(
                        ponto = ponto,
                        onFechar = { pontoSelecionado = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendaItem(cor: Color, nome: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = nome,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetalhePontoCard(
    ponto: ChartPoint,
    onFechar: () -> Unit
) {
    val corTipo = if (ponto.tipo == TipoExercicio.SIX_COUNT) Color(0xFF22C55E) else Color(0xFF38BDF8)
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")) }
    val dataTexto = remember(ponto.timestamp) { sdf.format(Date(ponto.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_detalhe_ponto_grafico"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(corTipo)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${ponto.tipo.titulo.substringBefore(" ")}: ${ponto.reps} reps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dataTexto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Fechar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onFechar() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
