package com.example.burpeelog.domain

import com.example.burpeelog.data.model.Sessao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class BackupFileInfo(
    val name: String,
    val lastModified: Long = 0L,
    val id: Any? = null
)

object AutoBackupCalculator {

    const val MAX_BACKUPS_TO_KEEP = 5
    const val FILE_PREFIX = "burpeelog-backup-"
    const val FILE_SUFFIX = ".json"

    val BACKUP_FILE_REGEX = Regex("""^burpeelog-backup-(\d{4}-\d{2}-\d{2})\.json$""")

    /**
     * Determines whether a backup is overdue.
     * Pure function receiving lastBackupDate, currentDate, frequency, and whether there are changes.
     *
     * Rules:
     * - If there are NO changes since the last backup (hasChanges == false): NEVER overdue.
     * - If no backup has ever been taken (lastBackupDate == null): overdue if hasChanges is true.
     * - Weekly frequency: overdue if at least 7 days have passed since lastBackupDate.
     * - Monthly frequency: overdue if at least 1 month has passed since lastBackupDate.
     */
    fun isBackupOverdue(
        lastBackupDate: LocalDate?,
        currentDate: LocalDate,
        frequency: BackupFrequency,
        hasChanges: Boolean
    ): Boolean {
        if (!hasChanges) return false
        if (lastBackupDate == null) return true

        return when (frequency) {
            BackupFrequency.WEEKLY -> {
                val nextDate = lastBackupDate.plusDays(7)
                !currentDate.isBefore(nextDate)
            }
            BackupFrequency.MONTHLY -> {
                val nextDate = lastBackupDate.plusMonths(1)
                !currentDate.isBefore(nextDate)
            }
        }
    }

    /**
     * Determines if a reminder notification can be shown today.
     * Pure function enforcing:
     * - Never if not overdue.
     * - At most one reminder notification per calendar day.
     */
    fun canSendReminderToday(
        lastReminderDate: LocalDate?,
        currentDate: LocalDate,
        isOverdue: Boolean
    ): Boolean {
        if (!isOverdue) return false
        if (lastReminderDate == null) return true
        return lastReminderDate.isBefore(currentDate)
    }

    /**
     * Checks if a filename matches strictly: burpeelog-backup-YYYY-MM-DD.json.
     */
    fun isBackupFileName(fileName: String?): Boolean {
        if (fileName.isNullOrBlank()) return false
        val match = BACKUP_FILE_REGEX.matchEntire(fileName) ?: return false
        val dateStr = match.groupValues[1]
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    /**
     * Extracts LocalDate from burpeelog-backup-YYYY-MM-DD.json.
     */
    fun extractDateFromBackupFileName(fileName: String?): LocalDate? {
        if (fileName.isNullOrBlank()) return null
        val match = BACKUP_FILE_REGEX.matchEntire(fileName) ?: return null
        val dateStr = match.groupValues[1]
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * Generates standard file name for the given date: burpeelog-backup-YYYY-MM-DD.json.
     */
    fun generateBackupFileName(date: LocalDate = LocalDate.now()): String {
        return "$FILE_PREFIX${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}$FILE_SUFFIX"
    }

    /**
     * Identifies which files to delete during rotation to keep at most [maxToKeep] files.
     * CRITICAL: Strictly ignores any files that do NOT match burpeelog-backup-*.json.
     * Sorts descending by date (newest first).
     * Returns the list of files that exceed [maxToKeep] (oldest).
     */
    fun findFilesToDeleteOnRotation(
        allFiles: List<BackupFileInfo>,
        maxToKeep: Int = MAX_BACKUPS_TO_KEEP
    ): List<BackupFileInfo> {
        val validBackups = allFiles
            .filter { isBackupFileName(it.name) }
            .sortedWith(
                compareByDescending<BackupFileInfo> {
                    extractDateFromBackupFileName(it.name) ?: LocalDate.MIN
                }.thenByDescending { it.lastModified }
            )

        return if (validBackups.size > maxToKeep) {
            validBackups.drop(maxToKeep)
        } else {
            emptyList()
        }
    }

    /**
     * Computes a deterministic signature of the session collection.
     * Allows detecting if any session was added, edited, or removed.
     */
    fun computeDatabaseSignature(sessoes: List<Sessao>): Long {
        if (sessoes.isEmpty()) return 0L
        var hash = 1125899906842597L // prime multiplier
        for (s in sessoes) {
            hash = 31 * hash + s.id
            hash = 31 * hash + s.dataHora
            hash = 31 * hash + s.tipo.hashCode()
            hash = 31 * hash + s.duracaoSegundos
            hash = 31 * hash + s.repsFeitas
            hash = 31 * hash + (s.repsPrevistas ?: 0)
            hash = 31 * hash + (s.padraoIntervalosSegundos?.hashCode() ?: 0)
            hash = 31 * hash + (s.observacao?.hashCode() ?: 0)
            hash = 31 * hash + if (s.importada) 1 else 0
        }
        return hash
    }
}
