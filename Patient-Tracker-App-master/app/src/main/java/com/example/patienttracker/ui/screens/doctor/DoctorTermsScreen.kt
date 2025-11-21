package com.example.patienttracker.ui.screens.doctor

import android.content.Context
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

private val BgColor = Color(0xFFFAF8F3)
private val CardColor = Color(0xFFF5F0E8)
private val AccentColor = Color(0xFFB8956A)
private val TextPrimary = Color(0xFF2F2019)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorTermsScreen(
    navController: NavController,
    context: Context
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        },
        containerColor = BgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Last Updated: November 21, 2025",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SectionCard(
                title = "1. Acceptance of Terms",
                content = "By accessing and using the Patient Tracker application as a healthcare provider, you agree to be bound by these Terms and Conditions. If you do not agree with any part of these terms, you may not use our services."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "2. Doctor Responsibilities",
                content = """
                    As a registered doctor on our platform, you agree to:
                    
                    • Maintain valid medical credentials and licenses
                    • Provide accurate and up-to-date information about your qualifications and specializations
                    • Keep your availability schedule current and accurate
                    • Respond to patient appointments in a timely manner
                    • Maintain patient confidentiality in accordance with HIPAA and local regulations
                    • Provide professional medical services to the best of your ability
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "3. Professional Conduct",
                content = """
                    You agree to:
                    
                    • Conduct yourself in a professional manner at all times
                    • Treat all patients with respect and dignity
                    • Follow ethical medical practices
                    • Report any technical issues or concerns promptly
                    • Not misuse the platform or patient information
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "4. Account Security",
                content = "You are responsible for maintaining the confidentiality of your account credentials. You must notify us immediately of any unauthorized access or security breach. You are fully responsible for all activities that occur under your account."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "5. Data Protection",
                content = "All patient data must be handled in compliance with applicable data protection laws. You must not share, sell, or misuse any patient information obtained through the platform. Patient records should only be accessed for legitimate medical purposes."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "6. Service Availability",
                content = "While we strive to provide uninterrupted service, we do not guarantee that the platform will always be available. We reserve the right to modify, suspend, or discontinue any part of the service at any time without prior notice."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "7. Limitation of Liability",
                content = "The platform is provided as a tool to facilitate healthcare delivery. We are not responsible for medical decisions, treatment outcomes, or any disputes between doctors and patients. You agree to indemnify us against any claims arising from your use of the platform."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "8. Termination",
                content = "We reserve the right to suspend or terminate your account if you violate these terms, engage in fraudulent activities, or fail to maintain valid medical credentials. You may also terminate your account at any time by contacting our support team."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "9. Changes to Terms",
                content = "We may update these Terms and Conditions from time to time. Continued use of the platform after changes are posted constitutes acceptance of the modified terms. We will notify you of significant changes via email or in-app notification."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "10. Contact Information",
                content = """
                    For questions or concerns about these terms:
                    
                    Email: support@patienttracker.com
                    Phone: +92-XXX-XXXXXXX
                    
                    We aim to respond to all inquiries within 48 hours.
                """.trimIndent()
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
