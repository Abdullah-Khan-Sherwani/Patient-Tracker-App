package com.example.patienttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.patienttracker.R
import com.example.patienttracker.ui.viewmodel.HealthInfoMode
import com.example.patienttracker.ui.viewmodel.HealthInfoViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// Teal Medical Design System
// ============================================================
private val TealAccent = Color(0xFF0E4944)
private val MintAccent = Color(0xFF76DCB0)
private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1F2937)
private val TextLight = Color(0xFF6B7280)
private val DividerColor = Color(0xFFE5E7EB)
private val WarningColor = Color(0xFFF59E0B)

/**
 * Read-only health information summary card for doctors
 * Displays patient's blood group, height, weight, age, and last updated time
 */
@Composable
fun PatientHealthSummaryCard(
    patientUid: String,
    modifier: Modifier = Modifier
) {
    val healthInfoViewModel: HealthInfoViewModel = viewModel(
        key = "doctor_view_$patientUid",
        factory = HealthInfoViewModel.Factory(
            HealthInfoMode.DoctorReadOnly(patientUid)
        )
    )
    
    val state by healthInfoViewModel.state
    
    if (state.isLoading) {
        // Compact loading state
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = TealAccent,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading health info...",
                    fontSize = 14.sp,
                    color = TextLight
                )
            }
        }
        return
    }
    
    val hasAnyData = !state.bloodGroup.isNullOrEmpty() || 
                     state.height.isNotEmpty() || 
                     state.weight.isNotEmpty()
    
    val isComplete = !state.bloodGroup.isNullOrEmpty() && 
                     state.height.isNotEmpty() && 
                     state.weight.isNotEmpty()
    
    val age = healthInfoViewModel.calculateAge()
    val lastUpdated = healthInfoViewModel.getLatestUpdateTimestamp()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = TealAccent.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = stringResource(R.string.patient_health_summary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }
                
                // Last updated badge
                lastUpdated?.let { timestamp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TealAccent.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.last_updated, 
                                formatTimestamp(timestamp)
                            ),
                            fontSize = 11.sp,
                            color = TealAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider(color = DividerColor, thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (!hasAnyData) {
                // No data available
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.health_details_unavailable),
                        fontSize = 13.sp,
                        color = TextLight
                    )
                }
            } else {
                // Two column layout for health stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Blood Group
                        HealthStatItem(
                            icon = Icons.Default.Bloodtype,
                            label = stringResource(R.string.blood_group),
                            value = state.bloodGroup ?: stringResource(R.string.not_set),
                            isSet = !state.bloodGroup.isNullOrEmpty()
                        )
                        
                        // Height
                        HealthStatItem(
                            icon = Icons.Default.Height,
                            label = stringResource(R.string.height_label),
                            value = if (state.height.isNotEmpty()) 
                                "${state.height} ${stringResource(R.string.height_unit)}" 
                            else 
                                stringResource(R.string.not_set),
                            isSet = state.height.isNotEmpty()
                        )
                    }
                    
                    // Right column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Age
                        HealthStatItem(
                            icon = Icons.Default.Cake,
                            label = stringResource(R.string.age_label),
                            value = age?.let { stringResource(R.string.age_years, it) } 
                                ?: stringResource(R.string.not_set),
                            isSet = age != null
                        )
                        
                        // Weight
                        HealthStatItem(
                            icon = Icons.Default.MonitorWeight,
                            label = stringResource(R.string.weight_label),
                            value = if (state.weight.isNotEmpty()) 
                                "${state.weight} ${stringResource(R.string.weight_unit)}" 
                            else 
                                stringResource(R.string.not_set),
                            isSet = state.weight.isNotEmpty()
                        )
                    }
                }
                
                // Warning if incomplete
                if (!isComplete) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarningColor.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WarningColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.health_details_unavailable),
                                fontSize = 12.sp,
                                color = WarningColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isSet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = TealAccent.copy(alpha = 0.08f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSet) TealAccent else TextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextLight
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSet) TextDark else TextLight
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}
