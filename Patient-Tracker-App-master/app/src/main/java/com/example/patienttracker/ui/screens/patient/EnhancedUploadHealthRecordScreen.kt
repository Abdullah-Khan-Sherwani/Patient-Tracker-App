package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.patienttracker.data.Dependent
import com.example.patienttracker.data.DependentRepository
import com.example.patienttracker.data.HealthRecordRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val SurfaceColor = Color(0xFFFFFFFF)        // Card surface
private val PrimaryColor = Color(0xFF0E4944)        // Deep Teal
private val AccentColor = Color(0xFF76DCB0)         // Mint accent
private val BorderColor = Color(0xFF16605A)         // Lighter Teal border

data class FileSelection(
    val uri: Uri,
    val name: String,
    val size: Long,
    val type: String
)

// Represents upload target - either self or a dependent
sealed class UploadTarget {
    object Self : UploadTarget()
    data class ForDependent(val dependent: Dependent) : UploadTarget()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedUploadHealthRecordScreen(
    navController: NavController,
    context: Context
) {
    var selectedFiles by remember { mutableStateOf<List<FileSelection>>(emptyList()) }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pastMedication by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // Dependent selection state
    var dependents by remember { mutableStateOf<List<Dependent>>(emptyList()) }
    var selectedTarget by remember { mutableStateOf<UploadTarget>(UploadTarget.Self) }
    var showTargetDropdown by remember { mutableStateOf(false) }
    var isLoadingDependents by remember { mutableStateOf(true) }
    
    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load dependents on screen load
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            try {
                dependents = DependentRepository.getDependentsForParent(currentUser.uid)
            } catch (e: Exception) {
                android.util.Log.e("EnhancedUpload", "Error loading dependents: ${e.message}")
            }
        }
        isLoadingDependents = false
    }

    // Multi-file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newFiles = uris.mapNotNull { uri ->
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        
                        val name = if (nameIndex >= 0) c.getString(nameIndex) else "unknown"
                        val size = if (sizeIndex >= 0) c.getLong(sizeIndex) else 0L
                        val type = contentResolver.getType(uri) ?: "application/octet-stream"
                        
                        // Validate file type and size
                        if ((type.startsWith("image/") || type == "application/pdf") && size <= 10 * 1024 * 1024) {
                            FileSelection(uri, name, size, type)
                        } else {
                            Toast.makeText(
                                context,
                                "Invalid file: $name. Must be image/PDF under 10MB",
                                Toast.LENGTH_SHORT
                            ).show()
                            null
                        }
                    } else null
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
        }
        selectedFiles = selectedFiles + newFiles
    }

    // Camera launcher for quick photo capture
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            try {
                val uri = capturedImageUri!!
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri) ?: "image/jpeg"
                val name = "camera_${System.currentTimeMillis()}.jpg"
                
                // Try to query the actual size using OpenableColumns, fall back to stream.available()
                val fileSize = try {
                    var sizeVal = 0L
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex >= 0) sizeVal = c.getLong(sizeIndex)
                        }
                    }
                    if (sizeVal <= 0L) {
                        contentResolver.openInputStream(uri)?.available()?.toLong() ?: 0L
                    } else sizeVal
                } catch (e: Exception) {
                    0L
                }

                selectedFiles = selectedFiles + FileSelection(uri, name, fileSize, type)
            } catch (e: Exception) {
                Toast.makeText(context, "Error processing photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Upload Health Records", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor,
                    titleContentColor = PrimaryColor
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload Target Selector (Self or Dependent)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Upload For",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryColor
                    )
                    
                    if (isLoadingDependents) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Loading...", color = PrimaryColor.copy(alpha = 0.7f))
                        }
                    } else {
                        // Dropdown for selecting target
                        ExposedDropdownMenuBox(
                            expanded = showTargetDropdown,
                            onExpandedChange = { showTargetDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = when (val target = selectedTarget) {
                                    is UploadTarget.Self -> "Myself"
                                    is UploadTarget.ForDependent -> "${target.dependent.getFullName()} (${target.dependent.relationship})"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTargetDropdown)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (selectedTarget) {
                                            is UploadTarget.Self -> Icons.Default.Person
                                            is UploadTarget.ForDependent -> Icons.Default.People
                                        },
                                        contentDescription = null,
                                        tint = AccentColor
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = BackgroundColor,
                                    focusedContainerColor = BackgroundColor,
                                    unfocusedBorderColor = BorderColor,
                                    focusedBorderColor = AccentColor
                                )
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showTargetDropdown,
                                onDismissRequest = { showTargetDropdown = false }
                            ) {
                                // Self option
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = PrimaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text("Myself")
                                        }
                                    },
                                    onClick = {
                                        selectedTarget = UploadTarget.Self
                                        showTargetDropdown = false
                                    }
                                )
                                
                                // Dependent options
                                dependents.forEach { dependent ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.People,
                                                    contentDescription = null,
                                                    tint = PrimaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        dependent.getFullName(),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        dependent.relationship,
                                                        fontSize = 12.sp,
                                                        color = PrimaryColor.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedTarget = UploadTarget.ForDependent(dependent)
                                            showTargetDropdown = false
                                        }
                                    )
                                }
                                
                                // Add dependent option if none exist
                                if (dependents.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    tint = AccentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text("Add a Dependent", color = AccentColor)
                                            }
                                        },
                                        onClick = {
                                            showTargetDropdown = false
                                            navController.navigate("patient_dependents")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Upload Guidelines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryColor
                        )
                    }
                    Text(
                        text = "• Supported formats: Images (JPG, PNG) and PDFs\n• Maximum size: 10MB per file\n• You can upload multiple files at once",
                        fontSize = 13.sp,
                        color = PrimaryColor.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Camera Button
                Button(
                    onClick = {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg").also { it.createNewFile() }
                            )
                            capturedImageUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Camera", fontWeight = FontWeight.Bold)
                }

                // File Picker Button
                // Use "*/*" so the picker returns PDFs and images; validation filters later
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse", fontWeight = FontWeight.Bold)
                }
            }

            // Selected Files List
            if (selectedFiles.isNotEmpty()) {
                Text(
                    text = "Selected Files (${selectedFiles.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryColor
                )

                selectedFiles.forEach { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when {
                                    file.type.startsWith("image/") -> Icons.Default.Image
                                    file.type == "application/pdf" -> Icons.Default.Description
                                    else -> Icons.Default.AttachFile
                                },
                                contentDescription = null,
                                tint = AccentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                                Text(
                                    text = "${file.size / 1024} KB",
                                    fontSize = 12.sp,
                                    color = AccentColor
                                )
                            }
                            
                            IconButton(
                                onClick = { selectedFiles = selectedFiles.filter { it != file } }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // Description Field
            Text(
                text = "Description",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryColor
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("E.g., Lab results, X-ray report, etc.") },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = AccentColor
                )
            )

            // Optional Notes
            Text(
                text = "Notes (Optional)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryColor
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Any additional notes about the record...") },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = AccentColor
                )
            )

            // Optional Past Medication
            Text(
                text = "Past Medication (Optional)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryColor
            )

            OutlinedTextField(
                value = pastMedication,
                onValueChange = { pastMedication = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("List any relevant past medications...") },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = AccentColor
                )
            )

            // Privacy Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPrivate) AccentColor.copy(alpha = 0.2f) else SurfaceColor
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private Record",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryColor
                        )
                        Text(
                            text = "Only you can see this record. Doctors need your permission to access.",
                            fontSize = 12.sp,
                            color = PrimaryColor.copy(alpha = 0.7f)
                        )
                    }
                    
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BorderColor
                        )
                    )
                }
            }

            // Upload Button
            Button(
                onClick = {
                    if (selectedFiles.isEmpty()) {
                        Toast.makeText(context, "Please select at least one file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (description.isBlank()) {
                        Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        isUploading = true
                        var successCount = 0
                        var failCount = 0
                        
                        // Determine dependent info based on selected target
                        val (dependentId, dependentName) = when (val target = selectedTarget) {
                            is UploadTarget.Self -> "" to ""
                            is UploadTarget.ForDependent -> target.dependent.dependentId to target.dependent.getFullName()
                        }

                        selectedFiles.forEachIndexed { index, file ->
                            uploadProgress = ((index + 1) * 100) / selectedFiles.size
                            
                            android.util.Log.d("HealthRecordUpload", "Uploading file: ${file.name}, type: ${file.type}, size: ${file.size}")
                            android.util.Log.d("HealthRecordUpload", "Target: ${if (dependentId.isBlank()) "Self" else "Dependent: $dependentName"}")
                            
                            val result = HealthRecordRepository.uploadRecord(
                                fileUri = file.uri,
                                fileName = file.name,
                                fileType = file.type,
                                fileSize = file.size,
                                description = description,
                                context = context,
                                isPrivate = isPrivate,
                                notes = notes,
                                pastMedication = pastMedication,
                                dependentId = dependentId,
                                dependentName = dependentName
                            )

                            if (result.isSuccess) {
                                successCount++
                                android.util.Log.d("HealthRecordUpload", "Upload successful: ${file.name}")
                            } else {
                                failCount++
                                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                android.util.Log.e("HealthRecordUpload", "Upload failed for ${file.name}: $errorMsg", result.exceptionOrNull())
                            }
                        }

                        isUploading = false

                        if (successCount > 0 && failCount == 0) {
                            // Show success snackbar
                            snackbarHostState.showSnackbar(
                                message = "Record uploaded successfully",
                                duration = SnackbarDuration.Short
                            )
                            // Navigate back after showing snackbar
                            navController.popBackStack()
                        } else if (successCount > 0) {
                            snackbarHostState.showSnackbar(
                                message = "Uploaded: $successCount, Failed: $failCount",
                                duration = SnackbarDuration.Long
                            )
                            navController.popBackStack()
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "Upload failed. Please try again.",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedFiles.isNotEmpty() && !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    disabledContainerColor = BorderColor
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Uploading... $uploadProgress%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Upload ${selectedFiles.size} File(s)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
