package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Appointment data model for Firebase
 */
@Parcelize
data class Appointment(
    val appointmentId: String = "",
    val patientUid: String = "",
    val patientName: String = "",
    val doctorUid: String = "",
    val doctorName: String = "",
    val speciality: String = "",
    val appointmentDate: Timestamp = Timestamp.now(),
    val timeSlot: String = "", // e.g., "09:00 AM - 10:00 AM"
    val status: String = "scheduled", // scheduled, completed, cancelled
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any>, appointmentId: String): Appointment {
            return Appointment(
                appointmentId = appointmentId,
                patientUid = data["patientUid"] as? String ?: "",
                patientName = data["patientName"] as? String ?: "",
                doctorUid = data["doctorUid"] as? String ?: "",
                doctorName = data["doctorName"] as? String ?: "",
                speciality = data["speciality"] as? String ?: "",
                appointmentDate = data["appointmentDate"] as? Timestamp ?: Timestamp.now(),
                timeSlot = data["timeSlot"] as? String ?: "",
                status = data["status"] as? String ?: "scheduled",
                notes = data["notes"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
    
    fun toFirestore(): Map<String, Any> {
        return hashMapOf(
            "patientUid" to patientUid,
            "patientName" to patientName,
            "doctorUid" to doctorUid,
            "doctorName" to doctorName,
            "speciality" to speciality,
            "appointmentDate" to appointmentDate,
            "timeSlot" to timeSlot,
            "status" to status,
            "notes" to notes,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }
    
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        val date = appointmentDate.toDate()
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Get formatted time string
     */
    fun getFormattedTime(): String {
        return timeSlot
    }
}
