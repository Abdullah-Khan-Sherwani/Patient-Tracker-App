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
     * @param isPrivate Whether record is private (only patient can see)
     * @param notes Optional notes
     * @param pastMedication Optional past medication info
     * @param dependentId Optional dependent ID if uploading for a dependent
     * @param dependentName Optional dependent name if uploading for a dependent
     */
    suspend fun uploadRecord(
        fileUri: Uri,
        fileName: String,
        fileType: String,
        fileSize: Long,
        description: String,
        appointmentId: String? = null,
        tags: List<String> = emptyList(),
        isPrivate: Boolean = false,
        notes: String = "",
        pastMedication: String = "",
        dependentId: String = "",
        dependentName: String = ""
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
                dependentId = dependentId,
                dependentName = dependentName,
                fileName = fileName,
                fileUrl = fileUrl,
                fileType = fileType,
                fileSize = fileSize,
                description = description,
                uploadDate = Timestamp.now(),
                appointmentId = appointmentId,
                doctorAccessList = emptyList(),
                tags = tags,
                isPrivate = isPrivate,
                notes = notes,
                pastMedication = pastMedication,
                viewedBy = emptyList(),
                glassBreakAccess = emptyList()
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
     * Get all health records for the current patient (self, not dependents)
     */
    suspend fun getPatientRecords(): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .whereEqualTo("dependentId", "") // Only self records
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
     * Get health records for a specific dependent
     */
    suspend fun getDependentRecords(dependentId: String): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .whereEqualTo("dependentId", dependentId)
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
    
    /**
     * Record that a doctor viewed a record
     */
    suspend fun recordView(recordId: String, wasGlassBreak: Boolean = false): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get doctor name
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val doctorName = "Dr. $firstName $lastName"
            
            // Get current record
            val docRef = db.collection(COLLECTION).document(recordId)
            val docSnapshot = docRef.get().await()
            val record = HealthRecord.fromFirestore(docSnapshot.data ?: return Result.failure(Exception("Record not found")), recordId)
            
            // Add view log
            val viewLog = ViewLog(
                doctorUid = currentUser.uid,
                doctorName = doctorName,
                viewedAt = Timestamp.now(),
                wasGlassBreak = wasGlassBreak
            )
            
            val updatedViewedBy = record.viewedBy.toMutableList()
            updatedViewedBy.add(viewLog)
            
            docRef.update("viewedBy", updatedViewedBy.map { log ->
                hashMapOf(
                    "doctorUid" to log.doctorUid,
                    "doctorName" to log.doctorName,
                    "viewedAt" to log.viewedAt,
                    "wasGlassBreak" to log.wasGlassBreak
                )
            }).await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Glass break access - emergency access to private records
     */
    suspend fun glassBreakAccess(recordId: String, reason: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get doctor info
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val doctorName = "Dr. $firstName $lastName"
            
            // Get record
            val docRef = db.collection(COLLECTION).document(recordId)
            val docSnapshot = docRef.get().await()
            val record = HealthRecord.fromFirestore(docSnapshot.data ?: return Result.failure(Exception("Record not found")), recordId)
            
            // Create glass break log
            val glassBreakLog = GlassBreakLog(
                doctorUid = currentUser.uid,
                doctorName = doctorName,
                accessedAt = Timestamp.now(),
                reason = reason,
                notificationSent = true // Will be implemented with notification system
            )
            
            val updatedGlassBreak = record.glassBreakAccess.toMutableList()
            updatedGlassBreak.add(glassBreakLog)
            
            // Update record
            docRef.update("glassBreakAccess", updatedGlassBreak.map { log ->
                hashMapOf(
                    "doctorUid" to log.doctorUid,
                    "doctorName" to log.doctorName,
                    "accessedAt" to log.accessedAt,
                    "reason" to log.reason,
                    "notificationSent" to log.notificationSent
                )
            }).await()
            
            // Also record as a view
            recordView(recordId, wasGlassBreak = true)
            
            // TODO: Send notification to patient and admin
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get records accessible to current doctor for a patient
     * Respects privacy settings and appointment access
     */
    suspend fun getDoctorAccessibleRecordsForPatient(patientUid: String): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Check if doctor has active appointment with patient
            val hasAppointment = AppointmentRepository.hasActiveAppointment(currentUser.uid, patientUid).getOrNull() ?: false
            
            // Get all patient records
            val recordsSnapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", patientUid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val allRecords = recordsSnapshot.documents.mapNotNull { doc ->
                HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }
            
            // Filter out private records unless doctor has explicit access or appointment
            val accessibleRecords = allRecords.filter { record ->
                !record.isPrivate || 
                record.doctorAccessList.contains(currentUser.uid) ||
                hasAppointment
            }
            
            Result.success(accessibleRecords)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
