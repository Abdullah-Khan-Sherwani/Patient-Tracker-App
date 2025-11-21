package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentStorage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.clickable

/**
 * Book Appointment Screen
 * 
 * THEME FIX: Now uses MaterialTheme.colorScheme for proper dark mode support
 * - Primary colors for header gradient and buttons
 * - Surface/background colors for proper contrast
 * - All text colors use theme-aware values
 */
@Composable
fun BookAppointmentScreen(
    navController: NavController,
    context: Context,
    doctor: DoctorFull
) {
    // THEME FIX: Use MaterialTheme colors instead of hardcoded gradient
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val gradient = Brush.verticalGradient(listOf(primaryColor, primaryContainer))

    val availableDays = doctor.days.split(",").map { it.trim().lowercase(Locale.ROOT) }
    val timing = doctor.timings
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var message by remember { mutableStateOf("") }
    var selectedReport by remember { mutableStateOf<String?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Book Appointment",
                        // THEME FIX: Use onPrimary for text on primary background
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        // THEME FIX: Use background color from theme
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Dr. ${doctor.firstName} ${doctor.lastName}", 
                fontWeight = FontWeight.Bold,
                // THEME FIX: Use onBackground color
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Speciality: ${doctor.speciality}", 
                // THEME FIX: Use primary color for specialty
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Available Days: ${doctor.days}",
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Timings: ${doctor.timings}",
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            DatePicker(selectedDate) { date ->
                selectedDate = date
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { /* TODO: Implement file picker */ },
                // THEME FIX: Use surfaceVariant for secondary button
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (selectedReport == null) "Attach Report" else "Report Attached: $selectedReport", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val dayName = selectedDate.dayOfWeek.name.lowercase(Locale.ROOT)
            val canBook = availableDays.any { dayName.contains(it.take(3)) || it.contains(dayName.take(3)) }

            Button(
                onClick = {
                    if (canBook) {
                        AppointmentStorage.saveAppointment(
                            context,
                            doctor,
                            selectedDate.format(dateFormatter),
                            timing,
                            selectedReport
                        )
                        message = "Appointment booked successfully!"
                    } else {
                        message = "Doctor not available on this day."
                    }
                },
                // THEME FIX: Use primary color from theme
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Booking", color = MaterialTheme.colorScheme.onPrimary)
            }

            if (message.isNotEmpty()) {
                Text(
                    message, 
                    color = if (message.contains("success")) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Date Picker Component
 * 
 * THEME FIX: Now uses MaterialTheme colors for proper dark mode support
 */
@Composable
fun DatePicker(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        val today = LocalDate.now()
        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val isSelected = date == selectedDate
            Surface(
                shape = RoundedCornerShape(8.dp),
                // THEME FIX: Use primary for selected, surfaceVariant for unselected
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onDateSelected(date) }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        date.dayOfMonth.toString(), 
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        date.dayOfWeek.name.take(3), 
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
