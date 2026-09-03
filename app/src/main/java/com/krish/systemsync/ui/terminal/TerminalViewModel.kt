package com.krish.systemsync.ui.terminal

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krish.systemsync.applock.AppLockRepository
import com.krish.systemsync.monitor.SystemMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val systemMonitor = SystemMonitor(application)
    private val appLockRepository = AppLockRepository(application)
    private val packageManager = application.packageManager

    private val _output = MutableStateFlow<List<String>>(listOf("System SYNC Terminal v1.0.0", "Type 'help' for commands."))
    val output = _output.asStateFlow()

    private val _history = mutableListOf<String>()
    private var historyIndex = -1

    fun executeCommand(commandLine: String) {
        if (commandLine.isBlank()) return
        
        val args = commandLine.trim().split("\\s+".toRegex())
        val command = args[0].lowercase()
        
        _output.value = _output.value + "> $commandLine"
        _history.add(commandLine)
        historyIndex = _history.size

        when (command) {
            "help" -> showHelp()
            "status" -> showStatus()
            "battery" -> showBattery()
            "cpu" -> showCpu()
            "ram" -> showRam()
            "storage" -> showStorage()
            "gpu" -> _output.value = _output.value + "GPU Info: Adreno (TM) 740 (Simulated)"
            "clear" -> _output.value = emptyList()
            "about" -> _output.value = _output.value + listOf(
                "System SYNC - Advanced Security & Monitoring Tool",
                "Version: 1.0.0",
                "Developer: Krish",
                "Built with Jetpack Compose & Navigation 3"
            )
            "vault" -> handleVaultCommand(args.drop(1))
            "apps" -> handleAppsCommand(args.drop(1))
            else -> _output.value = _output.value + "Unknown command: $command"
        }
    }

    private fun showHelp() {
        _output.value = _output.value + listOf(
            "Available commands:",
            "  help            - Show this list",
            "  status          - System overview",
            "  battery         - Battery info",
            "  cpu             - CPU usage",
            "  ram             - Memory stats",
            "  storage         - Storage stats",
            "  vault open      - Navigate to vault",
            "  vault close     - Lock vault",
            "  apps            - List all apps",
            "  apps locked     - List locked apps",
            "  clear           - Clear terminal",
            "  about           - Version info"
        )
    }

    private fun showStatus() = viewModelScope.launch {
        val stats = systemMonitor.getSystemStatsFlow().first()
        _output.value = _output.value + listOf(
            "--- SYSTEM STATUS ---",
            "CPU Usage: ${stats.cpuUsage}%",
            "RAM: ${formatSize(stats.ramUsed)} / ${formatSize(stats.ramTotal)}",
            "Battery: ${stats.batteryLevel}% (${if (stats.isCharging) "Charging" else "Discharging"})",
            "Storage: ${formatSize(stats.storageUsed)} / ${formatSize(stats.storageTotal)}",
            "Vault: ${if (isVaultOpen()) "OPEN" else "LOCKED"}"
        )
    }

    private fun showBattery() = viewModelScope.launch {
        val stats = systemMonitor.getSystemStatsFlow().first()
        _output.value = _output.value + "Battery: ${stats.batteryLevel}% | Temp: ${stats.batteryTemp}°C | Charging: ${stats.isCharging}"
    }

    private fun showCpu() = viewModelScope.launch {
        val stats = systemMonitor.getSystemStatsFlow().first()
        _output.value = _output.value + "CPU Usage: ${stats.cpuUsage}%"
    }

    private fun showRam() = viewModelScope.launch {
        val stats = systemMonitor.getSystemStatsFlow().first()
        _output.value = _output.value + "RAM Usage: ${formatSize(stats.ramUsed)} / ${formatSize(stats.ramTotal)}"
    }

    private fun showStorage() = viewModelScope.launch {
        val stats = systemMonitor.getSystemStatsFlow().first()
        _output.value = _output.value + "Storage: ${formatSize(stats.storageUsed)} / ${formatSize(stats.storageTotal)}"
    }

    private fun handleVaultCommand(args: List<String>) {
        if (args.isEmpty()) {
            _output.value = _output.value + "Usage: vault <open|close>"
            return
        }
        when (args[0].lowercase()) {
            "open" -> _output.value = _output.value + "Navigating to Vault... (Requires Auth)"
            "close" -> _output.value = _output.value + "Vault closed and locked."
            else -> _output.value = _output.value + "Unknown vault sub-command: ${args[0]}"
        }
    }

    private fun handleAppsCommand(args: List<String>) = viewModelScope.launch {
        if (args.isEmpty()) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val appNames = apps.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { packageManager.getApplicationLabel(it).toString() }
            _output.value = _output.value + "Installed User Apps:" + appNames.take(20) + (if (appNames.size > 20) "...and ${appNames.size - 20} more" else "")
            return@launch
        }
        
        if (args[0].lowercase() == "locked") {
            val lockedPackages = appLockRepository.lockedApps.first()
            if (lockedPackages.isEmpty()) {
                _output.value = _output.value + "No apps are currently locked."
            } else {
                val lockedNames = lockedPackages.map { pkg ->
                    try {
                        val info = packageManager.getApplicationInfo(pkg, 0)
                        packageManager.getApplicationLabel(info).toString()
                    } catch (e: Exception) { pkg }
                }
                _output.value = _output.value + "Locked Apps:" + lockedNames
            }
        }
    }

    private fun isVaultOpen(): Boolean = false // Placeholder

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        val gb = mb / 1024
        return when {
            gb > 0 -> "${gb}GB"
            mb > 0 -> "${mb}MB"
            kb > 0 -> "${kb}KB"
            else -> "${bytes}B"
        }
    }

    fun getHistoryUp(): String? {
        if (historyIndex > 0) {
            historyIndex--
            return _history[historyIndex]
        }
        return null
    }

    fun getHistoryDown(): String? {
        if (historyIndex < _history.size - 1) {
            historyIndex++
            return _history[historyIndex]
        } else if (historyIndex == _history.size - 1) {
            historyIndex = _history.size
            return ""
        }
        return null
    }
}
