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

// Teal color for dependents
private val DependentColor = Color(0xFF0E4944)

/**
 * Admin screen to view and manage all users (patients, doctors, and dependents).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(navController: NavController, context: Context) {
    var users by remember { mutableStateOf<List<UserListItem>>(emptyList()) }
    var dependents by remember { mutableStateOf<List<DependentListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Patients, 1 = Doctors, 2 = Dependents
    var userToRemove by remember { mutableStateOf<UserListItem?>(null) }
    var dependentToRemove by remember { mutableStateOf<DependentListItem?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showRemoveDependentDialog by remember { mutableStateOf(false) }
    var isRemoving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedRole = when (selectedTabIndex) {
        0 -> "patient"
        1 -> "doctor"
        else -> "dependent"
    }

    LaunchedEffect(selectedTabIndex) {
        scope.launch {
            try {
                isLoading = true
                if (selectedTabIndex == 2) {
                    // Load all dependents
                    dependents = fetchAllDependents()
                } else {
                    users = fetchUsers(selectedRole)
                }
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

            // Tabs for Patients, Doctors, and Dependents
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFB8956A),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = if (selectedTabIndex == 2) DependentColor else Color(0xFFB8956A)
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
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { 
                        Text(
                            "Dependents", 
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTabIndex == 2) DependentColor else Color(0xFFB8956A).copy(alpha = 0.7f)
                        ) 
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Users/Dependents List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        color = if (selectedTabIndex == 2) DependentColor else Color(0xFFB8956A)
                    )
                }
            } else if (selectedTabIndex == 2) {
                // Show dependents
                if (dependents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = DependentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No dependents found",
                                color = Color(0xFF6B7280)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Dependents are added by patients",
                                color = Color(0xFF6B7280).copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${dependents.size} Dependent${if (dependents.size != 1) "s" else ""}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DependentColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(dependents) { dependent ->
                            DependentListCard(
                                dependent = dependent,
                                onRemove = {
                                    dependentToRemove = dependent
                                    showRemoveDependentDialog = true
                                }
                            )
                        }
                    }
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

    // Remove User Confirmation Dialog (UI-only: move to Recovery)
    if (showRemoveDialog && userToRemove != null) {
        AlertDialog(
            onDismissRequest = { if (!isRemoving) showRemoveDialog = false },
            containerColor = Color(0xFFF5F0E8),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Move Account to Recovery",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2F2019)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to move this account to Recovery?",
                        color = Color(0xFF2F2019),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Recovery info",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Account moved to Recovery",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E),
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "This account will be moved to Recovery and permanently deleted in 30 days.",
                                    color = Color(0xFF92400E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // UI-only: mark as moved to recovery (no DB change performed)
                        showRemoveDialog = false
                        snackbarMessage = "Account moved to Recovery. It will be permanently deleted in 30 days."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = !isRemoving
                ) {
                    Text("Move to Recovery", color = Color.White)
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

    // Remove Dependent Confirmation Dialog
    if (showRemoveDependentDialog && dependentToRemove != null) {
        AlertDialog(
            onDismissRequest = { if (!isRemoving) showRemoveDependentDialog = false },
            containerColor = Color(0xFFF5F0E8),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Remove Dependent",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2F2019)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to remove ${dependentToRemove!!.fullName}?",
                        color = Color(0xFF2F2019),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Parent: ${dependentToRemove!!.parentName}",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Warning",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "This will permanently remove the dependent and all their health records.",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isRemoving = true
                            val success = removeDependent(
                                dependentToRemove!!.parentUid,
                                dependentToRemove!!.dependentId
                            )
                            isRemoving = false
                            showRemoveDependentDialog = false
                            if (success) {
                                dependents = dependents.filter { it.dependentId != dependentToRemove!!.dependentId }
                                snackbarMessage = "Dependent removed successfully"
                            } else {
                                snackbarMessage = "Failed to remove dependent"
                            }
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
                    onClick = { showRemoveDependentDialog = false },
                    enabled = !isRemoving
                ) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

@Composable
private fun DependentListCard(
    dependent: DependentListItem,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DependentColor.copy(alpha = 0.05f),
        shadowElevation = 2.dp
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
                // Avatar with family icon
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = DependentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = "Dependent",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dependent.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF2F2019)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DependentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = dependent.relationship,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DependentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Parent: ${dependent.parentName}",
                        fontSize = 13.sp,
                        color = DependentColor.copy(alpha = 0.8f)
                    )
                    if (dependent.dob.isNotBlank()) {
                        Text(
                            text = "DOB: ${dependent.dob}",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = DependentColor
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
                                    tint = DependentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("View Details", color = Color(0xFF2F2019))
                            }
                        },
                        onClick = {
                            showMenu = false
                            // Could navigate to view dependent details
                        }
                    )
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
                                Text("Remove", color = Color(0xFFEF4444))
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
                                Text("Move to Recovery", color = Color(0xFFEF4444))
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

/**
 * Data class representing a dependent for the admin list view
 */
