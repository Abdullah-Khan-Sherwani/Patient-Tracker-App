package com.example.patienttracker.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Health Record Firestore operations
 * Handles CRUD operations for patient health records
 */
object HealthRecordRepository {
    
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private const val COLLECTION = "healthRecords"
    
    /**
     * Upload a new health record (file + metadata)
     * @param fileUri Local file URI
     * @param fileName Original file name
     * @param fileType MIME type
     * @param fileSize File size in bytes
     * @param description User-provided description
     * @param appointmentId Optional appointment ID
     * @param tags Optional tags
     */
    suspend fun uploadRecord(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        fileSize: Long,
        description: String,
        appointmentId: String? = null,
        tags: List<String> = emptyList()
    ): Result<HealthRecord> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // 1) Upload file to Storage
            val uploadResult = StorageRepository.uploadHealthRecord(
                fileUri, currentUser.uid, fileName
            )
            
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull()!!)
            }
            
            val fileUrl = uploadResult.getOrNull()!!
            
            // 2) Get patient name
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val patientName = "$firstName $lastName"
            
            // 3) Create HealthRecord document
            val recordId = UUID.randomUUID().toString()
            val healthRecord = HealthRecord(
                recordId = recordId,
                patientUid = currentUser.uid,
                patientName = patientName,
                fileName = fileName,
                fileUrl = fileUrl,
                fileType = fileType,
                fileSize = fileSize,
                description = description,
                uploadDate = Timestamp.now(),
                appointmentId = appointmentId,
                doctorAccessList = emptyList(),
                tags = tags
            )
            
            // 4) Save to Firestore
            db.collection(COLLECTION)
                .document(recordId)
                .set(healthRecord.toFirestore())
                .await()
            
            Result.success(healthRecord)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all health records for the current patient
     */
    suspend fun getPatientRecords(): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val records = snapshot.documents.mapNotNull { doc ->
                HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }
            
            Result.success(records)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get health records accessible to a doctor for a specific patient
     * Enforces 2-day window based on appointments
     */
    suspend fun getDoctorAccessibleRecords(patientUid: String): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get doctor's appointments with this patient
            val appointmentsSnapshot = db.collection("appointments")
                .whereEqualTo("doctorUid", currentUser.uid)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .await()
            
            if (appointmentsSnapshot.isEmpty) {
                return Result.success(emptyList())
            }
            
            // Check if any appointment is within 2-day window
            val now = System.currentTimeMillis()
            val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
            
            val hasActiveAccess = appointmentsSnapshot.documents.any { apptDoc ->
                val appointmentDate = apptDoc.getTimestamp("appointmentDate")
                if (appointmentDate != null) {
                    val appointmentMillis = appointmentDate.toDate().time
                    val diff = now - appointmentMillis
                    // Access granted on appointment day and 2 days after
                    diff >= 0 && diff <= twoDaysInMillis
                } else {
                    false
                }
            }
            
            if (!hasActiveAccess) {
                return Result.success(emptyList())
            }
            
            // Fetch patient's health records
            val recordsSnapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", patientUid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val records = recordsSnapshot.documents.mapNotNull { doc ->
                HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }
            
            Result.success(records)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a health record (file + metadata)
     */
    suspend fun deleteRecord(recordId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // 1) Get record to get file URL
            val docSnapshot = db.collection(COLLECTION).document(recordId).get().await()
            val record = HealthRecord.fromFirestore(docSnapshot.data ?: return Result.failure(Exception("Record not found")), recordId)
            
            // 2) Verify ownership
            if (record.patientUid != currentUser.uid) {
                return Result.failure(Exception("Unauthorized"))
            }
            
            // 3) Delete file from Storage
            StorageRepository.deleteHealthRecord(record.fileUrl)
            
            // 4) Delete Firestore document
            db.collection(COLLECTION).document(recordId).delete().await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Share a record with a specific doctor (add to doctorAccessList)
     */
    suspend fun shareRecordWithDoctor(recordId: String, doctorUid: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val docRef = db.collection(COLLECTION).document(recordId)
            val docSnapshot = docRef.get().await()
            val record = HealthRecord.fromFirestore(docSnapshot.data ?: return Result.failure(Exception("Record not found")), recordId)
            
            // Verify ownership
            if (record.patientUid != currentUser.uid) {
                return Result.failure(Exception("Unauthorized"))
            }
            
            // Add doctor to access list
            val updatedList = record.doctorAccessList.toMutableList()
            if (!updatedList.contains(doctorUid)) {
                updatedList.add(doctorUid)
            }
            
            docRef.update("doctorAccessList", updatedList).await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
