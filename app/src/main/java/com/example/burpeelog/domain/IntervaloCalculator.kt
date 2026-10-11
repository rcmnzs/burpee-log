package com.example.burpeelog.domain

import com.example.burpeelog.data.model.DURACAO_MINUTOS

/**
 * Funções puras de cálculo para o padrão de intervalos e reps do Busy Dad Program.
 */
object IntervaloCalculator {

    const val INTERVALO_MIN_SEGUNDOS = 2
    const val INTERVALO_MAX_SEGUNDOS = 60
    const val SEGUNDOS_POR_MINUTO = 60

    /**
     * Calcula os instantes relativos (em segundos dentro do minuto, de 0 a 59)
     * em que as repetições ocorrem.
     *
     * A repetição sempre inicia no segundo 0 do minuto e continua a cada [intervaloSegundos]
     * enquanto o instante for menor que 60.
     */
    fun calcularInstantesDoMinuto(intervaloSegundos: Int): List<Int> {
        if (intervaloSegundos < INTERVALO_MIN_SEGUNDOS) {
            return emptyList()
        }
        val instantes = mutableListOf<Int>()
        var segundo = 0
        while (segundo < SEGUNDOS_POR_MINUTO) {
            instantes.add(segundo)
            segundo += intervaloSegundos
        }
        return instantes
    }

    /**
     * Calcula quantas repetições ocorrem em um único minuto com o intervalo fornecido.
     */
    fun calcularRepsNoMinuto(intervaloSegundos: Int): Int {
        if (intervaloSegundos < INTERVALO_MIN_SEGUNDOS) return 0
        return calcularInstantesDoMinuto(intervaloSegundos).size
    }

    /**
     * Calcula o total de repetições previstas para a sessão completa de [duracaoMinutos] (padrão 20).
     * O padrão se repete ciclicamente a cada minuto.
     */
    fun calcularRepsPrevistas(
        padrao: List<Int>,
        duracaoMinutos: Int = DURACAO_MINUTOS
    ): Int {
        if (padrao.isEmpty() || duracaoMinutos <= 0) return 0
        var total = 0
        for (minuto in 0 until duracaoMinutos) {
            val intervalo = padrao[minuto % padrao.size]
            total += calcularRepsNoMinuto(intervalo)
        }
        return total
    }

    /**
     * Retorna a lista completa e ordenada de todos os instantes absolutos (em segundos do treino,
     * de 0 até duracaoMinutos * 60 - 1) em que cada repetição deve ser executada.
     */
    fun calcularInstantesDasReps(
        padrao: List<Int>,
        duracaoMinutos: Int = DURACAO_MINUTOS
    ): List<Int> {
        if (padrao.isEmpty() || duracaoMinutos <= 0) return emptyList()
        val todosInstantes = mutableListOf<Int>()
        for (minuto in 0 until duracaoMinutos) {
            val intervalo = padrao[minuto % padrao.size]
            val instantesDoMinuto = calcularInstantesDoMinuto(intervalo)
            val offsetMinuto = minuto * SEGUNDOS_POR_MINUTO
            for (segundoRelativo in instantesDoMinuto) {
                todosInstantes.add(offsetMinuto + segundoRelativo)
            }
        }
        return todosInstantes
    }

    /**
     * Gera uma string formatada de prévia do ciclo para exibição na UI.
     * Exemplo: "Min 1: 2 reps (30 s) · Min 2: 3 reps (20 s)"
     */
    fun gerarPreviaDoCiclo(padrao: List<Int>): String {
        if (padrao.isEmpty()) return "Nenhum intervalo configurado"
        return padrao.mapIndexed { index, seg ->
            val reps = calcularRepsNoMinuto(seg)
            val repsTexto = if (reps == 1) "1 rep" else "$reps reps"
            "Min ${index + 1}: $repsTexto (${seg} s)"
        }.joinToString(" · ")
    }
}
