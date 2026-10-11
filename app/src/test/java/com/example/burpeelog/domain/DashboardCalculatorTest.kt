package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class DashboardCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `calcularRecordeSessao retorna o maior numero de reps de uma sessao`() {
        val sessoes = listOf(
            Sessao(tipo = TipoExercicio.SIX_COUNT, repsFeitas = 80),
            Sessao(tipo = TipoExercicio.SIX_COUNT, repsFeitas = 100),
            Sessao(tipo = TipoExercicio.SIX_COUNT, repsFeitas = 95),
            Sessao(tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 45)
        )

        assertEquals(100, DashboardCalculator.calcularRecordeSessao(sessoes, TipoExercicio.SIX_COUNT))
        assertEquals(45, DashboardCalculator.calcularRecordeSessao(sessoes, TipoExercicio.NAVY_SEAL))
    }

    @Test
    fun `calcularTotalRepsNoAno soma corretamente apenas para o ano solicitado`() {
        val sessoes = listOf(
            Sessao(dataHora = dateToMillis(2026, 3, 10), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 4, 15), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 60),
            Sessao(dataHora = dateToMillis(2025, 12, 30), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 40),
            Sessao(dataHora = dateToMillis(2026, 5, 20), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30)
        )

        assertEquals(110, DashboardCalculator.calcularTotalRepsNoAno(sessoes, 2026, TipoExercicio.SIX_COUNT))
        assertEquals(30, DashboardCalculator.calcularTotalRepsNoAno(sessoes, 2026, TipoExercicio.NAVY_SEAL))
        assertEquals(40, DashboardCalculator.calcularTotalRepsNoAno(sessoes, 2025, TipoExercicio.SIX_COUNT))
    }

    @Test
    fun `calcularMinutosSemanaAtual calcula os 20 min por sessao da semana corrente`() {
        // Segunda-feira 2026-10-05 até Domingo 2026-10-11
        val hoje = LocalDate.of(2026, 10, 11) // Domingo
        val sessoes = listOf(
            Sessao(dataHora = dateToMillis(2026, 10, 5), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50), // Segunda
            Sessao(dataHora = dateToMillis(2026, 10, 7), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30), // Quarta
            Sessao(dataHora = dateToMillis(2026, 10, 9), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 55), // Sexta
            Sessao(dataHora = dateToMillis(2026, 10, 4), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50)  // Semana passada
        )

        // 3 sessões na semana = 60 minutos
        assertEquals(60, DashboardCalculator.calcularMinutosSemanaAtual(sessoes, hoje))
    }

    @Test
    fun `calcularStreakSemanas conta semanas consecutivas atingindo 80 min`() {
        val hoje = LocalDate.of(2026, 10, 11) // Domingo da semana 2
        // Semana 1 (2026-09-28 a 2026-10-04): 4 sessões (bateu 80 min)
        // Semana 2 (2026-10-05 a 2026-10-11): 4 sessões (bateu 80 min)
        val sessoes = listOf(
            // Semana 1
            Sessao(dataHora = dateToMillis(2026, 9, 28), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 9, 29), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30),
            Sessao(dataHora = dateToMillis(2026, 10, 1), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 10, 3), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30),

            // Semana 2
            Sessao(dataHora = dateToMillis(2026, 10, 5), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 10, 6), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30),
            Sessao(dataHora = dateToMillis(2026, 10, 8), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 10, 10), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30)
        )

        assertEquals(2, DashboardCalculator.calcularStreakSemanas(sessoes, hoje))
    }

    @Test
    fun `calcularNivel mapeia percentuais do recorde para niveis 0 a 4`() {
        val recSix = 100
        val recNavy = 40

        // Modo Six-Count:
        assertEquals(0, DashboardCalculator.calcularNivel(0, 0, recSix, recNavy, HeatmapMode.SIX_COUNT))
        assertEquals(1, DashboardCalculator.calcularNivel(25, 0, recSix, recNavy, HeatmapMode.SIX_COUNT)) // 25% -> nivel 1
        assertEquals(2, DashboardCalculator.calcularNivel(50, 0, recSix, recNavy, HeatmapMode.SIX_COUNT)) // 50% -> nivel 2
        assertEquals(3, DashboardCalculator.calcularNivel(75, 0, recSix, recNavy, HeatmapMode.SIX_COUNT)) // 75% -> nivel 3
        assertEquals(4, DashboardCalculator.calcularNivel(90, 0, recSix, recNavy, HeatmapMode.SIX_COUNT)) // 90% -> nivel 4

        // Modo Ambos: media dos percentuais
        // Se Six = 100 (100%) e Navy = 0 (0%) -> média = 50% -> nivel 2
        assertEquals(2, DashboardCalculator.calcularNivel(100, 0, recSix, recNavy, HeatmapMode.AMBOS))
        // Se Six = 100 (100%) e Navy = 40 (100%) -> média = 100% -> nivel 4
        assertEquals(4, DashboardCalculator.calcularNivel(100, 40, recSix, recNavy, HeatmapMode.AMBOS))
        // Se dia sem treino: nivel 0
        assertEquals(0, DashboardCalculator.calcularNivel(0, 0, recSix, recNavy, HeatmapMode.AMBOS))
    }
}
