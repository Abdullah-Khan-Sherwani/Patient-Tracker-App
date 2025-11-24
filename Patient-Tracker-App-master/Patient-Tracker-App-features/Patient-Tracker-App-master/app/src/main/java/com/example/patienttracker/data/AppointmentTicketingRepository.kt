package com.example.patienttracker.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing appointment tickets and numbering system.
 * Generates unique ticket numbers for appointments within a doctor's schedule.
 */
object AppointmentTicketingRepository {
    private const val TAG = "AppointmentTicketing"
    private val db = Firebase.firestore

    /**
     * Generate next appointment ticket number for a specific doctor on a specific date.
     * Ticket numbers are reset daily and start from 001.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @return Next ticket number (e.g., "001", "002", "003")
     */
    suspend fun generateTicketNumber(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp
    ): Result<String> = try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(appointmentDate.toDate())
        val docId = "${doctorUid}_$dateString"

        // Get or create ticket counter document
        val counterRef = db.collection("appointment_tickets").document(docId)
        val counterSnapshot = counterRef.get().await()

        val nextNumber = if (counterSnapshot.exists()) {
            val currentCount = counterSnapshot.getLong("count")?.toInt() ?: 0
            currentCount + 1
        } else {
            1
        }

        // Update counter
        counterRef.set(
            mapOf(
                "doctorUid" to doctorUid,
                "date" to dateString,
                "count" to nextNumber,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
        ).await()

        val ticketNumber = String.format("%03d", nextNumber)
        Log.d(TAG, "Generated ticket number: $ticketNumber for doctor: $doctorUid on $dateString")
        Result.success(ticketNumber)
    } catch (e: Exception) {
        Log.e(TAG, "Error generating ticket number: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get all appointments for a doctor on a specific date, grouped by time slot category.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @return List of appointments for that date
     */
    suspend fun getAppointmentsForDate(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp
    ): Result<List<Appointment>> = try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(appointmentDate.toDate())

        val querySnapshot = db.collection("appointments")
            .whereEqualTo("doctorUid", doctorUid)
            .get()
            .await()

        val appointments = querySnapshot.documents.mapNotNull { doc ->
            val appt = Appointment.fromFirestore(doc.data ?: return@mapNotNull null, doc.id)
            val apptDateString = dateFormat.format(appt.appointmentDate.toDate())
            
            // Filter by date and status != cancelled
            if (apptDateString == dateString && appt.status != "cancelled") {
                appt
            } else {
                null
            }
        }

        Log.d(TAG, "Found ${appointments.size} appointments for doctor $doctorUid on $dateString")
        Result.success(appointments)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching appointments for date: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get appointment count for a specific time slot category for a doctor on a specific date.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @param category The time slot category
     * @return Count of appointments in that category
     */
    suspend fun getSlotCategoryCount(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp,
        category: SlotCategory
    ): Result<Int> = try {
        val appointments = getAppointmentsForDate(doctorUid, appointmentDate).getOrNull() ?: emptyList()
        
        val count = appointments.count { appointment ->
            appointment.timeSlot.contains(category.displayName, ignoreCase = true)
        }

        Log.d(TAG, "Slot category $category has $count appointments")
        Result.success(count)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting slot category count: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Check if a time slot is available for a doctor on a specific date.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @param timeSlot The time slot
     * @return true if available, false otherwise
     */
    suspend fun isSlotAvailable(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp,
        timeSlot: String
    ): Result<Boolean> = try {
        val appointments = getAppointmentsForDate(doctorUid, appointmentDate).getOrNull() ?: emptyList()
        
        val isAvailable = appointments.none { appointment ->
            appointment.timeSlot == timeSlot && appointment.status != "cancelled"
        }

        Log.d(TAG, "Slot $timeSlot available: $isAvailable")
        Result.success(isAvailable)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking slot availability: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get available time slots for a doctor on a specific date.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @return List of available time slots
     */
    suspend fun getAvailableSlots(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp
    ): Result<List<TimeSlot>> = try {
        val appointments = getAppointmentsForDate(doctorUid, appointmentDate).getOrNull() ?: emptyList()
        val bookedSlots = appointments.map { it.timeSlot }.toSet()

        val allSlots = DefaultTimeSlots.getAllSlots()
        val availableSlots = allSlots.filter { slot ->
            slot.displayName !in bookedSlots
        }

        Log.d(TAG, "Available slots: ${availableSlots.size}/${allSlots.size}")
        Result.success(availableSlots)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting available slots: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get available time slots for a specific category for a doctor on a specific date.
     *
     * @param doctorUid The doctor's UID
     * @param appointmentDate The appointment date
     * @param category The time slot category
     * @return List of available time slots in the category
     */
    suspend fun getAvailableSlotsByCategory(
        doctorUid: String,
        appointmentDate: com.google.firebase.Timestamp,
        category: SlotCategory
    ): Result<List<TimeSlot>> = try {
        val availableSlots = getAvailableSlots(doctorUid, appointmentDate).getOrNull() ?: emptyList()
        val categorySlots = availableSlots.filter { it.category == category }

        Log.d(TAG, "Available slots in category $category: ${categorySlots.size}")
        Result.success(categorySlots)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting available slots by category: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Reset ticket counter for past dates (cleanup operation).
     * Should be run periodically to remove old counter documents.
     *
     * @param daysOld Number of days to keep (delete counters older than this)
     */
    suspend fun cleanupOldTicketCounters(daysOld: Int = 30): Result<Int> = try {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -daysOld)
        val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val snapshot = db.collection("appointment_tickets")
            .whereArrayContains("date", cutoffDate)
            .get()
            .await()

        var deletedCount = 0
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
            deletedCount++
        }

        Log.d(TAG, "Deleted $deletedCount old ticket counter documents")
        Result.success(deletedCount)
    } catch (e: Exception) {
        Log.e(TAG, "Error cleaning up old counters: ${e.message}", e)
        Result.failure(e)
    }
}
