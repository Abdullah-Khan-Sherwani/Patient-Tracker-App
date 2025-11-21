package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

// Color scheme - light beige/beige gradient
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val CardTitleColor = Color(0xFF1A1A1A)
private val CardSubtitleColor = Color(0xFF757575)
private val ButtonGreen = Color(0xFFC9956E)
private val IconBgTeal = Color(0xFFE8D9CC)

// Dark mode colors
private val DarkHeaderTopColor = Color(0xFF2C2C2C)
private val DarkHeaderBottomColor = Color(0xFF1F1F1F)
private val DarkBackgroundColor = Color(0xFF121212)
private val DarkCardColor = Color(0xFF1E1E1E)
private val DarkTextColor = Color(0xFFE0E0E0)
private val DarkSecondaryTextColor = Color(0xFFB0B0B0)
private val DarkButtonColor = Color(0xFF8B7355)
private val DarkIconBgColor = Color(0xFF2C2C2C)

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
    val headerTopCol = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val headerBotCol = if (isDarkMode) DarkHeaderBottomColor else HeaderBottomColor
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // Header with greeting, date, and appointment info
                HeaderWithStats(fullName = fullName, navController = navController, isDarkMode = isDarkMode)

                // Floating feature cards grid (with negative margin to overlap header)
                FloatingCardGrid(navController = navController, fullName = fullName, isDarkMode = isDarkMode)

                Spacer(modifier = Modifier.height(32.dp))
            }
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
    val headerTopCol = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val cardCol = if (isDarkMode) DarkCardColor else CardWhite
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = headerTopCol,
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
                    tint = cardCol,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Dashboard",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = cardCol
            )

            IconButton(
                onClick = { /* Show notifications */ }
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = cardCol
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = headerTopCol,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
    val navBarColor = if (isDarkMode) DarkCardColor else CardWhite
    val headerTopCol = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val bgColor = if (isDarkMode) DarkBackgroundColor else BackgroundColor
    val statTextCol = if (isDarkMode) DarkTextColor else StatTextColor
    
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        containerColor = navBarColor,
        contentColor = statTextCol,
        tonalElevation = 8.dp
    ) {
        val tabs = listOf(
            Triple("Home", Icons.Default.Home, { /* Stay on dashboard */ }),
            Triple("Reports", Icons.Default.Assessment, { navController.navigate("patient_health_records") }),
            Triple("Appointments", Icons.Default.DateRange, { navController.navigate("full_schedule") }),
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
                    selectedIconColor = headerTopCol,
                    selectedTextColor = headerTopCol,
                    indicatorColor = bgColor,
                    unselectedIconColor = statTextCol.copy(alpha = 0.5f),
                    unselectedTextColor = statTextCol.copy(alpha = 0.5f)
                ),
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun HeaderWithStats(fullName: String, navController: NavController, isDarkMode: Boolean = false) {
    val headerTopCol = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val headerBotCol = if (isDarkMode) DarkHeaderBottomColor else HeaderBottomColor
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

                // White circular bell button
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = cardCol
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
                            tint = headerTopCol,
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
                Text(
                    text = "${appointment.timeSlot} • ${appointment.doctorName}, ${appointment.speciality}",
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
            title = "Recent Appointments",
            subtitle = "Latest checkups",
            icon = Icons.Default.DateRange,
            route = "full_schedule"
        ),
        FeatureCardData(
            title = "Book Appointment",
            subtitle = "Schedule a visit",
            icon = Icons.Default.AddCircle,
            route = "select_specialty"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-40).dp) // Negative margin to float over header
    ) {
        // First row of cards
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

        // Second row of cards
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

        Spacer(modifier = Modifier.height(40.dp))
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
    val titleCol = if (isDarkMode) DarkTextColor else CardTitleColor
    val subtitleCol = if (isDarkMode) DarkSecondaryTextColor else CardSubtitleColor
    val headerTopCol = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val iconBgCol = if (isDarkMode) DarkIconBgColor else IconBgTeal
    
    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { navController.navigate(cardData.route) }
            ),
        color = cardCol,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon in teal background
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
                        tint = headerTopCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card title
            Text(
                text = cardData.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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

