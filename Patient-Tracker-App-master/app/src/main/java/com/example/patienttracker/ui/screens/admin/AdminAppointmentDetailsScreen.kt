package com.example.patienttracker.ui.screens.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppointmentDetailsScreen(
    navController: NavController,
    context: Context,
    appointmentId: String
) {
    var appointment by remember { mutableStateOf<Appointment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appointmentId) {
        scope.launch {
            try {
                val db = Firebase.firestore
                val doc = db.collection("appointments").document(appointmentId).get().await()
                
                if (doc.exists()) {
                    appointment = Appointment.fromFirestore(doc.data ?: emptyMap(), doc.id)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load appointment: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment Details", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else if (appointment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Appointment not found",
                        fontSize = 18.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            val isUpcoming = appointment!!.appointmentDate.toDate().after(Date())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Status Badge
                val statusColor = when (appointment!!.status.lowercase()) {
                    "completed" -> Color(0xFF10B981)
                    "cancelled" -> Color(0xFFEF4444)
                    "scheduled" -> AccentColor
                    else -> TextSecondary
                }
                
                Surface(
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = appointment!!.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Patient Info
                DetailCard(
                    icon = Icons.Default.Person,
                    title = "Patient",
                    content = appointment!!.patientName
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Doctor Info
                DetailCard(
                    icon = Icons.Default.AccountBox,
                    title = "Doctor",
                    content = appointment!!.doctorName,
                    subtitle = appointment!!.speciality
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Date & Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailCard(
                        icon = Icons.Default.DateRange,
                        title = "Date",
                        content = appointment!!.getFormattedDate(),
                        modifier = Modifier.weight(1f)
                    )
                    
                    DetailCard(
                        icon = Icons.Default.Info,
                        title = "Time",
                        content = appointment!!.timeSlot,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Type (Fixed as In person)
                DetailCard(
                    icon = Icons.Default.Info,
                    title = "Type",
                    content = "In person"
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Notes (if any)
                if (appointment!!.notes.isNotEmpty()) {
                    DetailCard(
                        icon = Icons.Default.Info,
                        title = "Notes",
                        content = appointment!!.notes
                    )
                    Spacer(Modifier.height(16.dp))
                }
                
                // Success Message
                successMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = Color(0xFF10B981), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                // Error Message
                errorMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Action Buttons
                if (isUpcoming && appointment!!.status.lowercase() != "cancelled") {
                    // Cancel Appointment Button
                    Button(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCancelling
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cancel Appointment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    // Past appointment indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (appointment!!.status.lowercase() == "completed") {
                            Color(0xFF10B981).copy(alpha = 0.1f)
                        } else {
                            TextSecondary.copy(alpha = 0.1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (appointment!!.status.lowercase() == "completed") {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = if (appointment!!.status.lowercase() == "completed") {
                                    Color(0xFF10B981)
                                } else {
                                    TextSecondary
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (appointment!!.status.lowercase() == "completed") {
                                    "This appointment has been completed"
                                } else {
                                    "This appointment is in the past"
                                },
                                color = if (appointment!!.status.lowercase() == "completed") {
                                    Color(0xFF10B981)
                                } else {
                                    TextSecondary
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = CardColor,
            title = { Text("Cancel Appointment?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to cancel this appointment? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCancelling = true
                        showCancelDialog = false
                        scope.launch {
                            try {
                                val db = Firebase.firestore
                                val currentAppointment = appointment
                                
                                // Update appointment status with cancellation timestamp
                                db.collection("appointments").document(appointmentId)
                                    .update(
                                        mapOf(
                                            "status" to "Cancelled",
                                            "updatedAt" to com.google.firebase.Timestamp.now(),
                                            "cancelledAt" to com.google.firebase.Timestamp.now(),
                                            "cancelledBy" to "admin"
                                        )
                                    )
                                    .await()
                                
                                // Send notifications to both patient and doctor
                                if (currentAppointment != null) {
                                    try {
                                        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault())
                                        val formattedDate = dateFormat.format(currentAppointment.appointmentDate.toDate())
                                        
                                        NotificationRepository().createNotificationForBoth(
                                            patientUid = currentAppointment.patientUid,
                                            doctorUid = currentAppointment.doctorUid,
                                            patientTitle = "Appointment Cancelled",
                                            patientMessage = "Your appointment with Dr. ${currentAppointment.doctorName} scheduled on $formattedDate at ${currentAppointment.timeSlot} has been cancelled.",
                                            doctorTitle = "Appointment Cancelled",
                                            doctorMessage = "Your appointment with ${currentAppointment.patientName} scheduled on $formattedDate at ${currentAppointment.timeSlot} has been cancelled.",
                                            type = "appointment_cancelled",
                                            appointmentId = appointmentId
                                        )
                                    } catch (e: Exception) {
                                        // Notification failed but appointment cancelled successfully
                                        android.util.Log.e("AdminAppointmentDetails", "Failed to send notifications: ${e.message}")
                                    }
                                }
                                
                                // Reload appointment to update UI
                                val doc = db.collection("appointments").document(appointmentId).get().await()
                                if (doc.exists()) {
                                    appointment = Appointment.fromFirestore(doc.data ?: emptyMap(), doc.id)
                                }
                                
                                successMessage = "Appointment cancelled successfully."
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "Failed to cancel appointment. Please check your connection and try again."
                                successMessage = null
                            } finally {
                                isCancelling = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = !isCancelling
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep It", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailCard(
    icon: ImageVector,
    title: String,
    content: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AccentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = content,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
