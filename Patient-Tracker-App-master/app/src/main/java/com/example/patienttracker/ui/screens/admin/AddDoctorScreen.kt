package com.example.patienttracker.ui.screens.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin screen to add a new doctor to the system.
 * Admin provides doctor details and creates credentials.
 */
@Composable
fun AddDoctorScreen(navController: NavController, context: Context) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAF8F3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Add Doctor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8956A),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Register a new doctor in the system",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Form Fields
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = speciality,
                onValueChange = { speciality = it },
                label = { Text("Speciality") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Temporary Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    scope.launch {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                            phone.isBlank() || speciality.isBlank() || password.isBlank()
                        ) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password.length < 8) {
                            Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        isLoading = true
                        try {
                            // 1) Create Firebase Auth user
                            val auth = Firebase.auth
                            val user = auth.createUserWithEmailAndPassword(email.trim(), password)
                                .await().user ?: throw IllegalStateException("Auth failed")

                            // 2) Generate doctor humanId
                            val humanId = nextHumanId("doctor")

                            // 3) Create doctor profile in Firestore
                            createDoctorProfile(
                                uid = user.uid,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                humanId = humanId,
                                speciality = speciality.trim()
                            )

                            Toast.makeText(context, "Doctor added successfully! ID: $humanId", Toast.LENGTH_LONG).show()
                            navController.popBackStack()

                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Failed to add doctor", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8956A)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                Text(
                    if (isLoading) "Adding..." else "Add Doctor",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back Button
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        }
    }
}

// ============= Firestore Helpers =============

private suspend fun nextHumanId(role: String): String {
    val db = Firebase.firestore
    val ref = db.collection("counters").document(role)
    val next = db.runTransaction { tx ->
        val snap = tx.get(ref)
        val current = snap.getLong("next") ?: 1L
        tx.set(ref, mapOf("next" to (current + 1)), SetOptions.merge())
        current
    }.await()
    return next.toString().padStart(6, '0')
}

private suspend fun createDoctorProfile(
    uid: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    humanId: String,
    speciality: String
) {
    val db = Firebase.firestore
    val doc = mapOf(
        "role" to "doctor",
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "humanId" to humanId,
        "speciality" to speciality,
        "createdAt" to Timestamp.now()
    )
    db.collection("users").document(uid).set(doc).await()
}
