package com.example.burpeelog.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemePreferencesTest {

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        themePreferences = ThemePreferences(context)
    }

    @Test
    fun `padrao do tema e ESCURO`() = runTest {
        val tema = themePreferences.temaFlow.first()
        // Padrão solicitado: ESCURO
        assertEquals(AppTheme.ESCURO, tema)
        assertEquals("Escuro", tema.rotulo)
    }

    @Test
    fun `salvar tema CLARO atualiza o fluxo corretamente`() = runTest {
        themePreferences.salvarTema(AppTheme.CLARO)
        val tema = themePreferences.temaFlow.first()
        assertEquals(AppTheme.CLARO, tema)
        assertEquals("Claro", tema.rotulo)
    }

    @Test
    fun `salvar tema SEGUIR_SISTEMA atualiza o fluxo para Automatico`() = runTest {
        themePreferences.salvarTema(AppTheme.SEGUIR_SISTEMA)
        val tema = themePreferences.temaFlow.first()
        assertEquals(AppTheme.SEGUIR_SISTEMA, tema)
        assertEquals("Automático", tema.rotulo)
    }

    @Test
    fun `salvar tema ESCURO restaura para Escuro`() = runTest {
        themePreferences.salvarTema(AppTheme.CLARO)
        assertEquals(AppTheme.CLARO, themePreferences.temaFlow.first())

        themePreferences.salvarTema(AppTheme.ESCURO)
        assertEquals(AppTheme.ESCURO, themePreferences.temaFlow.first())
    }
}
