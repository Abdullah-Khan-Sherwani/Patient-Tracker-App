package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.isSystemInDarkTheme

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val TealAccent = Color(0xFF0E4944)          // Deep Teal for available slots
private val BookedRed = Color(0xFFE53935)           // Red for booked slots
private val SelectedTeal = Color(0xFF0E4944)        // Deep teal for selected slot

// Default slot duration in minutes
private const val DEFAULT_SLOT_DURATION_MINUTES = 10

/**
 * Time slot data class representing a bookable time slot
 */
data class TimeSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isBooked: Boolean = false,
    val blockName: String = ""
) {
    fun getDisplayTime(): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return "${startTime.format(formatter)} – ${endTime.format(formatter)}"
    }
    
    fun getStartTimeString(): String = startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    fun getEndTimeString(): String = endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

/**
 * Time block definition with availability range
 */
data class TimeBlock(
    val name: String, // Morning, Afternoon, Evening, Night
    val startHour: Int, // Block boundary start (e.g., 6 for Morning)
    val endHour: Int, // Block boundary end (e.g., 12 for Morning)
    val doctorStartTime: String? = null, // Actual doctor availability start (e.g., "08:00")
    val doctorEndTime: String? = null, // Actual doctor availability end (e.g., "10:00")
    val isAvailable: Boolean = false,
    val currentBookings: Int = 0,
    val maxCapacity: Int = 0,
    val isFullyBooked: Boolean = false,
    val slots: List<TimeSlot> = emptyList() // Generated time slots for this block
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
    specialty: String,
    rescheduleAppointmentId: String? = null
) {
    val doctorFullName = "Dr. $doctorFirstName $doctorLastName"
    val scope = rememberCoroutineScope()
    val isRescheduling = rescheduleAppointmentId != null
    var isReschedulingInProgress by remember { mutableStateOf(false) }
    
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
    var selectedBlock by remember { mutableStateOf<TimeBlock?>(null) }
    var selectedSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var timeBlocks by remember { mutableStateOf<List<TimeBlock>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load time blocks when date changes
    LaunchedEffect(selectedDate, doctorId) {
        isLoading = true
        scope.launch {
            try {
                timeBlocks = loadTimeBlocksWithSlots(doctorId, selectedDate.date)
            } catch (e: Exception) {
                android.util.Log.e("SelectDateTime", "Error loading time blocks: ${e.message}", e)
                timeBlocks = listOf(
                    TimeBlock("Morning", 6, 12, isAvailable = false),
                    TimeBlock("Afternoon", 12, 16, isAvailable = false),
                    TimeBlock("Evening", 16, 20, isAvailable = false),
                    TimeBlock("Night", 20, 24, isAvailable = false)
                )
            } finally {
                isLoading = false
                selectedBlock = null
                selectedSlot = null
            }
        }
    }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isRescheduling) "Reschedule Appointment" else "Select Date & Time Slot",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = doctorFullName,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                    }
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
                        selectedSlot?.let { slot ->
                            selectedBlock?.let { block ->
                                val dateStr = selectedDate.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                // Combine slot start and end into timeRange format
                                val timeRange = "${slot.getStartTimeString()}-${slot.getEndTimeString()}"
                                
                                if (isRescheduling && rescheduleAppointmentId != null) {
                                    // Handle reschedule flow
                                    scope.launch {
                                        isReschedulingInProgress = true
                                        try {
                                            // Convert date string to Timestamp
                                            val localDate = LocalDate.parse(dateStr)
                                            val instant = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                                            val timestamp = com.google.firebase.Timestamp(instant.epochSecond, instant.nano)
                                            
                                            // Call reschedule
                                            com.example.patienttracker.data.AppointmentRepository.rescheduleAppointment(
                                                appointmentId = rescheduleAppointmentId,
                                                newDate = timestamp,
                                                newTimeSlot = timeRange,
                                                newBlockName = block.name
                                            ).getOrThrow()
                                            
                                            // Show success message
                                            android.widget.Toast.makeText(
                                                context,
                                                "Appointment rescheduled successfully",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            
                                            // Navigate back to appointments list
                                            navController.navigate("full_schedule") {
                                                popUpTo("full_schedule") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("SelectDateTime", "Reschedule failed: ${e.message}", e)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to reschedule: ${e.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isReschedulingInProgress = false
                                        }
                                    }
                                } else {
                                    // Normal booking flow
                                    val encodedTimeRange = URLEncoder.encode(timeRange, StandardCharsets.UTF_8.toString())
                                    val encodedDoctorName = URLEncoder.encode(doctorFullName, StandardCharsets.UTF_8.toString())
                                    // Navigate to choose patient screen with slot info
                                    navController.navigate(
                                        "choose_patient_for_appointment/$doctorId/$encodedDoctorName/$specialty/$dateStr/${block.name}/$encodedTimeRange"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    enabled = selectedSlot != null && selectedSlot?.isBooked == false && !isReschedulingInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRescheduling) TealAccent else ButtonColor,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isReschedulingInProgress) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRescheduling) {
                                if (selectedSlot != null) "Confirm Reschedule" else "Select a time slot"
                            } else {
                                if (selectedSlot != null) "Select Time Slot" else "Select a time slot"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
        ) {
            // Horizontal date selector
            item {
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
                            }
                        )
                    }
                }
                Divider(color = Color.LightGray, thickness = 1.dp)
            }

            // Loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ButtonColor)
                    }
                }
            } else {
                // Time blocks section
                item {
                    Text(
                        text = "Select Time Block",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatTextColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Show message if no blocks are available
                if (timeBlocks.all { !it.isAvailable }) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Doctor Not Available",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "The doctor has not set availability for ${selectedDate.dayOfWeek}. Please select a different date.",
                                        fontSize = 14.sp,
                                        color = Color(0xFFC62828),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Time blocks
                items(timeBlocks) { block ->
                    Column {
                        TimeBlockCard(
                            block = block,
                            isSelected = selectedBlock == block,
                            onClick = {
                                if (block.isAvailable && !block.isFullyBooked) {
                                    selectedBlock = if (selectedBlock == block) null else block
                                    selectedSlot = null // Reset slot selection when changing block
                                }
                            }
                        )
                        
                        // Show time slots when block is selected
                        if (selectedBlock == block && block.isAvailable) {
                            TimeSlotSection(
                                block = block,
                                selectedSlot = selectedSlot,
                                onSlotSelected = { slot ->
                                    if (!slot.isBooked) {
                                        selectedSlot = slot
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Footer info
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Select your preferred time slot. Each slot is ${DEFAULT_SLOT_DURATION_MINUTES} minutes. Arrive 10 minutes before your scheduled time.",
                                fontSize = 13.sp,
                                color = Color(0xFF0D47A1),
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Time slot section with legend and slot grid
 */
@Composable
fun TimeSlotSection(
    block: TimeBlock,
    selectedSlot: TimeSlot?,
    onSlotSelected: (TimeSlot) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        // Color Legend
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Time Slots",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StatTextColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Legend row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Available legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(TealAccent.copy(alpha = 0.15f))
                                .border(1.dp, TealAccent, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Available",
                            fontSize = 12.sp,
                            color = TealAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Booked legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BookedRed.copy(alpha = 0.15f))
                                .border(1.dp, BookedRed, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Booked",
                            fontSize = 12.sp,
                            color = BookedRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Selected legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SelectedTeal)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selected",
                            fontSize = 12.sp,
                            color = SelectedTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Time slots grid
                if (block.slots.isEmpty()) {
                    Text(
                        text = "No time slots available in this block.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else if (block.slots.all { it.isBooked }) {
                    // All slots booked message
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = BookedRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No free time slots in this block.",
                                fontSize = 14.sp,
                                color = BookedRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Slots grid - 3 columns
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = true
                    ) {
                        items(block.slots) { slot ->
                            TimeSlotChip(
                                slot = slot,
                                isSelected = selectedSlot == slot,
                                onClick = { onSlotSelected(slot) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual time slot chip
 */
@Composable
fun TimeSlotChip(
    slot: TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> SelectedTeal
        slot.isBooked -> BookedRed.copy(alpha = 0.12f)
        else -> TealAccent.copy(alpha = 0.08f)
    }
    
    val borderColor = when {
        isSelected -> SelectedTeal
        slot.isBooked -> BookedRed.copy(alpha = 0.6f)
        else -> TealAccent.copy(alpha = 0.5f)
    }
    
    val textColor = when {
        isSelected -> Color.White
        slot.isBooked -> BookedRed.copy(alpha = 0.7f)
        else -> TealAccent
    }
    
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (slot.isBooked) {
                    Modifier.alpha(0.6f)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !slot.isBooked) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = slot.startTime.format(formatter),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "–",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = slot.endTime.format(formatter),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Load time blocks with generated time slots for a specific date
 */
suspend fun loadTimeBlocksWithSlots(doctorId: String, date: LocalDate): List<TimeBlock> {
    val db = Firebase.firestore
    val dayOfWeek = date.dayOfWeek.value
    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    
    try {
        // Fetch doctor availability for this day
        val availabilitySnapshot = db.collection("doctor_availability")
            .whereEqualTo("doctorUid", doctorId)
            .whereEqualTo("dayOfWeek", dayOfWeek)
            .whereEqualTo("isActive", true)
            .get()
            .await()
        
        val availability = availabilitySnapshot.documents.firstOrNull()
        val startTime = availability?.getString("startTime")
        val endTime = availability?.getString("endTime")
        
        // Fetch existing appointments for this date to mark booked slots
        val appointmentsSnapshot = db.collection("appointments")
            .whereEqualTo("doctorUid", doctorId)
            .get()
            .await()
        
        // Filter appointments for this specific date and get booked slots
        val bookedSlots = mutableSetOf<String>()
        appointmentsSnapshot.documents.forEach { doc ->
            val appointmentDate = doc.getTimestamp("appointmentDate")
            val status = doc.getString("status")?.lowercase() ?: ""
            val slotStartTime = doc.getString("slotStartTime") ?: ""
            
            // Consider scheduled, confirmed, rescheduled, and pending as booked (not cancelled or completed)
            val isActiveStatus = status in listOf("scheduled", "confirmed", "rescheduled", "pending")
            
            if (appointmentDate != null && isActiveStatus) {
                val appointmentLocalDate = appointmentDate.toDate()
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                
                if (appointmentLocalDate == date && slotStartTime.isNotBlank()) {
                    bookedSlots.add(slotStartTime)
                    android.util.Log.d("SelectDateTime", "Booked slot found: $slotStartTime on $appointmentLocalDate (status: $status)")
                }
            }
        }
        
        android.util.Log.d("SelectDateTime", "Total booked slots for $date: ${bookedSlots.size} - $bookedSlots")
        
        // Define time blocks
        val blockDefinitions = listOf(
            Triple("Morning", 6, 12),
            Triple("Afternoon", 12, 16),
            Triple("Evening", 16, 20),
            Triple("Night", 20, 24)
        )
        
        return blockDefinitions.map { (name, blockStart, blockEnd) ->
            val (doctorStart, doctorEnd, hours) = calculateBlockAvailability(
                startTime, endTime, blockStart, blockEnd
            )
            
            val isAvailable = doctorStart != null && doctorEnd != null
            
            // Generate time slots for this block
            val slots = if (isAvailable && doctorStart != null && doctorEnd != null) {
                generateTimeSlots(doctorStart, doctorEnd, name, bookedSlots)
            } else {
                emptyList()
            }
            
            val availableSlots = slots.count { !it.isBooked }
            val isFullyBooked = isAvailable && availableSlots == 0
            
            TimeBlock(
                name = name,
                startHour = blockStart,
                endHour = blockEnd,
                doctorStartTime = doctorStart,
                doctorEndTime = doctorEnd,
                isAvailable = isAvailable,
                currentBookings = slots.count { it.isBooked },
                maxCapacity = slots.size,
                isFullyBooked = isFullyBooked,
                slots = slots
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("SelectDateTime", "Error loading time blocks: ${e.message}", e)
        return listOf(
            TimeBlock("Morning", 6, 12, isAvailable = false),
            TimeBlock("Afternoon", 12, 16, isAvailable = false),
            TimeBlock("Evening", 16, 20, isAvailable = false),
            TimeBlock("Night", 20, 24, isAvailable = false)
        )
    }
}

/**
 * Generate time slots based on doctor's availability window
 */
fun generateTimeSlots(
    startTimeStr: String,
    endTimeStr: String,
    blockName: String,
    bookedSlots: Set<String>
): List<TimeSlot> {
    val slots = mutableListOf<TimeSlot>()
    
    try {
        // Parse the time strings - handle both 12-hour and 24-hour formats
        val formatter12 = DateTimeFormatter.ofPattern("h:mm a")
        val formatter24 = DateTimeFormatter.ofPattern("HH:mm")
        
        val startTime = try {
            LocalTime.parse(startTimeStr.trim(), formatter12)
        } catch (e: Exception) {
            LocalTime.parse(startTimeStr.trim(), formatter24)
        }
        
        val endTime = try {
            LocalTime.parse(endTimeStr.trim(), formatter12)
        } catch (e: Exception) {
            LocalTime.parse(endTimeStr.trim(), formatter24)
        }
        
        var currentStart = startTime
        val slotDuration = Duration.ofMinutes(DEFAULT_SLOT_DURATION_MINUTES.toLong())
        
        while (currentStart.plus(slotDuration) <= endTime || currentStart.plus(slotDuration) == endTime) {
            val currentEnd = currentStart.plus(slotDuration)
            val slotStartStr = currentStart.format(formatter24)
            
            slots.add(
                TimeSlot(
                    startTime = currentStart,
                    endTime = currentEnd,
                    isBooked = bookedSlots.contains(slotStartStr),
                    blockName = blockName
                )
            )
            
            currentStart = currentEnd
        }
    } catch (e: Exception) {
        android.util.Log.e("SelectDateTime", "Error generating time slots: ${e.message}", e)
    }
    
    return slots
}

/**
 * Calculate doctor availability within a time block
 */
fun calculateBlockAvailability(
    doctorStart: String?,
    doctorEnd: String?,
    blockStartHour: Int,
    blockEndHour: Int
): Triple<String?, String?, Int> {
    if (doctorStart.isNullOrBlank() || doctorEnd.isNullOrBlank()) {
        return Triple(null, null, 0)
    }
    
    try {
        val docStartTime = LocalTime.parse(doctorStart, DateTimeFormatter.ofPattern("HH:mm"))
        val docEndTime = LocalTime.parse(doctorEnd, DateTimeFormatter.ofPattern("HH:mm"))
        val blockStart = LocalTime.of(blockStartHour, 0)
        val blockEnd = LocalTime.of(blockEndHour, 0)
        
        // Check if doctor works during this block
        val noOverlap = docEndTime.isBefore(blockStart) || docEndTime == blockStart || 
                        docStartTime.isAfter(blockEnd) || docStartTime == blockEnd
        
        if (noOverlap) {
            return Triple(null, null, 0)
        }
        
        // Calculate overlap between doctor schedule and block
        val overlapStart = if (docStartTime.isAfter(blockStart)) docStartTime else blockStart
        val overlapEnd = if (docEndTime.isBefore(blockEnd)) docEndTime else blockEnd
        
        if (overlapStart.isBefore(overlapEnd)) {
            val duration = Duration.between(overlapStart, overlapEnd)
            val hours = kotlin.math.ceil(duration.toMinutes() / 60.0).toInt()
            
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedStart = overlapStart.format(formatter)
            val formattedEnd = overlapEnd.format(formatter)
            
            return Triple(formattedStart, formattedEnd, hours)
        }
        
        return Triple(null, null, 0)
    } catch (e: Exception) {
        android.util.Log.e("SelectDateTime", "Error calculating block availability: ${e.message}", e)
        return Triple(null, null, 0)
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
fun TimeBlockCard(
    block: TimeBlock,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDisabled = !block.isAvailable || block.isFullyBooked
    val isDarkMode = isSystemInDarkTheme()
    
    val backgroundColor = when {
        isDisabled && isDarkMode -> Color(0xFF2C2C2C)
        isDisabled -> Color(0xFFE0E0E0)
        isSelected && isDarkMode -> Color(0xFFB8936B)
        isSelected -> ButtonColor
        isDarkMode -> Color(0xFF3A3A3A)
        else -> CardWhite
    }
    
    val borderColor = when {
        isDisabled -> Color.Transparent
        isSelected -> Color.Transparent
        isDarkMode -> Color(0xFF555555)
        else -> Color(0xFF0E4944).copy(alpha = 0.3f)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = !isDisabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        tonalElevation = if (isSelected) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Available slots text in top right
            if (block.isAvailable && !block.isFullyBooked) {
                val availableSlots = block.slots.count { !it.isBooked }
                Text(
                    text = "$availableSlots/${block.slots.size} slots free",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isSelected -> Color.White.copy(alpha = 0.8f)
                        isDarkMode -> Color(0xFFAAAAAA)
                        else -> Color.Gray
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            
            // Expand/collapse indicator
            if (block.isAvailable && !block.isFullyBooked) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isSelected) "Collapse" else "Expand",
                    tint = when {
                        isSelected -> Color.White
                        isDarkMode -> Color(0xFF76DCB0)
                        else -> ButtonColor
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                )
            }
            
            // Main content row
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on block type
                val icon = when (block.name) {
                    "Morning" -> Icons.Default.WbSunny
                    "Afternoon" -> Icons.Default.LightMode
                    "Evening" -> Icons.Default.Nightlight
                    "Night" -> Icons.Default.DarkMode
                    else -> Icons.Default.Schedule
                }
                
                val iconColor = when {
                    isDisabled && isDarkMode -> Color(0xFF666666)
                    isDisabled -> Color.Gray
                    isSelected -> Color.White
                    isDarkMode -> Color(0xFF76DCB0)
                    else -> ButtonColor
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = block.name,
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Block info
                Column {
                    Text(
                        text = block.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDisabled && isDarkMode -> Color(0xFF888888)
                            isDisabled -> Color.Gray
                            isSelected -> Color.White
                            isDarkMode -> Color.White
                            else -> StatTextColor
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    if (block.isAvailable && !block.isFullyBooked) {
                        Text(
                            text = "${block.doctorStartTime} - ${block.doctorEndTime}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isSelected -> Color.White.copy(alpha = 0.9f)
                                isDarkMode -> Color(0xFF76DCB0)
                                else -> ButtonColor
                            }
                        )
                    } else if (block.isFullyBooked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFF888888) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fully Booked",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) Color(0xFF888888) else Color.Gray
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFF888888) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Not Available",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) Color(0xFF888888) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
