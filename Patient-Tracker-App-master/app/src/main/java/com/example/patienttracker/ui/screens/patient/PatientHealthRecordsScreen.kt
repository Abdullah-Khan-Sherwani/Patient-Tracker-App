package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorNote
import com.example.patienttracker.data.DoctorNoteRepository
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import com.example.patienttracker.util.PrescriptionPdfGenerator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Color palette
private val DeepTeal = Color(0xFF0E4944)
private val MintAccent = Color(0xFF76DCB0)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val ErrorRed = Color(0xFFEF4444)

/**
 * Open a file URL in browser
 */
private fun openFileInBrowser(context: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "Error: File URL is empty", Toast.LENGTH_LONG).show()
        return
    }
    
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Patient Health Records Screen
 * Displays list of uploaded health records AND doctor prescriptions in tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHealthRecordsScreen(navController: NavController, context: Context) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var doctorNotes by remember { mutableStateOf<List<DoctorNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HealthRecord?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = My Uploads, 1 = Prescriptions
    val scope = rememberCoroutineScope()

    // Load records and doctor notes on screen open
    LaunchedEffect(Unit) {
        isLoading = true
        
        // Load health records
        loadRecords(
            onSuccess = { loadedRecords ->
                records = loadedRecords
            },
            onError = { error ->
                Toast.makeText(context, "Error loading records: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        
        // Load doctor notes/prescriptions
        val currentUserId = Firebase.auth.currentUser?.uid ?: ""
        android.util.Log.d("PatientHealthRecords", "Loading notes for patient: $currentUserId")
        
        if (currentUserId.isNotEmpty()) {
            val notesResult = DoctorNoteRepository.getNotesForPatient(currentUserId)
            if (notesResult.isSuccess) {
                doctorNotes = notesResult.getOrNull() ?: emptyList()
                android.util.Log.d("PatientHealthRecords", "Loaded ${doctorNotes.size} doctor notes")
            } else {
                android.util.Log.e("PatientHealthRecords", "Failed to load notes: ${notesResult.exceptionOrNull()?.message}")
            }
        }
        
        isLoading = false
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
                    containerColor = DeepTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            // Only show FAB on My Uploads tab
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { navController.navigate("upload_health_record_enhanced") },
                    containerColor = MintAccent,
                    contentColor = DeepTeal
                ) {
                    Icon(Icons.Default.Add, "Upload Record")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = DeepTeal
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "My Uploads (${records.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalHospital,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) MintAccent else DeepTeal.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Prescriptions (${doctorNotes.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
            
            // Content based on selected tab
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepTeal)
                    }
                }
                selectedTab == 0 -> {
                    // My Uploads Tab
                    if (records.isEmpty()) {
                        EmptyState(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(records) { record ->
                                HealthRecordCard(
                                    record = record,
                                    context = context,
                                    onDelete = {
                                        recordToDelete = record
                                        showDeleteDialog = true
                                    },
                                    onView = {
                                        openFileInBrowser(context, record.fileUrl)
                                    }
                                )
                            }
                        }
                    }
                }
                selectedTab == 1 -> {
                    // Prescriptions Tab
                    if (doctorNotes.isEmpty()) {
                        PrescriptionsEmptyState(modifier = Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(doctorNotes) { note ->
                                DoctorNoteCard(
                                    note = note,
                                    context = context,
                                    onViewPdf = {
                                        val pdfFile = PrescriptionPdfGenerator.generatePrescriptionPdf(context, note)
                                        if (pdfFile != null) {
                                            PrescriptionPdfGenerator.openPdf(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSharePdf = {
                                        val pdfFile = PrescriptionPdfGenerator.generatePrescriptionPdf(context, note)
                                        if (pdfFile != null) {
                                            PrescriptionPdfGenerator.sharePdf(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
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
                    Text("Delete", color = ErrorRed)
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
fun DoctorNoteCard(
    note: DoctorNote,
    context: Context,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Doctor Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MintAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = DeepTeal,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Dr. ${note.doctorName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DeepTeal
                        )
                        if (note.speciality.isNotBlank()) {
                            Text(
                                note.speciality,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Text(
                            note.getFormattedDate(),
                            fontSize = 12.sp,
                            color = MintAccent
                        )
                    }
                }
                
                Surface(
                    color = DeepTeal,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Prescription",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Divider(color = Color(0xFFE5E7EB))
            
            // Doctor's Comments
            if (note.comments.isNotBlank()) {
                Column {
                    Text(
                        "Doctor's Notes:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DeepTeal
                    )
                    Text(
                        note.comments,
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Prescription
            if (note.prescription.isNotBlank()) {
                Column {
                    Text(
                        "Prescription:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = DeepTeal
                    )
                    Surface(
                        color = MintAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            note.prescription,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = DeepTeal,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View PDF Button
                Button(
                    onClick = onViewPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("View PDF", fontSize = 13.sp)
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Share Button
                IconButton(onClick = onSharePdf) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MintAccent
                    )
                }
            }
        }
    }
}

@Composable
fun PrescriptionsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocalHospital,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MintAccent
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Prescriptions Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepTeal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Prescriptions from your doctors will\nappear here after your appointments",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun HealthRecordCard(
    record: HealthRecord,
    context: Context,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(24.dp),  // 24dp radius per design spec
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        tint = Color(0xFF0E4944),  // Deep Teal
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = record.fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),  // Dark charcoal
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
                        Icon(Icons.Default.Visibility, "View", tint = Color(0xFF0E4944))  // Deep Teal
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444))  // Error red
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
            
            // Show notes if available
            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${record.notes}",
                    fontSize = 12.sp,
                    color = Color(0xFF76DCB0),  // Mint accent
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
                    color = Color(0xFF0E4944),  // Deep Teal
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Uploaded: ${formatDate(record.uploadDate.toDate())}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280)  // Subtle text
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
    // Only load patient's own records (not dependents)
    val result = HealthRecordRepository.getPatientSelfRecords()
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
