package com.krish.systemsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.ComponentName
import android.content.pm.PackageManager
import com.krish.systemsync.monitor.SystemMonitor
import com.krish.systemsync.monitor.SystemStats
import com.krish.systemsync.security.SecurityManager
import com.krish.systemsync.security.SecurityPrefs
import com.krish.systemsync.settings.AppLogo
import com.krish.systemsync.settings.SettingsManager
import com.krish.systemsync.settings.ThemeMode
import com.krish.systemsync.settings.AppAlias
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val securityPrefs = SecurityPrefs(application)
    private val securityManager = SecurityManager(application)
    private val systemMonitor = SystemMonitor(application)
    private val settingsManager = SettingsManager(application)

    val isSetupComplete = securityPrefs.isSetupComplete.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val systemStats = systemMonitor.getSystemStatsFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 
        SystemStats(0, 0, 0, 0, 0f, false, 0, 0)
    )

    private val _isDummyMode = MutableStateFlow(false)
    val isDummyMode = _isDummyMode.asStateFlow()

    private val _lockedAppPackage = MutableStateFlow<String?>(null)
    val lockedAppPackage = _lockedAppPackage.asStateFlow()

    fun setLockedApp(packageName: String?) {
        _lockedAppPackage.value = packageName
    }

    // Settings
    val themeMode = settingsManager.themeModeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.LIGHT)
    val appName = settingsManager.appNameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System SYNC")
    val appLogo = settingsManager.appLogoFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLogo.SHIELD)
    val customLogoUri = settingsManager.customLogoUriFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val shakeToLock = settingsManager.shakeToLockFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val screenshotProtection = settingsManager.screenshotProtectionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val showWarningScreen = settingsManager.showWarningScreenFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useBiometric = settingsManager.useBiometricFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val biometricFileAccess = settingsManager.biometricFileAccessFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val activeAlias = settingsManager.activeAliasFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "com.krish.systemsync.LauncherDefault")

    suspend fun authenticate(password: String): Boolean {
        val mainHash = securityPrefs.mainPasswordHash.first()
        val dummyHash = securityPrefs.dummyPasswordHash.first()
        
        return when {
            mainHash != null && securityManager.verifyPassword(password, mainHash) -> {
                _isDummyMode.value = false
                true
            }
            dummyHash != null && securityManager.verifyPassword(password, dummyHash) -> {
                _isDummyMode.value = true
                true
            }
            else -> false
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        return authenticate(password)
    }

    fun saveMainPassword(password: String) {
        viewModelScope.launch {
            val hash = securityManager.hashPassword(password)
            securityPrefs.saveMainPassword(hash)
        }
    }

    fun saveDummyPassword(password: String) {
        viewModelScope.launch {
            val hash = securityManager.hashPassword(password)
            securityPrefs.saveDummyPassword(hash)
        }
    }

    fun saveRecoveryKey(key: String) {
        viewModelScope.launch {
            val hash = securityManager.hashPassword(key)
            securityPrefs.saveRecoveryKey(hash)
            securityPrefs.setSetupComplete(true)
        }
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsManager.setThemeMode(mode) }
    fun setAppName(name: String) = viewModelScope.launch { settingsManager.setAppName(name) }
    fun setAppLogo(logo: AppLogo) = viewModelScope.launch { settingsManager.setAppLogo(logo) }
    fun setCustomLogoUri(uri: String?) = viewModelScope.launch { settingsManager.setCustomLogoUri(uri) }
    fun setShakeToLock(enabled: Boolean) = viewModelScope.launch { settingsManager.setShakeToLock(enabled) }
    fun setScreenshotProtection(enabled: Boolean) = viewModelScope.launch { settingsManager.setScreenshotProtection(enabled) }
    fun setShowWarningScreen(enabled: Boolean) = viewModelScope.launch { settingsManager.setShowWarningScreen(enabled) }
    fun setUseBiometric(enabled: Boolean) = viewModelScope.launch { settingsManager.setUseBiometric(enabled) }
    fun setBiometricFileAccess(enabled: Boolean) = viewModelScope.launch { settingsManager.setBiometricFileAccess(enabled) }

    fun updateActiveAlias(newAlias: AppAlias) {
        viewModelScope.launch {
            settingsManager.setActiveAlias(newAlias.className)
            
            val context = getApplication<Application>()
            val pm = context.packageManager
            
            // Also update the internal app name to match the disguise for full camouflage
            val aliasName = context.getString(newAlias.labelRes)
            settingsManager.setAppName(aliasName)
            
            AppAlias.values().forEach { alias ->
                val state = if (alias == newAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                
                pm.setComponentEnabledSetting(
                    ComponentName(context, alias.className),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
