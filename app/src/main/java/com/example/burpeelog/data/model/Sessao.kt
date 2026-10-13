package com.example.burpeelog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Constantes oficiais do Busy Dad Program.
 * Toda sessão dura exatamente 20 minutos (sem duração editável).
 * Meta semanal: 80 minutos por semana (4 sessões de 20 minutos).
 */
const val DURACAO_MINUTOS = 20
const val META_SEMANAL_MINUTOS = 80
const val META_SEMANAL_SESSOES = 4

/**
 * Entidade Room que representa uma sessão de treino de 20 minutos.
 */
@Entity(tableName = "sessoes")
data class Sessao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val dataHora: Long = System.currentTimeMillis(),

    val tipo: TipoExercicio,

    val padraoIntervalosSegundos: List<Int>? = null,

    val repsPrevistas: Int? = null,

    val repsFeitas: Int,

    val duracaoSegundos: Int = 1200,

    val observacao: String? = null,

    val importada: Boolean = false
) {
    /**
     * Duração fixa da sessão em minutos, conforme as regras do Busy Dad Program.
     */
    val duracaoMinutos: Int
        get() = DURACAO_MINUTOS
}
