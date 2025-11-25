package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.patienttracker.data.PatientFavoritesRepository
import kotlinx.coroutines.launch

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)

@Composable
fun SelectDoctorScreen(navController: NavController, context: Context, specialty: String) {
    var doctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(specialty) {
        try {
            val allDoctors = fetchDoctorsFromFirestore()
            doctors = if (specialty.equals("General", ignoreCase = true)) {
                // Only show doctors that don't match any defined specialty
                val definedSpecialties = listOf(
                    "General Physician", "Cardiologist", "Dermatologist", "Pediatrician",
                    "Neurologist", "Psychiatrist", "ENT Specialist", "Orthopedic",
                    "Gynecologist", "Dentist", "Urologist", "Oncologist", "Radiologist"
                )
                allDoctors.filter { doctor ->
                    definedSpecialties.none { it.equals(doctor.speciality, ignoreCase = true) }
                }
            } else {
                allDoctors.filter { it.speciality.equals(specialty, ignoreCase = true) }
            }
        } catch (e: Exception) {
            doctors = emptyList()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
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
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "$specialty Doctors",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HeaderTopColor)
                    }
                }
                doctors.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No doctors available in this specialty",
                            color = StatTextColor,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(doctors) { doctor ->
                            DoctorSelectionCard(doctor = doctor) {
                                navController.navigate("select_datetime/${doctor.id}/${doctor.firstName}/${doctor.lastName}/${doctor.speciality}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorSelectionCard(doctor: DoctorFull, onViewSlots: () -> Unit) {
    var isFavorite by remember { mutableStateOf(false) }
    var availabilityText by remember { mutableStateOf("Loading...") }
    val scope = rememberCoroutineScope()
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Check favorite status
    LaunchedEffect(doctor.id) {
        try {
            isFavorite = PatientFavoritesRepository.isDoctorFavorited(doctor.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Fetch real availability from doctor_availability collection
    LaunchedEffect(doctor.id) {
        try {
            val db = Firebase.firestore
            val today = java.time.LocalDate.now()
            val dayOfWeek = today.dayOfWeek.value // 1=Mon, 7=Sun
            
            val availabilitySnapshot = db.collection("doctor_availability")
                .whereEqualTo("doctorUid", doctor.id)
                .whereEqualTo("dayOfWeek", dayOfWeek)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            if (availabilitySnapshot.documents.isNotEmpty()) {
                val doc = availabilitySnapshot.documents.first()
                val startTime = doc.getString("startTime") // "09:00"
                val endTime = doc.getString("endTime") // "17:00"
                
                if (startTime != null && endTime != null) {
                    // Convert to 12-hour format
                    val formatter12 = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
                    val start = java.time.LocalTime.parse(startTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    val end = java.time.LocalTime.parse(endTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    
                    availabilityText = "${start.format(formatter12)} – ${end.format(formatter12)}"
                } else {
                    availabilityText = "Timings not provided"
                }
            } else {
                availabilityText = "Not available today"
            }
        } catch (e: Exception) {
            android.util.Log.e("DoctorCard", "Error loading availability: ${e.message}", e)
            availabilityText = "Availability unknown"
        }
    }
    
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else CardWhite
    val textPrimaryColor = if (isDarkMode) Color(0xFFFFFFFF) else StatTextColor
    val textSecondaryColor = if (isDarkMode) Color(0xFFBBBBBB) else StatTextColor.copy(alpha = 0.7f)
    val AccentColor = HeaderTopColor
    
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
                .padding(20.dp)
        ) {
            // Top row: Profile picture + Name + Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture with initials
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = AccentColor.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${doctor.firstName.firstOrNull()?.uppercaseChar() ?: "D"}${doctor.lastName.firstOrNull()?.uppercaseChar() ?: ""}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                // Doctor name
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        maxLines = 1
                    )
                }
                
                // Favorite heart icon
                IconButton(
                    onClick = {
                        scope.launch {
                            try {
                                PatientFavoritesRepository.toggleFavorite(doctor.id)
                                isFavorite = !isFavorite
                            } catch (e: Exception) {
                                android.util.Log.e("DoctorCard", "Error toggling favorite: ${e.message}", e)
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFE91E63) else textSecondaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Specialty and experience
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 70.dp) // Align with name
            ) {
                Text(
                    text = doctor.speciality.ifEmpty { "General Physician" },
                    fontSize = 14.sp,
                    color = textSecondaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Availability pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (availabilityText.contains("Not available") || availabilityText.contains("unknown")) {
                    Color(0xFFFFEBEE)
                } else {
                    Color(0xFFE8F5E9)
                },
                modifier = Modifier.padding(start = 70.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (availabilityText.contains("Not available") || availabilityText.contains("unknown")) {
                            Icons.Default.EventBusy
                        } else {
                            Icons.Default.AccessTime
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (availabilityText.contains("Not available") || availabilityText.contains("unknown")) {
                            Color(0xFFD32F2F)
                        } else {
                            Color(0xFF4CAF50)
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (availabilityText.contains("Not available") || availabilityText.contains("unknown")) {
                            availabilityText
                        } else {
                            "Next: $availabilityText"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (availabilityText.contains("Not available") || availabilityText.contains("unknown")) {
                            Color(0xFFD32F2F)
                        } else {
                            Color(0xFF2E7D32)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Consultation fee chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 70.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PKR 1,500",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            fontSize = 13.sp,
                            color = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In person",
                            fontSize = 12.sp,
                            color = textSecondaryColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Separator line
            Divider(
                color = if (isDarkMode) Color(0xFF37474F) else Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Full-width CTA button
            Button(
                onClick = onViewSlots,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Available Slots",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
