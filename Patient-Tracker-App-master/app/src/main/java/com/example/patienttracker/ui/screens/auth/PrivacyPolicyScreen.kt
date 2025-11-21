package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Privacy Policy screen displayed during sign-up/login flow.
 * Users must accept the privacy policy before proceeding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController, onAccept: () -> Unit = {}) {
    var agreedToPrivacy by remember { mutableStateOf(false) }
    
    val primaryColor = Color(0xFFB8956A)
    val bgColor = Color(0xFFFAF8F3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = primaryColor
                        )
                        Text("Privacy Policy", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = primaryColor,
                    navigationIconContentColor = primaryColor
                )
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Last Updated
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Last Updated: January 2024",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Section 1: Introduction
                PrivacySection(
                    title = "1. Introduction",
                    content = "Welcome to Patient Tracker. We are committed to protecting your personal data and respecting your privacy. " +
                            "This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our medical appointment booking application."
                )

                // Section 2: Information We Collect
                PrivacySection(
                    title = "2. Information We Collect",
                    content = """
                        We collect the following types of information:
                        
                        • Personal Identification: Name, date of birth, email address, phone number
                        • Health Information: Medical history, appointment details, health records you choose to upload
                        • Account Data: Login credentials, account preferences
                        • Device Information: Device type, OS version, unique identifiers
                        • Usage Data: How you interact with the app, appointment bookings
                        • Location Data: With your consent for nearby doctors feature
                    """.trimIndent()
                )

                // Section 3: How We Use Your Information
                PrivacySection(
                    title = "3. How We Use Your Information",
                    content = """
                        We use the collected information for:
                        
                        • Providing and improving our services
                        • Scheduling and managing your appointments
                        • Sending appointment reminders and notifications
                        • Processing payments for consultation fees
                        • Communicating important updates
                        • Security and fraud prevention
                        • Compliance with legal obligations
                    """.trimIndent()
                )

                // Section 4: Data Security
                PrivacySection(
                    title = "4. Data Security",
                    content = "We implement comprehensive security measures including encryption, secure servers, and access controls to protect your personal data. " +
                            "However, no method of transmission over the internet is 100% secure. We cannot guarantee absolute security of your information."
                )

                // Section 5: Third-Party Services
                PrivacySection(
                    title = "5. Third-Party Services",
                    content = "We use Firebase for backend services including authentication, data storage, and cloud messaging. " +
                            "We may share your information with healthcare providers for appointment purposes. " +
                            "We do not sell your personal information to third parties."
                )

                // Section 6: Your Rights
                PrivacySection(
                    title = "6. Your Privacy Rights",
                    content = """
                        You have the right to:
                        
                        • Access your personal data
                        • Correct inaccurate information
                        • Request deletion of your data (subject to legal retention requirements)
                        • Opt-out of marketing communications
                        • Data portability
                        
                        Contact us at support@patienttracker.com to exercise these rights.
                    """.trimIndent()
                )

                // Section 7: Contact Us
                PrivacySection(
                    title = "7. Contact Us",
                    content = "If you have questions about this Privacy Policy, please contact our Data Protection Officer at:\n\n" +
                            "📧 privacy@patienttracker.com\n" +
                            "📞 +92-300-1234567\n" +
                            "🏢 Patient Tracker Support, Islamabad, Pakistan"
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            Divider()

            // Acceptance Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToPrivacy,
                        onCheckedChange = { agreedToPrivacy = it }
                    )
                    Text(
                        text = "I have read and agree to the Privacy Policy",
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        if (agreedToPrivacy) {
                            onAccept()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = agreedToPrivacy,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Accept & Continue", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Decline", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String,
    primaryColor: Color = Color(0xFFB8956A)
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryColor
        )
        Text(
            text = content,
            fontSize = 13.sp,
            color = Color(0xFF5C4A42),
            lineHeight = 18.sp
        )
    }
}
