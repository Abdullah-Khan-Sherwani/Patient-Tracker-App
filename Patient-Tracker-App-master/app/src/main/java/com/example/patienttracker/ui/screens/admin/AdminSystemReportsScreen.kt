package com.example.patienttracker.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Teal/Mint Theme Colors
private val BgColor = Color(0xFFF4F6F7)        // Light gray background
private val CardColor = Color(0xFFFFFFFF)      // White cards
private val AccentColor = Color(0xFF04786A)    // Teal accent
private val TextPrimary = Color(0xFF0E4944)    // Deep teal text
private val TextSecondary = Color(0xFF6B7280)  // Gray secondary text
private val MintAccent = Color(0xFF76DCB0)     // Mint green accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSystemReportsScreen(navController: NavController, context: Context) {
    var totalPatients by remember { mutableStateOf(0) }
    var totalDoctors by remember { mutableStateOf(0) }
    var totalAppointments by remember { mutableStateOf(0) }
    var completedAppointments by remember { mutableStateOf(0) }
    var cancelledAppointments by remember { mutableStateOf(0) }
    var upcomingAppointments by remember { mutableStateOf(0) }
    var monthlyData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val db = Firebase.firestore
                
                // Get total patients
                val patientsSnapshot = db.collection("users")
                    .whereEqualTo("role", "patient")
                    .get()
                    .await()
                totalPatients = patientsSnapshot.size()
                
                // Get total doctors
                val doctorsSnapshot = db.collection("users")
                    .whereEqualTo("role", "doctor")
                    .get()
                    .await()
                totalDoctors = doctorsSnapshot.size()
                
                // Get all appointments
                val appointmentsSnapshot = db.collection("appointments")
                    .get()
                    .await()
                
                totalAppointments = appointmentsSnapshot.size()
                
                val now = Date()
                var completed = 0
                var cancelled = 0
                var upcoming = 0
                val monthlyCount = mutableMapOf<String, Int>()
                
                appointmentsSnapshot.documents.forEach { doc ->
                    try {
                        val status = doc.getString("status") ?: "scheduled"
                        val appointmentDate = doc.getTimestamp("appointmentDate")?.toDate()
                        
                        // Count by status
                        when {
                            status.contains("completed", ignoreCase = true) -> completed++
                            status.contains("cancelled", ignoreCase = true) -> cancelled++
                            appointmentDate != null && appointmentDate.after(now) -> upcoming++
                        }
                        
                        // Count by month for chart
                        appointmentDate?.let {
                            val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            val monthKey = monthFormat.format(it)
                            monthlyCount[monthKey] = (monthlyCount[monthKey] ?: 0) + 1
                        }
                    } catch (e: Exception) {
                        // Skip invalid entries
                    }
                }
                
                completedAppointments = completed
                cancelledAppointments = cancelled
                upcomingAppointments = upcoming
                monthlyData = monthlyCount.toList()
                    .sortedBy { SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(it.first) }
                    .takeLast(6)
                    .toMap()
                
            } catch (e: Exception) {
                // Handle error - show zeros
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Reports", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AccentColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                // Metrics Grid
                Text(
                    text = "Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // First Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.Person,
                        title = "Total Patients",
                        value = totalPatients.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.Person,
                        title = "Total Doctors",
                        value = totalDoctors.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Second Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.DateRange,
                        title = "Total Appointments",
                        value = totalAppointments.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Completed",
                        value = completedAppointments.toString(),
                        modifier = Modifier.weight(1f),
                        iconColor = Color(0xFF10B981)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Third Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.Close,
                        title = "Cancelled",
                        value = cancelledAppointments.toString(),
                        modifier = Modifier.weight(1f),
                        iconColor = Color(0xFFEF4444)
                    )
                    MetricCard(
                        icon = Icons.Default.Notifications,
                        title = "Upcoming",
                        value = upcomingAppointments.toString(),
                        modifier = Modifier.weight(1f),
                        iconColor = Color(0xFF3B82F6)
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Chart Section
                Text(
                    text = "Appointment Trends",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                SimpleBarChart(
                    data = monthlyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    iconColor: Color = AccentColor
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SimpleBarChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        shadowElevation = 2.dp
    ) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data available",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val maxValue = data.values.maxOrNull() ?: 1
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { (month, count) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Value label
                            Text(
                                text = count.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            // Bar
                            val heightFraction = if (maxValue > 0) count.toFloat() / maxValue else 0f
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight(heightFraction.coerceIn(0.1f, 1f))
                                    .background(
                                        color = AccentColor,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Month labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.keys.forEach { month ->
                        Text(
                            text = month.split(" ")[0],
                            fontSize = 10.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
