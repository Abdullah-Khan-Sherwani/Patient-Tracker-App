package com.example.patienttracker.ui.screens.common

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorAvailability
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

data class AvailabilityDayState(
    val dayOfWeek: Int,
    val dayName: String,
    val isActive: Boolean,
    val startTime: String,
    val endTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAvailabilityScreen(
    navController: NavController,
    context: Context,
    doctorUid: String
) {
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val availabilityList = remember {
        mutableStateListOf(
            AvailabilityDayState(1, "Monday", false, "09:00", "17:00"),
            AvailabilityDayState(2, "Tuesday", false, "09:00", "17:00"),
            AvailabilityDayState(3, "Wednesday", false, "09:00", "17:00"),
            AvailabilityDayState(4, "Thursday", false, "09:00", "17:00"),
            AvailabilityDayState(5, "Friday", false, "09:00", "17:00"),
            AvailabilityDayState(6, "Saturday", false, "09:00", "13:00"),
            AvailabilityDayState(7, "Sunday", false, "09:00", "13:00")
        )
    }
    
    val scope = rememberCoroutineScope()
    
    // Load existing availability
    LaunchedEffect(doctorUid) {
        scope.launch {
            try {
                val db = Firebase.firestore
                val snapshot = db.collection("doctor_availability")
                    .whereEqualTo("doctorUid", doctorUid)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val availability = DoctorAvailability.fromFirestore(doc)
                    availability?.let { avail ->
                        val index = availabilityList.indexOfFirst { it.dayOfWeek == avail.dayOfWeek }
                        if (index >= 0) {
                            availabilityList[index] = availabilityList[index].copy(
                                isActive = avail.isActive,
                                startTime = avail.startTime,
                                endTime = avail.endTime
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load availability: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Availability", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                try {
                                    saveAvailability(doctorUid, availabilityList)
                                    Toast.makeText(context, "Availability updated successfully", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to save: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (isSaving) TextSecondary else AccentColor
                        )
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
                Text(
                    text = "Working Hours",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Set your weekly availability for appointments",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                
                errorMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                // Weekly Timetable
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardColor,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
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
                                    color = AccentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            errorMessage = null
                            try {
                                saveAvailability(doctorUid, availabilityList)
                                Toast.makeText(context, "Availability updated successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                errorMessage = "Failed to save: ${e.message}"
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isSaving
                ) {
                    Text(
                        if (isSaving) "Saving..." else "Save Availability",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityDayRow(
    day: AvailabilityDayState,
    onToggle: (Boolean) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    context: Context
) {
    Column {
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
                        checkedTrackColor = AccentColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD1D5DB)
                    )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = day.dayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (day.isActive) TextPrimary else Color(0xFF9CA3AF)
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
                    Text("-", color = TextSecondary, fontSize = 14.sp)
                    TimePickerButton(
                        time = day.endTime,
                        onTimeSelected = onEndTimeChange,
                        context = context
                    )
                }
            }
        }
        
        // Validation warning
        if (day.isActive) {
            val startMinutes = timeToMinutes(day.startTime)
            val endMinutes = timeToMinutes(day.endTime)
            if (endMinutes <= startMinutes) {
                Text(
                    text = "⚠ End time must be after start time",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 60.dp, top = 4.dp)
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
                
                TimePickerDialog(
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
        border = BorderStroke(1.dp, AccentColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private suspend fun saveAvailability(
    doctorUid: String,
    availabilityList: List<AvailabilityDayState>
) {
    val db = Firebase.firestore
    
    try {
        android.util.Log.d("EditAvailability", "Saving availability for doctor: $doctorUid")
        
        // Use individual writes instead of batch for better error handling
        // and to avoid batch permission issues
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
            
            android.util.Log.d("EditAvailability", "Setting document: ${docRef.path}")
            
            // Use set with merge instead of batch
            docRef.set(availabilityDoc.toMap(), com.google.firebase.firestore.SetOptions.merge()).await()
        }
        
        android.util.Log.d("EditAvailability", "All availability documents saved successfully")
        
    } catch (e: Exception) {
        android.util.Log.e("EditAvailability", "Error saving availability: ${e.message}", e)
        throw e
    }
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    val hours = parts[0].toIntOrNull() ?: 0
    val minutes = parts[1].toIntOrNull() ?: 0
    return hours * 60 + minutes
}
