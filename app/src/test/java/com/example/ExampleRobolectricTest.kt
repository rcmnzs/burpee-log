package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.burpeelog.data.database.AppDatabase
import com.example.burpeelog.data.model.DURACAO_MINUTOS
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.data.model.TipoExercicio
import com.example.burpeelog.data.preferences.PatternPreferences
import com.example.burpeelog.data.repository.SessaoRepositoryImpl
import com.example.burpeelog.service.WorkoutPhase
import com.example.burpeelog.service.WorkoutSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Burpee Log", appName)
    }

    @Test
    fun `insert and retrieve sessao in room`() = runTest {
        val dao = database.sessaoDao()
        val repository = SessaoRepositoryImpl(dao)

        val sessao = Sessao(
            tipo = TipoExercicio.SIX_COUNT,
            padraoIntervalosSegundos = listOf(30, 20),
            repsPrevistas = 50,
            repsFeitas = 48,
            observacao = "Treino concluído com sucesso",
            importada = false
        )

        assertEquals(20, DURACAO_MINUTOS)
        assertEquals(20, sessao.duracaoMinutos)

        val id = repository.insert(sessao)
        val loaded = repository.getSessaoById(id).first()

        assertNotNull(loaded)
        assertEquals(TipoExercicio.SIX_COUNT, loaded?.tipo)
        assertEquals(48, loaded?.repsFeitas)
        assertEquals(listOf(30, 20), loaded?.padraoIntervalosSegundos)
        assertEquals(false, loaded?.importada)
    }

    @Test
    fun `update and delete sessao in room`() = runTest {
        val dao = database.sessaoDao()
        val repository = SessaoRepositoryImpl(dao)

        val sessao = Sessao(
            tipo = TipoExercicio.SIX_COUNT,
            repsFeitas = 40,
            importada = true
        )
        val id = repository.insert(sessao)
        val inserida = repository.getSessaoById(id).first()!!
        assertTrue(inserida.importada)

        // Atualizar
        val atualizada = inserida.copy(repsFeitas = 45, observacao = "Editado")
        repository.update(atualizada)

        val recarregada = repository.getSessaoById(id).first()
        assertEquals(45, recarregada?.repsFeitas)
        assertEquals("Editado", recarregada?.observacao)

        // Excluir
        repository.delete(recarregada!!)
        val deletada = repository.getSessaoById(id).first()
        assertNull(deletada)
    }

    @Test
    fun `filter sessoes by tipo`() = runTest {
        val dao = database.sessaoDao()
        val repository = SessaoRepositoryImpl(dao)

        repository.insert(Sessao(tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50))
        repository.insert(Sessao(tipo = TipoExercicio.SIX_COUNT, repsFeitas = 60))
        repository.insert(Sessao(tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30))

        val sixCounts = repository.getSessoesByTipo(TipoExercicio.SIX_COUNT).first()
        val navySeals = repository.getSessoesByTipo(TipoExercicio.NAVY_SEAL).first()

        assertEquals(2, sixCounts.size)
        assertEquals(1, navySeals.size)
    }

    @Test
    fun `getUltimaSessao retorna a sessao mais recente e permite sugerir alternancia`() = runTest {
        val dao = database.sessaoDao()
        val repository = SessaoRepositoryImpl(dao)

        val primeira = Sessao(
            dataHora = 1000L,
            tipo = TipoExercicio.SIX_COUNT,
            repsFeitas = 50
        )
        val segunda = Sessao(
            dataHora = 2000L,
            tipo = TipoExercicio.NAVY_SEAL,
            repsFeitas = 30
        )

        repository.insert(primeira)
        repository.insert(segunda)

        val ultima = repository.ultimaSessao.first()
        assertNotNull(ultima)
        assertEquals(TipoExercicio.NAVY_SEAL, ultima?.tipo)

        val proximoSugerido = if (ultima?.tipo == TipoExercicio.SIX_COUNT) {
            TipoExercicio.NAVY_SEAL
        } else {
            TipoExercicio.SIX_COUNT
        }
        assertEquals(TipoExercicio.SIX_COUNT, proximoSugerido)
    }

    @Test
    fun `PatternPreferences salva e recupera padroes por tipo`() {
        val prefs = PatternPreferences(context)

        prefs.salvarPadraoParaTipo(TipoExercicio.SIX_COUNT, listOf(30, 20))
        prefs.salvarPadraoParaTipo(TipoExercicio.NAVY_SEAL, listOf(40, 30))

        assertEquals(listOf(30, 20), prefs.getPadraoParaTipo(TipoExercicio.SIX_COUNT))
        assertEquals(listOf(40, 30), prefs.getPadraoParaTipo(TipoExercicio.NAVY_SEAL))
    }

    @Test
    fun `WorkoutSessionManager inicia contagem e gerencia estados`() {
        val manager = WorkoutSessionManager(context)

        assertEquals(WorkoutPhase.IDLE, manager.state.value.phase)

        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30, 20))
        assertEquals(WorkoutPhase.COUNTDOWN, manager.state.value.phase)
        assertEquals(50, manager.state.value.totalRepsPrevistas)

        manager.stopWorkout(savePartial = true)
        assertEquals(WorkoutPhase.STOPPED_EARLY, manager.state.value.phase)

        manager.reset()
        assertEquals(WorkoutPhase.IDLE, manager.state.value.phase)
    }
}
