package dev.atick.data.model.profile

data class DoctorRequest(
    val id: String,
    val uid: String,
    val fullName: String,
    val specialization: String,
    val licenseNumber: String,
    val status: String     // PENDING | APPROVED | REJECTED
)
