package com.example.patienttracker.ui.screens.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorAvailability
import com.example.patienttracker.data.Specializations
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class for availability day (must be outside composable)
data class AvailabilityDay(
    val dayOfWeek: Int,
    val dayName: String,
    var isActive: Boolean,
    var startTime: String,
    var endTime: String
)

/**
 * Admin screen to add a new doctor to the system.
 * Admin provides doctor details and creates credentials.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDoctorScreen(navController: NavController, context: Context) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedSpecialties by remember { mutableStateOf<Set<String>>(emptySet()) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSpecializationDropdown by remember { mutableStateOf(false) }
    
    // Availability states for 7 days (Monday to Sunday)
    val availabilityList = remember {
        mutableStateListOf(
            AvailabilityDay(1, "Monday", true, "09:00", "17:00"),
            AvailabilityDay(2, "Tuesday", true, "09:00", "17:00"),
            AvailabilityDay(3, "Wednesday", true, "09:00", "17:00"),
            AvailabilityDay(4, "Thursday", true, "09:00", "17:00"),
            AvailabilityDay(5, "Friday", true, "09:00", "17:00"),
            AvailabilityDay(6, "Saturday", false, "09:00", "13:00"),
            AvailabilityDay(7, "Sunday", false, "09:00", "13:00")
        )
    }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Doctor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAF8F3),
                    titleContentColor = Color(0xFFB8956A),
                    navigationIconContentColor = Color(0xFFB8956A)
                )
            )
        },
        containerColor = Color(0xFFFAF8F3)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Register a new doctor in the system",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Form Fields
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Specialization Multi-Select
            Text(
                text = "Select Specializations",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2F2019)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpecializationDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F0E8),
                    shadowElevation = 2.dp
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
                            Text(
                                text = if (selectedSpecialties.isEmpty()) "Choose specializations" else "${selectedSpecialties.size} selected",
                                color = if (selectedSpecialties.isEmpty()) Color(0xFF6B7280) else Color(0xFF2F2019),
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFFB8956A)
                            )
                        }
                        
                        // Display selected specialties as chips
                        if (selectedSpecialties.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedSpecialties.forEach { specialty ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFB8956A).copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = specialty,
                                                fontSize = 12.sp,
                                                color = Color(0xFF2F2019)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF2F2019),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        selectedSpecialties = selectedSpecialties - specialty
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showSpecializationDropdown,
                    onDismissRequest = { showSpecializationDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White)
                ) {
                    Specializations.list.forEach { spec ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(spec)
                                    if (selectedSpecialties.contains(spec)) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFB8956A)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedSpecialties = if (selectedSpecialties.contains(spec)) {
                                    selectedSpecialties - spec
                                } else {
                                    selectedSpecialties + spec
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Working Hours Section
            Text(
                text = "Availability",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F2019)
            )
            Text(
                text = "Set weekly working hours",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Weekly Timetable
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF5F0E8),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    availabilityList.forEachIndexed { index, day ->
                        AvailabilityDayRow(
                            day = day,
                            onToggle = { isActive ->
                                availabilityList[index] = day.copy(isActive = isActive)
                            },
                            onStartTimeChange = { time ->
                                availabilityList[index] = day.copy(startTime = time)
                            },
                            onEndTimeChange = { time ->
                                availabilityList[index] = day.copy(endTime = time)
                            },
                            context = context
                        )
                        if (index < availabilityList.size - 1) {
                            Divider(
                                color = Color(0xFFB8956A).copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Temporary Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    scope.launch {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                            phone.isBlank() || selectedSpecialties.isEmpty() || password.isBlank()
                        ) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password.length < 8) {
                            Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        isLoading = true
                        try {
                            // 1) Create Firebase Auth user
                            val auth = Firebase.auth
                            val user = auth.createUserWithEmailAndPassword(email.trim(), password)
                                .await().user ?: throw IllegalStateException("Auth failed")

                            // 2) Generate doctor humanId
                            val humanId = nextHumanId("doctor")

                            // 3) Create doctor profile in Firestore
                            createDoctorProfile(
                                uid = user.uid,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                humanId = humanId,
                                specialities = selectedSpecialties.toList()
                            )

                            // 4) Save doctor availability
                            saveDoctorAvailability(user.uid, availabilityList)

                            Toast.makeText(context, "Doctor added successfully! ID: $humanId", Toast.LENGTH_LONG).show()
                            navController.popBackStack()

                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Failed to add doctor", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8956A)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                Text(
                    if (isLoading) "Adding..." else "Add Doctor",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back Button
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    }
}

// ============= Firestore Helpers =============

private suspend fun nextHumanId(role: String): String {
    val db = Firebase.firestore
    val ref = db.collection("counters").document(role)
    val next = db.runTransaction { tx ->
        val snap = tx.get(ref)
        val current = snap.getLong("next") ?: 1L
        tx.set(ref, mapOf("next" to (current + 1)), SetOptions.merge())
        current
    }.await()
    return next.toString().padStart(6, '0')
}

private suspend fun createDoctorProfile(
    uid: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    humanId: String,
    specialities: List<String>
) {
    val db = Firebase.firestore
    val doc = mapOf(
        "role" to "doctor",
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "humanId" to humanId,
        "speciality" to specialities.joinToString(", "), // Store as comma-separated for backward compatibility
        "specialities" to specialities, // Store as array for future use
        "createdAt" to Timestamp.now()
    )
    db.collection("users").document(uid).set(doc).await()
}

private suspend fun saveDoctorAvailability(
    doctorUid: String,
    availabilityList: List<AvailabilityDay>
) {
    val db = Firebase.firestore
    val batch = db.batch()
    
    availabilityList.forEach { day ->
        val availabilityDoc = DoctorAvailability(
            doctorUid = doctorUid,
            dayOfWeek = day.dayOfWeek,
            isActive = day.isActive,
            startTime = day.startTime,
            endTime = day.endTime
        )
        
        val docRef = db.collection("doctor_availability")
            .document("${doctorUid}_${day.dayOfWeek}")
        batch.set(docRef, availabilityDoc.toMap(), SetOptions.merge())
    }
    
    batch.commit().await()
}

@Composable
private fun AvailabilityDayRow(
    day: AvailabilityDay,
    onToggle: (Boolean) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name and toggle
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = day.isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFB8956A),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD1D5DB)
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = day.dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (day.isActive) Color(0xFF2F2019) else Color(0xFF9CA3AF)
            )
        }
        
        if (day.isActive) {
            // Time pickers
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePickerButton(
                    time = day.startTime,
                    onTimeSelected = onStartTimeChange,
                    context = context
                )
                Text("-", color = Color(0xFF6B7280))
                TimePickerButton(
                    time = day.endTime,
                    onTimeSelected = onEndTimeChange,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun TimePickerButton(
    time: String,
    onTimeSelected: (String) -> Unit,
    context: Context
) {
    Surface(
        modifier = Modifier
            .clickable {
                val parts = time.split(":")
                val hour = parts[0].toIntOrNull() ?: 9
                val minute = parts[1].toIntOrNull() ?: 0
                
                android.app.TimePickerDialog(
                    context,
                    { _, h, m ->
                        onTimeSelected(String.format("%02d:%02d", h, m))
                    },
                    hour,
                    minute,
                    false
                ).show()
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB8956A).copy(alpha = 0.3f))
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = Color(0xFF2F2019),
            fontWeight = FontWeight.Medium
        )
    }
}
