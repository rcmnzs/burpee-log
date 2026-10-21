package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class BackupSessionError(
    val indice: Int,
    val mensagem: String
)

data class BackupPreview(
    val totalLidas: Int,
    val totalValidas: Int,
    val totalParaAdicionar: Int,
    val totalDuplicadas: Int,
    val totalErros: Int,
    val erros: List<BackupSessionError>,
    val sessoesParaAdicionar: List<Sessao>
)

sealed class BackupParseResult {
    data class Sucesso(val preview: BackupPreview) : BackupParseResult()
    data class ArquivoMuitoGrande(val tamanhoBytes: Long) : BackupParseResult()
    data class JsonInvalido(val detalhe: String) : BackupParseResult()
    data class VersaoNaoSuportada(val versaoEncontrada: Int, val versaoSuportada: Int) : BackupParseResult()
    data class FormatoInvalido(val detalhe: String) : BackupParseResult()
}

object BackupManager {

    const val CURRENT_SCHEMA_VERSION = 1
    const val MAX_BACKUP_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    const val MIN_DURACAO_SEGUNDOS = 1
    const val MAX_DURACAO_SEGUNDOS = 3600
    const val MAX_REPS_POR_SESSAO = 5000
    const val MIN_ANO_VALIDO = 2000
    const val MAX_ANO_VALIDO = 2100

    /**
     * Gera o nome padrão do arquivo de backup no formato burpeelog-backup-AAAA-MM-DD.json.
     */
    fun gerarNomeArquivoBackup(data: LocalDate = LocalDate.now()): String {
        val dataFormatada = data.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "burpeelog-backup-$dataFormatada.json"
    }

    /**
     * Serializa a lista de sessões para o formato JSON conforme schemaVersion 1.
     * Não inclui o ID interno do banco de dados.
     */
    fun gerarBackupJson(
        sessoes: List<Sessao>,
        appVersion: String,
        exportedAt: Instant = Instant.now()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("exportedAt", exportedAt.toString())
        root.put("appVersion", appVersion)

        val sessionsArray = JSONArray()
        for (sessao in sessoes) {
            val sObj = JSONObject()
            sObj.put("dataHora", Instant.ofEpochMilli(sessao.dataHora).toString())
            sObj.put("tipo", sessao.tipo.name)
            sObj.put("duracaoSegundos", sessao.duracaoSegundos)
            sObj.put("repsFeitas", sessao.repsFeitas)

            if (sessao.repsPrevistas != null) {
                sObj.put("repsPrevistas", sessao.repsPrevistas)
            } else {
                sObj.put("repsPrevistas", JSONObject.NULL)
            }

            if (sessao.padraoIntervalosSegundos != null) {
                val pArr = JSONArray()
                sessao.padraoIntervalosSegundos.forEach { pArr.put(it) }
                sObj.put("padraoIntervalosSegundos", pArr)
            } else {
                sObj.put("padraoIntervalosSegundos", JSONObject.NULL)
            }

            if (sessao.observacao != null) {
                sObj.put("observacao", sessao.observacao)
            } else {
                sObj.put("observacao", JSONObject.NULL)
            }

            sObj.put("importada", sessao.importada)
            sessionsArray.put(sObj)
        }
        root.put("sessions", sessionsArray)

        return root.toString(2)
    }

    /**
     * Processa e valida o arquivo JSON de backup.
     *
     * Validações:
     * - Tamanho máximo de 10 MB
     * - JSON sintaticamente válido
     * - schemaVersion conhecido (se > CURRENT_SCHEMA_VERSION ou <= 0, falha geral)
     * - Cada sessão com campos válidos:
     *     - tipo conhecido (SIX_COUNT ou NAVY_SEAL)
     *     - duracaoSegundos entre 1 e 3600
     *     - repsFeitas entre 0 e 5000
     *     - data válida (ISO-8601 ou timestamp numérico com ano razoável 2000-2100)
     *     - repsPrevistas e padraoIntervalosSegundos com valores válidos se presentes
     * - Sessões inválidas são ignoradas e listadas nos erros, nunca causando falha geral.
     * - Mesclagem sem duplicidade: compara dataHora e tipo com sessões existentes (e deduplica no próprio arquivo).
     */
    fun processarBackupJson(
        jsonString: String,
        sessoesExistentes: List<Sessao>
    ): BackupParseResult {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BACKUP_SIZE_BYTES) {
            return BackupParseResult.ArquivoMuitoGrande(bytes.size.toLong())
        }

