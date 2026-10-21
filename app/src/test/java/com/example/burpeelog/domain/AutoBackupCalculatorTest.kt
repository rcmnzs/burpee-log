package com.example.burpeelog.domain

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoBackupCalculatorTest {

    private lateinit var context: Context
    private lateinit var app: BurpeeLogApplication

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        app = context as BurpeeLogApplication
    }

    // ---------------------------------------------------------
    // 1. Testes de Backup Vencido (Semanal e Mensal, com e sem mudanças)
    // ---------------------------------------------------------

    @Test
    fun `isBackupOverdue - semanal sem mudancas nunca vence mesmo apos prazo`() {
        val lastBackup = LocalDate.of(2026, 10, 1)
        val today = LocalDate.of(2026, 10, 20) // 19 dias depois

        val overdue = AutoBackupCalculator.isBackupOverdue(
            lastBackupDate = lastBackup,
            currentDate = today,
            frequency = BackupFrequency.WEEKLY,
            hasChanges = false
        )

        assertFalse("Sem mudanças nas sessões, o backup nunca deve estar vencido", overdue)
    }

    @Test
    fun `isBackupOverdue - semanal com mudancas vence apos 7 dias`() {
        val lastBackup = LocalDate.of(2026, 10, 1)

        // 6 dias depois -> não venceu
        val day6 = LocalDate.of(2026, 10, 7)
        assertFalse(
            AutoBackupCalculator.isBackupOverdue(lastBackup, day6, BackupFrequency.WEEKLY, hasChanges = true)
        )

        // Exatamente 7 dias depois -> venceu
        val day7 = LocalDate.of(2026, 10, 8)
        assertTrue(
            AutoBackupCalculator.isBackupOverdue(lastBackup, day7, BackupFrequency.WEEKLY, hasChanges = true)
        )

        // 14 dias depois -> venceu
        val day14 = LocalDate.of(2026, 10, 15)
        assertTrue(
            AutoBackupCalculator.isBackupOverdue(lastBackup, day14, BackupFrequency.WEEKLY, hasChanges = true)
        )
    }

    @Test
    fun `isBackupOverdue - mensal sem mudancas nunca vence`() {
        val lastBackup = LocalDate.of(2026, 8, 1)
        val today = LocalDate.of(2026, 10, 21) // Quase 3 meses depois

        val overdue = AutoBackupCalculator.isBackupOverdue(
            lastBackupDate = lastBackup,
            currentDate = today,
            frequency = BackupFrequency.MONTHLY,
            hasChanges = false
        )

        assertFalse(overdue)
    }

    @Test
    fun `isBackupOverdue - mensal com mudancas vence apos 1 mes`() {
        val lastBackup = LocalDate.of(2026, 9, 15)

        // 20 dias depois -> não venceu
        val day20 = LocalDate.of(2026, 10, 5)
        assertFalse(
            AutoBackupCalculator.isBackupOverdue(lastBackup, day20, BackupFrequency.MONTHLY, hasChanges = true)
        )

        // 1 mês depois (15 de outubro) -> venceu
        val nextMonth = LocalDate.of(2026, 10, 15)
        assertTrue(
            AutoBackupCalculator.isBackupOverdue(lastBackup, nextMonth, BackupFrequency.MONTHLY, hasChanges = true)
        )

        // 1 mês e 5 dias depois -> venceu
        val nextMonthPlus5 = LocalDate.of(2026, 10, 20)
        assertTrue(
            AutoBackupCalculator.isBackupOverdue(lastBackup, nextMonthPlus5, BackupFrequency.MONTHLY, hasChanges = true)
        )
    }

    @Test
    fun `isBackupOverdue - sem backup anterior vence se houver mudancas ou sessoes`() {
        val today = LocalDate.of(2026, 10, 21)

        // Sem backup anterior e com sessões/mudanças -> venceu
        assertTrue(
            AutoBackupCalculator.isBackupOverdue(null, today, BackupFrequency.MONTHLY, hasChanges = true)
        )

        // Sem backup anterior e sem sessões/mudanças -> não venceu
        assertFalse(
            AutoBackupCalculator.isBackupOverdue(null, today, BackupFrequency.MONTHLY, hasChanges = false)
        )
    }

    // ---------------------------------------------------------
    // 2. Testes de Rotação (Mantém 5 e só apaga arquivos com padrão do nome)
    // ---------------------------------------------------------

    @Test
    fun `isBackupFileName - valida estritamente o padrao burpeelog-backup-AAAA-MM-DD json`() {
        assertTrue(AutoBackupCalculator.isBackupFileName("burpeelog-backup-2026-10-21.json"))
        assertTrue(AutoBackupCalculator.isBackupFileName("burpeelog-backup-2025-01-01.json"))

        // Inválidos
        assertFalse(AutoBackupCalculator.isBackupFileName("burpeelog-backup-2026-13-45.json")) // Mês inválido
        assertFalse(AutoBackupCalculator.isBackupFileName("burpeelog-backup.json"))
        assertFalse(AutoBackupCalculator.isBackupFileName("burpeelog-backup-2026-10-21.bak"))
        assertFalse(AutoBackupCalculator.isBackupFileName("burpeelog-backup-2026-10-21 (1).json"))
        assertFalse(AutoBackupCalculator.isBackupFileName("outro-backup-2026-10-21.json"))
        assertFalse(AutoBackupCalculator.isBackupFileName("notas.txt"))
        assertFalse(AutoBackupCalculator.isBackupFileName(""))
        assertFalse(AutoBackupCalculator.isBackupFileName(null))
    }

    @Test
    fun `rotacao - mantem 5 mais recentes e apaga SOMENTE arquivos com o padrao`() {
        val files = listOf(
            BackupFileInfo("burpeelog-backup-2026-10-21.json", lastModified = 600L),
            BackupFileInfo("burpeelog-backup-2026-10-20.json", lastModified = 500L),
            BackupFileInfo("burpeelog-backup-2026-10-19.json", lastModified = 400L),
            BackupFileInfo("burpeelog-backup-2026-10-18.json", lastModified = 300L),
            BackupFileInfo("burpeelog-backup-2026-10-17.json", lastModified = 200L),
            BackupFileInfo("burpeelog-backup-2026-10-16.json", lastModified = 100L), // 6º mais recente -> deve apagar
            BackupFileInfo("burpeelog-backup-2026-10-15.json", lastModified = 50L),  // 7º mais recente -> deve apagar
            // Arquivos alheios que NUNCA devem ser apagados
            BackupFileInfo("planilha_treino.xlsx", lastModified = 900L),
            BackupFileInfo("anotacoes_importantes.txt", lastModified = 800L),
            BackupFileInfo("fotos_treino.zip", lastModified = 700L),
            BackupFileInfo("backup_outro_app.json", lastModified = 650L)
        )

        val toDelete = AutoBackupCalculator.findFilesToDeleteOnRotation(files, maxToKeep = 5)

        assertEquals("Devem ser excluídos exatamente 2 backups excedentes", 2, toDelete.size)
        val namesToDelete = toDelete.map { it.name }

        assertTrue(namesToDelete.contains("burpeelog-backup-2026-10-16.json"))
        assertTrue(namesToDelete.contains("burpeelog-backup-2026-10-15.json"))

        // Garante que nenhum arquivo fora do padrão foi selecionado para exclusão
        assertFalse(namesToDelete.contains("planilha_treino.xlsx"))
        assertFalse(namesToDelete.contains("anotacoes_importantes.txt"))
        assertFalse(namesToDelete.contains("fotos_treino.zip"))
        assertFalse(namesToDelete.contains("backup_outro_app.json"))

        // Garante que os 5 mais recentes foram preservados
        assertFalse(namesToDelete.contains("burpeelog-backup-2026-10-21.json"))
        assertFalse(namesToDelete.contains("burpeelog-backup-2026-10-20.json"))
        assertFalse(namesToDelete.contains("burpeelog-backup-2026-10-19.json"))
        assertFalse(namesToDelete.contains("burpeelog-backup-2026-10-18.json"))
        assertFalse(namesToDelete.contains("burpeelog-backup-2026-10-17.json"))
    }

    @Test
    fun `rotacao - com 5 ou menos backups nao apaga nada`() {
        val files = listOf(
            BackupFileInfo("burpeelog-backup-2026-10-21.json"),
            BackupFileInfo("burpeelog-backup-2026-10-20.json"),
            BackupFileInfo("burpeelog-backup-2026-10-19.json"),
            BackupFileInfo("outras_notas.txt")
        )

        val toDelete = AutoBackupCalculator.findFilesToDeleteOnRotation(files, maxToKeep = 5)
        assertTrue(toDelete.isEmpty())
    }

    // ---------------------------------------------------------
    // 3. Testes de Limite de Notificações (Máximo 1 por dia)
    // ---------------------------------------------------------

    @Test
    fun `canSendReminderToday - nunca envia se nao estiver vencido`() {
        val today = LocalDate.of(2026, 10, 21)
        assertFalse(
            AutoBackupCalculator.canSendReminderToday(
                lastReminderDate = null,
                currentDate = today,
                isOverdue = false
            )
        )
    }

    @Test
    fun `canSendReminderToday - envia se vencido e sem lembrete anterior`() {
        val today = LocalDate.of(2026, 10, 21)
        assertTrue(
            AutoBackupCalculator.canSendReminderToday(
                lastReminderDate = null,
                currentDate = today,
                isOverdue = true
            )
        )
    }

    @Test
    fun `canSendReminderToday - bloqueia segundo lembrete no mesmo dia`() {
        val today = LocalDate.of(2026, 10, 21)
        val alreadySentToday = LocalDate.of(2026, 10, 21)

        val canSend = AutoBackupCalculator.canSendReminderToday(
            lastReminderDate = alreadySentToday,
            currentDate = today,
            isOverdue = true
        )

        assertFalse("Não deve enviar mais de 1 notificação no mesmo dia", canSend)
    }

    @Test
    fun `canSendReminderToday - permite lembrete no dia seguinte`() {
        val yesterday = LocalDate.of(2026, 10, 20)
        val today = LocalDate.of(2026, 10, 21)

        val canSend = AutoBackupCalculator.canSendReminderToday(
            lastReminderDate = yesterday,
            currentDate = today,
            isOverdue = true
        )

        assertTrue("Deve permitir nova notificação após mudar o dia", canSend)
    }

    // ---------------------------------------------------------
    // 4. Testes de Tratamento de Pasta Inacessível e Erros
    // ---------------------------------------------------------

    @Test
    fun `executarBackupNaPasta - trata pasta inexistente ou inacessivel sem crash e desativa auto backup`() {
        runBlocking {
            val invalidUri = Uri.parse("content://invalid.storage.provider/tree/non_existent_folder")
        val sessao = Sessao(
            dataHora = System.currentTimeMillis(),
            tipo = TipoExercicio.SIX_COUNT,
            repsFeitas = 50,
            duracaoSegundos = 1200
        )

        val result = AutoBackupManager.executarBackupNaPasta(
            context = context,
            folderUri = invalidUri,
            sessoes = listOf(sessao)
        )

        assertTrue("Resultado deve ser de erro", result is BackupExecutionResult.Error)

            val state = app.autoBackupPreferences.stateFlow.first()
            assertFalse("Auto backup deve ser desativado após erro", state.autoBackupEnabled)
            assertNotNull("Mensagem de erro deve ser gravada", state.lastBackupError)
        }
    }

    // ---------------------------------------------------------
    // 5. Testes de Assinatura do Banco de Dados (Detecção de Mudanças)
    // ---------------------------------------------------------

    @Test
    fun `computeDatabaseSignature - detecta alteracoes nas sessoes`() {
        val s1 = Sessao(id = 1L, dataHora = 1000L, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 40, duracaoSegundos = 1200)
        val s2 = Sessao(id = 2L, dataHora = 2000L, tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30, duracaoSegundos = 1200)

        val sigOriginal = AutoBackupCalculator.computeDatabaseSignature(listOf(s1, s2))
        val sigRepetida = AutoBackupCalculator.computeDatabaseSignature(listOf(s1, s2))
        assertEquals(sigOriginal, sigRepetida)

        // Adição de nova sessão
        val s3 = Sessao(id = 3L, dataHora = 3000L, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 45, duracaoSegundos = 1200)
        val sigAdicao = AutoBackupCalculator.computeDatabaseSignature(listOf(s1, s2, s3))
        assertTrue(sigOriginal != sigAdicao)

        // Modificação de reps
        val s1Modificada = s1.copy(repsFeitas = 42)
        val sigModificada = AutoBackupCalculator.computeDatabaseSignature(listOf(s1Modificada, s2))
        assertTrue(sigOriginal != sigModificada)

        // Remoção
        val sigRemocao = AutoBackupCalculator.computeDatabaseSignature(listOf(s1))
        assertTrue(sigOriginal != sigRemocao)
    }
}
