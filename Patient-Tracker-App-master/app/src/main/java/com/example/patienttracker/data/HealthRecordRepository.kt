package com.example.patienttracker.data

import android.content.Context
import android.net.Uri
import com.example.patienttracker.data.supabase.SupabaseStorageRepository
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
 * Files are stored in Supabase Storage, metadata in Firestore
 */
object HealthRecordRepository {
    
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private const val COLLECTION = "healthRecords"
    
    /**
     * Upload a new health record (file to Supabase + metadata to Firestore)
     * Falls back to Firebase Storage if Supabase fails
     * @param fileUri Local file URI
     * @param fileName Original file name
     * @param fileType MIME type
     * @param fileSize File size in bytes
     * @param description User-provided description
     * @param context Android context for Supabase upload
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
        context: Context,
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
            
            // Try Supabase first, fall back to Firebase Storage
            val fileUrl: String
            val uploadResult = SupabaseStorageRepository.uploadRecord(
                patientId = currentUser.uid,
                dependentId = dependentId.ifBlank { null },
                fileUri = fileUri,
                context = context
            )
            
            if (uploadResult.isSuccess) {
                fileUrl = uploadResult.getOrNull()!!
                android.util.Log.d("HealthRecordRepo", "Supabase upload succeeded: $fileUrl")
            } else {
                // Fall back to Firebase Storage
                android.util.Log.w("HealthRecordRepo", "Supabase failed, falling back to Firebase: ${uploadResult.exceptionOrNull()?.message}")
                val firebaseResult = StorageRepository.uploadHealthRecord(
                    fileUri, currentUser.uid, fileName
                )
                if (firebaseResult.isFailure) {
                    return Result.failure(firebaseResult.exceptionOrNull()!!)
                }
                fileUrl = firebaseResult.getOrNull()!!
                android.util.Log.d("HealthRecordRepo", "Firebase upload succeeded: $fileUrl")
            }
            
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
     * Legacy upload method without context - uses Firebase Storage (deprecated)
     * Kept for backward compatibility, prefer using the new uploadRecord with context
     */
    @Deprecated("Use uploadRecord with context parameter for Supabase Storage")
    suspend fun uploadRecordLegacy(
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
            
            // 1) Upload file to Firebase Storage (legacy)
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
     * Get all health records for the current patient (including dependents)
     */
    suspend fun getPatientRecords(): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            android.util.Log.d("HealthRecordRepo", "Fetching records for patient: ${currentUser.uid}")
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .get()
                .await()
            
            android.util.Log.d("HealthRecordRepo", "Found ${snapshot.size()} records")
            
            val records = snapshot.documents.mapNotNull { doc ->
                try {
                    HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("HealthRecordRepo", "Error parsing record ${doc.id}: ${e.message}")
                    null
                }
            }.sortedByDescending { it.uploadDate.seconds }
            
            android.util.Log.d("HealthRecordRepo", "Returning ${records.size} parsed records")
            Result.success(records)
            
        } catch (e: Exception) {
            android.util.Log.e("HealthRecordRepo", "Error fetching records: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get health records for self only (not dependents)
     */
    suspend fun getPatientSelfRecords(): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .get()
                .await()
            
            val records = snapshot.documents.mapNotNull { doc ->
                HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }.filter { it.dependentId.isBlank() }
             .sortedByDescending { it.uploadDate.seconds }
            
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
     * Delete a health record (file from Supabase + metadata from Firestore)
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
            
            // 3) Delete file from Supabase Storage (or Firebase for legacy records)
            if (record.fileUrl.contains("supabase.co")) {
                SupabaseStorageRepository.deleteRecord(record.fileUrl)
            } else {
                // Legacy Firebase Storage delete
                StorageRepository.deleteHealthRecord(record.fileUrl)
            }
            
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
     * Returns AccessDeniedException if doctor has no valid appointment relationship
     */
    suspend fun getDoctorAccessibleRecordsForPatient(patientUid: String): Result<List<HealthRecord>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Check if doctor has active appointment with patient (today or future scheduled, OR completed)
            val hasRelationship = AppointmentRepository.hasAppointmentRelationship(currentUser.uid, patientUid).getOrNull() ?: false
            
            // If no appointment relationship at all, deny access completely
            if (!hasRelationship) {
                android.util.Log.w("HealthRecordRepo", "Access denied: Doctor ${currentUser.uid} has no appointment relationship with patient $patientUid")
                return Result.failure(AccessDeniedException("You do not have an active appointment with this patient. Records cannot be viewed."))
            }
            
            // Check if doctor has ACTIVE (today or future) appointment for access to private records
            val hasActiveAppointment = AppointmentRepository.hasActiveAppointment(currentUser.uid, patientUid).getOrNull() ?: false
            
            // Get all patient records
            val recordsSnapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", patientUid)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val allRecords = recordsSnapshot.documents.mapNotNull { doc ->
                HealthRecord.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }
            
            // Filter out private records unless doctor has:
            // 1) Active appointment (scheduled for today or future), OR
            // 2) Explicit access in doctorAccessList
            val accessibleRecords = allRecords.filter { record ->
                !record.isPrivate || 
                record.doctorAccessList.contains(currentUser.uid) ||
                hasActiveAppointment
            }
            
            android.util.Log.d("HealthRecordRepo", "Doctor ${currentUser.uid} accessing ${accessibleRecords.size} of ${allRecords.size} records for patient $patientUid")
            Result.success(accessibleRecords)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if doctor has access to view patient records (without fetching)
     * Used for UI to show access denied state early
     */
    suspend fun checkDoctorAccessToPatient(patientUid: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val hasRelationship = AppointmentRepository.hasAppointmentRelationship(currentUser.uid, patientUid).getOrNull() ?: false
            Result.success(hasRelationship)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Exception thrown when doctor doesn't have access to patient records
 */
class AccessDeniedException(message: String) : Exception(message)
