package com.krish.systemsync.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.krish.systemsync.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SageDarkPrimary,
    onPrimary = Color(0xFF00381C),
    primaryContainer = SageDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFC8E4D6),
    secondary = Color(0xFFB3CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF354B40),
    onSecondaryContainer = Color(0xFFCFE8D9),
    background = SageDarkBackground,
    surface = SageDarkSurface,
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF222B26),
    onSurfaceVariant = Color(0xFFC3C8C2),
    outline = Color(0xFF8D938E)
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = Color(0xFF4E6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D7),
    onSecondaryContainer = Color(0xFF0C1F15),
    background = SageBackground,
    surface = SageSurface,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF414943),
    outline = Color(0xFF717972)
)

private val AmoledColorScheme = darkColorScheme(
    primary = SageDarkPrimary,
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFF81C784),
    background = AmoledBackground,
    surface = AmoledSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun SystemSYNCTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: Color? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }.run {
        if (accentColor != null) {
            copy(primary = accentColor)
        } else {
            this
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
