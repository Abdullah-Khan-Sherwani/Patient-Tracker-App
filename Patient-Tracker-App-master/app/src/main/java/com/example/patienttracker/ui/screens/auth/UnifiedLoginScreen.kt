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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.text.ClickableText
import com.example.patienttracker.R
import com.example.patienttracker.util.AppLanguage
import com.example.patienttracker.util.LanguagePreferenceManager
import com.example.patienttracker.util.LoginTranslations
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Unified login screen for all roles (patient, doctor, admin).
 * Accepts humanId or email + password.
 * Queries Firestore to determine role and routes to appropriate home screen.
 * Supports multilingual UI for Pakistani languages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedLoginScreen(
    navController: NavController,
    context: Context,
    onSignUp: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    // Language preference manager
    val languageManager = remember { LanguagePreferenceManager(context) }
    var selectedLanguage by remember { mutableStateOf(languageManager.getLanguage()) }
    val strings = remember(selectedLanguage) { LoginTranslations.getStrings(selectedLanguage) }
    
    var idOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var navigationTarget by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
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
    
    // Save language when changed
    LaunchedEffect(selectedLanguage) {
        languageManager.saveLanguage(selectedLanguage)
    }

    // Color scheme - using teal theme matching the app
    val tealPrimary = Color(0xFF0E4944)
    val tealLight = Color(0xFF16605A)
    val mintAccent = Color(0xFF76DCB0)
    val backgroundColor = if (isDarkModeEnabled) Color(0xFF1C1B1F) else Color(0xFFF0F5F4)
    val surfaceColor = if (isDarkModeEnabled) Color(0xFF2B2930) else Color.White
    val textColor = if (isDarkModeEnabled) Color(0xFFE6E1E5) else Color(0xFF1F2937)
    val secondaryTextColor = if (isDarkModeEnabled) Color(0xFFCAC4D0) else Color(0xFF6B7280)
    val buttonColor = tealPrimary

    // Apply RTL layout direction for RTL languages
    CompositionLocalProvider(
        LocalLayoutDirection provides selectedLanguage.layoutDirection
    ) {
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
                    Spacer(Modifier.height(80.dp)) // Space for language dropdown

                    // Title
                    Text(
                        text = strings.signIn,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))
                    
                    // Welcome subtitle
                    Text(
                        text = strings.welcomeBack,
                        fontSize = 16.sp,
                        color = tealPrimary,
                        fontWeight = FontWeight.Medium,
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
                            text = strings.dontHaveAccount + " ",
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                        Text(
                            text = strings.signUp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mintAccent,
                            modifier = Modifier.clickable { onSignUp() }
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    // Email field with leading icon
                    OutlinedTextField(
                        value = idOrEmail,
                        onValueChange = { idOrEmail = it },
                        label = { Text(strings.emailAddress) },
                        placeholder = { Text(strings.enterEmail, color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email icon",
                                tint = tealPrimary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tealPrimary,
                            unfocusedBorderColor = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = tealPrimary,
                            unfocusedLabelColor = secondaryTextColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password field with leading icon and trailing toggle
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(strings.password) },
                        placeholder = { Text(strings.enterPassword, color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password icon",
                                tint = tealPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) strings.passwordVisible else strings.passwordHidden,
                                    tint = secondaryTextColor
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tealPrimary,
                            unfocusedBorderColor = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = tealPrimary,
                            unfocusedLabelColor = secondaryTextColor
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
                                text = strings.forgotPassword,
                                fontSize = 13.sp,
                                color = tealLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

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
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isLoading) strings.signingIn else strings.signInButton,
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
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            thickness = 1.dp
                        )
                        Text(
                            text = " ${strings.orContinueWith} ",
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            thickness = 1.dp
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Social login icons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google icon button
                        IconButton(
                            onClick = { 
                                Toast.makeText(context, "Google sign-in coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(64.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = surfaceColor,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_google),
                                        contentDescription = strings.google,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Facebook icon button
                        IconButton(
                            onClick = { 
                                Toast.makeText(context, "Facebook sign-in coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(64.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = surfaceColor,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_facebook),
                                        contentDescription = "Facebook",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Email icon button
                        IconButton(
                            onClick = { 
                                Toast.makeText(context, "Email sign-in already available above", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(64.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = surfaceColor,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = tealPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
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
                            contentColor = tealPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, tealPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = strings.continueAsGuest,
                            fontSize = 16.sp,
                            color = tealPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // Footer text with clickable links
                    val annotatedText = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 12.sp)) {
                            append("By continuing, you agree to our ")
                        }
                        pushStringAnnotation(tag = "privacy", annotation = "privacy_policy")
                        withStyle(style = SpanStyle(
                            color = tealPrimary, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("Privacy Policy")
                        }
                        pop()
                        withStyle(style = SpanStyle(color = secondaryTextColor, fontSize = 12.sp)) {
                            append(" and ")
                        }
                        pushStringAnnotation(tag = "terms", annotation = "terms_and_conditions")
                        withStyle(style = SpanStyle(
                            color = tealPrimary, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("Terms & Conditions")
                        }
                        pop()
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

                // Language Selector Dropdown in top-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    // Language selector button
                    Surface(
                        onClick = { showLanguageDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceColor,
                        shadowElevation = 4.dp,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🌐",
                                fontSize = 18.sp
                            )
                            Text(
                                text = selectedLanguage.nativeName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select language",
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false },
                        modifier = Modifier
                            .wrapContentWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = language.nativeName,
                                                fontWeight = if (language == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                                                color = if (language == selectedLanguage) tealPrimary else textColor,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = language.displayName,
                                                fontSize = 12.sp,
                                                color = secondaryTextColor
                                            )
                                        }
                                        if (language == selectedLanguage) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = "Selected",
                                                tint = tealPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedLanguage = language
                                    showLanguageDropdown = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (language != AppLanguage.entries.last()) {
                                HorizontalDivider(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Settings icon in top left
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopStart)
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
                        text = "Settings",
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
                                tint = Color(0xFF0E4944)
                            )
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Enable dark theme",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = { isDarkModeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0E4944),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF9CA3AF)
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
