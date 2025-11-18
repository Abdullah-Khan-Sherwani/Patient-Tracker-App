package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

/**
 * Upload Health Record Screen
 * Allows patients to upload medical files (images, PDFs) with descriptions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadHealthRecordScreen(navController: NavController, context: Context) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var fileType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Get file info
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        
                        fileName = if (nameIndex >= 0) c.getString(nameIndex) else "unknown"
                        fileSize = if (sizeIndex >= 0) c.getLong(sizeIndex) else 0L
                        fileType = contentResolver.getType(it) ?: "application/octet-stream"
                        selectedFileUri = it
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Health Record") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF05B8C7),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            Text(
                text = "Upload Medical Records",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            
            Text(
                text = "Upload lab reports, prescriptions, X-rays, or other medical documents. Supported formats: Images (JPG, PNG) and PDFs. Max size: 10MB.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )

            // File picker button
            OutlinedButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(2.dp, Color(0xFF05B8C7), RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF0F9FF)
                )
            ) {
                if (selectedFileUri == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF05B8C7)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select File",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF05B8C7)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            when {
                                fileType.startsWith("image/") -> Icons.Default.Image
                                fileType == "application/pdf" -> Icons.Default.Description
                                else -> Icons.Default.AttachFile
                            },
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF05B8C7)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            "${fileSize / 1024} KB",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("E.g., Blood test results, X-ray report, prescription...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            // Upload button
            Button(
                onClick = {
                    if (selectedFileUri == null) {
                        Toast.makeText(context, "Please select a file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Validate file size (max 10MB)
                    if (fileSize > 10 * 1024 * 1024) {
                        Toast.makeText(context, "File size must be less than 10MB", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Validate file type
                    if (!fileType.startsWith("image/") && fileType != "application/pdf") {
                        Toast.makeText(context, "Only images and PDFs are supported", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isUploading = true
                    scope.launch {
                        val result = HealthRecordRepository.uploadRecord(
                            fileUri = selectedFileUri!!,
                            fileName = fileName,
                            fileType = fileType,
                            fileSize = fileSize,
                            description = description.trim(),
                            tags = listOf("patient_uploaded")
                        )

                        isUploading = false
                        
                        if (result.isSuccess) {
                            Toast.makeText(context, "Record uploaded successfully!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(
                                context,
                                "Upload failed: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isUploading && selectedFileUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF05B8C7)),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Uploading...", fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.CloudUpload, "Upload")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Record", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Cancel button
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    }
}
