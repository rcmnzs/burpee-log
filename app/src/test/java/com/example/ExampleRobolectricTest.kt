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
import org.junit.Assert.assertFalse
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

        manager.finalizarSessao(isEarly = true)
        assertEquals(WorkoutPhase.STOPPED_EARLY, manager.state.value.phase)
        assertEquals(0, manager.state.value.repsFeitasCalculadas)

        manager.reset()
        assertEquals(WorkoutPhase.IDLE, manager.state.value.phase)
    }

    @Test
    fun `WorkoutSessionManager finalizarSessao completa o treino com sucesso`() {
        val manager = WorkoutSessionManager(context)

        manager.startWorkout(TipoExercicio.SIX_COUNT, listOf(30))
        assertEquals(WorkoutPhase.COUNTDOWN, manager.state.value.phase)

        manager.finalizarSessao(isEarly = false)
        assertEquals(WorkoutPhase.COMPLETED, manager.state.value.phase)
        assertEquals(DURACAO_MINUTOS * 60, manager.state.value.tempoDecorridoSegundos)
        assertEquals(manager.state.value.repInstants.size, manager.state.value.repsFeitasCalculadas)

        manager.reset()
        assertEquals(WorkoutPhase.IDLE, manager.state.value.phase)
    }

    @Test
    fun `migracao 1 para 2 adiciona duracaoSegundos com valor padrao 1200`() = runTest {
        val dbName = "test_migration.db"
        context.deleteDatabase(dbName)
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
            .create(
                androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `sessoes` (" +
                                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                    "`dataHora` INTEGER NOT NULL, " +
                                    "`tipo` TEXT NOT NULL, " +
                                    "`padraoIntervalosSegundos` TEXT, " +
                                    "`repsPrevistas` INTEGER, " +
                                    "`repsFeitas` INTEGER NOT NULL, " +
                                    "`observacao` TEXT, " +
                                    "`importada` INTEGER NOT NULL)"
                            )
                        }

                        override fun onUpgrade(
                            db: androidx.sqlite.db.SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) {}
                    })
                    .build()
            )

        val dbV1 = helper.writableDatabase
        dbV1.execSQL(
            "INSERT INTO sessoes (dataHora, tipo, repsFeitas, importada) VALUES (1000, 'SIX_COUNT', 50, 0)"
        )
        dbV1.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val sessao = migratedDb.sessaoDao().getSessaoById(1).first()
        assertNotNull(sessao)
        assertEquals(1200, sessao?.duracaoSegundos)
        assertEquals(50, sessao?.repsFeitas)

        migratedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `dao e repository calculam minutos da semana somando duracaoSegundos e verificam meta de 4800`() = runTest {
        val dao = database.sessaoDao()
        val repository = SessaoRepositoryImpl(dao)
        val hoje = java.time.LocalDate.of(2026, 10, 14) // Quarta-feira
        val zoneId = java.time.ZoneId.systemDefault()

        // Sessão 1: Segunda-feira com 1800 segundos (30 min)
        val segMillis = java.time.LocalDate.of(2026, 10, 12).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        repository.insert(Sessao(dataHora = segMillis, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50, duracaoSegundos = 1800))

        // Sessão 2: Terça-feira com 1200 segundos (20 min)
        val terMillis = java.time.LocalDate.of(2026, 10, 13).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        repository.insert(Sessao(dataHora = terMillis, tipo = TipoExercicio.NAVY_SEAL, repsFeitas = 30, duracaoSegundos = 1200))

        // Total até agora: 3000 segundos (50 min). Ainda não atingiu 4800 (80 min)
        assertEquals(3000, repository.getSegundosSemana(hoje, zoneId).first())
        assertEquals(50, repository.getMinutosSemana(hoje, zoneId).first())
        assertFalse(repository.isSemanaCompleta(hoje, zoneId).first())

        // Sessão 3: Sexta-feira com 1800 segundos (30 min)
        val sexMillis = java.time.LocalDate.of(2026, 10, 16).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        repository.insert(Sessao(dataHora = sexMillis, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 45, duracaoSegundos = 1800))

        // Total agora: 4800 segundos (80 min) -> semana completa!
        assertEquals(4800, repository.getSegundosSemana(hoje, zoneId).first())
        assertEquals(80, repository.getMinutosSemana(hoje, zoneId).first())
        assertTrue(repository.isSemanaCompleta(hoje, zoneId).first())

        // Sessão da semana anterior (Domingo dia 11): NÃO deve ser incluída
        val domAnteriorMillis = java.time.LocalDate.of(2026, 10, 11).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli()
        repository.insert(Sessao(dataHora = domAnteriorMillis, tipo = TipoExercicio.SIX_COUNT, repsFeitas = 50, duracaoSegundos = 1200))

        // Permanece 4800 segundos e 80 minutos para a semana de 12 a 18 de Outubro
        assertEquals(4800, repository.getSegundosSemana(hoje, zoneId).first())
        assertEquals(80, repository.getMinutosSemana(hoje, zoneId).first())
        assertTrue(repository.isSemanaCompleta(hoje, zoneId).first())
    }
}

