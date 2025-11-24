package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

data class PatientInfo(
    val id: String,
    val name: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorViewRecordsScreen(
    navController: NavController,
    context: Context
) {
    var patients by remember { mutableStateOf<List<PatientInfo>>(emptyList()) }
    var selectedPatient by remember { mutableStateOf<PatientInfo?>(null) }
    var healthRecords by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoadingPatients by remember { mutableStateOf(true) }
    var isLoadingRecords by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = Firebase.firestore
                val snapshot = db.collection("users")
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                
                patients = snapshot.documents.mapNotNull { doc ->
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: ""
                    
                    if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                        PatientInfo(
                            id = doc.id,
                            name = "$firstName $lastName".trim(),
                            email = email
                        )
                    } else null
                }.sortedBy { it.name }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoadingPatients = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Patient Records", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedPatient != null) {
                            selectedPatient = null
                            healthRecords = emptyList()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        if (selectedPatient == null) {
            // Patient Selection View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select a Patient",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentColor,
                        focusedLeadingIconColor = AccentColor
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isLoadingPatients) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                } else {
                    val filteredPatients = patients.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
                    }
                    
                    if (filteredPatients.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "No patients found",
                                    fontSize = 18.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPatients) { patient ->
                                PatientCard(
                                    patient = patient,
                                    onClick = {
                                        selectedPatient = patient
                                        isLoadingRecords = true
                                        scope.launch {
                                            try {
                                                val result = HealthRecordRepository.getDoctorAccessibleRecordsForPatient(patient.id)
                                                healthRecords = result.getOrNull()?.sortedByDescending { it.uploadDate } ?: emptyList()
                                            } catch (e: Exception) {
                                                // Handle error
                                            } finally {
                                                isLoadingRecords = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Records View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                Text(
                    text = selectedPatient!!.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Text(
                    text = selectedPatient!!.email,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Medical Records",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(12.dp))
                
                if (isLoadingRecords) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                } else if (healthRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No records available",
                                fontSize = 18.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(healthRecords) { record ->
                            HealthRecordCard(
                                record = record,
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(record.fileUrl), "application/*")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Handle error - can't open file
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
private fun PatientCard(
    patient: PatientInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(AccentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = patient.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = patient.email,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun HealthRecordCard(
    record: HealthRecord,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.description.ifEmpty { record.fileName },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Uploaded: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(record.uploadDate.toDate())}",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}
