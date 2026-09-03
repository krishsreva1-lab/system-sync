package com.krish.systemsync.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.krish.systemsync.monitor.SystemStats
import com.krish.systemsync.settings.AppLogo
import com.krish.systemsync.ui.components.GlassSurface
import com.krish.systemsync.ui.theme.SystemSYNCTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: SystemStats,
    appName: String = "System SYNC",
    appLogo: AppLogo = AppLogo.SHIELD,
    customLogoUri: String? = null,
    onVerifyPassword: suspend (String) -> Boolean,
    onBiometricUnlock: () -> Unit = {},
    useBiometric: Boolean = false,
    onNavigateToVault: () -> Unit,
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToAppLock: () -> Unit,
    onNavigateToCustomization: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    onLockNow: () -> Unit
) {
    var terminalInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (useBiometric) {
        LaunchedEffect(Unit) {
            onBiometricUnlock()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            appName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            "Device Manager",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (customLogoUri != null) {
                                AsyncImage(
                                    model = java.io.File(customLogoUri),
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                val logoIcon = when (appLogo) {
                                    AppLogo.SHIELD -> Icons.Rounded.Shield
                                    AppLogo.TERMINAL -> Icons.Rounded.Terminal
                                    AppLogo.LOCK -> Icons.Rounded.Lock
                                    AppLogo.FOLDER -> Icons.Rounded.Folder
                                    AppLogo.SYSTEM -> Icons.Rounded.Settings
                                }
                                Icon(
                                    logoIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Hardware Grid
            val hardwareItems = listOf(
                HardwareItem(
                    title = "CPU",
                    value = "${stats.cpuUsage}%",
                    subText = "2.40 GHz",
                    icon = Icons.Rounded.DeveloperBoard,
                    progress = stats.cpuUsage / 100f,
                    color = MaterialTheme.colorScheme.primary
                ),
                HardwareItem(
                    title = "RAM",
                    value = "${stats.ramUsed / 1024 / 1024 / 1024} GB",
                    subText = "${stats.ramTotal / 1024 / 1024 / 1024} GB Total",
                    icon = Icons.Rounded.Memory,
                    progress = if (stats.ramTotal > 0) stats.ramUsed.toFloat() / stats.ramTotal else 0f,
                    color = Color(0xFF2E7D32)
                ),
                HardwareItem(
                    title = "GPU",
                    value = "12%",
                    subText = "Adreno 740",
                    icon = Icons.Rounded.GraphicEq,
                    progress = 0.12f,
                    color = Color(0xFF00695C)
                ),
                HardwareItem(
                    title = "Battery",
                    value = "${stats.batteryLevel}%",
                    subText = if (stats.isCharging) "Charging" else "Healthy",
                    icon = Icons.Rounded.BatteryChargingFull,
                    progress = stats.batteryLevel / 100f,
                    color = Color(0xFF00838F)
                ),
                HardwareItem(
                    title = "Storage",
                    value = "${(stats.storageTotal - stats.storageUsed) / 1024 / 1024 / 1024} GB",
                    subText = "Free Space",
                    icon = Icons.Rounded.Storage,
                    progress = if (stats.storageTotal > 0) stats.storageUsed.toFloat() / stats.storageTotal else 0f,
                    color = Color(0xFF37474F)
                ),
                HardwareItem(
                    title = "Temp",
                    value = "${stats.batteryTemp.toInt()}°C",
                    subText = "Normal",
                    icon = Icons.Rounded.Thermostat,
                    progress = (stats.batteryTemp - 20) / 40f,
                    color = Color(0xFF5D4037)
                )
            )

            // Grid structure
            hardwareItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { item ->
                        HardwareCard(
                            modifier = Modifier.weight(1f),
                            item = item
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(Modifier.height(16.dp))

            // Secure Terminal Card
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(32.dp),
                opacity = 0.25f,
                borderOpacity = 0.4f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SECURE TERMINAL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Icon(
                            Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "[SYSTEM] Booting secure kernel... Done\n[SEC] Establishing encrypted tunnel... OK\n[AUTH] Ready for system access credentials.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            TextField(
                                value = terminalInput,
                                onValueChange = { 
                                    terminalInput = it 
                                    errorMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        "user@vault:~$ _",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        )
                                    ) 
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        scope.launch {
                                            if (onVerifyPassword(terminalInput)) {
                                                val cmd = terminalInput.lowercase().trim()
                                                when {
                                                    cmd.contains("vault") -> onNavigateToVault()
                                                    cmd.contains("app") || cmd.contains("lock") -> onNavigateToAppLock()
                                                    cmd.contains("set") || cmd.contains("config") -> onNavigateToCustomization()
                                                    cmd.contains("log") -> onNavigateToLogs()
                                                    else -> onNavigateToVault() // Default to vault on successful password
                                                }
                                                terminalInput = ""
                                            } else {
                                                errorMessage = "Invalid credential"
                                            }
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                singleLine = true
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

data class HardwareItem(
    val title: String,
    val value: String,
    val subText: String,
    val icon: ImageVector,
    val progress: Float,
    val color: Color
)

@Composable
fun HardwareCard(
    modifier: Modifier = Modifier,
    item: HardwareItem
) {
    GlassSurface(
        modifier = modifier.aspectRatio(0.85f),
        shape = RoundedCornerShape(32.dp),
        opacity = 0.1f,
        borderOpacity = 0.15f
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    item.value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                SmoothLineGraph(color = item.color)
            }
        }
    }
}

@Composable
fun SmoothLineGraph(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val path = Path()
        
        path.moveTo(0f, height * 0.8f)
        
        path.cubicTo(
            width * 0.25f, height * 0.2f,
            width * 0.5f, height * 0.9f,
            width * 0.75f, height * 0.4f
        )
        path.lineTo(width, height * 0.6f)

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    val dummyStats = SystemStats(
        cpuUsage = 45,
        ramUsed = 4000000000L,
        ramTotal = 8000000000L,
        batteryLevel = 85,
        batteryTemp = 36.5f,
        isCharging = true,
        storageUsed = 60000000000L,
        storageTotal = 128000000000L
    )
    SystemSYNCTheme(themeMode = com.krish.systemsync.settings.ThemeMode.LIGHT) {
        DashboardScreen(
            stats = dummyStats,
            onVerifyPassword = { true },
            onNavigateToVault = {},
            onNavigateToAppLock = {},
            onNavigateToCustomization = {},
            onLockNow = {}
        )
    }
}
