package dev.atick.firebase.firestore.model

import kotlinx.serialization.Serializable
import com.google.firebase.Timestamp

@Serializable
data class FirebaseAppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "PATIENT",   // PATIENT | DOCTOR | ADMIN
    val createdAt: Long = 0L,
    val approvedAt: Long = 0L, // Null until approved as Doctor
)