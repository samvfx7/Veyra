package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFFF8FAFC)
val OnPrimary = Color(0xFF0F172A)
val PrimaryContainer = Color(0xFF1E2235)
val OnPrimaryContainer = Color(0xFFE2E8F0)

val Secondary = Color(0xFF94A3B8)
val OnSecondary = Color(0xFF0F172A)
val SecondaryContainer = Color(0xFF1E293B)
val OnSecondaryContainer = Color(0xFFCBD5E1)

val Tertiary = Color(0xFF818CF8)
val OnTertiary = Color(0xFF0F172A)

val Background = Color(0xFF08090D) 
val OnBackground = Color(0xFFF1F5F9)

val Surface = Color(0xFF0F111A) 
val OnSurface = Color(0xFFF1F5F9)
val SurfaceVariant = Color(0xFF1A1D2B)
val OnSurfaceVariant = Color(0xFF94A3B8)

val Outline = Color(0xFF2E344A)
val OutlineVariant = Color(0xFF1E2335)

val Error = Color(0xFFF87171)
val OnError = Color(0xFF450A0A)

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError
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
