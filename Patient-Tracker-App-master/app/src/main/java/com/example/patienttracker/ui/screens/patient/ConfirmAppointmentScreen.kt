package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
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
import com.example.patienttracker.data.NotificationRepository
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

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent

@Composable
fun ConfirmAppointmentScreen(
    navController: NavController,
    context: Context,
    doctorId: String,
    doctorName: String,
    specialty: String,
    dateStr: String,
    blockName: String,
    timeRange: String,
    dependentId: String = "",
    dependentName: String = ""
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
                patientPhone = userDoc.getString("phone") ?: "N/A"
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
                        )
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Confirm Appointment",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
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
                                
                                // Check for duplicate appointment
                                val currentUser = Firebase.auth.currentUser
                                if (currentUser != null) {
                                    val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    
                                    // Determine if this booking is for self or a dependent
                                    val isBookingForSelf = dependentId.isBlank() || dependentId == "self"
                                    val targetDependentId = if (isBookingForSelf) "" else dependentId
                                    
                                    android.util.Log.d("DuplicateCheck", "=== Starting duplicate check ===")
                                    android.util.Log.d("DuplicateCheck", "isBookingForSelf=$isBookingForSelf, targetDependentId='$targetDependentId', incoming dependentId='$dependentId'")
                                    android.util.Log.d("DuplicateCheck", "Looking for: doctorId=$doctorId, date=$dateStr, block=$blockName")
                                    
                                    // Query for existing appointments matching: patientUid and doctorUid
                                    val allAppointments = Firebase.firestore.collection("appointments")
                                        .whereEqualTo("patientUid", currentUser.uid)
                                        .whereEqualTo("doctorUid", doctorId)
                                        .get()
                                        .await()
                                    
                                    android.util.Log.d("DuplicateCheck", "Found ${allAppointments.documents.size} total appointments with this doctor")
                                    
                                    // Filter by recipient and active status
                                    val matchingAppointments = allAppointments.documents.filter { doc ->
                                        val apptDependentId = doc.getString("dependentId") ?: ""
                                        val apptRecipientType = doc.getString("recipientType") ?: "self"
                                        val status = doc.getString("status")?.lowercase() ?: ""
                                        
                                        android.util.Log.d("DuplicateCheck", "Appointment: recipientType='$apptRecipientType', dependentId='$apptDependentId', status='$status'")
                                        
                                        // Check if this appointment is for the same person we're booking for
                                        val isSameRecipient = if (isBookingForSelf) {
                                            // For self booking: match if recipientType is "self" OR dependentId is empty/blank/"self"
                                            apptRecipientType == "self" || apptDependentId.isBlank() || apptDependentId == "self"
                                        } else {
                                            // For dependent booking: match only if dependentId matches exactly
                                            apptDependentId == targetDependentId
                                        }
                                        
                                        val isActive = status == "scheduled" || status == "confirmed"
                                        
                                        android.util.Log.d("DuplicateCheck", "  -> isSameRecipient=$isSameRecipient, isActive=$isActive")
                                        
                                        isSameRecipient && isActive
                                    }
                                    
                                    android.util.Log.d("DuplicateCheck", "Found ${matchingAppointments.size} matching appointments for same recipient")
                                    
                                    val hasDuplicate = matchingAppointments.any { doc ->
                                        val apptDate = doc.getTimestamp("appointmentDate")
                                        val apptBlockName = doc.getString("blockName") ?: ""
                                        
                                        if (apptDate != null) {
                                            val apptLocalDate = apptDate.toDate().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDate()
                                            val apptDateStr = apptLocalDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                            val isDuplicate = apptDateStr == dateStr && apptBlockName == blockName
                                            android.util.Log.d("DuplicateCheck", "  Date check: apptDate=$apptDateStr, targetDate=$dateStr, apptBlock=$apptBlockName, targetBlock=$blockName -> isDuplicate=$isDuplicate")
                                            isDuplicate
                                        } else {
                                            false
                                        }
                                    }
                                    
                                    android.util.Log.d("DuplicateCheck", "=== Final result: hasDuplicate=$hasDuplicate ===")
                                    
                                    if (hasDuplicate) {
                                        Toast.makeText(context, "You already have an appointment with this doctor in the $blockName time slot on this date.", Toast.LENGTH_LONG).show()
                                        isBooking = false
                                        return@launch
                                    }
                                }
                                
                                // Create appointment with actual time range formatted
                                val formattedTimeRange = formatTimeRange(timeRange)
                                
                                // Normalize dependentId: convert "self" to empty string for consistency
                                val normalizedDependentId = if (dependentId.isBlank() || dependentId == "self") "" else dependentId
                                val normalizedDependentName = if (dependentId.isBlank() || dependentId == "self") "" else dependentName
                                
                                val recipientType = if (normalizedDependentId.isBlank()) "self" else "dependent"
                                val result = AppointmentRepository.createAppointment(
                                    doctorUid = doctorId,
                                    doctorName = doctorName,
                                    speciality = specialty,
                                    appointmentDate = timestamp,
                                    timeSlot = formattedTimeRange, // Store formatted doctor's time range
                                    blockName = blockName, // Store block name for slot counting
                                    recipientType = recipientType,
                                    dependentId = normalizedDependentId,
                                    dependentName = normalizedDependentName,
                                    notes = notes
                                )
                                
                                if (result.isSuccess) {
                                    val appointment = result.getOrNull()
                                    
                                    // Create notifications for patient and doctor (don't let this fail the whole flow)
                                    try {
                                        val currentUser = Firebase.auth.currentUser
                                        android.util.Log.d("ConfirmAppointment", "Current user: ${currentUser?.uid}, Appointment: ${appointment?.appointmentId}")
                                        
                                        if (currentUser != null && appointment != null) {
                                            android.util.Log.d("ConfirmAppointment", "Creating notifications for patient and doctor")
                                            val formattedSlotTime = formatTimeRange(timeRange)
                                            val (patientNotifId, doctorNotifId) = NotificationRepository().createNotificationForBoth(
                                                patientUid = currentUser.uid,
                                                doctorUid = doctorId,
                                                patientTitle = "Appointment Booked",
                                                patientMessage = "Your appointment with $doctorName on $formattedDate at $formattedSlotTime has been confirmed.".let {
                                                    if (dependentName.isNotBlank()) "$it (for $dependentName)" else it
                                                },
                                                doctorTitle = "New Appointment",
                                                doctorMessage = "New appointment booked by patient on $formattedDate at $formattedSlotTime.".let {
                                                    if (dependentName.isNotBlank()) "$it (for $dependentName)" else it
                                                },
                                                type = "appointment_created",
                                                appointmentId = appointment.appointmentId
                                            )
                                            android.util.Log.d("ConfirmAppointment", "Notifications created: patient=$patientNotifId, doctor=$doctorNotifId")
                                            Toast.makeText(context, "Appointment booked! Check notifications.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.util.Log.e("ConfirmAppointment", "Cannot create notifications - user or appointment is null")
                                        }
                                    } catch (notifError: Exception) {
                                        // Log but don't fail - notification is not critical and likely already created
                                        android.util.Log.d("ConfirmAppointment", "Notification error (can be ignored): ${notifError.message}")
                                    }
                                    
                                    // Navigate to success screen and clear booking flow from back stack
                                    val recipientType = if (dependentId.isBlank() || dependentId == "self") "self" else "dependent"
                                    navController.navigate("appointment_success/$doctorName/$formattedDate/$blockName/$timeRange/$recipientType") {
                                        popUpTo("patient_home") { inclusive = false }
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to create appointment. Please try again.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message ?: "Unknown error occurred"}", Toast.LENGTH_LONG).show()
                                e.printStackTrace()
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

                    // Show who the appointment is for
                    if (dependentId.isNotBlank() && dependentId != "self" && dependentName.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Appointment For",
                            value = dependentName
                        )
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Booked By",
                            value = patientName
                        )
                    } else {
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "Patient",
                            value = patientName
                        )
                    }

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
                        label = "Time Block",
                        value = blockName
                    )

                    DetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Time Range",
                        value = formatTimeRange(timeRange)
                    )

                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = "IATZ Hospital"
                    )
                    
                    // Price section with highlighted background
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Consultation Fee",
                                    fontSize = 15.sp,
                                    color = StatTextColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Rs. 1,500",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            // Important Notice - Appointment Time Slot
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE3F2FD),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Your time slot is ${formatTimeRange(timeRange)}. Please arrive 10 minutes early.",
                        fontSize = 13.sp,
                        color = Color(0xFF0D47A1),
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Medical Files Access Notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF9E6),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Once your appointment is booked, the doctor will have access to your medical files.",
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 18.sp
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

/**
 * Convert time range from 24-hour format (HH:mm - HH:mm) to 12-hour AM/PM format
 * Example: "08:00 - 10:00" -> "8:00 AM - 10:00 AM"
 */
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
        
        val parts = cleaned.split("-").map { it.trim() }
        if (parts.size == 2) {
            val startTime = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"))
            val formatter = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
            "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        } else {
            cleaned
        }
    } catch (e: Exception) {
        android.util.Log.e("ConfirmAppointment", "Error formatting time range: $timeRange", e)
        timeRange
    }
}
