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
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Color scheme - light beige/beige gradient
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

@Composable
fun FullScheduleScreen(navController: NavController, context: Context) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { _ ->
            try {
                val result = AppointmentRepository.getPatientAppointments()
                appointments = result.getOrElse { emptyList() }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Separate appointments into upcoming and past
    val now = LocalDateTime.now(ZoneId.systemDefault())
    val upcomingAppointments = appointments
        .filter { appointment ->
            val appointmentDateTime = appointment.appointmentDate.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            appointmentDateTime.isAfter(now)
        }
        .sortedBy { it.appointmentDate }

    val pastAppointments = appointments
        .filter { appointment ->
            val appointmentDateTime = appointment.appointmentDate.toDate()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            appointmentDateTime.isBefore(now)
        }
        .sortedByDescending { it.appointmentDate }

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
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "My Appointments",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("book_appointment") },
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
                        emptyMessage = "No upcoming appointments"
                    )
                    1 -> AppointmentsList(
                        appointments = pastAppointments,
                        emptyMessage = "No past appointments"
                    )
                }
            }
        }
    }
}

@Composable
fun AppointmentsList(appointments: List<Appointment>, emptyMessage: String) {
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
                AppointmentCard(appointment)
            }
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.getDefault())
    val appointmentDateTime = appointment.appointmentDate.toDate()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formattedDate = appointmentDateTime.format(formatter)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = formattedDate,
                color = HeaderTopColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = appointment.timeSlot,
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
        }
    }
}