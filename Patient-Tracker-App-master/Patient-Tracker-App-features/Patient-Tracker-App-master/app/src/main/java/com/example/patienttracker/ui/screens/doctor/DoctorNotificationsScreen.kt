package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorNotificationsScreen(navController: NavController, context: Context) {
    var notifications by remember { mutableStateOf<List<com.example.patienttracker.data.Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Fetch notifications
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    val result = NotificationRepository().getDoctorNotifications(currentUser.uid)
                    notifications = result.getOrDefault(emptyList())
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorNotifications", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    notifications.filter { !it.isRead }.forEach { notification ->
                                        NotificationRepository().markAsRead(notification.notificationId)
                                    }
                                    // Refresh
                                    val currentUser = Firebase.auth.currentUser
                                    if (currentUser != null) {
                                        val result = NotificationRepository().getDoctorNotifications(currentUser.uid)
                                        notifications = result.getOrDefault(emptyList())
                                    }
                                }
                            }
                        ) {
                            Text("Mark all read", color = AccentColor, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        fontSize = 18.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            scope.launch {
                                // Mark as read
                                if (!notification.isRead) {
                                    NotificationRepository().markAsRead(notification.notificationId)
                                    // Refresh
                                    val currentUser = Firebase.auth.currentUser
                                    if (currentUser != null) {
                                        val result = NotificationRepository().getDoctorNotifications(currentUser.uid)
                                        notifications = result.getOrDefault(emptyList())
                                    }
                                }
                                
                                // Navigate if appointment-related
                                if (notification.type.contains("appointment", ignoreCase = true)) {
                                    navController.navigate("doctor_appointments_full")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: com.example.patienttracker.data.Notification,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (notification.isRead) CardColor else CardColor.copy(alpha = 0.6f),
        tonalElevation = if (notification.isRead) 1.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        notification.type.contains("appointment", ignoreCase = true) -> Icons.Default.DateRange
                        notification.type.contains("reminder", ignoreCase = true) -> Icons.Default.Notifications
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63))
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = notification.getFormattedDate(),
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
