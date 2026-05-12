package com.example.spark.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SparkClay,
    onPrimary = Color.White,
    secondary = SparkPeach,
    onSecondary = SparkInk,
    tertiary = SparkGold,
    onTertiary = SparkInk,
    background = SparkBackground,
    onBackground = SparkInk,
    surface = SparkSurface,
    onSurface = SparkInk,
    surfaceVariant = SparkSand,
    onSurfaceVariant = SparkMuted,
    outline = SparkLine
)

private val DarkColorScheme = darkColorScheme(
    primary = SparkDarkPrimary,
    onPrimary = SparkInk,
    secondary = SparkDarkSecondary,
    onSecondary = SparkInk,
    tertiary = SparkDarkTertiary,
    onTertiary = SparkInk,
    background = SparkDarkBackground,
    onBackground = SparkDarkInk,
    surface = SparkDarkSurface,
    onSurface = SparkDarkInk,
    surfaceVariant = SparkDarkSurfaceVariant,
    onSurfaceVariant = SparkDarkMuted,
    outline = SparkDarkLine
)

@Composable
fun SparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
