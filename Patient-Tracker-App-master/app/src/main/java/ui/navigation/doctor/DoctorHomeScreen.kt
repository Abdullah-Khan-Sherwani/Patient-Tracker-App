package com.example.patienttracker.ui.screens.doctor

import android.content.Context
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Appointment as FirebaseAppointment
import com.example.patienttracker.data.AppointmentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ========== THEME COLORS ==========
private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

// ========== PUBLIC ENTRY ==========
@Composable
fun DoctorHomeScreen(
    navController: NavController,
    context: Context,
    firstName: String? = null,
    lastName: String? = null,
    doctorId: String? = null
) {
    val resolvedFirst = firstName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: "Doctor"

    val resolvedLast = lastName
        ?: navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: ""

    val fullName = "Dr. $resolvedFirst $resolvedLast"
    val initials = buildString {
        if (resolvedFirst.isNotBlank()) append(resolvedFirst.first().uppercaseChar())
        if (resolvedLast.isNotBlank()) append(resolvedLast.first().uppercaseChar())
    }.ifBlank { "DR" }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DoctorNavigationDrawer(
                navController = navController,
                doctorName = fullName,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ModernDoctorTopBar(
                    doctorName = fullName,
                    initials = initials,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onProfileClick = { /* TODO: Navigate to profile */ }
                )
            },
            bottomBar = {
                ModernDoctorBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    navController = navController
                )
            },
            containerColor = BgColor
        ) { padding ->
            ModernDoctorDashboard(
                modifier = Modifier.padding(padding),
                navController = navController,
                doctorLastName = resolvedLast.ifBlank { "Doctor" }
            )
        }
    }
}

// ========== TOP BAR ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDoctorTopBar(
    doctorName: String,
    initials: String,
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = doctorName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AccentColor)
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentColor)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
    )
}

