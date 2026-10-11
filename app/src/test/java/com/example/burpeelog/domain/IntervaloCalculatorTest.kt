package com.example.burpeelog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervaloCalculatorTest {

    @Test
    fun `calcularInstantesDoMinuto gera instantes corretos conforme regras`() {
        // Exemplo da especificação: 30 s = reps em 0 e 30
        assertEquals(listOf(0, 30), IntervaloCalculator.calcularInstantesDoMinuto(30))

        // Exemplo da especificação: 20 s = reps em 0, 20 e 40
        assertEquals(listOf(0, 20, 40), IntervaloCalculator.calcularInstantesDoMinuto(20))

        // Exemplo da especificação: 25 s = reps em 0, 25 e 50
        assertEquals(listOf(0, 25, 50), IntervaloCalculator.calcularInstantesDoMinuto(25))

        // Exemplo da especificação: 10 s = reps em 0, 10, 20, 30, 40, 50
        assertEquals(listOf(0, 10, 20, 30, 40, 50), IntervaloCalculator.calcularInstantesDoMinuto(10))

        // Limite máximo: 60 s = rep apenas no segundo 0
        assertEquals(listOf(0), IntervaloCalculator.calcularInstantesDoMinuto(60))

        // Limite mínimo: 2 s = 30 reps (0, 2, ..., 58)
        val instantes2s = IntervaloCalculator.calcularInstantesDoMinuto(2)
        assertEquals(30, instantes2s.size)
        assertEquals(0, instantes2s.first())
        assertEquals(58, instantes2s.last())
    }

    @Test
    fun `calcularRepsNoMinuto retorna a quantidade correta`() {
        assertEquals(2, IntervaloCalculator.calcularRepsNoMinuto(30))
        assertEquals(3, IntervaloCalculator.calcularRepsNoMinuto(20))
        assertEquals(3, IntervaloCalculator.calcularRepsNoMinuto(25))
        assertEquals(6, IntervaloCalculator.calcularRepsNoMinuto(10))
        assertEquals(1, IntervaloCalculator.calcularRepsNoMinuto(60))
        assertEquals(30, IntervaloCalculator.calcularRepsNoMinuto(2))
    }

    @Test
    fun `calcularRepsPrevistas calcula total de 20 minutos com padrao constante`() {
        // [10] = 6 reps/min * 20 min = 120 reps
        assertEquals(120, IntervaloCalculator.calcularRepsPrevistas(listOf(10), 20))

        // [25] = 3 reps/min * 20 min = 60 reps
        assertEquals(60, IntervaloCalculator.calcularRepsPrevistas(listOf(25), 20))

        // [60] = 1 rep/min * 20 min = 20 reps
        assertEquals(20, IntervaloCalculator.calcularRepsPrevistas(listOf(60), 20))
    }

    @Test
    fun `calcularRepsPrevistas calcula total de 20 minutos com padrao alternado ciclico`() {
        // [30, 20]: Minuto ímpar (30s = 2 reps), Minuto par (20s = 3 reps).
        // Em 20 minutos há 10 minutos de 30s e 10 minutos de 20s = 10*2 + 10*3 = 50 reps.
        assertEquals(50, IntervaloCalculator.calcularRepsPrevistas(listOf(30, 20), 20))

        // Ciclo de 3 itens: [30, 30, 20] (reps: 2, 2, 3)
        // 20 min = 6 ciclos completos (18 min) + 2 min adicionais
        // 6 * (2 + 2 + 3) = 42 + min19(2) + min20(2) = 46 reps
        assertEquals(46, IntervaloCalculator.calcularRepsPrevistas(listOf(30, 30, 20), 20))
    }

    @Test
    fun `calcularInstantesDasReps produz timestamps absolutos ordenados e coerentes`() {
        val padrao = listOf(30, 20)
        val instantes = IntervaloCalculator.calcularInstantesDasReps(padrao, duracaoMinutos = 20)

        // Tamanho deve bater com as reps previstas (50 reps)
        assertEquals(50, instantes.size)

        // Deve começar no segundo 0
        assertEquals(0, instantes[0])

        // Minuto 1 (segundos 0..59): 0 e 30
        assertEquals(30, instantes[1])

        // Minuto 2 (segundos 60..119): 60, 80, 100
        assertEquals(60, instantes[2])
        assertEquals(80, instantes[3])
        assertEquals(100, instantes[4])

        // Minuto 3 (segundos 120..179): 120, 150
        assertEquals(120, instantes[5])
        assertEquals(150, instantes[6])

        // Verificar estrita ordem crescente
        for (i in 0 until instantes.size - 1) {
            assertTrue("Instantes devem ser estritamente crescentes", instantes[i] < instantes[i + 1])
        }

        // O último instante deve ser menor que 20 * 60 = 1200
        assertTrue(instantes.last() < 20 * 60)
    }

    @Test
    fun `gerarPreviaDoCiclo formata corretamente a previa textual`() {
        val previa = IntervaloCalculator.gerarPreviaDoCiclo(listOf(30, 20))
        assertEquals("Min 1: 2 reps (30 s) · Min 2: 3 reps (20 s)", previa)

        val previaUnica = IntervaloCalculator.gerarPreviaDoCiclo(listOf(10))
        assertEquals("Min 1: 6 reps (10 s)", previaUnica)
    }
}
