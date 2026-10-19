package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object BusyDadRoadmapCalculator {

    fun calcularProgresso(
        sessoes: List<Sessao>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RoadmapProgress {
        val recordeSixCount = sessoes
            .filter { it.tipo == TipoExercicio.SIX_COUNT }
            .maxOfOrNull { it.repsFeitas } ?: 0

        val recordeNavySeal = sessoes
            .filter { it.tipo == TipoExercicio.NAVY_SEAL }
            .maxOfOrNull { it.repsFeitas } ?: 0

        // Agrupa por semana (segunda-feira da semana de cada sessão)
        // Semana: segunda a domingo
        val sessoesPorSemana = HashMap<LocalDate, MutableList<Sessao>>()
        for (sessao in sessoes) {
            val localDate = Instant.ofEpochMilli(sessao.dataHora).atZone(zoneId).toLocalDate()
            val segundaDaSemana = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            sessoesPorSemana.getOrPut(segundaDaSemana) { mutableListOf() }.add(sessao)
        }

        // Para cada semana, encontra o maior Six-Count e maior Navy Seal realizados
        data class SemanaMax(
            val segunda: LocalDate,
            val maxSix: Int,
            val maxNavy: Int
        )

        val semanas = sessoesPorSemana.map { (segunda, lista) ->
            val maxSix = lista.filter { it.tipo == TipoExercicio.SIX_COUNT }.maxOfOrNull { it.repsFeitas } ?: 0
            val maxNavy = lista.filter { it.tipo == TipoExercicio.NAVY_SEAL }.maxOfOrNull { it.repsFeitas } ?: 0
            SemanaMax(segunda, maxSix, maxNavy)
        }.sortedBy { it.segunda }

        // Mapeia data de desbloqueio para cada nível (primeira semana em que atingiu ambos na mesma semana)
        val dataDesbloqueio = mutableMapOf<BusyDadLevel, LocalDate>()
        for (level in BusyDadLevel.entries) {
            val semanaDesbloqueou = semanas.firstOrNull {
                it.maxSix >= level.sixCountTarget && it.maxNavy >= level.navySealTarget
            }
            if (semanaDesbloqueou != null) {
                dataDesbloqueio[level] = semanaDesbloqueou.segunda
            }
        }

        // Nível geral atual é o nível mais alto já desbloqueado
        val nivelGeralAtual: BusyDadLevel? = BusyDadLevel.entries
            .filter { it in dataDesbloqueio }
            .maxByOrNull { it.ordem }

        val proximoNivelGeral: BusyDadLevel? = when {
            nivelGeralAtual == null -> BusyDadLevel.NIVEL_1A
            nivelGeralAtual == BusyDadLevel.GRADUACAO -> null
            else -> BusyDadLevel.entries.getOrNull(nivelGeralAtual.ordinal + 1)
        }

        // Nível Six-Count individual pelo recorde
        val nivelSixCountAtual = BusyDadLevel.entries
            .filter { recordeSixCount >= it.sixCountTarget }
            .maxByOrNull { it.ordem }

        val proximoNivelSixCount = when {
            nivelSixCountAtual == null -> BusyDadLevel.NIVEL_1A
            nivelSixCountAtual == BusyDadLevel.GRADUACAO -> null
            else -> BusyDadLevel.entries.getOrNull(nivelSixCountAtual.ordinal + 1)
        }

        // Nível Navy Seal individual pelo recorde
        val nivelNavySealAtual = BusyDadLevel.entries
            .filter { recordeNavySeal >= it.navySealTarget }
            .maxByOrNull { it.ordem }

        val proximoNivelNavySeal = when {
            nivelNavySealAtual == null -> BusyDadLevel.NIVEL_1A
            nivelNavySealAtual == BusyDadLevel.GRADUACAO -> null
            else -> BusyDadLevel.entries.getOrNull(nivelNavySealAtual.ordinal + 1)
        }

        // O que falta para o próximo nível geral
        val faltaSix: Int
        val faltaNavy: Int
        val precisaMesmaSemana: Boolean
        val percentualProximo: Float

        if (proximoNivelGeral != null) {
            faltaSix = maxOf(0, proximoNivelGeral.sixCountTarget - recordeSixCount)
            faltaNavy = maxOf(0, proximoNivelGeral.navySealTarget - recordeNavySeal)
            // Se já atingiu os recordes numéricos dos dois individualmente, mas não desbloqueou, precisa na mesma semana
            precisaMesmaSemana = (faltaSix == 0 && faltaNavy == 0)

            val pSix = (recordeSixCount.toFloat() / proximoNivelGeral.sixCountTarget.toFloat()).coerceIn(0f, 1f)
            val pNavy = (recordeNavySeal.toFloat() / proximoNivelGeral.navySealTarget.toFloat()).coerceIn(0f, 1f)
            percentualProximo = (pSix + pNavy) / 2f
        } else {
            faltaSix = 0
            faltaNavy = 0
            precisaMesmaSemana = false
            percentualProximo = 1f
        }

        // Constrói status de cada nível
        val niveisStatus = BusyDadLevel.entries.map { level ->
            val unlockedDate = dataDesbloqueio[level]
            val isUnlocked = unlockedDate != null
            val isCurrent = (level == nivelGeralAtual)
            val isNext = (level == proximoNivelGeral)

            val state = when {
                isUnlocked -> LevelState.UNLOCKED
                isNext -> LevelState.NEXT
                else -> LevelState.LOCKED
            }

            val pSix = (recordeSixCount.toFloat() / level.sixCountTarget.toFloat()).coerceIn(0f, 1f)
            val pNavy = (recordeNavySeal.toFloat() / level.navySealTarget.toFloat()).coerceIn(0f, 1f)
            val pGeral = if (isUnlocked) 1f else (pSix + pNavy) / 2f

            NivelItemStatus(
                level = level,
                status = state,
                isCurrent = isCurrent,
                semanaDesbloqueio = unlockedDate,
                progressoSixCount = pSix,
                progressoNavySeal = pNavy,
                progressoGeral = pGeral
            )
        }

        return RoadmapProgress(
            recordeSixCount = recordeSixCount,
            recordeNavySeal = recordeNavySeal,
            nivelGeralAtual = nivelGeralAtual,
            proximoNivelGeral = proximoNivelGeral,
            nivelSixCountAtual = nivelSixCountAtual,
            proximoNivelSixCount = proximoNivelSixCount,
            nivelNavySealAtual = nivelNavySealAtual,
            proximoNivelNavySeal = proximoNivelNavySeal,
            faltaSixCountParaProximo = faltaSix,
            faltaNavySealParaProximo = faltaNavy,
            precisaMesmaSemana = precisaMesmaSemana,
            percentualProgressoProximo = percentualProximo,
            niveis = niveisStatus
        )
    }
}
