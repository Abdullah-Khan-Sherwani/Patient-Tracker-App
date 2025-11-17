package com.example.patienttracker.ui.screens.patient

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
 * Patient Health Records Screen
 * Displays list of uploaded health records with upload/delete options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHealthRecordsScreen(navController: NavController, context: Context) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    val scope = rememberCoroutineScope()

    // Load records on screen open
    LaunchedEffect(Unit) {
        loadRecords(
            onSuccess = { loadedRecords ->
                records = loadedRecords
                isLoading = false
            },
            onError = { error ->
                Toast.makeText(context, "Error loading records: ${error.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Health Records") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("upload_health_record") },
                containerColor = Color(0xFF05B8C7),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Upload Record")
            }
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
                records.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(records) { record ->
                            HealthRecordCard(
                                record = record,
                                onDelete = {
                                    recordToDelete = record
                                    showDeleteDialog = true
                                },
                                onView = {
                                    // Navigate to detail view or open file
                                    Toast.makeText(context, "Opening ${record.fileName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record?") },
            text = { Text("Are you sure you want to delete '${recordToDelete?.fileName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = HealthRecordRepository.deleteRecord(recordToDelete!!.recordId)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                                // Refresh list
                                loadRecords(
                                    onSuccess = { records = it },
                                    onError = {}
                                )
                            } else {
                                Toast.makeText(context, "Failed to delete: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                            recordToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HealthRecordCard(
    record: HealthRecord,
    onDelete: () -> Unit,
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
                // File icon + name
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
                
                // Action buttons
                Row {
                    IconButton(onClick = onView) {
                        Icon(Icons.Default.Visibility, "View", tint = Color(0xFF05B8C7))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                    }
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
fun EmptyState(modifier: Modifier = Modifier) {
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
            text = "No Health Records",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Upload your medical records, lab reports,\nand prescriptions to keep them organized",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private suspend fun loadRecords(
    onSuccess: (List<HealthRecord>) -> Unit,
    onError: (Exception) -> Unit
) {
    val result = HealthRecordRepository.getPatientRecords()
    if (result.isSuccess) {
        onSuccess(result.getOrNull() ?: emptyList())
    } else {
        onError(result.exceptionOrNull() as? Exception ?: Exception("Unknown error"))
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}
