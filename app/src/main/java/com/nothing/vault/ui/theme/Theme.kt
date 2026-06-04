package com.nothing.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = AccentLight,
    surface = Surface,
    background = Background,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onSurface = TextPrimary,
    onBackground = TextPrimary
)

@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
