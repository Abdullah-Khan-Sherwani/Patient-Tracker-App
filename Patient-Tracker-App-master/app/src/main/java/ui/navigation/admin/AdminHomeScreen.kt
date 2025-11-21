package com.example.patienttracker.ui.screens.admin

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
import com.example.patienttracker.data.AppointmentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Medify Beige Theme
private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(nav: NavController, ctx: Context) {
    var adminName by remember { mutableStateOf("Admin") }
    var totalDoctors by remember { mutableStateOf(0) }
    var totalPatients by remember { mutableStateOf(0) }
    var activeAppointments by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    adminName = currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "Admin"
                }
                
                val db = Firebase.firestore
                
                // Get total doctors
                val doctorsSnapshot = db.collection("users")
                    .whereEqualTo("role", "doctor")
                    .get()
                    .await()
                totalDoctors = doctorsSnapshot.size()
                
                // Get total patients
                val patientsSnapshot = db.collection("users")
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                totalPatients = patientsSnapshot.size()
                
                // Get today's appointments
                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val appointmentsResult = AppointmentRepository.getDoctorAppointmentsByDate(today)
                activeAppointments = appointmentsResult.getOrNull()?.size ?: 0
                
            } catch (e: Exception) {
                // Handle error silently - display zeros
            } finally {
                isLoading = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminNavigationDrawer(
                navController = nav,
                adminName = adminName,
                onItemClick = {
                    scope.launch { drawerState.close() }
                }
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                ModernAdminTopBar(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onProfileClick = {
                        // Navigate to admin profile
                    }
                )
            },
            containerColor = BgColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Greeting Section
                GreetingSection(adminName = adminName)
                
                Spacer(Modifier.height(24.dp))
                
                // Action Hub - 2x2 Grid
                Text(
                    text = "Admin Action Hub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                AdminActionGrid(navController = nav)
                
                Spacer(Modifier.height(32.dp))
                
                // System Analytics Glance
                Text(
                    text = "System Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                } else {
                    SystemMetrics(
                        totalDoctors = totalDoctors,
                        totalPatients = totalPatients,
                        activeAppointments = activeAppointments
                    )
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernAdminTopBar(
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Admin Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
    )
}

@Composable
private fun GreetingSection(adminName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Welcome, $adminName",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        Text(
            text = dateFormat.format(Date()),
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AdminActionGrid(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // First Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Person,
                title = "Add Doctor",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_add_doctor") {
                        launchSingleTop = true
                    }
                }
            )
            
            ActionCard(
                icon = Icons.Default.AccountBox,
                title = "Add Patient",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_add_patient") {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Search,
                title = "Manage Users",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_manage_users") {
                        launchSingleTop = true
                    }
                }
            )
            
            ActionCard(
                icon = Icons.Default.Info,
                title = "System Reports",
                modifier = Modifier.weight(1f),
                onClick = {
                    // Navigate to reports when implemented
                }
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardColor,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SystemMetrics(
    totalDoctors: Int,
    totalPatients: Int,
    activeAppointments: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            icon = Icons.Default.Person,
            label = "Total Doctors",
            value = totalDoctors.toString()
        )
        
        MetricCard(
            icon = Icons.Default.AccountBox,
            label = "Total Patients",
            value = totalPatients.toString()
        )
        
        MetricCard(
            icon = Icons.Default.DateRange,
            label = "Active Appointments Today",
            value = activeAppointments.toString()
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentColor
            )
        }
    }
}

@Composable
private fun AdminNavigationDrawer(
    navController: NavController,
    adminName: String,
    onItemClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = BgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp)
        ) {
            // Drawer Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = adminName.take(1).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = adminName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Text(
                    text = "Administrator",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextSecondary.copy(alpha = 0.2f)
            )
            
            // Menu Items
            DrawerMenuItem(
                icon = Icons.Default.Home,
                label = "Dashboard",
                onClick = {
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Person,
                label = "Add Doctor",
                onClick = {
                    navController.navigate("admin_add_doctor")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.AccountBox,
                label = "Add Patient",
                onClick = {
                    navController.navigate("admin_add_patient")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Search,
                label = "Manage Users",
                onClick = {
                    navController.navigate("admin_manage_users")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Info,
                label = "System Reports",
                onClick = {
                    onItemClick()
                }
            )
            
            Spacer(Modifier.weight(1f))
            
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = TextSecondary.copy(alpha = 0.2f)
            )
            
            // Sign Out
            DrawerMenuItem(
                icon = Icons.Default.ExitToApp,
                label = "Sign Out",
                onClick = {
                    Firebase.auth.signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                    onItemClick()
                },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFEF4444) else TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) Color(0xFFEF4444) else TextPrimary
            )
        }
    }
}