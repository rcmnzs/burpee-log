package com.example.burpeelog.domain

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.BuildConfig
import com.example.R
import com.example.burpeelog.BurpeeLogApplication
import com.example.burpeelog.data.model.Sessao
import com.example.burpeelog.service.BackupNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class BackupExecutionResult {
    data class Success(val fileName: String, val sessionsCount: Int) : BackupExecutionResult()
    data class OverdueChecked(val executed: Boolean, val reason: String) : BackupExecutionResult()
    data class Error(val message: String, val cause: Throwable? = null) : BackupExecutionResult()
}

object AutoBackupManager {

    /**
     * Executes a backup directly into the chosen folder URI.
     * Overwrites any existing file for the current day.
     * Enforces rotation keeping only the 5 most recent burpeelog-backup-*.json files.
     */
    suspend fun executarBackupNaPasta(
        context: Context,
        folderUri: Uri,
        sessoes: List<Sessao>,
        appVersion: String = BuildConfig.VERSION_NAME,
        date: LocalDate = LocalDate.now()
    ): BackupExecutionResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext as BurpeeLogApplication
        val prefs = app.autoBackupPreferences

        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
            if (folder == null || !folder.exists() || !folder.canWrite()) {
                val errorMsg = context.getString(R.string.backup_folder_error_banner)
                prefs.recordBackupError(errorMsg)
                return@withContext BackupExecutionResult.Error(errorMsg)
            }

            val fileName = AutoBackupCalculator.generateBackupFileName(date)

            // Overwrite if exists on the same day: remove prior instance first so createFile won't make "name (1).json"
            val existingFile = folder.findFile(fileName)
            if (existingFile != null && existingFile.exists()) {
                existingFile.delete()
            }

            val targetFile = folder.createFile("application/json", fileName)
                ?: throw IOException("Failed to create document file: $fileName")

            val jsonContent = BackupManager.gerarBackupJson(
                sessoes = sessoes,
                appVersion = appVersion
            )

            context.contentResolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: throw IOException("Could not open output stream for: ${targetFile.uri}")

            // Enforce rotation: only delete matching burpeelog-backup-*.json
            executarRotacao(folder)

            val nowMillis = System.currentTimeMillis()
            val signature = AutoBackupCalculator.computeDatabaseSignature(sessoes)
            prefs.recordBackupSuccess(timestamp = nowMillis, signature = signature)

            BackupExecutionResult.Success(fileName = fileName, sessionsCount = sessoes.size)
        } catch (e: Exception) {
            val errorMsg = context.getString(R.string.backup_folder_error_banner)
            prefs.recordBackupError(errorMsg)
            BackupExecutionResult.Error(message = errorMsg, cause = e)
        }
    }

    /**
     * Performs rotation on the folder, deleting older matching backup files beyond the limit.
     */
    fun executarRotacao(folder: DocumentFile, maxToKeep: Int = AutoBackupCalculator.MAX_BACKUPS_TO_KEEP): Int {
        val childFiles = folder.listFiles()
        val fileInfos = childFiles.map { doc ->
            BackupFileInfo(
                name = doc.name.orEmpty(),
                lastModified = doc.lastModified(),
                id = doc
            )
        }

        val toDelete = AutoBackupCalculator.findFilesToDeleteOnRotation(fileInfos, maxToKeep)
        var deletedCount = 0
        for (item in toDelete) {
            val doc = item.id as? DocumentFile
            if (doc != null && doc.exists()) {
                if (doc.delete()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    /**
     * Checks if backup is overdue and runs backup or reminder as needed.
     * Safe to call from WorkManager or App Startup.
     */
    suspend fun verificarEExecutar(context: Context): BackupExecutionResult = withContext(Dispatchers.IO) {
        val app = context.applicationContext as BurpeeLogApplication
        val prefs = app.autoBackupPreferences
        val repository = app.repository

        val state = prefs.stateFlow.first()
        val sessoes = repository.allSessoes.first()
        val today = LocalDate.now()

        val lastBackupDate = if (state.lastBackupTimestamp > 0L) {
            Instant.ofEpochMilli(state.lastBackupTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else {
            null
        }

        val currentSignature = AutoBackupCalculator.computeDatabaseSignature(sessoes)
        val hasChanges = if (state.lastBackupTimestamp == 0L) {
            sessoes.isNotEmpty()
        } else {
            currentSignature != state.lastBackupSignature
        }

        val isOverdue = AutoBackupCalculator.isBackupOverdue(
            lastBackupDate = lastBackupDate,
            currentDate = today,
            frequency = state.frequency,
            hasChanges = hasChanges
        )

        if (!isOverdue) {
            return@withContext BackupExecutionResult.OverdueChecked(
                executed = false,
                reason = "Backup is not overdue (or no changes detected)."
            )
        }

        val lastReminderDate = state.lastReminderDate?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                null
            }
        }

        // Case 1: Auto backup is enabled and folder is configured
        if (state.autoBackupEnabled && !state.folderUri.isNullOrBlank()) {
            val folderUri = Uri.parse(state.folderUri)
            val result = executarBackupNaPasta(
                context = context,
                folderUri = folderUri,
                sessoes = sessoes,
                date = today
            )

            if (result is BackupExecutionResult.Success) {
                return@withContext result
            }

            // If auto-backup failed, trigger reminder notification if enabled and allowed today
            if (state.reminderEnabled && AutoBackupCalculator.canSendReminderToday(lastReminderDate, today, isOverdue = true)) {
                BackupNotificationHelper.showBackupReminderNotification(context)
                prefs.recordReminderSent(today)
            }
            return@withContext result
        }

        // Case 2: Auto backup is not active, but reminder is enabled
        if (state.reminderEnabled) {
            if (AutoBackupCalculator.canSendReminderToday(lastReminderDate, today, isOverdue = true)) {
                BackupNotificationHelper.showBackupReminderNotification(context)
                prefs.recordReminderSent(today)
                return@withContext BackupExecutionResult.OverdueChecked(
                    executed = true,
                    reason = "Reminder notification sent."
                )
            }
        }

        BackupExecutionResult.OverdueChecked(
            executed = false,
            reason = "Overdue, but neither auto-backup nor reminder was pending."
        )
    }
}
