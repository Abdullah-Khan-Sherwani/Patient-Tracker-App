package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep Teal
private val ChipSelectedColor = Color(0xFF0E4944)   // Deep Teal for selected chips
private val ChipUnselectedColor = Color(0xFFE6F4F1) // Light teal for unselected chips

// Dark mode colors
private val DarkBackgroundColor = Color(0xFF0B0F12)
private val DarkCardColor = Color(0xFF1A2228)
private val DarkTextColor = Color(0xFFFFFFFF)
private val DarkSecondaryTextColor = Color(0xFFC3CCD2)
private val DarkIconTint = Color(0xFFFFFFFF)
private val DarkChipSelectedColor = Color(0xFF0F8B8D)
private val DarkChipUnselectedColor = Color(0xFF1A2228)

// ============================================================
// Symptom to Specialty Mapping (Filtered for available specialties)
// ============================================================
private val symptomToSpecialtyMap = mapOf(
    // General Physician
    "Fever" to listOf("General Physician"),
    "Cough" to listOf("General Physician"),
    "Allergic Reaction" to listOf("General Physician"),
    "Persistent Fatigue" to listOf("General Physician"),
    "Dehydration Symptoms" to listOf("General Physician"),
    "Heat Exhaustion" to listOf("General Physician"),
    "Swollen Lymph Nodes" to listOf("General Physician", "Oncologist"),
    
    // Cardiologist
    "Shortness of Breath" to listOf("Cardiologist"),
    "Chest Pain" to listOf("Cardiologist", "General Physician"),
    "Dizziness" to listOf("Cardiologist", "Neurologist"),
    "Palpitations" to listOf("Cardiologist"),
    "High Blood Pressure" to listOf("Cardiologist"),
    "Low Blood Pressure" to listOf("Cardiologist"),
    "Swelling in Legs" to listOf("Cardiologist"),
    "High Cholesterol" to listOf("Cardiologist"),
    "Cold Hands and Feet" to listOf("Cardiologist"),
    
    // Dermatologist
    "Skin Rash" to listOf("Dermatologist"),
    "Acne" to listOf("Dermatologist"),
    "Hair Loss" to listOf("Dermatologist"),
    "Itching" to listOf("Dermatologist"),
    "Hives" to listOf("Dermatologist"),
    
    // Neurologist
    "Headache" to listOf("Neurologist", "General Physician"),
    "Migraine" to listOf("Neurologist"),
    "Blurred Vision" to listOf("Neurologist"),
    "Muscle Weakness" to listOf("Neurologist"),
    "Numbness or Tingling" to listOf("Neurologist"),
    "Tremors" to listOf("Neurologist"),
    "Memory Loss" to listOf("Neurologist", "Psychiatrist"),
    "Sleep Problems" to listOf("Neurologist", "Psychiatrist"),
    "Hand Tremors" to listOf("Neurologist"),
    "Seizures" to listOf("Neurologist"),
    
    // Psychiatrist
    "Anxiety" to listOf("Psychiatrist"),
    "Depression" to listOf("Psychiatrist"),
    "Behavioral Issues" to listOf("Psychiatrist"),
    
    // ENT Specialist
    "Hearing Loss" to listOf("ENT Specialist"),
    "Ear Pain" to listOf("ENT Specialist"),
    "Sore Throat" to listOf("ENT Specialist", "General Physician"),
    "Blocked Nose" to listOf("ENT Specialist"),
    "Sinus Pain" to listOf("ENT Specialist"),
    "Difficulty Swallowing" to listOf("ENT Specialist"),
    
    // Orthopedic
    "Joint Pain" to listOf("Orthopedic"),
    "Back Pain" to listOf("Orthopedic"),
    "Neck Pain" to listOf("Orthopedic"),
    "Shoulder Pain" to listOf("Orthopedic"),
    "Knee Pain" to listOf("Orthopedic"),
    "Chronic Pain" to listOf("Orthopedic"),
    
    // Gynecologist
    "Menstrual Pain" to listOf("Gynecologist"),
    "Irregular Periods" to listOf("Gynecologist"),
    "Pregnancy Symptoms" to listOf("Gynecologist"),
    "Vaginal Discharge" to listOf("Gynecologist"),
    "Infertility" to listOf("Gynecologist"),
    
    // Pediatrician
    "Children Fever" to listOf("Pediatrician"),
    "Children Cough" to listOf("Pediatrician"),
    "Poor Growth in Children" to listOf("Pediatrician"),
    "Developmental Delay" to listOf("Pediatrician"),
    "Behavioral Problems in Children" to listOf("Pediatrician", "Psychiatrist"),
    
    // Dentist
    "Tooth Pain" to listOf("Dentist"),
    "Gum Bleeding" to listOf("Dentist"),
    "Chipped Tooth" to listOf("Dentist"),
    
    // Urologist
    "Urinary Burning" to listOf("Urologist"),
    "Frequent Urination" to listOf("Urologist"),
    "Blood in Urine" to listOf("Urologist"),
    
    // Oncologist
    "Breast Lump" to listOf("Oncologist")
)

