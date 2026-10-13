package com.example.burpeelog.data.repository

import com.example.burpeelog.data.dao.SessaoDao
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import kotlinx.coroutines.flow.Flow

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

interface SessaoRepository {
    val allSessoes: Flow<List<Sessao>>
    val totalSessoesCount: Flow<Int>
    val ultimaSessao: Flow<Sessao?>

    fun getSessaoById(id: Long): Flow<Sessao?>
    fun getSessoesByDateRange(inicio: Long, fim: Long): Flow<List<Sessao>>
    fun getSessoesByTipo(tipo: TipoExercicio): Flow<List<Sessao>>
    fun getUltimaSessaoPorTipo(tipo: TipoExercicio): Flow<Sessao?>
    fun getSessoesCountInRange(inicio: Long, fim: Long): Flow<Int>

    fun getDuracaoSegundosInRange(inicio: Long, fim: Long): Flow<Int>
    fun getSegundosSemana(inicio: Long, fim: Long): Flow<Int>
    fun getMinutosSemana(inicio: Long, fim: Long): Flow<Int>
    fun isSemanaCompleta(inicio: Long, fim: Long): Flow<Boolean>

    fun getMinutosSemana(
        hoje: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<Int>

    fun getSegundosSemana(
        hoje: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<Int>

    fun isSemanaCompleta(
        hoje: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<Boolean>

    suspend fun insert(sessao: Sessao): Long
    suspend fun insertAll(sessoes: List<Sessao>): List<Long>
    suspend fun importarSessoes(sessoes: List<Sessao>): List<Long>
    suspend fun update(sessao: Sessao)
    suspend fun delete(sessao: Sessao)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()

    companion object {
        const val META_SEMANAL_SEGUNDOS = 4800 // 80 minutos * 60 segundos

        /**
         * Calcula o timestamp inicial (segunda-feira 00:00:00) e final (domingo 23:59:59.999) da semana.
         */
        fun calcularLimitesSemana(
            hoje: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Pair<Long, Long> {
            val segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val domingo = hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val inicioMillis = segunda.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val fimMillis = domingo.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
            return Pair(inicioMillis, fimMillis)
        }
    }
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

    override fun getDuracaoSegundosInRange(inicio: Long, fim: Long): Flow<Int> =
        sessaoDao.getDuracaoSegundosInRange(inicio, fim)

    override fun getSegundosSemana(inicio: Long, fim: Long): Flow<Int> =
        sessaoDao.getSegundosSemana(inicio, fim)

    override fun getMinutosSemana(inicio: Long, fim: Long): Flow<Int> =
        sessaoDao.getMinutosSemana(inicio, fim)

    override fun isSemanaCompleta(inicio: Long, fim: Long): Flow<Boolean> =
        sessaoDao.isSemanaCompleta(inicio, fim)

    override fun getMinutosSemana(hoje: LocalDate, zoneId: ZoneId): Flow<Int> {
        val (inicio, fim) = SessaoRepository.calcularLimitesSemana(hoje, zoneId)
        return sessaoDao.getMinutosSemana(inicio, fim)
    }

    override fun getSegundosSemana(hoje: LocalDate, zoneId: ZoneId): Flow<Int> {
        val (inicio, fim) = SessaoRepository.calcularLimitesSemana(hoje, zoneId)
        return sessaoDao.getSegundosSemana(inicio, fim)
    }

    override fun isSemanaCompleta(hoje: LocalDate, zoneId: ZoneId): Flow<Boolean> {
        val (inicio, fim) = SessaoRepository.calcularLimitesSemana(hoje, zoneId)
        return sessaoDao.isSemanaCompleta(inicio, fim)
    }

    override suspend fun insert(sessao: Sessao): Long =
        sessaoDao.insertSessao(sessao)

    override suspend fun insertAll(sessoes: List<Sessao>): List<Long> =
        sessaoDao.insertAll(sessoes)

    override suspend fun importarSessoes(sessoes: List<Sessao>): List<Long> =
        sessaoDao.importarSessoesEmTransacao(sessoes)

    override suspend fun update(sessao: Sessao) =
        sessaoDao.updateSessao(sessao)

    override suspend fun delete(sessao: Sessao) =
        sessaoDao.deleteSessao(sessao)

    override suspend fun deleteById(id: Long) =
        sessaoDao.deleteSessaoById(id)

    override suspend fun clearAll() =
        sessaoDao.clearAll()
}
