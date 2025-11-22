package com.example.patienttracker.ui.screens.patient

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.example.patienttracker.ui.screens.patient.BookAppointmentScreen
import com.example.patienttracker.ui.screens.patient.FullScheduleScreen
import android.os.Parcelable
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize


@Parcelize
data class DoctorFull(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val speciality: String = "",
    val days: String = "",
    val timings: String = ""
) : Parcelable

/**
 * Doctor List Screen - Search Specialists
 * 
 * THEME FIX: Now uses MaterialTheme.colorScheme for proper dark mode support
 * - Primary colors for gradient header
 * - Surface/background colors for cards and list background
 * - All colors now respect system/app theme setting
 */
@Composable
fun DoctorListScreen(navController: NavController, context: Context, specialityFilter: String?) {
    // THEME FIX: Use MaterialTheme colors instead of hardcoded values
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val gradient = Brush.verticalGradient(listOf(primaryColor, primaryContainer))

    var doctors by remember { mutableStateOf<List<DoctorFull>>(emptyList()) }

    LaunchedEffect(Unit) {
        doctors = fetchDoctorsFromFirestore()
    }

    val filtered = remember(specialityFilter, doctors) {
        if (specialityFilter.isNullOrBlank() || specialityFilter == "All")
            doctors
        else doctors.filter { it.speciality.equals(specialityFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = specialityFilter?.ifBlank { "All Doctors" } ?: "All Doctors",
                        // THEME FIX: Use onPrimary for text on primary background
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                // THEME FIX: Use background color from theme
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filtered) { doc ->
                DoctorCard(doc) {
                    // Navigate to simplified booking with doctor info
                    val doctorFullName = "Dr. ${doc.firstName} ${doc.lastName}"
                    navController.navigate("book_appointment_simple/${doc.id}/${doctorFullName}/${doc.speciality}")
                }
            }
        }
    }
}

/**
 * Doctor Card Component
 * 
 * THEME FIX: Now uses MaterialTheme.colorScheme for all colors
 * - surface/surfaceVariant for card background
 * - onSurface/onSurfaceVariant for text
 * - primary for buttons
 * Properly supports dark mode
 */
@Composable
fun DoctorCard(doctor: DoctorFull, onBookClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        // THEME FIX: Use surface color from theme
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Dr. ${doctor.firstName} ${doctor.lastName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                // THEME FIX: Use onSurface color
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = doctor.speciality,
                // THEME FIX: Use primary color for specialty
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Days: ${doctor.days}", 
                // THEME FIX: Use onSurfaceVariant for secondary text
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Timings: ${doctor.timings}", 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            // Price badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                // THEME FIX: Use secondaryContainer for badge
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = "Rs. 1,500",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    // THEME FIX: Use onSecondaryContainer
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onBookClick,
                // THEME FIX: Use primary color from theme
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    "Book Appointment", 
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

        }
    }
}

suspend fun fetchDoctorsFromFirestore(): List<DoctorFull> {
    val db = Firebase.firestore
    return try {
        val querySnapshot = db.collection("users").whereEqualTo("role", "doctor").get().await()
        println("fetchDoctorsFromFirestore: Found ${querySnapshot.documents.size} doctor documents")
        querySnapshot.documents.mapNotNull { doc ->
            val firstName = doc.getString("firstName") ?: ""
            val lastName = doc.getString("lastName") ?: ""
            println("fetchDoctorsFromFirestore: Doctor ${doc.id} - Name: '$firstName $lastName'")
            
            // Only include doctors with at least a first name or last name
            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                DoctorFull(
                    id = doc.id,
                    firstName = firstName,
                    lastName = lastName,
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    speciality = doc.getString("speciality") ?: "General Physician",
                    days = doc.getString("days") ?: "",
                    timings = doc.getString("timings") ?: ""
                )
            } else {
                println("fetchDoctorsFromFirestore: Skipping doctor ${doc.id} - empty name")
                null
            }
        }
    } catch (e: Exception) {
        println("fetchDoctorsFromFirestore: Error - ${e.message}")
        e.printStackTrace()
        emptyList()
    }
}
