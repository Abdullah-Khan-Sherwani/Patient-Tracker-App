package com.example.patienttracker.ui.screens.patient

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
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.patienttracker.ui.viewmodel.ThemeViewModel

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val ProfileHeaderTopColor = Color(0xFF0E4944)    // Deep Teal
private val ProfileHeaderBottomColor = Color(0xFF16605A) // Lighter Teal
private val ProfileBackgroundColor = Color(0xFFF0F5F4)   // Dim background
private val CardWhiteColor = Color(0xFFFFFFFF)           // Card surface
private val TextDarkColor = Color(0xFF1F2937)            // Dark charcoal text
private val TextLightColor = Color(0xFF6B7280)           // Subtle gray
private val ButtonBeigeColor = Color(0xFF76DCB0)         // Mint accent for buttons
private val ButtonTextColor = Color(0xFF0E4944)          // Deep teal text on mint

// Dark mode colors - Deep Teal Dark Theme
private val DarkHeaderTopColor = Color(0xFF112B28)
private val DarkHeaderBottomColor = Color(0xFF0B1F1D)
private val DarkBackgroundColor = Color(0xFF0B1F1D)
private val DarkCardColor = Color(0xFF112B28)
private val DarkTextColor = Color(0xFFE8F5F3)
private val DarkTextLightColor = Color(0xFFA3C9C4)
private val DarkButtonColor = Color(0xFF76DCB0)

// Health Info Status Colors
private val HealthCompleteColor = Color(0xFF16A34A)  // Green
private val HealthIncompleteColor = Color(0xFFDC2626)  // Red

// Data class for health info status
data class HealthInfoStatus(
    val bloodGroup: String? = null,
    val height: String? = null,
    val weight: String? = null
) {
    val isComplete: Boolean
        get() = !bloodGroup.isNullOrEmpty() && !height.isNullOrEmpty() && !weight.isNullOrEmpty()
}

