package com.example.patienttracker.ui.screens.guest

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Light theme colors
private val LightBgColor = Color(0xFFFAF8F3)
private val LightCardColor = Color(0xFFF5F0E8)
private val LightAccentColor = Color(0xFFB8956A)
private val LightTextPrimary = Color(0xFF2F2019)
private val LightTextSecondary = Color(0xFF6B7280)

// Dark theme colors
private val DarkBgColor = Color(0xFF1A1A1A)
private val DarkCardColor = Color(0xFF2D2D2D)
private val DarkAccentColor = Color(0xFFD4AF8C)
private val DarkTextPrimary = Color(0xFFE5E5E5)
private val DarkTextSecondary = Color(0xFFB0B0B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestHomeScreen(navController: NavController, context: Context) {
    val preferences = remember { GuestPreferences(context) }
    var isDarkMode by remember { mutableStateOf(preferences.isDarkMode) }
    var isUrdu by remember { mutableStateOf(preferences.isUrdu) }
    
    // Update theme when returning from settings
    LaunchedEffect(Unit) {
        isDarkMode = preferences.isDarkMode
        isUrdu = preferences.isUrdu
    }
    
    val bgColor = if (isDarkMode) DarkBgColor else LightBgColor
    val cardColor = if (isDarkMode) DarkCardColor else LightCardColor
    val accentColor = if (isDarkMode) DarkAccentColor else LightAccentColor
    val textPrimary = if (isDarkMode) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkMode) DarkTextSecondary else LightTextSecondary
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GuestDrawerContent(
                navController = navController,
                onItemClick = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                cardColor = cardColor,
                accentColor = accentColor,
                textPrimary = textPrimary
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (isUrdu) "خوش آمدید" else "Welcome",
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = textPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("guest_settings")
                        }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = accentColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
                )
            },
            containerColor = bgColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Welcome Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardColor,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Browsing as Guest",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Browse doctors and learn more about our platform",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Main Action Card
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(Modifier.height(12.dp))

                GuestActionCard(
                    icon = Icons.Default.Person,
                    title = "View Doctors",
                    subtitle = "Browse our healthcare professionals",
                    onClick = {
                        navController.navigate("guest_doctors")
                    },
                    cardColor = cardColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                )

                Spacer(Modifier.height(16.dp))

                GuestActionCard(
                    icon = Icons.Default.Info,
                    title = "About Medify",
                    subtitle = "Learn more about our services",
                    onClick = {
                        navController.navigate("guest_about")
                    },
                    cardColor = cardColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accentColor = accentColor
                )

                Spacer(Modifier.height(24.dp))

                // CTA Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Want to book appointments?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Create an account to book appointments instantly",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                navController.navigate("register_patient") {
                                    popUpTo("guest_home") { inclusive = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Create Account",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun GuestActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    cardColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun GuestDrawerContent(
    navController: NavController,
    onItemClick: () -> Unit,
    cardColor: Color,
    accentColor: Color,
    textPrimary: Color
) {
    ModalDrawerSheet(
        drawerContainerColor = cardColor,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Guest Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Menu Items
            GuestDrawerItem(
                icon = Icons.Default.Home,
                label = "Home",
                onClick = {
                    navController.navigate("guest_home") {
                        popUpTo("guest_home") { inclusive = true }
                    }
                    onItemClick()
                },
                textPrimary = textPrimary
            )

            GuestDrawerItem(
                icon = Icons.Default.Person,
                label = "View Doctors",
                onClick = {
                    navController.navigate("guest_doctors")
                    onItemClick()
                },
                textPrimary = textPrimary
            )

            GuestDrawerItem(
                icon = Icons.Default.Info,
                label = "About Medify",
                onClick = {
                    navController.navigate("guest_about")
                    onItemClick()
                },
                textPrimary = textPrimary
            )

            GuestDrawerItem(
                icon = Icons.Default.Phone,
                label = "Contact Support",
                onClick = {
                    navController.navigate("guest_contact")
                    onItemClick()
                },
                textPrimary = textPrimary
            )

            Spacer(Modifier.weight(1f))

            Divider(color = accentColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

            // Highlighted Login/Create Account
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("login")
                        onItemClick()
                    },
                shape = RoundedCornerShape(12.dp),
                color = accentColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Login / Create Account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestDrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    textPrimary: Color
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
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }
    }
}

