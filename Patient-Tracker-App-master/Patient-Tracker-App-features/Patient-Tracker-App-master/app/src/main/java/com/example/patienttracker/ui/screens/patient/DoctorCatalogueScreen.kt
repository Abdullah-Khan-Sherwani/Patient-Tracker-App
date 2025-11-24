package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import com.example.patienttracker.data.PatientFavoritesRepository
import com.example.patienttracker.data.SearchRepository
import com.example.patienttracker.ui.components.SearchBar
import com.example.patienttracker.ui.components.ChatFloatingButton
import androidx.compose.ui.graphics.vector.ImageVector

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)
private val AccentColor = Color(0xFFB8956A)

// Dark mode colors
private val DarkBackgroundColor = Color(0xFF0B0F12)
private val DarkCardColor = Color(0xFF1A2228)
private val DarkTextColor = Color(0xFFFFFFFF)
private val DarkSecondaryTextColor = Color(0xFFC3CCD2)
private val DarkIconTint = Color(0xFFFFFFFF)

data class SpecialtyInfo(
    val name: String,
    val icon: ImageVector,
    val description: String
)

// Complete list of all specializations (always displayed)
private fun getAllSpecialties(): List<SpecialtyInfo> {
    return listOf(
        SpecialtyInfo("General", Icons.Default.MedicalServices, "All doctors and general specialists"),
        SpecialtyInfo("General Physician", Icons.Default.MedicalServices, "Primary care for common medical concerns"),
        SpecialtyInfo("Cardiologist", Icons.Default.Favorite, "Heart and blood circulation specialist"),
        SpecialtyInfo("Dermatologist", Icons.Default.Face, "Skin, hair and nail treatments"),
        SpecialtyInfo("Pediatrician", Icons.Default.ChildCare, "Care for babies and children"),
        SpecialtyInfo("Neurologist", Icons.Default.Psychology, "Brain and nervous system specialist"),
        SpecialtyInfo("Psychiatrist", Icons.Default.SelfImprovement, "Mental health and anxiety support"),
        SpecialtyInfo("ENT Specialist", Icons.Default.Hearing, "Ear, nose and throat issues"),
        SpecialtyInfo("Orthopedic", Icons.Default.Accessibility, "Bones, joints and muscle concerns"),
        SpecialtyInfo("Gynecologist", Icons.Default.PregnantWoman, "Women's reproductive care"),
        SpecialtyInfo("Dentist", Icons.Default.LocalHospital, "Teeth and oral health"),
        SpecialtyInfo("Urologist", Icons.Default.WaterDrop, "Kidneys, bladder and urinary concerns"),
        SpecialtyInfo("Oncologist", Icons.Default.HealthAndSafety, "Cancer diagnosis and treatment"),
        SpecialtyInfo("Radiologist", Icons.Default.Scanner, "Medical imaging and scan analysis")
    )
}

// Match doctor specialty to standardized name
private fun normalizeSpecialty(specialty: String): String {
    return when (specialty.lowercase().trim()) {
        "general physician" -> "General Physician"
        "cardiologist", "cardiology" -> "Cardiologist"
        "dermatologist", "dermatology" -> "Dermatologist"
        "pediatrician", "pediatrics" -> "Pediatrician"
        "neurologist", "neurology" -> "Neurologist"
        "psychiatrist", "psychiatry" -> "Psychiatrist"
        "ent specialist", "ent", "otolaryngology" -> "ENT Specialist"
        "orthopedic", "orthopedics" -> "Orthopedic"
        "gynecologist", "gynecology" -> "Gynecologist"
        "dentist", "dental" -> "Dentist"
        "urologist", "urology" -> "Urologist"
        "oncologist", "oncology" -> "Oncologist"
        "radiologist", "radiology" -> "Radiologist"
        else -> "General" // All unmatched doctors go to General category
    }
}

