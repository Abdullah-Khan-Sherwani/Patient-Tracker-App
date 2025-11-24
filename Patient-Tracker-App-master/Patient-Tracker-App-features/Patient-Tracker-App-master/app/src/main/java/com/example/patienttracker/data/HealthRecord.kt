package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

/**
 * Tracks when and by whom a record was viewed
 */
@Parcelize
data class ViewLog(
    val doctorUid: String = "",
    val doctorName: String = "",
    val viewedAt: Timestamp = Timestamp.now(),
    val wasGlassBreak: Boolean = false
) : Parcelable

/**
 * Tracks emergency glass break access
 */
@Parcelize
data class GlassBreakLog(
    val doctorUid: String = "",
    val doctorName: String = "",
    val accessedAt: Timestamp = Timestamp.now(),
    val reason: String = "",
    val notificationSent: Boolean = false
) : Parcelable

/**
 * Health Record data model
 * Represents a patient's uploaded health document (lab report, prescription, medical image, etc.)
 */
@Parcelize
data class HealthRecord(
    val recordId: String = "",
    val patientUid: String = "",
    val patientName: String = "",
    val fileName: String = "",
    val fileUrl: String = "",
    val fileType: String = "", // "image/jpeg", "image/png", "application/pdf", etc.
    val fileSize: Long = 0L, // in bytes
    val description: String = "",
    val uploadDate: Timestamp = Timestamp.now(),
    val appointmentId: String? = null, // Optional: link to specific appointment
    val doctorAccessList: List<String> = emptyList(), // List of doctor UIDs who can access this record
    val tags: List<String> = emptyList(), // e.g., "lab_report", "prescription", "xray"
    val metadata: Map<String, String> = emptyMap(), // Additional flexible data
    val isPrivate: Boolean = false, // Private records only visible to patient
    val notes: String = "", // Optional notes from patient
    val pastMedication: String = "", // Optional past medication information
    val viewedBy: List<ViewLog> = emptyList(), // Track which doctors viewed this record
    val glassBreakAccess: List<GlassBreakLog> = emptyList() // Track emergency access
) : Parcelable {
    
    /**
     * Get file extension from fileName
     */
    fun getFileExtension(): String {
        return fileName.substringAfterLast('.', "")
    }
    
    /**
     * Check if file is an image
     */
    fun isImage(): Boolean {
        return fileType.startsWith("image/") ||
                getFileExtension().lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }
    
    /**
     * Check if file is a PDF
     */
    fun isPdf(): Boolean {
        return fileType == "application/pdf" ||
                getFileExtension().lowercase() == "pdf"
    }
    
    /**
     * Get human-readable file size
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }
    }
    
    /**
     * Convert Firestore document to HealthRecord
     */
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestore(data: Map<String, Any>, recordId: String): HealthRecord {
            // Parse viewedBy list
            val viewedByList = (data["viewedBy"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { map ->
                    ViewLog(
                        doctorUid = map["doctorUid"] as? String ?: "",
                        doctorName = map["doctorName"] as? String ?: "",
                        viewedAt = map["viewedAt"] as? Timestamp ?: Timestamp.now(),
                        wasGlassBreak = map["wasGlassBreak"] as? Boolean ?: false
                    )
                }
            } ?: emptyList()
            
            // Parse glassBreakAccess list
            val glassBreakList = (data["glassBreakAccess"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { map ->
                    GlassBreakLog(
                        doctorUid = map["doctorUid"] as? String ?: "",
                        doctorName = map["doctorName"] as? String ?: "",
                        accessedAt = map["accessedAt"] as? Timestamp ?: Timestamp.now(),
                        reason = map["reason"] as? String ?: "",
                        notificationSent = map["notificationSent"] as? Boolean ?: false
                    )
                }
            } ?: emptyList()
            
            return HealthRecord(
                recordId = recordId,
                patientUid = data["patientUid"] as? String ?: "",
                patientName = data["patientName"] as? String ?: "",
                fileName = data["fileName"] as? String ?: "",
                fileUrl = data["fileUrl"] as? String ?: "",
                fileType = data["fileType"] as? String ?: "",
                fileSize = (data["fileSize"] as? Number)?.toLong() ?: 0L,
                description = data["description"] as? String ?: "",
                uploadDate = data["uploadDate"] as? Timestamp ?: Timestamp.now(),
                appointmentId = data["appointmentId"] as? String,
                doctorAccessList = (data["doctorAccessList"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                metadata = (data["metadata"] as? Map<*, *>)?.entries?.associate { 
                    (it.key as? String ?: "") to (it.value as? String ?: "") 
                } ?: emptyMap(),
                isPrivate = data["isPrivate"] as? Boolean ?: false,
                notes = data["notes"] as? String ?: "",
                pastMedication = data["pastMedication"] as? String ?: "",
                viewedBy = viewedByList,
                glassBreakAccess = glassBreakList
            )
        }
    }
    
    /**
     * Convert to Firestore document
     */
    fun toFirestore(): Map<String, Any> {
        return hashMapOf(
            "patientUid" to patientUid,
            "patientName" to patientName,
            "fileName" to fileName,
            "fileUrl" to fileUrl,
            "fileType" to fileType,
            "fileSize" to fileSize,
            "description" to description,
            "uploadDate" to uploadDate,
            "appointmentId" to (appointmentId ?: ""),
            "doctorAccessList" to doctorAccessList,
            "tags" to tags,
            "metadata" to metadata,
            "isPrivate" to isPrivate,
            "notes" to notes,
            "pastMedication" to pastMedication,
            "viewedBy" to viewedBy.map { log ->
                hashMapOf(
                    "doctorUid" to log.doctorUid,
                    "doctorName" to log.doctorName,
                    "viewedAt" to log.viewedAt,
                    "wasGlassBreak" to log.wasGlassBreak
                )
            },
            "glassBreakAccess" to glassBreakAccess.map { log ->
                hashMapOf(
                    "doctorUid" to log.doctorUid,
                    "doctorName" to log.doctorName,
                    "accessedAt" to log.accessedAt,
                    "reason" to log.reason,
                    "notificationSent" to log.notificationSent
                )
            }
        )
    }
}
