package com.example.patienttracker.ui.screens.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentRepository
import com.google.firebase.Timestamp
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

data class PatientItem(val uid: String, val name: String, val phone: String, val id: String)
data class DoctorItem(val uid: String, val name: String, val phone: String, val id: String, val speciality: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateAppointmentScreen(navController: NavController, context: Context) {
    var patients by remember { mutableStateOf<List<PatientItem>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<DoctorItem>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<PatientItem?>(null) }
    var selectedDoctor by remember { mutableStateOf<DoctorItem?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showPatientDialog by remember { mutableStateOf(false) }
    var showDoctorDialog by remember { mutableStateOf(false) }
    var patientSearchQuery by remember { mutableStateOf("") }
    var doctorSearchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = Firebase.firestore
                
                // Load patients
                val patientsSnapshot = db.collection("users")
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                
                patients = patientsSnapshot.documents.mapNotNull { doc ->
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val phone = doc.getString("phone") ?: "N/A"
                    val name = "$firstName $lastName".trim()
                    if (name.isNotEmpty()) {
                        PatientItem(doc.id, name, phone, doc.id.take(8))
                    } else null
                }.sortedBy { it.name }
                
                // Load doctors
                val doctorsSnapshot = db.collection("users")
                    .whereEqualTo("role", "doctor")
                    .get()
                    .await()
                
                doctors = doctorsSnapshot.documents.mapNotNull { doc ->
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val phone = doc.getString("phone") ?: "N/A"
                    val speciality = doc.getString("speciality") ?: doc.getString("specialization") ?: "General"
                    val name = "Dr. $firstName $lastName".trim()
                    if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                        DoctorItem(doc.id, name, phone, doc.id.take(8), speciality)
                    } else null
                }.sortedBy { it.name }
                
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Appointment", fontWeight = FontWeight.Bold) },
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Error/Success Messages
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

                // Patient Selection
                Text("Select Patient", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPatientDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = CardColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPatient?.name ?: "Choose patient",
                            color = if (selectedPatient == null) TextSecondary else TextPrimary,
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentColor)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Doctor Selection
                Text("Select Doctor", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDoctorDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = CardColor
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
                                    text = selectedDoctor?.name ?: "Choose doctor",
                                    color = if (selectedDoctor == null) TextSecondary else TextPrimary,
                                    fontSize = 16.sp
                                )
                                selectedDoctor?.let {
                                    Text(
                                        text = it.speciality,
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentColor)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Date Selection
                Text("Select Date", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val date = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }
                                    selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = CardColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.ifEmpty { "Choose date" },
                            color = if (selectedDate.isEmpty()) TextSecondary else TextPrimary,
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentColor)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Time Selection
                Text("Select Time", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val calendar = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val time = String.format("%02d:%02d", hour, minute)
                                    selectedTime = time
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = CardColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTime.ifEmpty { "Choose time" },
                            color = if (selectedTime.isEmpty()) TextSecondary else TextPrimary,
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentColor)
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Create Button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        
                        when {
                            selectedPatient == null -> errorMessage = "Please select a patient"
                            selectedDoctor == null -> errorMessage = "Please select a doctor"
                            selectedDate.isEmpty() -> errorMessage = "Please select a date"
                            selectedTime.isEmpty() -> errorMessage = "Please select a time"
                            else -> {
                                isSaving = true
                                scope.launch {
                                    try {
                                        // Parse date and time
                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        val appointmentDateTime = dateFormat.parse("$selectedDate $selectedTime")
                                        val timestamp = Timestamp(appointmentDateTime ?: Date())
                                        
                                        // Create appointment
                                        val result = AppointmentRepository.createAppointment(
                                            doctorUid = selectedDoctor!!.uid,
                                            doctorName = selectedDoctor!!.name,
                                            speciality = selectedDoctor!!.speciality,
                                            appointmentDate = timestamp,
                                            timeSlot = selectedTime,
                                            notes = "Created by admin"
                                        )
                                        
                                        if (result.isSuccess) {
                                            successMessage = "Appointment created successfully for ${selectedPatient!!.name} with ${selectedDoctor!!.name}"
                                            
                                            // Send notification to patient
                                            try {
                                                val db = Firebase.firestore
                                                val notificationData = hashMapOf(
                                                    "patientUid" to selectedPatient!!.uid,
                                                    "title" to "Appointment Confirmed",
                                                    "body" to "Your appointment with ${selectedDoctor!!.name} has been scheduled on $selectedDate at $selectedTime.",
                                                    "timestamp" to com.google.firebase.Timestamp.now(),
                                                    "isRead" to false,
                                                    "type" to "appointment_created"
                                                )
                                                db.collection("notifications")
                                                    .add(notificationData)
                                                    .await()
                                            } catch (e: Exception) {
                                                // Notification failed but appointment succeeded - log error but don't fail
                                                android.util.Log.e("AdminCreateAppointment", "Failed to send notification: ${e.message}")
                                            }
                                            
                                            // Navigate after delay
                                            kotlinx.coroutines.delay(2000)
                                            navController.navigate("admin_all_appointments") {
                                                popUpTo("admin_create_appointment") { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = "Failed to create appointment: ${result.exceptionOrNull()?.message}"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Appointment", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Patient Selection Bottom Sheet
    if (showPatientDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = { showPatientDialog = false })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.BottomCenter)
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BgColor,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Title
                    Text(
                        text = "Select Patient",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = patientSearchQuery,
                        onValueChange = { patientSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, phone, or ID", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentColor) },
                        trailingIcon = {
                            if (patientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { patientSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Filtered Patient List
                    val filteredPatients = patients.filter {
                        it.name.contains(patientSearchQuery, ignoreCase = true) ||
                        it.phone.contains(patientSearchQuery, ignoreCase = true) ||
                        it.id.contains(patientSearchQuery, ignoreCase = true)
                    }
                    
                    if (filteredPatients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No results found",
                                    fontSize = 16.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPatients) { patient ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPatient = patient
                                            showPatientDialog = false
                                            patientSearchQuery = ""
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = patient.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = patient.phone,
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "ID: ${patient.id}",
                                                fontSize = 12.sp,
                                                color = TextSecondary.copy(alpha = 0.7f)
                                            )
                                        }
                                        Icon(
                                            Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Select",
                                            tint = AccentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Cancel Button
                    TextButton(
                        onClick = {
                            showPatientDialog = false
                            patientSearchQuery = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // Doctor Selection Bottom Sheet
    if (showDoctorDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = { showDoctorDialog = false })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.BottomCenter)
                    .clickable(onClick = {}),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BgColor,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Title
                    Text(
                        text = "Select Doctor",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = doctorSearchQuery,
                        onValueChange = { doctorSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by name, phone, or ID", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentColor) },
                        trailingIcon = {
                            if (doctorSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { doctorSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentColor,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Filtered Doctor List
                    val filteredDoctors = doctors.filter {
                        it.name.contains(doctorSearchQuery, ignoreCase = true) ||
                        it.phone.contains(doctorSearchQuery, ignoreCase = true) ||
                        it.id.contains(doctorSearchQuery, ignoreCase = true) ||
                        it.speciality.contains(doctorSearchQuery, ignoreCase = true)
                    }
                    
                    if (filteredDoctors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No results found",
                                    fontSize = 16.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredDoctors) { doctor ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDoctor = doctor
                                            showDoctorDialog = false
                                            doctorSearchQuery = ""
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doctor.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = doctor.phone,
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Row {
                                                Text(
                                                    text = "ID: ${doctor.id}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = " • ",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = doctor.speciality,
                                                    fontSize = 12.sp,
                                                    color = AccentColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Select",
                                            tint = AccentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Cancel Button
                    TextButton(
                        onClick = {
                            showDoctorDialog = false
                            doctorSearchQuery = ""
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
