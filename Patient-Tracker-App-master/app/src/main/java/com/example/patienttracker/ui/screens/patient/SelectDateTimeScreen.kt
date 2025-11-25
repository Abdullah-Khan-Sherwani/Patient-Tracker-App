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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.isSystemInDarkTheme

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

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
    val isFullyBooked: Boolean = false
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
    val scope = rememberCoroutineScope()
    
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
    var timeBlocks by remember { mutableStateOf<List<TimeBlock>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Load time blocks when date changes
    LaunchedEffect(selectedDate, doctorId) {
        isLoading = true
        scope.launch {
            try {
                android.util.Log.d("SelectDateTime", "=== STARTING AVAILABILITY LOAD ===")
                android.util.Log.d("SelectDateTime", "Doctor ID: $doctorId")
                android.util.Log.d("SelectDateTime", "Selected Date: ${selectedDate.date}")
                android.util.Log.d("SelectDateTime", "Day of Week: ${selectedDate.date.dayOfWeek.value}")
                
                timeBlocks = loadTimeBlocksForDate(doctorId, selectedDate.date)
                
                android.util.Log.d("SelectDateTime", "Loaded ${timeBlocks.size} time blocks")
                timeBlocks.forEach { block ->
                    android.util.Log.d("SelectDateTime", "${block.name}: available=${block.isAvailable}, " +
                            "times=${block.doctorStartTime}-${block.doctorEndTime}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SelectDateTime", "Error in LaunchedEffect: ${e.message}", e)
                // Show error blocks
                timeBlocks = listOf(
                    TimeBlock("Morning", 6, 12, isAvailable = false),
                    TimeBlock("Afternoon", 12, 16, isAvailable = false),
                    TimeBlock("Evening", 16, 20, isAvailable = false),
                    TimeBlock("Night", 20, 24, isAvailable = false)
                )
            } finally {
                isLoading = false
                selectedBlock = null // Reset selection when date changes
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
                                text = "Select Date & Time Block",
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
                        // Spacer to balance the back button
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
                        selectedBlock?.let { block ->
                            val dateStr = selectedDate.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val timeRange = "${block.doctorStartTime} - ${block.doctorEndTime}"
                            // URL encode the time range to handle special characters
                            val encodedTimeRange = java.net.URLEncoder.encode(timeRange, "UTF-8")
                            // Navigate with block name and time range
                            navController.navigate("confirm_appointment/$doctorId/$doctorFullName/$specialty/$dateStr/${block.name}/$encodedTimeRange")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    enabled = selectedBlock != null && selectedBlock?.isAvailable == true && selectedBlock?.isFullyBooked == false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedBlock != null) "Continue" else "Select a time block",
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
                        }
                    )
                }
            }

            Divider(color = Color.LightGray, thickness = 1.dp)

            // Time blocks content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Available Time Blocks",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatTextColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Show message if no blocks are available
                    if (timeBlocks.all { !it.isAvailable }) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                            text = "The doctor has not set availability for ${selectedDate.dayOfWeek}. Please select a different date or contact the clinic.",
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

                    items(timeBlocks.size) { index ->
                        TimeBlockCard(
                            block = timeBlocks[index],
                            isSelected = selectedBlock == timeBlocks[index],
                            onClick = {
                                if (timeBlocks[index].isAvailable && !timeBlocks[index].isFullyBooked) {
                                    selectedBlock = timeBlocks[index]
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Information card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFF57C00),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "You'll be called based on your appointment number. The time shown is the doctor's availability window for that block.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF5D4037),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Load time blocks for a specific date based on doctor availability
 */
suspend fun loadTimeBlocksForDate(doctorId: String, date: LocalDate): List<TimeBlock> {
    android.util.Log.e("SelectDateTime", "╔═══════════════════════════════════════════╗")
    android.util.Log.e("SelectDateTime", "║  LOAD TIME BLOCKS - FUNCTION CALLED      ║")
    android.util.Log.e("SelectDateTime", "╚═══════════════════════════════════════════╝")
    
    val db = Firebase.firestore
    
    // Get day of week (1=Monday, 7=Sunday)
    val dayOfWeek = date.dayOfWeek.value
    
    try {
        android.util.Log.e("SelectDateTime", "🔍 INPUTS:")
        android.util.Log.e("SelectDateTime", "   Doctor ID: $doctorId")
        android.util.Log.e("SelectDateTime", "   Day of Week: $dayOfWeek (1=Mon, 7=Sun)")
        android.util.Log.e("SelectDateTime", "   Date: $date")
        
        // Test connection - try to read any document from doctor_availability
        try {
            val testQuery = db.collection("doctor_availability").limit(1).get().await()
            android.util.Log.d("SelectDateTime", "Connection test: SUCCESS - Can read doctor_availability collection")
            android.util.Log.d("SelectDateTime", "Total documents in test query: ${testQuery.documents.size}")
        } catch (testError: Exception) {
            android.util.Log.e("SelectDateTime", "Connection test: FAILED - Cannot read doctor_availability", testError)
            android.util.Log.e("SelectDateTime", "Error type: ${testError.javaClass.simpleName}")
            android.util.Log.e("SelectDateTime", "Error message: ${testError.message}")
        }
        
        // Fetch doctor availability for this day
        android.util.Log.d("SelectDateTime", "Querying: doctor_availability WHERE doctorUid='$doctorId' AND dayOfWeek=$dayOfWeek AND isActive=true")
        val availabilitySnapshot = db.collection("doctor_availability")
            .whereEqualTo("doctorUid", doctorId)
            .whereEqualTo("dayOfWeek", dayOfWeek)
            .whereEqualTo("isActive", true)
            .get()
            .await()
        
        android.util.Log.d("SelectDateTime", "Query result: ${availabilitySnapshot.documents.size} documents found")
        
        // Log all documents found (even if not active)
        if (availabilitySnapshot.isEmpty) {
            android.util.Log.w("SelectDateTime", "❌ No availability documents found for this doctor on this day")
            android.util.Log.w("SelectDateTime", "Query was: doctorUid='$doctorId', dayOfWeek=$dayOfWeek, isActive=true")
            // Try querying without isActive filter to see if documents exist
            val allDocsForDoctor = db.collection("doctor_availability")
                .whereEqualTo("doctorUid", doctorId)
                .get()
                .await()
            android.util.Log.d("SelectDateTime", "📊 Total documents for this doctor (all days): ${allDocsForDoctor.documents.size}")
            if (allDocsForDoctor.documents.isEmpty()) {
                android.util.Log.e("SelectDateTime", "⚠️ CRITICAL: Doctor has NO availability documents at all!")
                android.util.Log.e("SelectDateTime", "⚠️ Doctor needs to set availability in Edit Availability screen")
            } else {
                allDocsForDoctor.documents.forEach { doc ->
                    val docDayOfWeek = doc.getLong("dayOfWeek")
                    val docIsActive = doc.getBoolean("isActive")
                    val docStartTime = doc.getString("startTime")
                    val docEndTime = doc.getString("endTime")
                    android.util.Log.d("SelectDateTime", "📅 Doc ID: ${doc.id}")
                    android.util.Log.d("SelectDateTime", "   - dayOfWeek: $docDayOfWeek (${if (docDayOfWeek == dayOfWeek.toLong()) "MATCHES" else "different"})")
                    android.util.Log.d("SelectDateTime", "   - isActive: $docIsActive")
                    android.util.Log.d("SelectDateTime", "   - times: $docStartTime - $docEndTime")
                }
            }
        } else {
            android.util.Log.d("SelectDateTime", "✅ Found availability document!")
        }
        
        // Get availability (should be one document per day per doctor)
        val availability = availabilitySnapshot.documents.firstOrNull()
        val startTime = availability?.getString("startTime") // Format: "HH:mm"
        val endTime = availability?.getString("endTime")
        
        android.util.Log.e("SelectDateTime", "")
        android.util.Log.e("SelectDateTime", "📋 AVAILABILITY DATA EXTRACTED:")
        android.util.Log.e("SelectDateTime", "   startTime = '$startTime'")
        android.util.Log.e("SelectDateTime", "   endTime = '$endTime'")
        if (availability != null) {
            android.util.Log.e("SelectDateTime", "   Document ID: ${availability.id}")
            android.util.Log.e("SelectDateTime", "   All fields: ${availability.data}")
        } else {
            android.util.Log.e("SelectDateTime", "   ⚠️ availability document is NULL")
        }
        
        // Count existing bookings for this date
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val appointmentsSnapshot = db.collection("appointments")
            .whereEqualTo("doctorUid", doctorId)
            .get()
            .await()
        
        // Filter appointments by date and count by block
        val appointmentsByBlock = mutableMapOf(
            "Morning" to 0,
            "Afternoon" to 0,
            "Evening" to 0,
            "Night" to 0
        )
        
        appointmentsSnapshot.documents.forEach { doc ->
            val appointmentDate = doc.getTimestamp("appointmentDate")
            val status = doc.getString("status")?.lowercase() ?: ""
            val blockName = doc.getString("blockName") ?: "" // Use blockName field
            
            // Only count scheduled or confirmed appointments (not cancelled or completed)
            if (appointmentDate != null && (status == "scheduled" || status == "confirmed")) {
                val appointmentLocalDate = appointmentDate.toDate()
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                
                if (appointmentLocalDate == date && blockName.isNotEmpty()) {
                    appointmentsByBlock[blockName] = (appointmentsByBlock[blockName] ?: 0) + 1
                }
            }
        }
        
        android.util.Log.d("SelectDateTime", "Appointments by block for $date: $appointmentsByBlock")
        
        // Define time blocks
        val blockDefinitions = listOf(
            Triple("Morning", 6, 12),
            Triple("Afternoon", 12, 16),
            Triple("Evening", 16, 20),
            Triple("Night", 20, 24)
        )
        
        android.util.Log.e("SelectDateTime", "")
        android.util.Log.e("SelectDateTime", "🕐 CALCULATING TIME BLOCKS:")
        
        return blockDefinitions.map { (name, blockStart, blockEnd) ->
            android.util.Log.e("SelectDateTime", "  Processing: $name ($blockStart:00 - $blockEnd:00)")
            val (doctorStart, doctorEnd, hours) = calculateBlockAvailability(
                startTime, endTime, blockStart, blockEnd
            )
            
            android.util.Log.e("SelectDateTime", "  Result: doctorStart=$doctorStart, doctorEnd=$doctorEnd, hours=$hours")
            
            val isAvailable = doctorStart != null && doctorEnd != null
            val maxCapacity = if (isAvailable) (hours * 10) else 0
            val currentBookings = appointmentsByBlock[name] ?: 0
            val isFullyBooked = isAvailable && currentBookings >= maxCapacity
            
            TimeBlock(
                name = name,
                startHour = blockStart,
                endHour = blockEnd,
                doctorStartTime = doctorStart,
                doctorEndTime = doctorEnd,
                isAvailable = isAvailable,
                currentBookings = currentBookings,
                maxCapacity = maxCapacity,
                isFullyBooked = isFullyBooked
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("SelectDateTime", "╔═══════════════════════════════════════════╗")
        android.util.Log.e("SelectDateTime", "║  ❌ EXCEPTION IN loadTimeBlocksForDate   ║")
        android.util.Log.e("SelectDateTime", "╚═══════════════════════════════════════════╝")
        android.util.Log.e("SelectDateTime", "Exception type: ${e.javaClass.simpleName}")
        android.util.Log.e("SelectDateTime", "Error message: ${e.message}", e)
        e.printStackTrace()
        // Return empty blocks on error (all marked as unavailable)
        return listOf(
            TimeBlock("Morning", 6, 12, doctorStartTime = null, doctorEndTime = null, isAvailable = false),
            TimeBlock("Afternoon", 12, 16, doctorStartTime = null, doctorEndTime = null, isAvailable = false),
            TimeBlock("Evening", 16, 20, doctorStartTime = null, doctorEndTime = null, isAvailable = false),
            TimeBlock("Night", 20, 24, doctorStartTime = null, doctorEndTime = null, isAvailable = false)
        )
    }
}

/**
 * Calculate doctor availability within a time block
 * Returns (startTime, endTime, hours) or (null, null, 0) if not available
 */
fun calculateBlockAvailability(
    doctorStart: String?,
    doctorEnd: String?,
    blockStartHour: Int,
    blockEndHour: Int
): Triple<String?, String?, Int> {
    // Return null if doctor times are missing
    if (doctorStart.isNullOrBlank() || doctorEnd.isNullOrBlank()) {
        android.util.Log.d("SelectDateTime", "Doctor times are null or blank")
        return Triple(null, null, 0)
    }
    
    try {
        android.util.Log.d("SelectDateTime", "🔍 calculateBlockAvailability called:")
        android.util.Log.d("SelectDateTime", "   Input: doctorStart='$doctorStart', doctorEnd='$doctorEnd'")
        android.util.Log.d("SelectDateTime", "   Block: $blockStartHour:00 - $blockEndHour:00")
        
        // Parse doctor's working hours (24-hour format: "09:00", "17:00")
        val docStartTime = LocalTime.parse(doctorStart, DateTimeFormatter.ofPattern("HH:mm"))
        val docEndTime = LocalTime.parse(doctorEnd, DateTimeFormatter.ofPattern("HH:mm"))
        val blockStart = LocalTime.of(blockStartHour, 0)
        val blockEnd = LocalTime.of(blockEndHour, 0)
        
        android.util.Log.d("SelectDateTime", "   Parsed: Doctor=$docStartTime-$docEndTime, Block=$blockStart-$blockEnd")
        
        // Check if doctor works during this block
        val noOverlap = docEndTime.isBefore(blockStart) || docEndTime == blockStart || 
                        docStartTime.isAfter(blockEnd) || docStartTime == blockEnd
        
        android.util.Log.d("SelectDateTime", "   Overlap check:")
        android.util.Log.d("SelectDateTime", "   - docEndTime ($docEndTime) <= blockStart ($blockStart)? ${docEndTime.isBefore(blockStart) || docEndTime == blockStart}")
        android.util.Log.d("SelectDateTime", "   - docStartTime ($docStartTime) >= blockEnd ($blockEnd)? ${docStartTime.isAfter(blockEnd) || docStartTime == blockEnd}")
        android.util.Log.d("SelectDateTime", "   - No overlap: $noOverlap")
        
        if (noOverlap) {
            android.util.Log.d("SelectDateTime", "   ❌ No overlap - doctor not working during this block")
            return Triple(null, null, 0)
        }
        
        // Calculate overlap between doctor schedule and block
        val overlapStart = if (docStartTime.isAfter(blockStart)) docStartTime else blockStart
        val overlapEnd = if (docEndTime.isBefore(blockEnd)) docEndTime else blockEnd
        
        android.util.Log.d("SelectDateTime", "   Calculated overlap: $overlapStart - $overlapEnd")
        
        if (overlapStart.isBefore(overlapEnd)) {
            // Calculate hours - round up to ensure partial hours count (e.g., 1.5 hours = 2 slots)
            val duration = java.time.Duration.between(overlapStart, overlapEnd)
            val hours = kotlin.math.ceil(duration.toMinutes() / 60.0).toInt()
            
            // Format as 12-hour time with AM/PM (use 'h' for 1-12 hour without leading zero)
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedStart = overlapStart.format(formatter)
            val formattedEnd = overlapEnd.format(formatter)
            
            android.util.Log.d("SelectDateTime", "   ✅ AVAILABLE: $formattedStart - $formattedEnd ($hours hours, ${duration.toMinutes()} minutes)")
            return Triple(formattedStart, formattedEnd, hours)
        }
        
        android.util.Log.d("SelectDateTime", "   ❌ overlapStart ($overlapStart) not before overlapEnd ($overlapEnd)")
        return Triple(null, null, 0)
    } catch (e: Exception) {
        android.util.Log.e("SelectDateTime", "Error calculating block availability: ${e.message}", e)
        e.printStackTrace()
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
        else -> Color(0xFFD4AF8C).copy(alpha = 0.3f)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(enabled = !isDisabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        tonalElevation = if (isSelected) 4.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Available slots text in top right
            if (block.isAvailable && !block.isFullyBooked) {
                Text(
                    text = "Available: ${block.maxCapacity - block.currentBookings}/${block.maxCapacity} slots",
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
            
            // Main content row
            Row(
                modifier = Modifier.fillMaxSize(),
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
                    isDarkMode -> Color(0xFFD4AF8C)
                    else -> ButtonColor
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = block.name,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Block info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = block.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDisabled && isDarkMode -> Color(0xFF888888)
                            isDisabled -> Color.Gray
                            isSelected -> Color.White
                            isDarkMode -> Color.White
                            else -> StatTextColor
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (block.isAvailable && !block.isFullyBooked) {
                        Text(
                            text = "${block.doctorStartTime} - ${block.doctorEndTime}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isSelected -> Color.White.copy(alpha = 0.9f)
                                isDarkMode -> Color(0xFFD4AF8C)
                                else -> ButtonColor
                            }
                        )
                        Text(
                            text = "${block.maxCapacity - block.currentBookings} slots available",
                            fontSize = 12.sp,
                            color = when {
                                isSelected -> Color.White.copy(alpha = 0.7f)
                                isDarkMode -> Color(0xFFB8B8B8)
                                else -> Color.Gray
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
                
                // Selection indicator
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
