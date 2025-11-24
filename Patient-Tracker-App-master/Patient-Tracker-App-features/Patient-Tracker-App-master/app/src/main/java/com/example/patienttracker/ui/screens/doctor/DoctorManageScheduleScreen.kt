package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
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

data class TimeSlot(
    val date: String,
    val time: String,
    val isAvailable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorManageScheduleScreen(
    navController: NavController,
    context: Context
) {
    var timeSlots by remember { mutableStateOf<List<TimeSlot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    // Load schedule from Firebase
                    // For now, showing sample data
                    timeSlots = generateSampleSchedule()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Time Slot")
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
                    text = "Your Available Time Slots",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (timeSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
                                text = "No time slots scheduled",
                                fontSize = 18.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add availability",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    val groupedSlots = timeSlots.groupBy { it.date }
                    
                    groupedSlots.forEach { (date, slots) ->
                        DateScheduleCard(
                            date = date,
                            timeSlots = slots,
                            onRemoveSlot = { slot ->
                                timeSlots = timeSlots.filter { it != slot }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTimeSlotDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, time ->
                timeSlots = timeSlots + TimeSlot(date, time, true)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DateScheduleCard(
    date: String,
    timeSlots: List<TimeSlot>,
    onRemoveSlot: (TimeSlot) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = date,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            timeSlots.forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = slot.time,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    
                    IconButton(
                        onClick = { onRemoveSlot(slot) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTimeSlotDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        title = {
            Text("Add Time Slot", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Select date and time for availability",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Date (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        focusedLabelColor = AccentColor
                    )
                )
                
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Time (HH:MM AM/PM)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        focusedLabelColor = AccentColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                        onConfirm(selectedDate, selectedTime)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun generateSampleSchedule(): List<TimeSlot> {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val slots = mutableListOf<TimeSlot>()
    
    // Generate slots for next 7 days
    for (i in 0..6) {
        val date = dateFormat.format(calendar.time)
        slots.add(TimeSlot(date, "09:00 AM", true))
        slots.add(TimeSlot(date, "11:00 AM", true))
        slots.add(TimeSlot(date, "02:00 PM", true))
        slots.add(TimeSlot(date, "04:00 PM", true))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return slots
}
