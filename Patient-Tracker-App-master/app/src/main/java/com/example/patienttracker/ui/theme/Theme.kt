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

// Beige/Brown light theme colors
private val BeigeLight = Color(0xFFD4AF8C)
private val BeigeLight2 = Color(0xFFC9956E)
private val BeigeBackground = Color(0xFFF7EFE7)
private val BeigeCard = Color(0xFFFFFFFF)

// Dark theme colors
private val DarkPrimary = Color(0xFF2C2C2C)
private val DarkSecondary = Color(0xFF1F1F1F)
private val DarkBackground = Color(0xFF121212)
private val DarkCard = Color(0xFF1E1E1E)

private val DarkColorScheme = darkColorScheme(
    primary = BeigeLight,
    secondary = BeigeLight2,
    tertiary = BeigeLight,
    background = DarkBackground,
    surface = DarkCard,
    onPrimary = DarkPrimary,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BeigeLight,
    secondary = BeigeLight2,
    tertiary = BeigeLight,
    background = BeigeBackground,
    surface = BeigeCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF333333),
    onSurface = Color(0xFF333333)
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