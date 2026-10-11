package com.example.burpeelog.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.burpeelog.data.model.TipoExercicio

class PatternPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("burpee_pattern_prefs", Context.MODE_PRIVATE)

    fun getPadraoParaTipo(tipo: TipoExercicio): List<Int> {
        val key = "pattern_${tipo.name}"
        val saved = prefs.getString(key, null)
        if (!saved.isNullOrBlank()) {
            val list = saved.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (list.isNotEmpty()) return list
        }
        return when (tipo) {
            TipoExercicio.SIX_COUNT -> listOf(30)
            TipoExercicio.NAVY_SEAL -> listOf(40)
        }
    }

    fun salvarPadraoParaTipo(tipo: TipoExercicio, padrao: List<Int>) {
        if (padrao.isNotEmpty()) {
            val str = padrao.joinToString(",")
            prefs.edit().putString("pattern_${tipo.name}", str).apply()
        }
    }
}
