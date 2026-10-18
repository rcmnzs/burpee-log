package com.example.burpeelog.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalePreferencesTest {

    private lateinit var context: Context
    private lateinit var manager: LocalePreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        manager = LocalePreferencesManager(context)
    }

    @Test
    fun `idioma padrao e AUTO ao inicializar sem preferencias salvas`() = runTest {
        val idioma = manager.idiomaFlow.first()
        assertEquals(AppLanguage.AUTO, idioma)
    }

    @Test
    fun `salvar ENGLISH atualiza fluxo e sharedPreferences`() = runTest {
        manager.salvarIdioma(AppLanguage.ENGLISH)
        val idioma = manager.idiomaFlow.first()
        assertEquals(AppLanguage.ENGLISH, idioma)
        assertEquals("en", LocalePreferencesManager.getSavedLocaleCodeSync(context))
    }

    @Test
    fun `salvar PORTUGUESE atualiza fluxo e sharedPreferences`() = runTest {
        manager.salvarIdioma(AppLanguage.PORTUGUESE)
        val idioma = manager.idiomaFlow.first()
        assertEquals(AppLanguage.PORTUGUESE, idioma)
        assertEquals("pt-BR", LocalePreferencesManager.getSavedLocaleCodeSync(context))
    }

    @Test
    fun `salvar AUTO limpa a preferencia salva`() = runTest {
        manager.salvarIdioma(AppLanguage.PORTUGUESE)
        assertEquals(AppLanguage.PORTUGUESE, manager.idiomaFlow.first())

        manager.salvarIdioma(AppLanguage.AUTO)
        val idioma = manager.idiomaFlow.first()
        assertEquals(AppLanguage.AUTO, idioma)
        assertEquals("", LocalePreferencesManager.getSavedLocaleCodeSync(context))
    }

    @Test
    fun `fallback para ingles quando idioma nao e suportado`() {
        // Idiomas não suportados devem fazer fallback para ENGLISH
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("de"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("it"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("ja"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("zh"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("unknown"))

        // Suportados
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en-US"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt-BR"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromCode("pt"))

        // Vazio ou nulo retorna AUTO (segue o sistema)
        assertEquals(AppLanguage.AUTO, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.AUTO, AppLanguage.fromCode(""))
        assertEquals(AppLanguage.AUTO, AppLanguage.fromCode("   "))
    }

    @Test
    fun `nenhuma string ou plural esta ausente entre os arquivos de recursos`() {
        val rootResDir = File("src/main/res").takeIf { it.exists() }
            ?: File("app/src/main/res")

        val enFile = File(rootResDir, "values/strings.xml")
        val ptBrFile = File(rootResDir, "values-pt-rBR/strings.xml")
        val ptFile = File(rootResDir, "values-pt/strings.xml")

        assertTrue("values/strings.xml deve existir", enFile.exists())
        assertTrue("values-pt-rBR/strings.xml deve existir", ptBrFile.exists())
        assertTrue("values-pt/strings.xml deve existir", ptFile.exists())

        val enKeys = extractResourceKeys(enFile)
        val ptBrKeys = extractResourceKeys(ptBrFile)
        val ptKeys = extractResourceKeys(ptFile)

        // Verifica chaves presentes em EN mas ausentes em PT-BR
        val missingInPtBr = enKeys - ptBrKeys
        assertTrue(
            "As seguintes chaves estao presentes em values/strings.xml mas ausentes em values-pt-rBR/strings.xml: $missingInPtBr",
            missingInPtBr.isEmpty()
        )

        // Verifica chaves presentes em PT-BR mas ausentes em EN
        val missingInEn = ptBrKeys - enKeys
        assertTrue(
            "As seguintes chaves estao presentes em values-pt-rBR/strings.xml mas ausentes em values/strings.xml: $missingInEn",
            missingInEn.isEmpty()
        )

        // Verifica paridade com values-pt também
        val missingInPt = enKeys - ptKeys
        assertTrue(
            "As seguintes chaves estao presentes em values/strings.xml mas ausentes em values-pt/strings.xml: $missingInPt",
            missingInPt.isEmpty()
        )
    }

    private fun extractResourceKeys(xmlFile: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        doc.documentElement.normalize()

        val keys = mutableSetOf<String>()

        val stringNodes = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            if (node is Element) {
                val name = node.getAttribute("name")
                if (name.isNotEmpty()) {
                    keys.add(name)
                }
            }
        }

        val pluralNodes = doc.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val node = pluralNodes.item(i)
            if (node is Element) {
                val name = node.getAttribute("name")
                if (name.isNotEmpty()) {
                    keys.add("plurals:$name")
                }
            }
        }

        return keys
    }
}
