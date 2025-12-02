package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Dependent
import com.example.patienttracker.data.DependentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal text
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep teal accent
private val DeleteColor = Color(0xFFEF4444)         // Error red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDependentsScreen(navController: NavController, context: Context) {
    val scope = rememberCoroutineScope()
    var dependents by remember { mutableStateOf<List<Dependent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dependentToDelete by remember { mutableStateOf<Dependent?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    // Load dependents
    fun loadDependents() {
        scope.launch {
            isLoading = true
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                dependents = DependentRepository.getDependentsForParent(currentUser.uid)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadDependents()
    }

    // Delete confirmation dialog
    if (showDeleteDialog && dependentToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                dependentToDelete = null
            },
            title = {
                Text(
                    text = "Remove Dependent",
                    fontWeight = FontWeight.Bold,
                    color = StatTextColor
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${dependentToDelete?.getFullName()}?\n\nThis will also remove appointments and records for this dependent.",
                    color = StatTextColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser != null && dependentToDelete != null) {
                                val result = DependentRepository.removeDependent(
                                    currentUser.uid,
                                    dependentToDelete!!.dependentId
                                )
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Dependent removed successfully", Toast.LENGTH_SHORT).show()
                                    loadDependents()
                                } else {
                                    Toast.makeText(context, "Failed to remove dependent", Toast.LENGTH_SHORT).show()
                                }
                            }
                            isDeleting = false
                            showDeleteDialog = false
                            dependentToDelete = null
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteColor)
                ) {
                    if (isDeleting) {
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
                    onClick = { 
                        showDeleteDialog = false
                        dependentToDelete = null
                    }
                ) {
                    Text("Cancel", color = StatTextColor)
                }
            },
            containerColor = CardWhite
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Family Members",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage family members",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = ButtonColor
                        )
                    }
                    dependents.isEmpty() -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = AccentColor.copy(alpha = 0.2f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ButtonColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "You have no dependents added yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatTextColor,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Add children or family members to book appointments for them.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    else -> {
                        // Dependents list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(dependents) { dependent ->
                                DependentCard(
                                    dependent = dependent,
                                    onClick = {
                                        navController.navigate("view_dependent/${dependent.dependentId}")
                                    },
                                    onEdit = {
                                        navController.navigate("edit_dependent/${dependent.dependentId}")
                                    },
                                    onDelete = {
                                        dependentToDelete = dependent
                                        showDeleteDialog = true
                                    }
                                )
                            }
                            
                            // Bottom spacing for FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = { navController.navigate("add_dependent") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            containerColor = ButtonColor,
            contentColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Dependent",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DependentCard(
    dependent: Dependent,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val age = try {
        if (dependent.dob.isNotBlank()) {
            val parts = dependent.dob.split("-")
            val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val period = Period.between(date, LocalDate.now())
            if (period.years > 0) "${period.years} yrs"
            else if (period.months > 0) "${period.months} months"
            else "${period.days} days"
        } else "-"
    } catch (e: Exception) { "-" }

    // Gender icon color
    val genderColor = when (dependent.gender.lowercase()) {
        "male" -> Color(0xFF3B82F6)  // Blue
        "female" -> Color(0xFFEC4899) // Pink
        else -> ButtonColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AccentColor.copy(alpha = 0.8f),
                                ButtonColor.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dependent.firstName.take(1).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dependent.getFullName(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatTextColor
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Relationship tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = dependent.relationship,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Age tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ButtonColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = age,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Gender tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = genderColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = dependent.gender,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = genderColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Action buttons column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Edit button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEdit() },
                    shape = RoundedCornerShape(10.dp),
                    color = ButtonColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = AccentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentColor
                        )
                    }
                }
                
                // Delete button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onDelete() },
                    shape = RoundedCornerShape(10.dp),
                    color = DeleteColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = DeleteColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeleteColor
                        )
                    }
                }
            }
        }
    }
}
