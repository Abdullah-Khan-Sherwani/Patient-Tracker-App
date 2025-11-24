package com.example.patienttracker.ui.screens.patient

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
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Design Colors
private val BackgroundColor = Color(0xFFDDD2CE)
private val SurfaceColor = Color(0xFFF7ECE8)
private val PrimaryColor = Color(0xFF2F2019)
private val AccentColor = Color(0xFFB36B3C)
private val BorderColor = Color(0xFF9E8B82)
private val PrivateColor = Color(0xFFE57373)

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
    onViewAccessLog: () -> Unit,
    onDelete: () -> Unit
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isPrivate) PrivateColor.copy(alpha = 0.1f) else SurfaceColor
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            record.isImage() -> Icons.Default.Image
                            record.isPdf() -> Icons.Default.Description
                            else -> Icons.Default.AttachFile
                        },
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        record.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryColor
                    )
                }

                if (record.isPrivate) {
                    Surface(
                        color = PrivateColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Private",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                record.description,
                fontSize = 14.sp,
                color = PrimaryColor.copy(alpha = 0.7f)
            )

            if (record.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Notes: ${record.notes}",
                    fontSize = 12.sp,
                    color = AccentColor
                )
            }

            Spacer(Modifier.height(12.dp))

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
                        fontSize = 12.sp,
                        color = AccentColor
                    )
                    Text(
                        record.getFormattedFileSize(),
                        fontSize = 11.sp,
                        color = PrimaryColor.copy(alpha = 0.5f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // View Access Log Button
                    Button(
                        onClick = onViewAccessLog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentColor.copy(alpha = 0.2f),
                            contentColor = AccentColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${record.viewedBy.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
