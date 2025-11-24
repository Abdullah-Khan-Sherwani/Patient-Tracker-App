package com.example.patienttracker.ui.screens.doctor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    navController: NavController,
    context: Context
) {
    var doctorName by remember { mutableStateOf("Doctor") }
    var doctorEmail by remember { mutableStateOf("") }
    var doctorPhone by remember { mutableStateOf("") }
    var doctorSpecialization by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    val db = Firebase.firestore
                    val doc = db.collection("users").document(currentUser.uid).get().await()
                    
                    doctorName = "Dr. ${doc.getString("firstName") ?: ""} ${doc.getString("lastName") ?: ""}".trim()
                    doctorEmail = doc.getString("email") ?: currentUser.email ?: ""
                    doctorPhone = doc.getString("phone") ?: ""
                    doctorSpecialization = doc.getString("speciality") ?: doc.getString("specialization") ?: ""
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(32.dp),
                    color = AccentColor
                )
            } else {
                // Profile Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(AccentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = doctorName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = doctorName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    if (doctorSpecialization.isNotEmpty()) {
                        Text(
                            text = doctorSpecialization,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Profile Information
                Text(
                    text = "Personal Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(12.dp))

                ProfileInfoCard(icon = Icons.Default.Email, label = "Email", value = doctorEmail)
                Spacer(Modifier.height(12.dp))
                ProfileInfoCard(icon = Icons.Default.Phone, label = "Phone", value = doctorPhone.ifEmpty { "Not provided" })

                Spacer(Modifier.height(32.dp))

                // Account Options
                Text(
                    text = "Account Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(Modifier.height(12.dp))

                ProfileMenuItem(
                    icon = Icons.Default.DateRange,
                    title = "Manage Availability",
                    subtitle = "Set your working hours",
                    onClick = {
                        val doctorUid = Firebase.auth.currentUser?.uid ?: ""
                        navController.navigate("edit_availability/$doctorUid")
                    }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    onClick = { navController.navigate("doctor_change_password") }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "Terms & Conditions",
                    onClick = { navController.navigate("doctor_terms") }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    title = "Privacy Policy",
                    onClick = { navController.navigate("doctor_privacy") }
                )

                Spacer(Modifier.height(32.dp))

                // Logout Button
                var showLogoutDialog by remember { mutableStateOf(false) }
                
                Button(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                
                // Logout confirmation dialog
                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Confirm Logout") },
                        text = { Text("Are you sure you want to log out?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLogoutDialog = false
                                    Firebase.auth.signOut()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFEF4444)
                                )
                            ) {
                                Text("Logout")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
