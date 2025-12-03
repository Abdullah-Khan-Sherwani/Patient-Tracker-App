package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.patienttracker.R
import com.example.patienttracker.ui.viewmodel.HealthInfoEvent
import com.example.patienttracker.ui.viewmodel.HealthInfoViewModel
import com.example.patienttracker.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest

// ============================================================
// Teal Medical Design System (matching PatientProfileScreen)
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)       // Deep Teal
private val HeaderBottomColor = Color(0xFF16605A)    // Lighter Teal
private val BackgroundColor = Color(0xFFF0F5F4)      // Dim background
private val CardWhiteColor = Color(0xFFFFFFFF)       // Card surface
private val TextDarkColor = Color(0xFF1F2937)        // Dark charcoal text
private val TextLightColor = Color(0xFF6B7280)       // Subtle gray
private val TealAccent = Color(0xFF0E4944)           // Teal accent
private val MintAccent = Color(0xFF76DCB0)           // Mint accent
private val DividerColor = Color(0xFFE5E7EB)         // Light divider
private val ErrorColor = Color(0xFFDC2626)           // Red for errors
private val SuccessColor = Color(0xFF16A34A)         // Green for success
private val LockedColor = Color(0xFF9CA3AF)          // Gray for locked fields

// Dark mode colors
private val DarkHeaderTopColor = Color(0xFF112B28)
private val DarkHeaderBottomColor = Color(0xFF0B1F1D)
private val DarkBackgroundColor = Color(0xFF0B1F1D)
private val DarkCardColor = Color(0xFF112B28)
private val DarkTextColor = Color(0xFFE8F5F3)
private val DarkTextLightColor = Color(0xFFA3C9C4)
private val DarkDividerColor = Color(0xFF1F3D38)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthInformationScreen(
    navController: NavController,
    context: Context = LocalContext.current,
    themeViewModel: ThemeViewModel,
    healthInfoViewModel: HealthInfoViewModel = viewModel()
) {
    val state by healthInfoViewModel.state
    val isDarkMode by themeViewModel.isDarkMode
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Blood group confirmation dialog state
    var showBloodGroupDialog by remember { mutableStateOf(false) }
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var bloodGroupDropdownExpanded by remember { mutableStateOf(false) }
    
    // Collect events
    LaunchedEffect(Unit) {
        healthInfoViewModel.events.collectLatest { event ->
            when (event) {
                is HealthInfoEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.health_info_updated),
                        duration = SnackbarDuration.Short
                    )
                }
                is HealthInfoEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    val backgroundColor = if (isDarkMode) DarkBackgroundColor else BackgroundColor
    val cardColor = if (isDarkMode) DarkCardColor else CardWhiteColor
    val textColor = if (isDarkMode) DarkTextColor else TextDarkColor
    val textLightColor = if (isDarkMode) DarkTextLightColor else TextLightColor
    val dividerColor = if (isDarkMode) DarkDividerColor else DividerColor
    
    Scaffold(
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TealAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            HealthInfoHeader(
                navController = navController,
                isDarkMode = isDarkMode
            )
            
            // Content
            if (state.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TealAccent,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Age Card (non-editable, calculated from DOB)
                    val age = healthInfoViewModel.calculateAge() ?: 21  // Default to 21 if not set
                    HealthInfoCard(
                        title = stringResource(R.string.age_label),
                        icon = Icons.Default.Cake,
                        isDarkMode = isDarkMode,
                        cardColor = cardColor,
                        textColor = textColor,
                        textLightColor = textLightColor
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.age_years, age),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = if (healthInfoViewModel.calculateAge() != null) 
                                    stringResource(R.string.age_calculated) 
                                else 
                                    "Default",
                                fontSize = 12.sp,
                                color = textLightColor
                            )
                        }
                    }
                    
                    // Blood Group Card (read-only for patients - only admin/doctor can set)
                    HealthInfoCard(
                        title = stringResource(R.string.blood_group),
                        icon = Icons.Default.Bloodtype,
                        isDarkMode = isDarkMode,
                        cardColor = cardColor,
                        textColor = textColor,
                        textLightColor = textLightColor,
                        isLocked = state.isBloodGroupLocked || !healthInfoViewModel.canEditBloodGroup,
                        showMandatoryIndicator = !state.isBloodGroupLocked && state.bloodGroup.isNullOrEmpty()
                    ) {
                        if (state.isBloodGroupLocked || !state.bloodGroup.isNullOrEmpty()) {
                            // Display blood group (locked or set)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Blood group badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TealAccent.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = state.bloodGroup ?: "",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealAccent,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = LockedColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.blood_group_locked),
                                        fontSize = 11.sp,
                                        color = LockedColor
                                    )
                                }
                            }
                        } else if (healthInfoViewModel.canEditBloodGroup) {
                            // Blood group dropdown - only for doctors/admins
                            ExposedDropdownMenuBox(
                                expanded = bloodGroupDropdownExpanded,
                                onExpandedChange = { bloodGroupDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedBloodGroup ?: state.bloodGroup ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { 
                                        Text(
                                            stringResource(R.string.blood_group_hint),
                                            color = textLightColor
                                        ) 
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = dividerColor,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = bloodGroupDropdownExpanded,
                                    onDismissRequest = { bloodGroupDropdownExpanded = false }
                                ) {
                                    healthInfoViewModel.bloodGroupOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    option,
                                                    fontWeight = FontWeight.Medium
                                                ) 
                                            },
                                            onClick = {
                                                selectedBloodGroup = option
                                                bloodGroupDropdownExpanded = false
                                                showBloodGroupDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Bloodtype,
                                                    contentDescription = null,
                                                    tint = TealAccent
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Patient view - blood group not set yet, show message
                            Column {
                                Text(
                                    text = stringResource(R.string.blood_group_not_set),
                                    fontSize = 16.sp,
                                    color = textLightColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.only_doctor_can_edit),
                                    fontSize = 12.sp,
                                    color = textLightColor
                                )
                            }
                        }
                    }
                    
                    // Height Card (read-only for patients - only admin/doctor can edit)
                    HealthInfoCard(
                        title = stringResource(R.string.height_label),
                        icon = Icons.Default.Height,
                        isDarkMode = isDarkMode,
                        cardColor = cardColor,
                        textColor = textColor,
                        textLightColor = textLightColor,
                        showMandatoryIndicator = state.height.isEmpty()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.height,
                                onValueChange = { healthInfoViewModel.updateHeight(it) },
                                placeholder = { 
                                    Text(
                                        stringResource(R.string.height_hint),
                                        color = textLightColor
                                    ) 
                                },
                                suffix = { 
                                    Text(
                                        stringResource(R.string.height_unit),
                                        color = textLightColor,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = healthInfoViewModel.isReadOnly,
                                enabled = !healthInfoViewModel.isReadOnly,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = dividerColor,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledBorderColor = dividerColor,
                                    disabledTextColor = textColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Show info that only doctor/admin can edit
                            if (healthInfoViewModel.isReadOnly && state.height.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.only_doctor_can_edit),
                                    fontSize = 12.sp,
                                    color = textLightColor
                                )
                            }
                            
                            // Last updated timestamp
                            state.heightLastUpdated?.let { timestamp ->
                                Text(
                                    text = stringResource(
                                        R.string.last_updated_on,
                                        healthInfoViewModel.formatTimestamp(timestamp) ?: ""
                                    ),
                                    fontSize = 12.sp,
                                    color = textLightColor
                                )
                            }
                        }
                    }
                    
                    // Weight Card (read-only for patients - only admin/doctor can edit)
                    HealthInfoCard(
                        title = stringResource(R.string.weight_label),
                        icon = Icons.Default.MonitorWeight,
                        isDarkMode = isDarkMode,
                        cardColor = cardColor,
                        textColor = textColor,
                        textLightColor = textLightColor,
                        showMandatoryIndicator = state.weight.isEmpty()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.weight,
                                onValueChange = { healthInfoViewModel.updateWeight(it) },
                                placeholder = { 
                                    Text(
                                        stringResource(R.string.weight_hint),
                                        color = textLightColor
                                    ) 
                                },
                                suffix = { 
                                    Text(
                                        stringResource(R.string.weight_unit),
                                        color = textLightColor,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = healthInfoViewModel.isReadOnly,
                                enabled = !healthInfoViewModel.isReadOnly,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealAccent,
                                    unfocusedBorderColor = dividerColor,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledBorderColor = dividerColor,
                                    disabledTextColor = textColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Show info that only doctor/admin can edit
                            if (healthInfoViewModel.isReadOnly && state.weight.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.only_doctor_can_edit),
                                    fontSize = 12.sp,
                                    color = textLightColor
                                )
                            }
                            
                            // Last updated timestamp
                            state.weightLastUpdated?.let { timestamp ->
                                Text(
                                    text = stringResource(
                                        R.string.last_updated_on,
                                        healthInfoViewModel.formatTimestamp(timestamp) ?: ""
                                    ),
                                    fontSize = 12.sp,
                                    color = textLightColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Save Button - only show if user can edit (doctors/admins)
                    if (!healthInfoViewModel.isReadOnly) {
                        Button(
                            onClick = { healthInfoViewModel.saveHeightAndWeight() },
                            enabled = healthInfoViewModel.hasUnsavedChanges() && !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TealAccent,
                                contentColor = Color.White,
                                disabledContainerColor = TealAccent.copy(alpha = 0.3f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.saving),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.save_changes),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Blood Group Confirmation Dialog
    if (showBloodGroupDialog && selectedBloodGroup != null) {
        AlertDialog(
            onDismissRequest = { 
                showBloodGroupDialog = false
                selectedBloodGroup = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bloodtype,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.confirm_blood_group),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.blood_group_warning, selectedBloodGroup ?: ""),
                    textAlign = TextAlign.Center,
                    color = TextLightColor
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedBloodGroup?.let { bg ->
                            healthInfoViewModel.saveBloodGroup(bg)
                        }
                        showBloodGroupDialog = false
                        selectedBloodGroup = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBloodGroupDialog = false
                        selectedBloodGroup = null
                    }
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = TextLightColor
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun HealthInfoHeader(
    navController: NavController,
    isDarkMode: Boolean
) {
    val headerTopColor = if (isDarkMode) DarkHeaderTopColor else HeaderTopColor
    val headerBottomColor = if (isDarkMode) DarkHeaderBottomColor else HeaderBottomColor
    val textColor = if (isDarkMode) DarkTextColor else Color.White
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
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
                // Top Row: Back + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = stringResource(R.string.health_information),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    // Spacer for symmetry
                    Spacer(modifier = Modifier.size(40.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Medical icon
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MintAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.health_info_subtitle),
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HealthInfoCard(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    cardColor: Color,
    textColor: Color,
    textLightColor: Color,
    isLocked: Boolean = false,
    showMandatoryIndicator: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp)),
        color = cardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon container
                    Surface(
                        shape = CircleShape,
                        color = TealAccent.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
                
                // Status indicator
                if (showMandatoryIndicator) {
                    Surface(
                        shape = CircleShape,
                        color = ErrorColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = "Required",
                                tint = ErrorColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = SuccessColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider
            Divider(
                color = if (isDarkMode) DarkDividerColor else DividerColor,
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            content()
        }
    }
}
