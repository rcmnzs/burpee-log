package com.example.burpeelog.domain

import com.example.R
import java.time.LocalDate

/**
 * Níveis oficiais do Busy Dad Program (busydadtraining.com/levels).
 * Cada nível requer um número de repetições em uma sessão de 20 minutos
 * para Six-Count e Navy Seal, alcançadas na mesma semana para desbloqueio.
 */
enum class BusyDadLevel(
    val id: String,
    val titleRes: Int,
    val sixCountTarget: Int,
    val navySealTarget: Int,
    val ordem: Int
) {
    NIVEL_1A("1A", R.string.level_1a, 1, 1, 1),
    NIVEL_1B("1B", R.string.level_1b, 50, 20, 2),
    NIVEL_1C("1C", R.string.level_1c, 100, 40, 3),
    NIVEL_1D("1D", R.string.level_1d, 150, 60, 4),
    NIVEL_2A("2A", R.string.level_2a, 200, 80, 5),
    NIVEL_2B("2B", R.string.level_2b, 225, 90, 6),
    NIVEL_3A("3A", R.string.level_3a, 250, 100, 7),
    NIVEL_3B("3B", R.string.level_3b, 260, 110, 8),
    NIVEL_4A("4A", R.string.level_4a, 275, 120, 9),
    NIVEL_4B("4B", R.string.level_4b, 300, 135, 10),
    GRADUACAO("GRAD", R.string.level_graduation, 325, 150, 11);

    val isGraduation: Boolean get() = this == GRADUACAO
}

enum class LevelState {
    UNLOCKED,
    NEXT,
    LOCKED
}

data class NivelItemStatus(
    val level: BusyDadLevel,
    val status: LevelState,
    val isCurrent: Boolean,
    val semanaDesbloqueio: LocalDate?,
    val progressoSixCount: Float,
    val progressoNavySeal: Float,
    val progressoGeral: Float
)

data class RoadmapProgress(
    val recordeSixCount: Int,
    val recordeNavySeal: Int,
    val nivelGeralAtual: BusyDadLevel?,
    val proximoNivelGeral: BusyDadLevel?,
    val nivelSixCountAtual: BusyDadLevel?,
    val proximoNivelSixCount: BusyDadLevel?,
    val nivelNavySealAtual: BusyDadLevel?,
    val proximoNivelNavySeal: BusyDadLevel?,
    val faltaSixCountParaProximo: Int,
    val faltaNavySealParaProximo: Int,
    val precisaMesmaSemana: Boolean,
    val percentualProgressoProximo: Float,
    val niveis: List<NivelItemStatus>
)
