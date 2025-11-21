package com.example.patienttracker.ui.screens.patient.drawer

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

private val BackgroundColor = Color(0xFFF5F1ED)
private val HeaderColor = Color(0xFFD4AF8C)
private val CardWhite = Color(0xFFFFFFFF)
private val TextColor = Color(0xFF333333)
private val SubTextColor = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMedifyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Medify") },
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
                    Text(
                        text = "Our Mission",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Medify is a digital medical companion designed to help patients track their health records, manage prescriptions, schedule and track appointments, and communicate with doctors securely.",
                        fontSize = 15.sp,
                        color = SubTextColor,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Our mission is to make healthcare more organized and accessible for everyone from home. We believe that managing your health should be simple, secure, and empowering.",
                        fontSize = 15.sp,
                        color = SubTextColor,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Key Features",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FeatureItem("Secure storage of medical records and reports")
                    FeatureItem("Easy appointment booking and management")
                    FeatureItem("Direct communication with healthcare providers")
                    FeatureItem("Prescription tracking and reminders")
                    FeatureItem("Complete medical history at your fingertips")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "• ",
            fontSize = 15.sp,
            color = HeaderColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            fontSize = 15.sp,
            color = SubTextColor,
            lineHeight = 22.sp
        )
    }
}
