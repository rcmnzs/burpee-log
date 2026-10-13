package com.example.burpeelog.service

import android.app.Service
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
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
    RUNNING,
    PAUSED,
    FINISHED,
    COMPLETED,
    STOPPED_EARLY
}

data class WorkoutActiveState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val tipo: TipoExercicio = TipoExercicio.SIX_COUNT,
    val padrao: List<Int> = listOf(30),
    val totalRepsPrevistas: Int = 40,
    val countdownSeconds: Int = 5,
    val elapsedSeconds: Int = 0,
    val tempoDecorridoSegundos: Int = 0,
    val remainingSeconds: Int = DURACAO_MINUTOS * 60,
    val tempoFormatado: String = "20:00",
    val currentRepIndex: Int = 0, // 0-indexed da rep que acabou de tocar
    val currentRepDisplay: Int = 0, // 1-indexed para exibição
    val secondsUntilNextRep: Int = 30,
    val nextRepProgress: Float = 0f, // 0.0f a 1.0f
    val currentMinute: Int = 1,
    val progressOverall: Float = 0f,
    val repInstants: List<Int> = emptyList(),
    val repsCompletadas: Int = 0,
    val repsFeitasCalculadas: Int = 0
)

class WorkoutSessionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null
    private var countdownJob: Job? = null
    private var beepJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val feedbackHelper = WorkoutFeedbackHelper(context)

    private val _state = MutableStateFlow(WorkoutActiveState())
    val state: StateFlow<WorkoutActiveState> = _state.asStateFlow()

    // Variáveis de tempo monotônico imunes a deriva e mudanças de relógio
    private var sessionStartRealtime: Long = 0L
    private var pauseStartRealtime: Long = 0L
    private var totalPausedDurationMillis: Long = 0L

    private var lastFiredRepIndex: Int = -1
    private var lastFiredMinute: Int = 0

    fun setWakeLock(lock: PowerManager.WakeLock?) {
        this.wakeLock = lock
    }

    fun startWorkout(tipo: TipoExercicio, padrao: List<Int>) {
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
        totalPausedDurationMillis = 0L

        _state.value = WorkoutActiveState(
            phase = WorkoutPhase.COUNTDOWN,
            tipo = tipo,
            padrao = padrao,
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
            repsFeitasCalculadas = 0
        )

        // Inicia o Foreground Service no Android
        WorkoutTimerService.startService(context)

        tickerJob = scope.launch {
            // Contagem regressiva de 5 segundos
            for (c in 5 downTo 1) {
                if (!isActive) return@launch
                _state.update { it.copy(countdownSeconds = c) }
                feedbackHelper.playCountdownBeep(isFinal = (c == 1))
                delay(1000L)
            }

            if (!isActive) return@launch

            // Início do treino ativo
            sessionStartRealtime = SystemClock.elapsedRealtime()
            totalPausedDurationMillis = 0L

            _state.update {
                it.copy(
                    phase = WorkoutPhase.RUNNING,
                    countdownSeconds = 0,
                    currentRepIndex = 0,
                    currentRepDisplay = 1,
                    repsCompletadas = 1,
                    repsFeitasCalculadas = 1
                )
            }

            // A primeira repetição ocorre no instante 0s
            lastFiredRepIndex = 0
            lastFiredMinute = 1
            feedbackHelper.playRepCue()

            // Loop de execução do treino com relógio monotônico
            loopTreinoAtivo()
        }
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

            if (currentState.phase != WorkoutPhase.RUNNING) {
                break
            }

            val now = SystemClock.elapsedRealtime()
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
                tipo = currentState.tipo.titulo,
                isPaused = false
            )

            delay(150L)
        }
    }

    fun pauseWorkout() {
        if (_state.value.phase != WorkoutPhase.RUNNING) return
        pauseStartRealtime = SystemClock.elapsedRealtime()
        _state.update { it.copy(phase = WorkoutPhase.PAUSED) }

        WorkoutTimerService.updateNotification(
            context = context,
            tempoRestante = _state.value.tempoFormatado,
            repAtual = _state.value.currentRepDisplay,
            totalReps = _state.value.totalRepsPrevistas,
            tipo = _state.value.tipo.titulo,
            isPaused = true
        )
    }

    fun resumeWorkout() {
        if (_state.value.phase != WorkoutPhase.PAUSED) return
        val resumeRealtime = SystemClock.elapsedRealtime()
        totalPausedDurationMillis += (resumeRealtime - pauseStartRealtime)
        _state.update { it.copy(phase = WorkoutPhase.RUNNING) }

        WorkoutTimerService.updateNotification(
            context = context,
            tempoRestante = _state.value.tempoFormatado,
            repAtual = _state.value.currentRepDisplay,
            totalReps = _state.value.totalRepsPrevistas,
            tipo = _state.value.tipo.titulo,
            isPaused = false
        )
    }

    /**
     * Finaliza a sessão de treino ativa.
     * 1. Cancela todos os CoroutineJobs ativos (cronômetro, agendador de bipes, contagem).
     * 2. Para o foreground service: chama stopForeground(STOP_FOREGROUND_REMOVE) e stopSelf(), removendo a notificação.
     * 3. Libera qualquer WakeLock ativo se estiver sendo mantido.
     * 4. Congela dois valores no estado: tempoDecorridoSegundos (tempo total treinado descontando pausas)
     *    e repsFeitasCalculadas (número de instantes de repetição cujo tempo previsto já passou).
     * 5. Muda a fase para STOPPED_EARLY se isEarly = true, ou COMPLETED se false.
     */
    fun finalizarSessao(isEarly: Boolean) {
        // 1. Cancela todos os CoroutineJobs ativos
        tickerJob?.cancel()
        tickerJob = null
        countdownJob?.cancel()
        countdownJob = null
        beepJob?.cancel()
        beepJob = null

        // 2. Para o foreground service: chama stopForeground(STOP_FOREGROUND_REMOVE) e stopSelf(), removendo a notificação
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
                WorkoutPhase.RUNNING -> {
                    val now = SystemClock.elapsedRealtime()
                    val activeElapsedMillis = (now - sessionStartRealtime) - totalPausedDurationMillis
                    (activeElapsedMillis / 1000).toInt().coerceIn(0, totalTreinoSegundos)
                }
                WorkoutPhase.PAUSED -> {
                    val activeElapsedMillis = (pauseStartRealtime - sessionStartRealtime) - totalPausedDurationMillis
                    (activeElapsedMillis / 1000).toInt().coerceIn(0, totalTreinoSegundos)
                }
                WorkoutPhase.COUNTDOWN, WorkoutPhase.IDLE -> 0
                else -> currentState.elapsedSeconds.coerceIn(0, totalTreinoSegundos)
            }
        }

        val instantes = currentState.repInstants
        val repsFeitasCalculadas: Int = if (!isEarly) {
            instantes.size
        } else if (currentState.phase == WorkoutPhase.COUNTDOWN || currentState.phase == WorkoutPhase.IDLE) {
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
