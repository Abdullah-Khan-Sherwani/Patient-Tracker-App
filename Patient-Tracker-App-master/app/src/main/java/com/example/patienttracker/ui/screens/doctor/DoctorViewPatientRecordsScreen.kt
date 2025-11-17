package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Doctor View Patient Records Screen
 * Allows doctors to view health records of patients with active appointments (2-day window)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorViewPatientRecordsScreen(
    navController: NavController,
    context: Context,
    patientUid: String,
    patientName: String = "Patient"
) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var accessDenied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load patient records
    LaunchedEffect(Unit) {
        scope.launch {
            val result = HealthRecordRepository.getDoctorAccessibleRecords(patientUid)
            isLoading = false
            
            if (result.isSuccess) {
                val loadedRecords = result.getOrNull() ?: emptyList()
                if (loadedRecords.isEmpty()) {
                    accessDenied = true
                } else {
                    records = loadedRecords
                }
            } else {
                Toast.makeText(
                    context,
                    "Error loading records: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
                accessDenied = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$patientName's Records") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF05B8C7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF05B8C7)
                    )
                }
                accessDenied -> {
                    AccessDeniedState(modifier = Modifier.align(Alignment.Center))
                }
                records.isEmpty() -> {
                    NoRecordsState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Info banner
                        item {
                            InfoBanner()
                        }
                        
                        items(records) { record ->
                            DoctorHealthRecordCard(
                                record = record,
                                onView = {
                                    Toast.makeText(context, "Opening ${record.fileName}", Toast.LENGTH_SHORT).show()
                                    // TODO: Implement file viewer or download
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
fun InfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "You have read-only access to these records based on your appointment.",
                fontSize = 12.sp,
                color = Color(0xFF1E40AF)
            )
        }
    }
}

@Composable
fun DoctorHealthRecordCard(
    record: HealthRecord,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            record.isImage() -> Icons.Default.Image
                            record.isPdf() -> Icons.Default.Description
                            else -> Icons.Default.AttachFile
                        },
                        contentDescription = null,
                        tint = Color(0xFF05B8C7),
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = record.fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = record.getFormattedFileSize(),
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                
                IconButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, "View", tint = Color(0xFF05B8C7))
                }
            }
            
            if (record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = record.description,
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Uploaded: ${formatDate(record.uploadDate.toDate())}",
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
fun AccessDeniedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD1D5DB)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Access Not Available",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You can only access patient records within 2 days\nafter an appointment with this patient.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun NoRecordsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD1D5DB)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Records Available",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This patient hasn't uploaded any health records yet.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}
