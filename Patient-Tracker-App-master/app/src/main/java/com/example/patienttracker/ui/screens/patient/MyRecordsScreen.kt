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
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
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
 */
fun openFileUrl(context: Context, url: String, mimeType: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        // Try to open with specific mime type first
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        // Final fallback - just open in browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(browserIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecordsScreen(
    navController: NavController,
    context: Context
) {
    var records by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var showAccessLog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        val result = HealthRecordRepository.getPatientRecords()
        if (result.isSuccess) {
            records = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                            openFileUrl(context, record.fileUrl, record.fileType)
                        }
                    )
                }
            }
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
