package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Notification data model for Firebase
 */
@Parcelize
data class Notification(
    val notificationId: String = "",
    val patientUid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "appointment_created", "appointment_cancelled", "appointment_updated"
    val appointmentId: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any>, notificationId: String): Notification {
            return Notification(
                notificationId = notificationId,
                patientUid = data["patientUid"] as? String ?: "",
                title = data["title"] as? String ?: "",
                message = data["message"] as? String ?: "",
                type = data["type"] as? String ?: "",
                appointmentId = data["appointmentId"] as? String ?: "",
                isRead = data["isRead"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
    
    fun toFirestore(): Map<String, Any> {
        return hashMapOf(
            "patientUid" to patientUid,
            "title" to title,
            "message" to message,
            "type" to type,
            "appointmentId" to appointmentId,
            "isRead" to isRead,
            "createdAt" to createdAt
        )
    }
    
    fun getFormattedDate(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.ENGLISH)
            sdf.format(createdAt.toDate())
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
