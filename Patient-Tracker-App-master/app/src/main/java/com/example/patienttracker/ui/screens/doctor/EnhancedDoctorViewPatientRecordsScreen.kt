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
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
private val GlassBreakColor = Color(0xFFFF5722)

enum class SortOption {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, TYPE
}

enum class FilterOption {
    ALL, IMAGES, PDFS, PRIVATE_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDoctorViewPatientRecordsScreen(
    navController: NavController,
    context: Context,
    patientUid: String,
    patientName: String
) {
    var allRecords by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var displayedRecords by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showGlassBreakDialog by remember { mutableStateOf(false) }
    var glassBreakReason by remember { mutableStateOf("") }
    var selectedPrivateRecord by remember { mutableStateOf<HealthRecord?>(null) }
    val scope = rememberCoroutineScope()

    // Load records
    LaunchedEffect(Unit) {
        isLoading = true
        val result = HealthRecordRepository.getDoctorAccessibleRecordsForPatient(patientUid)
        if (result.isSuccess) {
            allRecords = result.getOrNull() ?: emptyList()
            displayedRecords = allRecords
            
            // Record views for accessible records
            allRecords.forEach { record ->
                if (!record.isPrivate || record.doctorAccessList.contains(Firebase.auth.currentUser?.uid)) {
                    HealthRecordRepository.recordView(record.recordId, wasGlassBreak = false)
                }
            }
        } else {
            errorMessage = result.exceptionOrNull()?.message
        }
        isLoading = false
    }

    // Apply sorting and filtering
    LaunchedEffect(sortOption, filterOption, allRecords) {
        var filtered = when (filterOption) {
            FilterOption.ALL -> allRecords
            FilterOption.IMAGES -> allRecords.filter { it.isImage() }
            FilterOption.PDFS -> allRecords.filter { it.isPdf() }
            FilterOption.PRIVATE_ONLY -> allRecords.filter { it.isPrivate }
        }

        displayedRecords = when (sortOption) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.uploadDate.seconds }
            SortOption.DATE_ASC -> filtered.sortedBy { it.uploadDate.seconds }
            SortOption.NAME_ASC -> filtered.sortedBy { it.fileName }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.fileName }
            SortOption.TYPE -> filtered.sortedBy { it.fileType }
        }
    }

    // Glass Break Dialog
    if (showGlassBreakDialog && selectedPrivateRecord != null) {
        AlertDialog(
            onDismissRequest = { showGlassBreakDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = GlassBreakColor)
                    Spacer(Modifier.width(8.dp))
                    Text("Emergency Access", fontWeight = FontWeight.Bold, color = GlassBreakColor)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This is a PRIVATE record. By proceeding, you are initiating glass break emergency access.",
                        fontSize = 14.sp,
                        color = PrimaryColor
                    )
                    Text(
                        "• Patient will be notified\n• Admin will be notified\n• Access will be logged",
                        fontSize = 12.sp,
                        color = PrimaryColor.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = glassBreakReason,
                        onValueChange = { glassBreakReason = it },
                        label = { Text("Reason (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderColor,
                            focusedBorderColor = GlassBreakColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (glassBreakReason.isNotBlank()) {
                            scope.launch {
                                val result = HealthRecordRepository.glassBreakAccess(
                                    selectedPrivateRecord!!.recordId,
                                    glassBreakReason
                                )
                                if (result.isSuccess) {
                                    // Reload records
                                    val reloadResult = HealthRecordRepository.getDoctorAccessibleRecordsForPatient(patientUid)
                                    if (reloadResult.isSuccess) {
                                        allRecords = reloadResult.getOrNull() ?: emptyList()
                                    }
                                }
                                showGlassBreakDialog = false
                                glassBreakReason = ""
                            }
                        }
                    },
                    enabled = glassBreakReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassBreakColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Break Glass & Access", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showGlassBreakDialog = false
                    glassBreakReason = ""
                }) {
                    Text("Cancel", color = AccentColor)
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
                    Column {
                        Text(
                            "Patient Records", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            patientName,
                            fontSize = 14.sp,
                            color = AccentColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter Menu
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Files") },
                                onClick = {
                                    filterOption = FilterOption.ALL
                                    showFilterMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Description, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Images Only") },
                                onClick = {
                                    filterOption = FilterOption.IMAGES
                                    showFilterMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Image, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("PDFs Only") },
                                onClick = {
                                    filterOption = FilterOption.PDFS
                                    showFilterMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Private Only") },
                                onClick = {
                                    filterOption = FilterOption.PRIVATE_ONLY
                                    showFilterMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrivateColor) }
                            )
                        }
                    }
                    
                    // Sort Menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    sortOption = SortOption.DATE_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = {
                                    sortOption = SortOption.DATE_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name A-Z") },
                                onClick = {
                                    sortOption = SortOption.NAME_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name Z-A") },
                                onClick = {
                                    sortOption = SortOption.NAME_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("By Type") },
                                onClick = {
                                    sortOption = SortOption.TYPE
                                    showSortMenu = false
                                }
                            )
                        }
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
        } else if (displayedRecords.isEmpty()) {
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
                        "No records found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    )
                    Text(
                        if (filterOption != FilterOption.ALL) 
                            "Try changing the filter" 
                        else 
                            "Patient hasn't uploaded any records yet",
                        fontSize = 14.sp,
                        color = PrimaryColor.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Stats Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceColor.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${displayedRecords.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                            Text(
                                "Records",
                                fontSize = 11.sp,
                                color = AccentColor
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${displayedRecords.count { it.isImage() }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                            Text(
                                "Images",
                                fontSize = 11.sp,
                                color = AccentColor
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${displayedRecords.count { it.isPdf() }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                            Text(
                                "PDFs",
                                fontSize = 11.sp,
                                color = AccentColor
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${displayedRecords.count { it.isPrivate }}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrivateColor
                            )
                            Text(
                                "Private",
                                fontSize = 11.sp,
                                color = AccentColor
                            )
                        }
                    }
                }

                // Records List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedRecords) { record ->
                        DoctorRecordCard(
                            record = record,
                            onGlassBreak = {
                                selectedPrivateRecord = record
                                showGlassBreakDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorRecordCard(
    record: HealthRecord,
    onGlassBreak: () -> Unit
) {
    val currentUserUid = Firebase.auth.currentUser?.uid
    val canAccess = !record.isPrivate || 
                   record.doctorAccessList.contains(currentUserUid) ||
                   record.glassBreakAccess.any { it.doctorUid == currentUserUid }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAccess) { /* TODO: Open file viewer */ },
        colors = CardDefaults.cardColors(
            containerColor = when {
                !canAccess -> PrivateColor.copy(alpha = 0.15f)
                record.isPrivate -> PrivateColor.copy(alpha = 0.1f)
                else -> SurfaceColor
            }
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
                        tint = if (canAccess) AccentColor else PrivateColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            record.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryColor
                        )
                        Text(
                            record.getFormattedFileSize(),
                            fontSize = 12.sp,
                            color = AccentColor
                        )
                    }
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
                                "PRIVATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (canAccess) {
                Text(
                    record.description,
                    fontSize = 14.sp,
                    color = PrimaryColor.copy(alpha = 0.8f)
                )

                if (record.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Notes: ${record.notes}",
                        fontSize = 12.sp,
                        color = AccentColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                if (record.pastMedication.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Past Medication: ${record.pastMedication}",
                        fontSize = 12.sp,
                        color = AccentColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                Text(
                    "🔒 This is a private record. Click \"Break Glass\" to request emergency access.",
                    fontSize = 13.sp,
                    color = PrivateColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(record.uploadDate.toDate()),
                    fontSize = 12.sp,
                    color = AccentColor,
                    fontWeight = FontWeight.Medium
                )

                if (!canAccess) {
                    Button(
                        onClick = onGlassBreak,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassBreakColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Break Glass",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
