package com.krish.systemsync.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey

@Serializable
object Welcome : Screen

@Serializable
object MainPasswordSetup : Screen

@Serializable
object DummyPasswordSetup : Screen

@Serializable
object RecoverySetup : Screen

@Serializable
object Dashboard : Screen

@Serializable
object Login : Screen

@Serializable
object Vault : Screen

@Serializable
object AppLock : Screen

@Serializable
object Terminal : Screen

@Serializable
object Customization : Screen

@Serializable
object Notes : Screen

@Serializable
object Trash : Screen

@Serializable
data class Player(val startIndex: Int) : Screen

@Serializable
object Logs : Screen

@Serializable
object Warning : Screen
