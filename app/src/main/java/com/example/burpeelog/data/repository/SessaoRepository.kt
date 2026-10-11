package com.example.burpeelog.data.repository

import com.example.burpeelog.data.dao.SessaoDao
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import kotlinx.coroutines.flow.Flow

interface SessaoRepository {
    val allSessoes: Flow<List<Sessao>>
    val totalSessoesCount: Flow<Int>
    val ultimaSessao: Flow<Sessao?>

    fun getSessaoById(id: Long): Flow<Sessao?>
    fun getSessoesByDateRange(inicio: Long, fim: Long): Flow<List<Sessao>>
    fun getSessoesByTipo(tipo: TipoExercicio): Flow<List<Sessao>>
    fun getUltimaSessaoPorTipo(tipo: TipoExercicio): Flow<Sessao?>
    fun getSessoesCountInRange(inicio: Long, fim: Long): Flow<Int>

    suspend fun insert(sessao: Sessao): Long
    suspend fun insertAll(sessoes: List<Sessao>): List<Long>
    suspend fun update(sessao: Sessao)
    suspend fun delete(sessao: Sessao)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}

class SessaoRepositoryImpl(
    private val sessaoDao: SessaoDao
) : SessaoRepository {

    override val allSessoes: Flow<List<Sessao>> = sessaoDao.getAllSessoes()

    override val totalSessoesCount: Flow<Int> = sessaoDao.getTotalSessoesCount()

    override val ultimaSessao: Flow<Sessao?> = sessaoDao.getUltimaSessao()

    override fun getSessaoById(id: Long): Flow<Sessao?> =
        sessaoDao.getSessaoById(id)

    override fun getSessoesByDateRange(inicio: Long, fim: Long): Flow<List<Sessao>> =
        sessaoDao.getSessoesByDateRange(inicio, fim)

    override fun getSessoesByTipo(tipo: TipoExercicio): Flow<List<Sessao>> =
        sessaoDao.getSessoesByTipo(tipo)

    override fun getUltimaSessaoPorTipo(tipo: TipoExercicio): Flow<Sessao?> =
        sessaoDao.getUltimaSessaoPorTipo(tipo)

    override fun getSessoesCountInRange(inicio: Long, fim: Long): Flow<Int> =
        sessaoDao.getSessoesCountInRange(inicio, fim)

    override suspend fun insert(sessao: Sessao): Long =
        sessaoDao.insertSessao(sessao)

    override suspend fun insertAll(sessoes: List<Sessao>): List<Long> =
        sessaoDao.insertAll(sessoes)

    override suspend fun update(sessao: Sessao) =
        sessaoDao.updateSessao(sessao)

    override suspend fun delete(sessao: Sessao) =
        sessaoDao.deleteSessao(sessao)

    override suspend fun deleteById(id: Long) =
        sessaoDao.deleteSessaoById(id)

    override suspend fun clearAll() =
        sessaoDao.clearAll()
}
