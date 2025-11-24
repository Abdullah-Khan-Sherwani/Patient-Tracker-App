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
fun DoctorPrivacyScreen(
    navController: NavController,
    context: Context
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
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
                text = "Effective Date: November 21, 2025",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SectionCard(
                title = "1. Introduction",
                content = "Patient Tracker is committed to protecting your privacy and the privacy of the patients you serve. This Privacy Policy explains how we collect, use, disclose, and safeguard information when you use our healthcare management platform as a registered doctor."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "2. Information We Collect",
                content = """
                    We collect the following types of information:
                    
                    Personal Information:
                    • Name, email address, phone number
                    • Medical credentials and license information
                    • Specialization and qualifications
                    • Work schedule and availability
                    
                    Professional Information:
                    • Patient appointment records
                    • Medical notes and observations
                    • Treatment history
                    
                    Technical Information:
                    • Device information and IP address
                    • Usage data and activity logs
                    • Session information
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "3. How We Use Your Information",
                content = """
                    We use collected information to:
                    
                    • Provide and maintain our services
                    • Facilitate appointment scheduling and management
                    • Enable communication between doctors and patients
                    • Verify your professional credentials
                    • Improve platform functionality and user experience
                    • Ensure compliance with healthcare regulations
                    • Send important notifications and updates
                    • Provide technical support
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "4. Data Security",
                content = "We implement industry-standard security measures to protect your data, including encryption, secure servers, and regular security audits. However, no method of transmission over the internet is 100% secure. We use Firebase authentication and Firestore database with security rules to protect sensitive information."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "5. Patient Data Protection",
                content = """
                    As a healthcare provider, you have access to sensitive patient information. You must:
                    
                    • Only access patient data for legitimate medical purposes
                    • Comply with HIPAA and local data protection regulations
                    • Not share patient information with unauthorized parties
                    • Report any data breaches immediately
                    • Use secure connections when accessing the platform
                    
                    We encrypt all patient data in transit and at rest.
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "6. Information Sharing",
                content = """
                    We do not sell your personal information. We may share information with:
                    
                    • Patients you are treating (appointment and treatment information)
                    • Healthcare administrators for operational purposes
                    • Law enforcement if required by legal obligation
                    • Service providers who assist in platform operations (under strict confidentiality agreements)
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "7. Data Retention",
                content = "We retain your account information for as long as your account is active. Medical records and appointment data are retained according to legal requirements (typically 7-10 years). You may request account deletion, but some information may be retained for legal compliance."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "8. Your Rights",
                content = """
                    You have the right to:
                    
                    • Access your personal information
                    • Correct inaccurate data
                    • Request deletion of your account
                    • Opt-out of non-essential communications
                    • Export your data in a portable format
                    • Lodge complaints with data protection authorities
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "9. Cookies and Tracking",
                content = "We use session cookies and similar technologies to maintain your logged-in state and improve user experience. You can control cookie settings through your device, but this may affect platform functionality."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "10. Third-Party Services",
                content = "We use Firebase (Google Cloud) for authentication, database, and storage services. These services have their own privacy policies. We ensure all third-party providers comply with applicable data protection regulations."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "11. Updates to Privacy Policy",
                content = "We may update this Privacy Policy periodically. We will notify you of significant changes via email or in-app notification. Continued use of the platform after updates constitutes acceptance of the revised policy."
            )

            Spacer(Modifier.height(16.dp))

            SectionCard(
                title = "12. Contact Us",
                content = """
                    For privacy-related questions or concerns:
                    
                    Email: privacy@patienttracker.com
                    Data Protection Officer: dpo@patienttracker.com
                    Phone: +92-XXX-XXXXXXX
                    
                    We respond to all privacy inquiries within 30 days.
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
