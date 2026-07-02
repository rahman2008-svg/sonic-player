package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = SonicPrimary,
    secondary = SonicSecondary,
    tertiary = SonicTertiary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color(0xFF003258), // Dark contrast on the bright blue primary
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium cosmic dark theme for Sonic Player
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve brand identity
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}
