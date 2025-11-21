package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

data class TimeSlot(
    val time: String,
    val isAvailable: Boolean = true
)

data class DateOption(
    val date: LocalDate,
    val dayOfWeek: String,
    val dayOfMonth: String
)

@Composable
fun SelectDateTimeScreen(
    navController: NavController,
    context: Context,
    doctorId: String,
    doctorFirstName: String,
    doctorLastName: String,
    specialty: String
) {
    val doctorFullName = "Dr. $doctorFirstName $doctorLastName"
    
    // Generate next 14 days
    val dates = remember {
        (0..13).map { dayOffset ->
            val date = LocalDate.now().plusDays(dayOffset.toLong())
            DateOption(
                date = date,
                dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEE")),
                dayOfMonth = date.dayOfMonth.toString()
            )
        }
    }

    var selectedDate by remember { mutableStateOf(dates[0]) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }

    // Helper function to check if time slot is in the past for today
    val isSlotAvailable: (String, LocalDate) -> Boolean = { timeSlot, date ->
        val currentTime = java.time.LocalTime.now()
        val currentDate = LocalDate.now()
        
        if (date.isEqual(currentDate)) {
            // For today, parse the time and check if it's in the future
            try {
                val slotTime = java.time.LocalTime.parse(
                    timeSlot.replace(" AM", "").replace(" PM", ""),
                    java.time.format.DateTimeFormatter.ofPattern("hh:mm")
                )
                val adjustedSlotTime = if (timeSlot.contains("PM") && !timeSlot.startsWith("12")) {
                    slotTime.plusHours(12)
                } else if (timeSlot.contains("AM") && timeSlot.startsWith("12")) {
                    slotTime.minusHours(12)
                } else {
                    slotTime
                }
                adjustedSlotTime.isAfter(currentTime.plusMinutes(30)) // Need at least 30 min advance booking
            } catch (e: Exception) {
                true
            }
        } else {
            true // Future dates are always available
        }
    }

    // Sample time slots by period
    val morningSlots = listOf(
        "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM"
    ).map { TimeSlot(it, isSlotAvailable(it, selectedDate.date)) }

    val afternoonSlots = listOf(
        "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM", "04:00 PM", "04:30 PM"
    ).map { TimeSlot(it, isSlotAvailable(it, selectedDate.date)) }

    val eveningSlots = listOf(
        "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM"
    ).map { TimeSlot(it, isSlotAvailable(it, selectedDate.date)) }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Column(
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
                    Text(
                        text = "Select Date & Time",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = doctorFullName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
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
                        selectedTimeSlot?.let { time ->
                            val dateStr = selectedDate.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            navController.navigate("confirm_appointment/$doctorId/$doctorFullName/$specialty/$dateStr/$time")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    enabled = selectedTimeSlot != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedTimeSlot != null) "Continue" else "Select a time slot",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
        ) {
            // Horizontal date selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { dateOption ->
                    DateCard(
                        dateOption = dateOption,
                        isSelected = selectedDate == dateOption,
                        onClick = {
                            selectedDate = dateOption
                            selectedTimeSlot = null // Reset time selection
                        }
                    )
                }
            }

            Divider(color = Color.LightGray, thickness = 1.dp)

            // Time slots
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TimeSlotSection(
                        title = "Morning",
                        slots = morningSlots,
                        selectedSlot = selectedTimeSlot,
                        onSlotSelected = { selectedTimeSlot = it }
                    )
                }

                item {
                    TimeSlotSection(
                        title = "Afternoon",
                        slots = afternoonSlots,
                        selectedSlot = selectedTimeSlot,
                        onSlotSelected = { selectedTimeSlot = it }
                    )
                }

                item {
                    TimeSlotSection(
                        title = "Evening",
                        slots = eveningSlots,
                        selectedSlot = selectedTimeSlot,
                        onSlotSelected = { selectedTimeSlot = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DateCard(dateOption: DateOption, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(64.dp)
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ButtonColor else CardWhite,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dateOption.dayOfWeek,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else StatTextColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateOption.dayOfMonth,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else StatTextColor
            )
        }
    }
}

@Composable
fun TimeSlotSection(
    title: String,
    slots: List<TimeSlot>,
    selectedSlot: String?,
    onSlotSelected: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = StatTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slots.chunked(3).forEach { row ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { slot ->
                        TimeSlotChip(
                            slot = slot,
                            isSelected = selectedSlot == slot.time,
                            onClick = { if (slot.isAvailable) onSlotSelected(slot.time) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSlotChip(slot: TimeSlot, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(enabled = slot.isAvailable) { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = when {
            !slot.isAvailable -> Color.Gray.copy(alpha = 0.2f)
            isSelected -> ButtonColor
            else -> CardWhite
        },
        border = if (!isSelected && slot.isAvailable) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray) else null,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = slot.time,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    !slot.isAvailable -> Color.Gray
                    isSelected -> Color.White
                    else -> StatTextColor
                }
            )
        }
    }
}
