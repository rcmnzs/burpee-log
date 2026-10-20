package com.example.burpeelog.service

import android.app.Service
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.example.R
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.domain.IntervaloCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    IDLE,
    COUNTDOWN,
    WARMUP_BURPEE_1, // 1 min (60s)
    WARMUP_REST_1,   // 2 min (120s)
    WARMUP_BURPEE_2, // 1 min (60s)
    WARMUP_REST_2,   // 3 min (180s)
    ACTIVE,          // 20 min (1200s)
    PAUSED,
    FINISHED,
    COMPLETED,
    STOPPED_EARLY;

    val isWarmup: Boolean
        get() = this == WARMUP_BURPEE_1 || this == WARMUP_REST_1 || this == WARMUP_BURPEE_2 || this == WARMUP_REST_2

    val isWarmupBurpee: Boolean
        get() = this == WARMUP_BURPEE_1 || this == WARMUP_BURPEE_2

    val isWarmupRest: Boolean
        get() = this == WARMUP_REST_1 || this == WARMUP_REST_2

    companion object {
        // Alias para compatibilidade com chamadas legado a WorkoutPhase.RUNNING
        val RUNNING: WorkoutPhase get() = ACTIVE
    }
}

data class WorkoutActiveState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val previousPhaseBeforePause: WorkoutPhase = WorkoutPhase.ACTIVE,
    val tipo: TipoExercicio = TipoExercicio.SIX_COUNT,
    val padrao: List<Int> = listOf(30),
    val aquecimentoAtivo: Boolean = false,
    val totalRepsPrevistas: Int = 40,
    val countdownSeconds: Int = 5,
    val elapsedSeconds: Int = 0,
    val tempoDecorridoSegundos: Int = 0,
    val remainingSeconds: Int = DURACAO_MINUTOS * 60,
    val tempoFormatado: String = "20:00",
    val currentRepIndex: Int = 0,
    val currentRepDisplay: Int = 0,
    val secondsUntilNextRep: Int = 30,
    val nextRepProgress: Float = 0f,
    val currentMinute: Int = 1,
    val progressOverall: Float = 0f,
    val repInstants: List<Int> = emptyList(),
    val repsCompletadas: Int = 0,
    val repsFeitasCalculadas: Int = 0,
    // Estado específico do aquecimento (isolado do treino principal)
    val warmupBlockSecondsElapsed: Int = 0,
    val warmupBlockSecondsTotal: Int = 0,
    val warmupRepsCompletadas: Int = 0,
    val warmupTempoRestanteFormatado: String = ""
)

