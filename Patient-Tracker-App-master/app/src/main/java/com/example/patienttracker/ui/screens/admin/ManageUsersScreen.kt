package com.example.patienttracker.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin screen to view and manage all users (patients, doctors, admins).
 */
@Composable
fun ManageUsersScreen(navController: NavController, context: Context) {
    var users by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf("all") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedRole) {
        scope.launch {
            try {
                users = fetchUsers(selectedRole)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAF8F3)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Manage Users",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8956A),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Role Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "patient", "doctor", "admin").forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Users List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator(color = Color(0xFFB8956A))
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No users found", color = Color(0xFF6B7280))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        UserListCard(user)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Back Button
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back", color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun UserListCard(user: UserListItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    color = getRoleColor(user.role)
                ) {
                    Text(
                        text = user.role.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "ID: ${user.humanId} | Phone: ${user.phone}",
                fontSize = 10.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

private fun getRoleColor(role: String): Color {
    return when (role) {
        "patient" -> Color(0xFF3B82F6)
        "doctor" -> Color(0xFF10B981)
        "admin" -> Color(0xFFF59E0B)
        else -> Color(0xFF6B7280)
    }
}

data class UserListItem(
    val uid: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val humanId: String
)

private suspend fun fetchUsers(roleFilter: String): List<UserListItem> {
    val db = Firebase.firestore
    val query = if (roleFilter == "all") {
        db.collection("users").get().await()
    } else {
        db.collection("users")
            .whereEqualTo("role", roleFilter)
            .get()
            .await()
    }

    return query.documents.mapNotNull { doc ->
        try {
            UserListItem(
                uid = doc.id,
                role = doc.getString("role") ?: "",
                firstName = doc.getString("firstName") ?: "",
                lastName = doc.getString("lastName") ?: "",
                email = doc.getString("email") ?: "",
                phone = doc.getString("phone") ?: "",
                humanId = doc.getString("humanId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
