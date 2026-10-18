package com.example.burpeelog.data.model

import com.example.R

/**
 * Tipos de exercícios do Busy Dad Program.
 */
enum class TipoExercicio(
    val tituloRes: Int,
    val descricaoRes: Int,
    val titulo: String,
    val descricao: String
) {
    SIX_COUNT(
        tituloRes = R.string.exercise_six_count,
        descricaoRes = R.string.exercise_six_count_desc,
        titulo = "Six-Count Burpee",
        descricao = "Burpee tradicional em 6 tempos com flexão única."
    ),
    NAVY_SEAL(
        tituloRes = R.string.exercise_navy_seal,
        descricaoRes = R.string.exercise_navy_seal_desc,
        titulo = "Navy Seal Burpee",
        descricao = "Burpee militar com 3 flexões e 2 joelhadas intercaladas."
    );

    val titleRes: Int get() = tituloRes
    val descRes: Int get() = descricaoRes

    override fun toString(): String = titulo
}
