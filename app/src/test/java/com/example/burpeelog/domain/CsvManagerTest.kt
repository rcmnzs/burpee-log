package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvManagerTest {

    private val zoneId = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(12, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `parseia formatos de data dd_MM_yyyy, d_M_yyyy e yyyy_MM_dd`() {
        assertEquals(LocalDate.of(2026, 10, 11), CsvManager.parseDate("11/10/2026"))
        assertEquals(LocalDate.of(2026, 3, 5), CsvManager.parseDate("5/3/2026"))
        assertEquals(LocalDate.of(2026, 10, 11), CsvManager.parseDate("2026-10-11"))
    }

    @Test
    fun `processa separadores virgula e ponto e virgula com variacoes de cabecalho`() {
        val csvVirgula = """
            Data,Six Counts,Navy Seals
            11/10/2026,50,30
        """.trimIndent()

        val previewVirgula = CsvManager.processarCsvImportacao(csvVirgula, emptyList())
        assertEquals(1, previewVirgula.totalLinhasLidas)
        assertEquals(2, previewVirgula.totalSessoesCriadas)
        assertEquals(0, previewVirgula.erros.size)

        val csvPontoEVirgula = """
            DATA;SIX-COUNTS;NAVYSEALS
            2026-10-11;60;0
        """.trimIndent()

        val previewPontoVirgula = CsvManager.processarCsvImportacao(csvPontoEVirgula, emptyList())
        assertEquals(1, previewPontoVirgula.totalLinhasLidas)
        assertEquals(1, previewPontoVirgula.totalSessoesCriadas)
        assertEquals(TipoExercicio.SIX_COUNT, previewPontoVirgula.sessoesParaCriar.first().tipo)
        assertEquals(60, previewPontoVirgula.sessoesParaCriar.first().repsFeitas)
    }

    @Test
    fun `trata celulas vazias, zero e texto nao numerico como sem treino`() {
        val csv = """
            Data,Six-Counts,Navy Seals
            10/10/2026,,0
            11/10/2026,45,N/A
            12/10/2026,-,35
        """.trimIndent()

        val preview = CsvManager.processarCsvImportacao(csv, emptyList())
        assertEquals(3, preview.totalLinhasLidas)
        assertEquals(0, preview.erros.size)
        // Linha 1: 0 reps -> 0 sessões
        // Linha 2: 45 Six-Count -> 1 sessão
        // Linha 3: 35 Navy Seal -> 1 sessão
        assertEquals(2, preview.totalSessoesCriadas)

        val s1 = preview.sessoesParaCriar[0]
        assertEquals(TipoExercicio.SIX_COUNT, s1.tipo)
        assertEquals(45, s1.repsFeitas)
        assertTrue(s1.importada)

        val s2 = preview.sessoesParaCriar[1]
        assertEquals(TipoExercicio.NAVY_SEAL, s2.tipo)
        assertEquals(35, s2.repsFeitas)
        assertTrue(s2.importada)
    }

    @Test
    fun `ignora sessoes duplicadas na mesma data e tipo existentes no banco`() {
        // Já existe Six-Count em 11/10/2026
        val existentes = listOf(
            Sessao(
                dataHora = dateToMillis(2026, 10, 11),
                tipo = TipoExercicio.SIX_COUNT,
                repsFeitas = 50
            )
        )

        val csv = """
            Data,Six-Counts,Navy Seals
            11/10/2026,60,40
            12/10/2026,55,0
        """.trimIndent()

        val preview = CsvManager.processarCsvImportacao(csv, existentes)
        // Linha 1: Six-Count já existe -> duplicada (1). Navy Seal não existe -> criada (1).
        // Linha 2: Six-Count -> criada (2).
        assertEquals(1, preview.totalDuplicadas)
        assertEquals(2, preview.totalSessoesCriadas)
    }

    @Test
    fun `relata linhas com data invalida nos erros com numero da linha`() {
        val csv = """
            Data,Six-Counts,Navy Seals
            11/10/2026,50,0
            data_invalida,40,20
            12/10/2026,60,30
        """.trimIndent()

        val preview = CsvManager.processarCsvImportacao(csv, emptyList())
        assertEquals(3, preview.totalLinhasLidas)
        assertEquals(1, preview.erros.size)
        assertEquals(3, preview.erros[0].numeroLinha)
        assertEquals(3, preview.totalSessoesCriadas) // Linhas 2 e 4 criam 1 + 2 = 3 sessoes
    }

    @Test
    fun `gerarCsvExportacao formata dados agrupados por dia corretamente`() {
        val sessoes = listOf(
            Sessao(dataHora = dateToMillis(2026, 10, 11), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50),
            Sessao(dataHora = dateToMillis(2026, 10, 11), tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30),
            Sessao(dataHora = dateToMillis(2026, 10, 12), tipo = TipoExercicio.SIX_COUNT, repsFeitas = 45)
        )

        val csv = CsvManager.gerarCsvExportacao(sessoes)
        assertTrue(csv.startsWith("Data,Six-Counts,Navy Seals"))
        assertTrue(csv.contains("11/10/2026,50,30"))
        assertTrue(csv.contains("12/10/2026,45,0"))
    }
}
