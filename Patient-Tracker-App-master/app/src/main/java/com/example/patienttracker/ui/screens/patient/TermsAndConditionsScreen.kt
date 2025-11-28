package com.example.patienttracker.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ============================================================
// Deep Teal & Mint Design System
// ============================================================
private val BackgroundColor = Color(0xFFF0F5F4)     // Dim background
private val CardWhite = Color(0xFFFFFFFF)           // Card surface
private val HeaderColor = Color(0xFF0E4944)         // Deep Teal
private val TextColor = Color(0xFF1F2937)           // Dark charcoal
private val SubTextColor = Color(0xFF6B7280)        // Subtle gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    TermsSection(
                        title = "Section 1 — Purpose",
                        content = "Medify allows patients to store and manage health records, book appointments and communicate with doctors when permitted."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TermsSection(
                        title = "Section 2 — Personal Information",
                        content = "Users are responsible for providing accurate personal information. Medify is not liable for losses caused by false or incomplete information."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TermsSection(
                        title = "Section 3 — Appointments",
                        content = "Appointment dates and times are controlled by doctors and availability. Medify is not responsible for last-minute cancellations or schedule changes."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TermsSection(
                        title = "Section 4 — Medical Records",
                        content = "Users must upload valid medical records that belong to them. Illegitimate or misleading files may result in restricted access."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TermsSection(
                        title = "Section 5 — Emergency Disclaimer",
                        content = "Medify is not an emergency medical service. In case of emergency, contact local medical services directly."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    TermsSection(
                        title = "Section 6 — Updates & Modifications",
                        content = "Medify may update terms and features over time. Continued usage of the app means the user accepts these updates."
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextColor,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = content,
            fontSize = 15.sp,
            color = SubTextColor,
            lineHeight = 22.sp
        )
    }
}
