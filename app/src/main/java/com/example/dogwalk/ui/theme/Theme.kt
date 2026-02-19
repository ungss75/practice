package com.example.dogwalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF58CC02),
    secondary = Color(0xFF1CB0F6),
    tertiary = Color(0xFFFFC800),
    background = Color(0xFFF7F7F7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF58CC02),
    secondary = Color(0xFF1CB0F6),
    tertiary = Color(0xFFFFC800)
)

@Composable
fun DogWalkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
