package dev.atick.firebase.auth.model

import kotlinx.serialization.Serializable
import com.google.firebase.Timestamp

@Serializable
data class FirebaseAppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "PATIENT",   // PATIENT | DOCTOR | ADMIN
    val createdAt: Timestamp? = null,
    val approvedAt: Timestamp? = null,
)