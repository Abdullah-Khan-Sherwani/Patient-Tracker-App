package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

// Color scheme - light beige/beige gradient
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

/**
 * Convert time block name to time range string
 */
private fun getTimeRangeForBlock(blockName: String): String {
    return when (blockName.lowercase()) {
        "morning" -> "6:00 AM - 12:00 PM"
        "afternoon" -> "12:00 PM - 6:00 PM"
        "evening" -> "6:00 PM - 12:00 AM"
        else -> blockName // Return original if not a recognized block
    }
}

private fun formatTimeRange(timeRange: String): String {
    return try {
        // Clean the input - remove + signs and extra spaces, handle AM/PM already present
        val cleaned = timeRange.replace("+", " ").replace("\\s+".toRegex(), " ").trim()
        
        // Check if it's already formatted with AM/PM
        if (cleaned.contains("AM", ignoreCase = true) || cleaned.contains("PM", ignoreCase = true)) {
            // Just ensure proper spacing
            return cleaned.replace("AM", " AM").replace("PM", " PM")
                .replace("am", " AM").replace("pm", " PM")
                .replace("\\s+".toRegex(), " ").trim()
        }
        
        // Parse as 24-hour format
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

@Composable
fun FullScheduleScreen(navController: NavController, context: Context, initialTab: String = "upcoming") {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var selectedTabIndex by remember { 
        mutableStateOf(
            when (initialTab) {
                "cancelled" -> 2
                "past" -> 1
                else -> 0
            }
        )
    }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Refresh appointments when screen is displayed or refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        currentUser?.uid?.let { userId ->
            try {
                println("DEBUG: Fetching appointments for user: $userId")
                val result = AppointmentRepository.getPatientAppointments()
                appointments = result.getOrElse { emptyList() }
                println("DEBUG: Fetched ${appointments.size} appointments")
                appointments.forEach { apt ->
                    println("DEBUG: Appointment - ${apt.doctorName} on ${apt.appointmentDate.toDate()}")
                }
            } catch (e: Exception) {
                println("DEBUG: Error fetching appointments: ${e.message}")
            }
        } ?: run {
            println("DEBUG: No current user found")
        }
    }

    // Separate appointments into upcoming and past
    val now = LocalDateTime.now(ZoneId.systemDefault())
    val today = now.toLocalDate()
    
    val upcomingAppointments = appointments
        .filter { appointment ->
            val appointmentDate = appointment.appointmentDate.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            // Include appointments from today and future dates, but exclude cancelled
            val isUpcoming = appointmentDate.isAfter(today) || appointmentDate.isEqual(today)
            println("DEBUG: Appointment on $appointmentDate - isUpcoming: $isUpcoming (today: $today)")
            isUpcoming && appointment.status != "cancelled"
        }
        .sortedBy { it.appointmentDate }

    val pastAppointments = appointments
        .filter { appointment ->
            val appointmentDate = appointment.appointmentDate.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            appointmentDate.isBefore(today) && appointment.status != "cancelled"
        }
        .sortedByDescending { it.appointmentDate }
    
    val cancelledAppointments = appointments
        .filter { appointment ->
            appointment.status == "cancelled"
        }
        .sortedByDescending { it.appointmentDate }
    
    println("DEBUG: Upcoming: ${upcomingAppointments.size}, Past: ${pastAppointments.size}, Cancelled: ${cancelledAppointments.size}")

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "My Appointments",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("select_specialty") },
                containerColor = ButtonColor,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Book Appointment"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 4.dp,
                        color = HeaderTopColor
                    )
                },
                divider = { Divider(color = Color.LightGray, thickness = 1.dp) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "Upcoming",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) HeaderTopColor else StatTextColor
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "Past",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) HeaderTopColor else StatTextColor
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            "Cancelled",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTabIndex == 2) HeaderTopColor else StatTextColor
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundColor)
            ) {
                when (selectedTabIndex) {
                    0 -> AppointmentsList(
                        appointments = upcomingAppointments,
                        emptyMessage = "No upcoming appointments",
                        context = context,
                        onRefresh = { refreshTrigger++ }
                    )
                    1 -> AppointmentsList(
                        appointments = pastAppointments,
                        emptyMessage = "No past appointments",
                        context = context,
                        onRefresh = { refreshTrigger++ }
                    )
                    2 -> AppointmentsList(
                        appointments = cancelledAppointments,
                        emptyMessage = "No cancelled appointments",
                        context = context,
                        onRefresh = { refreshTrigger++ }
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentsList(
    appointments: List<Appointment>,
    emptyMessage: String,
    context: Context,
    onRefresh: () -> Unit
) {
    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = StatTextColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    context = context,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    context: Context,
    onRefresh: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.getDefault())
    val appointmentDateTime = appointment.appointmentDate.toDate()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formattedDate = appointmentDateTime.format(formatter)
    
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    
    // Check if this is an upcoming appointment (can be cancelled)
    val now = LocalDateTime.now(ZoneId.systemDefault())
    val today = now.toLocalDate()
    val isUpcoming = appointmentDateTime.isAfter(today) || appointmentDateTime.isEqual(today)
    val isCancelled = appointment.status == "cancelled"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        color = HeaderTopColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    val displayTime = if (appointment.timeSlot.contains("-") || appointment.timeSlot.contains(":")) {
                        formatTimeRange(appointment.timeSlot)
                    } else {
                        "${appointment.timeSlot} • ${getTimeRangeForBlock(appointment.timeSlot)}"
                    }
                    Text(
                        text = displayTime,
                        color = StatTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = appointment.doctorName,
                        color = StatTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appointment.speciality,
                        color = HeaderBottomColor
                    )
                    
                    // Show appointment number if available
                    if (appointment.appointmentNumber.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Appointment #${appointment.appointmentNumber}",
                            color = StatTextColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Status badge or cancel button
                if (isCancelled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "Cancelled",
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (isUpcoming && !isCancelling) {
                    TextButton(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Cancel")
                    }
                } else if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = HeaderTopColor,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment") },
            text = { Text("Are you sure you want to cancel this appointment with ${appointment.doctorName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isCancelling = true
                            showCancelDialog = false
                            try {
                                // Cancel appointment
                                AppointmentRepository.cancelAppointment(appointment.appointmentId, "patient").getOrThrow()
                                
                                // Create notifications for patient and doctor
                                try {
                                    val notificationRepo = NotificationRepository()
                                    notificationRepo.createNotificationForBoth(
                                        patientUid = appointment.patientUid,
                                        doctorUid = appointment.doctorUid,
                                        patientTitle = "Appointment Cancelled",
                                        patientMessage = "Your appointment with ${appointment.doctorName} on $formattedDate at ${appointment.timeSlot} has been cancelled.",
                                        doctorTitle = "Appointment Cancelled",
                                        doctorMessage = "Appointment with ${appointment.patientName} scheduled on $formattedDate at ${appointment.timeSlot} has been cancelled by patient.",
                                        type = "appointment_cancelled",
                                        appointmentId = appointment.appointmentId
                                    )
                                } catch (e: Exception) {
                                    println("Failed to create notifications: ${e.message}")
                                }
                                
                                // Show success message
                                android.widget.Toast.makeText(
                                    context,
                                    "Appointment cancelled successfully",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                
                                // Trigger refresh
                                onRefresh()
                            } catch (e: Exception) {
                                println("Failed to cancel appointment: ${e.message}")
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to cancel appointment: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isCancelling = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep It")
                }
            }
        )
    }
}