@Composable
fun PatientProfileScreen(
    navController: NavController,
    firstName: String?,
    lastName: String?,
    themeViewModel: ThemeViewModel
) {
    val safeFirstName = firstName ?: "Patient"
    val safeLastName = lastName ?: ""
    val fullName = if (safeLastName.isNotEmpty()) "$safeFirstName $safeLastName" else safeFirstName
    
    var userPhoneNumber by remember { mutableStateOf<String?>(null) }
    var userDisplayName by remember { mutableStateOf(fullName) }
    var healthInfoStatus by remember { mutableStateOf(HealthInfoStatus()) }
    
    // Fetch user data from Firestore
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val firstName = document.getString("firstName") ?: safeFirstName
                        val lastName = document.getString("lastName") ?: safeLastName
                        val phone = document.getString("phoneNumber")
                        
                        userDisplayName = if (lastName.isNotEmpty()) "$firstName $lastName" else firstName
                        userPhoneNumber = phone
                        
                        // Load health info status
                        healthInfoStatus = HealthInfoStatus(
                            bloodGroup = document.getString("bloodGroup"),
                            height = document.getString("height"),
                            weight = document.getString("weight")
                        )
                    }
                }
        }
    }
    
    // Refresh health info when returning from HealthInformationScreen
    val currentBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(currentBackStackEntry) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        healthInfoStatus = HealthInfoStatus(
                            bloodGroup = document.getString("bloodGroup"),
                            height = document.getString("height"),
                            weight = document.getString("weight")
                        )
                    }
                }
        }
    }
    
    var languageSelected by remember { mutableStateOf(false) } // false = English, true = Urdu
    val isDarkMode by themeViewModel.isDarkMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) DarkBackgroundColor else ProfileBackgroundColor)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        ProfileHeader(
            navController = navController,
            fullName = userDisplayName,
            phoneNumber = userPhoneNumber,
            isDarkMode = isDarkMode
        )

        // General Settings Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Section Title
            Text(
                text = "General",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextColor else TextDarkColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Health Information Card (NEW)
            HealthInfoSettingItem(
                title = stringResource(R.string.health_information),
                subtitle = if (healthInfoStatus.isComplete) 
                    stringResource(R.string.health_info_complete) 
                else 
                    stringResource(R.string.health_info_incomplete),
                isComplete = healthInfoStatus.isComplete,
                onClick = { navController.navigate("health_information") },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Language Setting
            SettingItemWithToggle(
                title = "Language",
                subtitle = if (languageSelected) "Urdu" else "English",
                icon = Icons.Default.Language,
                isEnabled = languageSelected,
                onToggle = { languageSelected = it },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Setting
            SettingItemWithToggle(
                title = "Theme",
                subtitle = if (isDarkMode) "Dark" else "Light",
                icon = Icons.Default.Brightness4,
                isEnabled = isDarkMode,
                onToggle = { newValue ->
                    themeViewModel.toggleDarkMode(newValue)
                },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dependents
            SettingItem(
                title = "Dependents",
                icon = Icons.Default.Person,
                onClick = { navController.navigate("patient_dependents") },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Change Password
            SettingItem(
                title = "Change Password",
                icon = Icons.Default.Lock,
                onClick = { navController.navigate("change_password") },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Account Settings Section Title
            Text(
                text = "Account Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextColor else TextDarkColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Change Email
            SettingItemWithSubtitle(
                title = "Change Email",
                subtitle = Firebase.auth.currentUser?.email ?: "Not set",
                icon = Icons.Default.Email,
                onClick = { /* TODO: Navigate to change email screen */ },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Change Contact Number
            SettingItemWithSubtitle(
                title = "Change Contact Number",
                subtitle = userPhoneNumber ?: "Not set",
                icon = Icons.Default.Phone,
                onClick = { /* TODO: Navigate to change contact screen */ },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Legal Section Title
            Text(
                text = "Legal",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextColor else TextDarkColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Privacy Policy
            SettingItem(
                title = "Privacy Policy",
                icon = Icons.Default.PrivacyTip,
                onClick = { navController.navigate("privacy_policy") },
                isDarkMode = isDarkMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terms and Conditions
            SettingItem(
                title = "Terms & Conditions",
                icon = Icons.Default.Description,
                onClick = { navController.navigate("terms_and_conditions") },
                isDarkMode = isDarkMode
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout Button
        LogoutButton(navController = navController, isDarkMode = isDarkMode)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProfileHeader(navController: NavController, fullName: String, phoneNumber: String?, isDarkMode: Boolean) {
    val headerTopColor = if (isDarkMode) DarkHeaderTopColor else ProfileHeaderTopColor
    val headerBottomColor = if (isDarkMode) DarkHeaderBottomColor else ProfileHeaderBottomColor
    val textColor = if (isDarkMode) DarkTextColor else CardWhiteColor
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(headerTopColor, headerBottomColor)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Back + Title + Bell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    IconButton(
                        onClick = { /* Notification placeholder */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Avatar
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    color = if (isDarkMode) DarkCardColor else CardWhiteColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = if (isDarkMode) DarkTextColor else ProfileHeaderTopColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = "Hello, $fullName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Phone (from Firestore)
                if (!phoneNumber.isNullOrEmpty()) {
                    Text(
                        text = phoneNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingItemWithToggle(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp)),
        color = if (isDarkMode) DarkCardColor else CardWhiteColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) DarkTextColor else TextDarkColor
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDarkMode) DarkTextLightColor else TextLightColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ButtonBeigeColor,
                    checkedTrackColor = ButtonBeigeColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        color = if (isDarkMode) DarkCardColor else CardWhiteColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) DarkTextColor else TextDarkColor
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = if (isDarkMode) DarkTextLightColor else TextLightColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SettingItemWithSubtitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        color = if (isDarkMode) DarkCardColor else CardWhiteColor
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon container
                Surface(
                    shape = CircleShape,
                    color = ProfileHeaderTopColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ProfileHeaderTopColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) DarkTextColor else TextDarkColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDarkMode) DarkTextLightColor else TextLightColor
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = if (isDarkMode) DarkTextLightColor else TextLightColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LogoutButton(navController: NavController, isDarkMode: Boolean) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            onClick = {
                showLogoutDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkMode) DarkButtonColor else ButtonBeigeColor,
                contentColor = if (isDarkMode) DarkTextColor else CardWhiteColor
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "LOG OUT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) DarkTextColor else CardWhiteColor
            )
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        Firebase.auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HealthInfoSettingItem(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        color = if (isDarkMode) DarkCardColor else CardWhiteColor
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon container
                Surface(
                    shape = CircleShape,
                    color = ProfileHeaderTopColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = ProfileHeaderTopColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) DarkTextColor else TextDarkColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isComplete) HealthCompleteColor else HealthIncompleteColor
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status indicator
                Surface(
                    shape = CircleShape,
                    color = if (isComplete) 
                        HealthCompleteColor.copy(alpha = 0.1f) 
                    else 
                        HealthIncompleteColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isComplete) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.PriorityHigh,
                            contentDescription = if (isComplete) "Complete" else "Incomplete",
                            tint = if (isComplete) HealthCompleteColor else HealthIncompleteColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = if (isDarkMode) DarkTextLightColor else TextLightColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}