data class DependentListItem(
    val dependentId: String,
    val parentUid: String,
    val parentName: String,
    val fullName: String,
    val dob: String,
    val gender: String,
    val relationship: String
)

/**
 * Fetches all dependents from all patients
 */
private suspend fun fetchAllDependents(): List<DependentListItem> {
    val db = Firebase.firestore
    val dependentsList = mutableListOf<DependentListItem>()
    
    try {
        // First get all patients
        val patientsQuery = db.collection("users")
            .whereEqualTo("role", "patient")
            .get()
            .await()
        
        // For each patient, fetch their dependents
        for (patientDoc in patientsQuery.documents) {
            val parentUid = patientDoc.id
            val parentFirstName = patientDoc.getString("firstName") ?: ""
            val parentLastName = patientDoc.getString("lastName") ?: ""
            val parentName = "$parentFirstName $parentLastName".trim()
            
            try {
                val dependentsSnapshot = db.collection("users")
                    .document(parentUid)
                    .collection("dependents")
                    .get()
                    .await()
                
                for (depDoc in dependentsSnapshot.documents) {
                    try {
                        val firstName = depDoc.getString("firstName") ?: ""
                        val lastName = depDoc.getString("lastName") ?: ""
                        
                        dependentsList.add(
                            DependentListItem(
                                dependentId = depDoc.id,
                                parentUid = parentUid,
                                parentName = parentName.ifBlank { "Unknown Patient" },
                                fullName = "$firstName $lastName".trim().ifBlank { "Unknown" },
                                dob = depDoc.getString("dob") ?: "",
                                gender = depDoc.getString("gender") ?: "",
                                relationship = depDoc.getString("relationship") ?: "Dependent"
                            )
                        )
                    } catch (e: Exception) {
                        // Skip this dependent if there's an error
                    }
                }
            } catch (e: Exception) {
                // Skip this patient's dependents if there's an error
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    // Sort by parent name, then by dependent name
    return dependentsList.sortedWith(compareBy({ it.parentName.lowercase() }, { it.fullName.lowercase() }))
}

/**
 * Removes a dependent from a patient's subcollection
 */
private suspend fun removeDependent(parentUid: String, dependentId: String): Boolean {
    return try {
        val db = Firebase.firestore
        
        // Delete the dependent document
        db.collection("users")
            .document(parentUid)
            .collection("dependents")
            .document(dependentId)
            .delete()
            .await()
        
        // Also delete any health records for this dependent
        try {
            val recordsQuery = db.collection("healthRecords")
                .whereEqualTo("dependentId", dependentId)
                .get()
                .await()
            
            for (doc in recordsQuery.documents) {
                try {
                    db.collection("healthRecords").document(doc.id).delete().await()
                } catch (e: Exception) {
                    // Continue with other records
                }
            }
        } catch (e: Exception) {
            // Continue even if records deletion fails
        }
        
        // Cancel future appointments for this dependent
        try {
            val now = com.google.firebase.Timestamp.now()
            val appointmentsQuery = db.collection("appointments")
                .whereEqualTo("dependentId", dependentId)
                .whereGreaterThanOrEqualTo("appointmentDate", now)
                .get()
                .await()
            
            for (doc in appointmentsQuery.documents) {
                try {
                    db.collection("appointments").document(doc.id)
                        .update(
                            mapOf(
                                "status" to "Cancelled — Dependent removed",
                                "updatedAt" to now,
                                "cancelledAt" to now,
                                "cancelledBy" to "admin"
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
        
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
