package com.example.patienttracker.ui.screens.doctor

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.patienttracker.data.AccessDeniedException
import com.example.patienttracker.data.HealthRecord
import com.example.patienttracker.data.HealthRecordRepository
import com.example.patienttracker.ui.components.CompactRecordCard
import com.example.patienttracker.ui.components.RecordDetailsBottomSheet
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.patienttracker.ui.components.PatientHealthSummaryCard

// ============================================================
// Deep Teal & Mint Design System - Matching App Theme
// WCAG Compliant - Professional Healthcare Theme
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Light teal-tinted background
private val SurfaceColor = Color(0xFFFFFFFF)        // White card surface
private val PrimaryColor = Color(0xFF0E4944)        // Deep teal for text
private val AccentColor = Color(0xFF0F8B8D)         // Teal accent
private val TextSecondary = Color(0xFF6B7280)       // Gray secondary text
private val BorderColor = Color(0xFFE5E7EB)         // Light gray border
private val PrivateColor = Color(0xFFDC2626)        // Red for private records
private val GlassBreakColor = Color(0xFFFF5722)     // Orange for glass break
private val AccessDeniedColor = Color(0xFFD32F2F)   // Red for access denied
private val MintAccent = Color(0xFF76DCB0)          // Mint green accent

/**
 * Open a file URL in an external app (browser, PDF viewer, image viewer)
 * For web URLs (http/https), opens directly in browser which handles PDFs natively
 */
