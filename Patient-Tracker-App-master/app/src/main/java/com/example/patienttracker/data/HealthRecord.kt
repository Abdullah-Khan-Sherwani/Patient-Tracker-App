package com.example.patienttracker.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

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
    val metadata: Map<String, String> = emptyMap() // Additional flexible data
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
                } ?: emptyMap()
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
            "metadata" to metadata
        )
    }
}