// Get all symptoms sorted alphabetically
private fun getAllSymptoms(): List<String> = symptomToSpecialtyMap.keys.sorted()

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
fun DoctorCatalogueScreen(
    navController: NavController, 
    context: Context,
    preselectedSpecialty: String? = null
) {
    var allDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var selectedSpecialty by remember { mutableStateOf<String?>(preselectedSpecialty) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Symptom selection state
    var selectedSymptoms by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSymptomDropdownExpanded by remember { mutableStateOf(false) }
    var symptomSearchQuery by remember { mutableStateOf("") }
    
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Always show all specialties
    val allSpecialties = getAllSpecialties()
    val allSymptoms = remember { getAllSymptoms() }
    
    // Filter symptoms based on search query
    val filteredSymptoms = remember(symptomSearchQuery, allSymptoms) {
        if (symptomSearchQuery.isBlank()) {
            allSymptoms
        } else {
            allSymptoms.filter { it.lowercase().contains(symptomSearchQuery.lowercase()) }
        }
    }
    
    // Get specialties that match selected symptoms
    val symptomMatchedSpecialties = remember(selectedSymptoms) {
        if (selectedSymptoms.isEmpty()) {
            emptySet()
        } else {
            selectedSymptoms.flatMap { symptom ->
                symptomToSpecialtyMap[symptom] ?: emptyList()
            }.toSet()
        }
    }
    
    // Normalize doctor specialties and group by standardized names
    val doctorsBySpecialty = remember(allDoctors) {
        allDoctors.groupBy { normalizeSpecialty(it.speciality) }
    }
    
    // Filter doctors based on selected symptoms
    val symptomFilteredDoctors = remember(selectedSymptoms, allDoctors, symptomMatchedSpecialties) {
        if (selectedSymptoms.isEmpty()) {
            allDoctors
        } else {
            allDoctors.filter { doctor ->
                val normalizedSpec = normalizeSpecialty(doctor.speciality)
                symptomMatchedSpecialties.contains(normalizedSpec)
            }
        }
    }
    
    // Filtered doctors based on selected specialty (from symptom-filtered list)
    val filteredDoctors = remember(selectedSpecialty, symptomFilteredDoctors) {
        if (selectedSpecialty == null) {
            emptyList()
        } else if (selectedSpecialty == "General") {
            symptomFilteredDoctors
        } else {
            symptomFilteredDoctors.filter { normalizeSpecialty(it.speciality) == selectedSpecialty }
        }
    }
    
    // Local search results - instant filtering from symptom-filtered doctors
    val localSearchResults = remember(searchQuery, symptomFilteredDoctors) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val lowerQuery = searchQuery.lowercase().trim()
            symptomFilteredDoctors.filter { doctor ->
                val firstName = doctor.firstName.lowercase()
                val lastName = doctor.lastName.lowercase()
                val fullName = "$firstName $lastName"
                val speciality = doctor.speciality.lowercase()
                val normalizedSpeciality = normalizeSpecialty(doctor.speciality).lowercase()
                
                // Match doctor name OR speciality
                firstName.contains(lowerQuery) ||
                lastName.contains(lowerQuery) ||
                fullName.contains(lowerQuery) ||
                speciality.contains(lowerQuery) ||
                normalizedSpeciality.contains(lowerQuery)
            }
        }
    }
    
    // Filter specialties based on search query (for specialty grid filtering)
    val filteredSpecialties = remember(searchQuery, allSpecialties) {
        if (searchQuery.isBlank()) {
            allSpecialties
        } else {
            val lowerQuery = searchQuery.lowercase().trim()
            allSpecialties.filter { specialty ->
                specialty.name.lowercase().contains(lowerQuery) ||
                specialty.description.lowercase().contains(lowerQuery)
            }
        }
    }
    
    // Combine local and remote search results (prefer local for instant feedback)
    val effectiveSearchResults = remember(localSearchResults, searchResults, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            // Use local results for instant feedback, they're the same source anyway
            localSearchResults.ifEmpty { searchResults }
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
    
    // Handle search (still keep remote search for completeness)
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
            } else if (searchQuery.isNotEmpty() && effectiveSearchResults.isNotEmpty()) {
                // Search results view - shows doctor cards when searching
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundColor)
                ) {
                    // Search bar
                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search by doctor name or specialty...",
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
                                            text = "All (${effectiveSearchResults.size})",
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
                            val specialtiesInResults = effectiveSearchResults
                                .map { normalizeSpecialty(it.speciality) }
                                .distinct()
                                .sorted()
                            
                            items(specialtiesInResults) { specialty ->
                                val count = effectiveSearchResults.count { 
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
                        effectiveSearchResults.filter { normalizeSpecialty(it.speciality) == selectedSpecialty }
                    } else {
                        effectiveSearchResults
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
                        .background(if (isDarkMode) DarkBackgroundColor else BackgroundColor)
                ) {
                    // Search bar on main catalogue
                    SearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search by doctor name or specialty...",
                        backgroundColor = if (isDarkMode) DarkCardColor else CardWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    // Symptom Selection Dropdown
                    SymptomSelectionDropdown(
                        selectedSymptoms = selectedSymptoms,
                        onSymptomToggle = { symptom ->
                            selectedSymptoms = if (selectedSymptoms.contains(symptom)) {
                                selectedSymptoms - symptom
                            } else {
                                selectedSymptoms + symptom
                            }
                        },
                        onClearAll = { selectedSymptoms = emptySet() },
                        symptomSearchQuery = symptomSearchQuery,
                        onSymptomSearchChange = { symptomSearchQuery = it },
                        filteredSymptoms = filteredSymptoms,
                        isExpanded = isSymptomDropdownExpanded,
                        onExpandedChange = { isSymptomDropdownExpanded = it },
                        isDarkMode = isDarkMode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    // Show matched specialties info when symptoms are selected
                    if (selectedSymptoms.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkMode) DarkChipSelectedColor.copy(alpha = 0.2f) else ChipUnselectedColor
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = if (isDarkMode) DarkChipSelectedColor else AccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Showing ${symptomFilteredDoctors.size} doctor(s) matching your symptoms",
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) DarkTextColor else StatTextColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (searchQuery.isEmpty()) {
                        // Filter specialties based on selected symptoms
                        val displaySpecialties = if (selectedSymptoms.isEmpty()) {
                            allSpecialties
                        } else {
                            allSpecialties.filter { specialty ->
                                specialty.name == "General" || symptomMatchedSpecialties.contains(specialty.name)
                            }
                        }
                        
                        // Specialties grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDarkMode) DarkBackgroundColor else BackgroundColor),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displaySpecialties) { specialtyInfo ->
                                val doctorCount = if (selectedSymptoms.isEmpty()) {
                                    doctorsBySpecialty[specialtyInfo.name]?.size ?: 0
                                } else {
                                    symptomFilteredDoctors.count { normalizeSpecialty(it.speciality) == specialtyInfo.name }
                                }
                                SpecialtyCategoryCard(
                                    specialtyInfo = specialtyInfo,
                                    doctorCount = doctorCount,
                                    onClick = { selectedSpecialty = specialtyInfo.name }
                                )
                            }
                        }
                    } else if (isSearching && localSearchResults.isEmpty()) {
                        // Loading indicator while searching (only if no local results yet)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = HeaderTopColor)
                        }
                    } else if (effectiveSearchResults.isEmpty()) {
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
                                    text = "No doctors found for \"$searchQuery\"",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatTextColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try searching by doctor name (e.g., \"Dr. Smith\")\nor specialty (e.g., \"Cardiologist\")",
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

// ============================================================
// Symptom Selection Dropdown Component
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomSelectionDropdown(
    selectedSymptoms: Set<String>,
    onSymptomToggle: (String) -> Unit,
    onClearAll: () -> Unit,
    symptomSearchQuery: String,
    onSymptomSearchChange: (String) -> Unit,
    filteredSymptoms: List<String>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    val cardColor = if (isDarkMode) DarkCardColor else CardWhite
    val textColor = if (isDarkMode) DarkTextColor else StatTextColor
    val secondaryTextColor = if (isDarkMode) DarkSecondaryTextColor else StatTextColor.copy(alpha = 0.7f)
    val chipSelectedBg = if (isDarkMode) DarkChipSelectedColor else ChipSelectedColor
    val chipUnselectedBg = if (isDarkMode) DarkChipUnselectedColor else ChipUnselectedColor
    val borderColor = if (isDarkMode) DarkChipSelectedColor.copy(alpha = 0.5f) else AccentColor.copy(alpha = 0.3f)
    
    Column(modifier = modifier) {
        // Main dropdown card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = cardColor,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                // Dropdown header - clickable to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!isExpanded) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = null,
                            tint = if (isDarkMode) DarkChipSelectedColor else AccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (selectedSymptoms.isEmpty()) {
                                "Select symptoms to find specialists"
                            } else {
                                "${selectedSymptoms.size} symptom(s) selected"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedSymptoms.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAll,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear all",
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Selected symptoms chips (always visible when there are selections)
                if (selectedSymptoms.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selectedSymptoms.toList()) { symptom ->
                            InputChip(
                                selected = true,
                                onClick = { onSymptomToggle(symptom) },
                                label = {
                                    Text(
                                        text = symptom,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = chipSelectedBg,
                                    selectedLabelColor = Color.White,
                                    selectedTrailingIconColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = null
                            )
                        }
                    }
                }
                
                // Expanded content - search and symptom list
                if (isExpanded) {
                    Divider(color = borderColor, thickness = 1.dp)
                    
                    // Search field
                    OutlinedTextField(
                        value = symptomSearchQuery,
                        onValueChange = onSymptomSearchChange,
                        placeholder = {
                            Text(
                                "Search symptoms...",
                                fontSize = 14.sp,
                                color = secondaryTextColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (symptomSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSymptomSearchChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkMode) DarkChipSelectedColor else AccentColor,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = if (isDarkMode) DarkChipSelectedColor else AccentColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    
                    // Symptom list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredSymptoms) { symptom ->
                            val isSelected = selectedSymptoms.contains(symptom)
                            val specialties = symptomToSpecialtyMap[symptom] ?: emptyList()
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSymptomToggle(symptom) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    chipSelectedBg.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = symptom,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                if (isDarkMode) DarkChipSelectedColor else AccentColor
                                            } else {
                                                textColor
                                            }
                                        )
                                        Text(
                                            text = specialties.joinToString(", "),
                                            fontSize = 11.sp,
                                            color = secondaryTextColor,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onSymptomToggle(symptom) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = if (isDarkMode) DarkChipSelectedColor else AccentColor,
                                            uncheckedColor = secondaryTextColor,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        
                        if (filteredSymptoms.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No symptoms match \"$symptomSearchQuery\"",
                                        fontSize = 14.sp,
                                        color = secondaryTextColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    var weeklyAvailability by remember { mutableStateOf<List<DayAvailability>>(emptyList()) }
    var isLoadingAvailability by remember { mutableStateOf(true) }
    var isAvailableNow by remember { mutableStateOf<Boolean?>(null) } // null = unknown/loading
    val scope = rememberCoroutineScope()
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    
    // Availability chip colors - Deep Teal & Mint
    val AvailabilityChipBg = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFE6F4F1)  // Light mint tint
    val AvailabilityChipText = if (isDarkMode) Color(0xFFE5E5E5) else Color(0xFF0E4944)  // Deep Teal
    
    // Check favorite status
    LaunchedEffect(doctor.id) {
        try {
            isFavorite = PatientFavoritesRepository.isDoctorFavorited(doctor.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Fetch weekly availability from doctor_availability collection
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
                            scope.launch {
                                try {
                                    PatientFavoritesRepository.toggleFavorite(doctor.id)
                                    isFavorite = !isFavorite
                                } catch (e: Exception) {
                                    android.util.Log.e("DoctorCard", "Error toggling favorite: ${e.message}", e)
                                }
                            }
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
            Divider(
                color = if (isDarkMode) Color(0xFF37474F) else Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Full-width CTA button
            Button(
                onClick = onBookAppointment,
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
