package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.patienttracker.data.HealthRecordRepository
import kotlinx.coroutines.launch

// Design Colors
private val BackgroundColor = Color(0xFFDDD2CE)
private val SurfaceColor = Color(0xFFF7ECE8)
private val PrimaryColor = Color(0xFF2F2019)
private val AccentColor = Color(0xFFB36B3C)
private val BorderColor = Color(0xFF9E8B82)

data class FileSelection(
    val uri: Uri,
    val name: String,
    val size: Long,
    val type: String
)

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

                        selectedFiles.forEachIndexed { index, file ->
                            uploadProgress = ((index + 1) * 100) / selectedFiles.size
                            
                            android.util.Log.d("HealthRecordUpload", "Uploading file: ${file.name}, type: ${file.type}, size: ${file.size}")
                            
                            val result = HealthRecordRepository.uploadRecord(
                                fileUri = file.uri,
                                fileName = file.name,
                                fileType = file.type,
                                fileSize = file.size,
                                description = description,
                                isPrivate = isPrivate,
                                notes = notes,
                                pastMedication = pastMedication
                            )

                            if (result.isSuccess) {
                                successCount++
                                android.util.Log.d("HealthRecordUpload", "Upload successful: ${file.name}")
                            } else {
                                failCount++
                                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                android.util.Log.e("HealthRecordUpload", "Upload failed for ${file.name}: $errorMsg", result.exceptionOrNull())
                                Toast.makeText(
                                    context,
                                    "Failed to upload ${file.name}: $errorMsg",
                                    Toast.LENGTH_LONG
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
                    containerColor = PrimaryColor,
                    disabledContainerColor = BorderColor
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        progress = uploadProgress / 100f
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
