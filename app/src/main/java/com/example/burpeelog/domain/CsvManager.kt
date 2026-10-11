package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class CsvError(
    val numeroLinha: Int,
    val mensagem: String
)

data class CsvPreviewLinha(
    val numeroLinha: Int,
    val dataFormatada: String,
    val sixCounts: Int,
    val navySeals: Int
)

data class CsvImportPreview(
    val totalLinhasLidas: Int,
    val totalSessoesCriadas: Int,
    val totalDuplicadas: Int,
    val primeirasLinhas: List<CsvPreviewLinha>,
    val erros: List<CsvError>,
    val sessoesParaCriar: List<Sessao>
)

object CsvManager {

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy")
    )

    fun normalizarTexto(texto: String): String {
        val nfd = Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .replace("[-_\\s]".toRegex(), "")
    }

    fun parseDate(rawDate: String): LocalDate? {
        val clean = rawDate.trim()
        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, formatter)
            } catch (_: DateTimeParseException) {}
        }
        return null
    }

    /**
     * Analisa o conteúdo CSV e gera a prévia completa de importação sem gravar no banco.
     */
    fun processarCsvImportacao(
        conteudoCsv: String,
        sessoesExistentes: List<Sessao>
    ): CsvImportPreview {
        val linhasBrutas = conteudoCsv.lines()
        if (linhasBrutas.isEmpty()) {
            return CsvImportPreview(0, 0, 0, emptyList(), listOf(CsvError(1, "Arquivo CSV está vazio")), emptyList())
        }

        // Encontrar linha de cabeçalho
        var headerIndex = -1
        var headerLine = ""
        for (i in linhasBrutas.indices) {
            val l = linhasBrutas[i].trim()
            if (l.isNotEmpty()) {
                headerIndex = i
                headerLine = l
                break
            }
        }

        if (headerIndex == -1) {
            return CsvImportPreview(0, 0, 0, emptyList(), listOf(CsvError(1, "Nenhum cabeçalho encontrado")), emptyList())
        }

        // Detectar separador (vírgula ou ponto e vírgula)
        val separador = if (headerLine.count { it == ';' } > headerLine.count { it == ',' }) ';' else ','

        val colunasCabecalho = headerLine.split(separador).map { normalizarTexto(it) }

        var idxData = -1
        var idxSix = -1
        var idxNavy = -1

        colunasCabecalho.forEachIndexed { index, col ->
            when {
                col.contains("data") || col.contains("date") || col.contains("dia") -> idxData = index
                col.contains("six") || col.contains("6count") -> idxSix = index
                col.contains("navy") -> idxNavy = index
            }
        }

        val erros = mutableListOf<CsvError>()

        if (idxData == -1 || idxSix == -1 || idxNavy == -1) {
            val faltantes = mutableListOf<String>()
            if (idxData == -1) faltantes.add("Data")
            if (idxSix == -1) faltantes.add("Six-Counts")
            if (idxNavy == -1) faltantes.add("Navy Seals")
            erros.add(CsvError(headerIndex + 1, "Colunas obrigatórias não encontradas no cabeçalho: ${faltantes.joinToString(", ")}"))
            return CsvImportPreview(0, 0, 0, emptyList(), erros, emptyList())
        }

        // Conjunto de chaves de sessões existentes (LocalDate + TipoExercicio)
        val chavesExistentes = HashSet<Pair<LocalDate, TipoExercicio>>()
        for (sessao in sessoesExistentes) {
            val data = DashboardCalculator.timestampToLocalDate(sessao.dataHora)
            chavesExistentes.add(Pair(data, sessao.tipo))
        }

        val sessoesParaCriar = mutableListOf<Sessao>()
        val primeirasLinhas = mutableListOf<CsvPreviewLinha>()
        var totalLinhasLidas = 0
        var totalDuplicadas = 0

        for (i in (headerIndex + 1) until linhasBrutas.size) {
            val linha = linhasBrutas[i].trim()
            if (linha.isEmpty()) continue

            val numeroLinhaAtual = i + 1
            totalLinhasLidas++

            val celulas = linha.split(separador).map { it.trim() }
            val maxIdx = maxOf(idxData, idxSix, idxNavy)
            if (celulas.size <= maxIdx) {
                erros.add(CsvError(numeroLinhaAtual, "Número de colunas insuficiente (${celulas.size} colunas encontradas)"))
                continue
            }

            val rawData = celulas[idxData]
            val localDate = parseDate(rawData)
            if (localDate == null) {
                erros.add(CsvError(numeroLinhaAtual, "Data inválida ou em formato desconhecido: '$rawData'"))
                continue
            }

            val sixCounts = celulas[idxSix].toIntOrNull()?.coerceAtLeast(0) ?: 0
            val navySeals = celulas[idxNavy].toIntOrNull()?.coerceAtLeast(0) ?: 0

            // Se for uma das 5 primeiras linhas lidas, registra na prévia
            if (primeirasLinhas.size < 5) {
                val dataFormatada = String.format("%02d/%02d/%04d", localDate.dayOfMonth, localDate.monthValue, localDate.year)
                primeirasLinhas.add(CsvPreviewLinha(numeroLinhaAtual, dataFormatada, sixCounts, navySeals))
            }

            // Timestamp fixo às 12:00:00 da data
            val dataHoraMillis = localDate.atTime(12, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Processar Six-Count se > 0
            if (sixCounts > 0) {
                val chaveSix = Pair(localDate, TipoExercicio.SIX_COUNT)
                if (chavesExistentes.contains(chaveSix)) {
                    totalDuplicadas++
                } else {
                    chavesExistentes.add(chaveSix)
                    sessoesParaCriar.add(
                        Sessao(
                            dataHora = dataHoraMillis,
                            tipo = TipoExercicio.SIX_COUNT,
                            padraoIntervalosSegundos = null,
                            repsPrevistas = null,
                            repsFeitas = sixCounts,
                            observacao = null,
                            importada = true
                        )
                    )
                }
            }

            // Processar Navy Seal se > 0
            if (navySeals > 0) {
                val chaveNavy = Pair(localDate, TipoExercicio.NAVY_SEAL)
                if (chavesExistentes.contains(chaveNavy)) {
                    totalDuplicadas++
                } else {
                    chavesExistentes.add(chaveNavy)
                    sessoesParaCriar.add(
                        Sessao(
                            dataHora = dataHoraMillis,
                            tipo = TipoExercicio.NAVY_SEAL,
                            padraoIntervalosSegundos = null,
                            repsPrevistas = null,
                            repsFeitas = navySeals,
                            observacao = null,
                            importada = true
                        )
                    )
                }
            }
        }

        return CsvImportPreview(
            totalLinhasLidas = totalLinhasLidas,
            totalSessoesCriadas = sessoesParaCriar.size,
            totalDuplicadas = totalDuplicadas,
            primeirasLinhas = primeirasLinhas,
            erros = erros,
            sessoesParaCriar = sessoesParaCriar
        )
    }

    /**
     * Gera o conteúdo CSV para exportação: Data (dd/MM/yyyy), Six-Counts, Navy Seals
     * agrupando as sessões do mesmo dia.
     */
    fun gerarCsvExportacao(sessoes: List<Sessao>): String {
        val cabecalho = "Data,Six-Counts,Navy Seals\n"
        val agrupado = HashMap<LocalDate, Pair<Int, Int>>()

        for (s in sessoes) {
            val date = DashboardCalculator.timestampToLocalDate(s.dataHora)
            val atual = agrupado[date] ?: Pair(0, 0)
            agrupado[date] = if (s.tipo == TipoExercicio.SIX_COUNT) {
                Pair(atual.first + s.repsFeitas, atual.second)
            } else {
                Pair(atual.first, atual.second + s.repsFeitas)
            }
        }

        val linhas = agrupado.entries
            .sortedByDescending { it.key }
            .joinToString("\n") { (date, reps) ->
                val dataStr = String.format("%02d/%02d/%04d", date.dayOfMonth, date.monthValue, date.year)
                "$dataStr,${reps.first},${reps.second}"
            }

        return cabecalho + linhas
    }
}
