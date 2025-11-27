package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import android.os.Parcelable
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize

// Data class for weekly availability display
data class DayAvailability(
    val dayOfWeek: Int, // 1=Mon, 7=Sun
    val dayAbbrev: String,
    val timeRange: String,
    val isActive: Boolean
)


@Parcelize
data class DoctorFull(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val speciality: String = "",
    val days: String = "",
    val timings: String = ""
) : Parcelable

/**
 * Doctor List Screen - Search Specialists
 * 
 * THEME FIX: Now uses MaterialTheme.colorScheme for proper dark mode support
 * - Primary colors for gradient header
 * - Surface/background colors for cards and list background
 * - All colors now respect system/app theme setting
 */
@Composable
fun DoctorListScreen(navController: NavController, context: Context, specialityFilter: String?) {
    // THEME FIX: Use MaterialTheme colors instead of hardcoded values
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val gradient = Brush.verticalGradient(listOf(primaryColor, primaryContainer))

    var doctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }

    LaunchedEffect(Unit) {
        doctors = fetchDoctorsFromFirestore()
    }

    val filtered = remember(specialityFilter, doctors) {
        if (specialityFilter.isNullOrBlank() || specialityFilter == "All")
            doctors
        else doctors.filter { it.speciality.equals(specialityFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
                        // THEME FIX: Use onPrimary for text on primary background
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                // THEME FIX: Use background color from theme
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filtered) { doc ->
                DoctorCard(doc) {
                    // Navigate to simplified booking with doctor info
                    val doctorFullName = "Dr. ${doc.firstName} ${doc.lastName}"
                    navController.navigate("book_appointment_simple/${doc.id}/${doctorFullName}/${doc.speciality}")
                }
            }
        }
    }
}

/**
 * Doctor Card Component
 * 
 * THEME FIX: Now uses MaterialTheme.colorScheme for all colors
 * - surface/surfaceVariant for card background
 * - onSurface/onSurfaceVariant for text
 * - primary for buttons
 * Properly supports dark mode
 */
