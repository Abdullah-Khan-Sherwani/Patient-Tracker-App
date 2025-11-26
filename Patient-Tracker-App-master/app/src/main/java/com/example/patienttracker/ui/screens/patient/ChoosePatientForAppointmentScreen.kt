package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.Dependent
import com.example.patienttracker.data.DependentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

// Beige theme colors
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)
private val ButtonColor = Color(0xFFC9956E)
private val AccentColor = Color(0xFFD4AF8C)
private val SelectionColor = Color(0xFFE8D4C4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoosePatientForAppointmentScreen(
    navController: NavController,
    context: Context,
    doctorId: String,
    doctorName: String,
    specialty: String,
    dateStr: String,
    blockName: String,
    timeRange: String
) {
    val scope = rememberCoroutineScope()
    var dependents by remember { mutableStateOf<List<Dependent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDependentId by remember { mutableStateOf("") }
    var selectedDependentName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                dependents = DependentRepository.getDependentsForParent(currentUser.uid)
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient and back button
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Who is this appointment for?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Self card
                    item {
                        PatientSelectCard(
                            name = "Self",
                            subtitle = "Book for yourself",
                            isSelected = selectedDependentId.isEmpty() || selectedDependentId == "self",
                            onClick = {
                                selectedDependentId = "self"
                                selectedDependentName = "Self"
                            }
                        )
                    }

                    if (dependents.isNotEmpty()) {
                        item {
                            Text(
                                text = "Dependents",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = StatTextColor,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(dependents) { dep ->
                            PatientSelectCard(
                                name = dep.getFullName(),
                                subtitle = dep.relationship,
                                metadata = dep.dob,
                                isSelected = selectedDependentId == dep.dependentId,
                                onClick = {
                                    selectedDependentId = dep.dependentId
                                    selectedDependentName = dep.getFullName()
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Button(
                            onClick = {
                                // default to self if none selected
                                val depId = if (selectedDependentId.isBlank()) "self" else selectedDependentId
                                val depName = if (selectedDependentName.isBlank()) "Self" else selectedDependentName
                                val encodedDepId = URLEncoder.encode(depId, StandardCharsets.UTF_8.toString())
                                val encodedDepName = URLEncoder.encode(depName, StandardCharsets.UTF_8.toString())
                                val encodedTimeRange = URLEncoder.encode(timeRange, StandardCharsets.UTF_8.toString())
                                navController.navigate("confirm_appointment/$doctorId/${URLEncoder.encode(doctorName, StandardCharsets.UTF_8.toString())}/$specialty/$dateStr/$blockName/$encodedTimeRange/$encodedDepId/$encodedDepName")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)
                        ) {
                            Text(
                                text = "Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientSelectCard(
    name: String,
    subtitle: String,
    metadata: String = "",
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SelectionColor else CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = StatTextColor
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            if (metadata.isNotBlank()) {
                Text(
                    text = metadata,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Selection indicator
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = ButtonColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
