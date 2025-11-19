package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

@Composable
fun ConfirmAppointmentScreen(
    navController: NavController,
    context: Context,
    doctorId: String,
    doctorName: String,
    specialty: String,
    dateStr: String,
    timeSlot: String
) {
    val scope = rememberCoroutineScope()
    
    var patientName by remember { mutableStateOf("Loading...") }
    var patientPhone by remember { mutableStateOf("Loading...") }
    var notes by remember { mutableStateOf("") }
    var isBooking by remember { mutableStateOf(false) }
    
    // Fetch patient info
    LaunchedEffect(Unit) {
        try {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val userDoc = Firebase.firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                patientName = "$firstName $lastName"
                patientPhone = userDoc.getString("phoneNumber") ?: "N/A"
            }
        } catch (e: Exception) {
            patientName = "Patient"
            patientPhone = "N/A"
        }
    }

    // Format date for display
    val formattedDate = try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy"))
    } catch (e: Exception) {
        dateStr
    }

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
                        .padding(
                            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Confirm Appointment",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardWhite,
                tonalElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isBooking = true
                            try {
                                // Parse date and create timestamp at noon (12:00 PM)
                                val date = LocalDate.parse(dateStr)
                                val appointmentDateTime = date.atTime(12, 0).atZone(ZoneId.systemDefault())
                                val timestamp = Timestamp(appointmentDateTime.toEpochSecond(), 0)
                                
                                // Create appointment
                                val result = AppointmentRepository.createAppointment(
                                    doctorUid = doctorId,
                                    doctorName = doctorName,
                                    speciality = specialty,
                                    appointmentDate = timestamp,
                                    timeSlot = timeSlot,
                                    notes = notes
                                )
                                
                                if (result.isSuccess) {
                                    val appointment = result.getOrNull()
                                    // Navigate to success screen and clear booking flow from back stack
                                    navController.navigate("appointment_success/${appointment?.appointmentId ?: ""}/$doctorName/$formattedDate/$timeSlot") {
                                        popUpTo("patient_home") { inclusive = false }
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle error
                            } finally {
                                isBooking = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    enabled = !isBooking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isBooking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirm Appointment",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appointment Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Appointment Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatTextColor
                    )

                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Patient",
                        value = patientName
                    )

                    DetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = patientPhone
                    )

                    DetailRow(
                        icon = Icons.Default.LocalHospital,
                        label = "Doctor",
                        value = doctorName
                    )

                    DetailRow(
                        icon = Icons.Default.MedicalServices,
                        label = "Specialty",
                        value = specialty
                    )

                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date",
                        value = formattedDate
                    )

                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Time",
                        value = timeSlot
                    )

                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = "Hospital Clinic"
                    )
                }
            }

            // Notes Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Notes for Doctor (Optional)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatTextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = {
                            Text(
                                text = "Add any symptoms or concerns you'd like to discuss...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderTopColor,
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = StatTextColor,
                            unfocusedTextColor = StatTextColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = BackgroundColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = HeaderTopColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = StatTextColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
