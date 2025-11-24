package com.example.patienttracker.ui.screens.patient

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val BackgroundColor = Color(0xFFF5F1ED)
private val HeaderColor = Color(0xFFD4AF8C)
private val CardWhite = Color(0xFFFFFFFF)
private val ButtonColor = Color(0xFFC9956E)
private val ErrorColor = Color(0xFFD32F2F)
private val TextColor = Color(0xFF333333)
private val HintColor = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var oldPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Old Password Field
            PasswordField(
                value = oldPassword,
                onValueChange = { 
                    oldPassword = it
                    oldPasswordError = null
                },
                label = "Old Password",
                error = oldPasswordError,
                isVisible = oldPasswordVisible,
                onVisibilityToggle = { oldPasswordVisible = !oldPasswordVisible }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // New Password Field
            PasswordField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    newPasswordError = null
                },
                label = "New Password",
                error = newPasswordError,
                isVisible = newPasswordVisible,
                onVisibilityToggle = { newPasswordVisible = !newPasswordVisible }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Confirm Password Field
            PasswordField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = "Confirm New Password",
                error = confirmPasswordError,
                isVisible = confirmPasswordVisible,
                onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Update Password Button
            Button(
                onClick = {
                    // Reset errors
                    oldPasswordError = null
                    newPasswordError = null
                    confirmPasswordError = null
                    
                    // Validation
                    var hasError = false
                    
                    if (oldPassword.isEmpty()) {
                        oldPasswordError = "Old password cannot be empty"
                        hasError = true
                    }
                    
                    if (newPassword.isEmpty()) {
                        newPasswordError = "New password cannot be empty"
                        hasError = true
                    } else if (newPassword.length < 8) {
                        newPasswordError = "Password must be at least 8 characters"
                        hasError = true
                    }
                    
                    if (confirmPassword.isEmpty()) {
                        confirmPasswordError = "Please confirm your new password"
                        hasError = true
                    } else if (newPassword != confirmPassword) {
                        confirmPasswordError = "Passwords do not match"
                        hasError = true
                    }
                    
                    if (!hasError && newPassword == oldPassword) {
                        newPasswordError = "New password must be different from old password"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        isLoading = true
                        scope.launch {
                            try {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null && user.email != null) {
                                    // Re-authenticate user with old password
                                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                                    
                                    try {
                                        user.reauthenticate(credential).await()
                                        
                                        // Old password is correct, update to new password
                                        user.updatePassword(newPassword).await()
                                        
                                        isLoading = false
                                        Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                        
                                    } catch (e: Exception) {
                                        isLoading = false
                                        // Re-authentication failed - old password is incorrect
                                        oldPasswordError = "Old password is incorrect"
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Could not update password. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Update Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password Requirements Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Password Requirements:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• At least 8 characters long\n• Different from your old password",
                        fontSize = 13.sp,
                        color = HintColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isVisible) "Hide password" else "Show password",
                        tint = HintColor
                    )
                }
            },
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ButtonColor,
                unfocusedBorderColor = HintColor.copy(alpha = 0.5f),
                errorBorderColor = ErrorColor,
                focusedLabelColor = ButtonColor,
                unfocusedLabelColor = HintColor,
                cursorColor = ButtonColor
            ),
            singleLine = true
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = ErrorColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
