package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.ui.viewmodel.ThemeViewModel

// ========== THEME COLORS ==========
private val LightBgColor = Color(0xFFFAF8F3)
private val LightCardColor = Color(0xFFF5F0E8)
private val LightAccentColor = Color(0xFF04786A)  // Teal accent
private val LightTextPrimary = Color(0xFF2F2019)
private val LightTextSecondary = Color(0xFF6B7280)

private val DarkBgColor = Color(0xFF121212)
private val DarkCardColor = Color(0xFF1E1E1E)
private val DarkAccentColor = Color(0xFF18BC86)  // Lighter teal for dark mode
private val DarkTextPrimary = Color(0xFFE0E0E0)
private val DarkTextSecondary = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSettingsScreen(
    navController: NavController,
    context: Context,
    themeViewModel: ThemeViewModel? = null
) {
    val isDarkMode by themeViewModel?.isDarkMode ?: remember { mutableStateOf(false) }
    val selectedLanguage by themeViewModel?.selectedLanguage ?: remember { mutableStateOf("en") }
    
    var notificationsEnabled by remember { mutableStateOf(true) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(false) }

    // Dynamic colors based on theme
    val bgColor = if (isDarkMode) DarkBgColor else LightBgColor
    val cardColor = if (isDarkMode) DarkCardColor else LightCardColor
    val accentColor = if (isDarkMode) DarkAccentColor else LightAccentColor
    val textPrimary = if (isDarkMode) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkMode) DarkTextSecondary else LightTextSecondary

    // Localized strings based on selected language
    val strings = getLocalizedStrings(selectedLanguage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        strings.settingsTitle, 
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = accentColor
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
                .padding(20.dp)
        ) {
            // Appearance Section
            SettingsSectionHeader(
                title = strings.appearance,
                textColor = textPrimary
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Dark Mode Toggle
            SettingsSwitchItem(
                icon = Icons.Default.DarkMode,
                title = strings.darkMode,
                description = strings.darkModeDesc,
                checked = isDarkMode,
                onCheckedChange = { themeViewModel?.toggleDarkMode(it) },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Language Toggle
            SettingsLanguageItem(
                icon = Icons.Default.Language,
                title = strings.language,
                description = strings.languageDesc,
                selectedLanguage = selectedLanguage,
                onLanguageChange = { themeViewModel?.setLanguage(it) },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                englishLabel = strings.english,
                urduLabel = strings.urdu
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Notifications Section
            SettingsSectionHeader(
                title = strings.notifications,
                textColor = textPrimary
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = strings.pushNotifications,
                description = strings.pushNotificationsDesc,
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.DateRange,
                title = strings.appointmentReminders,
                description = strings.appointmentRemindersDesc,
                checked = appointmentReminders,
                onCheckedChange = { appointmentReminders = it },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.Email,
                title = strings.emailNotifications,
                description = strings.emailNotificationsDesc,
                checked = emailNotifications,
                onCheckedChange = { emailNotifications = it },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
            
            Spacer(Modifier.height(32.dp))
            
            // App Info Section
            SettingsSectionHeader(
                title = strings.about,
                textColor = textPrimary
            )
            
            Spacer(Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.appVersion,
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                        Text(
                            text = "1.0.0",
                            fontSize = 14.sp,
                            color = textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ========== SECTION HEADER ==========
@Composable
private fun SettingsSectionHeader(
    title: String,
    textColor: Color
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}

// ========== SETTINGS SWITCH ITEM ==========
@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    cardColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = textSecondary
                )
            )
        }
    }
}

// ========== LANGUAGE TOGGLE ITEM ==========
@Composable
private fun SettingsLanguageItem(
    icon: ImageVector,
    title: String,
    description: String,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    cardColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    englishLabel: String,
    urduLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Language Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // English Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("en") },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedLanguage == "en") accentColor else accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = englishLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedLanguage == "en") Color.White else accentColor,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                // Urdu Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("ur") },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedLanguage == "ur") accentColor else accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = urduLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedLanguage == "ur") Color.White else accentColor,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

// ========== LOCALIZED STRINGS ==========
private data class LocalizedStrings(
    val settingsTitle: String,
    val appearance: String,
    val darkMode: String,
    val darkModeDesc: String,
    val language: String,
    val languageDesc: String,
    val english: String,
    val urdu: String,
    val notifications: String,
    val pushNotifications: String,
    val pushNotificationsDesc: String,
    val appointmentReminders: String,
    val appointmentRemindersDesc: String,
    val emailNotifications: String,
    val emailNotificationsDesc: String,
    val about: String,
    val appVersion: String
)

private fun getLocalizedStrings(languageCode: String): LocalizedStrings {
    return when (languageCode) {
        "ur" -> LocalizedStrings(
            settingsTitle = "ترتیبات",
            appearance = "ظاہری شکل",
            darkMode = "ڈارک موڈ",
            darkModeDesc = "ڈارک تھیم فعال کریں",
            language = "زبان",
            languageDesc = "زبان منتخب کریں",
            english = "انگریزی",
            urdu = "اردو",
            notifications = "اطلاعات",
            pushNotifications = "پش اطلاعات",
            pushNotificationsDesc = "ملاقاتوں کے بارے میں اطلاعات حاصل کریں",
            appointmentReminders = "ملاقات کی یاد دہانی",
            appointmentRemindersDesc = "ملاقاتوں سے پہلے یاد دہانی حاصل کریں",
            emailNotifications = "ای میل اطلاعات",
            emailNotificationsDesc = "ای میل کے ذریعے اپ ڈیٹس حاصل کریں",
            about = "کے بارے میں",
            appVersion = "ایپ ورژن"
        )
        else -> LocalizedStrings(
            settingsTitle = "Settings",
            appearance = "Appearance",
            darkMode = "Dark Mode",
            darkModeDesc = "Enable dark theme",
            language = "Language",
            languageDesc = "Select language",
            english = "English",
            urdu = "Urdu",
            notifications = "Notifications",
            pushNotifications = "Push Notifications",
            pushNotificationsDesc = "Receive notifications about appointments",
            appointmentReminders = "Appointment Reminders",
            appointmentRemindersDesc = "Get reminded before appointments",
            emailNotifications = "Email Notifications",
            emailNotificationsDesc = "Receive updates via email",
            about = "About",
            appVersion = "App Version"
        )
    }
}
