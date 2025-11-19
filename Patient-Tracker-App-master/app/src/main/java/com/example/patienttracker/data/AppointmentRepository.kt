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
            
            // Create appointment
            val appointmentId = UUID.randomUUID().toString()
            val appointment = Appointment(
                appointmentId = appointmentId,
                patientUid = currentUser.uid,
                patientName = patientName,
                doctorUid = doctorUid,
                doctorName = doctorName,
                speciality = speciality,
                appointmentDate = appointmentDate,
                timeSlot = timeSlot,
                status = "scheduled",
                notes = notes,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            // Save to Firestore
            db.collection(COLLECTION)
                .document(appointmentId)
                .set(appointment.toFirestore())
                .await()
            
            Result.success(appointment)
            
        } catch (e: Exception) {
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
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", currentUser.uid)
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
            db.collection(COLLECTION)
                .document(appointmentId)
                .update(
                    mapOf(
                        "status" to status,
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
     * Cancel appointment
     */
    suspend fun cancelAppointment(appointmentId: String): Result<Unit> {
        return updateAppointmentStatus(appointmentId, "cancelled")
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
