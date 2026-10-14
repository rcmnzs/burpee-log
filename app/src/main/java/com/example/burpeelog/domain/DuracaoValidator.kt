package com.example.burpeelog.domain

/**
 * Validador para os campos de duração (minutos e segundos).
 * Regras:
 * - Mínimo: 1 segundo (00:01)
 * - Máximo: 3600 segundos (60:00)
 * - Segundos devem estar no intervalo de 0 a 59
 */
object DuracaoValidator {

    data class ResultadoValidacao(
        val ehValido: Boolean,
        val totalSegundos: Int = 0,
        val mensagemErro: String? = null
    )

    fun validar(minutosTexto: String, segundosTexto: String): ResultadoValidacao {
        if (minutosTexto.isBlank() || segundosTexto.isBlank()) {
            return ResultadoValidacao(
                ehValido = false,
                mensagemErro = "Informe os minutos e segundos."
            )
        }
        val min = minutosTexto.toIntOrNull()
        val seg = segundosTexto.toIntOrNull()
        if (min == null || seg == null) {
            return ResultadoValidacao(
                ehValido = false,
                mensagemErro = "Valores numéricos inválidos."
            )
        }
        if (seg !in 0..59) {
            return ResultadoValidacao(
                ehValido = false,
                mensagemErro = "Os segundos devem estar entre 0 e 59."
            )
        }
        val total = (min * 60) + seg
        if (total < 1) {
            return ResultadoValidacao(
                ehValido = false,
                mensagemErro = "A duração mínima é de 1 segundo (00:01)."
            )
        }
        if (total > 3600) {
            return ResultadoValidacao(
                ehValido = false,
                mensagemErro = "A duração máxima é de 3600 segundos (60:00)."
            )
        }
        return ResultadoValidacao(ehValido = true, totalSegundos = total)
    }
}
