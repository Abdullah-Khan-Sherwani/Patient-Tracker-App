package com.example.patienttracker.ui.screens.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Theme colors matching Admin Dashboard
private val BgColor = Color(0xFFF4F6F7)
private val CardColor = Color(0xFFFFFFFF)
private val AccentColor = Color(0xFF04786A)
private val AccentColorLight = Color(0xFF18BC86)
private val TextPrimary = Color(0xFF082026)
private val TextSecondary = Color(0xFF6B7280)
private val ErrorColor = Color(0xFFEF4444)
private val WarningColor = Color(0xFFF59E0B)

data class EmergencyAccessGrant(
    val id: String = "",
    val patientUid: String = "",
    val patientName: String = "",
    val doctorUid: String = "",
    val doctorName: String = "",
    val grantedBy: String = "",
    val grantedByName: String = "",
    val grantedAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val reason: String = "",
    val isActive: Boolean = true
)

data class UserInfo(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmergencyAccessScreen(nav: NavController, ctx: Context) {
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    
    // States
    var patients by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var doctors by remember { mutableStateOf<List<UserInfo>>(emptyList()) }
    var activeGrants by remember { mutableStateOf<List<EmergencyAccessGrant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Form states
    var selectedPatient by remember { mutableStateOf<UserInfo?>(null) }
    var selectedDoctor by remember { mutableStateOf<UserInfo?>(null) }
    var accessReason by remember { mutableStateOf("") }
    var showDuration by remember { mutableStateOf(false) }
    var durationHours by remember { mutableStateOf("24") }
    
    // Dropdown states
    var patientDropdownExpanded by remember { mutableStateOf(false) }
    var doctorDropdownExpanded by remember { mutableStateOf(false) }
    
    // Confirmation dialog
    var showRevokeDialog by remember { mutableStateOf(false) }
    var grantToRevoke by remember { mutableStateOf<EmergencyAccessGrant?>(null) }
    
    // Load data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Load patients
                val patientsSnapshot = db.collection("users")
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                patients = patientsSnapshot.documents.mapNotNull { doc ->
                    UserInfo(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        role = "patient"
                    )
                }.sortedBy { it.name }
                
                // Load doctors
                val doctorsSnapshot = db.collection("users")
                    .whereEqualTo("role", "doctor")
                    .get()
                    .await()
                doctors = doctorsSnapshot.documents.mapNotNull { doc ->
                    UserInfo(
                        uid = doc.id,
                        name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        role = "doctor"
                    )
                }.sortedBy { it.name }
                
                // Load active emergency access grants
                val grantsSnapshot = db.collection("emergency_access")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                activeGrants = grantsSnapshot.documents.mapNotNull { doc ->
                    try {
                        EmergencyAccessGrant(
                            id = doc.id,
                            patientUid = doc.getString("patientUid") ?: "",
                            patientName = doc.getString("patientName") ?: "",
                            doctorUid = doc.getString("doctorUid") ?: "",
                            doctorName = doc.getString("doctorName") ?: "",
                            grantedBy = doc.getString("grantedBy") ?: "",
                            grantedByName = doc.getString("grantedByName") ?: "",
                            grantedAt = doc.getTimestamp("grantedAt") ?: Timestamp.now(),
                            expiresAt = doc.getTimestamp("expiresAt"),
                            reason = doc.getString("reason") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.grantedAt.seconds }
                
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }
    
    fun grantEmergencyAccess() {
        if (selectedPatient == null || selectedDoctor == null) {
            Toast.makeText(ctx, "Please select both patient and doctor", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (accessReason.isBlank()) {
            Toast.makeText(ctx, "Please provide a reason for emergency access", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSubmitting = true
        scope.launch {
            try {
                val adminName = currentUser?.displayName ?: currentUser?.email?.split("@")?.get(0) ?: "Admin"
                
                val expiresAt = if (showDuration) {
                    val hours = durationHours.toIntOrNull() ?: 24
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.HOUR, hours)
                    Timestamp(calendar.time)
                } else null
                
                val grantData = hashMapOf(
                    "patientUid" to selectedPatient!!.uid,
                    "patientName" to selectedPatient!!.name,
                    "doctorUid" to selectedDoctor!!.uid,
                    "doctorName" to selectedDoctor!!.name,
                    "grantedBy" to (currentUser?.uid ?: ""),
                    "grantedByName" to adminName,
                    "grantedAt" to Timestamp.now(),
                    "expiresAt" to expiresAt,
                    "reason" to accessReason,
                    "isActive" to true
                )
                
                val docRef = db.collection("emergency_access").add(grantData).await()
                
                // Create index document for fast Firestore rule lookups
                val indexId = "${selectedPatient!!.uid}_${selectedDoctor!!.uid}"
                val indexData = hashMapOf(
                    "patientUid" to selectedPatient!!.uid,
                    "doctorUid" to selectedDoctor!!.uid,
                    "accessId" to docRef.id,
                    "isActive" to true,
                    "expiresAt" to expiresAt
                )
                db.collection("emergency_access_index").document(indexId).set(indexData).await()
                
                // Reload grants
                val grantsSnapshot = db.collection("emergency_access")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                activeGrants = grantsSnapshot.documents.mapNotNull { doc ->
                    try {
                        EmergencyAccessGrant(
                            id = doc.id,
                            patientUid = doc.getString("patientUid") ?: "",
                            patientName = doc.getString("patientName") ?: "",
                            doctorUid = doc.getString("doctorUid") ?: "",
                            doctorName = doc.getString("doctorName") ?: "",
                            grantedBy = doc.getString("grantedBy") ?: "",
                            grantedByName = doc.getString("grantedByName") ?: "",
                            grantedAt = doc.getTimestamp("grantedAt") ?: Timestamp.now(),
                            expiresAt = doc.getTimestamp("expiresAt"),
                            reason = doc.getString("reason") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.grantedAt.seconds }
                
                // Reset form
                selectedPatient = null
                selectedDoctor = null
                accessReason = ""
                showDuration = false
                durationHours = "24"
                
                Toast.makeText(ctx, "Emergency access granted successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error granting access: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmitting = false
            }
        }
    }
    
    fun revokeAccess(grant: EmergencyAccessGrant) {
        scope.launch {
            try {
                // Update the main grant document
                db.collection("emergency_access")
                    .document(grant.id)
                    .update(
                        mapOf(
                            "isActive" to false,
                            "revokedAt" to Timestamp.now(),
                            "revokedBy" to (currentUser?.uid ?: "")
                        )
                    )
                    .await()
                
                // Delete the index document to remove access
                val indexId = "${grant.patientUid}_${grant.doctorUid}"
                db.collection("emergency_access_index").document(indexId).delete().await()
                
                activeGrants = activeGrants.filter { it.id != grant.id }
                Toast.makeText(ctx, "Access revoked successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(ctx, "Error revoking access: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emergency Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Banner
                item {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = WarningColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Emergency access grants doctors permission to view patient health records without an appointment. Use only in emergencies.",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                
                // Grant Access Form
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CardColor,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Grant Emergency Access",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            // Patient Dropdown
                            ExposedDropdownMenuBox(
                                expanded = patientDropdownExpanded,
                                onExpandedChange = { patientDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedPatient?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Patient") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentColor,
                                        focusedLabelColor = AccentColor
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = patientDropdownExpanded,
                                    onDismissRequest = { patientDropdownExpanded = false }
                                ) {
                                    patients.forEach { patient ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(patient.name, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        patient.email,
                                                        fontSize = 12.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPatient = patient
                                                patientDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Doctor Dropdown
                            ExposedDropdownMenuBox(
                                expanded = doctorDropdownExpanded,
                                onExpandedChange = { doctorDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedDoctor?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Doctor") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentColor,
                                        focusedLabelColor = AccentColor
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = doctorDropdownExpanded,
                                    onDismissRequest = { doctorDropdownExpanded = false }
                                ) {
                                    doctors.forEach { doctor ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(doctor.name, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        doctor.email,
                                                        fontSize = 12.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedDoctor = doctor
                                                doctorDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // Reason
                            OutlinedTextField(
                                value = accessReason,
                                onValueChange = { accessReason = it },
                                label = { Text("Reason for Emergency Access") },
                                placeholder = { Text("e.g., Patient unconscious, urgent care needed") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentColor,
                                    focusedLabelColor = AccentColor
                                )
                            )
                            
                            // Duration Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Set Expiration",
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Switch(
                                    checked = showDuration,
                                    onCheckedChange = { showDuration = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentColor
                                    )
                                )
                            }
                            
                            if (showDuration) {
                                OutlinedTextField(
                                    value = durationHours,
                                    onValueChange = { durationHours = it.filter { c -> c.isDigit() } },
                                    label = { Text("Access Duration (hours)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentColor,
                                        focusedLabelColor = AccentColor
                                    )
                                )
                            }
                            
                            // Grant Button
                            Button(
                                onClick = { grantEmergencyAccess() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !isSubmitting && selectedPatient != null && selectedDoctor != null && accessReason.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Grant Emergency Access", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Active Grants Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Active Emergency Access Grants",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                if (activeGrants.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CardColor
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "No active emergency access grants",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(activeGrants) { grant ->
                        EmergencyAccessCard(
                            grant = grant,
                            onRevoke = {
                                grantToRevoke = grant
                                showRevokeDialog = true
                            }
                        )
                    }
                }
                
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
    
    // Revoke Confirmation Dialog
    if (showRevokeDialog && grantToRevoke != null) {
        AlertDialog(
            onDismissRequest = { 
                showRevokeDialog = false
                grantToRevoke = null
            },
            title = { Text("Revoke Access", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Are you sure you want to revoke ${grantToRevoke!!.doctorName}'s access to ${grantToRevoke!!.patientName}'s records?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        revokeAccess(grantToRevoke!!)
                        showRevokeDialog = false
                        grantToRevoke = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Revoke")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRevokeDialog = false
                    grantToRevoke = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmergencyAccessCard(
    grant: EmergencyAccessGrant,
    onRevoke: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentColorLight)
                    )
                    Text(
                        text = "Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentColorLight
                    )
                }
                
                IconButton(
                    onClick = onRevoke,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Revoke",
                        tint = ErrorColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Divider(color = TextSecondary.copy(alpha = 0.1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Patient",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = grant.patientName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Doctor",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = grant.doctorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
            
            Column {
                Text(
                    text = "Reason",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = grant.reason,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Granted At",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = dateFormat.format(grant.grantedAt.toDate()),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
                
                if (grant.expiresAt != null) {
                    Column {
                        Text(
                            text = "Expires At",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = dateFormat.format(grant.expiresAt.toDate()),
                            fontSize = 12.sp,
                            color = if (grant.expiresAt.toDate().before(Date())) ErrorColor else TextPrimary
                        )
                    }
                }
            }
            
            Text(
                text = "Granted by: ${grant.grantedByName}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
