package com.example.burpeelog.domain

import com.example.R
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import java.time.LocalDate

enum class ChartPeriodFilter(val labelRes: Int, val label: String, val days: Long?) {
    DAYS_30(R.string.chart_filter_30d, "30 dias", 30L),
    DAYS_90(R.string.chart_filter_90d, "90 dias", 90L),
    ALL(R.string.chart_filter_all, "Tudo", null)
}

data class ChartPoint(
    val timestamp: Long,
    val reps: Int,
    val tipo: TipoExercicio,
    val sessaoId: Long
)

object HistoryChartCalculator {

    /**
     * Filtra sessões pelo período selecionado (30 dias, 90 dias ou tudo) em relação à data de referência.
     */
    fun filtrarSessoesPorPeriodo(
        sessoes: List<Sessao>,
        periodo: ChartPeriodFilter,
        hoje: LocalDate = LocalDate.now()
    ): List<Sessao> {
        val days = periodo.days ?: return sessoes
        val limiteData = hoje.minusDays(days)
        return sessoes.filter { sessao ->
            val sessaoDate = DashboardCalculator.timestampToLocalDate(sessao.dataHora)
            !sessaoDate.isBefore(limiteData)
        }
    }

    /**
     * Separa as sessões filtradas em ordem cronológica (mais antiga para mais recente)
     * para desenhar a linha do tempo no gráfico.
     */
    fun prepararPontosGrafico(
        sessoes: List<Sessao>,
        tipo: TipoExercicio
    ): List<ChartPoint> {
        return sessoes
            .filter { it.tipo == tipo }
            .sortedBy { it.dataHora }
            .map {
                ChartPoint(
                    timestamp = it.dataHora,
                    reps = it.repsFeitas,
                    tipo = it.tipo,
                    sessaoId = it.id
                )
            }
    }

    /**
     * Retorna o valor máximo de reps entre as duas séries para calcular a escala Y.
     * Mínimo garantido de 10 reps para dar uma altura visual agradável.
     */
    fun calcularMaxReps(pontosSix: List<ChartPoint>, pontosNavy: List<ChartPoint>): Int {
        val maxSix = pontosSix.maxOfOrNull { it.reps } ?: 0
        val maxNavy = pontosNavy.maxOfOrNull { it.reps } ?: 0
        val maxAbsoluto = maxOf(maxSix, maxNavy)
        return if (maxAbsoluto <= 0) 50 else (maxAbsoluto + (maxAbsoluto * 0.15f).toInt()).coerceAtLeast(10)
    }
}
