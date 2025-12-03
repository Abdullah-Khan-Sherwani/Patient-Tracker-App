package com.example.patienttracker.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Appointment Firestore operations
 */
object AppointmentRepository {
    
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private const val COLLECTION = "appointments"
    
    /**
     * Create a new appointment
     */
    suspend fun createAppointment(
        doctorUid: String,
        doctorName: String,
        speciality: String,
        appointmentDate: Timestamp,
        timeSlot: String,
        blockName: String = "",
        recipientType: String = "self",
        dependentId: String = "",
        dependentName: String = "",
        notes: String = ""
    ): Result<Appointment> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get patient name
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val patientName = "$firstName $lastName"
            
            // Fetch consultation fee from configuration (admin-editable)
            val consultationFee = ConsultationFeeRepository.getDoctorFee(doctorUid)
            
            // Create appointment
            val appointmentId = UUID.randomUUID().toString()
            
            // Extract slot start and end times from timeSlot (format: "HH:mm-HH:mm")
            val slotParts = timeSlot.split("-").map { it.trim() }
            val slotStartTime = if (slotParts.size >= 1) slotParts[0] else ""
            val slotEndTime = if (slotParts.size >= 2) slotParts[1] else ""
            
            val appointment = Appointment(
                appointmentId = appointmentId,
                patientUid = currentUser.uid,
                patientName = patientName,
                doctorUid = doctorUid,
                doctorName = doctorName,
                speciality = speciality,
                appointmentDate = appointmentDate,
                timeSlot = timeSlot,
                slotStartTime = slotStartTime,
                slotEndTime = slotEndTime,
                blockName = blockName,
                status = "scheduled",
                notes = notes,
                recipientType = recipientType,
                dependentId = dependentId,
                dependentName = dependentName,
                price = consultationFee,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            // Save to Firestore with slot time fields and dependent info
            val appointmentData = appointment.toFirestore().toMutableMap()
            appointmentData["blockName"] = blockName
            appointmentData["recipientType"] = recipientType
            appointmentData["dependentId"] = dependentId
            appointmentData["dependentName"] = dependentName
            appointmentData["slotStartTime"] = slotStartTime
            appointmentData["slotEndTime"] = slotEndTime

            db.collection(COLLECTION)
                .document(appointmentId)
                .set(appointmentData)
                .await()
            
            Result.success(appointment)
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("AppointmentRepository", "Failed to create appointment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all appointments for current patient
     */
    suspend fun getPatientAppointments(): Result<List<Appointment>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            android.util.Log.d("AppointmentRepository", "Fetching appointments for user: ${currentUser.uid}")
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .get()
                .await()
            
            android.util.Log.d("AppointmentRepository", "Found ${snapshot.size()} appointments")
            
            val appointments = snapshot.documents.mapNotNull { doc ->
                try {
                    val appointment = Appointment.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
                    android.util.Log.d("AppointmentRepository", "Parsed appointment: ${appointment.doctorName} on ${appointment.appointmentDate.toDate()}")
                    appointment
                } catch (e: Exception) {
                    android.util.Log.e("AppointmentRepository", "Failed to parse appointment ${doc.id}: ${e.message}", e)
                    null
                }
            }.sortedByDescending { it.appointmentDate }
            
            android.util.Log.d("AppointmentRepository", "Returning ${appointments.size} appointments")
            Result.success(appointments)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepository", "Error fetching appointments: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Get all appointments for a specific dependent
     */
    suspend fun getDependentAppointments(dependentId: String): Result<List<Appointment>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
                .whereEqualTo("dependentId", dependentId)
                .get()
                .await()
            
            val appointments = snapshot.documents.mapNotNull { doc ->
                try {
                    Appointment.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.appointmentDate }
            
            Result.success(appointments)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all appointments for current doctor
     */
    suspend fun getDoctorAppointments(): Result<List<Appointment>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", currentUser.uid)
                .orderBy("appointmentDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val appointments = snapshot.documents.mapNotNull { doc ->
                Appointment.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            }
            
            Result.success(appointments)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get appointments for a specific date (for doctor)
     */
    suspend fun getDoctorAppointmentsByDate(date: String): Result<List<Appointment>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get all doctor appointments and filter by date
            val allAppointments = getDoctorAppointments().getOrNull() ?: emptyList()
            
            val filtered = allAppointments.filter { appointment ->
                appointment.getFormattedDate() == date
            }
            
            Result.success(filtered)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update appointment status
     */
    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Unit> {
        return try {
            val currentUser = Firebase.auth.currentUser
            android.util.Log.d("AppointmentRepo", "Updating appointment $appointmentId to status=$status, currentUser=${currentUser?.uid}")
            
            // First, fetch the appointment to verify doctorUid
            val apptDoc = db.collection(COLLECTION).document(appointmentId).get().await()
            val doctorUid = apptDoc.getString("doctorUid")
            android.util.Log.d("AppointmentRepo", "Appointment doctorUid=$doctorUid, currentUser=${currentUser?.uid}, match=${doctorUid == currentUser?.uid}")
            
            db.collection(COLLECTION)
                .document(appointmentId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            android.util.Log.d("AppointmentRepo", "Successfully updated appointment")
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "Error updating appointment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel appointment
     */
    suspend fun cancelAppointment(appointmentId: String, cancelledBy: String = "patient"): Result<Unit> {
        return try {
            db.collection(COLLECTION)
                .document(appointmentId)
                .update(
                    mapOf(
                        "status" to "cancelled",
                        "cancelledBy" to cancelledBy,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reschedule appointment - updates date, time, and sets status to "rescheduled"
     */
    suspend fun rescheduleAppointment(
        appointmentId: String,
        newDate: Timestamp,
        newTimeSlot: String,
        newBlockName: String = ""
    ): Result<Unit> {
        return try {
            // Extract slot times from timeSlot
            val slotParts = newTimeSlot.split("-").map { it.trim() }
            val slotStartTime = if (slotParts.isNotEmpty()) slotParts[0] else ""
            val slotEndTime = if (slotParts.size >= 2) slotParts[1] else ""
            
            db.collection(COLLECTION)
                .document(appointmentId)
                .update(
                    mapOf(
                        "appointmentDate" to newDate,
                        "timeSlot" to newTimeSlot,
                        "slotStartTime" to slotStartTime,
                        "slotEndTime" to slotEndTime,
                        "blockName" to newBlockName,
                        "status" to "rescheduled",
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "Error rescheduling appointment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete appointment
     */
    suspend fun completeAppointment(appointmentId: String): Result<Unit> {
        return updateAppointmentStatus(appointmentId, "completed")
    }
    
    /**
     * Check if doctor has active appointment with patient (for access control)
     * Active means: status IN (scheduled, confirmed, pending) AND appointmentDate >= today
     */
    suspend fun hasActiveAppointment(doctorUid: String, patientUid: String): Result<Boolean> {
        return try {
            // Get start of today (midnight)
            val todayCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayTimestamp = Timestamp(todayCalendar.time)
            
            // Check for active statuses: scheduled, confirmed, pending
            val activeStatuses = listOf("scheduled", "confirmed", "pending")
            
            for (status in activeStatuses) {
                val snapshot = db.collection(COLLECTION)
                    .whereEqualTo("doctorUid", doctorUid)
                    .whereEqualTo("patientUid", patientUid)
                    .whereEqualTo("status", status)
                    .whereGreaterThanOrEqualTo("appointmentDate", todayTimestamp)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    android.util.Log.d("AppointmentRepo", "hasActiveAppointment: doctor=$doctorUid, patient=$patientUid, found with status=$status")
                    return Result.success(true)
                }
            }
            
            android.util.Log.d("AppointmentRepo", "hasActiveAppointment: doctor=$doctorUid, patient=$patientUid, found=false")
            Result.success(false)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "hasActiveAppointment error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if doctor has any relationship with patient (any appointment regardless of status)
     * Used for accessing records - doctors who have ANY appointment with a patient can view non-private records
     */
    suspend fun hasAppointmentRelationship(doctorUid: String, patientUid: String): Result<Boolean> {
        return try {
            // Simply check if ANY appointment exists between doctor and patient
            // This is the most permissive check - if doctor appears in patient's appointments, allow access
            val anyAppointmentSnapshot = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", doctorUid)
                .whereEqualTo("patientUid", patientUid)
                .limit(1) // We only need to know if at least one exists
                .get()
                .await()
            
            val hasRelationship = !anyAppointmentSnapshot.isEmpty
            android.util.Log.d("AppointmentRepo", "hasAppointmentRelationship: doctor=$doctorUid, patient=$patientUid, hasAnyAppointment=$hasRelationship")
            
            Result.success(hasRelationship)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "hasAppointmentRelationship error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if doctor has any appointment relationship with a specific dependent
     */
    suspend fun hasAppointmentRelationshipWithDependent(doctorUid: String, patientUid: String, dependentId: String): Result<Boolean> {
        return try {
            // Check if any appointment exists with this specific dependent
            val anyAppointmentSnapshot = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", doctorUid)
                .whereEqualTo("patientUid", patientUid)
                .whereEqualTo("dependentId", dependentId)
                .limit(1)
                .get()
                .await()
            
            val hasRelationship = !anyAppointmentSnapshot.isEmpty
            android.util.Log.d("AppointmentRepo", "hasAppointmentRelationshipWithDependent: doctor=$doctorUid, dependent=$dependentId, has=$hasRelationship")
            
            Result.success(hasRelationship)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "hasAppointmentRelationshipWithDependent error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if doctor has an ACTIVE (today or future scheduled) appointment with a specific dependent
     */
    suspend fun hasActiveAppointmentWithDependent(doctorUid: String, patientUid: String, dependentId: String): Result<Boolean> {
        return try {
            // Get start of today (midnight)
            val todayCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayTimestamp = Timestamp(todayCalendar.time)
            
            // Check for active statuses: scheduled, confirmed, pending
            val activeStatuses = listOf("scheduled", "confirmed", "pending")
            
            for (status in activeStatuses) {
                val snapshot = db.collection(COLLECTION)
                    .whereEqualTo("doctorUid", doctorUid)
                    .whereEqualTo("patientUid", patientUid)
                    .whereEqualTo("dependentId", dependentId)
                    .whereEqualTo("status", status)
                    .whereGreaterThanOrEqualTo("appointmentDate", todayTimestamp)
                    .limit(1)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    android.util.Log.d("AppointmentRepo", "hasActiveAppointmentWithDependent: doctor=$doctorUid, dependent=$dependentId, found with status=$status")
                    return Result.success(true)
                }
            }
            
            android.util.Log.d("AppointmentRepo", "hasActiveAppointmentWithDependent: doctor=$doctorUid, dependent=$dependentId, hasActive=false")
            Result.success(false)
            
        } catch (e: Exception) {
            android.util.Log.e("AppointmentRepo", "hasActiveAppointmentWithDependent error: ${e.message}")
            Result.failure(e)
        }
    }
}
