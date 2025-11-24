package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)
private val SuccessGreen = Color(0xFF4CAF50)

@Composable
fun AppointmentSuccessScreen(
    navController: NavController,
    context: Context,
    appointmentNumber: String,
    doctorName: String,
    date: String,
    blockName: String,
    timeRange: String
) {
    var uploadedFiles by remember { mutableStateOf<List<UploadedFile>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var showUploadSection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // File picker for multiple files
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isUploading = true
                try {
                    uris.forEach { uri ->
                        val fileName = uri.lastPathSegment ?: "document"
                        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileSize = try {
                            context.contentResolver.openInputStream(uri)?.available() ?: 0
                        } catch (e: Exception) {
                            0
                        }
                        
                        // Validate file
                        if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                            Toast.makeText(context, "File too large: $fileName (max 10MB)", Toast.LENGTH_SHORT).show()
                            return@forEach
                        }
                        
                        val validMimeTypes = listOf("application/pdf", "image/jpeg", "image/png", "image/jpg")
                        if (!validMimeTypes.contains(mimeType)) {
                            Toast.makeText(context, "Unsupported file type: $fileName", Toast.LENGTH_SHORT).show()
                            return@forEach
                        }
                        
                        // Add to uploaded files list
                        uploadedFiles = uploadedFiles + UploadedFile(
                            name = fileName,
                            uri = uri.toString(),
                            mimeType = mimeType,
                            size = fileSize
                        )
                    }
                    Toast.makeText(context, "Files added successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("FileUpload", "Error processing files: ${e.message}", e)
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Success Icon
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = SuccessGreen.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Success Message
            Text(
                text = "Appointment Confirmed!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = StatTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your appointment has been successfully booked",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Appointment Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryItem(label = "Doctor", value = doctorName)
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    SummaryItem(label = "Date", value = date)
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    SummaryItem(label = "Block", value = blockName)
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    SummaryItem(label = "Time Range", value = timeRange)
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    
                    // Highlighted Appointment Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Appointment No.",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "#$appointmentNumber",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    
                    // Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consultation Fee",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Rs. 1,500",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Important Notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE3F2FD),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Note: This time is approximate. You will be called based on your appointment number.",
                        fontSize = 13.sp,
                        color = Color(0xFF0D47A1),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Upload Records Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Attach Medical Records",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatTextColor
                        )
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Upload",
                            tint = ButtonColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "Optionally upload lab reports, prescriptions, or medical documents",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    // Display uploaded files
                    if (uploadedFiles.isNotEmpty()) {
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                        
                        uploadedFiles.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name.take(30),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatTextColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${file.size / 1024}KB",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        uploadedFiles = uploadedFiles.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonColor.copy(alpha = 0.1f),
                            contentColor = ButtonColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ButtonColor
                            )
                        } else {
                            Text(
                                text = if (uploadedFiles.isEmpty()) "Choose Files" else "Add More Files",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Navigate to appointments list and clear success screen
                        navController.navigate("full_schedule") {
                            popUpTo(navController.currentBackStackEntry?.destination?.route ?: "appointment_success") { 
                                inclusive = true 
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "View Appointments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = {
                        // Pop back to dashboard (patient_home with params)
                        while (navController.currentBackStackEntry?.destination?.route?.contains("patient_home") == false) {
                            if (!navController.popBackStack()) {
                                // If we can't pop back, navigate to patient_home without params as fallback
                                navController.navigate("patient_home") {
                                    popUpTo(0) { inclusive = false }
                                }
                                break
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ButtonColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, ButtonColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Go to Dashboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            color = StatTextColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Data class for uploaded files
data class UploadedFile(
    val name: String,
    val uri: String,
    val mimeType: String,
    val size: Int
)

