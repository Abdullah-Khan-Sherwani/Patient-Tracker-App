package com.example.patienttracker.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFDDD2CE)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7ECE8))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Last Updated: November 22, 2025",
                fontSize = 13.sp,
                color = Color(0xFF6B5B54),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            SectionTitle("1. Acceptance of Terms")
            SectionContent(
                "By accessing and using the Patient Tracker App ('the App'), you accept and agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the App. Your continued use of the App constitutes acceptance of any modifications to these terms."
            )

            SectionTitle("2. Description of Service")
            SectionContent(
                "Patient Tracker App is a healthcare management platform that facilitates:\n\n" +
                "• Appointment scheduling between patients and doctors\n" +
                "• Medical record storage and management\n" +
                "• Secure communication between healthcare providers and patients\n" +
                "• Prescription and test report management\n" +
                "• Health data tracking and monitoring\n\n" +
                "The App is designed to assist with healthcare coordination but does not replace professional medical advice, diagnosis, or treatment."
            )

            SectionTitle("3. User Eligibility")
            SectionContent(
                "To use the App, you must:\n\n" +
                "• Be at least 18 years old or have parental/guardian consent\n" +
                "• Provide accurate and complete registration information\n" +
                "• Maintain the confidentiality of your account credentials\n" +
                "• Be legally capable of entering into binding contracts\n" +
                "• Comply with all local, state, and federal laws"
            )

            SectionTitle("4. Account Registration")
            SectionContent(
                "When creating an account, you agree to:\n\n" +
                "• Provide truthful, accurate, and complete information\n" +
                "• Update your information promptly when changes occur\n" +
                "• Keep your password secure and confidential\n" +
                "• Notify us immediately of unauthorized account access\n" +
                "• Be responsible for all activities under your account\n\n" +
                "We reserve the right to suspend or terminate accounts that violate these terms or provide false information."
            )

            SectionTitle("5. User Responsibilities")
            
            SubSectionTitle("5.1 For Patients")
            SectionContent(
                "• Provide accurate medical history and health information\n" +
                "• Attend scheduled appointments or cancel with appropriate notice\n" +
                "• Follow prescribed treatment plans and medical advice\n" +
                "• Report any adverse effects or concerns promptly\n" +
                "• Respect healthcare providers' time and expertise"
            )

            SubSectionTitle("5.2 For Doctors")
            SectionContent(
                "• Maintain valid medical licenses and certifications\n" +
                "• Provide professional and ethical healthcare services\n" +
                "• Keep patient information confidential and secure\n" +
                "• Respond to patient inquiries within reasonable timeframes\n" +
                "• Comply with medical best practices and regulations"
            )

            SectionTitle("6. Medical Disclaimer")
            SectionContent(
                "IMPORTANT: The Patient Tracker App is a communication and record-keeping tool only. It does not:\n\n" +
                "• Provide medical diagnoses or treatment recommendations\n" +
                "• Replace in-person medical consultations\n" +
                "• Guarantee specific health outcomes\n" +
                "• Serve as a substitute for emergency medical services\n\n" +
                "IN CASE OF MEDICAL EMERGENCY, CALL YOUR LOCAL EMERGENCY NUMBER IMMEDIATELY. Do not rely on the App for emergency medical assistance."
            )

            SectionTitle("7. Prohibited Activities")
            SectionContent(
                "Users may not:\n\n" +
                "• Share false or misleading health information\n" +
                "• Impersonate healthcare professionals or other users\n" +
                "• Attempt to access unauthorized areas of the App\n" +
                "• Upload malicious code, viruses, or harmful content\n" +
                "• Use the App for illegal or fraudulent purposes\n" +
                "• Harass, abuse, or threaten other users\n" +
                "• Sell or transfer your account to others\n" +
                "• Extract or scrape data without permission"
            )

            SectionTitle("8. Intellectual Property")
            SectionContent(
                "All content, features, and functionality of the App, including but not limited to text, graphics, logos, icons, images, and software, are the exclusive property of Patient Tracker App and are protected by copyright, trademark, and other intellectual property laws.\n\n" +
                "You may not:\n" +
                "• Reproduce, distribute, or modify any content without written permission\n" +
                "• Use our trademarks or branding without authorization\n" +
                "• Reverse engineer or decompile the application"
            )

            SectionTitle("9. Payment and Fees")
            SectionContent(
                "• Consultation fees are determined by individual healthcare providers\n" +
                "• All fees must be paid as agreed upon with the provider\n" +
                "• The App may charge service fees for premium features\n" +
                "• Refunds are subject to our refund policy and applicable laws\n" +
                "• We reserve the right to modify pricing with advance notice"
            )

            SectionTitle("10. Appointment Cancellations")
            SectionContent(
                "• Patients must cancel appointments at least 24 hours in advance\n" +
                "• Late cancellations may incur cancellation fees\n" +
                "• Doctors may cancel appointments due to emergencies or unforeseen circumstances\n" +
                "• Repeated no-shows may result in account suspension"
            )

            SectionTitle("11. Data Privacy and Security")
            SectionContent(
                "We are committed to protecting your privacy. Please review our Privacy Policy for detailed information about data collection, use, and protection. By using the App, you consent to our data practices as described in the Privacy Policy."
            )

            SectionTitle("12. Limitation of Liability")
            SectionContent(
                "TO THE FULLEST EXTENT PERMITTED BY LAW:\n\n" +
                "• We are not liable for any medical outcomes or complications\n" +
                "• The App is provided 'AS IS' without warranties of any kind\n" +
                "• We do not guarantee uninterrupted or error-free service\n" +
                "• We are not responsible for third-party content or services\n" +
                "• Our maximum liability is limited to fees paid for the service\n" +
                "• We are not liable for indirect, incidental, or consequential damages"
            )

            SectionTitle("13. Indemnification")
            SectionContent(
                "You agree to indemnify and hold harmless Patient Tracker App, its officers, employees, and affiliates from any claims, damages, losses, or expenses (including legal fees) arising from:\n\n" +
                "• Your use or misuse of the App\n" +
                "• Violation of these Terms and Conditions\n" +
                "• Infringement of any third-party rights\n" +
                "• Your interactions with other users"
            )

            SectionTitle("14. Termination")
            SectionContent(
                "We reserve the right to:\n\n" +
                "• Suspend or terminate your account at any time for violations\n" +
                "• Remove or modify content that violates these terms\n" +
                "• Discontinue the App or any features with notice\n\n" +
                "Upon termination:\n" +
                "• Your access to the App will cease immediately\n" +
                "• We may retain data as required by law\n" +
                "• Outstanding payment obligations remain enforceable"
            )

            SectionTitle("15. Dispute Resolution")
            SectionContent(
                "Any disputes arising from these Terms or use of the App shall be resolved through:\n\n" +
                "1. Good faith negotiations between parties\n" +
                "2. Mediation if negotiations fail\n" +
                "3. Binding arbitration under applicable arbitration rules\n\n" +
                "You waive the right to participate in class action lawsuits against Patient Tracker App."
            )

            SectionTitle("16. Governing Law")
            SectionContent(
                "These Terms and Conditions are governed by and construed in accordance with the laws of [Your Jurisdiction], without regard to its conflict of law provisions. You consent to the exclusive jurisdiction of courts in [Your Jurisdiction]."
            )

            SectionTitle("17. Changes to Terms")
            SectionContent(
                "We reserve the right to modify these Terms and Conditions at any time. Changes will be effective upon posting to the App. Your continued use after changes constitutes acceptance. We will notify users of material changes via email or in-app notification."
            )

            SectionTitle("18. Severability")
            SectionContent(
                "If any provision of these Terms is found to be invalid or unenforceable, the remaining provisions shall continue in full force and effect."
            )

            SectionTitle("19. Contact Information")
            SectionContent(
                "For questions about these Terms and Conditions, please contact us at:\n\n" +
                "Email: legal@patienttracker.com\n" +
                "Support: support@patienttracker.com\n" +
                "Address: Patient Tracker App Legal Department\n" +
                "Phone: +1 (555) 123-4567\n\n" +
                "Business Hours: Monday - Friday, 9:00 AM - 5:00 PM"
            )

            SectionTitle("20. Acknowledgment")
            SectionContent(
                "BY USING THE PATIENT TRACKER APP, YOU ACKNOWLEDGE THAT YOU HAVE READ, UNDERSTOOD, AND AGREE TO BE BOUND BY THESE TERMS AND CONDITIONS. IF YOU DO NOT AGREE, YOU MUST DISCONTINUE USE OF THE APP IMMEDIATELY."
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF2F2019),
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@Composable
private fun SubSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF2F2019),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionContent(content: String) {
    Text(
        text = content,
        fontSize = 14.sp,
        color = Color(0xFF6B5B54),
        lineHeight = 22.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
