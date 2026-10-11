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
