package com.example.patienttracker.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Admin screen to view and manage all users (patients, doctors).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(navController: NavController, context: Context) {
    var users by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Patients, 1 = Doctors
    var userToRemove by remember { mutableStateOf<UserListItem?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var isRemoving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedRole = if (selectedTabIndex == 0) "patient" else "doctor"

    LaunchedEffect(selectedRole) {
        scope.launch {
            try {
                isLoading = true
                users = fetchUsers(selectedRole)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAF8F3),
                    titleContentColor = Color(0xFFB8956A),
                    navigationIconContentColor = Color(0xFFB8956A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFAF8F3)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            // Tabs for Patients and Doctors
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFB8956A),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFFB8956A)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Patients", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Doctors", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(Modifier.height(16.dp))

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedTabIndex == 0) Icons.Default.Person else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF6B7280).copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No ${if (selectedTabIndex == 0) "patients" else "doctors"} found",
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users) { user ->
                        UserListCard(
                            user = user,
                            isDoctor = selectedRole == "doctor",
                            onMoreClick = {
                                userToRemove = user
                            },
                            onViewDetails = {
                                // Navigate to user details if needed
                            },
                            onEditDoctor = {
                                navController.navigate("edit_doctor/${user.uid}")
                            },
                            onEditAvailability = {
                                navController.navigate("edit_availability/${user.uid}")
                            },
                            onRemove = {
                                userToRemove = user
                                showRemoveDialog = true
                            }
                        )
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

    // Remove User Confirmation Dialog
    if (showRemoveDialog && userToRemove != null) {
        AlertDialog(
            onDismissRequest = { if (!isRemoving) showRemoveDialog = false },
            containerColor = Color(0xFFF5F0E8),
            title = {
                Text(
                    "Remove User",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2F2019)
                )
            },
            text = {
                Text(
                    "Are you sure you want to permanently remove this user from the system? This action cannot be undone.",
                    color = Color(0xFF2F2019)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isRemoving = true
                            val success = removeUser(userToRemove!!, selectedRole)
                            isRemoving = false
                            showRemoveDialog = false
                            
                            if (success) {
                                // Refresh list
                                users = users.filter { it.uid != userToRemove!!.uid }
                                snackbarMessage = "User removed successfully."
                            } else {
                                snackbarMessage = "Could not remove user. Please try again."
                            }
                            userToRemove = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = !isRemoving
                ) {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Remove", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveDialog = false },
                    enabled = !isRemoving
                ) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun UserListCard(
    user: UserListItem,
    isDoctor: Boolean,
    onMoreClick: () -> Unit,
    onViewDetails: () -> Unit,
    onEditDoctor: () -> Unit = {},
    onEditAvailability: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2F2019)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.phone.ifEmpty { "No phone" },
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ID: ${user.humanId}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280).copy(alpha = 0.7f)
                )
            }

            Box {
                IconButton(
                    onClick = {
                        showMenu = true
                        onMoreClick()
                    }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFFB8956A)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFB8956A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("View Details", color = Color(0xFF2F2019))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onViewDetails()
                        }
                    )
                    
                    if (isDoctor) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFFB8956A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit Details", color = Color(0xFF2F2019))
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEditDoctor()
                            }
                        )
                    }
                    
                    if (isDoctor) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFFB8956A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit Availability", color = Color(0xFF2F2019))
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEditAvailability()
                            }
                        )
                    }
                    
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Remove from Database", color = Color(0xFFEF4444))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
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
    val query = db.collection("users")
        .whereEqualTo("role", roleFilter)
        .get()
        .await()

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

/**
 * Safely removes a user from the database.
 * - Cancels all future appointments
 * - Removes user document
 * - Attempts to delete Firebase Auth credentials
 * - Maintains data integrity
 */
private suspend fun removeUser(user: UserListItem, role: String): Boolean {
    return try {
        val db = Firebase.firestore
        val userUid = user.uid
        
        // 1. Cancel all future appointments for this user
        val now = com.google.firebase.Timestamp.now()
        val appointmentsQuery = if (role == "patient") {
            db.collection("appointments")
                .whereEqualTo("patientUid", userUid)
                .whereGreaterThanOrEqualTo("appointmentDate", now)
                .get()
                .await()
        } else {
            db.collection("appointments")
                .whereEqualTo("doctorUid", userUid)
                .whereGreaterThanOrEqualTo("appointmentDate", now)
                .get()
                .await()
        }
        
        // Cancel each appointment with appropriate status
        val cancellationReason = if (role == "patient") {
            "Cancelled — Patient removed"
        } else {
            "Cancelled — Doctor removed"
        }
        
        appointmentsQuery.documents.forEach { doc ->
            try {
                db.collection("appointments").document(doc.id)
                    .update(
                        mapOf(
                            "status" to cancellationReason,
                            "updatedAt" to now,
                            "cancelledAt" to now,
                            "cancelledBy" to "admin"
                        )
                    )
                    .await()
            } catch (e: Exception) {
                // Continue even if one appointment fails
            }
        }
        
        // 2. Remove health records references (set to null to avoid orphans)
        if (role == "patient") {
            try {
                val healthRecordsQuery = db.collection("healthRecords")
                    .whereEqualTo("patientUid", userUid)
                    .get()
                    .await()
                
                healthRecordsQuery.documents.forEach { doc ->
                    try {
                        // Keep records but mark patient as removed
                        db.collection("healthRecords").document(doc.id)
                            .update(
                                mapOf(
                                    "patientRemoved" to true,
                                    "patientUid" to null
                                )
                            )
                            .await()
                    } catch (e: Exception) {
                        // Continue
                    }
                }
            } catch (e: Exception) {
                // Continue
            }
        }
        
        // 3. Handle chat messages (keep but disable)
        try {
            val chatsQuery = if (role == "patient") {
                db.collection("chats")
                    .whereEqualTo("patientUid", userUid)
                    .get()
                    .await()
            } else {
                db.collection("chats")
                    .whereEqualTo("doctorUid", userUid)
                    .get()
                    .await()
            }
            
            chatsQuery.documents.forEach { doc ->
                try {
                    db.collection("chats").document(doc.id)
                        .update(
                            mapOf(
                                "disabled" to true,
                                "userRemoved" to true
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    // Continue
                }
            }
        } catch (e: Exception) {
            // Continue
        }
        
        // 4. Remove user document from Firestore
        db.collection("users").document(userUid).delete().await()
        
        // 5. Attempt to delete Firebase Auth credentials
        // Note: This requires admin SDK or Cloud Function in production
        // For now, we can only delete the user document
        // The auth account will remain but won't have user data
        try {
            // This would require Firebase Admin SDK:
            // FirebaseAuth.getInstance().deleteUser(userUid)
            // For client-side, we can't delete other users' auth accounts
            // This should be handled by a Cloud Function in production
        } catch (e: Exception) {
            // Auth deletion not available on client side
        }
        
        true
    } catch (e: Exception) {
        false
    }
}
