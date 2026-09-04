package com.krish.systemsync

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.krish.systemsync.applock.AppLockRepository
import com.krish.systemsync.navigation.*
import com.krish.systemsync.security.EmergencyLockManager
import com.krish.systemsync.security.SecurityManager
import com.krish.systemsync.settings.AppAlias
import com.krish.systemsync.ui.applock.AppLockScreen
import com.krish.systemsync.ui.dashboard.DashboardScreen
import com.krish.systemsync.ui.login.TerminalLoginScreen
import com.krish.systemsync.ui.player.PlayerScreen
import com.krish.systemsync.ui.settings.CustomizationScreen
import com.krish.systemsync.ui.settings.AboutScreen
import com.krish.systemsync.ui.settings.UpdateScreen
import com.krish.systemsync.ui.settings.LogsScreen
import com.krish.systemsync.ui.setup.*
import com.krish.systemsync.ui.terminal.TerminalScreen
import com.krish.systemsync.ui.terminal.TerminalViewModel
import com.krish.systemsync.ui.theme.SystemSYNCTheme
import com.krish.systemsync.ui.vault.NotesScreen
import com.krish.systemsync.ui.vault.TrashScreen
import com.krish.systemsync.ui.vault.VaultScreen
import com.krish.systemsync.vault.NotesRepository
import com.krish.systemsync.vault.VaultViewModel
import com.krish.systemsync.ui.components.glassmorphism
import com.krish.systemsync.ui.components.GlassSurface

