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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.text.ClickableText
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
    var navigationTarget by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isUrduEnabled by remember { mutableStateOf(false) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Handle navigation after successful login
    LaunchedEffect(navigationTarget) {
        navigationTarget?.let { target ->
            navController.navigate(target) {
                popUpTo("login") { inclusive = true }
            }
            navigationTarget = null
        }
    }

    // Color scheme based on dark mode
    val backgroundColor = if (isDarkModeEnabled) Color(0xFF1C1B1F) else Color(0xFFDDD2CE)
    val surfaceColor = if (isDarkModeEnabled) Color(0xFF2B2930) else Color(0xFFF7ECE8)
    val textColor = if (isDarkModeEnabled) Color(0xFFE6E1E5) else Color(0xFF2F2019)
    val secondaryTextColor = if (isDarkModeEnabled) Color(0xFFCAC4D0) else Color(0xFF6B5B54)
    val buttonColor = if (isDarkModeEnabled) Color(0xFFD0BCFF) else Color(0xFF2F2019)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
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
                    text = if (isUrduEnabled) "سائن ان" else "Sign In",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
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
                        text = if (isUrduEnabled) "اکاؤنٹ نہیں ہے؟ " else "Do not have an account? ",
                        fontSize = 14.sp,
                        color = secondaryTextColor
                    )
                    Text(
                        text = if (isUrduEnabled) "سائن اپ" else "Sign Up",
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
                    label = { Text(if (isUrduEnabled) "ای میل" else "Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email icon",
                            tint = secondaryTextColor
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB36B3C),
                        unfocusedBorderColor = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFF9E8B82),
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Password field with leading icon and trailing toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isUrduEnabled) "پاس ورڈ" else "Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password icon",
                            tint = secondaryTextColor
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = secondaryTextColor
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFB36B3C),
                        unfocusedBorderColor = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFF9E8B82),
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Forgot Password link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onForgotPassword() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isUrduEnabled) "پاس ورڈ بھول گئے؟" else "Forgot Password?",
                            fontSize = 13.sp,
                            color = Color(0xFFA8653A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        if (isLoading) {
                            if (isUrduEnabled) "سائن ان ہو رہا ہے..." else "Signing In..."
                        } else {
                            if (isUrduEnabled) "سائن ان" else "Sign In"
                        },
                        color = if (isDarkModeEnabled) Color(0xFF381E72) else Color.White,
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
                        color = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFF9E8B82),
                        thickness = 1.dp
                    )
                    Text(
                        text = if (isUrduEnabled) " یا " else " or ",
                        fontSize = 14.sp,
                        color = secondaryTextColor
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFF9E8B82),
                        thickness = 1.dp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Google Sign In button
                OutlinedButton(
                    onClick = { /* TODO: Google sign-in */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = surfaceColor
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isUrduEnabled) "گوگل کے ساتھ سائن ان" else "Sign In with Google",
                        fontSize = 16.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Continue as Guest button
                OutlinedButton(
                    onClick = {
                        navController.navigate("guest_home") {
                            popUpTo("unified_login") { inclusive = false }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = textColor
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isUrduEnabled) "مہمان کے طور پر جاری رکھیں" else "Continue as Guest",
                        fontSize = 16.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Footer text with clickable links
                val annotatedText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 12.sp)) {
                        append(if (isUrduEnabled) "جاری رکھ کر، آپ ہماری " else "By continuing, you agree to our ")
                    }
                    pushStringAnnotation(tag = "privacy", annotation = "privacy_policy")
                    withStyle(style = SpanStyle(
                        color = Color(0xFFA8653A), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(if (isUrduEnabled) "رازداری کی پالیسی" else "Privacy Policy")
                    }
                    pop()
                    withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 12.sp)) {
                        append(if (isUrduEnabled) " اور " else " and ")
                    }
                    pushStringAnnotation(tag = "terms", annotation = "terms_and_conditions")
                    withStyle(style = SpanStyle(
                        color = Color(0xFFA8653A), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(if (isUrduEnabled) "شرائط و ضوابط" else "Terms & Conditions")
                    }
                    pop()
                    withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 12.sp)) {
                        append(if (isUrduEnabled) " سے اتفاق کرتے ہیں" else "")
                    }
                }

                ClickableText(
                    text = annotatedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController.navigate("privacy_policy")
                            }
                        annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController.navigate("terms_and_conditions")
                            }
                    }
                )

                Spacer(Modifier.height(40.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp),
                    color = buttonColor
                )
            }

            // Settings icon in top right (rendered on top)
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrduEnabled) "سیٹنگز" else "Settings",
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showSettingsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Urdu Translation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = Color(0xFFA8653A)
                            )
                            Column {
                                Text(
                                    text = if (isUrduEnabled) "اردو زبان" else "Urdu Language",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isUrduEnabled) "انگریزی سے اردو میں ترجمہ" else "Translate to Urdu",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B5B54)
                                )
                            }
                        }
                        Switch(
                            checked = isUrduEnabled,
                            onCheckedChange = { isUrduEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFA8653A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF9E8B82)
                            )
                        )
                    }

                    Divider(color = Color(0xFFE0E0E0))

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Mode",
                                tint = Color(0xFFA8653A)
                            )
                            Column {
                                Text(
                                    text = if (isUrduEnabled) "ڈارک موڈ" else "Dark Mode",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isUrduEnabled) "تاریک تھیم فعال کریں" else "Enable dark theme",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B5B54)
                                )
                            }
                        }
                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = { isDarkModeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFA8653A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF9E8B82)
                            )
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = if (isDarkModeEnabled) Color(0xFF2B2930) else Color.White
        )
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
