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
     * Generate next appointment number per doctor, per day (resets daily)
     * Format: Simple integer (1, 2, 3...)
     * Each doctor has their own sequence that resets at midnight
     */
    private suspend fun getNextAppointmentNumber(doctorUid: String, appointmentDate: Timestamp): String {
        return try {
            // Extract date string from timestamp (yyyy-MM-dd format)
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = appointmentDate.seconds * 1000
            val dateString = String.format(
                "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            
            // Count existing appointments for this doctor on this date (exclude cancelled)
            val existingAppointments = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", doctorUid)
                .get()
                .await()
            
            // Filter by date and non-cancelled status
            val appointmentsOnDate = existingAppointments.documents.count { doc ->
                val docDate = doc.getTimestamp("appointmentDate")
                val docStatus = doc.getString("status") ?: ""
                
                if (docDate != null && docStatus != "cancelled") {
                    val docCalendar = java.util.Calendar.getInstance()
                    docCalendar.timeInMillis = docDate.seconds * 1000
                    val docDateString = String.format(
                        "%04d-%02d-%02d",
                        docCalendar.get(java.util.Calendar.YEAR),
                        docCalendar.get(java.util.Calendar.MONTH) + 1,
                        docCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    docDateString == dateString
                } else {
                    false
                }
            }
            
            // Next number is count + 1
            val nextNumber = appointmentsOnDate + 1
            nextNumber.toString()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to "1" if something goes wrong
            "1"
        }
    }
    
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
            
            // Generate appointment number (per doctor, per day)
            val appointmentNumber = getNextAppointmentNumber(doctorUid, appointmentDate)
            
            // Fetch consultation fee from configuration (admin-editable)
            val consultationFee = ConsultationFeeRepository.getDoctorFee(doctorUid)
            
            // Create appointment
            val appointmentId = UUID.randomUUID().toString()
            val appointment = Appointment(
                appointmentId = appointmentId,
                appointmentNumber = appointmentNumber,
                patientUid = currentUser.uid,
                patientName = patientName,
                doctorUid = doctorUid,
                doctorName = doctorName,
                speciality = speciality,
                appointmentDate = appointmentDate,
                timeSlot = timeSlot,
                status = "scheduled",
                notes = notes,
                recipientType = recipientType,
                dependentId = dependentId,
                dependentName = dependentName,
                price = consultationFee,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            // Save to Firestore with blockName and dependent info for slot counting
            val appointmentData = appointment.toFirestore().toMutableMap()
            appointmentData["blockName"] = blockName
            appointmentData["recipientType"] = recipientType
            appointmentData["dependentId"] = dependentId
            appointmentData["dependentName"] = dependentName

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
     * Complete appointment
     */
    suspend fun completeAppointment(appointmentId: String): Result<Unit> {
        return updateAppointmentStatus(appointmentId, "completed")
    }
    
    /**
     * Check if doctor has active appointment with patient (for access control)
     */
    suspend fun hasActiveAppointment(doctorUid: String, patientUid: String): Result<Boolean> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", doctorUid)
                .whereEqualTo("patientUid", patientUid)
                .whereEqualTo("status", "scheduled")
                .get()
                .await()
            
            Result.success(!snapshot.isEmpty)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
