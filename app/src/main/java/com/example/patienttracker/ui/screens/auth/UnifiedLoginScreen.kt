package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Unified login screen for all roles (patient, doctor, admin).
 * Accepts humanId or email + password.
 * Queries Firestore to determine role and routes to appropriate home screen.
 */
@Composable
fun UnifiedLoginScreen(
    navController: NavController,
    context: Context,
    onSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    var idOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FEFF)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "HealthTrack",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF05B8C7)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Track your health, securely",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(48.dp))

                OutlinedTextField(
                    value = idOrEmail,
                    onValueChange = { idOrEmail = it },
                    label = { Text("ID or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                // 1) Resolve email from humanId or use email directly
                                val emailToUse = if (idOrEmail.contains("@")) {
                                    idOrEmail.trim()
                                } else {
                                    // Treat as humanId, look up in Firestore
                                    val user = findUserByHumanId(idOrEmail.trim())
                                        ?: throw IllegalArgumentException("No user with this ID")
                                    user.email
                                }

                                // 2) Firebase Auth sign-in
                                val authUser = Firebase.auth
                                    .signInWithEmailAndPassword(emailToUse, password)
                                    .await()
                                    .user ?: throw IllegalStateException("Auth failed")

                                // 3) Fetch full profile from Firestore
                                val profile = fetchUserProfile(authUser.uid)
                                    ?: throw IllegalStateException("Profile not found")

                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                // 4) Route based on role
                                when (profile.role) {
                                    "patient" -> {
                                        navController.navigate("patient_home/${profile.firstName}/${profile.lastName}") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    "doctor" -> {
                                        navController.navigate("doctor_home/${profile.firstName}/${profile.lastName}/${profile.humanId}") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    "admin" -> {
                                        navController.navigate("admin_dashboard/${profile.firstName}/${profile.lastName}") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    else -> throw IllegalStateException("Unknown role: ${profile.role}")
                                }

                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    e.message ?: "Invalid ID/email or password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && idOrEmail.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF05B8C7)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        if (isLoading) "Signing In..." else "Log In",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Forgot ID / Password?",
                    color = Color(0xFF0077B6),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onForgotPassword() },
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New patient? ", color = Color(0xFF6B7280), fontSize = 14.sp)
                    Text(
                        "Sign up",
                        color = Color(0xFF05B8C7),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onSignUp() }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color(0xFF05B8C7)
                )
            }
        }
    }
}

/* ------------ Firestore Helpers ------------ */

/**
 * Find user by humanId (generic across all roles)
 */
private suspend fun findUserByHumanId(humanId: String): AppUser? {
    val db = Firebase.firestore
    val snap = db.collection("users")
        .whereEqualTo("humanId", humanId)
        .limit(1)
        .get()
        .await()

    val d = snap.documents.firstOrNull() ?: return null
    return AppUser(
        uid = d.id,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}

/**
 * Fetch user profile by Firebase UID
 */
private suspend fun fetchUserProfile(uid: String): AppUser? {
    val db = Firebase.firestore
    val d = db.collection("users").document(uid).get().await()
    if (!d.exists()) return null
    return AppUser(
        uid = uid,
        role = d.getString("role") ?: "",
        firstName = d.getString("firstName") ?: "",
        lastName = d.getString("lastName") ?: "",
        email = d.getString("email") ?: "",
        humanId = d.getString("humanId") ?: ""
    )
}

/**
 * Data class for user profile (same as in UserRepository)
 */
data class AppUser(
    val uid: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val humanId: String
)
