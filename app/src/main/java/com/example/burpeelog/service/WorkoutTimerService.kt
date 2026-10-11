package com.example.burpeelog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.burpeelog.BurpeeLogApplication

class WorkoutTimerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BurpeeLog:WorkoutTimerWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L) // Máximo 30 minutos por precaução
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? BurpeeLogApplication
        val sessionManager = app?.workoutSessionManager

        when (intent?.action) {
            ACTION_STOP -> {
                sessionManager?.stopWorkout(savePartial = true)
                pararForeground()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                sessionManager?.pauseWorkout()
            }
            ACTION_RESUME -> {
                sessionManager?.resumeWorkout()
            }
            ACTION_UPDATE -> {
                val tempo = intent.getStringExtra(EXTRA_TEMPO) ?: "20:00"
                val repAtual = intent.getIntExtra(EXTRA_REP_ATUAL, 0)
                val totalReps = intent.getIntExtra(EXTRA_TOTAL_REPS, 0)
                val tipo = intent.getStringExtra(EXTRA_TIPO) ?: "Burpee"
                val isPaused = intent.getBooleanExtra(EXTRA_PAUSED, false)

                val notificacao = criarNotificacao(tempo, repAtual, totalReps, tipo, isPaused)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notificacao)
            }
            ACTION_START -> {
                val notificacao = criarNotificacao("20:00", 0, 0, "Burpee Log", false)
                startForeground(NOTIFICATION_ID, notificacao)
            }
        }

        return START_STICKY
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Treino Ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente durante o treino de 20 minutos"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun criarNotificacao(
        tempoRestante: String,
        repAtual: Int,
        totalReps: Int,
        tipo: String,
        isPaused: Boolean
    ): Notification {
        val intentAbreApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentAbre = PendingIntent.getActivity(
            this,
            0,
            intentAbreApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intentPausaRetoma = Intent(this, WorkoutTimerService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingIntentPausaRetoma = PendingIntent.getService(
            this,
            1,
            intentPausaRetoma,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intentEncerrar = Intent(this, WorkoutTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingIntentEncerrar = PendingIntent.getService(
            this,
            2,
            intentEncerrar,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titulo = if (isPaused) "⏸ Treino Pausado • $tipo" else "🔥 Treino Ativo • $tipo"
        val texto = "$tempoRestante restantes • Rep $repAtual de $totalReps"

        val actionTexto = if (isPaused) "Retomar" else "Pausar"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntentAbre)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .addAction(0, actionTexto, pendingIntentPausaRetoma)
            .addAction(0, "Encerrar", pendingIntentEncerrar)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun pararForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        pararForeground()
    }

    companion object {
        const val CHANNEL_ID = "burpee_workout_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.burpeelog.ACTION_START"
        const val ACTION_PAUSE = "com.example.burpeelog.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.burpeelog.ACTION_RESUME"
        const val ACTION_STOP = "com.example.burpeelog.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.burpeelog.ACTION_UPDATE"

        const val EXTRA_TEMPO = "extra_tempo"
        const val EXTRA_REP_ATUAL = "extra_rep_atual"
        const val EXTRA_TOTAL_REPS = "extra_total_reps"
        const val EXTRA_TIPO = "extra_tipo"
        const val EXTRA_PAUSED = "extra_paused"

        fun startService(context: Context) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateNotification(
            context: Context,
            tempoRestante: String,
            repAtual: Int,
            totalReps: Int,
            tipo: String,
            isPaused: Boolean
        ) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TEMPO, tempoRestante)
                putExtra(EXTRA_REP_ATUAL, repAtual)
                putExtra(EXTRA_TOTAL_REPS, totalReps)
                putExtra(EXTRA_TIPO, tipo)
                putExtra(EXTRA_PAUSED, isPaused)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WorkoutTimerService::class.java)
            context.stopService(intent)
        }
    }
}
