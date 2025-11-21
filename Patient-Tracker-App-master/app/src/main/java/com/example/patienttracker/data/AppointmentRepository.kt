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
     * Generate next appointment number (001, 002, etc.)
     */
    private suspend fun getNextAppointmentNumber(): String {
        val counterRef = db.collection("counters").document("appointments")
        
        return try {
            // First, ensure counter document exists
            val counterDoc = counterRef.get().await()
            if (!counterDoc.exists()) {
                counterRef.set(mapOf("count" to 0L)).await()
            }
            
            // Then run transaction to increment
            db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val currentCount = snapshot.getLong("count") ?: 0L
                val nextCount = currentCount + 1
                
                transaction.update(counterRef, "count", nextCount)
                
                // Format as 3-digit number with leading zeros
                String.format("%03d", nextCount)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            // If counter fails, use timestamp-based number
            String.format("%03d", (System.currentTimeMillis() % 1000))
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
            
            // Generate appointment number
            val appointmentNumber = getNextAppointmentNumber()
            
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
                price = 1500,
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
