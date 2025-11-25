package com.example.patienttracker.ui.screens.admin

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
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAllAppointmentsScreen(navController: NavController, context: Context) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var filteredAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Past, 2 = Cancelled
    var sortOption by remember { mutableStateOf("Most Recent") }
    var showSortMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = Firebase.firestore
                val snapshot = db.collection("appointments")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                appointments = snapshot.documents.mapNotNull { doc ->
                    Appointment.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
                }
                
                filteredAppointments = appointments
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    // Filter and sort
    LaunchedEffect(searchQuery, sortOption, appointments, selectedTabIndex) {
        val now = Date()
        
        // First filter by tab
        var result = when (selectedTabIndex) {
            0 -> {
                // Upcoming: date >= today and status != cancelled
                appointments.filter {
                    try {
                        it.appointmentDate.toDate().after(Date(now.time - 86400000)) &&
                        !it.status.contains("cancelled", ignoreCase = true)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            1 -> {
                // Past: date < today and status != cancelled
                appointments.filter {
                    try {
                        it.appointmentDate.toDate().before(Date(now.time - 86400000)) &&
                        !it.status.contains("cancelled", ignoreCase = true)
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            2 -> {
                // Cancelled: status = cancelled
                appointments.filter {
                    it.status.contains("cancelled", ignoreCase = true)
                }
            }
            else -> appointments
        }
        
        // Then apply search filter
        if (searchQuery.isNotEmpty()) {
            result = result.filter {
                it.patientName.contains(searchQuery, ignoreCase = true) ||
                it.doctorName.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Finally apply sorting
        result = when (sortOption) {
            "Most Recent" -> result.sortedByDescending { it.createdAt }
            "Oldest" -> result.sortedBy { it.createdAt }
            "Upcoming" -> result.sortedBy { it.appointmentDate }
            "Completed" -> result.filter { it.status.contains("completed", ignoreCase = true) }
                .sortedByDescending { it.appointmentDate }
            "Cancelled" -> result.filter { it.status.contains("cancelled", ignoreCase = true) }
                .sortedByDescending { it.appointmentDate }
            else -> result
        }
        
        filteredAppointments = result
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
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Sort", tint = TextPrimary)
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf("Most Recent", "Oldest", "Upcoming", "Completed", "Cancelled").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                placeholder = { Text("Search by patient or doctor name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AccentColor)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            // Tabs for Upcoming / Past / Cancelled
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = BgColor,
                contentColor = AccentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AccentColor
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Upcoming", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Past", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Cancelled", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Sort indicator
            Text(
                text = "Sorted by: $sortOption (${filteredAppointments.size} appointments)",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else if (filteredAppointments.isEmpty()) {
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
                            text = if (searchQuery.isEmpty()) "No appointments found" else "No matching appointments",
                            fontSize = 18.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAppointments) { appointment ->
                        AppointmentListItem(
                            appointment = appointment,
                            onClick = {
                                navController.navigate("admin_appointment_details/${appointment.appointmentId}")
                            },
                            showCancelledBy = selectedTabIndex == 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentListItem(
    appointment: Appointment,
    onClick: () -> Unit,
    showCancelledBy: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.patientName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "with ${appointment.doctorName}",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Status Badge
                val statusColor = when (appointment.status.lowercase()) {
                    "completed" -> Color(0xFF10B981)
                    "cancelled" -> Color(0xFFEF4444)
                    "scheduled" -> AccentColor
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = appointment.getFormattedDate(),
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatTimeRange(appointment.timeSlot),
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            }
            
            if (appointment.speciality.isNotEmpty()) {
                Text(
                    text = appointment.speciality,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Show who cancelled the appointment if it's cancelled
            if (showCancelledBy && appointment.status.contains("cancelled", ignoreCase = true) && appointment.cancelledBy.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Cancelled by: ${appointment.cancelledBy.replaceFirstChar { it.uppercase() }}",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
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
