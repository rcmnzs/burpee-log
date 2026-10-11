package com.example.burpeelog.data.model

/**
 * Tipos de exercícios do Busy Dad Program.
 */
enum class TipoExercicio(val titulo: String, val descricao: String) {
    SIX_COUNT(
        titulo = "Six-Count Burpee",
        descricao = "Burpee tradicional em 6 tempos com flexão única."
    ),
    NAVY_SEAL(
        titulo = "Navy Seal Burpee",
        descricao = "Burpee militar com 3 flexões e 2 joelhadas intercaladas."
    );

    override fun toString(): String = titulo
}
