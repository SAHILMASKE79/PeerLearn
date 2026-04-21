package com.sahil.peerlearn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppGreen,
    tertiary = AppPrimaryDark,
    background = AppBackground,
    surface = AppSurface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary
)

@Composable
fun PeerlearnTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