class MainActivity : FragmentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private var emergencyLockManager: EmergencyLockManager? = null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val lockPackage = intent.getStringExtra("LOCK_APP_PACKAGE")
        if (lockPackage != null) {
            viewModel.setLockedApp(lockPackage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val lockPackage = intent.getStringExtra("LOCK_APP_PACKAGE")
        if (lockPackage != null) {
            viewModel.setLockedApp(lockPackage)
        }

        // Start App Lock Service safely as a regular background service
        try {
            val intent = android.content.Intent(this, com.krish.systemsync.applock.AppLockService::class.java)
            startService(intent)
        } catch (_: Exception) {}

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val appName by viewModel.appName.collectAsState()
            val appLogo by viewModel.appLogo.collectAsState()
            val customLogoUri by viewModel.customLogoUri.collectAsState()
            val shakeToLock by viewModel.shakeToLock.collectAsState()
            val screenshotProtection by viewModel.screenshotProtection.collectAsState()
            val showWarningScreen by viewModel.showWarningScreen.collectAsState()
            val useBiometric by viewModel.useBiometric.collectAsState()
            val biometricFileAccess by viewModel.biometricFileAccess.collectAsState()
            val activeAlias by viewModel.activeAlias.collectAsState()
            val lockedAppPackage by viewModel.lockedAppPackage.collectAsState()

            // Screenshot Protection
            LaunchedEffect(screenshotProtection) {
                if (screenshotProtection) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            SystemSYNCTheme(
                themeMode = themeMode
            ) {
                val isSetupComplete by viewModel.isSetupComplete.collectAsState()
                val systemStats by viewModel.systemStats.collectAsState()
                val isDummyMode by viewModel.isDummyMode.collectAsState()

                val vaultViewModel: VaultViewModel = viewModel(
                    key = "vault_$isDummyMode",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return VaultViewModel(application, isDummyMode) as T
                        }
                    }
                )

                if (isSetupComplete == null) return@SystemSYNCTheme

                val backStack = rememberNavBackStack(
                    if (isSetupComplete == true) {
                        if (showWarningScreen) Warning else Dashboard
                    } else Welcome
                )

                DisposableEffect(shakeToLock) {
                    if (shakeToLock) {
                        emergencyLockManager = EmergencyLockManager(this@MainActivity) {
                            backStack.clear()
                            backStack.add(if (showWarningScreen) Warning else Dashboard)
                        }
                        emergencyLockManager?.startListening()
                    }
                    onDispose {
                        emergencyLockManager?.stopListening()
                        emergencyLockManager = null
                    }
                }

                val currentRoute = backStack.lastOrNull()
                val showBottomBar = currentRoute in listOf(Vault, AppLock, if (isDummyMode) null else Customization).filterNotNull()

                Scaffold(
                    bottomBar = {
                        if (showBottomBar && lockedAppPackage == null) {
                            CustomBottomNavigation(
                                currentRoute = (currentRoute as? Screen) ?: Welcome,
                                isDummyMode = isDummyMode,
                                onNavigate = { route ->
                                    if (currentRoute == route && route == Vault) {
                                        vaultViewModel.requestReset()
                                    }
                                    if (currentRoute != route) {
                                        backStack.removeAt(backStack.size - 1)
                                        backStack.add(route)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.padding(
                            bottom = if (showBottomBar && lockedAppPackage == null) innerPadding.calculateBottomPadding() else 0.dp
                        ),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (lockedAppPackage != null) {
                            val stealthMode by AppLockRepository(application).stealthMode.collectAsState(initial = false)
                            AppLockOverlay(
                                packageName = lockedAppPackage!!,
                                vaultName = appName,
                                stealthMode = stealthMode,
                                onUnlock = { viewModel.setLockedApp(null) },
                                onVerify = { viewModel.verifyPassword(it) },
                                useBiometric = useBiometric
                            )
                        } else {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                            ) { key ->
                                when (key) {
                                    is Welcome -> NavEntry(key) {
                                        WelcomeScreen(onGetStarted = { backStack.add(MainPasswordSetup) })
                                    }
                                    is MainPasswordSetup -> NavEntry(key) {
                                        PasswordSetupScreen(
                                            title = "Main Password",
                                            description = if (isSetupComplete == true) "Update your primary vault password" else "Create a secure password for your primary vault",
                                            onPasswordSet = {
                                                viewModel.saveMainPassword(it)
                                                if (isSetupComplete == true) {
                                                    backStack.removeAt(backStack.size - 1)
                                                } else {
                                                    backStack.add(DummyPasswordSetup)
                                                }
                                            }
                                        )
                                    }
                                    is DummyPasswordSetup -> NavEntry(key) {
                                        PasswordSetupScreen(
                                            title = "Dummy Password",
                                            description = if (isSetupComplete == true) "Update your decoy password" else "Create a decoy password to show fake data",
                                            onPasswordSet = {
                                                viewModel.saveDummyPassword(it)
                                                if (isSetupComplete == true) {
                                                    backStack.removeAt(backStack.size - 1)
                                                } else {
                                                    backStack.add(RecoverySetup)
                                                }
                                            }
                                        )
                                    }
                                    is RecoverySetup -> NavEntry(key) {
                                        RecoverySetupScreen(
                                            onComplete = {
                                                viewModel.saveRecoveryKey(it)
                                                backStack.add(Warning)
                                            }
                                        )
                                    }
                                    is Login -> NavEntry(key) {
                                        TerminalLoginScreen(
                                            appName = appName,
                                            onLoginSuccess = { 
                                                backStack.clear()
                                                backStack.add(Dashboard) 
                                            },
                                            onVerify = { viewModel.verifyPassword(it) }
                                        )
                                    }
                                    is Dashboard -> NavEntry(key) {
                                        DashboardScreen(
                                            stats = systemStats,
                                            appName = appName,
                                            appLogo = appLogo,
                                            customLogoUri = customLogoUri,
                                            onVerifyPassword = { password -> viewModel.verifyPassword(password) },
                                            useBiometric = useBiometric,
                                            onBiometricUnlock = {
                                                SecurityManager(this@MainActivity).showBiometricPrompt(
                                                    activity = this@MainActivity,
                                                    title = "Biometric Unlock",
                                                    subtitle = "Scan to access the vault",
                                                    onSuccess = {
                                                        backStack.removeAt(backStack.size - 1)
                                                        backStack.add(Vault)
                                                    },
                                                    onError = { }
                                                )
                                            },
                                            onNavigateToVault = { 
                                                backStack.removeAt(backStack.size - 1)
                                                backStack.add(Vault) 
                                            },
                                            onNavigateToTerminal = {
                                                backStack.removeAt(backStack.size - 1)
                                                backStack.add(Terminal)
                                            },
                                            onNavigateToAppLock = { 
                                                backStack.removeAt(backStack.size - 1)
                                                backStack.add(AppLock) 
                                            },
                                            onNavigateToCustomization = { 
                                                backStack.removeAt(backStack.size - 1)
                                                backStack.add(Customization) 
                                            },
                                            onNavigateToLogs = {
                                                backStack.removeAt(backStack.size - 1)
                                                backStack.add(Logs)
                                            },
                                            onLockNow = {
                                                backStack.clear()
                                                backStack.add(if (showWarningScreen) Warning else Dashboard)
                                            }
                                        )
                                    }
                                    is Vault -> NavEntry(key) {
                                        VaultScreen(
                                            viewModel = vaultViewModel,
                                            biometricFileAccess = biometricFileAccess,
                                            onVerifyBiometric = { onSuccess, onError ->
                                                SecurityManager(this@MainActivity).showBiometricPrompt(
                                                    activity = this@MainActivity,
                                                    title = "Unlock Vault File",
                                                    subtitle = "Scan fingerprint to view file",
                                                    onSuccess = onSuccess,
                                                    onError = onError
                                                )
                                            },
                                            onNavigateToNotes = { backStack.add(Notes) },
                                            onNavigateToTrash = { backStack.add(Trash) },
                                            onOpenPlayer = { queue, startIndex ->
                                                vaultViewModel.setPlayerQueue(queue)
                                                backStack.add(Player(startIndex))
                                            },
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is AppLock -> NavEntry(key) {
                                        AppLockScreen(
                                            repository = AppLockRepository(application),
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Terminal -> NavEntry(key) {
                                        val terminalViewModel: TerminalViewModel = viewModel()
                                        TerminalScreen(
                                            viewModel = terminalViewModel,
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Customization -> NavEntry(key) {
                                        CustomizationScreen(
                                            currentThemeMode = themeMode,
                                            currentAppAlias = AppAlias.fromClassName(activeAlias),
                                            shakeToLock = shakeToLock,
                                            screenshotProtection = screenshotProtection,
                                            showWarningScreen = showWarningScreen,
                                            useBiometric = useBiometric,
                                            biometricFileAccess = biometricFileAccess,
                                            onThemeModeChange = { viewModel.setThemeMode(it) },
                                            onAppAliasChange = { viewModel.updateActiveAlias(it) },
                                            onShakeToLockToggle = { viewModel.setShakeToLock(it) },
                                            onScreenshotProtectionToggle = { viewModel.setScreenshotProtection(it) },
                                            onShowWarningScreenToggle = { viewModel.setShowWarningScreen(it) },
                                            onUseBiometricToggle = { viewModel.setUseBiometric(it) },
                                            onBiometricFileAccessToggle = { viewModel.setBiometricFileAccess(it) },
                                            onChangeMainPassword = { backStack.add(MainPasswordSetup) },
                                            onChangeDummyPassword = { backStack.add(DummyPasswordSetup) },
                                            onViewLogs = { backStack.add(Logs) },
                                            onNavigateToAboutUs = { backStack.add(AboutUs) },
                                            onNavigateToUpdateCheck = { backStack.add(UpdateCheck) },
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is AboutUs -> NavEntry(key) {
                                        AboutScreen(
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is UpdateCheck -> NavEntry(key) {
                                        UpdateScreen(
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Notes -> NavEntry(key) {
                                        NotesScreen(
                                            repository = NotesRepository(application, isDummyMode),
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Trash -> NavEntry(key) {
                                        TrashScreen(
                                            viewModel = vaultViewModel,
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Player -> NavEntry(key) {
                                        val queue by vaultViewModel.playerQueue.collectAsState()
                                        PlayerScreen(
                                            viewModel = vaultViewModel,
                                            queue = queue,
                                            startIndex = key.startIndex,
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Logs -> NavEntry(key) {
                                        LogsScreen(
                                            logger = com.krish.systemsync.security.SecurityLogger(application),
                                            onBack = { backStack.removeAt(backStack.size - 1) }
                                        )
                                    }
                                    is Warning -> NavEntry(key) {
                                        WarningScreen(
                                            appName = appName,
                                            onSecretSuccess = {
                                                backStack.clear()
                                                backStack.add(Dashboard)
                                            }
                                        )
                                    }
                                    else -> NavEntry(key) {
                                        WelcomeScreen({})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    packageName: String,
    vaultName: String = "System SYNC",
    stealthMode: Boolean = false,
    blurLevel: Float = 15f,
    onUnlock: () -> Unit,
    onVerify: suspend (String) -> Boolean,
    useBiometric: Boolean
) {
    val context = LocalContext.current
    var showLogin by remember { mutableStateOf(!stealthMode) }

    val lockedAppName = remember(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (showLogin) {
            TerminalLoginScreen(
                appName = vaultName,
                onLoginSuccess = onUnlock,
                onVerify = onVerify
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "$lockedAppName is Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Stealth Mode: Fake Crash Dialog using Glassmorphism
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    blurLevel = blurLevel,
                    opacity = 0.2f
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.ReportProblem,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Application Error",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Unfortunately, $lockedAppName has stopped working unexpectedly. We've notified our developers.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(24.dp))
                        
                        var tapCount by remember { mutableIntStateOf(0) }
                        
                        Button(
                            onClick = { 
                                tapCount++
                                if (tapCount >= 3) {
                                    showLogin = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
            
            // Handle back button to "exit" the app (go home)
            androidx.activity.compose.BackHandler {
                val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                onUnlock() // Close overlay
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    currentRoute: Screen,
    isDummyMode: Boolean = false,
    blurLevel: Float = 0f,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Rounded.Home,
                label = "Vault",
                isSelected = currentRoute == Vault,
                onClick = { onNavigate(Vault) }
            )
            BottomNavItem(
                icon = Icons.Rounded.PhonelinkLock,
                label = "Apps",
                isSelected = currentRoute == AppLock,
                onClick = { onNavigate(AppLock) }
            )
            if (!isDummyMode) {
                BottomNavItem(
                    icon = Icons.Rounded.Settings,
                    label = "Settings",
                    isSelected = currentRoute == Customization,
                    onClick = { onNavigate(Customization) }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        modifier = Modifier
            .height(52.dp)
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
