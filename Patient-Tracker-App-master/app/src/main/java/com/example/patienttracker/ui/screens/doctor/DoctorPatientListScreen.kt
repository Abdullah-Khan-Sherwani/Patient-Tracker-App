package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment
import com.example.patienttracker.data.AppointmentRepository
import kotlinx.coroutines.launch

// Teal/Mint Theme Colors
private val BackgroundColor = Color(0xFFF4F6F7)   // Light gray background
private val SurfaceColor = Color(0xFFFFFFFF)      // White cards
private val PrimaryColor = Color(0xFF0E4944)      // Deep teal text
private val AccentColor = Color(0xFF04786A)       // Teal accent
private val BorderColor = Color(0xFFE5E7EB)       // Light gray border
private val DependentColor = Color(0xFF04786A)    // Teal for dependents
private val MintAccent = Color(0xFF76DCB0)        // Mint green accent

/**
 * Data class representing a patient or dependent to display in the list
 */
data class PatientListItem(
    val patientUid: String,
    val displayName: String,
    val lastAppointmentDate: String,
    val isDependent: Boolean = false,
    val dependentId: String = "",
    val parentName: String = "" // For dependents, show their parent's name
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPatientListScreen(
    navController: NavController,
    context: Context
) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Load doctor's appointments to get patient list
    LaunchedEffect(Unit) {
        isLoading = true
        val result = AppointmentRepository.getDoctorAppointments()
        if (result.isSuccess) {
            appointments = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message
        }
        isLoading = false
    }

    // Get unique patients AND dependents from appointments
    val patientList = remember(appointments) {
        val patients = mutableListOf<PatientListItem>()
        val seenPatients = mutableSetOf<String>() // Track unique patientUid
        val seenDependents = mutableSetOf<String>() // Track unique dependentId
        
        // Sort appointments by date descending to get most recent first
        val sortedAppointments = appointments.sortedByDescending { it.appointmentDate.seconds }
        
        for (appointment in sortedAppointments) {
            if (appointment.recipientType == "dependent" && appointment.dependentId.isNotBlank()) {
                // This is a dependent appointment
                if (!seenDependents.contains(appointment.dependentId)) {
                    seenDependents.add(appointment.dependentId)
                    patients.add(
                        PatientListItem(
                            patientUid = appointment.patientUid, // Parent's UID for accessing records
                            displayName = appointment.dependentName.ifBlank { "Dependent" },
                            lastAppointmentDate = appointment.getFormattedDate(),
                            isDependent = true,
                            dependentId = appointment.dependentId,
                            parentName = appointment.patientName
                        )
                    )
                }
            } else {
                // This is a self appointment
                if (!seenPatients.contains(appointment.patientUid)) {
                    seenPatients.add(appointment.patientUid)
                    patients.add(
                        PatientListItem(
                            patientUid = appointment.patientUid,
                            displayName = appointment.patientName,
                            lastAppointmentDate = appointment.getFormattedDate(),
                            isDependent = false
                        )
                    )
                }
            }
        }
        
        // Sort: patients first, then dependents, alphabetically within each group
        patients.sortedWith(compareBy({ it.isDependent }, { it.displayName.lowercase() }))
    }

    // Filter by search query
    val filteredPatients = remember(patientList, searchQuery) {
        if (searchQuery.isBlank()) {
            patientList
        } else {
            patientList.filter { 
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                (it.isDependent && it.parentName.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Patients", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AccentColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = SurfaceColor,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentColor
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search patients or dependents...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            errorMessage!!,
                            color = Color.Red,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (filteredPatients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            if (searchQuery.isBlank()) 
                                "No patients yet" 
                            else 
                                "No patients found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor
                        )
                        Text(
                            if (searchQuery.isBlank()) 
                                "Patients will appear here after appointments" 
                            else 
                                "Try a different search term",
                            fontSize = 14.sp,
                            color = PrimaryColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Count patients and dependents
                val patientCount = filteredPatients.count { !it.isDependent }
                val dependentCount = filteredPatients.count { it.isDependent }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${filteredPatients.size} Total",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "$patientCount Patient${if (patientCount != 1) "s" else ""}",
                                    fontSize = 14.sp,
                                    color = AccentColor
                                )
                                if (dependentCount > 0) {
                                    Text(
                                        "$dependentCount Dependent${if (dependentCount != 1) "s" else ""}",
                                        fontSize = 14.sp,
                                        color = DependentColor
                                    )
                                }
                            }
                        }
                    }

                    items(filteredPatients) { patient ->
                        PatientListCard(
                            item = patient,
                            onClick = {
                                // Navigate to patient/dependent records with proper parameters
                                val encodedName = java.net.URLEncoder.encode(patient.displayName, "UTF-8")
                                if (patient.isDependent) {
                                    // For dependents, include dependentId
                                    val encodedDependentId = java.net.URLEncoder.encode(patient.dependentId, "UTF-8")
                                    navController.navigate(
                                        "doctor_view_patient_records_enhanced/${patient.patientUid}/$encodedName/$encodedDependentId"
                                    )
                                } else {
                                    // For patients, use empty dependentId
                                    navController.navigate(
                                        "doctor_view_patient_records_enhanced/${patient.patientUid}/$encodedName/_self"
                                    )
                                }
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PatientListCard(
    item: PatientListItem,
    onClick: () -> Unit
) {
    // Decode name in case it has URL encoding (+ for spaces)
    val displayName = item.displayName.replace("+", " ")
    val parentName = item.parentName.replace("+", " ")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (item.isDependent) 
            DependentColor.copy(alpha = 0.05f) 
        else 
            SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (item.isDependent) 0.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = if (item.isDependent) DependentColor else AccentColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isDependent) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = "Dependent",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = displayName
                                .split(" ")
                                .mapNotNull { it.firstOrNull() }
                                .take(2)
                                .joinToString(""),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryColor
                    )
                    
                    if (item.isDependent) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DependentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Dependent",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DependentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                if (item.isDependent && parentName.isNotBlank()) {
                    Text(
                        text = "Parent: $parentName",
                        fontSize = 12.sp,
                        color = DependentColor.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(2.dp))
                }
                
                Text(
                    text = "Last appointment: ${item.lastAppointmentDate}",
                    fontSize = 13.sp,
                    color = AccentColor
                )
            }

            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = "View Records",
                tint = if (item.isDependent) DependentColor else AccentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
