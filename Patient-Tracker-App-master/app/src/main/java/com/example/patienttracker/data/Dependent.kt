package com.example.patienttracker.data

import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class Dependent(
    val dependentId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "", // ISO date yyyy-MM-dd
    val gender: String = "",
    val relationship: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    companion object {
        fun fromMap(id: String, data: Map<String, Any>?): Dependent? {
            if (data == null) return null
            return Dependent(
                dependentId = id,
                firstName = data["firstName"] as? String ?: "",
                lastName = data["lastName"] as? String ?: "",
                dob = data["dob"] as? String ?: "",
                gender = data["gender"] as? String ?: "",
                relationship = data["relationship"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "dob" to dob,
            "gender" to gender,
            "relationship" to relationship,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun getFullName(): String {
        return listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    }
}
