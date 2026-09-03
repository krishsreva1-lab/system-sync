package com.krish.systemsync.security

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class LogType {
    AUTH_SUCCESS, AUTH_FAIL, VAULT_ACCESS, SETTINGS_CHANGE, EMERGENCY_LOCK
}

data class SecurityLog(
    val type: LogType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SecurityLogger(private val context: Context) {
    private val logFile = File(context.filesDir, "security_logs.txt")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun log(type: LogType, message: String) {
        val logEntry = "${dateFormat.format(Date())} | ${type.name} | $message\n"
        logFile.appendText(logEntry)
    }

    fun getLogs(): List<SecurityLog> {
        if (!logFile.exists()) return emptyList()
        return logFile.readLines().mapNotNull { line ->
            val parts = line.split(" | ")
            if (parts.size == 3) {
                try {
                    val date = dateFormat.parse(parts[0])
                    SecurityLog(
                        type = LogType.valueOf(parts[1]),
                        message = parts[2],
                        timestamp = date?.time ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }.reversed()
    }

    fun clearLogs() {
        logFile.delete()
    }
}
