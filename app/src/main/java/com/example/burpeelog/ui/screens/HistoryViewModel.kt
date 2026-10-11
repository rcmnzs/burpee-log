package com.example.burpeelog.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.data.repository.SessaoRepository
import com.example.burpeelog.domain.ChartPeriodFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessoes: List<Sessao> = emptyList(),
    val todasSessoes: List<Sessao> = emptyList(),
    val filtroSelecionado: TipoExercicio? = null,
    val periodoGrafico: ChartPeriodFilter = ChartPeriodFilter.ALL,
    val totalGeral: Int = 0,
    val totalSixCount: Int = 0,
    val totalNavySeal: Int = 0
)

class HistoryViewModel(
    private val repository: SessaoRepository
) : ViewModel() {

    private val _filtro = MutableStateFlow<TipoExercicio?>(null)
    val filtro: StateFlow<TipoExercicio?> = _filtro.asStateFlow()

    private val _periodoGrafico = MutableStateFlow(ChartPeriodFilter.ALL)
    val periodoGrafico: StateFlow<ChartPeriodFilter> = _periodoGrafico.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allSessoes,
        _filtro,
        _periodoGrafico
    ) { todas, filtroAtivo, periodo ->
        val sessoesOrdenadas = todas.sortedByDescending { it.dataHora }
        val filtradas = if (filtroAtivo != null) {
            sessoesOrdenadas.filter { it.tipo == filtroAtivo }
        } else {
            sessoesOrdenadas
        }

        HistoryUiState(
            sessoes = filtradas,
            todasSessoes = sessoesOrdenadas,
            filtroSelecionado = filtroAtivo,
            periodoGrafico = periodo,
            totalGeral = todas.size,
            totalSixCount = todas.count { it.tipo == TipoExercicio.SIX_COUNT },
            totalNavySeal = todas.count { it.tipo == TipoExercicio.NAVY_SEAL }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun definirFiltro(tipo: TipoExercicio?) {
        _filtro.value = tipo
    }

    fun definirPeriodoGrafico(periodo: ChartPeriodFilter) {
        _periodoGrafico.value = periodo
    }

    fun excluirSessao(sessao: Sessao) {
        viewModelScope.launch {
            repository.delete(sessao)
        }
    }

    fun atualizarSessao(sessao: Sessao) {
        viewModelScope.launch {
            repository.update(sessao)
        }
    }

    fun adicionarSessaoPassada(
        dataHora: Long,
        tipo: TipoExercicio,
        repsFeitas: Int,
        observacao: String?
    ) {
        viewModelScope.launch {
            val novaSessao = Sessao(
                dataHora = dataHora,
                tipo = tipo,
                repsFeitas = repsFeitas,
                observacao = observacao?.trim()?.ifEmpty { null },
                importada = false
            )
            repository.insert(novaSessao)
        }
    }

    class Factory(
        private val repository: SessaoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
