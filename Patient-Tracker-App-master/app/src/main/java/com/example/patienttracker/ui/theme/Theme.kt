package com.example.patienttracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ============================================================
// Deep Teal & Mint Design System
// WCAG Compliant - Professional Healthcare Theme
// ============================================================

// Light theme colors
private val TealPrimary = Color(0xFF0E4944)        // Deep Teal - headers, primary
private val TealPrimaryLight = Color(0xFF16605A)   // Lighter Teal - secondary
private val MintAccent = Color(0xFF76DCB0)         // Mint Green - CTAs, accents
private val BackgroundLight = Color(0xFFF0F5F4)    // Dim background
private val SurfaceLight = Color(0xFFFFFFFF)       // Card surface
private val TextDark = Color(0xFF1F2937)           // Dark charcoal text

// Dark theme colors
private val DarkPrimary = Color(0xFF76DCB0)        // Mint accent in dark
private val DarkSecondary = Color(0xFF16605A)      // Lighter teal
private val DarkBackground = Color(0xFF0B1F1D)     // Very dark teal
private val DarkSurface = Color(0xFF112B28)        // Dark teal surface
private val DarkText = Color(0xFFE8F5F3)           // Light text

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = MintAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF1A3D38),
    onPrimary = Color(0xFF0B1F1D),
    onSecondary = Color.White,
    onTertiary = Color(0xFF0E4944),
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = Color(0xFFA3C9C4),
    primaryContainer = Color(0xFF1A3D38),
    onPrimaryContainer = MintAccent,
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealPrimaryLight,
    tertiary = MintAccent,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFE8F5F3),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = TealPrimary,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = Color(0xFF6B7280),
    primaryContainer = Color(0xFFE8F5F3),
    onPrimaryContainer = TealPrimary,
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun PatientTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}