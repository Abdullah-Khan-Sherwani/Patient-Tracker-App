package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep Teal accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependentAppointmentHistoryScreen(
    navController: NavController,
    context: Context,
    dependentId: String,
    dependentName: String
) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Load dependent appointments on screen open
    LaunchedEffect(dependentId) {
        scope.launch {
            val result = AppointmentRepository.getDependentAppointments(dependentId)
            if (result.isSuccess) {
                appointments = result.getOrNull() ?: emptyList()
            } else {
                Toast.makeText(
                    context,
                    "Error loading appointments: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            isLoading = false
        }
    }

    val now = System.currentTimeMillis()
    val upcomingAppointments = appointments.filter { 
        it.appointmentDate.toDate().time > now && it.status != "Cancelled"
    }
    val pastAppointments = appointments.filter { 
        it.appointmentDate.toDate().time <= now || it.status == "Cancelled"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.navigate("patient_home") }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Appointment History",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "For $dependentName",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardWhite,
                contentColor = ButtonColor
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Upcoming (${upcomingAppointments.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Past (${pastAppointments.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ButtonColor)
                    }
                }
                appointments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = AccentColor.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No Appointments",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatTextColor
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "No appointments have been booked\nfor $dependentName yet",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    val displayList = if (selectedTab == 0) upcomingAppointments else pastAppointments
                    
                    if (displayList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.EventAvailable else Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = AccentColor.copy(alpha = 0.5f)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = if (selectedTab == 0) "No Upcoming Appointments" else "No Past Appointments",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StatTextColor
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayList) { appointment ->
                                DependentAppointmentCard(
                                    appointment = appointment,
                                    isPast = selectedTab == 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DependentAppointmentCard(
    appointment: Appointment,
    isPast: Boolean
) {
    val dateFormatter = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val appointmentDate = appointment.appointmentDate.toDate()
    
    val statusColor = when (appointment.status.lowercase()) {
        "confirmed" -> Color(0xFF4CAF50)
        "scheduled" -> Color(0xFFE57373)
        "cancelled" -> Color(0xFFE57373)
        "completed" -> Color(0xFF2196F3)
        else -> Color.Gray
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentColor.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ButtonColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateFormatter.format(appointmentDate),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatTextColor
                            )
                        }
                    }
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = appointment.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Doctor Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AccentColor.copy(alpha = 0.3f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appointment.doctorName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ButtonColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.doctorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appointment.speciality,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Time and Token Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeFormatter.format(appointmentDate),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Show time slot if available
            if (appointment.timeSlot.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appointment.timeSlot,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
