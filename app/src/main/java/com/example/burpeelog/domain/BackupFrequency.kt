package com.example.burpeelog.domain

import androidx.annotation.StringRes
import com.example.R

enum class BackupFrequency(@StringRes val labelRes: Int) {
    WEEKLY(R.string.backup_freq_weekly),
    MONTHLY(R.string.backup_freq_monthly);

    companion object {
        fun fromName(name: String?): BackupFrequency {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MONTHLY
        }
    }
}
