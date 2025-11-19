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
import androidx.compose.material.icons.filled.Person
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterPatientScreen(navController: NavController, context: Context) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFDDD2CE) // Warm peach/beige background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(40.dp))

            // Title
            Text(
                text = "Sign Up",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F2019),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle with clickable "Sign In"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = Color(0xFF6B5B54)
                )
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA8653A),
                    modifier = Modifier.clickable {
                        navController.popBackStack()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Full Name field
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Name icon",
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

            // Last Name field
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Name icon",
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

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
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

            // Password field with toggle
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

            // Sign Up button
            Button(
                onClick = {
                    scope.launch {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                            password.isBlank()
                        ) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isUpperCase() }) {
                            Toast.makeText(
                                context,
                                "Password must be at least 8 characters, include a number and uppercase letter",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        isLoading = true
                        try {
                            val auth = Firebase.auth
                            val user = auth.createUserWithEmailAndPassword(email.trim(), password)
                                .await().user ?: throw IllegalStateException("Auth user is null")

                            val humanId = nextHumanId("patient")

                            createUserProfile(
                                uid = user.uid,
                                role = "patient",
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                humanId = humanId
                            )

                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()

                            navController.navigate("patient_home/${firstName.trim()}/${lastName.trim()}") {
                                popUpTo("register_patient") { inclusive = true }
                                popUpTo("login") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F2019)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (isLoading) "Creating..." else "Sign Up",
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

            // Google Sign Up button
            OutlinedButton(
                onClick = { /* TODO: Google sign-up */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF7ECE8)
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Sign Up with Google",
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

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = Color(0xFF2F2019)
                )
            }
        }
    }
}

// ------------------ Firestore helpers ------------------

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

private suspend fun createUserProfile(
    uid: String,
    role: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    humanId: String
) {
    val db = Firebase.firestore
    val doc = mapOf(
        "role" to role,
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "humanId" to humanId,
        "createdAt" to Timestamp.now()
    )
    db.collection("users").document(uid).set(doc).await()
}
