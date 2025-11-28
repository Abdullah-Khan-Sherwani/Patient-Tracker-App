package com.example.patienttracker.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy colors (keeping for backwards compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============================================================
// Deep Teal & Mint Design System
// WCAG Compliant - Professional Healthcare Theme
// ============================================================

// Primary Colors
val PrimaryDark = Color(0xFF0E4944)      // Deep Teal - headers, active tab icons, primary text on light
val PrimaryLight = Color(0xFF16605A)     // Lighter Teal - hover states on dark backgrounds
val AccentMint = Color(0xFF76DCB0)       // Mint Green - primary CTA buttons, progress bars

// Surface Colors
val SurfaceWhite = Color(0xFFFFFFFF)     // Content cards
val BackgroundDim = Color(0xFFF0F5F4)    // Screen background behind cards

// Text Colors
val TextOnDark = Color(0xFFFFFFFF)       // White text on dark backgrounds
val TextOnLight = Color(0xFF1F2937)      // Dark Charcoal text on light backgrounds
val TextSubtle = Color(0xFF6B7280)       // Subtle text for descriptions

// Card Styling
val CardShadow = Color(0x140E4944)       // Soft shadow: rgba(14, 73, 68, 0.08)

// Status Colors
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val InfoBlue = Color(0xFF3B82F6)

// Dark Mode Colors for Deep Teal Theme
val DarkPrimaryBg = Color(0xFF0B1F1D)           // Very dark teal background
val DarkSurfaceBg = Color(0xFF112B28)           // Dark teal surface for cards
val DarkCardBg = Color(0xFF1A3D38)              // Slightly lighter for elevated cards
val DarkAccentMint = Color(0xFF76DCB0)          // Same mint accent
val DarkTextPrimary = Color(0xFFE8F5F3)         // Light text on dark
val DarkTextSecondary = Color(0xFFA3C9C4)       // Muted text on dark
val DarkDivider = Color(0xFF2A4A46)             // Divider color

// Bottom Navigation Colors
val NavActiveColor = PrimaryDark
val NavInactiveColor = Color(0xFF9CA3AF)
val NavIndicatorColor = AccentMint.copy(alpha = 0.15f)