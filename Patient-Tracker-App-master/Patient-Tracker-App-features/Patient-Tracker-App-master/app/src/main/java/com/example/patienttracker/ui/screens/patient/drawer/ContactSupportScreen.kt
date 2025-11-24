package com.example.patienttracker.ui.screens.patient.drawer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

private val BackgroundColor = Color(0xFFF5F1ED)
private val HeaderColor = Color(0xFFD4AF8C)
private val CardWhite = Color(0xFFFFFFFF)
private val TextColor = Color(0xFF333333)
private val SubTextColor = Color(0xFF666666)
private val ButtonColor = Color(0xFFC9956E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMessageForm by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Support") },
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
            if (!showMessageForm) {
                // Contact Information
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
                            text = "Get in Touch",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        ContactInfoRow(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = "support@medify.com"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ContactInfoRow(
                            icon = Icons.Default.Phone,
                            label = "Phone",
                            value = "+92-3XX-XXXXXXX"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = { showMessageForm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Message to Support",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Message Form
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
                            text = "Send Support Request",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonColor,
                                unfocusedBorderColor = SubTextColor.copy(alpha = 0.5f),
                                focusedLabelColor = ButtonColor,
                                cursorColor = ButtonColor
                            ),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ButtonColor,
                                unfocusedBorderColor = SubTextColor.copy(alpha = 0.5f),
                                focusedLabelColor = ButtonColor,
                                cursorColor = ButtonColor
                            ),
                            maxLines = 6
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showMessageForm = false
                                    subject = ""
                                    message = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ButtonColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    if (subject.isBlank() || message.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Please fill in all fields",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                // Simulate sending message
                                                kotlinx.coroutines.delay(1000)
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Support request sent. We will contact you soon.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                showMessageForm = false
                                                subject = ""
                                                message = ""
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Could not send message. Please check your connection.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Send")
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ButtonColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ButtonColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                color = SubTextColor
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextColor
            )
        }
    }
}
