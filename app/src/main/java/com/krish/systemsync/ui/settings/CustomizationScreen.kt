package com.krish.systemsync.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.krish.systemsync.settings.ThemeMode
import com.krish.systemsync.settings.AppAlias
import com.krish.systemsync.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    currentThemeMode: ThemeMode,
    currentAppAlias: AppAlias,
    shakeToLock: Boolean,
    screenshotProtection: Boolean,
    showWarningScreen: Boolean,
    useBiometric: Boolean,
    biometricFileAccess: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppAliasChange: (AppAlias) -> Unit,
    onShakeToLockToggle: (Boolean) -> Unit,
    onScreenshotProtectionToggle: (Boolean) -> Unit,
    onShowWarningScreenToggle: (Boolean) -> Unit,
    onUseBiometricToggle: (Boolean) -> Unit,
    onBiometricFileAccessToggle: (Boolean) -> Unit,
    onChangeMainPassword: () -> Unit,
    onChangeDummyPassword: () -> Unit,
    onViewLogs: () -> Unit,
    onBack: () -> Unit
) {
    var hasSaved by remember { mutableStateOf(false) }

    // Revert changes on exit if SAVE was not clicked
    DisposableEffect(Unit) {
        onDispose {
            if (!hasSaved) {
                onThemeModeChange(currentThemeMode)
                onAppAliasChange(currentAppAlias)
                onShakeToLockToggle(shakeToLock)
                onScreenshotProtectionToggle(screenshotProtection)
                onShowWarningScreenToggle(showWarningScreen)
                onUseBiometricToggle(useBiometric)
                onBiometricFileAccessToggle(biometricFileAccess)
            }
        }
    }

    BackHandler {
        onBack()
    }

    var activeDialog by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        hasSaved = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Settings Saved Successfully")
                        }
                    }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Mode Section
            SectionTitle("Appearance")
            ThemeModeSelector(currentThemeMode, onThemeModeChange)

            // Stealth Disguise Section
            SectionTitle("App Drawer Disguise")
            Text(
                "Change the app icon and name in your phone's app drawer to hide it from others.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppAliasSelector(currentAppAlias, onAppAliasChange)

            // Security Section
            SectionTitle("Security Refinement")
            SecuritySettings(
                shakeToLock,
                screenshotProtection,
                showWarningScreen,
                useBiometric,
                biometricFileAccess,
                onShakeToLockToggle = onShakeToLockToggle,
                onScreenshotProtectionToggle = onScreenshotProtectionToggle,
                onShowWarningScreenToggle = onShowWarningScreenToggle,
                onUseBiometricToggle = onUseBiometricToggle,
                onBiometricFileAccessToggle = onBiometricFileAccessToggle,
                onChangeMainPassword,
                onChangeDummyPassword
            )

            SectionTitle("System Logs")
            Button(
                onClick = onViewLogs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Security Logs")
            }

            // Legal & About Section at the Very Bottom
            SectionTitle("About & Legal")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { activeDialog = "about" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("About Us")
                }
                OutlinedButton(
                    onClick = { activeDialog = "terms" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Terms and Conditions")
                }
                OutlinedButton(
                    onClick = { activeDialog = "privacy" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Privacy Policy")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (activeDialog != null) {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = {
                Text(
                    when (activeDialog) {
                        "about" -> "About Us"
                        "terms" -> "Terms and Conditions"
                        "privacy" -> "Privacy Policy"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    when (activeDialog) {
                        "about" -> "System SYNC is an advanced local device manager and secure vault suite. It gives you professional-grade tools to monitor hardware performance, lock sensitive applications, and protect your private files with military-grade local encryption."
                        "terms" -> "By using System SYNC, you acknowledge and agree that all data, passwords, and encrypted files are stored exclusively on your local device. We assume no responsibility for lost decryption keys or recovery data."
                        "privacy" -> "System SYNC operates on a strict zero-knowledge, local-only architecture. No personal data, passwords, logs, or vault contents are ever collected, tracked, or transmitted to any external servers."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ThemeModeSelector(currentMode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThemeModeCard("Light", Icons.Rounded.LightMode, currentMode == ThemeMode.LIGHT, Modifier.weight(1f)) {
            onModeChange(ThemeMode.LIGHT)
        }
        ThemeModeCard("Dark", Icons.Rounded.DarkMode, currentMode == ThemeMode.DARK, Modifier.weight(1f)) {
            onModeChange(ThemeMode.DARK)
        }
        ThemeModeCard("AMOLED", Icons.Rounded.AutoAwesome, currentMode == ThemeMode.AMOLED, Modifier.weight(1f)) {
            onModeChange(ThemeMode.AMOLED)
        }
    }
}

@Composable
fun ThemeModeCard(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun AppAliasSelector(currentAlias: AppAlias, onAliasChange: (AppAlias) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppAlias.values().forEach { alias ->
            val isSelected = currentAlias == alias
            Surface(
                onClick = { onAliasChange(alias) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (alias == AppAlias.DEFAULT) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(alias.iconRes),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(alias.labelRes),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (alias == AppAlias.DEFAULT) {
                            Text("Original look", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySettings(
    shakeToLock: Boolean,
    screenshotProtection: Boolean,
    showWarningScreen: Boolean,
    useBiometric: Boolean,
    biometricFileAccess: Boolean,
    onShakeToLockToggle: (Boolean) -> Unit,
    onScreenshotProtectionToggle: (Boolean) -> Unit,
    onShowWarningScreenToggle: (Boolean) -> Unit,
    onUseBiometricToggle: (Boolean) -> Unit,
    onBiometricFileAccessToggle: (Boolean) -> Unit,
    onChangeMainPassword: () -> Unit,
    onChangeDummyPassword: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggle(
            title = "Shake to Lock",
            subtitle = "Quickly lock the vault by shaking the device",
            checked = shakeToLock,
            onCheckedChange = onShakeToLockToggle,
            icon = Icons.Rounded.Vibration
        )
        SettingsToggle(
            title = "Screenshot Protection",
            subtitle = "Prevent screenshots on sensitive screens",
            checked = screenshotProtection,
            onCheckedChange = onScreenshotProtectionToggle,
            icon = Icons.Rounded.NoEncryption
        )
        SettingsToggle(
            title = "Decoy Camouflage",
            subtitle = "Show fake error screen on startup",
            checked = showWarningScreen,
            onCheckedChange = onShowWarningScreenToggle,
            icon = Icons.Rounded.Security
        )
        SettingsToggle(
            title = "Biometric Access",
            subtitle = "Unlock vault with fingerprint",
            checked = useBiometric,
            onCheckedChange = onUseBiometricToggle,
            icon = Icons.Rounded.Fingerprint
        )
        SettingsToggle(
            title = "Biometric File Access",
            subtitle = "Require fingerprint before viewing vault files/videos",
            checked = biometricFileAccess,
            onCheckedChange = onBiometricFileAccessToggle,
            icon = Icons.Rounded.LockPerson
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Setup System Fingerprint")
        }

        OutlinedButton(
            onClick = onChangeMainPassword,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Change Main Password")
        }

        OutlinedButton(
            onClick = onChangeDummyPassword,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.LockOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Change Dummy Password")
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
fun CustomizationPreview() {
    SystemSYNCTheme {
        CustomizationScreen(
            currentThemeMode = ThemeMode.DARK,
            currentAppAlias = AppAlias.DEFAULT,
            shakeToLock = true,
            screenshotProtection = true,
            showWarningScreen = true,
            useBiometric = false,
            biometricFileAccess = false,
            onThemeModeChange = {},
            onAppAliasChange = {},
            onShakeToLockToggle = {},
            onScreenshotProtectionToggle = {},
            onShowWarningScreenToggle = {},
            onUseBiometricToggle = {},
            onBiometricFileAccessToggle = {},
            onChangeMainPassword = {},
            onChangeDummyPassword = {},
            onViewLogs = {},
            onBack = {}
        )
    }
}
