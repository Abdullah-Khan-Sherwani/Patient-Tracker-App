package com.example.patienttracker.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Represents doctor's availability for a specific day of the week
 */
data class DoctorAvailability(
    val doctorUid: String = "",
    val dayOfWeek: Int = 1, // 1=Monday, 7=Sunday
    val isActive: Boolean = false,
    val startTime: String = "09:00", // Format: HH:mm
    val endTime: String = "17:00",   // Format: HH:mm
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): DoctorAvailability? {
            return try {
                DoctorAvailability(
                    doctorUid = doc.getString("doctorUid") ?: "",
                    dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: 1,
                    isActive = doc.getBoolean("isActive") ?: false,
                    startTime = doc.getString("startTime") ?: "09:00",
                    endTime = doc.getString("endTime") ?: "17:00",
                    createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                    updatedAt = doc.getTimestamp("updatedAt") ?: Timestamp.now()
                )
            } catch (e: Exception) {
                null
            }
        }

        fun getDayName(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                7 -> "Sunday"
                else -> "Unknown"
            }
        }

        fun getDayShortName(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                7 -> "Sun"
                else -> "N/A"
            }
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "doctorUid" to doctorUid,
            "dayOfWeek" to dayOfWeek,
            "isActive" to isActive,
            "startTime" to startTime,
            "endTime" to endTime,
            "createdAt" to createdAt,
            "updatedAt" to Timestamp.now()
        )
    }
}

/**
 * Predefined list of medical specializations
 */
object Specializations {
    val list = listOf(
        "General Physician",
        "Cardiologist",
        "Pediatrician",
        "Dermatologist",
        "Psychiatrist",
        "Orthopedic",
        "ENT Specialist",
        "Neurologist",
        "Gynecologist",
        "Dentist",
        "Urologist",
        "Oncologist",
        "Radiologist"
    )
}
