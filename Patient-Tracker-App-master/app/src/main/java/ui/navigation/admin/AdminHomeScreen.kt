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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

// Medify Teal/Mint Theme (Matching Patient Dashboard)
private val BgColor = Color(0xFFF4F6F7)           // Light gray background
private val CardColor = Color(0xFFFFFFFF)          // White cards
private val CardColorAlt = Color(0xFFDFF7F0)       // Mint background for cards
private val AccentColor = Color(0xFF04786A)        // Teal accent
private val AccentColorLight = Color(0xFF18BC86)   // Lighter teal/green for CTAs
private val TextPrimary = Color(0xFF082026)        // Dark text
private val TextSecondary = Color(0xFF6B7280)      // Gray secondary text
private val HeaderGradientStart = Color(0xFF04645A) // Gradient start
private val HeaderGradientEnd = Color(0xFF0FB992)   // Gradient end

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
                
                // Get today's appointments using timestamp range
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = com.google.firebase.Timestamp(calendar.time)
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfDay = com.google.firebase.Timestamp(calendar.time)
                
                val appointmentsSnapshot = db.collection("appointments")
                    .whereGreaterThanOrEqualTo("appointmentDate", startOfDay)
                    .whereLessThanOrEqualTo("appointmentDate", endOfDay)
                    .get()
                    .await()
                activeAppointments = appointmentsSnapshot.size()
                
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
                PremiumAdminTopBar(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onProfileClick = {
                        nav.navigate("admin_profile") {
                            launchSingleTop = true
                        }
                    },
                    navController = nav
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
                // Summary Strip
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(24.dp))
                    }
                } else {
                    QuickSummaryStrip(
                        totalPatients = totalPatients,
                        totalDoctors = totalDoctors,
                        totalAppointments = activeAppointments,
                        navController = nav
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Action Hub Section
                Text(
                    text = "Quick Actions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                PremiumActionGrid(navController = nav)
                
                Spacer(Modifier.height(24.dp))
                
                // Footer Banner
                FooterBanner()
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernAdminTopBar(
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    navController: NavController
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
                icon = Icons.Default.Add,
                title = "Create Appointment",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_create_appointment") {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Third Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Info,
                title = "System Reports",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_system_reports") {
                        launchSingleTop = true
                    }
                }
            )
            
            ActionCard(
                icon = Icons.Default.DateRange,
                title = "All Appointments",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("admin_all_appointments") {
                        launchSingleTop = true
                    }
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
                icon = Icons.Default.Add,
                label = "Create Appointment",
                onClick = {
                    navController.navigate("admin_create_appointment")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.DateRange,
                label = "All Appointments",
                onClick = {
                    navController.navigate("admin_all_appointments")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Info,
                label = "System Reports",
                onClick = {
                    navController.navigate("admin_system_reports")
                    onItemClick()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Security,
                label = "Emergency Access",
                onClick = {
                    navController.navigate("admin_emergency_access")
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

// ===== NEW PREMIUM COMPONENTS =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumAdminTopBar(
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    navController: NavController
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardColor,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Icon
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Admin Dashboard Title - Centered
                Text(
                    text = "Admin Dashboard",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )

                // Profile Icon
                IconButton(onClick = onProfileClick) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSummaryStrip(
    totalPatients: Int,
    totalDoctors: Int,
    totalAppointments: Int,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryPill(
            label = "Patients",
            count = totalPatients,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("admin_manage_users") }
        )
        SummaryPill(
            label = "Doctors",
            count = totalDoctors,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("admin_manage_users") }
        )
        SummaryPill(
            label = "Today's Appointments",
            count = totalAppointments,
            modifier = Modifier.weight(1f),
            onClick = { navController.navigate("admin_all_appointments") }
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AccentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PremiumActionGrid(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumActionCard(
                title = "Create Appointment",
                subtitle = "Schedule new patient visits",
                icon = Icons.Default.CalendarToday,
                onClick = { navController.navigate("admin_create_appointment") },
                modifier = Modifier.weight(1f)
            )
            PremiumActionCard(
                title = "Add Doctor",
                subtitle = "Register healthcare providers",
                icon = Icons.Default.PersonAdd,
                onClick = { navController.navigate("admin_add_doctor") },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PremiumActionCard(
                title = "Add Patient",
                subtitle = "Enroll new patients",
                icon = Icons.Default.PersonAdd,
                onClick = { navController.navigate("admin_add_patient") },
                modifier = Modifier.weight(1f)
            )
            PremiumActionCard(
                title = "Manage Users",
                subtitle = "View and edit user records",
                icon = Icons.Default.People,
                onClick = { navController.navigate("admin_manage_users") },
                modifier = Modifier.weight(1f)
            )
        }

        // Full Width Cards
        PremiumFullWidthActionCard(
            title = "System Reports",
            subtitle = "Analytics and performance insights",
            icon = Icons.Default.BarChart,
            onClick = { navController.navigate("admin_system_reports") }
        )
        
        // Emergency Access Card
        PremiumFullWidthActionCard(
            title = "Emergency Access",
            subtitle = "Grant doctors access to patient records without appointments",
            icon = Icons.Default.Security,
            onClick = { navController.navigate("admin_emergency_access") }
        )
    }
}

@Composable
private fun PremiumActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PremiumFullWidthActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CardColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FooterBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = CardColor,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Manage the entire healthcare platform seamlessly from your dashboard.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}