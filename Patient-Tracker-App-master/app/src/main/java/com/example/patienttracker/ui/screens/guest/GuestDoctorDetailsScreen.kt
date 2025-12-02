package com.example.patienttracker.ui.screens.guest

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.patienttracker.data.DoctorAvailability
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Deep Teal & Mint Design System
private val BgColor = Color(0xFFF0F5F4)           // Dim background
private val CardColor = Color(0xFFFFFFFF)         // Card surface
private val AccentColor = Color(0xFF0E4944)       // Deep Teal
private val TextPrimary = Color(0xFF1F2937)       // Dark charcoal
private val TextSecondary = Color(0xFF4B5563)     // Medium gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestDoctorDetailsScreen(
    navController: NavController,
    context: Context,
    doctorUid: String
) {
    var doctorName by remember { mutableStateOf("Doctor") }
    var specialization by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var timings by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf<List<DoctorAvailability>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showLoginDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(doctorUid) {
        scope.launch {
            try {
                val db = Firebase.firestore
                
                // Load doctor info - only non-personal details for guests
                val doctorDoc = db.collection("users").document(doctorUid).get().await()
                val firstName = doctorDoc.getString("firstName") ?: ""
                val lastName = doctorDoc.getString("lastName") ?: ""
                doctorName = "Dr. $firstName $lastName".trim()
                specialization = doctorDoc.getString("speciality") ?: "General Physician"
                // Note: email and phone are NOT loaded for guest view - privacy protection
                days = doctorDoc.getString("days") ?: ""
                timings = doctorDoc.getString("timings") ?: ""
                
                // Load availability
                val availSnapshot = db.collection("doctor_availability")
                    .whereEqualTo("doctorUid", doctorUid)
                    .get()
                    .await()
                
                availability = availSnapshot.documents.mapNotNull { doc ->
                    DoctorAvailability.fromFirestore(doc)
                }.sortedBy { it.dayOfWeek }
                
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Details", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Doctor Profile Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardColor,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(AccentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = doctorName.split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .take(2)
                                    .joinToString(""),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = doctorName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = specialization,
                            fontSize = 16.sp,
                            color = AccentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Availability Schedule
                if (days.isNotEmpty() || timings.isNotEmpty()) {
                    Text(
                        text = "Availability",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AccentColor.copy(alpha = 0.15f),
                        shadowElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (days.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = AccentColor
                                    )
                                    Text(
                                        text = "Available Days",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = days,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    lineHeight = 22.sp
                                )
                            }

                            if (timings.isNotEmpty()) {
                                if (days.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = AccentColor
                                    )
                                    Text(
                                        text = "Consultation Timings",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = timings,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // Consultation Fee
                Text(
                    text = "Consultation Fee",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardColor,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Fee per consultation",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PKR 1500",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Booking CTA (Disabled for Guest)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AccentColor.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentColor,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Login or create an account to continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Create an account to book appointments with $doctorName",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                navController.navigate("unified_login") {
                                    popUpTo("guest_home") { inclusive = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Login / Sign Up",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
