package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = NeonCyan,
    secondary = ConnectedGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF063E20),
    onSecondaryContainer = ConnectedGreen,
    tertiary = AccentPurple,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = DisconnectRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // VPN apps feature dark mode by default for premium tech look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
