package com.krish.systemsync.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

class SecurityPrefs(private val context: Context) {

    companion object {
        private val MAIN_PASSWORD_HASH = stringPreferencesKey("main_password_hash")
        private val DUMMY_PASSWORD_HASH = stringPreferencesKey("dummy_password_hash")
        private val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        private val RECOVERY_KEY_HASH = stringPreferencesKey("recovery_key_hash")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SETUP_COMPLETE] ?: false
    }

    val mainPasswordHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MAIN_PASSWORD_HASH]
    }

    val dummyPasswordHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DUMMY_PASSWORD_HASH]
    }

    suspend fun saveMainPassword(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[MAIN_PASSWORD_HASH] = hash
        }
    }

    suspend fun saveDummyPassword(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[DUMMY_PASSWORD_HASH] = hash
        }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SETUP_COMPLETE] = complete
        }
    }

    suspend fun saveRecoveryKey(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[RECOVERY_KEY_HASH] = hash
        }
    }
}
