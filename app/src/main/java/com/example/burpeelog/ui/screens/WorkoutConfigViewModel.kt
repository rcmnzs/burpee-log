package com.example.burpeelog.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.data.preferences.PatternPreferences
import com.example.burpeelog.data.repository.SessaoRepository
import com.example.burpeelog.domain.IntervaloCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutConfigUiState(
    val tipoSelecionado: TipoExercicio = TipoExercicio.SIX_COUNT,
    val tipoSugerido: TipoExercicio = TipoExercicio.SIX_COUNT,
    val tipoAlteradoManualmente: Boolean = false,
    val padraoIntervalos: List<Int> = listOf(30),
    val aquecimentoAtivo: Boolean = false,
    val duracaoMinutos: Int = DURACAO_MINUTOS,
    val repsPrevistas: Int = 40,
    val previaCiclo: String = "",
    val ultimaSessao: Sessao? = null,
    val sessaoIniciadaDialog: Boolean = false
)

class WorkoutConfigViewModel(
    private val repository: SessaoRepository,
    private val patternPreferences: PatternPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutConfigUiState())
    val uiState: StateFlow<WorkoutConfigUiState> = _uiState.asStateFlow()

    init {
        // Observar a última sessão para sugerir alternância conforme o Busy Dad Program
        viewModelScope.launch {
            repository.ultimaSessao.collect { ultima ->
                _uiState.update { current ->
                    val sugerido = when (ultima?.tipo) {
                        TipoExercicio.SIX_COUNT -> TipoExercicio.NAVY_SEAL
                        TipoExercicio.NAVY_SEAL -> TipoExercicio.SIX_COUNT
                        null -> TipoExercicio.SIX_COUNT
                    }

                    val tipoFinal = if (!current.tipoAlteradoManualmente) sugerido else current.tipoSelecionado
                    val padrao = patternPreferences.getPadraoParaTipo(tipoFinal)
                    val aquecimento = patternPreferences.getAquecimentoParaTipo(tipoFinal)
                    val reps = IntervaloCalculator.calcularRepsPrevistas(padrao, DURACAO_MINUTOS)
                    val previa = IntervaloCalculator.gerarPreviaDoCiclo(padrao)

                    current.copy(
                        ultimaSessao = ultima,
                        tipoSugerido = sugerido,
                        tipoSelecionado = tipoFinal,
                        padraoIntervalos = padrao,
                        aquecimentoAtivo = aquecimento,
                        repsPrevistas = reps,
                        previaCiclo = previa
                    )
                }
            }
        }
    }

    fun selecionarTipo(tipo: TipoExercicio) {
        val padrao = patternPreferences.getPadraoParaTipo(tipo)
        val aquecimento = patternPreferences.getAquecimentoParaTipo(tipo)
        val reps = IntervaloCalculator.calcularRepsPrevistas(padrao, DURACAO_MINUTOS)
        val previa = IntervaloCalculator.gerarPreviaDoCiclo(padrao)

        _uiState.update {
            it.copy(
                tipoSelecionado = tipo,
                tipoAlteradoManualmente = true,
                padraoIntervalos = padrao,
                aquecimentoAtivo = aquecimento,
                repsPrevistas = reps,
                previaCiclo = previa
            )
        }
    }

    fun alternarAquecimento(ativo: Boolean) {
        val tipo = _uiState.value.tipoSelecionado
        patternPreferences.salvarAquecimentoParaTipo(tipo, ativo)
        _uiState.update { it.copy(aquecimentoAtivo = ativo) }
    }

    fun alterarIntervalo(index: Int, novoValorSegundos: Int) {
        val valorSanitizado = novoValorSegundos.coerceIn(
            IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
            IntervaloCalculator.INTERVALO_MAX_SEGUNDOS
        )

        _uiState.update { state ->
            if (index !in state.padraoIntervalos.indices) return@update state
            val novaLista = state.padraoIntervalos.toMutableList()
            novaLista[index] = valorSanitizado
            salvarEAtualizarPadrao(novaLista, state)
        }
    }

    fun adicionarMinuto(valorSegundos: Int? = null) {
        _uiState.update { state ->
            val novoValor = (valorSegundos ?: state.padraoIntervalos.lastOrNull() ?: 30).coerceIn(
                IntervaloCalculator.INTERVALO_MIN_SEGUNDOS,
                IntervaloCalculator.INTERVALO_MAX_SEGUNDOS
            )
            val novaLista = state.padraoIntervalos + novoValor
            salvarEAtualizarPadrao(novaLista, state)
        }
    }

    fun removerMinuto(index: Int) {
        _uiState.update { state ->
            // A lista nunca pode ficar vazia
            if (state.padraoIntervalos.size <= 1) return@update state
            if (index !in state.padraoIntervalos.indices) return@update state
            val novaLista = state.padraoIntervalos.toMutableList()
            novaLista.removeAt(index)
            salvarEAtualizarPadrao(novaLista, state)
        }
    }

    fun moverMinutoParaCima(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            if (index !in state.padraoIntervalos.indices) return@update state
            val novaLista = state.padraoIntervalos.toMutableList()
            val item = novaLista.removeAt(index)
            novaLista.add(index - 1, item)
            salvarEAtualizarPadrao(novaLista, state)
        }
    }

    fun moverMinutoParaBaixo(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            if (index !in 0 until state.padraoIntervalos.size - 1) return@update state
            val novaLista = state.padraoIntervalos.toMutableList()
            val item = novaLista.removeAt(index)
            novaLista.add(index + 1, item)
            salvarEAtualizarPadrao(novaLista, state)
        }
    }

    private fun salvarEAtualizarPadrao(
        novaLista: List<Int>,
        state: WorkoutConfigUiState
    ): WorkoutConfigUiState {
        patternPreferences.salvarPadraoParaTipo(state.tipoSelecionado, novaLista)
        val reps = IntervaloCalculator.calcularRepsPrevistas(novaLista, DURACAO_MINUTOS)
        val previa = IntervaloCalculator.gerarPreviaDoCiclo(novaLista)
        return state.copy(
            padraoIntervalos = novaLista,
            repsPrevistas = reps,
            previaCiclo = previa
        )
    }

    fun iniciarTreino() {
        // Salva com garantia o padrão atual do tipo e aquecimento
        val state = _uiState.value
        patternPreferences.salvarPadraoParaTipo(state.tipoSelecionado, state.padraoIntervalos)
        patternPreferences.salvarAquecimentoParaTipo(state.tipoSelecionado, state.aquecimentoAtivo)
        _uiState.update { it.copy(sessaoIniciadaDialog = true) }
    }

    fun fecharDialogIniciar() {
        _uiState.update { it.copy(sessaoIniciadaDialog = false) }
    }

    class Factory(
        private val repository: SessaoRepository,
        private val patternPreferences: PatternPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutConfigViewModel(repository, patternPreferences) as T
        }
    }
}
