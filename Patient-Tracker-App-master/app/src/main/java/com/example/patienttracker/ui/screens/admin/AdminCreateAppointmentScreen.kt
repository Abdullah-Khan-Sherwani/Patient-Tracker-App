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
import com.example.patienttracker.data.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Teal/Mint Theme Colors
private val BgColor = Color(0xFFF4F6F7)        // Light gray background
private val CardColor = Color(0xFFFFFFFF)      // White cards
private val AccentColor = Color(0xFF04786A)    // Teal accent
private val TextPrimary = Color(0xFF0E4944)    // Deep teal text
private val TextSecondary = Color(0xFF6B7280)  // Gray secondary text
private val MintAccent = Color(0xFF76DCB0)     // Mint green accent

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
    var availableTimeSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingSlots by remember { mutableStateOf(false) }
    var doctorUnavailableMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showPatientDialog by remember { mutableStateOf(false) }
    var showDoctorDialog by remember { mutableStateOf(false) }
    var patientSearchQuery by remember { mutableStateOf("") }
    var doctorSearchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Load time slots when doctor and date are selected
    LaunchedEffect(selectedDoctor, selectedDate) {
        if (selectedDoctor != null && selectedDate.isNotEmpty()) {
            isLoadingSlots = true
            doctorUnavailableMessage = null
            selectedTime = ""
            scope.launch {
                try {
                    val slots = loadAvailableTimeSlots(
                        doctorUid = selectedDoctor!!.uid,
                        dateString = selectedDate
                    )
                    availableTimeSlots = slots
                    if (slots.isEmpty()) {
                        doctorUnavailableMessage = "Doctor not available on selected date"
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to load time slots: ${e.message}"
                    availableTimeSlots = emptyList()
                } finally {
                    isLoadingSlots = false
                }
            }
        } else {
            availableTimeSlots = emptyList()
            doctorUnavailableMessage = null
        }
    }

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
                title = { Text("Create Appointment", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AccentColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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

                // Time Slot Selection
                Text("Select Time Slot", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                
                if (selectedDoctor == null || selectedDate.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardColor.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Select doctor and date first",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (isLoadingSlots) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Loading available slots...", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else if (doctorUnavailableMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(doctorUnavailableMessage!!, color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                } else if (availableTimeSlots.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("No available slots for this date", color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                } else {
                    // Time Slot Grid
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CardColor,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Available Slots (${availableTimeSlots.size})",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Grid layout for time slots
                            val rows = availableTimeSlots.chunked(3)
                            rows.forEach { rowSlots ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSlots.forEach { slot ->
                                        TimeSlotChip(
                                            time = slot,
                                            isSelected = selectedTime == slot,
                                            onSelect = { selectedTime = slot },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    // Fill empty spaces
                                    repeat(3 - rowSlots.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
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
                                            val appointment = result.getOrNull()
                                            successMessage = "Appointment created successfully for ${selectedPatient!!.name} with ${selectedDoctor!!.name}"
                                            
                                            // Send notifications to both patient and doctor
                                            try {
                                                NotificationRepository().createNotificationForBoth(
                                                    patientUid = selectedPatient!!.uid,
                                                    doctorUid = selectedDoctor!!.uid,
                                                    patientTitle = "Appointment Confirmed",
                                                    patientMessage = "Your appointment with Dr. ${selectedDoctor!!.name} has been scheduled on $selectedDate at $selectedTime.",
                                                    doctorTitle = "New Appointment",
                                                    doctorMessage = "New appointment scheduled with ${selectedPatient!!.name} on $selectedDate at $selectedTime.",
                                                    type = "appointment_created",
                                                    appointmentId = appointment?.appointmentId ?: ""
                                                )
                                            } catch (e: Exception) {
                                                // Notification failed but appointment succeeded - log error but don't fail
                                                android.util.Log.e("AdminCreateAppointment", "Failed to send notifications: ${e.message}")
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

@Composable
private fun TimeSlotChip(
    time: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AccentColor else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) AccentColor else AccentColor.copy(alpha = 0.3f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = time,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

private suspend fun loadAvailableTimeSlots(
    doctorUid: String,
    dateString: String
): List<String> {
    try {
        // Parse the date to get day of week
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString) ?: return emptyList()
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        
        // Load doctor availability for this day
        val db = Firebase.firestore
        val availabilityDoc = db.collection("doctor_availability")
            .document("${doctorUid}_$dayOfWeek")
            .get()
            .await()
        
        if (!availabilityDoc.exists()) {
            return emptyList()
        }
        
        val isActive = availabilityDoc.getBoolean("isActive") ?: false
        if (!isActive) {
            return emptyList()
        }
        
        val startTime = availabilityDoc.getString("startTime") ?: "09:00"
        val endTime = availabilityDoc.getString("endTime") ?: "17:00"
        
        // Generate 30-minute time slots
        val slots = generateTimeSlots(startTime, endTime, 30)
        
        // Load existing appointments for this doctor on this date
        val existingAppointments = db.collection("appointments")
            .whereEqualTo("doctorUid", doctorUid)
            .get()
            .await()
        
        val bookedSlots = existingAppointments.documents.mapNotNull { doc ->
            val appointmentDate = doc.getTimestamp("appointmentDate")
            if (appointmentDate != null) {
                val appointmentCal = Calendar.getInstance().apply { time = appointmentDate.toDate() }
                val appointmentDateStr = dateFormat.format(appointmentCal.time)
                
                if (appointmentDateStr == dateString) {
                    doc.getString("timeSlot")
                } else null
            } else null
        }.toSet()
        
        // Filter out booked slots
        return slots.filter { it !in bookedSlots }
        
    } catch (e: Exception) {
        android.util.Log.e("AdminCreateAppointment", "Error loading time slots: ${e.message}")
        return emptyList()
    }
}

private fun generateTimeSlots(startTime: String, endTime: String, intervalMinutes: Int): List<String> {
    val slots = mutableListOf<String>()
    
    val startParts = startTime.split(":")
    val endParts = endTime.split(":")
    
    val startHour = startParts[0].toIntOrNull() ?: 9
    val startMinute = startParts[1].toIntOrNull() ?: 0
    val endHour = endParts[0].toIntOrNull() ?: 17
    val endMinute = endParts[1].toIntOrNull() ?: 0
    
    val startTotalMinutes = startHour * 60 + startMinute
    val endTotalMinutes = endHour * 60 + endMinute
    
    var currentMinutes = startTotalMinutes
    while (currentMinutes < endTotalMinutes) {
        val hour = currentMinutes / 60
        val minute = currentMinutes % 60
        slots.add(String.format("%02d:%02d", hour, minute))
        currentMinutes += intervalMinutes
    }
    
    return slots
}
