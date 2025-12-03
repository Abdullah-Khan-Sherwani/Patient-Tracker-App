package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val TealAccent = Color(0xFF0F8B8D)          // Teal accent for buttons
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val SecondaryTextColor = Color(0xFF6B7280) // Secondary text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val CancelRed = Color(0xFFDC2626)           // Red for cancel button
private val RescheduleBlue = Color(0xFF0F8B8D)      // Teal for reschedule button

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
            // Include appointments from today and future dates, but exclude cancelled and completed
            val isUpcoming = appointmentDate.isAfter(today) || appointmentDate.isEqual(today)
            println("DEBUG: Appointment on $appointmentDate - isUpcoming: $isUpcoming (today: $today)")
            isUpcoming && appointment.status != "cancelled" && appointment.status != "completed"
        }
        .sortedBy { it.appointmentDate }

    val pastAppointments = appointments
        .filter { appointment ->
            val appointmentDate = appointment.appointmentDate.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            // Include past appointments OR completed appointments (regardless of date)
            val isPast = appointmentDate.isBefore(today)
            val isCompleted = appointment.status == "completed"
            (isPast || isCompleted) && appointment.status != "cancelled"
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
                        onClick = { navController.navigate("patient_home") },
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
                onClick = { navController.navigate("doctor_catalogue") },
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
                        navController = navController,
                        onRefresh = { refreshTrigger++ }
                    )
                    1 -> AppointmentsList(
                        appointments = pastAppointments,
                        emptyMessage = "No past appointments",
                        context = context,
                        navController = navController,
                        onRefresh = { refreshTrigger++ }
                    )
                    2 -> AppointmentsList(
                        appointments = cancelledAppointments,
                        emptyMessage = "No cancelled appointments",
                        context = context,
                        navController = navController,
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
    navController: NavController,
    onRefresh: () -> Unit
) {
    if (appointments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = SecondaryTextColor.copy(alpha = 0.5f)
                )
                Text(
                    text = emptyMessage,
                    color = SecondaryTextColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
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
                    navController = navController,
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
    navController: NavController,
    onRefresh: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy", Locale.getDefault())
    val appointmentDateTime = appointment.appointmentDate.toDate()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formattedDate = appointmentDateTime.format(formatter)
    
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    
    // Check if this is an upcoming appointment (can be cancelled/rescheduled)
    val now = LocalDateTime.now(ZoneId.systemDefault())
    val today = now.toLocalDate()
    val isUpcoming = appointmentDateTime.isAfter(today) || appointmentDateTime.isEqual(today)
    val isCancelled = appointment.status == "cancelled"
    val isCompleted = appointment.status == "completed"
    val isRescheduled = appointment.status == "rescheduled"
    val isScheduled = appointment.status == "scheduled"
    
    // Check if appointment is within 12 hours (for late cancellation warning)
    val appointmentInstant = appointment.appointmentDate.toDate().toInstant()
    val nowInstant = java.time.Instant.now()
    val hoursUntilAppointment = java.time.Duration.between(nowInstant, appointmentInstant).toHours()
    val isWithin12Hours = hoursUntilAppointment in 0..12

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Date and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = TealAccent
                    )
                    Text(
                        text = formattedDate,
                        color = StatTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Status Badge
                when {
                    isCancelled -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CancelRed.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Cancelled",
                                color = CancelRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isCompleted -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Completed",
                                color = Color(0xFF2196F3),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isRescheduled -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Rescheduled",
                                color = Color(0xFFFF9800),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    isScheduled && isUpcoming -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TealAccent.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Scheduled",
                                color = TealAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Time Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SecondaryTextColor
                )
                val displayTime = if (appointment.timeSlot.contains("-") || appointment.timeSlot.contains(":")) {
                    formatTimeRange(appointment.timeSlot)
                } else {
                    "${appointment.timeSlot} • ${getTimeRangeForBlock(appointment.timeSlot)}"
                }
                Text(
                    text = displayTime,
                    color = SecondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Doctor Name
            Text(
                text = appointment.doctorName,
                color = StatTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            // Speciality
            Text(
                text = appointment.speciality,
                color = TealAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Show dependent name if this is a dependent appointment
            if (appointment.recipientType == "dependent" && appointment.dependentName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TealAccent.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "For: ${appointment.dependentName}",
                        color = TealAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            
            // Action Buttons (only for upcoming scheduled/rescheduled appointments)
            if (isUpcoming && !isCancelled && !isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reschedule Button
                    OutlinedButton(
                        onClick = {
                            // Navigate to date/time selection for same doctor
                            // Parse doctor name - format is typically "Dr. First Last"
                            val nameParts = appointment.doctorName
                                .removePrefix("Dr. ")
                                .removePrefix("Dr ")
                                .split(" ")
                            val firstName = URLEncoder.encode(nameParts.firstOrNull() ?: "", StandardCharsets.UTF_8.toString())
                            val lastName = URLEncoder.encode(nameParts.drop(1).joinToString(" ").ifEmpty { " " }, StandardCharsets.UTF_8.toString())
                            val encodedSpeciality = URLEncoder.encode(appointment.speciality, StandardCharsets.UTF_8.toString())
                            navController.navigate("select_datetime/${appointment.doctorUid}/$firstName/$lastName/$encodedSpeciality?rescheduleFrom=${appointment.appointmentId}")
                        },
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RescheduleBlue
                        ),
                        border = BorderStroke(1.dp, RescheduleBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Reschedule",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Cancel Button
                    if (!isCancelling) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CancelRed
                            ),
                            border = BorderStroke(1.dp, CancelRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TealAccent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp),
            title = { 
                Text(
                    text = "Cancel Appointment",
                    fontWeight = FontWeight.Bold,
                    color = StatTextColor
                )
            },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to cancel this appointment?",
                        color = SecondaryTextColor
                    )
                    
                    // Late cancellation warning
                    if (isWithin12Hours) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Late Cancellation Warning",
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You are cancelling within 12 hours of your appointment. Repeated late cancellations may result in account restrictions.",
                                        color = Color(0xFFBF360C),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
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
                                        patientMessage = "Your appointment with ${appointment.doctorName} on $formattedDate has been cancelled.",
                                        doctorTitle = "Appointment Cancelled",
                                        doctorMessage = "Appointment with ${appointment.patientName} scheduled on $formattedDate has been cancelled by patient.",
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CancelRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Yes", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SecondaryTextColor
                    ),
                    border = BorderStroke(1.dp, SecondaryTextColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("No", fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}