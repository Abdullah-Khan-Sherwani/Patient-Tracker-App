package com.example.patienttracker.ui.screens.guest

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Light theme colors
private val LightBgColor = Color(0xFFFAF8F3)
private val LightCardColor = Color(0xFFF5F0E8)
private val LightAccentColor = Color(0xFFB8956A)
private val LightTextPrimary = Color(0xFF2F2019)
private val LightTextSecondary = Color(0xFF6B7280)

// Dark theme colors
private val DarkBgColor = Color(0xFF1A1A1A)
private val DarkCardColor = Color(0xFF2D2D2D)
private val DarkAccentColor = Color(0xFFD4AF8C)
private val DarkTextPrimary = Color(0xFFE5E5E5)
private val DarkTextSecondary = Color(0xFFB0B0B0)

class GuestPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("guest_prefs", Context.MODE_PRIVATE)
    
    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()
    
    var isUrdu: Boolean
        get() = prefs.getBoolean("is_urdu", false)
        set(value) = prefs.edit().putBoolean("is_urdu", value).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestSettingsScreen(navController: NavController, context: Context) {
    val preferences = remember { GuestPreferences(context) }
    var isDarkMode by remember { mutableStateOf(preferences.isDarkMode) }
    var isUrdu by remember { mutableStateOf(preferences.isUrdu) }
    
    // Update colors based on theme
    val bgColor = if (isDarkMode) DarkBgColor else LightBgColor
    val cardColor = if (isDarkMode) DarkCardColor else LightCardColor
    val accentColor = if (isDarkMode) DarkAccentColor else LightAccentColor
    val textPrimary = if (isDarkMode) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkMode) DarkTextSecondary else LightTextSecondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isUrdu) "ترتیبات" else "Settings",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Setting
            SettingCard(
                title = if (isUrdu) "زبان" else "Language",
                subtitle = if (isUrdu) "انگریزی اور اردو کے درمیان سوئچ کریں" else "Switch between English and Urdu",
                icon = Icons.Default.Language,
                cardColor = cardColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = accentColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) "اردو" else "English",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                    Switch(
                        checked = isUrdu,
                        onCheckedChange = { 
                            isUrdu = it
                            preferences.isUrdu = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = textSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Dark Mode Setting
            SettingCard(
                title = if (isUrdu) "ڈارک موڈ" else "Dark Mode",
                subtitle = if (isUrdu) "اپنی آنکھوں کو آرام دیں" else "Easy on the eyes",
                icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                cardColor = cardColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accentColor = accentColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkMode) {
                            if (isUrdu) "آن" else "On"
                        } else {
                            if (isUrdu) "آف" else "Off"
                        },
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { 
                            isDarkMode = it
                            preferences.isDarkMode = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = textSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardColor.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isUrdu) 
                            "یہ ترتیبات صرف مہمان موڈ کے لیے ہیں۔ مکمل رسائی کے لیے سائن اپ کریں۔"
                        else
                            "These settings apply to guest mode only. Sign up for full access.",
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            content()
        }
    }
}
