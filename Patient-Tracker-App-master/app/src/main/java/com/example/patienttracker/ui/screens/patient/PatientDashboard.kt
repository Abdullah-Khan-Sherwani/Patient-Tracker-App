package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState

// ============================================================
// Deep Teal & Mint Design System - Light Mode
// WCAG Compliant - Professional Healthcare Theme
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)   // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val StatTextColor = Color(0xFFFFFFFF)       // Text on dark headers
private val CardTitleColor = Color(0xFF0E4944)      // Deep teal for card titles
private val CardSubtitleColor = Color(0xFF6B7280)   // Subtle gray
private val ButtonGreen = Color(0xFF76DCB0)         // Mint accent for CTAs
private val IconBgTeal = Color(0xFF0E4944)          // Deep teal for icon backgrounds
private val ButtonTextColor = Color(0xFF0E4944)    // Deep teal text on mint buttons

// Dark mode colors - Deep Teal Dark Theme
private val DarkBackgroundColor = Color(0xFF0B1F1D)   // Very dark teal
private val DarkCardColor = Color(0xFF112B28)         // Dark teal surface
private val DarkDividerColor = Color(0xFF2A4A46)      // Divider
private val DarkTextColor = Color(0xFFE8F5F3)         // Light text
private val DarkSecondaryTextColor = Color(0xFFA3C9C4) // Muted text
private val DarkIconTint = Color(0xFFE8F5F3)          // Light icon tint
private val DarkDisabledIcon = Color(0xFF4A6B66)      // Disabled icons
private val DarkAccentColor = Color(0xFF76DCB0)       // Mint accent
private val DarkBottomBarBg = Color(0xFF112B28)       // Bottom bar background
private val DarkBottomBarActive = Color(0xFF76DCB0)   // Active nav item
private val DarkBottomBarInactive = Color(0xFF6B8A85) // Inactive nav item

data class FeatureCardData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun PatientDashboard(navController: NavController, context: Context, isDarkMode: Boolean = false) {
    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Get user name from saved state or default
    val userName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("firstName")
        ?: navController.currentBackStackEntry?.arguments?.getString("firstName")
        ?: "Patient"
    
    val userLastName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("lastName")
        ?: navController.currentBackStackEntry?.arguments?.getString("lastName")
        ?: ""
    
    val fullName = if (userLastName.isNotEmpty()) "$userName $userLastName" else userName

    // Select colors based on dark mode
    val bgColor = if (isDarkMode) DarkBackgroundColor else BackgroundColor
    val headerTopCol = if (isDarkMode) DarkCardColor else HeaderTopColor
    val headerBotCol = if (isDarkMode) DarkBackgroundColor else HeaderBottomColor
    val cardCol = if (isDarkMode) DarkCardColor else CardWhite
    val textCol = if (isDarkMode) DarkTextColor else CardTitleColor
    val secondaryTextCol = if (isDarkMode) DarkSecondaryTextColor else CardSubtitleColor

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState
            )
        }
    ) {
        Scaffold(
            topBar = {
                DashboardTopAppBar(
                    navController = navController, 
                    userName = userName, 
                    userLastName = userLastName, 
                    isDarkMode = isDarkMode,
                    drawerState = drawerState
                )
            },
        bottomBar = {
            DashboardBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                navController = navController,
                userName = userName,
                userLastName = userLastName,
                isDarkMode = isDarkMode
            )
        },
            containerColor = bgColor,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp) // Extra padding to accommodate fixed AssistantBar
            ) {
                // Header with greeting, date, and appointment info
                HeaderWithStats(fullName = fullName, navController = navController, isDarkMode = isDarkMode)

                // Floating feature cards grid (with negative margin to overlap header)
                FloatingCardGrid(navController = navController, fullName = fullName, isDarkMode = isDarkMode)

                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Fixed Smart Assistant Bar - positioned at bottom of content area, above bottom nav
            AssistantBar(
                navController = navController,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp) // 8dp margin above the bottom nav
            )
        }
        }
    }
}