        val root = try {
            JSONObject(jsonString)
        } catch (e: JSONException) {
            return BackupParseResult.JsonInvalido(e.message ?: "Invalid JSON syntax")
        }

        if (!root.has("schemaVersion")) {
            return BackupParseResult.VersaoNaoSuportada(
                versaoEncontrada = -1,
                versaoSuportada = CURRENT_SCHEMA_VERSION
            )
        }

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion > CURRENT_SCHEMA_VERSION || schemaVersion <= 0) {
            return BackupParseResult.VersaoNaoSuportada(
                versaoEncontrada = schemaVersion,
                versaoSuportada = CURRENT_SCHEMA_VERSION
            )
        }

        val sessionsArray = root.optJSONArray("sessions")
            ?: return BackupParseResult.FormatoInvalido("Missing 'sessions' array")

        val totalLidas = sessionsArray.length()
        val erros = mutableListOf<BackupSessionError>()
        val sessoesValidas = mutableListOf<Sessao>()

        for (i in 0 until totalLidas) {
            val itemIndex = i + 1
            val sObj = sessionsArray.optJSONObject(i)
            if (sObj == null) {
                erros.add(BackupSessionError(itemIndex, "Item $itemIndex não é um objeto JSON válido"))
                continue
            }

            // Validação de dataHora
            val rawDataHora = sObj.opt("dataHora")
            if (rawDataHora == null || rawDataHora == JSONObject.NULL) {
                erros.add(BackupSessionError(itemIndex, "Campo 'dataHora' ausente ou nulo"))
                continue
            }
            val dataHoraMillis = parseDataHora(rawDataHora)
            if (dataHoraMillis == null) {
                erros.add(BackupSessionError(itemIndex, "Data inválida ou fora do intervalo 2000-2100: '$rawDataHora'"))
                continue
            }

            // Validação de tipo
            val rawTipo = sObj.optString("tipo", "")
            val tipo = TipoExercicio.values().find { it.name.equals(rawTipo, ignoreCase = true) }
            if (tipo == null) {
                erros.add(BackupSessionError(itemIndex, "Tipo de exercício desconhecido: '$rawTipo'"))
                continue
            }

            // Validação de duracaoSegundos
            if (!sObj.has("duracaoSegundos") || sObj.isNull("duracaoSegundos")) {
                erros.add(BackupSessionError(itemIndex, "Campo obrigatório 'duracaoSegundos' ausente"))
                continue
            }
            val duracaoSegundos = sObj.optInt("duracaoSegundos", -1)
            if (duracaoSegundos !in MIN_DURACAO_SEGUNDOS..MAX_DURACAO_SEGUNDOS) {
                erros.add(
                    BackupSessionError(
                        itemIndex,
                        "Duração fora do limite permitido ($MIN_DURACAO_SEGUNDOS..$MAX_DURACAO_SEGUNDOS segundos): $duracaoSegundos"
                    )
                )
                continue
            }

            // Validação de repsFeitas
            if (!sObj.has("repsFeitas") || sObj.isNull("repsFeitas")) {
                erros.add(BackupSessionError(itemIndex, "Campo obrigatório 'repsFeitas' ausente"))
                continue
            }
            val repsFeitas = sObj.optInt("repsFeitas", -1)
            if (repsFeitas < 0 || repsFeitas > MAX_REPS_POR_SESSAO) {
                erros.add(
                    BackupSessionError(
                        itemIndex,
                        "Repetições feitas fora do limite permitido (0..$MAX_REPS_POR_SESSAO): $repsFeitas"
                    )
                )
                continue
            }

            // Validação de repsPrevistas (opcional)
            var repsPrevistas: Int? = null
            if (sObj.has("repsPrevistas") && !sObj.isNull("repsPrevistas")) {
                val rp = sObj.optInt("repsPrevistas", -1)
                if (rp < 0 || rp > MAX_REPS_POR_SESSAO) {
                    erros.add(
                        BackupSessionError(
                            itemIndex,
                            "Repetições previstas fora do limite permitido: $rp"
                        )
                    )
                    continue
                }
                repsPrevistas = rp
            }

            // Validação de padraoIntervalosSegundos (opcional)
            var padraoIntervalos: List<Int>? = null
            if (sObj.has("padraoIntervalosSegundos") && !sObj.isNull("padraoIntervalosSegundos")) {
                val pArr = sObj.optJSONArray("padraoIntervalosSegundos")
                if (pArr == null) {
                    erros.add(BackupSessionError(itemIndex, "Campo 'padraoIntervalosSegundos' inválido"))
                    continue
                }
                var itemInvalido = false
                val list = mutableListOf<Int>()
                for (k in 0 until pArr.length()) {
                    val v = pArr.optInt(k, -1)
                    if (v <= 0 || v > 3600) {
                        itemInvalido = true
                        break
                    }
                    list.add(v)
                }
                if (itemInvalido || list.isEmpty()) {
                    erros.add(
                        BackupSessionError(
                            itemIndex,
                            "Intervalos da cadência devem conter valores inteiros entre 1 e 3600 segundos"
                        )
                    )
                    continue
                }
                padraoIntervalos = list
            }

            // Validação de observação (opcional, truncada com segurança se excessiva)
            var observacao: String? = null
            if (sObj.has("observacao") && !sObj.isNull("observacao")) {
                val obs = sObj.optString("observacao")
                observacao = if (obs.length > 5000) obs.take(5000) else obs
            }

            val importada = sObj.optBoolean("importada", false)

            sessoesValidas.add(
                Sessao(
                    id = 0L,
                    dataHora = dataHoraMillis,
                    tipo = tipo,
                    padraoIntervalosSegundos = padraoIntervalos,
                    repsPrevistas = repsPrevistas,
                    repsFeitas = repsFeitas,
                    duracaoSegundos = duracaoSegundos,
                    observacao = observacao,
                    importada = importada
                )
            )
        }

        // Deduplicação e mesclagem
        val existingKeys = sessoesExistentes.map { it.dataHora to it.tipo }.toMutableSet()
        val sessoesParaAdicionar = mutableListOf<Sessao>()
        var totalDuplicadas = 0

        for (sessao in sessoesValidas) {
            val key = sessao.dataHora to sessao.tipo
            if (key in existingKeys) {
                totalDuplicadas++
            } else {
                existingKeys.add(key)
                sessoesParaAdicionar.add(sessao)
            }
        }

        val preview = BackupPreview(
            totalLidas = totalLidas,
            totalValidas = sessoesValidas.size,
            totalParaAdicionar = sessoesParaAdicionar.size,
            totalDuplicadas = totalDuplicadas,
            totalErros = erros.size,
            erros = erros,
            sessoesParaAdicionar = sessoesParaAdicionar
        )

        return BackupParseResult.Sucesso(preview)
    }

    /**
     * Converte dataHora para timestamp em milissegundos.
     * Suporta formato ISO-8601 (Instant, OffsetDateTime, ZonedDateTime, LocalDateTime, LocalDate)
     * e também números inteiros (milissegundos da época).
     */
    fun parseDataHora(raw: Any?): Long? {
        if (raw == null || raw == JSONObject.NULL) return null

        if (raw is Number) {
            val millis = raw.toLong()
            return if (validarTimestamp(millis)) millis else null
        }

        if (raw is String) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            // Se for número em texto
            trimmed.toLongOrNull()?.let { millis ->
                if (validarTimestamp(millis)) return millis
            }

            // ISO-8601 Instant (ex: 2026-10-20T14:00:38Z ou 2026-10-20T14:00:38.123Z)
            try {
                val instant = Instant.parse(trimmed)
                val millis = instant.toEpochMilli()
                if (validarTimestamp(millis)) return millis
            } catch (_: Exception) {}

            // OffsetDateTime (ex: 2026-10-20T14:00:38-03:00)
            try {
                val odt = OffsetDateTime.parse(trimmed)
                val millis = odt.toInstant().toEpochMilli()
                if (validarTimestamp(millis)) return millis
            } catch (_: Exception) {}

            // ZonedDateTime
            try {
                val zdt = ZonedDateTime.parse(trimmed)
                val millis = zdt.toInstant().toEpochMilli()
                if (validarTimestamp(millis)) return millis
            } catch (_: Exception) {}

            // LocalDateTime (sem offset/zona de fuso)
            try {
                val ldt = LocalDateTime.parse(trimmed)
                val millis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (validarTimestamp(millis)) return millis
            } catch (_: Exception) {}

            // LocalDate (ex: 2026-10-20)
            try {
                val ld = LocalDate.parse(trimmed)
                val millis = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (validarTimestamp(millis)) return millis
            } catch (_: Exception) {}
        }

        return null
    }

    private fun validarTimestamp(millis: Long): Boolean {
        if (millis <= 0) return false
        return try {
            val ano = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).year
            ano in MIN_ANO_VALIDO..MAX_ANO_VALIDO
        } catch (_: Exception) {
            false
        }
    }
}
