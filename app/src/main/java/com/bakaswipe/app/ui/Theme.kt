package com.bakaswipe.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette tirée du logo : bleu nuit étoilé, cyan, jaune étoile
val Night = Color(0xFF0B1030)
val Cyan = Color(0xFF5BC0DE)
val StarYellow = Color(0xFFFFC83D)
val SwipeGreen = Color(0xFF4CD18A)
val SwipeRed = Color(0xFFFF5D73)

private val Scheme = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF04131C),
    primaryContainer = Color(0xFF1E4F73),
    onPrimaryContainer = Color(0xFFD6F2FF),
    secondary = Color(0xFF8C9BFF),
    onSecondary = Color(0xFF0B1030),
    secondaryContainer = Color(0xFF2B3470),
    onSecondaryContainer = Color(0xFFE0E4FF),
    tertiary = StarYellow,
    onTertiary = Color(0xFF2A1E00),
    background = Night,
    onBackground = Color(0xFFE6ECFF),
    surface = Night,
    onSurface = Color(0xFFE6ECFF),
    surfaceVariant = Color(0xFF232C62),
    onSurfaceVariant = Color(0xFFB4BEEA),
    surfaceContainerLowest = Color(0xFF080C26),
    surfaceContainerLow = Color(0xFF111740),
    surfaceContainer = Color(0xFF151C4A),
    surfaceContainerHigh = Color(0xFF1B2356),
    surfaceContainerHighest = Color(0xFF222B62),
    outline = Color(0xFF4A5594),
    outlineVariant = Color(0xFF2E376E),
)

@Composable
fun BakaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
