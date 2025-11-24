package com.example.patienttracker.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin screen to edit doctor details (name, specialization, phone, email, etc).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditDoctorScreen(
    navController: NavController,
    context: Context,
    doctorId: String
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore

    // Load doctor details
    LaunchedEffect(doctorId) {
        scope.launch {
            try {
                val doc = db.collection("users").document(doctorId).get().await()
                firstName = doc.getString("firstName") ?: ""
                lastName = doc.getString("lastName") ?: ""
                specialization = doc.getString("speciality") ?: ""
                phone = doc.getString("phone") ?: ""
                email = doc.getString("email") ?: ""
                qualification = doc.getString("qualification") ?: ""
                experience = doc.getString("experience") ?: ""
            } catch (e: Exception) {
                errorMessage = "Failed to load doctor details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Save doctor details
    fun saveDoctorDetails() {
        if (firstName.isBlank() || lastName.isBlank()) {
            errorMessage = "Name fields cannot be empty"
            return
        }
        if (specialization.isBlank()) {
            errorMessage = "Specialization cannot be empty"
            return
        }
        if (phone.isBlank()) {
            errorMessage = "Phone number cannot be empty"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Invalid email address"
            return
        }

        isSaving = true
        scope.launch {
            try {
                db.collection("users").document(doctorId).update(
                    mapOf(
                        "firstName" to firstName.trim(),
                        "lastName" to lastName.trim(),
                        "speciality" to specialization.trim(),
                        "phone" to phone.trim(),
                        "email" to email.trim(),
                        "qualification" to qualification.trim(),
                        "experience" to experience.trim()
                    )
                ).await()
                
                successMessage = "Doctor details updated successfully"
                isSaving = false
                
                // Navigate back after success
                kotlinx.coroutines.delay(500)
                navController.popBackStack()
            } catch (e: Exception) {
                errorMessage = "Failed to save: ${e.message}"
                isSaving = false
            }
        }
    }

    val bgColor = Color(0xFFFAF8F3)
    val surfaceColor = Color(0xFFF5F0E8)
    val primaryColor = Color(0xFFB8956A)
    val textPrimary = Color(0xFF2F2019)
    val textSecondary = Color(0xFF6B7280)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Doctor Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Success message
                if (successMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = successMessage!!,
                                color = Color(0xFF10B981),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Personal Information Section
                Text(
                    text = "Personal Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Professional Information Section
                Text(
                    text = "Professional Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                // Specialization
                OutlinedTextField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = { Text("Specialization") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Qualification
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification (e.g., MBBS, MD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Experience
                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    label = { Text("Years of Experience") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Save Button
                Button(
                    onClick = { saveDoctorDetails() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                // Cancel Button
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text("Cancel", color = textSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
