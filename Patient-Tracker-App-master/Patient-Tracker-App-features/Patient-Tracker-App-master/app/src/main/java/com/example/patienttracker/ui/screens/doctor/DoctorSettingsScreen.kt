package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSettingsScreen(
    navController: NavController,
    context: Context
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Notifications Section
            Text(
                text = "Notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                description = "Receive notifications about appointments",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.DateRange,
                title = "Appointment Reminders",
                description = "Get reminded before appointments",
                checked = appointmentReminders,
                onCheckedChange = { appointmentReminders = it }
            )
            
            Spacer(Modifier.height(12.dp))
            
            SettingsSwitchItem(
                icon = Icons.Default.Email,
                title = "Email Notifications",
                description = "Receive updates via email",
                checked = emailNotifications,
                onCheckedChange = { emailNotifications = it }
            )
            
            Spacer(Modifier.height(32.dp))
            
            // App Info Section
            Text(
                text = "About",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            
            Spacer(Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardColor
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
                            text = "App Version",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "1.0.0",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = TextSecondary
                )
            )
        }
    }
}
