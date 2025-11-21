package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars

/**
 * Select Specialty Screen
 * 
 * THEME FIX: Removed all hardcoded colors, now uses MaterialTheme.colorScheme
 * for proper dark mode support
 */

data class SpecialtyCategory(
    val name: String,
    val icon: ImageVector,
    val description: String
)

// Complete list of all specializations (always displayed)
private fun getAllSpecialtiesForBooking(): List<SpecialtyCategory> {
    return listOf(
        SpecialtyCategory("General", Icons.Default.MedicalServices, "All doctors and general specialists"),
        SpecialtyCategory("General Physician", Icons.Default.MedicalServices, "Primary care for common medical concerns"),
        SpecialtyCategory("Cardiologist", Icons.Default.Favorite, "Heart and blood circulation specialist"),
        SpecialtyCategory("Dermatologist", Icons.Default.Face, "Skin, hair and nail treatments"),
        SpecialtyCategory("Pediatrician", Icons.Default.ChildCare, "Care for babies and children"),
        SpecialtyCategory("Neurologist", Icons.Default.Psychology, "Brain and nervous system specialist"),
        SpecialtyCategory("Psychiatrist", Icons.Default.SelfImprovement, "Mental health and anxiety support"),
        SpecialtyCategory("ENT Specialist", Icons.Default.Hearing, "Ear, nose and throat issues"),
        SpecialtyCategory("Orthopedic", Icons.Default.Accessibility, "Bones, joints and muscle concerns"),
        SpecialtyCategory("Gynecologist", Icons.Default.PregnantWoman, "Women's reproductive care"),
        SpecialtyCategory("Dentist", Icons.Default.LocalHospital, "Teeth and oral health"),
        SpecialtyCategory("Urologist", Icons.Default.WaterDrop, "Kidneys, bladder and urinary concerns"),
        SpecialtyCategory("Oncologist", Icons.Default.HealthAndSafety, "Cancer diagnosis and treatment"),
        SpecialtyCategory("Radiologist", Icons.Default.Scanner, "Medical imaging and scan analysis")
    )
}

@Composable
fun SelectSpecialtyScreen(navController: NavController, context: Context) {
    val specialties = getAllSpecialtiesForBooking()

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // THEME FIX: Use theme colors for gradient
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(
                            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            // THEME FIX: Use onPrimary for icon on primary background
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "Select Specialty",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        // THEME FIX: Use background color from theme
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(specialties) { specialty ->
                    BookingSpecialtyCard(specialty) {
                        navController.navigate("select_doctor/${specialty.name}")
                    }
                }
            }
        }
    }
}

/**
 * Booking Specialty Card Component
 * THEME FIX: Now uses MaterialTheme.colorScheme
 */
@Composable
fun BookingSpecialtyCard(specialty: SpecialtyCategory, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        // THEME FIX: Use surface color from theme
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                // THEME FIX: Use primaryContainer for icon background
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = specialty.icon,
                        contentDescription = specialty.name,
                        modifier = Modifier.size(28.dp),
                        // THEME FIX: Use onPrimaryContainer
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Specialty name
            Text(
                text = specialty.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                // THEME FIX: Use onSurface
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description
            Text(
                text = specialty.description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                // THEME FIX: Use onSurfaceVariant
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}
