package com.example.patienttracker.ui.screens.guest

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.fetchDoctorsFromFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Medify Deep Teal & Mint color scheme
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep Teal

data class SpecialtyInfo(
    val name: String,
    val icon: ImageVector,
    val description: String
)

// Complete list of all specializations
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
        else -> "General"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDoctorsScreen(navController: NavController, context: Context) {
    var allDoctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedSpecialty by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                allDoctors = fetchDoctorsFromFirestore()
                println("GuestDoctorsScreen: Fetched ${allDoctors.size} doctors")
                allDoctors.forEach { doctor ->
                    println("Doctor: ${doctor.firstName} ${doctor.lastName}, Specialty: ${doctor.speciality}")
                }
            } catch (e: Exception) {
                println("GuestDoctorsScreen: Error fetching doctors - ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val specialties = getAllSpecialties()
    val definedSpecialties = specialties.map { it.name }.filter { it != "General" }

    // Group doctors by normalized specialty
    val doctorsBySpecialty = remember(allDoctors) {
        allDoctors.groupBy { doctor ->
            normalizeSpecialty(doctor.speciality)
        }
    }

    // Count doctors for each specialty
    val specialtyCounts = remember(allDoctors, specialties, definedSpecialties) {
        println("GuestDoctorsScreen: Total doctors for counting: ${allDoctors.size}")
        println("GuestDoctorsScreen: Doctors by specialty: $doctorsBySpecialty")
        specialties.associate { specialty ->
            val count = when (specialty.name) {
                "General" -> allDoctors.count { doctor ->
                    val normalized = normalizeSpecialty(doctor.speciality)
                    !definedSpecialties.contains(normalized)
                }
                else -> doctorsBySpecialty[specialty.name]?.size ?: 0
            }
            println("GuestDoctorsScreen: ${specialty.name} has $count doctors")
            specialty.name to count
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedSpecialty != null) {
                            IconButton(onClick = { selectedSpecialty = null }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back to specialties",
                                    tint = Color.White
                                )
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                        Text(
                            text = if (selectedSpecialty != null) selectedSpecialty!! else "Doctor Catalogue",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HeaderTopColor)
                }
            } else if (selectedSpecialty == null) {
                // Show specialty categories grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(specialties) { specialty ->
                        GuestSpecialtyCategoryCard(
                            specialty = specialty,
                            doctorCount = specialtyCounts[specialty.name] ?: 0,
                            onClick = { selectedSpecialty = specialty.name }
                        )
                    }
                }
            } else {
                // Show doctors for selected specialty
                val filteredDoctors = when (selectedSpecialty) {
                    "General" -> allDoctors.filter { doctor ->
                        val normalized = normalizeSpecialty(doctor.speciality)
                        !definedSpecialties.contains(normalized)
                    }
                    else -> doctorsBySpecialty[selectedSpecialty] ?: emptyList()
                }

                if (filteredDoctors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = StatTextColor.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No doctors available",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatTextColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No doctors are available for this specialty right now.",
                                fontSize = 14.sp,
                                color = StatTextColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredDoctors) { doctor ->
                            GuestDoctorListCard(
                                doctor = doctor,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestSpecialtyCategoryCard(
    specialty: SpecialtyInfo,
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
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Doctor count badge in top-right corner
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = AccentColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Text(
                        text = doctorCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = specialty.icon,
                    contentDescription = specialty.name,
                    modifier = Modifier.size(48.dp),
                    tint = HeaderTopColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = specialty.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatTextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = specialty.description,
                    fontSize = 11.sp,
                    color = StatTextColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GuestDoctorListCard(
    doctor: DoctorFull,
    navController: NavController
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile placeholder
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = HeaderTopColor.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = HeaderTopColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Doctor info
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
            }

            // Availability section
            if (doctor.days.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentColor.copy(alpha = 0.15f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = HeaderTopColor
                            )
                            Text(
                                text = "Available Days",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = doctor.days,
                            fontSize = 13.sp,
                            color = StatTextColor,
                            lineHeight = 18.sp
                        )

                        if (doctor.timings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = HeaderTopColor
                                )
                                Text(
                                    text = "Timings",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatTextColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = doctor.timings,
                                fontSize = 13.sp,
                                color = StatTextColor
                            )
                        }
                    }
                }
            }

            // Consultation fee
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Consultation Fee",
                        fontSize = 12.sp,
                        color = StatTextColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PKR 1500",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderTopColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign up prompt
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentColor.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = HeaderTopColor
                    )
                    Text(
                        text = "Sign up to book an appointment with this doctor",
                        fontSize = 13.sp,
                        color = StatTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // View Profile button
            Button(
                onClick = { navController.navigate("guest_doctor_details/${doctor.id}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "View Full Profile",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
