package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependentViewRecordsScreen(
    navController: NavController,
    context: Context,
    dependentId: String,
    dependentName: String
) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    val scope = rememberCoroutineScope()

    // Load dependent records on screen open
    LaunchedEffect(dependentId) {
        scope.launch {
            val result = HealthRecordRepository.getDependentRecords(dependentId)
            if (result.isSuccess) {
                records = result.getOrNull() ?: emptyList()
            } else {
                Toast.makeText(
                    context,
                    "Error loading records: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            isLoading = false
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Record?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${recordToDelete?.fileName}'? This action cannot be undone.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = HealthRecordRepository.deleteRecord(recordToDelete!!.recordId)
                            if (result.isSuccess) {
                                Toast.makeText(context, "Record deleted", Toast.LENGTH_SHORT).show()
                                // Refresh list
                                val refreshResult = HealthRecordRepository.getDependentRecords(dependentId)
                                if (refreshResult.isSuccess) {
                                    records = refreshResult.getOrNull() ?: emptyList()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to delete: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showDeleteDialog = false
                            recordToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = StatTextColor)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Medical Records",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "For $dependentName",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        
                        // Upload button
                        IconButton(
                            onClick = {
                                val encodedName = URLEncoder.encode(dependentName, StandardCharsets.UTF_8.toString())
                                navController.navigate("dependent_upload_records/$dependentId/$encodedName")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Upload",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ButtonColor)
                    }
                }
                records.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = AccentColor.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No Records Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatTextColor
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Upload medical records, lab reports,\nand prescriptions for $dependentName",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = {
                                    val encodedName = URLEncoder.encode(dependentName, StandardCharsets.UTF_8.toString())
                                    navController.navigate("dependent_upload_records/$dependentId/$encodedName")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload First Record")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(records) { record ->
                            DependentRecordCard(
                                record = record,
                                onDelete = {
                                    recordToDelete = record
                                    showDeleteDialog = true
                                },
                                onView = {
                                    Toast.makeText(context, "Opening ${record.fileName}", Toast.LENGTH_SHORT).show()
                                    // TODO: Open file in viewer
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
private fun DependentRecordCard(
    record: HealthRecord,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardWhite,
        shadowElevation = 2.dp
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
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = AccentColor.copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    record.isImage() -> Icons.Default.Image
                                    record.isPdf() -> Icons.Default.Description
                                    else -> Icons.Default.AttachFile
                                },
                                contentDescription = null,
                                tint = ButtonColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = record.fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = record.getFormattedFileSize(),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Action buttons
                Row {
                    IconButton(onClick = onView) {
                        Icon(
                            Icons.Default.Visibility,
                            "View",
                            tint = ButtonColor
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = Color(0xFFE57373)
                        )
                    }
                }
            }
            
            if (record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = record.description,
                    fontSize = 13.sp,
                    color = StatTextColor.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Show notes if available
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${record.notes}",
                    fontSize = 12.sp,
                    color = ButtonColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Show past medication if available
            if (record.pastMedication.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Past Medication: ${record.pastMedication}",
                    fontSize = 12.sp,
                    color = AccentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Uploaded: ${formatRecordDate(record.uploadDate.toDate())}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatRecordDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}
