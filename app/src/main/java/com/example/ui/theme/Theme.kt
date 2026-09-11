package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFF000000)
val PrimaryContainer = Color(0xFF1A1A1A)
val OnPrimaryContainer = Color(0xFFFFFFFF)

val Secondary = Color(0xFFA0A0A0)
val OnSecondary = Color(0xFF000000)

val Background = Color(0xFF050505) 
val OnBackground = Color(0xFFEDEDED)

val Surface = Color(0xFF0A0A0A) 
val OnSurface = Color(0xFFEDEDED)
val SurfaceVariant = Color(0xFF141414)
val OnSurfaceVariant = Color(0xFFA0A0A0)

val Error = Color(0xFFEF4444)

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
