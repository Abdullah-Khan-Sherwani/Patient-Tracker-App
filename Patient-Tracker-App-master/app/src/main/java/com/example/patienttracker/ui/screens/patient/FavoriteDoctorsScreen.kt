package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.patienttracker.data.PatientFavoritesRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep Teal accent

@Composable
fun FavoriteDoctorsScreen(navController: NavController, context: Context) {
    var favoriteDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Load favorite doctors
    LaunchedEffect(Unit) {
        try {
            val favoriteIds = PatientFavoritesRepository.getFavoriteDoctorIds()
            if (favoriteIds.isNotEmpty()) {
                val allDoctors = fetchDoctorsFromFirestore()
                favoriteDoctors = allDoctors.filter { it.id in favoriteIds }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                        text = "Favorite Doctors",
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
                favoriteDoctors.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = StatTextColor.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No favorite doctors yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatTextColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the heart on a doctor to save them here.",
                            fontSize = 14.sp,
                            color = StatTextColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoriteDoctors) { doctor ->
                            FavoriteDoctorCard(
                                doctor = doctor,
                                onBookAppointment = {
                                    // Navigate directly to date/time selection
                                    navController.navigate(
                                        "select_datetime/${doctor.id}/${doctor.firstName}/${doctor.lastName}/${doctor.speciality}"
                                    )
                                },
                                onUnfavorite = {
                                    scope.launch {
                                        PatientFavoritesRepository.removeFavoriteDoctor(doctor.id)
                                        // Reload favorites
                                        val favoriteIds = PatientFavoritesRepository.getFavoriteDoctorIds()
                                        if (favoriteIds.isNotEmpty()) {
                                            val allDoctors = fetchDoctorsFromFirestore()
                                            favoriteDoctors = allDoctors.filter { it.id in favoriteIds }
                                        } else {
                                            favoriteDoctors = emptyList()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteDoctorCard(
    doctor: DoctorFull,
    onBookAppointment: () -> Unit,
    onUnfavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header with heart icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Doctor initial circle
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = AccentColor.copy(alpha = 0.2f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = doctor.firstName.firstOrNull()?.toString() ?: "D",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentColor
                        )
                    }
                }
                
                // Heart icon
                IconButton(
                    onClick = onUnfavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Doctor name
            Text(
                text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StatTextColor,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Specialization
            Text(
                text = doctor.speciality,
                fontSize = 12.sp,
                color = HeaderBottomColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Availability summary
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BackgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = if (doctor.days.isNotEmpty()) doctor.days else "Availability not provided",
                        fontSize = 10.sp,
                        color = StatTextColor,
                        maxLines = 1
                    )
                    if (doctor.timings.isNotEmpty()) {
                        Text(
                            text = doctor.timings,
                            fontSize = 10.sp,
                            color = StatTextColor.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Book appointment button
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.End)
                    .clickable(onClick = onBookAppointment),
                shape = CircleShape,
                color = ButtonColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Book Appointment",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
