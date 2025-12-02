package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Doctor's Note data model
 * Contains prescription and comments written by doctor after an appointment
 */
@Parcelize
data class DoctorNote(
    val noteId: String = "",
    val appointmentId: String = "",
    val patientUid: String = "",
    val patientName: String = "",
    val doctorUid: String = "",
    val doctorName: String = "",
    val speciality: String = "",
    val comments: String = "", // Doctor's comments/diagnosis
    val prescription: String = "", // Prescribed medications
    val appointmentDate: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any>, noteId: String): DoctorNote {
            return DoctorNote(
                noteId = noteId,
                appointmentId = data["appointmentId"] as? String ?: "",
                patientUid = data["patientUid"] as? String ?: "",
                patientName = data["patientName"] as? String ?: "",
                doctorUid = data["doctorUid"] as? String ?: "",
                doctorName = data["doctorName"] as? String ?: "",
                speciality = data["speciality"] as? String ?: "",
                comments = data["comments"] as? String ?: "",
                prescription = data["prescription"] as? String ?: "",
                appointmentDate = data["appointmentDate"] as? Timestamp ?: Timestamp.now(),
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
    
    fun toFirestore(): Map<String, Any> {
        return hashMapOf(
            "appointmentId" to appointmentId,
            "patientUid" to patientUid,
            "patientName" to patientName,
            "doctorUid" to doctorUid,
            "doctorName" to doctorName,
            "speciality" to speciality,
            "comments" to comments,
            "prescription" to prescription,
            "appointmentDate" to appointmentDate,
            "createdAt" to createdAt
        )
    }
    
    /**
     * Get formatted date for display
     */
    fun getFormattedDate(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            sdf.format(appointmentDate.toDate())
        } catch (e: Exception) {
            "Unknown date"
        }
    }
    
    /**
     * Get formatted doctor name (avoids "Dr. Dr." duplication)
     */
    fun getFormattedDoctorName(): String {
        return if (doctorName.startsWith("Dr.", ignoreCase = true) || 
                   doctorName.startsWith("Dr ", ignoreCase = true)) {
            doctorName
        } else {
            "Dr. $doctorName"
        }
    }
}
