package com.krish.systemsync.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krish.systemsync.ui.theme.SystemSYNCTheme

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var showPrivacyInfo by remember { mutableStateOf(false) }

    SetupLayout(
        title = "System SYNC",
        subtitle = "Advanced Device Protection & Monitoring",
        icon = Icons.Default.Shield
    ) {
        Text(
            text = "Welcome to the next generation of mobile security. System SYNC provides professional-grade tools to monitor your hardware and secure your data with multi-layered encryption.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
        
        TextButton(onClick = { showPrivacyInfo = true }) {
            Text("Privacy Information", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showPrivacyInfo) {
        AlertDialog(
            onDismissRequest = { showPrivacyInfo = false },
            title = { Text("Privacy Information") },
            text = {
                Text(
                    "System SYNC stores everything locally on this device only. " +
                    "Files you hide are either AES-256 encrypted (Maximum Privacy) or moved into an " +
                    "app-private folder marked with .nomedia (Standard Hidden), and both are removed " +
                    "from your Gallery/Files apps. Nothing is uploaded anywhere, and your main and " +
                    "dummy passwords are only stored as salted hashes on this device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyInfo = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
fun PasswordSetupScreen(
    title: String,
    description: String,
    onPasswordSet: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SetupLayout(
        title = title,
        subtitle = description,
        icon = Icons.Default.Lock
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Enter Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                }
            },
            isError = error != null
        )
        
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (password.length < 4) {
                    error = "Password must be at least 4 characters"
                } else if (password != confirmPassword) {
                    error = "Passwords do not match"
                } else {
                    onPasswordSet(password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun RecoverySetupScreen(onComplete: (String) -> Unit) {
    var recoveryKey by remember { mutableStateOf("") }

    SetupLayout(
        title = "Recovery Key",
        subtitle = "In case you forget your main password",
        icon = Icons.Default.Security
    ) {
        Text(
            text = "This key will be used to recover access to your vault. Store it in a safe place offline.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = recoveryKey,
            onValueChange = { recoveryKey = it },
            label = { Text("Recovery Answer / Key") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { if (recoveryKey.isNotBlank()) onComplete(recoveryKey) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = recoveryKey.isNotBlank()
        ) {
            Text("Finish Setup")
        }
    }
}

@Composable
fun SetupLayout(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePreview() {
    SystemSYNCTheme {
        WelcomeScreen({})
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordSetupPreview() {
    SystemSYNCTheme {
        PasswordSetupScreen("Main Password", "Create a secure password for your primary vault", {})
    }
}

// Removed duplicate theme wrapper