fun openFileUrlDoctor(context: Context, url: String, mimeType: String) {
    try {
        android.util.Log.d("DoctorRecordsScreen", "Opening URL: $url with mimeType: $mimeType")
        
        // For web URLs, just open in browser - it handles PDFs and images natively
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(browserIntent)
        
    } catch (e: Exception) {
        android.util.Log.e("DoctorRecordsScreen", "Error opening file: ${e.message}", e)
        android.widget.Toast.makeText(
            context,
            "Cannot open file. Please try again.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

enum class SortOption {
    DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, TYPE
}

enum class FilterOption {
    ALL, IMAGES, PDFS, PRIVATE_ONLY
}

/**
 * Sealed UI State for doctor records screen
 */
sealed class DoctorRecordsUiState {
    object Loading : DoctorRecordsUiState()
    data class Success(val records: List<HealthRecord>) : DoctorRecordsUiState()
    data class Error(val message: String) : DoctorRecordsUiState()
    data class AccessDenied(val message: String) : DoctorRecordsUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDoctorViewPatientRecordsScreen(
    navController: NavController,
    context: Context,
    patientUid: String,
    patientName: String,
    dependentId: String = "" // Empty or "_self" means patient's own records
) {
    // Determine if viewing dependent or patient
    val isViewingDependent = dependentId.isNotBlank() && dependentId != "_self"
    
    var uiState by remember { mutableStateOf<DoctorRecordsUiState>(DoctorRecordsUiState.Loading) }
    var allRecords by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var displayedRecords by remember { mutableStateOf<List<HealthRecord>>(emptyList()) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showGlassBreakDialog by remember { mutableStateOf(false) }
    var glassBreakReason by remember { mutableStateOf("") }
    var selectedPrivateRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var detailsRecord by remember { mutableStateOf<HealthRecord?>(null) }
    val scope = rememberCoroutineScope()

    // Load records - separate for patient self vs dependent
    LaunchedEffect(patientUid, dependentId) {
        uiState = DoctorRecordsUiState.Loading
        
        val result = if (isViewingDependent) {
            // Get dependent's records only
            HealthRecordRepository.getDoctorAccessibleRecordsForDependent(patientUid, dependentId)
        } else {
            // Get patient's self records only (no dependents)
            HealthRecordRepository.getDoctorAccessibleRecordsForPatientSelf(patientUid)
        }
        
        uiState = if (result.isSuccess) {
            val records = result.getOrNull() ?: emptyList()
            allRecords = records
            displayedRecords = records
            
            // Record views for accessible records
            records.forEach { record ->
                if (!record.isPrivate || record.doctorAccessList.contains(Firebase.auth.currentUser?.uid)) {
                    HealthRecordRepository.recordView(record.recordId, wasGlassBreak = false)
                }
            }
            
            DoctorRecordsUiState.Success(records)
        } else {
            val exception = result.exceptionOrNull()
            if (exception is AccessDeniedException) {
                DoctorRecordsUiState.AccessDenied(exception.message ?: "Access denied")
            } else {
                DoctorRecordsUiState.Error(exception?.message ?: "Unknown error")
            }
        }
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
                                    // Reload records - use appropriate method based on dependent
                                    val reloadResult = if (isViewingDependent) {
                                        HealthRecordRepository.getDoctorAccessibleRecordsForDependent(patientUid, dependentId)
                                    } else {
                                        HealthRecordRepository.getDoctorAccessibleRecordsForPatientSelf(patientUid)
                                    }
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

    // Record Details Bottom Sheet for Doctors
    if (showDetailsSheet && detailsRecord != null) {
        val currentUserUid = Firebase.auth.currentUser?.uid
        val canAccess = !detailsRecord!!.isPrivate || 
                       detailsRecord!!.doctorAccessList.contains(currentUserUid) ||
                       detailsRecord!!.glassBreakAccess.any { it.doctorUid == currentUserUid }
        
        if (canAccess) {
            RecordDetailsBottomSheet(
                record = detailsRecord!!,
                context = context,
                onDismiss = { 
                    showDetailsSheet = false
                    detailsRecord = null
                },
                onOpenFile = {
                    openFileUrlDoctor(context, detailsRecord!!.fileUrl, detailsRecord!!.fileType)
                },
                onDownload = {
                    // Open in browser for download
                    openFileUrlDoctor(context, detailsRecord!!.fileUrl, detailsRecord!!.fileType)
                },
                onDelete = null, // Doctors cannot delete patient records
                onViewAccessLog = null, // Only patients see access logs
                showDeleteButton = false // Doctors cannot delete patient records
            )
        } else {
            // If can't access, trigger glass break dialog
            selectedPrivateRecord = detailsRecord
            showGlassBreakDialog = true
            showDetailsSheet = false
            detailsRecord = null
        }
    }

    // Teal color for dependent indicator (on white badge)
    val DependentBadgeColor = MintAccent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (isViewingDependent) "Dependent Records" else "Patient Records", 
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            if (isViewingDependent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "Dependent",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            patientName,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
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
                    containerColor = HeaderTopColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        
        when (val state = uiState) {
            is DoctorRecordsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HeaderTopColor)
                }
            }
            
            is DoctorRecordsUiState.AccessDenied -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(50),
                            color = AccessDeniedColor.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = AccessDeniedColor,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        Text(
                            "Access Denied",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccessDeniedColor
                        )
                        
                        Text(
                            state.message,
                            fontSize = 15.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Surface(
                            color = SurfaceColor,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "To access patient records:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HeaderTopColor
                                )
                                Text(
                                    "• Patient must have an appointment with you scheduled for today or later",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    "• Or you must have previously completed an appointment with this patient",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = HeaderTopColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Go Back", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            is DoctorRecordsUiState.Error -> {
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
                            state.message,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            is DoctorRecordsUiState.Success -> {
                if (displayedRecords.isEmpty()) {
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
                            // Health Summary Card even when no records
                            PatientHealthSummaryCard(
                                patientUid = patientUid,
                                dependentId = if (isViewingDependent) dependentId else "",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = HeaderTopColor,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "No records found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderTopColor
                            )
                            Text(
                                if (filterOption != FilterOption.ALL) 
                                    "Try changing the filter" 
                                else 
                                    "Patient hasn't uploaded any records yet",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Patient Health Summary Card
                        PatientHealthSummaryCard(
                            patientUid = patientUid,
                            dependentId = if (isViewingDependent) dependentId else "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Stats Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = HeaderTopColor.copy(alpha = 0.08f)
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
                                        color = HeaderTopColor
                                    )
                                    Text(
                                        "Records",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${displayedRecords.count { it.isImage() }}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeaderTopColor
                                    )
                                    Text(
                                        "Images",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${displayedRecords.count { it.isPdf() }}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeaderTopColor
                                    )
                                    Text(
                                        "PDFs",
                                        fontSize = 11.sp,
                                        color = TextSecondary
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
                                        color = TextSecondary
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
                                val currentUserUid = Firebase.auth.currentUser?.uid
                                val canAccess = !record.isPrivate || 
                                               record.doctorAccessList.contains(currentUserUid) ||
                                               record.glassBreakAccess.any { it.doctorUid == currentUserUid }
                                
                                CompactRecordCard(
                                    record = record,
                                    context = context,
                                    onOpenFile = {
                                        if (canAccess) {
                                            openFileUrlDoctor(context, record.fileUrl, record.fileType)
                                        } else {
                                            selectedPrivateRecord = record
                                            showGlassBreakDialog = true
                                        }
                                    },
                                    onCardClick = {
                                        if (canAccess) {
                                            // Open the details bottom sheet
                                            detailsRecord = record
                                            showDetailsSheet = true
                                        } else {
                                            // Trigger glass break dialog for private records
                                            selectedPrivateRecord = record
                                            showGlassBreakDialog = true
                                        }
                                    },
                                    showGlassBreakButton = !canAccess,
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
    }
}
