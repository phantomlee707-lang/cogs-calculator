package com.rainweaver.cogscalc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Teal, onPrimary = Paper, primaryContainer = TealLight, secondary = TealDark,
    background = Paper, surface = Color.White, onBackground = Ink, onSurface = Ink,
)

@Composable
fun CogsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
