package com.example.patienttracker.ui.screens.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFDDD2CE) // Warm peach/beige background
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(40.dp))

                // Title
                Text(
                    text = "Sign In",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2F2019),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                // Subtitle with clickable "Sign Up"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Do not have an account? ",
                        fontSize = 14.sp,
                        color = Color(0xFF6B5B54)
                    )
                    Text(
                        text = "Sign Up",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA8653A),
                        modifier = Modifier.clickable { onSignUp() }
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Email field with leading icon
                OutlinedTextField(
                    value = idOrEmail,
                    onValueChange = { idOrEmail = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email icon",
                            tint = Color(0xFF6B5B54)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB36B3C),
                        unfocusedBorderColor = Color(0xFF9E8B82),
                        focusedContainerColor = Color(0xFFF7ECE8),
                        unfocusedContainerColor = Color(0xFFF7ECE8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Password field with leading icon and trailing toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password icon",
                            tint = Color(0xFF6B5B54)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFF6B5B54)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB36B3C),
                        unfocusedBorderColor = Color(0xFF9E8B82),
                        focusedContainerColor = Color(0xFFF7ECE8),
                        unfocusedContainerColor = Color(0xFFF7ECE8)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // Sign In button
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val emailToUse = if (idOrEmail.contains("@")) {
                                    idOrEmail.trim()
                                } else {
                                    val user = findUserByHumanId(idOrEmail.trim())
                                        ?: throw IllegalArgumentException("No user with this ID")
                                    user.email
                                }

                                val authUser = Firebase.auth
                                    .signInWithEmailAndPassword(emailToUse, password)
                                    .await()
                                    .user ?: throw IllegalStateException("Auth failed")

                                val profile = fetchUserProfile(authUser.uid)
                                    ?: throw IllegalStateException("Profile not found")

                                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                                navigationTarget = when (profile.role) {
                                    "patient" -> "patient_home/${profile.firstName}/${profile.lastName}"
                                    "doctor" -> "doctor_home/${profile.firstName}/${profile.lastName}/${profile.humanId}"
                                    "admin" -> "admin_home"
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F2019)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        if (isLoading) "Signing In..." else "Sign In",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Divider with "or"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF9E8B82),
                        thickness = 1.dp
                    )
                    Text(
                        text = " or ",
                        fontSize = 14.sp,
                        color = Color(0xFF6B5B54)
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF9E8B82),
                        thickness = 1.dp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Google Sign In button
                OutlinedButton(
                    onClick = { /* TODO: Google sign-in */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF7ECE8)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Sign In with Google",
                        fontSize = 16.sp,
                        color = Color(0xFF2F2019),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Footer text with links
                val annotatedText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF6B5B54), fontSize = 12.sp)) {
                        append("Before continuing, you agree to our ")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "privacy_policy")
                    withStyle(style = SpanStyle(color = Color(0xFFA8653A), fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append("Privacy Policy")
                    }
                    pop()
                    withStyle(style = SpanStyle(color = Color(0xFF6B5B54), fontSize = 12.sp)) {
                        append(" and ")
                    }
                    pushStringAnnotation(tag = "terms", annotation = "terms_of_service")
                    withStyle(style = SpanStyle(color = Color(0xFFA8653A), fontSize = 12.sp, fontWeight = FontWeight.Medium)) {
                        append("Terms of Service")
                    }
                    pop()
                }

                Text(
                    text = annotatedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(40.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = Color(0xFF2F2019)
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
