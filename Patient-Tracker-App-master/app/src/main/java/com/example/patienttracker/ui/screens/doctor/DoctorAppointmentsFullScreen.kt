package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentsFullScreen(
    navController: NavController,
    context: Context
) {
    var selectedTab by remember { mutableStateOf(0) }
    var upcomingAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var pastAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var cancelledAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val currentUser = Firebase.auth.currentUser
            android.util.Log.d("DoctorAppointmentsFull", "Current user: ${currentUser?.uid}")
            if (currentUser != null) {
                try {
                    android.util.Log.d("DoctorAppointmentsFull", "Fetching doctor appointments...")
                    val result = AppointmentRepository.getDoctorAppointments()
                    val allAppointments = result.getOrNull() ?: emptyList()
                    android.util.Log.d("DoctorAppointmentsFull", "Total appointments fetched: ${allAppointments.size}")
                    
                    if (result.isFailure) {
                        android.util.Log.e("DoctorAppointmentsFull", "Failed to fetch appointments: ${result.exceptionOrNull()?.message}")
                    }
                    
                    val now = Date()
                    
                    upcomingAppointments = allAppointments
                        .filter { appointment ->
                            try {
                                val isUpcoming = appointment.appointmentDate.toDate().after(Date(now.time - 86400000)) // Include today
                                android.util.Log.d("DoctorAppointmentsFull", "Appointment ${appointment.patientName}: ${appointment.getFormattedDate()}, isUpcoming=$isUpcoming, status=${appointment.status}")
                                isUpcoming && appointment.status.lowercase() != "cancelled" && appointment.status.lowercase() != "completed"
                            } catch (e: Exception) {
                                android.util.Log.e("DoctorAppointmentsFull", "Error processing appointment: ${e.message}")
                                false
                            }
                        }
                        .sortedBy { it.appointmentDate }
                    
                    pastAppointments = allAppointments
                        .filter { appointment ->
                            try {
                                val isPast = appointment.appointmentDate.toDate().before(Date(now.time - 86400000))
                                val isCompleted = appointment.status.lowercase() == "completed"
                                (isPast || isCompleted) && appointment.status.lowercase() != "cancelled"
                            } catch (e: Exception) {
                                false
                            }
                        }
                        .sortedByDescending { it.appointmentDate }
                    
                    cancelledAppointments = allAppointments
                        .filter { appointment ->
                            appointment.status.lowercase() == "cancelled"
                        }
                        .sortedByDescending { it.appointmentDate }
                    
                    android.util.Log.d("DoctorAppointmentsFull", "Upcoming: ${upcomingAppointments.size}, Past: ${pastAppointments.size}, Cancelled: ${cancelledAppointments.size}")
                } catch (e: Exception) {
                    android.util.Log.e("DoctorAppointmentsFull", "Exception: ${e.message}", e)
                } finally {
                    isLoading = false
                }
            } else {
                android.util.Log.e("DoctorAppointmentsFull", "User not authenticated")
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Appointments", fontWeight = FontWeight.Bold) },
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
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BgColor,
                contentColor = AccentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Upcoming (${upcomingAppointments.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Past (${pastAppointments.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { 
                        Text(
                            "Cancelled (${cancelledAppointments.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                val appointments = when (selectedTab) {
                    0 -> upcomingAppointments
                    1 -> pastAppointments
                    else -> cancelledAppointments
                }
                
                if (appointments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "No upcoming appointments"
                                    1 -> "No past appointments"
                                    else -> "No cancelled appointments"
                                },
                                fontSize = 18.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(appointments) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                showActions = selectedTab == 0,
                                onMarkComplete = {
                                    scope.launch {
                                        android.util.Log.d("DoctorAppointments", "Marking appointment ${appointment.appointmentId} as completed")
                                        val result = AppointmentRepository.updateAppointmentStatus(
                                            appointment.appointmentId,
                                            "completed"
                                        )
                                        if (result.isSuccess) {
                                            android.util.Log.d("DoctorAppointments", "Successfully marked as completed")
                                            // Refresh appointments
                                            val refreshResult = AppointmentRepository.getDoctorAppointments()
                                            val allAppointments = refreshResult.getOrNull() ?: emptyList()
                                            val now = Date()
                                            
                                            upcomingAppointments = allAppointments
                                                .filter { it.appointmentDate.toDate().after(Date(now.time - 86400000)) && it.status.lowercase() != "cancelled" && it.status.lowercase() != "completed" }
                                                .sortedBy { it.appointmentDate }
                                            
                                            pastAppointments = allAppointments
                                                .filter { 
                                                    val isPast = it.appointmentDate.toDate().before(Date(now.time - 86400000))
                                                    val isCompleted = it.status.lowercase() == "completed"
                                                    (isPast || isCompleted) && it.status.lowercase() != "cancelled"
                                                }
                                                .sortedByDescending { it.appointmentDate }
                                            
                                            cancelledAppointments = allAppointments
                                                .filter { it.status.lowercase() == "cancelled" }
                                                .sortedByDescending { it.appointmentDate }
                                            
                                            android.widget.Toast.makeText(context, "Appointment marked as completed", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                            android.util.Log.e("DoctorAppointments", "Failed to mark as completed: $errorMsg", result.exceptionOrNull())
                                            android.widget.Toast.makeText(context, "Failed to update appointment: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onCancel = {
                                    scope.launch {
                                        android.util.Log.d("DoctorAppointments", "Cancelling appointment ${appointment.appointmentId}")
                                        val result = AppointmentRepository.updateAppointmentStatus(
                                            appointment.appointmentId,
                                            "cancelled"
                                        )
                                        if (result.isSuccess) {
                                            android.util.Log.d("DoctorAppointments", "Successfully cancelled appointment")
                                            
                                            // Send notification to patient
                                            try {
                                                val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault())
                                                val formattedDate = dateFormat.format(appointment.appointmentDate.toDate())
                                                
                                                NotificationRepository().createNotification(
                                                    patientUid = appointment.patientUid,
                                                    title = "Appointment Cancelled",
                                                    message = "Your appointment with Dr. ${appointment.doctorName} scheduled on $formattedDate at ${appointment.timeSlot} has been cancelled by the doctor.",
                                                    type = "appointment_cancelled",
                                                    appointmentId = appointment.appointmentId
                                                )
                                            } catch (e: Exception) {
                                                android.util.Log.e("DoctorAppointments", "Failed to send notification: ${e.message}")
                                            }
                                            
                                            // Refresh appointments
                                            val refreshResult = AppointmentRepository.getDoctorAppointments()
                                            val allAppointments = refreshResult.getOrNull() ?: emptyList()
                                            val now = Date()
                                            
                                            upcomingAppointments = allAppointments
                                                .filter { it.appointmentDate.toDate().after(Date(now.time - 86400000)) && it.status.lowercase() != "cancelled" && it.status.lowercase() != "completed" }
                                                .sortedBy { it.appointmentDate }
                                            
                                            pastAppointments = allAppointments
                                                .filter { 
                                                    val isPast = it.appointmentDate.toDate().before(Date(now.time - 86400000))
                                                    val isCompleted = it.status.lowercase() == "completed"
                                                    (isPast || isCompleted) && it.status.lowercase() != "cancelled"
                                                }
                                                .sortedByDescending { it.appointmentDate }
                                            
                                            cancelledAppointments = allAppointments
                                                .filter { it.status.lowercase() == "cancelled" }
                                                .sortedByDescending { it.appointmentDate }
                                            
                                            android.widget.Toast.makeText(context, "Appointment cancelled", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                            android.util.Log.e("DoctorAppointments", "Failed to cancel appointment: $errorMsg", result.exceptionOrNull())
                                            android.widget.Toast.makeText(context, "Failed to cancel appointment: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
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
private fun AppointmentCard(
    appointment: Appointment,
    showActions: Boolean = false,
    onMarkComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.patientName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = appointment.speciality,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                // Status badge
                val statusColor = when (appointment.status.lowercase()) {
                    "confirmed" -> Color(0xFF10B981)
                    "completed" -> Color(0xFF3B82F6)
                    "pending" -> Color(0xFFF59E0B)
                    "cancelled" -> Color(0xFFEF4444)
                    else -> TextSecondary
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = appointment.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = appointment.getFormattedDate(),
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatTimeRange(appointment.timeSlot),
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }
            
            // Action buttons for upcoming appointments
            if (showActions) {
                val status = appointment.status.lowercase()
                
                // Check if appointment is today
                val appointmentDate = try {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = appointment.appointmentDate.toDate()
                    calendar.get(java.util.Calendar.YEAR) to 
                    calendar.get(java.util.Calendar.DAY_OF_YEAR)
                } catch (e: Exception) {
                    null
                }
                
                val today = java.util.Calendar.getInstance().let { cal ->
                    cal.get(java.util.Calendar.YEAR) to 
                    cal.get(java.util.Calendar.DAY_OF_YEAR)
                }
                
                val isToday = appointmentDate == today
                
                android.util.Log.d("AppointmentCard", "showActions=$showActions, status=$status, isToday=$isToday")
                
                if (status == "confirmed" || status == "pending" || status == "scheduled") {
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel button - always show for upcoming appointments
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("AppointmentCard", "Cancel clicked")
                                onCancel()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel", fontSize = 14.sp)
                        }
                        
                        // Mark as Completed button - only show on the day of appointment
                        if (isToday) {
                            Button(
                                onClick = {
                                    android.util.Log.d("AppointmentCard", "Mark as completed clicked")
                                    onMarkComplete()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Complete", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeRange(timeRange: String): String {
    return try {
        val cleaned = timeRange.replace("+", " ").replace("\\s+".toRegex(), " ").trim()
        if (cleaned.contains("AM", ignoreCase = true) || cleaned.contains("PM", ignoreCase = true)) {
            return cleaned.replace("AM", " AM").replace("PM", " PM")
                .replace("am", " AM").replace("pm", " PM")
                .replace("\\s+".toRegex(), " ").trim()
        }
        val parts = cleaned.split("-").map { it.trim() }
        if (parts.size == 2) {
            val startTime = java.time.LocalTime.parse(parts[0], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = java.time.LocalTime.parse(parts[1], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
            "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        } else {
            cleaned
        }
    } catch (e: Exception) {
        timeRange
    }
}
