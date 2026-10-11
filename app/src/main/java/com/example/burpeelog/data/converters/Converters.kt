package com.example.burpeelog.data.converters

import androidx.room.TypeConverter
import com.example.burpeelog.data.model.TipoExercicio

/**
 * Conversores de tipo para o Room Database.
 */
class Converters {

    @TypeConverter
    fun fromTipoExercicio(tipo: TipoExercicio?): String? {
        return tipo?.name
    }

    @TypeConverter
    fun toTipoExercicio(value: String?): TipoExercicio {
        if (value.isNullOrBlank()) return TipoExercicio.SIX_COUNT
        return runCatching { TipoExercicio.valueOf(value) }
            .getOrDefault(TipoExercicio.SIX_COUNT)
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        if (list == null) return null
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrBlank()) return null
        return value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
