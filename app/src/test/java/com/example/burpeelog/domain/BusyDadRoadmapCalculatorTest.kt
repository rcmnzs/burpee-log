package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BusyDadRoadmapCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `historico vazio resulta em nenhum nivel geral e proximo nivel 1A`() {
        val result = BusyDadRoadmapCalculator.calcularProgresso(emptyList(), zoneId)

        assertEquals(0, result.recordeSixCount)
        assertEquals(0, result.recordeNavySeal)
        assertNull(result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_1A, result.proximoNivelGeral)
        assertNull(result.nivelSixCountAtual)
        assertEquals(BusyDadLevel.NIVEL_1A, result.proximoNivelSixCount)
        assertNull(result.nivelNavySealAtual)
        assertEquals(BusyDadLevel.NIVEL_1A, result.proximoNivelNavySeal)
        assertEquals(1, result.faltaSixCountParaProximo)
        assertEquals(1, result.faltaNavySealParaProximo)
        assertFalse(result.precisaMesmaSemana)
    }

    @Test
    fun `regra da mesma semana - ambas as marcas na mesma semana desbloqueiam o nivel`() {
        // Semana: Segunda 2026-10-05 até Domingo 2026-10-11
        val sessoesMesmaSemana = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 10, 5), // Segunda
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 200 // Limiar 2A
            ),
            Sessao(
                dataHora = dateToMillis(2026, 10, 11), // Domingo da mesma semana
                tipo = TipoExercicio.NAVY_SEAL,
                repsFeitas = 80 // Limiar 2A
            )
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoesMesmaSemana, zoneId)

        assertEquals(BusyDadLevel.NIVEL_2A, result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_2B, result.proximoNivelGeral)

        val item2A = result.niveis.first { it.level == BusyDadLevel.NIVEL_2A }
        assertEquals(LevelState.UNLOCKED, item2A.status)
        assertTrue(item2A.isCurrent)
        assertNotNull(item2A.semanaDesbloqueio)
        assertEquals(LocalDate.of(2026, 10, 5), item2A.semanaDesbloqueio)
    }

    @Test
    fun `regra da mesma semana - marcas em semanas diferentes NAO desbloqueiam o nivel geral`() {
        // Semana 1: Domingo 2026-10-11 (Six-Count 200)
        // Semana 2: Segunda 2026-10-12 (Navy Seal 80)
        val sessoesSemanasDiferentes = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 10, 11), // Domingo (Semana 1)
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 200
            ),
            Sessao(
                dataHora = dateToMillis(2026, 10, 12), // Segunda (Semana 2)
                tipo = TipoExercicio.NAVY_SEAL,
                repsFeitas = 80
            )
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoesSemanasDiferentes, zoneId)

        // Nível geral não deve ter alcançado 2A nem 1A porque não houve semana com ambos os exercícios
        assertNull(result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_1A, result.proximoNivelGeral)

        // Porém os recordes e níveis individuais refletem os PRs
        assertEquals(BusyDadLevel.NIVEL_2A, result.nivelSixCountAtual)
        assertEquals(BusyDadLevel.NIVEL_2A, result.nivelNavySealAtual)
    }

    @Test
    fun `calculo de quanto falta para o proximo nivel`() {
        // Usuário desbloqueou 1D (150 Six-Count e 60 Navy Seal) na mesma semana
        // Recordes atuais: 170 Six-Counts e 66 Navy Seals
        // Próximo nível: 2A (alvos: 200 Six-Counts e 80 Navy Seals)
        val sessoes = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 9, 7), // Segunda
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 170
            ),
            Sessao(
                dataHora = dateToMillis(2026, 9, 9), // Quarta
                tipo = TipoExercicio.NAVY_SEAL,
                repsFeitas = 66
            )
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoes, zoneId)

        assertEquals(BusyDadLevel.NIVEL_1D, result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_2A, result.proximoNivelGeral)
        // 200 - 170 = 30 Six-Counts faltantes
        assertEquals(30, result.faltaSixCountParaProximo)
        // 80 - 66 = 14 Navy Seals faltantes
        assertEquals(14, result.faltaNavySealParaProximo)
        assertFalse(result.precisaMesmaSemana)
    }

    @Test
    fun `calculo de quanto falta quando apenas um exercicio atingiu a meta do proximo nivel`() {
        // Usuário desbloqueou 1D
        // Recorde Six-Count é 210 (já atingiu os 200 de 2A)
        // Recorde Navy Seal é 66 (faltam 14 para 80 de 2A)
        val sessoes = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 9, 7),
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 210
            ),
            Sessao(
                dataHora = dateToMillis(2026, 9, 9),
                tipo = TipoExercicio.NAVY_SEAL,
                repsFeitas = 66
            )
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoes, zoneId)

        assertEquals(BusyDadLevel.NIVEL_1D, result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_2A, result.proximoNivelGeral)
        assertEquals(0, result.faltaSixCountParaProximo)
        assertEquals(14, result.faltaNavySealParaProximo)
        assertFalse(result.precisaMesmaSemana)
    }

    @Test
    fun `precisaMesmaSemana e verdadeiro quando ambos os PRs atingem o alvo mas em semanas diferentes`() {
        // Semana 1: 1D desbloqueado (150 / 60) e depois Six-Count atinge 205 (meta 2A)
        // Semana 2: Navy Seal atinge 85 (meta 2A), mas Six-Count não foi feito
        val sessoes = listOf(
            Sessao(dataHora = dateToMillis(2026, 9, 1), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 150),
            Sessao(dataHora = dateToMillis(2026, 9, 3), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 60),
            Sessao(dataHora = dateToMillis(2026, 9, 5), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 205),
            // Semana seguinte
            Sessao(dataHora = dateToMillis(2026, 9, 10), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 85)
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoes, zoneId)

        assertEquals(BusyDadLevel.NIVEL_1D, result.nivelGeralAtual)
        assertEquals(BusyDadLevel.NIVEL_2A, result.proximoNivelGeral)
        assertEquals(0, result.faltaSixCountParaProximo)
        assertEquals(0, result.faltaNavySealParaProximo)
        assertTrue(result.precisaMesmaSemana)
    }

    @Test
    fun `graduacao alcancada tem proximo nivel nulo e zero faltantes`() {
        val sessoes = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 10, 1),
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 330 // Alvo 325
            ),
            Sessao(
                dataHora = dateToMillis(2026, 10, 3),
                tipo = TipoExercicio.NAVY_SEAL,
                repsFeitas = 155 // Alvo 150
            )
        )

        val result = BusyDadRoadmapCalculator.calcularProgresso(sessoes, zoneId)

        assertEquals(BusyDadLevel.GRADUACAO, result.nivelGeralAtual)
        assertNull(result.proximoNivelGeral)
        assertEquals(0, result.faltaSixCountParaProximo)
        assertEquals(0, result.faltaNavySealParaProximo)
        assertFalse(result.precisaMesmaSemana)

        val itemGrad = result.niveis.first { it.level == BusyDadLevel.GRADUACAO }
        assertEquals(LevelState.UNLOCKED, itemGrad.status)
        assertTrue(itemGrad.isCurrent)
    }

    @Test
    fun `ordem e metas oficiais de todos os niveis sao exatas`() {
        val niveis = BusyDadLevel.entries

        assertEquals(11, niveis.size)

        assertEquals(BusyDadLevel.NIVEL_1A, niveis[0])
        assertEquals(1, niveis[0].sixCountTarget)
        assertEquals(1, niveis[0].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_1B, niveis[1])
        assertEquals(50, niveis[1].sixCountTarget)
        assertEquals(20, niveis[1].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_1C, niveis[2])
        assertEquals(100, niveis[2].sixCountTarget)
        assertEquals(40, niveis[2].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_1D, niveis[3])
        assertEquals(150, niveis[3].sixCountTarget)
        assertEquals(60, niveis[3].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_2A, niveis[4])
        assertEquals(200, niveis[4].sixCountTarget)
        assertEquals(80, niveis[4].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_2B, niveis[5])
        assertEquals(225, niveis[5].sixCountTarget)
        assertEquals(90, niveis[5].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_3A, niveis[6])
        assertEquals(250, niveis[6].sixCountTarget)
        assertEquals(100, niveis[6].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_3B, niveis[7])
        assertEquals(260, niveis[7].sixCountTarget)
        assertEquals(110, niveis[7].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_4A, niveis[8])
        assertEquals(275, niveis[8].sixCountTarget)
        assertEquals(120, niveis[8].navySealTarget)

        assertEquals(BusyDadLevel.NIVEL_4B, niveis[9])
        assertEquals(300, niveis[9].sixCountTarget)
        assertEquals(135, niveis[9].navySealTarget)

        assertEquals(BusyDadLevel.GRADUACAO, niveis[10])
        assertEquals(325, niveis[10].sixCountTarget)
        assertEquals(150, niveis[10].navySealTarget)
    }
}
