package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.HealthRecordRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

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
private val BorderColor = Color(0xFF0E4944)         // Deep Teal border

data class DependentFileSelection(
    val uri: Uri,
    val name: String,
    val size: Long,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependentUploadRecordsScreen(
    navController: NavController,
    context: Context,
    dependentId: String,
    dependentName: String
) {
    var selectedFiles by remember { mutableStateOf<List<DependentFileSelection>>(emptyList()) }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pastMedication by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

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
                            DependentFileSelection(uri, name, size, type)
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

                selectedFiles = selectedFiles + DependentFileSelection(uri, name, fileSize, type)
            } catch (e: Exception) {
                Toast.makeText(context, "Error processing photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = "Upload Records",
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
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instructions Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    shadowElevation = 2.dp
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
                                tint = ButtonColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Upload Guidelines",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StatTextColor
                            )
                        }
                        Text(
                            text = "• Supported formats: Images (JPG, PNG) and PDFs\n• Maximum size: 10MB per file\n• You can upload multiple files at once",
                            fontSize = 13.sp,
                            color = StatTextColor.copy(alpha = 0.7f)
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
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Camera", fontWeight = FontWeight.Bold)
                    }

                    // File Picker Button
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatTextColor),
                        shape = RoundedCornerShape(16.dp)
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
                        color = StatTextColor
                    )

                    selectedFiles.forEach { file ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CardWhite,
                            shadowElevation = 2.dp
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
                                    tint = ButtonColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = StatTextColor
                                    )
                                    Text(
                                        text = "${file.size / 1024} KB",
                                        fontSize = 12.sp,
                                        color = Color.Gray
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
                    color = StatTextColor
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("E.g., Lab results, X-ray report, etc.") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CardWhite,
                        focusedContainerColor = CardWhite,
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = ButtonColor
                    )
                )

                // Optional Notes
                Text(
                    text = "Notes (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StatTextColor
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Any additional notes about the record...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CardWhite,
                        focusedContainerColor = CardWhite,
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = ButtonColor
                    )
                )

                // Optional Past Medication
                Text(
                    text = "Past Medication (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = StatTextColor
                )

                OutlinedTextField(
                    value = pastMedication,
                    onValueChange = { pastMedication = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("List any relevant past medications...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CardWhite,
                        focusedContainerColor = CardWhite,
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = ButtonColor
                    )
                )

                // Privacy Toggle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPrivate) AccentColor.copy(alpha = 0.2f) else CardWhite,
                    shadowElevation = 2.dp
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
                                color = StatTextColor
                            )
                            Text(
                                text = "Only you can see this record. Doctors need your permission to access.",
                                fontSize = 12.sp,
                                color = StatTextColor.copy(alpha = 0.7f)
                            )
                        }
                        
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ButtonColor,
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

                            selectedFiles.forEachIndexed { index, file ->
                                uploadProgress = ((index + 1) * 100) / selectedFiles.size
                                
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
                                } else {
                                    failCount++
                                    Toast.makeText(
                                        context,
                                        "Failed to upload ${file.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            isUploading = false

                            Toast.makeText(
                                context,
                                "Uploaded: $successCount, Failed: $failCount",
                                Toast.LENGTH_LONG
                            ).show()

                            if (successCount > 0) {
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedFiles.isNotEmpty() && !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        disabledContainerColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp)
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
}
