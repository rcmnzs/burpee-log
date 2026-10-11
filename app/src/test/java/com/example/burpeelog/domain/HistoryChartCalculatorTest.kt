package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryChartCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `filtrarSessoesPorPeriodo 30 dias filtra corretamente`() {
        val hoje = LocalDate.of(2026, 10, 11)
        val sessoes = listOf(
            Sessao(id = 1, dataHora = dateToMillis(2026, 10, 10), repsFeitas = 50, tipo = TipoExercicio.SIX_COUNT),
            Sessao(id = 2, dataHora = dateToMillis(2026, 9, 20), repsFeitas = 45, tipo = TipoExercicio.SIX_COUNT), // 21 dias atrás -> entra
            Sessao(id = 3, dataHora = dateToMillis(2026, 8, 1), repsFeitas = 40, tipo = TipoExercicio.SIX_COUNT)   // > 70 dias atrás -> sai
        )

        val filtradas30 = HistoryChartCalculator.filtrarSessoesPorPeriodo(sessoes, ChartPeriodFilter.DAYS_30, hoje)
        assertEquals(2, filtradas30.size)
        assertEquals(1L, filtradas30[0].id)
        assertEquals(2L, filtradas30[1].id)
    }

    @Test
    fun `filtrarSessoesPorPeriodo 90 dias filtra corretamente`() {
        val hoje = LocalDate.of(2026, 10, 11)
        val sessoes = listOf(
            Sessao(id = 1, dataHora = dateToMillis(2026, 10, 10), repsFeitas = 50, tipo = TipoExercicio.SIX_COUNT),
            Sessao(id = 2, dataHora = dateToMillis(2026, 8, 1), repsFeitas = 45, tipo = TipoExercicio.SIX_COUNT), // ~71 dias atrás -> entra
            Sessao(id = 3, dataHora = dateToMillis(2026, 6, 1), repsFeitas = 40, tipo = TipoExercicio.SIX_COUNT)  // > 130 dias atrás -> sai
        )

        val filtradas90 = HistoryChartCalculator.filtrarSessoesPorPeriodo(sessoes, ChartPeriodFilter.DAYS_90, hoje)
        assertEquals(2, filtradas90.size)
        assertEquals(1L, filtradas90[0].id)
        assertEquals(2L, filtradas90[1].id)
    }

    @Test
    fun `filtrarSessoesPorPeriodo Tudo retorna todas as sessoes`() {
        val sessoes = listOf(
            Sessao(id = 1, dataHora = dateToMillis(2026, 10, 10), repsFeitas = 50, tipo = TipoExercicio.SIX_COUNT),
            Sessao(id = 2, dataHora = dateToMillis(2025, 1, 1), repsFeitas = 45, tipo = TipoExercicio.SIX_COUNT)
        )

        val filtradas = HistoryChartCalculator.filtrarSessoesPorPeriodo(sessoes, ChartPeriodFilter.ALL)
        assertEquals(2, filtradas.size)
    }

    @Test
    fun `prepararPontosGrafico separa por tipo e ordena cronologicamente`() {
        val sessoes = listOf(
            Sessao(id = 1, dataHora = dateToMillis(2026, 10, 10), repsFeitas = 80, tipo = TipoExercicio.SIX_COUNT),
            Sessao(id = 2, dataHora = dateToMillis(2026, 10, 5), repsFeitas = 75, tipo = TipoExercicio.SIX_COUNT),
            Sessao(id = 3, dataHora = dateToMillis(2026, 10, 8), repsFeitas = 30, tipo = TipoExercicio.NAVY_SEAL)
        )

        val pontosSix = HistoryChartCalculator.prepararPontosGrafico(sessoes, TipoExercicio.SIX_COUNT)
        assertEquals(2, pontosSix.size)
        assertEquals(75, pontosSix[0].reps) // O mais antigo primeiro (dia 5)
        assertEquals(80, pontosSix[1].reps) // O mais recente depois (dia 10)

        val pontosNavy = HistoryChartCalculator.prepararPontosGrafico(sessoes, TipoExercicio.NAVY_SEAL)
        assertEquals(1, pontosNavy.size)
        assertEquals(30, pontosNavy[0].reps)
    }

    @Test
    fun `calcularMaxReps calcula teto adequado para o eixo Y`() {
        val pontosSix = listOf(ChartPoint(1000L, 100, TipoExercicio.SIX_COUNT, 1L))
        val pontosNavy = listOf(ChartPoint(1000L, 40, TipoExercicio.NAVY_SEAL, 2L))

        val maxY = HistoryChartCalculator.calcularMaxReps(pontosSix, pontosNavy)
        // 100 + 15% = 115
        assertEquals(115, maxY)
    }
}
