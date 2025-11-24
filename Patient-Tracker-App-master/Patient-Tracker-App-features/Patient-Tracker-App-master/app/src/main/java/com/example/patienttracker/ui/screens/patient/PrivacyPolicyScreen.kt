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

private val BackgroundColor = Color(0xFFF7EFE7)
private val CardWhite = Color(0xFFFFFFFF)
private val HeaderColor = Color(0xFFD4AF8C)
private val TextColor = Color(0xFF333333)
private val SubTextColor = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
                    PolicySection(
                        title = "Section 1 — Data Usage",
                        content = "Medify collects personal data such as name, phone number, appointment history and medical reports solely for providing app services."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PolicySection(
                        title = "Section 2 — Data Security",
                        content = "Medical data is encrypted and protected. Access is restricted to the user and their assigned healthcare providers."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PolicySection(
                        title = "Section 3 — Sharing of Information",
                        content = "Data is never shared with third parties except:\n   • healthcare professionals involved in care\n   • if required by law enforcement or safety regulations"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PolicySection(
                        title = "Section 4 — Messages & Chat",
                        content = "Conversations between doctor and patient may be stored to support continuity of care."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PolicySection(
                        title = "Section 5 — User Rights",
                        content = "Users may request deletion of their profile and medical data at any time."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PolicySection(
                        title = "Section 6 — Cookies / Analytics",
                        content = "Medify may use anonymized analytics to improve performance, without identifying users personally."
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
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
