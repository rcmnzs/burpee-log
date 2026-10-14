package com.example.burpeelog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuracaoValidatorTest {

    @Test
    fun `validar aceita duracao minima de 1 segundo`() {
        val resultado = DuracaoValidator.validar("0", "01")
        assertTrue(resultado.ehValido)
        assertEquals(1, resultado.totalSegundos)
        assertNull(resultado.mensagemErro)
    }

    @Test
    fun `validar rejeita duracao menor que 1 segundo`() {
        val resultado = DuracaoValidator.validar("0", "00")
        assertFalse(resultado.ehValido)
        assertEquals("A duração mínima é de 1 segundo (00:01).", resultado.mensagemErro)
    }

    @Test
    fun `validar aceita duracao maxima de 3600 segundos (60 minutos)`() {
        val resultado = DuracaoValidator.validar("60", "00")
        assertTrue(resultado.ehValido)
        assertEquals(3600, resultado.totalSegundos)
        assertNull(resultado.mensagemErro)
    }

    @Test
    fun `validar rejeita duracao maior que 3600 segundos`() {
        val resultado = DuracaoValidator.validar("60", "01")
        assertFalse(resultado.ehValido)
        assertEquals("A duração máxima é de 3600 segundos (60:00).", resultado.mensagemErro)

        val resultado61Min = DuracaoValidator.validar("61", "00")
        assertFalse(resultado61Min.ehValido)
        assertEquals("A duração máxima é de 3600 segundos (60:00).", resultado61Min.mensagemErro)
    }

    @Test
    fun `validar rejeita segundos fora do intervalo de 0 a 59`() {
        val resultado60Seg = DuracaoValidator.validar("10", "60")
        assertFalse(resultado60Seg.ehValido)
        assertEquals("Os segundos devem estar entre 0 e 59.", resultado60Seg.mensagemErro)

        val resultado99Seg = DuracaoValidator.validar("10", "99")
        assertFalse(resultado99Seg.ehValido)
        assertEquals("Os segundos devem estar entre 0 e 59.", resultado99Seg.mensagemErro)
    }

    @Test
    fun `validar aceita segundos no intervalo valido de 0 a 59`() {
        val resultadoZero = DuracaoValidator.validar("20", "00")
        assertTrue(resultadoZero.ehValido)
        assertEquals(1200, resultadoZero.totalSegundos)

        val resultado59 = DuracaoValidator.validar("19", "59")
        assertTrue(resultado59.ehValido)
        assertEquals(1199, resultado59.totalSegundos)
    }

    @Test
    fun `validar rejeita campos vazios ou invalidos`() {
        val vazioMin = DuracaoValidator.validar("", "30")
        assertFalse(vazioMin.ehValido)
        assertEquals("Informe os minutos e segundos.", vazioMin.mensagemErro)

        val vazioSeg = DuracaoValidator.validar("20", "")
        assertFalse(vazioSeg.ehValido)
        assertEquals("Informe os minutos e segundos.", vazioSeg.mensagemErro)

        val naoNumerico = DuracaoValidator.validar("abc", "00")
        assertFalse(naoNumerico.ehValido)
        assertEquals("Valores numéricos inválidos.", naoNumerico.mensagemErro)
    }
}
