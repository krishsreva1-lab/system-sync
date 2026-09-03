package com.krish.systemsync.security

import kotlinx.coroutines.flow.firstOrNull

class DummyVaultManager(
    private val securityManager: SecurityManager,
    private val securityPrefs: SecurityPrefs
) {
    sealed class AuthResult {
        object SuccessMain : AuthResult()
        object SuccessDummy : AuthResult()
        object Failure : AuthResult()
    }

    suspend fun authenticate(password: String): AuthResult {
        val mainHash = securityPrefs.mainPasswordHash.firstOrNull()
        val dummyHash = securityPrefs.dummyPasswordHash.firstOrNull()

        if (mainHash != null && securityManager.verifyPassword(password, mainHash)) {
            return AuthResult.SuccessMain
        }

        if (dummyHash != null && securityManager.verifyPassword(password, dummyHash)) {
            return AuthResult.SuccessDummy
        }

        return AuthResult.Failure
    }
}
