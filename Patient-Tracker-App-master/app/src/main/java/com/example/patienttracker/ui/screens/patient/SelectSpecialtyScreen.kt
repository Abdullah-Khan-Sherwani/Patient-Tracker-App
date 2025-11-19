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

// Color scheme
private val HeaderTopColor = Color(0xFFD4AF8C)
private val HeaderBottomColor = Color(0xFFC9956E)
private val BackgroundColor = Color(0xFFF5F1ED)
private val CardWhite = Color(0xFFFFFFFF)
private val StatTextColor = Color(0xFF5C4A42)

data class SpecialtyCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SelectSpecialtyScreen(navController: NavController, context: Context) {
    val specialties = listOf(
        SpecialtyCategory("General", Icons.Default.HealthAndSafety, Color(0xFFD4AF8C)),
        SpecialtyCategory("Cardiologist", Icons.Default.Favorite, Color(0xFFE57373)),
        SpecialtyCategory("Dentist", Icons.Default.Medication, Color(0xFF81C784)),
        SpecialtyCategory("Dermatologist", Icons.Default.Face, Color(0xFF64B5F6)),
        SpecialtyCategory("Orthopedics", Icons.Default.Accessibility, Color(0xFFFFB74D)),
        SpecialtyCategory("Pediatrics", Icons.Default.ChildCare, Color(0xFFBA68C8)),
        SpecialtyCategory("Neurologist", Icons.Default.Psychology, Color(0xFF4DB6AC)),
        SpecialtyCategory("Ophthalmologist", Icons.Default.RemoveRedEye, Color(0xFFA1887F))
    )

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, tonalElevation = 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(HeaderTopColor, HeaderBottomColor)
                            )
                        )
                        .padding(
                            top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select Specialty",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "What type of doctor do you need?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatTextColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(specialties) { specialty ->
                    SpecialtyCard(specialty) {
                        navController.navigate("select_doctor/${specialty.name}")
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialtyCard(specialty: SpecialtyCategory, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = specialty.color.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = specialty.icon,
                        contentDescription = specialty.name,
                        tint = specialty.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = specialty.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = StatTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
