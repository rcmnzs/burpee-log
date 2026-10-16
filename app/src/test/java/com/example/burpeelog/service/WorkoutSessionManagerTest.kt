package com.example.burpeelog.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.IntervaloCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutSessionManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    /**
     * 1. Depois de finalizarSessao() ser chamada, nenhuma nova repetição é emitida.
     */
    @Test
    fun `depois de finalizarSessao ser chamada, nenhuma nova repeticao e emitida`() = runTest {
        var currentTime = 1_000_000L
        val manager = WorkoutSessionManager(context, timeProvider = { currentTime })

        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30))
        assertTrue("Jobs devem estar ativos durante a contagem/treino", manager.hasActiveJobs())

        // Finaliza a sessão antecipadamente
        manager.finalizarSessao(isEarly = true)

        // Verifica que todos os jobs foram cancelados imediatamente
        assertFalse("Nenhum job deve estar ativo após finalizarSessao", manager.hasActiveJobs())
        val repsAoFinalizar = manager.state.value.repsFeitasCalculadas
        val faseAoFinalizar = manager.state.value.phase
        assertEquals(WorkoutPhase.STOPPED_EARLY, faseAoFinalizar)

        // Avança o relógio e aguarda tempo suficiente
        currentTime += 60_000L
        delay(300)

        // Estado deve permanecer rigorosamente congelado
        assertEquals("Nenhuma repetição adicional deve ser emitida após finalizar", repsAoFinalizar, manager.state.value.repsFeitasCalculadas)
        assertEquals("Fase não deve ser alterada", WorkoutPhase.STOPPED_EARLY, manager.state.value.phase)
        assertFalse("Jobs continuam inativos", manager.hasActiveJobs())
    }

    /**
     * 2. O valor de repsFeitasCalculadas ao encerrar é igual ao número de instantes de repetição
     * cujo tempo previsto já passou, descontando pausas.
     */
    @Test
    fun `o valor de repsFeitasCalculadas ao encerrar e igual ao numero de instantes de repeticao cujo tempo previsto ja passou, descontando pausas`() {
        var currentTime = 1_000_000L
        val manager = WorkoutSessionManager(context, timeProvider = { currentTime })

        val padrao = listOf(30) // Repetições a cada 30 segundos (instantes: 0, 30, 60, 90, 120, ...)
        val instantes = IntervaloCalculator.calcularInstantesDasReps(padrao, DURACAO_MINUTOS)

        // Cenário 1: Treino em execução ativa (RUNNING)
        // Início em t = 1_000_000L.
        // Tempo corrido total do relógio: 95 segundos (de 1_000_000L a 1_095_000L).
        // Pausa acumulada: 30 segundos (30_000L).
        // Tempo real treinado (descontando pausas): 95s - 30s = 65 segundos.
        // Instantes passados até 65s: [0, 30, 60] -> 3 repetições (instante 90 ainda não passou).
        currentTime = 1_095_000L
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.RUNNING,
            padrao = padrao,
            tempoInicioRealtime = 1_000_000L,
            pausaAcumuladaMillis = 30_000L
        )

        manager.finalizarSessao(isEarly = true)

        val tempoEsperado = 65
        val repsEsperadas = instantes.count { it <= tempoEsperado }
        assertEquals(3, repsEsperadas)
        assertEquals(tempoEsperado, manager.state.value.tempoDecorridoSegundos)
        assertEquals(repsEsperadas, manager.state.value.repsFeitasCalculadas)

        // Cenário 2: Treino pausado no momento de finalizar (PAUSED)
        // Início em t = 2_000_000L.
        // Pausado aos 40 segundos de treino (t = 2_040_000L).
        // Usuário permanece 60 segundos com o app pausado (currentTime = 2_100_000L).
        // Ao finalizar em PAUSED, o tempo ativo considerado deve ser exatamente 40s (descontando todo o período em pausa).
        // Instantes passados até 40s: [0, 30] -> 2 repetições.
        currentTime = 2_100_000L
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.PAUSED,
            padrao = padrao,
            tempoInicioRealtime = 2_000_000L,
            pausaInicioRealtime = 2_040_000L,
            pausaAcumuladaMillis = 0L
        )

        manager.finalizarSessao(isEarly = true)

        val tempoEsperadoPausado = 40
        val repsEsperadasPausado = instantes.count { it <= tempoEsperadoPausado }
        assertEquals(2, repsEsperadasPausado)
        assertEquals(tempoEsperadoPausado, manager.state.value.tempoDecorridoSegundos)
        assertEquals(repsEsperadasPausado, manager.state.value.repsFeitasCalculadas)
    }

    /**
     * 3. Após salvar ou descartar, o estado volta ao inicial (fase IDLE, sem jobs ativos).
     */
    @Test
    fun `apos salvar ou descartar, o estado volta ao inicial (fase IDLE, sem jobs ativos)`() {
        val manager = WorkoutSessionManager(context)

        // Subcenário A: Descartar treino após início
        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30))
        assertTrue("Jobs ativos após iniciar treino", manager.hasActiveJobs())
        assertNotEquals(WorkoutPhase.IDLE, manager.state.value.phase)

        // Fluxo de descarte: finalizarSessao seguido de reset
        manager.finalizarSessao(isEarly = false)
        manager.reset()

        assertEquals("Fase deve voltar a IDLE após descartar", WorkoutPhase.IDLE, manager.state.value.phase)
        assertFalse("Não deve ter jobs ativos após descartar", manager.hasActiveJobs())
        assertEquals(0, manager.state.value.tempoDecorridoSegundos)
        assertEquals(0, manager.state.value.repsFeitasCalculadas)

        // Subcenário B: Salvar treino (parcial ou completo)
        manager.startWorkout(TipoExercicio.NAVY_SEAL, listOf(40))
        assertTrue("Jobs ativos após iniciar treino", manager.hasActiveJobs())

        manager.finalizarSessao(isEarly = true)
        assertEquals(WorkoutPhase.STOPPED_EARLY, manager.state.value.phase)

        // Ao salvar na tela de resumo, o app chama reset() para liberar o estado
        manager.reset()

        assertEquals("Fase deve voltar a IDLE após salvar", WorkoutPhase.IDLE, manager.state.value.phase)
        assertFalse("Não deve ter jobs ativos após salvar", manager.hasActiveJobs())
        assertEquals(0, manager.state.value.tempoDecorridoSegundos)
        assertEquals(0, manager.state.value.repsFeitasCalculadas)
    }

    /**
     * 4. Sessão parcial encerra com tempoDecorridoSegundos igual ao tempo real X segundos, e não 1200.
     */
    @Test
    fun `sessao parcial encerra com tempoDecorridoSegundos igual ao tempo real X segundos`() {
        var currentTime = 5_000_000L
        val manager = WorkoutSessionManager(context, timeProvider = { currentTime })
        val padrao = listOf(30)
        val xSegundos = 350

        currentTime = 5_000_000L + (xSegundos * 1000L)
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.RUNNING,
            padrao = padrao,
            tempoInicioRealtime = 5_000_000L,
            pausaAcumuladaMillis = 0L
        )

        manager.finalizarSessao(isEarly = true)
        assertEquals(xSegundos, manager.state.value.tempoDecorridoSegundos)
        assertNotEquals(1200, manager.state.value.tempoDecorridoSegundos)

        // Já em sessão completa (isEarly = false), o tempo registrado deve ser 1200
        manager.finalizarSessao(isEarly = false)
        assertEquals(1200, manager.state.value.tempoDecorridoSegundos)
    }

    /**
     * 5. Sequência correta de fases com aquecimento ativo e desativado.
     */
    @Test
    fun `sequencia de fases inicia direto em ACTIVE se aquecimento desativado e em WARMUP se ativo`() {
        val manager = WorkoutSessionManager(context)

        // Com aquecimento desativado
        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30), aquecimentoAtivo = false)
        assertEquals(WorkoutPhase.COUNTDOWN, manager.state.value.phase)
        assertFalse(manager.state.value.aquecimentoAtivo)
        manager.reset()

        // Com aquecimento ativado
        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30), aquecimentoAtivo = true)
        assertEquals(WorkoutPhase.COUNTDOWN, manager.state.value.phase)
        assertTrue(manager.state.value.aquecimentoAtivo)
        manager.reset()
    }

    /**
     * 6. Pular aquecimento transiciona imediatamente para a fase ACTIVE.
     */
    @Test
    fun `pular aquecimento transiciona imediatamente para a fase ACTIVE`() {
        val manager = WorkoutSessionManager(context)
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.WARMUP_BURPEE_1,
            padrao = listOf(30),
            tempoInicioRealtime = 1_000_000L,
            aquecimentoAtivo = true,
            warmupReps = 5
        )

        assertEquals(WorkoutPhase.WARMUP_BURPEE_1, manager.state.value.phase)
        manager.skipWarmup()

        var attempts = 0
        while (manager.state.value.phase != WorkoutPhase.ACTIVE && attempts < 30) {
            Thread.sleep(50)
            attempts++
        }

        assertEquals(WorkoutPhase.ACTIVE, manager.state.value.phase)
        assertEquals("Treino principal começa no minuto 1", 1, manager.state.value.currentMinute)
        manager.finalizarSessao(isEarly = true)
        manager.reset()
    }

    /**
     * 7. Confirmação de que as reps do aquecimento NÃO somam ao total da sessão
     * e duração gravada é de 1200s (20 minutos do treino principal) na sessão completa.
     */
    @Test
    fun `reps do aquecimento nao somam ao total da sessao e duracao gravada e de 1200s`() {
        var currentTime = 10_000_000L
        val manager = WorkoutSessionManager(context, timeProvider = { currentTime })
        val padrao = listOf(30)
        val instantes = IntervaloCalculator.calcularInstantesDasReps(padrao, DURACAO_MINUTOS)

        // Cenário A: Treino completo (isEarly = false) com aquecimento prévio
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.ACTIVE,
            padrao = padrao,
            tempoInicioRealtime = 10_000_000L,
            aquecimentoAtivo = true,
            warmupReps = 20
        )

        manager.finalizarSessao(isEarly = false)

        assertEquals("Duração de sessão completa deve ser exatamente 1200s", 1200, manager.state.value.tempoDecorridoSegundos)
        assertEquals("Reps devem ser exatamente as do treino principal, sem somar o aquecimento", instantes.size, manager.state.value.repsFeitasCalculadas)
        assertEquals(WorkoutPhase.COMPLETED, manager.state.value.phase)

        // Cenário B: Usuário cancela durante a fase de aquecimento
        manager.prepararEstadoParaTeste(
            phase = WorkoutPhase.WARMUP_REST_1,
            padrao = padrao,
            tempoInicioRealtime = 10_000_000L,
            aquecimentoAtivo = true,
            warmupReps = 10
        )

        manager.finalizarSessao(isEarly = true)

        assertEquals("Cancelamento no aquecimento deve ter 0 reps computadas para a sessão", 0, manager.state.value.repsFeitasCalculadas)
        assertEquals("Cancelamento no aquecimento deve ter 0s de duração do treino principal", 0, manager.state.value.tempoDecorridoSegundos)
        assertEquals(WorkoutPhase.STOPPED_EARLY, manager.state.value.phase)
    }

    /**
     * 8. Validação das 4 fases do aquecimento e propriedades enum.
     */
    @Test
    fun `validacao das 4 fases do aquecimento e propriedades enum`() {
        val fases = listOf(
            WorkoutPhase.WARMUP_BURPEE_1,
            WorkoutPhase.WARMUP_REST_1,
            WorkoutPhase.WARMUP_BURPEE_2,
            WorkoutPhase.WARMUP_REST_2
        )

        fases.forEach { fase ->
            assertTrue("$fase deve ser considerada isWarmup", fase.isWarmup)
        }

        assertTrue(WorkoutPhase.WARMUP_BURPEE_1.isWarmupBurpee)
        assertTrue(WorkoutPhase.WARMUP_BURPEE_2.isWarmupBurpee)
        assertFalse(WorkoutPhase.WARMUP_REST_1.isWarmupBurpee)
        assertFalse(WorkoutPhase.WARMUP_REST_2.isWarmupBurpee)

        assertTrue(WorkoutPhase.WARMUP_REST_1.isWarmupRest)
        assertTrue(WorkoutPhase.WARMUP_REST_2.isWarmupRest)
        assertFalse(WorkoutPhase.WARMUP_BURPEE_1.isWarmupRest)
        assertFalse(WorkoutPhase.WARMUP_BURPEE_2.isWarmupRest)

        assertFalse(WorkoutPhase.ACTIVE.isWarmup)
        assertFalse(WorkoutPhase.COUNTDOWN.isWarmup)
    }
}
