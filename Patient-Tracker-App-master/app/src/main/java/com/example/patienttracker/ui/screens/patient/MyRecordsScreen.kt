package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val SurfaceColor = Color(0xFFFFFFFF)        // Card surface
private val PrimaryColor = Color(0xFF0E4944)        // Deep Teal
private val AccentColor = Color(0xFF76DCB0)         // Mint accent
private val BorderColor = Color(0xFF16605A)         // Lighter teal border
private val PrivateColor = Color(0xFFEF4444)        // Error red

/**
 * Open a file URL in an external app (browser, PDF viewer, image viewer)
 * For web URLs (http/https), opens directly in browser which handles PDFs natively
 */
fun openFileUrl(context: Context, url: String, mimeType: String) {
    android.util.Log.d("MyRecordsScreen", "=== OPEN FILE CALLED ===")
    android.util.Log.d("MyRecordsScreen", "URL: '$url'")
    android.util.Log.d("MyRecordsScreen", "MIME Type: '$mimeType'")
    android.util.Log.d("MyRecordsScreen", "URL length: ${url.length}")
    android.util.Log.d("MyRecordsScreen", "URL is blank: ${url.isBlank()}")
    
    if (url.isBlank()) {
        android.util.Log.e("MyRecordsScreen", "ERROR: URL is empty or blank!")
        android.widget.Toast.makeText(
            context,
            "Error: File URL is empty",
            android.widget.Toast.LENGTH_LONG
        ).show()
        return
    }
    
    try {
        // For web URLs, just open in browser - it handles PDFs and images natively
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        android.util.Log.d("MyRecordsScreen", "Starting activity with intent...")
        context.startActivity(browserIntent)
        android.util.Log.d("MyRecordsScreen", "Activity started successfully")
        
    } catch (e: Exception) {
        android.util.Log.e("MyRecordsScreen", "Error opening file: ${e.message}", e)
        android.widget.Toast.makeText(
            context,
            "Cannot open file: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecordsScreen(
    navController: NavController,
    context: Context
) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var doctorNotes by remember { mutableStateOf<List<DoctorNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var showAccessLog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = My Records, 1 = Prescriptions
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        // Fetch health records
        val result = HealthRecordRepository.getPatientRecords()
        if (result.isSuccess) {
            records = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message
        }
        
        // Fetch doctor notes/prescriptions
        val currentUserId = Firebase.auth.currentUser?.uid ?: ""
        android.util.Log.d("MyRecordsScreen", "Current user ID for notes: $currentUserId")
        
        if (currentUserId.isNotEmpty()) {
            val notesResult = DoctorNoteRepository.getNotesForPatient(currentUserId)
            if (notesResult.isSuccess) {
                doctorNotes = notesResult.getOrNull() ?: emptyList()
                android.util.Log.d("MyRecordsScreen", "Loaded ${doctorNotes.size} doctor notes")
            } else {
                android.util.Log.e("MyRecordsScreen", "Failed to load notes: ${notesResult.exceptionOrNull()?.message}")
            }
        } else {
            android.util.Log.e("MyRecordsScreen", "No current user ID found!")
        }
        
        isLoading = false
    }

    // Access Log Dialog
    if (showAccessLog && selectedRecord != null) {
        AlertDialog(
            onDismissRequest = { showAccessLog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = AccentColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Access Log", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedRecord!!.viewedBy.isEmpty()) {
                        item {
                            Text(
                                "No doctors have viewed this record yet.",
                                color = PrimaryColor.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        items(selectedRecord!!.viewedBy) { log ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (log.wasGlassBreak) 
                                        PrivateColor.copy(alpha = 0.2f) 
                                    else SurfaceColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            log.doctorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryColor
                                        )
                                        if (log.wasGlassBreak) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                color = PrivateColor,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "GLASS BREAK",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                            .format(log.viewedAt.toDate()),
                                        fontSize = 12.sp,
                                        color = AccentColor
                                    )
                                }
                            }
                        }
                    }

                    if (selectedRecord!!.glassBreakAccess.isNotEmpty()) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                "Emergency Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrivateColor
                            )
                        }
                        
                        items(selectedRecord!!.glassBreakAccess) { glassBreak ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = PrivateColor.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        glassBreak.doctorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryColor
                                    )
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                            .format(glassBreak.accessedAt.toDate()),
                                        fontSize = 12.sp,
                                        color = AccentColor
                                    )
                                    if (glassBreak.reason.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Reason: ${glassBreak.reason}",
                                            fontSize = 12.sp,
                                            color = PrimaryColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAccessLog = false }
                ) {
                    Text("Close", color = AccentColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "My Health Records", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Upload button
                    IconButton(
                        onClick = { navController.navigate("upload_health_record_enhanced") }
                    ) {
                        Icon(Icons.Default.Add, "Upload", tint = AccentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = PrimaryColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("upload_health_record_enhanced") },
                containerColor = PrimaryColor,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.CloudUpload, "Upload", tint = Color.White)
            }
        },
        containerColor = BackgroundColor
    ) { padding ->
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No records yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    )
                    Text(
                        "Upload your first health record",
                        fontSize = 14.sp,
                        color = PrimaryColor.copy(alpha = 0.6f)
                    )
                    
                    // Show prescriptions link if there are any
                    if (doctorNotes.isNotEmpty()) {
                        Text(
                            "You have ${doctorNotes.size} prescription(s) from doctors",
                            fontSize = 14.sp,
                            color = AccentColor,
                            modifier = Modifier.clickable { selectedTab = 1 }
                        )
                    }
                    
                    Button(
                        onClick = { navController.navigate("upload_health_record_enhanced") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload Record")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tab Row for switching between Records and Prescriptions
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceColor,
                    contentColor = PrimaryColor
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { 
                            Text(
                                "My Records (${records.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) AccentColor else PrimaryColor.copy(alpha = 0.6f)
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
                when (selectedTab) {
                    0 -> {
                        // My Records Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Stats Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "${records.size}",
                                                fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryColor
                                )
                                Text(
                                    "Total Records",
                                    fontSize = 12.sp,
                                    color = AccentColor
                                )
                            }
                            
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = BorderColor
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${records.count { it.isPrivate }}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrivateColor
                                )
                                Text(
                                    "Private",
                                    fontSize = 12.sp,
                                    color = AccentColor
                                )
                            }
                            
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = BorderColor
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${records.sumOf { it.viewedBy.size }}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentColor
                                )
                                Text(
                                    "Views",
                                    fontSize = 12.sp,
                                    color = AccentColor
                                )
                            }
                        }
                    }
                }

                items(records) { record ->
                    RecordCard(
                        record = record,
                        context = context,
                        onViewAccessLog = {
                            selectedRecord = record
                            showAccessLog = true
                        },
                        onDelete = {
                            scope.launch {
                                val result = HealthRecordRepository.deleteRecord(record.recordId)
                                if (result.isSuccess) {
                                    records = records.filter { it.recordId != record.recordId }
                                }
                            }
                        },
                        onOpenFile = {
                            android.util.Log.d("MyRecordsScreen", "RecordCard clicked! Opening: ${record.fileName}")
                            android.util.Log.d("MyRecordsScreen", "Record fileUrl: ${record.fileUrl}")
                            android.util.Log.d("MyRecordsScreen", "Record fileType: ${record.fileType}")
                            openFileUrl(context, record.fileUrl, record.fileType)
                        }
                    )
                }
            }
                    } // End of tab 0
                    
                    1 -> {
                        // Prescriptions Tab - Doctor's Notes
                        if (doctorNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = AccentColor,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        "No prescriptions yet",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryColor
                                    )
                                    Text(
                                        "Prescriptions from your doctors will appear here",
                                        fontSize = 14.sp,
                                        color = PrimaryColor.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(doctorNotes) { note ->
                                    PrescriptionCard(
                                        note = note,
                                        context = context,
                                        onViewPdf = {
                                            // Generate and open PDF
                                            val pdfFile = PrescriptionPdfGenerator.generatePrescriptionPdf(context, note)
                                            if (pdfFile != null) {
                                                PrescriptionPdfGenerator.openPdf(context, pdfFile)
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onSharePdf = {
                                            // Generate and share PDF
                                            val pdfFile = PrescriptionPdfGenerator.generatePrescriptionPdf(context, note)
                                            if (pdfFile != null) {
                                                PrescriptionPdfGenerator.sharePdf(context, pdfFile)
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed to generate PDF", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } // End of tab 1
                } // End of when
            } // End of Column
        }
    }
}

@Composable
fun RecordCard(
    record: HealthRecord,
    context: Context,
    onViewAccessLog: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Record?", fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = AccentColor)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFile() },
        colors = CardDefaults.cardColors(
            containerColor = if (record.isPrivate) PrivateColor.copy(alpha = 0.1f) else SurfaceColor
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail Preview
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentColor.copy(alpha = 0.1f))
                    .clickable { onOpenFile() },
                contentAlignment = Alignment.Center
            ) {
                if (record.isImage()) {
                    // Show image thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(record.fileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // PDF or other file - show icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            when {
                                record.isPdf() -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = if (record.isPdf()) Color(0xFFE53935) else AccentColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            record.getFileExtension().uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Tap to open overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.0f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Optional: show play/open icon on hover
                }
            }
            
            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            record.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Show dependent name if this is for a dependent
                        if (record.dependentName.isNotBlank()) {
                            Text(
                                "For: ${record.dependentName}",
                                fontSize = 11.sp,
                                color = AccentColor
                            )
                        }
                    }

                    if (record.isPrivate) {
                        Surface(
                            color = PrivateColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "Private",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                // Description
                if (record.description.isNotBlank()) {
                    Text(
                        record.description,
                        fontSize = 12.sp,
                        color = PrimaryColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (record.notes.isNotBlank()) {
                    Text(
                        "Notes: ${record.notes}",
                        fontSize = 11.sp,
                        color = AccentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(record.uploadDate.toDate()),
                            fontSize = 11.sp,
                            color = AccentColor
                        )
                        Text(
                            record.getFormattedFileSize(),
                            fontSize = 10.sp,
                            color = PrimaryColor.copy(alpha = 0.5f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Open File Button
                        Surface(
                            onClick = { 
                                android.util.Log.d("RecordCard", "Open button clicked for: ${record.fileName}")
                                onOpenFile() 
                            },
                            color = PrimaryColor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Open",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        // View Access Log Button
                        Surface(
                            onClick = onViewAccessLog,
                            color = AccentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AccentColor
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${record.viewedBy.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentColor
                                )
                            }
                        }

                        // Delete Button
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionCard(
    note: DoctorNote,
    context: Context,
    onViewPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        color = AccentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = PrimaryColor,
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
                            color = PrimaryColor
                        )
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(note.createdAt.toDate()),
                            fontSize = 12.sp,
                            color = AccentColor
                        )
                    }
                }
                
                Surface(
                    color = PrimaryColor,
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
            
            Divider(color = BorderColor.copy(alpha = 0.3f))
            
            // Doctor's Comments
            if (note.comments.isNotBlank()) {
                Column {
                    Text(
                        "Doctor's Notes:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PrimaryColor
                    )
                    Text(
                        note.comments,
                        fontSize = 14.sp,
                        color = PrimaryColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Prescriptions
            if (note.prescription.isNotBlank()) {
                Column {
                    Text(
                        "Prescriptions:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PrimaryColor
                    )
                    Surface(
                        color = AccentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            note.prescription,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = PrimaryColor
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
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
                        tint = AccentColor
                    )
                }
            }
        }
    }
}
