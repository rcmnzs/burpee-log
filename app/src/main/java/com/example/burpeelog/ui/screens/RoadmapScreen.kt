package com.example.burpeelog.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.domain.BusyDadLevel
import com.example.burpeelog.domain.BusyDadRoadmapCalculator
import com.example.burpeelog.domain.LevelState
import com.example.burpeelog.domain.NivelItemStatus
import com.example.burpeelog.domain.RoadmapProgress
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? BurpeeLogApplication
    val sessoes by (app?.repository?.allSessoes ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())

    val progress = remember(sessoes) {
        BusyDadRoadmapCalculator.calcularProgresso(sessoes)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_roadmap"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.roadmap_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.roadmap_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_voltar_roadmap")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                CardDestaqueRoadmap(progress = progress)
            }

            item {
                Text(
                    text = stringResource(R.string.roadmap_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(progress.niveis, key = { it.level.id }) { itemStatus ->
                CardNivelItem(itemStatus = itemStatus)
            }

            item {
                NotaRodapeRoadmap()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Card de destaque: Nível Geral Atual, recordes e o que falta para o próximo nível.
 */
@Composable
private fun CardDestaqueRoadmap(
    progress: RoadmapProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_destaque_roadmap"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabeçalho: Ícone do Troféu + Título do Nível Geral
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (progress.nivelGeralAtual?.isGraduation == true) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.roadmap_overall_level),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progress.nivelGeralAtual?.let { stringResource(it.titleRes) }
                            ?: stringResource(R.string.roadmap_no_level_yet),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Cards de Recorde e Nível por Exercício (Six-Count e Navy Seal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Six-Count Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.six_count_short),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${progress.recordeSixCount} reps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = progress.nivelSixCountAtual?.let { stringResource(it.titleRes) }
                                ?: stringResource(R.string.roadmap_no_level_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Navy Seal Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.navy_seal_short),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${progress.recordeNavySeal} reps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = progress.nivelNavySealAtual?.let { stringResource(it.titleRes) }
                                ?: stringResource(R.string.roadmap_no_level_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bloco: Próximo Nível e O que falta
            if (progress.proximoNivelGeral != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.roadmap_next_goal,
                                        stringResource(progress.proximoNivelGeral.titleRes)
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${(progress.percentualProgressoProximo * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB300)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress.percentualProgressoProximo },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFFB300),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Texto indicando exatamente o que falta
                        val textoFalta = when {
                            progress.precisaMesmaSemana -> {
                                stringResource(
                                    R.string.roadmap_missing_same_week,
                                    progress.proximoNivelGeral.sixCountTarget,
                                    progress.proximoNivelGeral.navySealTarget
                                )
                            }
                            progress.faltaSixCountParaProximo > 0 && progress.faltaNavySealParaProximo > 0 -> {
                                stringResource(
                                    R.string.roadmap_missing_both,
                                    progress.faltaSixCountParaProximo,
                                    progress.faltaNavySealParaProximo
                                )
                            }
                            progress.faltaSixCountParaProximo > 0 -> {
                                stringResource(
                                    R.string.roadmap_missing_six_only,
                                    progress.faltaSixCountParaProximo
                                )
                            }
                            else -> {
                                stringResource(
                                    R.string.roadmap_missing_navy_only,
                                    progress.faltaNavySealParaProximo
                                )
                            }
                        }

                        Text(
                            text = textoFalta,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF00C853).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.roadmap_all_levels_completed),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card individual de cada nível na lista vertical.
 */
@Composable
private fun CardNivelItem(
    itemStatus: NivelItemStatus,
    modifier: Modifier = Modifier
) {
    val level = itemStatus.level
    val state = itemStatus.status

    val containerColor = when (state) {
        LevelState.UNLOCKED -> if (itemStatus.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        }
        LevelState.NEXT -> MaterialTheme.colorScheme.surface
        LevelState.LOCKED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    val borderColor = when (state) {
        LevelState.UNLOCKED -> if (itemStatus.isCurrent) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFF00C853).copy(alpha = 0.5f)
        }
        LevelState.NEXT -> Color(0xFFFF9800)
        LevelState.LOCKED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val borderWidth = when {
        itemStatus.isCurrent -> 2.dp
        state == LevelState.NEXT -> 1.5.dp
        else -> 1.dp
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("item_nivel_${level.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha do Topo: Nome do Nível + Badge de Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconColor = when (state) {
                        LevelState.UNLOCKED -> Color(0xFF00E676)
                        LevelState.NEXT -> Color(0xFFFFB300)
                        LevelState.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }

                    val icon = when (state) {
                        LevelState.UNLOCKED -> Icons.Default.CheckCircle
                        LevelState.NEXT -> Icons.Default.Star
                        LevelState.LOCKED -> Icons.Default.Lock
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(level.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state == LevelState.LOCKED) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // Badges de Status
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (itemStatus.isCurrent) {
                        BadgeStatus(
                            texto = stringResource(R.string.status_current),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    when (state) {
                        LevelState.UNLOCKED -> {
                            BadgeStatus(
                                texto = stringResource(R.string.status_unlocked),
                                containerColor = Color(0xFF00C853).copy(alpha = 0.2f),
                                contentColor = Color(0xFF00E676)
                            )
                        }
                        LevelState.NEXT -> {
                            BadgeStatus(
                                texto = stringResource(R.string.status_next),
                                containerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                                contentColor = Color(0xFFFFB300)
                            )
                        }
                        LevelState.LOCKED -> {
                            BadgeStatus(
                                texto = stringResource(R.string.status_locked),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Metas do Nível
            Text(
                text = stringResource(
                    R.string.roadmap_target_format,
                    level.sixCountTarget,
                    level.navySealTarget
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (state == LevelState.LOCKED) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Data de desbloqueio quando alcançado
            if (state == LevelState.UNLOCKED && itemStatus.semanaDesbloqueio != null) {
                val dataTexto = itemStatus.semanaDesbloqueio.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                Text(
                    text = stringResource(R.string.unlocked_week_format, dataTexto),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Barra de Progresso quando próximo
            if (state == LevelState.NEXT) {
                Spacer(modifier = Modifier.height(2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.percent_achieved, (itemStatus.progressoGeral * 100).toInt()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { itemStatus.progressoGeral },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFFB300),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeStatus(
    texto: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Nota informativa no rodapé sobre as regras do programa oficial.
 */
@Composable
private fun NotaRodapeRoadmap() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.roadmap_footer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
