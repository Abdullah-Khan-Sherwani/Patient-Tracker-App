package dev.atick.firebase.firestore.model

import kotlinx.serialization.Serializable
import com.google.firebase.Timestamp

@Serializable
data class FirebaseDoctorRequest(
    val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val specialization: String = "",
    val licenseNumber: String = "",
    val status: String = "PENDING", // PENDING | APPROVED | REJECTED
    val createdAt: Long = 0L,
    val decidedAt: Long = 0L,
)