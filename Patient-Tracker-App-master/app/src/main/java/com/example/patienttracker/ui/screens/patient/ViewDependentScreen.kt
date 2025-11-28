package com.example.patienttracker.ui.screens.patient

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFF1F2937)       // Dark charcoal
private val ButtonColor = Color(0xFF76DCB0)         // Mint accent
private val AccentColor = Color(0xFF0E4944)         // Deep Teal
private val IconBgColor = Color(0xFFE6F4F1)         // Light mint tint
private val IconTintColor = Color(0xFF0E4944)       // Deep Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewDependentScreen(
    navController: NavController,
    context: Context,
    dependentId: String
) {
    val scope = rememberCoroutineScope()
    var dependent by remember { mutableStateOf<Dependent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(dependentId) {
        scope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                val deps = DependentRepository.getDependentsForParent(currentUser.uid)
                dependent = deps.find { it.dependentId == dependentId }
            }
            isLoading = false
        }
    }

    val age = try {
        if (dependent?.dob?.isNotBlank() == true) {
            val parts = dependent!!.dob.split("-")
            val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val period = Period.between(date, LocalDate.now())
            if (period.years > 0) "${period.years} years"
            else if (period.months > 0) "${period.months} months"
            else "${period.days} days"
        } else "-"
    } catch (e: Exception) { "-" }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Remove Dependent?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${dependent?.getFullName() ?: "this dependent"}? This action cannot be undone.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val currentUser = Firebase.auth.currentUser
                            if (currentUser != null) {
                                val result = DependentRepository.removeDependent(currentUser.uid, dependentId)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Dependent removed", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Failed to remove", Toast.LENGTH_SHORT).show()
                                }
                            }
                            isDeleting = false
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Remove")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = StatTextColor)
                }
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dependent?.getFullName() ?: "Dependent Details",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // Edit and Delete buttons
                        Row {
                            IconButton(
                                onClick = { navController.navigate("edit_dependent/$dependentId") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonColor)
                }
            } else if (dependent == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dependent not found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Avatar
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        color = AccentColor.copy(alpha = 0.3f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dependent!!.firstName.take(1).uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = ButtonColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = dependent!!.getFullName(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatTextColor
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick info chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoChip(
                            icon = Icons.Default.People,
                            text = dependent!!.relationship
                        )
                        InfoChip(
                            icon = Icons.Default.Person,
                            text = dependent!!.gender
                        )
                        InfoChip(
                            icon = Icons.Default.Cake,
                            text = age
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Section Title
                    Text(
                        text = "Quick Actions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatTextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    // Action Cards
                    val dependentName = dependent!!.getFullName()
                    val encodedName = URLEncoder.encode(dependentName, StandardCharsets.UTF_8.toString())
                    
                    ActionCard(
                        title = "Upload Medical Reports",
                        subtitle = "Add health records and documents",
                        icon = Icons.Default.CloudUpload,
                        iconBgColor = Color(0xFFE3F2FD),
                        iconTint = Color(0xFF1976D2),
                        onClick = {
                            navController.navigate("dependent_upload_records/$dependentId/$encodedName")
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ActionCard(
                        title = "View Medical Records",
                        subtitle = "Browse uploaded health files",
                        icon = Icons.Default.Folder,
                        iconBgColor = Color(0xFFFFF3E0),
                        iconTint = Color(0xFFF57C00),
                        onClick = {
                            navController.navigate("dependent_view_records/$dependentId/$encodedName")
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ActionCard(
                        title = "Appointment History",
                        subtitle = "View past and upcoming appointments",
                        icon = Icons.Default.History,
                        iconBgColor = Color(0xFFE8F5E9),
                        iconTint = Color(0xFF388E3C),
                        onClick = {
                            navController.navigate("dependent_appointments/$dependentId/$encodedName")
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AccentColor.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = StatTextColor
            )
            Text(
                text = text,
                fontSize = 12.sp,
                color = StatTextColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconBgColor
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconTint
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StatTextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
    }
}
