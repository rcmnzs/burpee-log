package com.example.burpeelog.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.burpeelog.domain.AutoBackupManager
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AutoBackupManager.verificarEExecutar(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.success() // Do not crash or continuously retry in background
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "burpeelog_daily_auto_backup"

        fun scheduleDailyCheck(context: Context) {
            val dailyWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        }
    }
}