class WorkoutSessionManager(
    private val context: Context,
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    internal var tickerJob: Job? = null
    internal var countdownJob: Job? = null
    internal var beepJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val feedbackHelper = WorkoutFeedbackHelper(context)

    private val _state = MutableStateFlow(WorkoutActiveState())
    val state: StateFlow<WorkoutActiveState> = _state.asStateFlow()

    // Variáveis de tempo monotônico imunes a deriva e mudanças de relógio
    internal var sessionStartRealtime: Long = 0L
    internal var pauseStartRealtime: Long = 0L
    internal var totalPausedDurationMillis: Long = 0L

    // Variáveis de bloco de aquecimento
    internal var warmupBlockStartRealtime: Long = 0L

    private var lastFiredRepIndex: Int = -1
    private var lastFiredMinute: Int = 0
    private var lastFiredWarmupRepSec: Int = -1

    fun hasActiveJobs(): Boolean {
        return tickerJob?.isActive == true || countdownJob?.isActive == true || beepJob?.isActive == true
    }

    internal fun prepararEstadoParaTeste(
        phase: WorkoutPhase,
        padrao: List<Int>,
        tempoInicioRealtime: Long,
        pausaAcumuladaMillis: Long = 0L,
        pausaInicioRealtime: Long = 0L,
        aquecimentoAtivo: Boolean = false,
        warmupReps: Int = 0
    ) {
        val repsPrevistas = IntervaloCalculator.calcularRepsPrevistas(padrao, DURACAO_MINUTOS)
        val instantes = IntervaloCalculator.calcularInstantesDasReps(padrao, DURACAO_MINUTOS)
        sessionStartRealtime = tempoInicioRealtime
        totalPausedDurationMillis = pausaAcumuladaMillis
        pauseStartRealtime = pausaInicioRealtime
        _state.value = WorkoutActiveState(
            phase = phase,
            padrao = padrao,
            aquecimentoAtivo = aquecimentoAtivo,
            totalRepsPrevistas = repsPrevistas,
            repInstants = instantes,
            warmupRepsCompletadas = warmupReps
        )
    }

    fun setWakeLock(lock: PowerManager.WakeLock?) {
        this.wakeLock = lock
    }

    fun startWorkout(tipo: TipoExercicio, padrao: List<Int>, aquecimentoAtivo: Boolean = false) {
        tickerJob?.cancel()
        tickerJob = null
        countdownJob?.cancel()
        countdownJob = null
        beepJob?.cancel()
        beepJob = null

        val repsPrevistas = IntervaloCalculator.calcularRepsPrevistas(padrao, DURACAO_MINUTOS)
        val instantes = IntervaloCalculator.calcularInstantesDasReps(padrao, DURACAO_MINUTOS)

        lastFiredRepIndex = -1
        lastFiredMinute = 0
        lastFiredWarmupRepSec = -1
        totalPausedDurationMillis = 0L

        _state.value = WorkoutActiveState(
            phase = WorkoutPhase.COUNTDOWN,
            tipo = tipo,
            padrao = padrao,
            aquecimentoAtivo = aquecimentoAtivo,
            totalRepsPrevistas = repsPrevistas,
            countdownSeconds = 5,
            elapsedSeconds = 0,
            tempoDecorridoSegundos = 0,
            remainingSeconds = DURACAO_MINUTOS * 60,
            tempoFormatado = "20:00",
            currentRepIndex = 0,
            currentRepDisplay = 0,
            secondsUntilNextRep = if (instantes.isNotEmpty()) instantes[0] else 0,
            nextRepProgress = 0f,
            currentMinute = 1,
            progressOverall = 0f,
            repInstants = instantes,
            repsCompletadas = 0,
            repsFeitasCalculadas = 0,
            warmupBlockSecondsElapsed = 0,
            warmupBlockSecondsTotal = 0,
            warmupRepsCompletadas = 0,
            warmupTempoRestanteFormatado = ""
        )

        // Inicia o Foreground Service no Android
        WorkoutTimerService.startService(context)

        tickerJob = scope.launch {
            // Contagem regressiva inicial de 5 segundos
            for (c in 5 downTo 1) {
                if (!isActive) return@launch
                _state.update { it.copy(countdownSeconds = c) }
                feedbackHelper.playCountdownBeep(isFinal = (c == 1))
                delay(1000L)
            }

            if (!isActive) return@launch

            if (aquecimentoAtivo) {
                executarSequenciaAquecimento()
            } else {
                iniciarTreinoPrincipalAtivo()
            }
        }
    }

    /**
     * Executa os 4 blocos de aquecimento em sequência:
     * Bloco 1: 1 min burpees (60s)
     * Bloco 2: 2 min descanso (120s)
     * Bloco 3: 1 min burpees (60s)
     * Bloco 4: 3 min descanso (180s)
     * Seguido do treino principal (20 min) automaticamente.
     */
    internal suspend fun executarSequenciaAquecimento() {
        // Bloco 1: WARMUP_BURPEE_1 (60s)
        executarBlocoAquecimentoBurpee(WorkoutPhase.WARMUP_BURPEE_1, 60)
        if (!scope.isActive || _state.value.phase == WorkoutPhase.IDLE || _state.value.phase == WorkoutPhase.COMPLETED || _state.value.phase == WorkoutPhase.STOPPED_EARLY) return

        // Bloco 2: WARMUP_REST_1 (120s)
        executarBlocoAquecimentoDescanso(WorkoutPhase.WARMUP_REST_1, 120)
        if (!scope.isActive || _state.value.phase == WorkoutPhase.IDLE || _state.value.phase == WorkoutPhase.COMPLETED || _state.value.phase == WorkoutPhase.STOPPED_EARLY) return

        // Bloco 3: WARMUP_BURPEE_2 (60s)
        executarBlocoAquecimentoBurpee(WorkoutPhase.WARMUP_BURPEE_2, 60)
        if (!scope.isActive || _state.value.phase == WorkoutPhase.IDLE || _state.value.phase == WorkoutPhase.COMPLETED || _state.value.phase == WorkoutPhase.STOPPED_EARLY) return

        // Bloco 4: WARMUP_REST_2 (180s)
        executarBlocoAquecimentoDescanso(WorkoutPhase.WARMUP_REST_2, 180)
        if (!scope.isActive || _state.value.phase == WorkoutPhase.IDLE || _state.value.phase == WorkoutPhase.COMPLETED || _state.value.phase == WorkoutPhase.STOPPED_EARLY) return

        // Aquecimento finalizado -> Inicia o treino principal de 20 minutos automaticamente
        feedbackHelper.playCountdownBeep(isFinal = true)
        iniciarTreinoPrincipalAtivo()
    }

    private suspend fun executarBlocoAquecimentoBurpee(phase: WorkoutPhase, duracaoSegundos: Int) {
        val padrao = _state.value.padrao
        val intervalo = (padrao.firstOrNull() ?: 30).coerceAtLeast(1)

        warmupBlockStartRealtime = timeProvider()
        totalPausedDurationMillis = 0L
        lastFiredWarmupRepSec = 0

        var warmupReps = _state.value.warmupRepsCompletadas + 1 // rep no segundo 0
        _state.update {
            it.copy(
                phase = phase,
                countdownSeconds = 0,
                warmupBlockSecondsElapsed = 0,
                warmupBlockSecondsTotal = duracaoSegundos,
                warmupRepsCompletadas = warmupReps,
                warmupTempoRestanteFormatado = String.format("%02d:%02d", duracaoSegundos / 60, duracaoSegundos % 60),
                secondsUntilNextRep = intervalo,
                nextRepProgress = 0f
            )
        }

        feedbackHelper.playRepCue()

        while (scope.isActive) {
            val currentState = _state.value
            if (currentState.phase == WorkoutPhase.PAUSED) {
                delay(200L)
                continue
            }
            if (currentState.phase != phase) {
                break
            }

            val now = timeProvider()
            val activeMillis = (now - warmupBlockStartRealtime) - totalPausedDurationMillis
            val elapsedSec = (activeMillis / 1000).toInt()

            if (elapsedSec >= duracaoSegundos) {
                break
            }

            // Disparar reps conforme o intervalo
            val repAtualIndex = elapsedSec / intervalo
            val repSec = repAtualIndex * intervalo
            if (repSec > lastFiredWarmupRepSec) {
                lastFiredWarmupRepSec = repSec
                warmupReps++
                feedbackHelper.playRepCue()
            }

            val secUntilNext = (intervalo - (elapsedSec % intervalo)).let { if (it == 0) intervalo else it }
            val nextProgress = ((intervalo - secUntilNext).toFloat() / intervalo.toFloat()).coerceIn(0f, 1f)
            val remaining = (duracaoSegundos - elapsedSec).coerceAtLeast(0)
            val formatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

            _state.update {
                it.copy(
                    warmupBlockSecondsElapsed = elapsedSec,
                    warmupTempoRestanteFormatado = formatted,
                    warmupRepsCompletadas = warmupReps,
                    secondsUntilNextRep = secUntilNext,
                    nextRepProgress = nextProgress
                )
            }

            WorkoutTimerService.updateNotification(
                context = context,
                tempoRestante = formatted,
                repAtual = warmupReps,
                totalReps = (duracaoSegundos / intervalo) + 1,
                tipo = "${context.getString(R.string.warmup_title)} • ${context.getString(currentState.tipo.tituloRes)}",
                isPaused = false
            )

            delay(150L)
        }
    }

    private suspend fun executarBlocoAquecimentoDescanso(phase: WorkoutPhase, duracaoSegundos: Int) {
        warmupBlockStartRealtime = timeProvider()
        totalPausedDurationMillis = 0L

        _state.update {
            it.copy(
                phase = phase,
                warmupBlockSecondsElapsed = 0,
                warmupBlockSecondsTotal = duracaoSegundos,
                warmupTempoRestanteFormatado = String.format("%02d:%02d", duracaoSegundos / 60, duracaoSegundos % 60),
                secondsUntilNextRep = 0,
                nextRepProgress = 0f
            )
        }

        while (scope.isActive) {
            val currentState = _state.value
            if (currentState.phase == WorkoutPhase.PAUSED) {
                delay(200L)
                continue
            }
            if (currentState.phase != phase) {
                break
            }

            val now = timeProvider()
            val activeMillis = (now - warmupBlockStartRealtime) - totalPausedDurationMillis
            val elapsedSec = (activeMillis / 1000).toInt()

            if (elapsedSec >= duracaoSegundos) {
                break
            }

            val remaining = (duracaoSegundos - elapsedSec).coerceAtLeast(0)
            val formatted = String.format("%02d:%02d", remaining / 60, remaining % 60)

            _state.update {
                it.copy(
                    warmupBlockSecondsElapsed = elapsedSec,
                    warmupTempoRestanteFormatado = formatted
                )
            }

            WorkoutTimerService.updateNotification(
                context = context,
                tempoRestante = formatted,
                repAtual = 0,
                totalReps = 0,
                tipo = "Aquecimento • Descanso",
                isPaused = false
            )

            delay(150L)
        }
    }

    /**
     * Pula o aquecimento e inicia o treino principal de 20 minutos imediatamente.
     */
    fun skipWarmup() {
        val currentPhase = _state.value.phase
        val isWarmupActive = currentPhase.isWarmup || (currentPhase == WorkoutPhase.PAUSED && _state.value.previousPhaseBeforePause.isWarmup)
        if (!isWarmupActive) return
        tickerJob?.cancel()
        tickerJob = scope.launch {
            iniciarTreinoPrincipalAtivo()
        }
    }

    internal suspend fun iniciarTreinoPrincipalAtivo() {
        sessionStartRealtime = timeProvider()
        totalPausedDurationMillis = 0L

        lastFiredRepIndex = 0
        lastFiredMinute = 1

        val instantes = _state.value.repInstants

        _state.update {
            it.copy(
                phase = WorkoutPhase.ACTIVE,
                countdownSeconds = 0,
                elapsedSeconds = 0,
                tempoDecorridoSegundos = 0,
                remainingSeconds = DURACAO_MINUTOS * 60,
                tempoFormatado = "20:00",
                currentRepIndex = 0,
                currentRepDisplay = 1,
                repsCompletadas = 1,
                repsFeitasCalculadas = 1,
                secondsUntilNextRep = if (instantes.size > 1) instantes[1] else 0,
                nextRepProgress = 0f,
                currentMinute = 1,
                progressOverall = 0f
            )
        }

        // A primeira repetição ocorre no instante 0s
        feedbackHelper.playRepCue()

        // Loop de execução do treino principal com relógio monotônico
        loopTreinoAtivo()
    }

    private suspend fun loopTreinoAtivo() {
        val totalTreinoSegundos = DURACAO_MINUTOS * 60
        val instantes = _state.value.repInstants

        while (scope.isActive) {
            val currentState = _state.value

            if (currentState.phase == WorkoutPhase.PAUSED) {
                delay(200L)
                continue
            }

            if (currentState.phase != WorkoutPhase.ACTIVE) {
                break
            }

            val now = timeProvider()
            val activeElapsedMillis = (now - sessionStartRealtime) - totalPausedDurationMillis
            val elapsedSeconds = (activeElapsedMillis / 1000).toInt()

            if (elapsedSeconds >= totalTreinoSegundos) {
                // Treino concluído com sucesso (20 minutos completos)
                feedbackHelper.playCountdownBeep(isFinal = true)
                finalizarSessao(isEarly = false)
                break
            }

            // Verificar se alguma repetição deve disparar
            var repDisparou = false
            for (i in (lastFiredRepIndex + 1) until instantes.size) {
                if (elapsedSeconds >= instantes[i]) {
                    lastFiredRepIndex = i
                    feedbackHelper.playRepCue()
                    repDisparou = true
                } else {
                    break
                }
            }

            // Verificar virada de minuto (aviso mais discreto)
            val minutoAtual = (elapsedSeconds / 60) + 1
            if (minutoAtual > lastFiredMinute && elapsedSeconds > 0) {
                lastFiredMinute = minutoAtual
                if (!repDisparou) {
                    feedbackHelper.playMinuteCue()
                }
            }

            // Calcular próxima repetição e progresso até ela
            val nextRepIdx = instantes.indexOfFirst { it > elapsedSeconds }
            val secondsUntilNext: Int
            val nextRepProgress: Float

            if (nextRepIdx != -1) {
                val nextInstant = instantes[nextRepIdx]
                val prevInstant = if (nextRepIdx > 0) instantes[nextRepIdx - 1] else 0
                val span = (nextInstant - prevInstant).coerceAtLeast(1)
                secondsUntilNext = nextInstant - elapsedSeconds
                nextRepProgress = ((span - secondsUntilNext).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            } else {
                secondsUntilNext = 0
                nextRepProgress = 1f
            }

            val remainingSeconds = (totalTreinoSegundos - elapsedSeconds).coerceAtLeast(0)
            val mm = remainingSeconds / 60
            val ss = remainingSeconds % 60
            val tempoFormatado = String.format("%02d:%02d", mm, ss)
            val progressOverall = (elapsedSeconds.toFloat() / totalTreinoSegundos.toFloat()).coerceIn(0f, 1f)

            val repAtualDisplay = if (lastFiredRepIndex >= 0) (lastFiredRepIndex + 1).coerceAtMost(instantes.size) else 0

            _state.update {
                it.copy(
                    elapsedSeconds = elapsedSeconds,
                    tempoDecorridoSegundos = elapsedSeconds,
                    remainingSeconds = remainingSeconds,
                    tempoFormatado = tempoFormatado,
                    currentRepIndex = lastFiredRepIndex,
                    currentRepDisplay = repAtualDisplay,
                    repsCompletadas = repAtualDisplay,
                    repsFeitasCalculadas = repAtualDisplay,
                    secondsUntilNextRep = secondsUntilNext,
                    nextRepProgress = nextRepProgress,
                    currentMinute = minutoAtual,
                    progressOverall = progressOverall
                )
            }

            // Atualiza notificação do foreground service
            WorkoutTimerService.updateNotification(
                context = context,
                tempoRestante = tempoFormatado,
                repAtual = repAtualDisplay,
                totalReps = instantes.size,
                tipo = context.getString(currentState.tipo.tituloRes),
                isPaused = false
            )

            delay(150L)
        }
    }

    fun pauseWorkout() {
        val currentPhase = _state.value.phase
        if (!currentPhase.isWarmup && currentPhase != WorkoutPhase.ACTIVE) return
        pauseStartRealtime = timeProvider()
        _state.update { it.copy(phase = WorkoutPhase.PAUSED, previousPhaseBeforePause = currentPhase) }

        WorkoutTimerService.updateNotification(
            context = context,
            tempoRestante = if (currentPhase.isWarmup) _state.value.warmupTempoRestanteFormatado else _state.value.tempoFormatado,
            repAtual = if (currentPhase.isWarmup) _state.value.warmupRepsCompletadas else _state.value.currentRepDisplay,
            totalReps = _state.value.totalRepsPrevistas,
            tipo = context.getString(_state.value.tipo.tituloRes),
            isPaused = true
        )
    }

    fun resumeWorkout() {
        if (_state.value.phase != WorkoutPhase.PAUSED) return
        val resumeRealtime = timeProvider()
        val pauseDuration = resumeRealtime - pauseStartRealtime
        totalPausedDurationMillis += pauseDuration
        val originalPhase = _state.value.previousPhaseBeforePause
        _state.update { it.copy(phase = originalPhase) }

        WorkoutTimerService.updateNotification(
            context = context,
            tempoRestante = if (originalPhase.isWarmup) _state.value.warmupTempoRestanteFormatado else _state.value.tempoFormatado,
            repAtual = if (originalPhase.isWarmup) _state.value.warmupRepsCompletadas else _state.value.currentRepDisplay,
            totalReps = _state.value.totalRepsPrevistas,
            tipo = context.getString(_state.value.tipo.tituloRes),
            isPaused = false
        )
    }

    /**
     * Finaliza a sessão de treino.
     * Regra estrita: As repetições feitas durante o aquecimento NÃO contam para o total
     * de repsFeitas da sessão. A duração gravada é de 1200s (treino completo de 20 min)
     * ou o tempo real do treino principal se encerrado antes.
     */
    fun finalizarSessao(isEarly: Boolean) {
        // 1. Cancela todos os CoroutineJobs ativos
        tickerJob?.cancel()
        tickerJob = null
        countdownJob?.cancel()
        countdownJob = null
        beepJob?.cancel()
        beepJob = null

        // 2. Para o foreground service
        val serviceInstance = WorkoutTimerService.activeInstance
        if (serviceInstance != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                serviceInstance.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                serviceInstance.stopForeground(true)
            }
            serviceInstance.stopSelf()
        } else {
            WorkoutTimerService.stopService(context)
        }

        // 3. Libera qualquer WakeLock ativo se estiver sendo mantido
        serviceInstance?.liberarWakeLock()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null

        // 4. Congela tempoDecorridoSegundos e repsFeitasCalculadas
        val currentState = _state.value
        val totalTreinoSegundos = DURACAO_MINUTOS * 60

        val tempoDecorridoSegundos: Int = if (!isEarly) {
            totalTreinoSegundos
        } else {
            when (currentState.phase) {
                WorkoutPhase.ACTIVE -> {
                    val now = timeProvider()
                    val activeElapsedMillis = (now - sessionStartRealtime) - totalPausedDurationMillis
                    (activeElapsedMillis / 1000).toInt().coerceIn(0, totalTreinoSegundos)
                }
                WorkoutPhase.PAUSED -> {
                    if (currentState.previousPhaseBeforePause == WorkoutPhase.ACTIVE) {
                        val activeElapsedMillis = (pauseStartRealtime - sessionStartRealtime) - totalPausedDurationMillis
                        (activeElapsedMillis / 1000).toInt().coerceIn(0, totalTreinoSegundos)
                    } else {
                        0
                    }
                }
                WorkoutPhase.COUNTDOWN, WorkoutPhase.IDLE,
                WorkoutPhase.WARMUP_BURPEE_1, WorkoutPhase.WARMUP_REST_1,
                WorkoutPhase.WARMUP_BURPEE_2, WorkoutPhase.WARMUP_REST_2 -> 0
                else -> currentState.elapsedSeconds.coerceIn(0, totalTreinoSegundos)
            }
        }

        val instantes = currentState.repInstants
        val repsFeitasCalculadas: Int = if (!isEarly) {
            instantes.size
        } else if (currentState.phase.isWarmup || currentState.phase == WorkoutPhase.COUNTDOWN || currentState.phase == WorkoutPhase.IDLE) {
            0
        } else {
            instantes.count { it <= tempoDecorridoSegundos }
        }

        // 5. Muda a fase para STOPPED_EARLY se isEarly = true, ou COMPLETED se false
        val novaFase = if (isEarly) WorkoutPhase.STOPPED_EARLY else WorkoutPhase.COMPLETED

        val remainingSecs = (totalTreinoSegundos - tempoDecorridoSegundos).coerceAtLeast(0)
        val mm = remainingSecs / 60
        val ss = remainingSecs % 60
        val tempoFormatado = String.format("%02d:%02d", mm, ss)

        _state.update {
            it.copy(
                phase = novaFase,
                tempoDecorridoSegundos = tempoDecorridoSegundos,
                repsFeitasCalculadas = repsFeitasCalculadas,
                elapsedSeconds = tempoDecorridoSegundos,
                remainingSeconds = remainingSecs,
                tempoFormatado = tempoFormatado,
                currentRepIndex = if (repsFeitasCalculadas > 0) repsFeitasCalculadas - 1 else 0,
                currentRepDisplay = repsFeitasCalculadas,
                repsCompletadas = repsFeitasCalculadas,
                secondsUntilNextRep = 0,
                nextRepProgress = 1f,
                progressOverall = (tempoDecorridoSegundos.toFloat() / totalTreinoSegundos.toFloat()).coerceIn(0f, 1f)
            )
        }
    }

    fun stopWorkout(savePartial: Boolean) {
        if (savePartial) {
            finalizarSessao(isEarly = true)
        } else {
            tickerJob?.cancel()
            tickerJob = null
            countdownJob?.cancel()
            countdownJob = null
            beepJob?.cancel()
            beepJob = null

            val serviceInstance = WorkoutTimerService.activeInstance
            if (serviceInstance != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    serviceInstance.stopForeground(Service.STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    serviceInstance.stopForeground(true)
                }
                serviceInstance.liberarWakeLock()
                serviceInstance.stopSelf()
            } else {
                WorkoutTimerService.stopService(context)
            }

            try {
                if (wakeLock?.isHeld == true) wakeLock?.release()
            } catch (_: Exception) {}
            wakeLock = null

            _state.update { it.copy(phase = WorkoutPhase.IDLE) }
        }
    }

    fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        countdownJob?.cancel()
        countdownJob = null
        beepJob?.cancel()
        beepJob = null

        val serviceInstance = WorkoutTimerService.activeInstance
        if (serviceInstance != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                serviceInstance.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                serviceInstance.stopForeground(true)
            }
            serviceInstance.liberarWakeLock()
            serviceInstance.stopSelf()
        } else {
            WorkoutTimerService.stopService(context)
        }

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null

        _state.value = WorkoutActiveState()
    }
}
