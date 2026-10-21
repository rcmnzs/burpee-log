package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupManagerTest {

    @Test
    fun `gerarNomeArquivoBackup produz formato burpeelog-backup-AAAA-MM-DD json`() {
        val data = LocalDate.of(2026, 10, 21)
        val nome = BackupManager.gerarNomeArquivoBackup(data)
        assertEquals("burpeelog-backup-2026-10-21.json", nome)
    }

    @Test
    fun `ida e volta - exportar e restaurar produz as mesmas sessoes com todos os campos`() {
        val agoraInstant = Instant.parse("2026-10-21T10:30:00Z")
        val dataHora1 = Instant.parse("2026-10-20T14:00:00Z").toEpochMilli()
        val dataHora2 = Instant.parse("2026-10-21T08:15:30Z").toEpochMilli()

        val sessao1 = Sessao(
            id = 1L, // id interno no DB não deve ser exportado
            dataHora = dataHora1,
            tipo = TipoExercicio.SIX_COUNT,
            padraoIntervalosSegundos = listOf(30, 20),
            repsPrevistas = 50,
            repsFeitas = 48,
            duracaoSegundos = 1200,
            observacao = "Treino forte com observações especiais e acentuação: Olá mundo!",
            importada = false
        )

        val sessao2 = Sessao(
            id = 2L,
            dataHora = dataHora2,
            tipo = TipoExercicio.NAVY_SEAL,
            padraoIntervalosSegundos = null,
            repsPrevistas = null,
            repsFeitas = 32,
            duracaoSegundos = 350,
            observacao = null,
            importada = true
        )

        val sessoesOriginais = listOf(sessao1, sessao2)

        // Exportação
        val jsonString = BackupManager.gerarBackupJson(
            sessoes = sessoesOriginais,
            appVersion = "1.0.9",
            exportedAt = agoraInstant
        )

        // Verifica que o ID interno não foi exportado no JSON
        val rootJson = JSONObject(jsonString)
        assertEquals(1, rootJson.getInt("schemaVersion"))
        assertEquals("1.0.9", rootJson.getString("appVersion"))
        assertEquals(agoraInstant.toString(), rootJson.getString("exportedAt"))

        val sessionsArray = rootJson.getJSONArray("sessions")
        assertEquals(2, sessionsArray.length())
        val firstObj = sessionsArray.getJSONObject(0)
        assertTrue(!firstObj.has("id"))

        // Restauração a partir do JSON gerado
        val resultado = BackupManager.processarBackupJson(jsonString, emptyList())
        assertTrue("Resultado deve ser Sucesso", resultado is BackupParseResult.Sucesso)

        val preview = (resultado as BackupParseResult.Sucesso).preview
        assertEquals(2, preview.totalLidas)
        assertEquals(2, preview.totalValidas)
        assertEquals(2, preview.totalParaAdicionar)
        assertEquals(0, preview.totalDuplicadas)
        assertEquals(0, preview.totalErros)
        assertTrue(preview.erros.isEmpty())

        val restauradas = preview.sessoesParaAdicionar
        assertEquals(2, restauradas.size)

        // Validação da Sessão 1
        val r1 = restauradas[0]
        assertEquals(dataHora1, r1.dataHora)
        assertEquals(TipoExercicio.SIX_COUNT, r1.tipo)
        assertEquals(listOf(30, 20), r1.padraoIntervalosSegundos)
        assertEquals(50, r1.repsPrevistas)
        assertEquals(48, r1.repsFeitas)
        assertEquals(1200, r1.duracaoSegundos)
        assertEquals("Treino forte com observações especiais e acentuação: Olá mundo!", r1.observacao)
        assertEquals(false, r1.importada)

        // Validação da Sessão 2
        val r2 = restauradas[1]
        assertEquals(dataHora2, r2.dataHora)
        assertEquals(TipoExercicio.NAVY_SEAL, r2.tipo)
        assertNull(r2.padraoIntervalosSegundos)
        assertNull(r2.repsPrevistas)
        assertEquals(32, r2.repsFeitas)
        assertEquals(350, r2.duracaoSegundos)
        assertNull(r2.observacao)
        assertEquals(true, r2.importada)
    }

    @Test
    fun `duplicidade na mesclagem - ignora sessoes ja existentes no banco e duplicatas no proprio arquivo`() {
        val dataHoraA = Instant.parse("2026-10-18T10:00:00Z").toEpochMilli()
        val dataHoraB = Instant.parse("2026-10-19T10:00:00Z").toEpochMilli()
        val dataHoraC = Instant.parse("2026-10-20T10:00:00Z").toEpochMilli()

        // Sessões existentes no banco
        val sessoesExistentes = listOf(
            Sessao(dataHora = dataHoraA, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50, duracaoSegundos = 1200),
            Sessao(dataHora = dataHoraB, tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30, duracaoSegundos = 1200)
        )

        // Sessões no arquivo JSON:
        // 1. Sessão com dataHoraA e SIX_COUNT (duplicada com banco)
        // 2. Sessão com dataHoraA e NAVY_SEAL (tipo diferente na mesma data - DEVE ser adicionada!)
        // 3. Sessão com dataHoraC e SIX_COUNT (nova)
        // 4. Sessão com dataHoraC e SIX_COUNT (duplicada dentro do próprio arquivo - deve ser ignorada)
        val jsonArray = JSONArray().apply {
            put(JSONObject().apply {
                put("dataHora", "2026-10-18T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 50)
            })
            put(JSONObject().apply {
                put("dataHora", "2026-10-18T10:00:00Z")
                put("tipo", "NAVY_SEAL")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 25)
            })
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 55)
            })
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 55)
            })
        }

        val jsonString = JSONObject().apply {
            put("schemaVersion", 1)
            put("exportedAt", "2026-10-21T00:00:00Z")
            put("appVersion", "1.0.9")
            put("sessions", jsonArray)
        }.toString()

        val resultado = BackupManager.processarBackupJson(jsonString, sessoesExistentes)
        assertTrue(resultado is BackupParseResult.Sucesso)

        val preview = (resultado as BackupParseResult.Sucesso).preview
        assertEquals(4, preview.totalLidas)
        assertEquals(4, preview.totalValidas)
        assertEquals(2, preview.totalParaAdicionar)
        assertEquals(2, preview.totalDuplicadas)
        assertEquals(0, preview.totalErros)

        // As duas que devem ser adicionadas: (dataHoraA, NAVY_SEAL) e (dataHoraC, SIX_COUNT)
        assertEquals(2, preview.sessoesParaAdicionar.size)
        assertEquals(TipoExercicio.NAVY_SEAL, preview.sessoesParaAdicionar[0].tipo)
        assertEquals(TipoExercicio.SIX_COUNT, preview.sessoesParaAdicionar[1].tipo)
    }

    @Test
    fun `arquivo com schemaVersion maior que o suportado retorna VersaoNaoSuportada sem gravar`() {
        val jsonString = """
            {
              "schemaVersion": 2,
              "exportedAt": "2026-10-21T10:00:00Z",
              "appVersion": "2.0.0",
              "sessions": [
                {
                  "dataHora": "2026-10-20T14:00:00Z",
                  "tipo": "SIX_COUNT",
                  "duracaoSegundos": 1200,
                  "repsFeitas": 50
                }
              ]
            }
        """.trimIndent()

        val resultado = BackupManager.processarBackupJson(jsonString, emptyList())
        assertTrue("Deve falhar com VersaoNaoSuportada", resultado is BackupParseResult.VersaoNaoSuportada)
        val versao = resultado as BackupParseResult.VersaoNaoSuportada
        assertEquals(2, versao.versaoEncontrada)
        assertEquals(BackupManager.CURRENT_SCHEMA_VERSION, versao.versaoSuportada)
    }

    @Test
    fun `JSON invalido sintaticamente retorna JsonInvalido`() {
        val jsonInvalido = "{ schemaVersion: 1, sessions: [ incomplete json ... "

        val resultado = BackupManager.processarBackupJson(jsonInvalido, emptyList())
        assertTrue("Deve retornar JsonInvalido", resultado is BackupParseResult.JsonInvalido)
    }

    @Test
    fun `sessoes com campos fora do limite sao ignoradas com erro sem falha geral`() {
        val sessionsArray = JSONArray().apply {
            // Item 1: Duração = 0 (< 1 seg)
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 0)
                put("repsFeitas", 40)
            })
            // Item 2: Duração = 3601 (> 3600 seg)
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 3601)
                put("repsFeitas", 40)
            })
            // Item 3: Reps feitas < 0
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", -5)
            })
            // Item 4: Reps feitas > 5000
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 5001)
            })
            // Item 5: Tipo de exercício desconhecido
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "BURPEE_EXTREMO")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 40)
            })
            // Item 6: Data inválida (texto não data)
            put(JSONObject().apply {
                put("dataHora", "data-invalida-xyz")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 40)
            })
            // Item 7: Data fora do intervalo 2000-2100 (ano 1980)
            put(JSONObject().apply {
                put("dataHora", "1980-05-10T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 40)
            })
            // Item 8: Cadência com intervalo inválido (0s)
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "SIX_COUNT")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 40)
                put("padraoIntervalosSegundos", JSONArray().apply { put(0) })
            })
            // Item 9: SESSÃO VÁLIDA (deve ser aceita com sucesso apesar dos outros itens inválidos!)
            put(JSONObject().apply {
                put("dataHora", "2026-10-20T10:00:00Z")
                put("tipo", "NAVY_SEAL")
                put("duracaoSegundos", 1200)
                put("repsFeitas", 35)
                put("observacao", "Sessão válida")
            })
        }

        val jsonString = JSONObject().apply {
            put("schemaVersion", 1)
            put("exportedAt", "2026-10-21T00:00:00Z")
            put("appVersion", "1.0.9")
            put("sessions", sessionsArray)
        }.toString()

        val resultado = BackupManager.processarBackupJson(jsonString, emptyList())
        assertTrue("Deve processar sem falha geral", resultado is BackupParseResult.Sucesso)

        val preview = (resultado as BackupParseResult.Sucesso).preview
        assertEquals(9, preview.totalLidas)
        assertEquals(1, preview.totalValidas)
        assertEquals(8, preview.totalErros)
        assertEquals(1, preview.totalParaAdicionar)
        assertEquals(0, preview.totalDuplicadas)

        assertEquals(8, preview.erros.size)
        // O único adicionado deve ser a sessão válida (NAVY_SEAL, 35 reps)
        assertEquals(1, preview.sessoesParaAdicionar.size)
        assertEquals(TipoExercicio.NAVY_SEAL, preview.sessoesParaAdicionar[0].tipo)
        assertEquals(35, preview.sessoesParaAdicionar[0].repsFeitas)
    }

    @Test
    fun `parseDataHora suporta timestamp numerico e diversos formatos ISO-8601`() {
        val instantExpected = Instant.parse("2026-10-20T14:30:00Z").toEpochMilli()

        // Instant ISO-8601
        assertEquals(instantExpected, BackupManager.parseDataHora("2026-10-20T14:30:00Z"))

        // Número Long direto
        assertEquals(instantExpected, BackupManager.parseDataHora(instantExpected))

        // Número como String
        assertEquals(instantExpected, BackupManager.parseDataHora(instantExpected.toString()))

        // LocalDate simples (yyyy-MM-dd)
        assertNotNull(BackupManager.parseDataHora("2026-10-20"))

        // Null / Vazio
        assertNull(BackupManager.parseDataHora(null))
        assertNull(BackupManager.parseDataHora(""))
        assertNull(BackupManager.parseDataHora("   "))

        // Ano fora do limite 2000-2100
        assertNull(BackupManager.parseDataHora("1995-01-01T00:00:00Z"))
        assertNull(BackupManager.parseDataHora("2150-01-01T00:00:00Z"))
    }
}
