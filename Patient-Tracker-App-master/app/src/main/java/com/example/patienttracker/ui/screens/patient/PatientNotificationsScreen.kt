package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Notification
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientNotificationsScreen(navController: NavController, context: Context) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val notificationRepo = remember { NotificationRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    // Load notifications
    LaunchedEffect(refreshTrigger) {
        currentUser?.uid?.let { uid ->
            android.util.Log.d("PatientNotifications", "Loading notifications for user: $uid")
            isLoading = true
            scope.launch {
                try {
                    notifications = notificationRepo.getPatientNotifications(uid)
                    android.util.Log.d("PatientNotifications", "Loaded ${notifications.size} notifications")
                    Toast.makeText(context, "Loaded ${notifications.size} notifications", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("PatientNotifications", "Error loading notifications: ${e.message}", e)
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        } ?: run {
            android.util.Log.e("PatientNotifications", "No current user found")
            Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    
                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    currentUser?.uid?.let { uid ->
                                        notificationRepo.markAllAsRead(uid)
                                        notifications = notifications.map { it.copy(isRead = true) }
                                    }
                                }
                            }
                        ) {
                            Text(
                                "Mark all read",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD4AF8C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F1ED))
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFC9956E)
                    )
                }
                notifications.isEmpty() -> {
                    EmptyNotificationsView()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    scope.launch {
                                        if (!notification.isRead) {
                                            notificationRepo.markAsRead(notification.notificationId)
                                            notifications = notifications.map {
                                                if (it.notificationId == notification.notificationId) {
                                                    it.copy(isRead = true)
                                                } else it
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (notification.type) {
        "appointment_created" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "appointment_cancelled" -> Icons.Default.Cancel to Color(0xFFF44336)
        "appointment_updated" -> Icons.Default.Update to Color(0xFFFF9800)
        else -> Icons.Default.Notifications to Color(0xFF3CC7CD)
    }
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(notification.createdAt.toDate())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFFAF8F3)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1C3D5A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB8956A))
                        )
                    }
                }
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color(0xFF6C7A89),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color(0xFF9BA4B0),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFE8D9CC)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notifications yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6C7A89)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You'll see notifications about your appointments here",
            fontSize = 14.sp,
            color = Color(0xFF9BA4B0),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
