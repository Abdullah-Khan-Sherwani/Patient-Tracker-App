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
    val scope = rememberCoroutineScope()
    
    // Check if doctor is favorited
    LaunchedEffect(doctor.id) {
        isFavorite = PatientFavoritesRepository.isDoctorFavorited(doctor.id)
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = doctor.speciality,
                        fontSize = 14.sp,
                        color = HeaderBottomColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Heart icon for favorites
                IconButton(
                    onClick = {
                        scope.launch {
                            PatientFavoritesRepository.toggleFavorite(doctor.id)
                            isFavorite = !isFavorite
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFE91E63) else StatTextColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Availability info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Available: ${doctor.days} • ${doctor.timings}",
                        fontSize = 13.sp,
                        color = StatTextColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Price badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50)
                ) {
                    Text(
                        text = "Rs. 1,500",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Slots button
            Button(
                onClick = onViewSlots,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "View Available Slots",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
