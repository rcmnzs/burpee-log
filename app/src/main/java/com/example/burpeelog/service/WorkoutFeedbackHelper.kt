package com.example.burpeelog.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gerenciador de bipe e vibração para os avisos do treino.
 * - Bipe curto e vibração em cada repetição.
 * - Aviso mais discreto no início de cada minuto.
 * - Bipes de contagem regressiva inicial.
 */
class WorkoutFeedbackHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        } catch (_: Exception) {
            // Em caso de restrição do sistema, continua sem falhar
        }
    }

    /**
     * Bipe curto e vibração marcante ao iniciar cada repetição.
     */
    fun playRepCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(140)
            }
        } catch (_: Exception) {}
    }

    /**
     * Aviso sonoro e tátil mais discreto para a virada de cada minuto.
     */
    fun playMinuteCue() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Padrão sutil: vibra 40ms, pausa 60ms, vibra 40ms
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 40, 60, 40), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Bipe da contagem regressiva inicial (5s).
     */
    fun playCountdownBeep(isFinal: Boolean) {
        try {
            if (isFinal) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 220)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
            }
        } catch (_: Exception) {}

        try {
            val duration = if (isFinal) 200L else 70L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
        toneGenerator = null
    }
}
