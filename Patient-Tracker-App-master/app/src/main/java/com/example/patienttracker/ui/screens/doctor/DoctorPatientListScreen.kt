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

// Design Colors
private val BackgroundColor = Color(0xFFFAF8F3)
private val SurfaceColor = Color(0xFFF5F0E8)
private val PrimaryColor = Color(0xFF2F2019)
private val AccentColor = Color(0xFFB8956A)
private val BorderColor = Color(0xFFD4C4B0)

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

    // Get unique patients
    val uniquePatients = remember(appointments) {
        appointments
            .distinctBy { it.patientUid }
            .sortedBy { it.patientName }
    }

    // Filter patients by search query
    val filteredPatients = remember(uniquePatients, searchQuery) {
        if (searchQuery.isBlank()) {
            uniquePatients
        } else {
            uniquePatients.filter { 
                it.patientName.contains(searchQuery, ignoreCase = true)
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
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = PrimaryColor
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
                        placeholder = { Text("Search patients...") },
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${filteredPatients.size} Patient${if (filteredPatients.size != 1) "s" else ""}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(filteredPatients) { appointment ->
                        PatientListCard(
                            patientName = appointment.patientName,
                            patientUid = appointment.patientUid,
                            lastAppointment = appointment.getFormattedDate(),
                            onClick = {
                                navController.navigate(
                                    "doctor_view_patient_records_enhanced/${appointment.patientUid}/${appointment.patientName}"
                                )
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
    patientName: String,
    patientUid: String,
    lastAppointment: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                color = AccentColor
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = patientName
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

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Last appointment: $lastAppointment",
                    fontSize = 13.sp,
                    color = AccentColor
                )
            }

            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = "View Records",
                tint = AccentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
