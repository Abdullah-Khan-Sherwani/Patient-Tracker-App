package com.example.patienttracker.ui.screens.auth

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import java.util.Calendar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.example.patienttracker.util.AppLanguage
import com.example.patienttracker.util.LanguagePreferenceManager
import com.example.patienttracker.util.LoginTranslations
import com.example.patienttracker.util.ValidationUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPatientScreen(navController: NavController, context: Context) {
    // Language preference manager
    val languageManager = remember { LanguagePreferenceManager(context) }
    var selectedLanguage by remember { mutableStateOf(languageManager.getLanguage()) }
    val strings = remember(selectedLanguage) { LoginTranslations.getStrings(selectedLanguage) }
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Email validation state
    val isEmailValid = ValidationUtils.isValidEmail(email)
    
    // Password validation requirements
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val isPasswordValid = hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar

    // Color scheme - using teal theme matching the app
    val tealPrimary = Color(0xFF0E4944)
    val tealLight = Color(0xFF16605A)
    val mintAccent = Color(0xFF76DCB0)
    val backgroundColor = if (isDarkModeEnabled) Color(0xFF1C1B1F) else Color(0xFFF0F5F4)
    val surfaceColor = if (isDarkModeEnabled) Color(0xFF2B2930) else Color.White
    val textColor = if (isDarkModeEnabled) Color(0xFFE6E1E5) else Color(0xFF1F2937)
    val secondaryTextColor = if (isDarkModeEnabled) Color(0xFFCAC4D0) else Color(0xFF6B7280)
    val buttonColor = tealPrimary

    // Date picker setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateOfBirth = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR) - 25,
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Save language when changed
    LaunchedEffect(selectedLanguage) {
        languageManager.saveLanguage(selectedLanguage)
    }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(80.dp))

                    // Title
                    Text(
                        text = strings.signUp,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Welcome subtitle
                    Text(
                        text = "Create your account",
                        fontSize = 16.sp,
                        color = tealPrimary,
                        fontWeight = FontWeight.Medium,
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
                            color = secondaryTextColor
                        )
                        Text(
                            text = strings.signIn,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mintAccent,
                            modifier = Modifier.clickable {
                                navController.popBackStack()
                            }
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    // First Name field
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        placeholder = { Text("Enter first name", color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name icon",
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

                    // Last Name field
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        placeholder = { Text("Enter last name", color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name icon",
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

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            if (emailError != null) {
                                emailError = null
                            }
                        },
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
                            focusedBorderColor = if (emailError != null) Color(0xFFD32F2F) else tealPrimary,
                            unfocusedBorderColor = if (emailError != null) Color(0xFFD32F2F) else if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = tealPrimary,
                            unfocusedLabelColor = secondaryTextColor,
                            errorBorderColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        supportingText = if (emailError != null) {
                            { Text(text = emailError!!, color = Color(0xFFD32F2F), fontSize = 12.sp) }
                        } else null
                    )

                    Spacer(Modifier.height(16.dp))

                    // Phone Number field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 15) {
                                phone = newValue
                            }
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("03001234567", color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone icon",
                                tint = tealPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                    // Date of Birth field
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date of Birth (DD/MM/YYYY)") },
                        placeholder = { Text("DD/MM/YYYY", color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date icon",
                                tint = tealPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Choose date",
                                    tint = mintAccent
                                )
                            }
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

                    // Password field
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
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
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

                    // Password requirements indicator
                    if (password.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColor
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Password must contain:",
                                    fontSize = 12.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                PasswordRequirement("At least 8 characters", hasMinLength, isDarkModeEnabled)
                                PasswordRequirement("One uppercase letter", hasUpperCase, isDarkModeEnabled)
                                PasswordRequirement("One lowercase letter", hasLowerCase, isDarkModeEnabled)
                                PasswordRequirement("One number", hasDigit, isDarkModeEnabled)
                                PasswordRequirement("One special character (!@#\$%^&*)", hasSpecialChar, isDarkModeEnabled)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Confirm Password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter password", color = secondaryTextColor.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password icon",
                                tint = tealPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = secondaryTextColor
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmPassword.isNotEmpty() && passwordsMatch) Color(0xFF4CAF50) else tealPrimary,
                            unfocusedBorderColor = if (isDarkModeEnabled) Color(0xFF938F99) else Color(0xFFD1D5DB),
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = tealPrimary,
                            unfocusedLabelColor = secondaryTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch
                    )

                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Passwords do not match",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Sign Up button
                    Button(
                        onClick = {
                            scope.launch {
                                if (firstName.isBlank() || lastName.isBlank() || email.isBlank() ||
                                    dateOfBirth.isBlank() || password.isBlank()
                                ) {
                                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                if (!ValidationUtils.isValidEmail(email)) {
                                    emailError = "Please enter a valid email address"
                                    return@launch
                                }
                                if (!dateOfBirth.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
                                    Toast.makeText(context, "Please enter date in DD/MM/YYYY format", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                if (!isPasswordValid) {
                                    Toast.makeText(
                                        context,
                                        "Password does not meet all requirements",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                if (!passwordsMatch) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
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
                                        humanId = humanId,
                                        dateOfBirth = dateOfBirth.trim()
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
                            if (isLoading) "Creating..." else strings.signUp,
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
                                Toast.makeText(context, "Google sign-up coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(64.dp)
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
                                Toast.makeText(context, "Facebook sign-up coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(64.dp)
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
                                Toast.makeText(context, "Email sign-up already available above", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(64.dp)
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
                                popUpTo("register_patient") { inclusive = false }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = tealPrimary
                        ),
                        border = BorderStroke(1.5.dp, tealPrimary),
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
                            append("By signing up, you agree to our ")
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
                                tint = tealPrimary
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
                                checkedTrackColor = tealPrimary,
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
    humanId: String,
    dateOfBirth: String
) {
    val db = Firebase.firestore
    val doc = mapOf(
        "role" to role,
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "humanId" to humanId,
        "dateOfBirth" to dateOfBirth,
        "createdAt" to Timestamp.now()
    )
    db.collection("users").document(uid).set(doc).await()
}

@Composable
private fun PasswordRequirement(text: String, isMet: Boolean, isDarkMode: Boolean) {
    val successColor = Color(0xFF4CAF50)
    val unmetColor = if (isDarkMode) Color(0xFFCAC4D0) else Color(0xFF6B7280)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = if (isMet) "✓" else "○",
            fontSize = 14.sp,
            color = if (isMet) successColor else unmetColor,
            fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isMet) successColor else unmetColor
        )
    }
}
