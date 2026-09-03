package com.krish.systemsync.applock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.krish.systemsync.security.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLockRepository(private val context: Context) {
    companion object {
        private val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
        private val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        private val LOCK_WHEN_SCREEN_OFF = booleanPreferencesKey("lock_when_screen_off")
        private val STEALTH_MODE = booleanPreferencesKey("stealth_mode")
    }

    val lockedApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[LOCKED_APPS] ?: emptySet()
    }

    val lockTimeout: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LOCK_TIMEOUT] ?: "Immediately"
    }

    val lockWhenScreenOff: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LOCK_WHEN_SCREEN_OFF] ?: true
    }

    val stealthMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STEALTH_MODE] ?: false
    }

    suspend fun setLockTimeout(timeout: String) {
        context.dataStore.edit { preferences ->
            preferences[LOCK_TIMEOUT] = timeout
        }
    }

    suspend fun setLockWhenScreenOff(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCK_WHEN_SCREEN_OFF] = enabled
        }
    }

    suspend fun setStealthMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STEALTH_MODE] = enabled
        }
    }

    suspend fun toggleAppLock(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LOCKED_APPS] ?: emptySet()
            if (current.contains(packageName)) {
                preferences[LOCKED_APPS] = current - packageName
            } else {
                preferences[LOCKED_APPS] = current + packageName
            }
        }
    }
}