@Composable
fun DoctorCard(doctor: DoctorFull, onBookClick: () -> Unit) {
    // Dark mode support - matching DoctorCatalogueScreen colors
    val isDarkMode = isSystemInDarkTheme()
    val DarkCardColor = Color(0xFF2C2C2E)
    val DarkTextColor = Color(0xFFE5E5E5)
    val DarkSecondaryTextColor = Color(0xFFB0B0B0)
    val DarkIconTint = Color(0xFF8E8E93)
    val AccentColor = Color(0xFFB8956A)
    val ButtonColor = Color(0xFFC9956E)
    val CardWhite = Color(0xFFFFFFFF)
    val StatTextColor = Color(0xFF5C4A42)
    
    // Availability chip colors
    val AvailabilityChipBg = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFF5EDE4)
    val AvailabilityChipText = if (isDarkMode) Color(0xFFE5E5E5) else Color(0xFF5C4A42)
    
    var isFavorite by remember { mutableStateOf(false) }
    var weeklyAvailability by remember { mutableStateOf<List<DayAvailability>>(emptyList()) }
    var isLoadingAvailability by remember { mutableStateOf(true) }
    var isAvailableNow by remember { mutableStateOf<Boolean?>(null) } // null = unknown/loading
    val scope = rememberCoroutineScope()
    
    // Fetch weekly availability and check real-time status
    LaunchedEffect(doctor.id) {
        try {
            val db = Firebase.firestore
            val today = LocalDate.now()
            val todayDayOfWeek = today.dayOfWeek.value // 1=Mon, 7=Sun
            
            val availabilitySnapshot = db.collection("doctor_availability")
                .whereEqualTo("doctorUid", doctor.id)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val dayAbbreviations = mapOf(
                1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
                5 to "Fri", 6 to "Sat", 7 to "Sun"
            )
            
            val formatter12 = DateTimeFormatter.ofPattern("h a")
            val formatter24 = DateTimeFormatter.ofPattern("HH:mm")
            
            val availList = availabilitySnapshot.documents.mapNotNull { doc ->
                val dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: return@mapNotNull null
                val startTime = doc.getString("startTime") ?: return@mapNotNull null
                val endTime = doc.getString("endTime") ?: return@mapNotNull null
                
                try {
                    val start = LocalTime.parse(startTime, formatter24)
                    val end = LocalTime.parse(endTime, formatter24)
                    val timeRange = "${start.format(formatter12)}–${end.format(formatter12)}"
                    
                    DayAvailability(
                        dayOfWeek = dayOfWeek,
                        dayAbbrev = dayAbbreviations[dayOfWeek] ?: "",
                        timeRange = timeRange,
                        isActive = true
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.dayOfWeek }
            
            weeklyAvailability = availList
            
            // Check real-time availability: is current time within any active block?
            val currentTime = LocalTime.now()
            val todayAvailability = availabilitySnapshot.documents.find { doc ->
                doc.getLong("dayOfWeek")?.toInt() == todayDayOfWeek
            }
            
            isAvailableNow = if (todayAvailability != null) {
                val startTimeStr = todayAvailability.getString("startTime")
                val endTimeStr = todayAvailability.getString("endTime")
                if (startTimeStr != null && endTimeStr != null) {
                    try {
                        val startTime = LocalTime.parse(startTimeStr, formatter24)
                        val endTime = LocalTime.parse(endTimeStr, formatter24)
                        currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
                    } catch (e: Exception) {
                        false
                    }
                } else false
            } else false
            
            isLoadingAvailability = false
        } catch (e: Exception) {
            android.util.Log.e("DoctorCard", "Error loading availability: ${e.message}", e)
            isAvailableNow = null
            isLoadingAvailability = false
        }
    }
    
    val cardColor = if (isDarkMode) DarkCardColor else CardWhite
    val textPrimaryColor = if (isDarkMode) DarkTextColor else StatTextColor
    val textSecondaryColor = if (isDarkMode) DarkSecondaryTextColor else StatTextColor.copy(alpha = 0.7f)
    val iconTint = if (isDarkMode) DarkIconTint else AccentColor
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Profile picture + Name + Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture with initials
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = AccentColor.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${doctor.firstName.firstOrNull()?.uppercaseChar() ?: "D"}${doctor.lastName.firstOrNull()?.uppercaseChar() ?: ""}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Doctor name and specialty
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = doctor.speciality.ifEmpty { "General Physician" },
                        fontSize = 13.sp,
                        color = textSecondaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Status dot + Favorite heart icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status indicator dot
                    val statusColor = when (isAvailableNow) {
                        true -> Color(0xFF4CAF50) // Green - available now
                        false -> Color(0xFFE53935) // Red - not available now
                        null -> Color(0xFFFFC107) // Yellow - unknown/loading
                    }
                    val statusTooltip = when (isAvailableNow) {
                        true -> "Available now"
                        false -> "Not available now"
                        null -> "Checking availability..."
                    }
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = statusColor
                    ) {}
                    
                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFE91E63) else textSecondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Weekly availability bar - horizontally scrollable
            if (isLoadingAvailability) {
                Text(
                    text = "Availability loading…",
                    fontSize = 11.sp,
                    color = textSecondaryColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 64.dp)
                )
            } else if (weeklyAvailability.isEmpty()) {
                // Not available - show "Not Available Today" state
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.padding(start = 64.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Not Available",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weeklyAvailability.forEach { dayAvail ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AvailabilityChipBg
                        ) {
                            Text(
                                text = "${dayAvail.dayAbbrev} ${dayAvail.timeRange}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AvailabilityChipText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Fee display - plain text, right-aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "RS 1500",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimaryColor
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Separator line
            HorizontalDivider(
                color = if (isDarkMode) Color(0xFF37474F) else Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Full-width CTA button
            Button(
                onClick = onBookClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "View Available Slots",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

suspend fun fetchDoctorsFromFirestore(): List<DoctorFull> {
    val db = Firebase.firestore
    return try {
        val querySnapshot = db.collection("users").whereEqualTo("role", "doctor").get().await()
        println("fetchDoctorsFromFirestore: Found ${querySnapshot.documents.size} doctor documents")
        querySnapshot.documents.mapNotNull { doc ->
            val firstName = doc.getString("firstName") ?: ""
            val lastName = doc.getString("lastName") ?: ""
            val timings = doc.getString("timings") ?: ""
            val days = doc.getString("days") ?: ""
            val speciality = doc.getString("speciality") ?: "General Physician"
            
            println("fetchDoctorsFromFirestore: Doctor ${doc.id} - Name: '$firstName $lastName', Speciality: '$speciality', Timings: '$timings', Days: '$days'")
            
            // Warn if critical booking data is missing
            if (timings.isBlank() || days.isBlank()) {
                println("⚠️ WARNING: Doctor ${doc.id} ($firstName $lastName) missing booking data - Timings: '$timings', Days: '$days'")
            }
            
            // Only include doctors with at least a first name or last name
            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                DoctorFull(
                    id = doc.id,
                    firstName = firstName,
                    lastName = lastName,
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    speciality = speciality,
                    days = days,
                    timings = timings
                )
            } else {
                println("fetchDoctorsFromFirestore: Skipping doctor ${doc.id} - empty name")
                null
            }
        }
    } catch (e: Exception) {
        println("fetchDoctorsFromFirestore: Error - ${e.message}")
        e.printStackTrace()
        emptyList()
    }
}