@Composable
fun DashboardTopAppBar(
    navController: NavController, 
    userName: String, 
    userLastName: String, 
    isDarkMode: Boolean = false,
    drawerState: DrawerState? = null
) {
    val scope = rememberCoroutineScope()
    val headerBgCol = if (isDarkMode) DarkBottomBarBg else HeaderTopColor
    val textIconCol = if (isDarkMode) DarkIconTint else Color.White
    var unreadCount by remember { mutableStateOf(0) }
    
    // Fetch unread notification count
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                currentUser?.uid?.let { uid ->
                    val notifications = com.example.patienttracker.data.NotificationRepository().getPatientNotifications(uid)
                    unreadCount = notifications.count { !it.isRead }
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardTopBar", "Error fetching notification count: ${e.message}")
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBgCol)
            .statusBarsPadding() // Respect status bar insets
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = headerBgCol,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
            IconButton(onClick = { 
                drawerState?.let { scope.launch { it.open() } }
            }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = textIconCol,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome,",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textIconCol.copy(alpha = 0.9f)
                )
                Text(
                    text = if (userLastName.isNotEmpty()) "$userName $userLastName" else userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textIconCol
                )
            }

            IconButton(
                onClick = { navController.navigate("patient_notifications") }
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isDarkMode) DarkCardColor else Color.White
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (isDarkMode) DarkIconTint else HeaderTopColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Unread badge
                    if (unreadCount > 0) {
                        Surface(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp),
                            shape = CircleShape,
                            color = Color(0xFFE91E63)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun AssistantBar(
    navController: NavController,
    isDarkMode: Boolean = false,
    hasUnreadMessages: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Colors
    val barColor = if (isDarkMode) Color(0xFF1A3D3A) else Color(0xFF4DB6AC)  // Lighter teal shade
    val textColor = Color.White
    val iconColor = Color.White
    val glowColor = if (isDarkMode) Color(0xFF76DCB0) else Color(0xFF00E5CC)
    val bgColor = if (isDarkMode) DarkBackgroundColor else BackgroundColor
    
    // Fade-in animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "fadeIn"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "slideUp"
    )
    
    // Pulsing glow animation for unread messages
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = offsetY.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Glow effect behind the bar (only when unread messages)
        if (hasUnreadMessages) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(26.dp),
                color = glowColor.copy(alpha = glowAlpha * 0.4f),
                shadowElevation = 0.dp
            ) {}
        }
        
        // Main assistant bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { navController.navigate("chatbot") }
                .then(
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    )
                ),
            shape = RoundedCornerShape(26.dp),
            color = barColor.copy(alpha = alpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left icon - Robot/Chatbot
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Assistant",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Center text
                Text(
                    text = "Ask Medify Assistant",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                
                // Right arrow icon
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Assistant",
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController,
    userName: String,
    userLastName: String,
    isDarkMode: Boolean = false
) {
    val navBarColor = if (isDarkMode) DarkBottomBarBg else CardWhite
    val activeColor = if (isDarkMode) DarkBottomBarActive else Color(0xFF00B8B8)
    val inactiveColor = if (isDarkMode) DarkBottomBarInactive else Color(0xFF00B8B8)
    val indicatorColor = if (isDarkMode) DarkBackgroundColor else BackgroundColor
    
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        containerColor = navBarColor,
        contentColor = inactiveColor,
        tonalElevation = 8.dp
    ) {
        val tabs = listOf(
            Triple("Home", Icons.Default.Home, { /* Stay on dashboard */ }),
            Triple("Favorites", Icons.Default.Favorite, { navController.navigate("favorite_doctors") }),
            Triple("Doctors", Icons.Default.LocalHospital, { navController.navigate("doctor_catalogue") }),
            Triple("Profile", Icons.Default.Person, { navController.navigate("patient_profile/$userName/$userLastName") })
        )

        tabs.forEachIndexed { index, (label, icon, action) ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                selected = selectedTab == index,
                onClick = {
                    onTabSelected(index)
                    if (index != 0) action()
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeColor,
                    selectedTextColor = activeColor,
                    indicatorColor = indicatorColor,
                    unselectedIconColor = inactiveColor,
                    unselectedTextColor = inactiveColor
                ),
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun HeaderWithStats(fullName: String, navController: NavController, isDarkMode: Boolean = false) {
    val headerTopCol = if (isDarkMode) DarkCardColor else HeaderTopColor
    val headerBotCol = if (isDarkMode) DarkBackgroundColor else HeaderBottomColor
    val statTextCol = if (isDarkMode) DarkTextColor else StatTextColor
    val cardCol = if (isDarkMode) DarkCardColor else CardWhite
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Gradient header background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(headerTopCol, headerBotCol)
                        )
                    )
            )
        }

        // Header content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            // Top row: Greeting + Bell
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hello, $fullName",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statTextCol,
                    modifier = Modifier.weight(1f)
                )

                // Notification bell button
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (isDarkMode) DarkBackgroundColor else CardWhite
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (isDarkMode) DarkIconTint else HeaderTopColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Upcoming Appointment heading with box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = statTextCol.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Upcoming Appointment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statTextCol
                    )
                }
            }

            // Next Appointment Info
            AppointmentInfoBlock(navController = navController, isDarkMode = isDarkMode)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AppointmentInfoBlock(navController: NavController, isDarkMode: Boolean = false) {
    val statTextCol = if (isDarkMode) DarkTextColor else StatTextColor
    var upcomingAppointment by remember { mutableStateOf<com.example.patienttracker.data.Appointment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Fetch upcoming appointments
    LaunchedEffect(Unit) {
        try {
            val result = com.example.patienttracker.data.AppointmentRepository.getPatientAppointments()
            val appointments = result.getOrElse { emptyList() }
            
            val today = java.time.LocalDate.now()
            val upcoming = appointments
                .filter { appointment ->
                    // Exclude cancelled appointments
                    val status = appointment.status ?: "pending"
                    if (status == "cancelled") return@filter false
                    
                    val appointmentDate = appointment.appointmentDate.toDate()
                        .toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    appointmentDate.isAfter(today) || appointmentDate.isEqual(today)
                }
                .sortedBy { it.appointmentDate }
                .firstOrNull()
            
            upcomingAppointment = upcoming
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { navController.navigate("full_schedule") }
            )
    ) {
        when {
            isLoading -> {
                Text(
                    text = "Loading...",
                    fontSize = 18.sp,
                    color = statTextCol.copy(alpha = 0.75f)
                )
            }
            upcomingAppointment != null -> {
                val appointment = upcomingAppointment!!
                val appointmentDate = appointment.appointmentDate.toDate()
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
                val formattedDate = appointmentDate.format(formatter)
                
                Text(
                    text = formattedDate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statTextCol
                )
                val displayTime = if (appointment.timeSlot.contains("-") || appointment.timeSlot.contains(":")) {
                    formatTimeRange(appointment.timeSlot)
                } else {
                    "${appointment.timeSlot} • ${getTimeRangeForBlock(appointment.timeSlot)}"
                }
                // Show dependent name if this is a dependent appointment
                val forWhom = if (appointment.recipientType == "dependent" && appointment.dependentName.isNotBlank()) {
                    " (for ${appointment.dependentName})"
                } else {
                    ""
                }
                Text(
                    text = "$displayTime • ${appointment.doctorName}, ${appointment.speciality}$forWhom",
                    fontSize = 18.sp,
                    color = statTextCol.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            else -> {
                Text(
                    text = "No Upcoming Appointments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statTextCol
                )
                Text(
                    text = "Book your next appointment to see it here",
                    fontSize = 15.sp,
                    color = statTextCol.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingCardGrid(navController: NavController, fullName: String = "Patient", isDarkMode: Boolean = false) {
    val cards = listOf(
        FeatureCardData(
            title = "Upload Records",
            subtitle = "Add medical files",
            icon = Icons.Default.CloudUpload,
            route = "upload_health_record_enhanced"
        ),
        FeatureCardData(
            title = "View Medical History",
            subtitle = "See your medical journey",
            icon = Icons.Default.Description,
            route = "patient_health_records"
        ),
        FeatureCardData(
            title = "My Appointments",
            subtitle = "Latest checkups",
            icon = Icons.Default.DateRange,
            route = "full_schedule"
        ),
        FeatureCardData(
            title = "Family Members",
            subtitle = "Add or view family",
            icon = Icons.Default.People,
            route = "patient_dependents"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-40).dp) // Negative margin to float over header
    ) {
        // First row of cards (2 columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingCard(
                cardData = cards[0],
                navController = navController,
                fullName = fullName,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            FloatingCard(
                cardData = cards[1],
                navController = navController,
                fullName = fullName,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Second row of cards (2 columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingCard(
                cardData = cards[2],
                navController = navController,
                fullName = fullName,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            FloatingCard(
                cardData = cards[3],
                navController = navController,
                fullName = fullName,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full-width Book Appointment CTA
        BookAppointmentCTA(
            navController = navController,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BookAppointmentCTA(navController: NavController, isDarkMode: Boolean = false) {
    val ctaColor = if (isDarkMode) DarkAccentColor else ButtonGreen
    val ctaTextColor = if (isDarkMode) DarkBackgroundColor else HeaderTopColor // Deep teal text on mint
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(80.dp)
            .clickable { navController.navigate("select_specialty") },
        color = ctaColor,
        shape = RoundedCornerShape(40.dp), // Pill shape as per design spec
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Book Appointment",
                tint = ctaTextColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Book an Appointment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ctaTextColor
                )
                Text(
                    text = "Schedule a visit instantly",
                    fontSize = 14.sp,
                    color = ctaTextColor.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to booking",
                tint = ctaTextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FloatingCard(
    cardData: FeatureCardData,
    navController: NavController,
    fullName: String = "Patient",
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardCol = if (isDarkMode) DarkCardColor else CardWhite
    val titleCol = if (isDarkMode) DarkTextColor else HeaderTopColor  // Deep teal titles
    val subtitleCol = if (isDarkMode) DarkSecondaryTextColor else CardSubtitleColor
    val iconTintCol = if (isDarkMode) DarkAccentColor else Color.White
    val iconBgCol = if (isDarkMode) DarkDividerColor else HeaderTopColor  // Deep teal icon bg

    Surface(
        modifier = modifier
            .then(
                if (!isDarkMode) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF0E4944).copy(alpha = 0.08f),  // Design spec shadow
                        spotColor = Color(0xFF0E4944).copy(alpha = 0.12f)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { navController.navigate(cardData.route) }
            ),
        color = cardCol,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (isDarkMode) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon in deep teal background
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp)),
                color = iconBgCol
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cardData.icon,
                        contentDescription = cardData.title,
                        modifier = Modifier.size(26.dp),
                        tint = iconTintCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card title - Deep teal, bold
            Text(
                text = cardData.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleCol,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Card subtitle
            Text(
                text = cardData.subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleCol,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
fun getCurrentDateFormatted(): String {
    val today = LocalDate.now()
    val dayOfWeek = today.format(DateTimeFormatter.ofPattern("EEEE"))
    val dayOfMonth = today.dayOfMonth
    val month = today.format(DateTimeFormatter.ofPattern("MMM"))
    val year = today.year
    return "$dayOfWeek, $dayOfMonth $month $year"
}

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "MEDIFY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeaderTopColor
                )
            }
            
            Divider(color = Color(0xFFE0E0E0))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Drawer Items
            DrawerMenuItem(
                icon = Icons.Default.Help,
                title = "Help Center",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("help_center")
                    }
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.QuestionAnswer,
                title = "FAQs",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("faqs")
                    }
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "About Medify",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("about_medify")
                    }
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.ContactSupport,
                title = "Contact Support",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate("contact_support")
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // App Version at bottom
            Divider(color = Color(0xFFE0E0E0))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Version 1.0.0",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HeaderTopColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                fontSize = 15.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getTimeRangeForBlock(blockName: String): String {
    return when (blockName) {
        "Morning" -> "6:00 AM - 12:00 PM"
        "Afternoon" -> "12:00 PM - 6:00 PM"
        "Evening" -> "6:00 PM - 10:00 PM"
        else -> ""
    }
}

private fun formatTimeRange(timeRange: String): String {
    return try {
        // Clean the input - remove + signs and extra spaces, handle AM/PM already present
        val cleaned = timeRange.replace("+", " ").replace("\\s+".toRegex(), " ").trim()
        
        // Check if it's already formatted with AM/PM
        if (cleaned.contains("AM", ignoreCase = true) || cleaned.contains("PM", ignoreCase = true)) {
            // Just ensure proper spacing
            return cleaned.replace("AM", " AM").replace("PM", " PM")
                .replace("am", " AM").replace("pm", " PM")
                .replace("\\s+".toRegex(), " ").trim()
        }
        
        // Parse as 24-hour format
        val parts = cleaned.split("-").map { it.trim() }
        if (parts.size == 2) {
            val startTime = java.time.LocalTime.parse(parts[0], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = java.time.LocalTime.parse(parts[1], java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
            "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        } else {
            cleaned
        }
    } catch (e: Exception) {
        timeRange
    }
}
