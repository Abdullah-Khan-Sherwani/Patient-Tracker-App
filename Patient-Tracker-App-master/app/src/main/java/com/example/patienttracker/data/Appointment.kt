package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Appointment data model for Firebase
 * Updated to support time-slot based booking system
 */
@Parcelize
data class Appointment(
    val appointmentId: String = "",
    val appointmentNumber: String = "", // Kept for backward compatibility
    val patientUid: String = "",
    val patientName: String = "",
    val doctorUid: String = "",
    val doctorName: String = "",
    val speciality: String = "",
    val appointmentDate: Timestamp = Timestamp.now(),
    val timeSlot: String = "", // Legacy: block name or time range string
    // New time-slot booking fields
    val slotStartTime: String = "", // Format: "HH:mm" (24-hour) e.g., "09:00"
    val slotEndTime: String = "", // Format: "HH:mm" (24-hour) e.g., "09:20"
    val blockName: String = "", // Morning, Afternoon, Evening, Night
    val status: String = "scheduled", // scheduled, completed, cancelled
    val cancelledBy: String = "", // patient, doctor, or admin - who cancelled the appointment
    val recipientType: String = "self", // "self" or "dependent"
    val dependentId: String = "",
    val dependentName: String = "",
    val notes: String = "",
    val price: Int = 1500, // Fixed price in PKR
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) : Parcelable {
    
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any>, appointmentId: String): Appointment {
            return Appointment(
                appointmentId = appointmentId,
                appointmentNumber = data["appointmentNumber"] as? String ?: "",
                patientUid = data["patientUid"] as? String ?: "",
                patientName = data["patientName"] as? String ?: "",
                doctorUid = data["doctorUid"] as? String ?: "",
                doctorName = data["doctorName"] as? String ?: "",
                speciality = data["speciality"] as? String ?: "",
                appointmentDate = data["appointmentDate"] as? Timestamp ?: Timestamp.now(),
                timeSlot = data["timeSlot"] as? String ?: "",
                slotStartTime = data["slotStartTime"] as? String ?: "",
                slotEndTime = data["slotEndTime"] as? String ?: "",
                blockName = data["blockName"] as? String ?: "",
                status = data["status"] as? String ?: "scheduled",
                cancelledBy = data["cancelledBy"] as? String ?: "",
                recipientType = data["recipientType"] as? String ?: "self",
                dependentId = data["dependentId"] as? String ?: "",
                dependentName = data["dependentName"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                price = (data["price"] as? Long)?.toInt() ?: 1500,
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
    
    fun toFirestore(): Map<String, Any> {
        return hashMapOf(
            "appointmentNumber" to appointmentNumber,
            "patientUid" to patientUid,
            "patientName" to patientName,
            "doctorUid" to doctorUid,
            "doctorName" to doctorName,
            "speciality" to speciality,
            "appointmentDate" to appointmentDate,
            "timeSlot" to timeSlot,
            "slotStartTime" to slotStartTime,
            "slotEndTime" to slotEndTime,
            "blockName" to blockName,
            "recipientType" to recipientType,
            "dependentId" to dependentId,
            "dependentName" to dependentName,
            "status" to status,
            "cancelledBy" to cancelledBy,
            "notes" to notes,
            "price" to price,
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
     * Get formatted time string - prefers slot times over legacy timeSlot
     */
    fun getFormattedTime(): String {
        // If we have slot times, format them nicely
        if (slotStartTime.isNotBlank() && slotEndTime.isNotBlank()) {
            return formatSlotTime(slotStartTime, slotEndTime)
        }
        // Fallback to legacy timeSlot
        return timeSlot
    }
    
    /**
     * Get display time in format like "3:20 PM – 3:40 PM"
     */
    fun getDisplaySlotTime(): String {
        if (slotStartTime.isNotBlank() && slotEndTime.isNotBlank()) {
            return formatSlotTime(slotStartTime, slotEndTime)
        }
        // Fallback: try to parse timeSlot if it looks like a time range
        if (timeSlot.contains("-") || timeSlot.contains("–")) {
            return timeSlot
        }
        return timeSlot
    }
    
    /**
     * Format slot times from 24-hour to 12-hour AM/PM format
     */
    private fun formatSlotTime(start: String, end: String): String {
        return try {
            val formatter24 = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            val formatter12 = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
            val startTime = java.time.LocalTime.parse(start, formatter24)
            val endTime = java.time.LocalTime.parse(end, formatter24)
            "${startTime.format(formatter12)} – ${endTime.format(formatter12)}"
        } catch (e: Exception) {
            "$start – $end"
        }
    }
    
    /**
     * Format doctor name ensuring "Dr." prefix appears only once
     * Handles cases where doctorName already contains "Dr." prefix
     */
    fun formatDoctorName(): String {
        return if (doctorName.startsWith("Dr.", ignoreCase = true) || 
                   doctorName.startsWith("Dr ", ignoreCase = true)) {
            doctorName
        } else {
            "Dr. $doctorName"
        }
    }
}
