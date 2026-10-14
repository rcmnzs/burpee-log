package com.example.burpeelog.domain

import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.META_SEMANAL_MINUTOS
import com.example.burpeelog.data.model.META_SEMANAL_SESSOES
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class HeatmapMode(val rotulo: String) {
    SIX_COUNT("Six-Count"),
    NAVY_SEAL("Navy Seal"),
    AMBOS("Ambos")
}

data class DayHeatmapData(
    val date: LocalDate,
    val repsSixCount: Int = 0,
    val repsNavySeal: Int = 0
) {
    val totalReps: Int get() = repsSixCount + repsNavySeal
}

object DashboardCalculator {

    val ZONE_ID: ZoneId = ZoneId.systemDefault()

    fun timestampToLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).toLocalDate()
    }

    /**
     * Calcula o recorde histórico de reps em uma única sessão para o tipo dado.
     */
    fun calcularRecordeSessao(sessoes: List<Sessao>, tipo: TipoExercicio): Int {
        return sessoes
            .filter { it.tipo == tipo }
            .maxOfOrNull { it.repsFeitas } ?: 0
    }

    /**
     * Total de reps acumuladas no ano para o tipo dado.
     */
    fun calcularTotalRepsNoAno(sessoes: List<Sessao>, ano: Int, tipo: TipoExercicio): Int {
        return sessoes.filter {
            it.tipo == tipo && timestampToLocalDate(it.dataHora).year == ano
        }.sumOf { it.repsFeitas }
    }

    /**
     * Calcula os minutos de treino na semana atual (Segunda a Domingo).
     */
    fun calcularMinutosSemanaAtual(sessoes: List<Sessao>, hoje: LocalDate = LocalDate.now()): Int {
        val segundaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val domingoAtual = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val segundosSemana = sessoes.filter {
            val data = timestampToLocalDate(it.dataHora)
            !data.isBefore(segundaAtual) && !data.isAfter(domingoAtual)
        }.sumOf { it.duracaoSegundos }

        return segundosSemana / 60
    }

    /**
     * Calcula a quantidade de sessões realizadas na semana atual (Segunda a Domingo).
     */
    fun calcularContagemSessoesSemanaAtual(sessoes: List<Sessao>, hoje: LocalDate = LocalDate.now()): Int {
        val segundaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val domingoAtual = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        return sessoes.count {
            val data = timestampToLocalDate(it.dataHora)
            !data.isBefore(segundaAtual) && !data.isAfter(domingoAtual)
        }
    }

    /**
     * Calcula a sequência (streak) de semanas consecutivas que atingiram a meta de 80 minutos.
     * Considera semanas de Segunda a Domingo.
     * Se a semana atual ainda não atingiu a meta (por estar em andamento), a sequência é contada
     * a partir da semana anterior.
     */
    fun calcularStreakSemanas(sessoes: List<Sessao>, hoje: LocalDate = LocalDate.now()): Int {
        if (sessoes.isEmpty()) return 0

        val segundaAtual = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Agrupa sessões por início da semana (segunda-feira) somando duracaoSegundos
        val segundosPorSemana = HashMap<LocalDate, Int>()
        for (sessao in sessoes) {
            val data = timestampToLocalDate(sessao.dataHora)
            val segundaDaSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            segundosPorSemana[segundaDaSemana] = (segundosPorSemana[segundaDaSemana] ?: 0) + sessao.duracaoSegundos
        }

        val metaSegundos = 4800 // 80 minutos * 60 segundos
        val segundosSemanaAtual = segundosPorSemana[segundaAtual] ?: 0
        var streak = 0
        var semanaCheck = if (segundosSemanaAtual >= metaSegundos) {
            streak++
            segundaAtual.minusWeeks(1)
        } else {
            segundaAtual.minusWeeks(1)
        }

        while (true) {
            val count = segundosPorSemana[semanaCheck] ?: 0
            if (count >= metaSegundos) {
                streak++
                semanaCheck = semanaCheck.minusWeeks(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Calcula o nível de cor (0 a 4) para um determinado dia conforme as regras do heatmap.
     */
    fun calcularNivel(
        repsSix: Int,
        repsNavy: Int,
        recordeSix: Int,
        recordeNavy: Int,
        modo: HeatmapMode
    ): Int {
        return when (modo) {
            HeatmapMode.SIX_COUNT -> {
                if (recordeSix <= 0 || repsSix <= 0) return 0
                val pct = repsSix.toFloat() / recordeSix.toFloat()
                classificarNivelPorPercentual(pct)
            }
            HeatmapMode.NAVY_SEAL -> {
                if (recordeNavy <= 0 || repsNavy <= 0) return 0
                val pct = repsNavy.toFloat() / recordeNavy.toFloat()
                classificarNivelPorPercentual(pct)
            }
            HeatmapMode.AMBOS -> {
                if (repsSix <= 0 && repsNavy <= 0) return 0
                if (recordeSix <= 0 && recordeNavy <= 0) return 0

                val pctSix = if (recordeSix > 0) repsSix.toFloat() / recordeSix.toFloat() else 0f
                val pctNavy = if (recordeNavy > 0) repsNavy.toFloat() / recordeNavy.toFloat() else 0f
                val pctMedia = (pctSix + pctNavy) / 2f
                classificarNivelPorPercentual(pctMedia)
            }
        }
    }

    private fun classificarNivelPorPercentual(pct: Float): Int {
        return when {
            pct <= 0f -> 0
            pct <= 0.25f -> 1
            pct <= 0.50f -> 2
            pct <= 0.75f -> 3
            else -> 4
        }
    }

    /**
     * Gera a descrição textual das faixas para a legenda.
     * Ex.: "1–25 · 26–50 · 51–75 · 76+"
     */
    fun gerarFaixasLegenda(modo: HeatmapMode, recordeSix: Int, recordeNavy: Int): String {
        val recorde = when (modo) {
            HeatmapMode.SIX_COUNT -> recordeSix
            HeatmapMode.NAVY_SEAL -> recordeNavy
            HeatmapMode.AMBOS -> null
        }

        if (modo == HeatmapMode.AMBOS) {
            return "Média dos recordes: 1%–25% · 26%–50% · 51%–75% · 76%+"
        }

        if (recorde == null || recorde <= 0) {
            return "Nenhum recorde registrado ainda"
        }

        val f1 = (recorde * 0.25).toInt().coerceAtLeast(1)
        val f2 = (recorde * 0.50).toInt().coerceAtLeast(f1 + 1)
        val f3 = (recorde * 0.75).toInt().coerceAtLeast(f2 + 1)

        return "1–$f1 · ${f1 + 1}–$f2 · ${f2 + 1}–$f3 · ${f3 + 1}+"
    }
}
