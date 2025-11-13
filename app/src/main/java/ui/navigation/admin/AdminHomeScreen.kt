package com.example.patienttracker.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun AdminHomeScreen(nav: NavController, ctx: Context) {
    var adminName by remember { mutableStateOf("Admin") }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                adminName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Admin"
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FEFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Admin Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF05B8C7)
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Welcome, $adminName",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Management Options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdminMenuCard(
                    title = "Add Doctor",
                    description = "Register a new doctor in the system",
                    icon = "👨‍⚕️",
                    onClick = {
                        nav.navigate("admin_add_doctor") {
                            launchSingleTop = true
                        }
                    }
                )

                AdminMenuCard(
                    title = "Add Patient",
                    description = "Register a new patient in the system",
                    icon = "👤",
                    onClick = {
                        nav.navigate("admin_add_patient") {
                            launchSingleTop = true
                        }
                    }
                )

                AdminMenuCard(
                    title = "Manage Users",
                    description = "View and manage all users",
                    icon = "👥",
                    onClick = {
                        nav.navigate("admin_manage_users") {
                            launchSingleTop = true
                        }
                    }
                )

                AdminMenuCard(
                    title = "System Reports",
                    description = "View system analytics and reports",
                    icon = "📊",
                    onClick = {
                        // Placeholder for reports
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Sign Out Button
            Button(
                onClick = {
                    Firebase.auth.signOut()
                    nav.navigate("login") {
                        popUpTo("admin_home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Sign Out", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AdminMenuCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2
                )
            }

            Text(
                text = ">",
                fontSize = 24.sp,
                color = Color(0xFF05B8C7),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}