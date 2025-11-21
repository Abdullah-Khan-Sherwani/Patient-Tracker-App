package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.AppointmentRepository
import com.example.patienttracker.ui.components.AMPMTimePicker
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Design Colors
private val BackgroundColor = Color(0xFFDDD2CE)
private val SurfaceColor = Color(0xFFF7ECE8)
private val PrimaryColor = Color(0xFF2F2019)
private val AccentColor = Color(0xFFB36B3C)
private val BorderColor = Color(0xFF9E8B82)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedBookAppointmentScreen(
    navController: NavController,
    context: Context,
    doctorUid: String,
    doctorName: String,
    speciality: String
) {
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Generate available dates (next 30 days)
    val availableDates = remember {
        val dates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        for (i in 0 until 30) {
            dates.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        dates
    }

    // Time slots (9 AM to 5 PM, hourly)
    val timeSlots = remember {
        listOf(
            "09:00 AM - 10:00 AM",
            "10:00 AM - 11:00 AM",
            "11:00 AM - 12:00 PM",
            "12:00 PM - 01:00 PM",
            "01:00 PM - 02:00 PM",
            "02:00 PM - 03:00 PM",
            "03:00 PM - 04:00 PM",
            "04:00 PM - 05:00 PM"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Book Appointment", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = PrimaryColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        
        if (showSuccess) {
            // Success Dialog
            AlertDialog(
                onDismissRequest = { 
                    showSuccess = false
                    navController.popBackStack()
                },
                title = { 
                    Text(
                        "Success!", 
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    ) 
                },
                text = { 
                    Text(
                        "Your appointment has been booked successfully!",
                        color = PrimaryColor
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showSuccess = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = SurfaceColor,
                shape = RoundedCornerShape(28.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Doctor Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Doctor Avatar
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(30.dp),
                            color = AccentColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = doctorName.split(" ").mapNotNull { it.firstOrNull() }
                                        .take(2).joinToString(""),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = doctorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PrimaryColor
                            )
                            Text(
                                text = speciality,
                                fontSize = 14.sp,
                                color = AccentColor
                            )
                        }
                    }
                }
            }

            // Select Date Section
            item {
                Text(
                    text = "Select Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryColor
                )
            }

            item {
                // Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDates.take(14)) { date ->
                        DateCell(
                            date = date,
                            isSelected = selectedDate == date,
                            onClick = { 
                                selectedDate = date
                                selectedTimeSlot = null // Reset time when date changes
                            }
                        )
                    }
                }
            }

            // Select Time Section (with improved AM/PM picker)
            if (selectedDate != null) {
                item {
                    Text(
                        text = "Select Time Slot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryColor
                    )
                }

                item {
                    AMPMTimePicker(
                        initialTime = selectedTimeSlot ?: "09:00 AM",
                        onTimeSelected = { time ->
                            selectedTimeSlot = time
                        },
                        primaryColor = AccentColor,
                        backgroundColor = SurfaceColor
                    )
                }

                // Optional Notes
                item {
                    Text(
                        text = "Notes (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryColor
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Add any notes for the doctor...") },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceColor,
                            focusedContainerColor = SurfaceColor,
                            unfocusedBorderColor = BorderColor,
                            focusedBorderColor = AccentColor
                        )
                    )
                }

                // Confirm Button
                item {
                    Button(
                        onClick = {
                            if (selectedDate != null && selectedTimeSlot != null) {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    val result = AppointmentRepository.createAppointment(
                                        doctorUid = doctorUid,
                                        doctorName = doctorName,
                                        speciality = speciality,
                                        appointmentDate = Timestamp(selectedDate!!),
                                        timeSlot = selectedTimeSlot!!,
                                        notes = notes
                                    )
                                    
                                    isLoading = false
                                    
                                    if (result.isSuccess) {
                                        showSuccess = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message 
                                            ?: "Failed to book appointment"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedDate != null && selectedTimeSlot != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor,
                            disabledContainerColor = BorderColor
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Confirm Booking",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateCell(
    date: Date,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.time = date
    
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryColor else SurfaceColor,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isSelected) Color.White else PrimaryColor
            )
            Text(
                text = dayOfWeek,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else AccentColor
            )
        }
    }
}

@Composable
fun TimeSlotChip(
    timeSlot: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (isSelected) PrimaryColor else SurfaceColor,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = timeSlot,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else PrimaryColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