@Composable
fun DoctorCatalogueScreen(navController: NavController, context: Context) {
    var allDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var selectedSpecialty by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Always show all specialties
    val allSpecialties = getAllSpecialties()
    
    // Normalize doctor specialties and group by standardized names
    val doctorsBySpecialty = remember(allDoctors) {
        allDoctors.groupBy { normalizeSpecialty(it.speciality) }
    }
    
    // Filtered doctors based on selected specialty
    val filteredDoctors = remember(selectedSpecialty, doctorsBySpecialty) {
        if (selectedSpecialty == null) {
            emptyList()
        } else {
            doctorsBySpecialty[selectedSpecialty] ?: emptyList()
        }
    }
    
    // Load all doctors
    LaunchedEffect(Unit) {
        try {
            allDoctors = fetchDoctorsFromFirestore()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Handle search
    val scope = rememberCoroutineScope()
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearching = true
            scope.launch {
                try {
                    val result = SearchRepository.searchDoctors(searchQuery)
                    searchResults = result.getOrNull() ?: emptyList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    searchResults = emptyList()
                } finally {
                    isSearching = false
                }
            }
        } else {
            searchResults = emptyList()
            isSearching = false
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
                        text = "Doctor Catalogue",
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HeaderTopColor)
                }
            } else if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                // Search results view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundColor)
                ) {
                    // Search bar
                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search doctors by name, specialty...",
                        backgroundColor = CardWhite,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Specialty filter chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardWhite)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Filter by Specialty",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatTextColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "All" chip
                            item {
                                FilterChip(
                                    selected = selectedSpecialty == null,
                                    onClick = { selectedSpecialty = null },
                                    label = {
                                        Text(
                                            text = "All (${searchResults.size})",
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeaderTopColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundColor,
                                        labelColor = StatTextColor
                                    )
                                )
                            }
                            
                            // Specialty chips for specialties in search results
                            val specialtiesInResults = searchResults
                                .map { normalizeSpecialty(it.speciality) }
                                .distinct()
                                .sorted()
                            
                            items(specialtiesInResults) { specialty ->
                                val count = searchResults.count { 
                                    normalizeSpecialty(it.speciality) == specialty 
                                }
                                FilterChip(
                                    selected = selectedSpecialty == specialty,
                                    onClick = { 
                                        selectedSpecialty = if (selectedSpecialty == specialty) null else specialty 
                                    },
                                    label = {
                                        Text(
                                            text = "$specialty ($count)",
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HeaderTopColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = BackgroundColor,
                                        labelColor = StatTextColor
                                    )
                                )
                            }
                        }
                    }

                    Divider(color = BackgroundColor, thickness = 1.dp)

                    // Filter search results by selected specialty
                    val filteredSearchResults = if (selectedSpecialty != null) {
                        searchResults.filter { normalizeSpecialty(it.speciality) == selectedSpecialty }
                    } else {
                        searchResults
                    }

                    // Search results count
                    Text(
                        text = if (selectedSpecialty != null) {
                            "${filteredSearchResults.size} doctor(s) found in $selectedSpecialty"
                        } else {
                            "${filteredSearchResults.size} doctor(s) found"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = StatTextColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Search results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSearchResults) { doctor ->
                            DoctorListCard(
                                doctor = doctor,
                                onBookAppointment = {
                                    navController.navigate(
                                        "select_datetime/${doctor.id}/${doctor.firstName}/${doctor.lastName}/${doctor.speciality}"
                                    )
                                }
                            )
                        }
                    }
                }
            } else if (selectedSpecialty == null) {
                // Main catalogue view - 2 column grid of all specialties
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundColor)
                ) {
                    // Search bar on main catalogue
                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search doctors by name, specialty...",
                        backgroundColor = CardWhite,
                        modifier = Modifier.padding(16.dp)
                    )

                    if (searchQuery.isEmpty()) {
                        // Specialties grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundColor),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(allSpecialties) { specialtyInfo ->
                                SpecialtyCategoryCard(
                                    specialtyInfo = specialtyInfo,
                                    doctorCount = doctorsBySpecialty[specialtyInfo.name]?.size ?: 0,
                                    onClick = { selectedSpecialty = specialtyInfo.name }
                                )
                            }
                        }
                    } else if (isSearching) {
                        // Loading indicator while searching
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HeaderTopColor)
                        }
                    } else if (searchResults.isEmpty()) {
                        // No results found
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = StatTextColor.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No doctors found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatTextColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try searching by name or specialization",
                                    fontSize = 14.sp,
                                    color = StatTextColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                // Doctor list view for selected specialty
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundColor)
                ) {
                    // Search bar in specialty view
                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search in ${selectedSpecialty}...",
                        backgroundColor = CardWhite,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Back to categories button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CardWhite,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSpecialty = null }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to categories",
                                tint = AccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Back to all specializations",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatTextColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Doctors list
                    if (filteredDoctors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = StatTextColor.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No doctors are available for this specialty right now.",
                                    fontSize = 16.sp,
                                    color = StatTextColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredDoctors) { doctor ->
                                DoctorListCard(
                                    doctor = doctor,
                                    onBookAppointment = {
                                        navController.navigate(
                                            "select_datetime/${doctor.id}/${doctor.firstName}/${doctor.lastName}/${doctor.speciality}"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Chat Button
            ChatFloatingButton(
                onClick = { navController.navigate("chatbot") },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun SpecialtyCategoryCard(
    specialtyInfo: SpecialtyInfo,
    doctorCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = AccentColor.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = specialtyInfo.icon,
                            contentDescription = specialtyInfo.name,
                            modifier = Modifier.size(28.dp),
                            tint = AccentColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Specialty name
                Text(
                    text = specialtyInfo.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description
                Text(
                    text = specialtyInfo.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = StatTextColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
            
            // Doctor count badge in top-right corner
            if (doctorCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ButtonColor,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Text(
                            text = doctorCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorListCard(
    doctor: DoctorFull,
    onBookAppointment: () -> Unit
) {
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
                onClick = onBookAppointment,
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
                    text = "Book Appointment",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
