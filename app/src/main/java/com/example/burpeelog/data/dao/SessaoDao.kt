package com.example.burpeelog.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import kotlinx.coroutines.flow.Flow

@Dao
interface SessaoDao {

    @Query("SELECT * FROM sessoes ORDER BY dataHora DESC")
    fun getAllSessoes(): Flow<List<Sessao>>

    @Query("SELECT * FROM sessoes ORDER BY dataHora DESC LIMIT 1")
    fun getUltimaSessao(): Flow<Sessao?>

    @Query("SELECT * FROM sessoes WHERE tipo = :tipo ORDER BY dataHora DESC LIMIT 1")
    fun getUltimaSessaoPorTipo(tipo: TipoExercicio): Flow<Sessao?>

    @Query("SELECT * FROM sessoes WHERE id = :id LIMIT 1")
    fun getSessaoById(id: Long): Flow<Sessao?>

    @Query("SELECT * FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim ORDER BY dataHora DESC")
    fun getSessoesByDateRange(inicio: Long, fim: Long): Flow<List<Sessao>>

    @Query("SELECT * FROM sessoes WHERE tipo = :tipo ORDER BY dataHora DESC")
    fun getSessoesByTipo(tipo: TipoExercicio): Flow<List<Sessao>>

    @Query("SELECT COUNT(*) FROM sessoes")
    fun getTotalSessoesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim")
    fun getSessoesCountInRange(inicio: Long, fim: Long): Flow<Int>

    /**
     * Soma a duração em segundos das sessões em um intervalo de tempo.
     */
    @Query("SELECT COALESCE(SUM(duracaoSegundos), 0) FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim")
    fun getDuracaoSegundosInRange(inicio: Long, fim: Long): Flow<Int>

    /**
     * Soma a duração em segundos das sessões da semana (de segunda a domingo).
     */
    @Query("SELECT COALESCE(SUM(duracaoSegundos), 0) FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim")
    fun getSegundosSemana(inicio: Long, fim: Long): Flow<Int>

    /**
     * Calcula os minutos de treino da semana dividindo a soma de duracaoSegundos por 60.
     */
    @Query("SELECT COALESCE(SUM(duracaoSegundos), 0) / 60 FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim")
    fun getMinutosSemana(inicio: Long, fim: Long): Flow<Int>

    /**
     * Verifica se a semana atingiu a meta de 4800 segundos (80 minutos).
     */
    @Query("SELECT CASE WHEN COALESCE(SUM(duracaoSegundos), 0) >= 4800 THEN 1 ELSE 0 END FROM sessoes WHERE dataHora >= :inicio AND dataHora <= :fim")
    fun isSemanaCompleta(inicio: Long, fim: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessao(sessao: Sessao): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessoes: List<Sessao>): List<Long>

    @androidx.room.Transaction
    suspend fun importarSessoesEmTransacao(sessoes: List<Sessao>): List<Long> {
        return insertAll(sessoes)
    }

    @Update
    suspend fun updateSessao(sessao: Sessao)

    @Delete
    suspend fun deleteSessao(sessao: Sessao)

    @Query("DELETE FROM sessoes WHERE id = :id")
    suspend fun deleteSessaoById(id: Long)

    @Query("DELETE FROM sessoes")
    suspend fun clearAll()
}