// ========== MAIN DASHBOARD ==========
@Composable
private fun ModernDoctorDashboard(
    modifier: Modifier = Modifier,
    navController: NavController,
    doctorLastName: String
) {
    var todayAppointments by remember { mutableStateOf<List<DoctorAppointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            todayAppointments = fetchTodayAppointments()
            isLoading = false
        }
    }

    val currentDate = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)
    val formattedDate = currentDate.format(dateFormatter)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // GREETING
        Text(
            text = "Welcome back, Dr. $doctorLastName",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = formattedDate,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        // SECTION 1: TODAY AT A GLANCE
        Text(
            text = "Today at a Glance",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DateRange,
                title = "Total Today",
                value = todayAppointments.size.toString(),
                onClick = { navController.navigate("doctor_appointments_full") }
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                title = "Completed",
                value = todayAppointments.filter { it.status == "completed" }.size.toString(),
                onClick = { navController.navigate("doctor_appointments_full") }
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Add,
                title = "Pending",
                value = todayAppointments.filter { it.status == "pending" }.size.toString(),
                onClick = { navController.navigate("doctor_appointments_full") }
            )
        }

        Spacer(Modifier.height(24.dp))

        // SECTION: MY SCHEDULE & STATUS
        DoctorScheduleStatusCard(navController = navController)

        Spacer(Modifier.height(28.dp))

        // SECTION 2: TODAY'S APPOINTMENTS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Appointments",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "View All",
                fontSize = 14.sp,
                color = AccentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { navController.navigate("doctor_appointments_full") }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp),
                color = AccentColor
            )
        } else if (todayAppointments.isEmpty()) {
            EmptyAppointmentCard()
        } else {
            todayAppointments.forEach { appointment ->
                AppointmentListCard(
                    appointment = appointment,
                    onClick = { 
                        // Navigate to appointment details with appointment ID if available
                        navController.navigate("doctor_appointment_details/${appointment.patientName}")
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        // SECTION 3: QUICK ACTIONS
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Person,
                title = "View Patients",
                onClick = { navController.navigate("doctor_patient_list") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DateRange,
                title = "Manage Schedule",
                onClick = { navController.navigate("doctor_manage_schedule") }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Folder,
                title = "View Records",
                onClick = { navController.navigate("doctor_view_records") }
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = { navController.navigate("doctor_settings") }
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ========== NAVIGATION DRAWER ==========
@Composable
private fun DoctorNavigationDrawer(
    navController: NavController,
    doctorName: String,
    onClose: () -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = BgColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Menu",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AccentColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Text(
                text = doctorName,
                fontSize = 16.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Divider(color = Color(0xFFD4C4B0), modifier = Modifier.padding(bottom = 16.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("My Profile") },
                selected = false,
                onClick = { 
                    onClose()
                    navController.navigate("doctor_profile")
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AccentColor,
                    unselectedTextColor = TextPrimary
                )
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                label = { Text("My Schedule") },
                selected = false,
                onClick = { 
                    onClose()
                    navController.navigate("doctor_manage_schedule")
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AccentColor,
                    unselectedTextColor = TextPrimary
                )
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Patient List") },
                selected = false,
                onClick = {
                    onClose()
                    navController.navigate("doctor_patient_list")
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AccentColor,
                    unselectedTextColor = TextPrimary
                )
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = { 
                    onClose()
                    navController.navigate("doctor_settings")
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AccentColor,
                    unselectedTextColor = TextPrimary
                )
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                label = { Text("View Patient Records") },
                selected = false,
                onClick = { 
                    onClose()
                    navController.navigate("doctor_view_records")
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = AccentColor,
                    unselectedTextColor = TextPrimary
                )
            )

            Spacer(Modifier.weight(1f))

            Divider(color = Color(0xFFD4C4B0), modifier = Modifier.padding(vertical = 16.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    Firebase.auth.signOut()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = Color(0xFFEF4444),
                    unselectedTextColor = Color(0xFFEF4444)
                )
            )
        }
    }
}

// ========== METRIC CARD (TODAY AT A GLANCE) ==========
@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ========== APPOINTMENT CARD ==========
@Composable
private fun AppointmentListCard(
    appointment: DoctorAppointment,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 1.dp
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
                    text = appointment.patientName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = appointment.time,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = appointment.type,
                    fontSize = 12.sp,
                    color = AccentColor
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (appointment.status) {
                    "confirmed" -> Color(0xFF10B981).copy(alpha = 0.1f)
                    "pending" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                }
            ) {
                Text(
                    text = appointment.status.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (appointment.status) {
                        "confirmed" -> Color(0xFF10B981)
                        "pending" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAppointmentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No appointments today",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ========== QUICK ACTION CARD ==========
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ========== BOTTOM NAVIGATION BAR ==========
@Composable
private fun ModernDoctorBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController
) {
    NavigationBar(
        containerColor = CardColor,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentColor,
                selectedTextColor = AccentColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = AccentColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Patients") },
            label = { Text("Patients", fontSize = 11.sp) },
            selected = selectedTab == 1,
            onClick = { 
                onTabSelected(1)
                navController.navigate("doctor_patient_list")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentColor,
                selectedTextColor = AccentColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = AccentColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Appointments") },
            label = { Text("Appointments", fontSize = 11.sp) },
            selected = selectedTab == 2,
            onClick = { 
                onTabSelected(3)
                navController.navigate("doctor_appointments_full")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentColor,
                selectedTextColor = AccentColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = AccentColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
            label = { Text("Profile", fontSize = 11.sp) },
            selected = selectedTab == 3,
            onClick = { 
                onTabSelected(3)
                navController.navigate("doctor_profile")
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentColor,
                selectedTextColor = AccentColor,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = AccentColor.copy(alpha = 0.1f)
            )
        )
    }
}

// ========== SCHEDULE & STATUS CARD ==========
@Composable
private fun DoctorScheduleStatusCard(navController: NavController) {
    var availability by remember { mutableStateOf<List<com.example.patienttracker.data.DoctorAvailability>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }

    // Update current time every minute
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = LocalDateTime.now()
            kotlinx.coroutines.delay(60000) // Update every minute
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val doctorUid = Firebase.auth.currentUser?.uid ?: return@launch
                val db = Firebase.firestore
                val snapshot = db.collection("doctor_availability")
                    .whereEqualTo("doctorUid", doctorUid)
                    .get()
                    .await()
                
                availability = snapshot.documents.mapNotNull { doc ->
                    com.example.patienttracker.data.DoctorAvailability.fromFirestore(doc)
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoading = false
            }
        }
    }

    val currentDayOfWeek = currentTime.value.dayOfWeek.value // 1=Monday, 7=Sunday
    val todayAvailability = availability.find { it.dayOfWeek == currentDayOfWeek && it.isActive }
    
    // Determine if doctor is currently available
    val isCurrentlyAvailable = todayAvailability?.let { avail ->
        val currentHour = currentTime.value.hour
        val currentMinute = currentTime.value.minute
        val currentMinutes = currentHour * 60 + currentMinute
        
        val startParts = avail.startTime.split(":")
        val startMinutes = (startParts[0].toIntOrNull() ?: 0) * 60 + (startParts[1].toIntOrNull() ?: 0)
        
        val endParts = avail.endTime.split(":")
        val endMinutes = (endParts[0].toIntOrNull() ?: 0) * 60 + (endParts[1].toIntOrNull() ?: 0)
        
        currentMinutes in startMinutes..endMinutes
    } ?: false

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardColor,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Schedule",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = com.example.patienttracker.data.DoctorAvailability.getDayName(currentDayOfWeek),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCurrentlyAvailable) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentlyAvailable) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (isCurrentlyAvailable) "Available" else "Off Duty",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentlyAvailable) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally),
                    color = AccentColor
                )
            } else if (todayAvailability != null) {
                // Today's timing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Today's Hours",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${todayAvailability.startTime} - ${todayAvailability.endTime}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))
                Divider(color = TextSecondary.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))

                // Current time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Current Time",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentlyAvailable) Color(0xFF10B981) else TextPrimary
                    )
                }
            } else {
                Text(
                    text = "No schedule set for today",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Manage Schedule Button
            Button(
                onClick = {
                    val doctorUid = Firebase.auth.currentUser?.uid ?: ""
                    navController.navigate("edit_availability/$doctorUid")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Manage Availability", fontSize = 14.sp)
            }
        }
    }
}

// ========== DATA MODELS ==========
data class DoctorAppointment(
    val patientName: String,
    val time: String,
    val type: String,
    val status: String
)

// ========== DATA FETCHING ==========
private suspend fun fetchTodayAppointments(): List<DoctorAppointment> {
    return try {
        val result = AppointmentRepository.getDoctorAppointments()
        val allAppointments: List<FirebaseAppointment> = result.getOrElse { emptyList() }
        val today = LocalDate.now()

        allAppointments.filter { appointment ->
            try {
                val appointmentDate = Instant.ofEpochSecond(appointment.appointmentDate.seconds)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                appointmentDate == today
            } catch (e: Exception) {
                false
            }
        }.map { appointment ->
            DoctorAppointment(
                patientName = appointment.patientName,
                time = appointment.timeSlot,
                type = appointment.speciality,
                status = appointment.status.lowercase()
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}