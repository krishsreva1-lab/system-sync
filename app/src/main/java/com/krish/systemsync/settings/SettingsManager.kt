package com.krish.systemsync.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { LIGHT, DARK, AMOLED }

enum class AppLogo(val label: String) {
    SHIELD("Shield"),
    TERMINAL("Terminal"),
    LOCK("Lock"),
    FOLDER("Folder"),
    SYSTEM("System")
}

class SettingsManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_NAME = stringPreferencesKey("app_name")
        val APP_LOGO = stringPreferencesKey("app_logo")
        val CUSTOM_LOGO_URI = stringPreferencesKey("custom_logo_uri")
        val SHAKE_TO_LOCK = booleanPreferencesKey("shake_to_lock")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val SHOW_WARNING_SCREEN = booleanPreferencesKey("show_warning_screen")
        val USE_BIOMETRIC = booleanPreferencesKey("use_biometric")
        val BIOMETRIC_FILE_ACCESS = booleanPreferencesKey("biometric_file_access")
        val CLEAR_CLIPBOARD_TIMEOUT = intPreferencesKey("clear_clipboard_timeout")
        val ACTIVE_ALIAS = stringPreferencesKey("active_alias")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.LIGHT.name)
    }

    val appNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_NAME] ?: "System SYNC"
    }

    val appLogoFlow: Flow<AppLogo> = context.dataStore.data.map { preferences ->
        AppLogo.valueOf(preferences[PreferencesKeys.APP_LOGO] ?: AppLogo.SHIELD.name)
    }

    val customLogoUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_LOGO_URI]
    }

    val shakeToLockFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHAKE_TO_LOCK] ?: false
    }

    val screenshotProtectionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: true
    }

    val showWarningScreenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_WARNING_SCREEN] ?: true
    }

    val useBiometricFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_BIOMETRIC] ?: false
    }

    val biometricFileAccessFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIOMETRIC_FILE_ACCESS] ?: false
    }

    val clearClipboardTimeoutFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CLEAR_CLIPBOARD_TIMEOUT] ?: 30 // seconds
    }

    val activeAliasFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_ALIAS] ?: "com.krish.systemsync.LauncherDefault"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAppName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_NAME] = name
        }
    }

    suspend fun setAppLogo(logo: AppLogo) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOGO] = logo.name
        }
    }

    suspend fun setCustomLogoUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PreferencesKeys.CUSTOM_LOGO_URI)
            } else {
                preferences[PreferencesKeys.CUSTOM_LOGO_URI] = uri
            }
        }
    }

    suspend fun setShakeToLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHAKE_TO_LOCK] = enabled
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setShowWarningScreen(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_WARNING_SCREEN] = enabled
        }
    }

    suspend fun setUseBiometric(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIOMETRIC] = enabled
        }
    }

    suspend fun setBiometricFileAccess(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_FILE_ACCESS] = enabled
        }
    }

    suspend fun setClearClipboardTimeout(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLEAR_CLIPBOARD_TIMEOUT] = seconds
        }
    }

    suspend fun setActiveAlias(aliasClassName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_ALIAS] = aliasClassName
        }
    }
}
