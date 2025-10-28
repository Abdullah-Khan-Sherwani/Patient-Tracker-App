package dev.atick.data.model.profile

data class AppUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String,      // PATIENT | DOCTOR | ADMIN
    val approved: Boolean  // true if approvedAt != null
)
