package com.example.patienttracker.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for Doctor Notes operations
 */
object DoctorNoteRepository {
    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION = "doctor_notes"
    
    /**
     * Save a new doctor's note
     */
    suspend fun saveDoctorNote(
        appointmentId: String,
        patientUid: String,
        patientName: String,
        doctorName: String,
        speciality: String,
        comments: String,
        prescription: String,
        appointmentDate: Timestamp
    ): Result<DoctorNote> {
        return try {
            val currentUser = Firebase.auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val noteData = hashMapOf(
                "appointmentId" to appointmentId,
                "patientUid" to patientUid,
                "patientName" to patientName,
                "doctorUid" to currentUser.uid,
                "doctorName" to doctorName,
                "speciality" to speciality,
                "comments" to comments,
                "prescription" to prescription,
                "appointmentDate" to appointmentDate,
                "createdAt" to Timestamp.now()
            )
            
            val docRef = db.collection(COLLECTION).add(noteData).await()
            
            val note = DoctorNote(
                noteId = docRef.id,
                appointmentId = appointmentId,
                patientUid = patientUid,
                patientName = patientName,
                doctorUid = currentUser.uid,
                doctorName = doctorName,
                speciality = speciality,
                comments = comments,
                prescription = prescription,
                appointmentDate = appointmentDate,
                createdAt = Timestamp.now()
            )
            
            android.util.Log.d("DoctorNoteRepo", "Doctor note saved: ${docRef.id}")
            Result.success(note)
        } catch (e: Exception) {
            android.util.Log.e("DoctorNoteRepo", "Failed to save doctor note: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all doctor notes for a patient
     */
    suspend fun getNotesForPatient(patientUid: String): Result<List<DoctorNote>> {
        return try {
            android.util.Log.d("DoctorNoteRepo", "Fetching notes for patient: $patientUid")
            
            // Simple query without orderBy to avoid index issues
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("patientUid", patientUid)
                .get()
                .await()
            
            val notes = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.data?.let { DoctorNote.fromFirestore(it, doc.id) }
                } catch (e: Exception) {
                    android.util.Log.e("DoctorNoteRepo", "Failed to parse note ${doc.id}: ${e.message}")
                    null
                }
            }.sortedByDescending { it.createdAt.toDate() } // Sort in memory instead
            
            android.util.Log.d("DoctorNoteRepo", "Successfully fetched ${notes.size} notes for patient $patientUid")
            Result.success(notes)
        } catch (e: Exception) {
            android.util.Log.e("DoctorNoteRepo", "Failed to fetch notes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get doctor note for a specific appointment
     */
    suspend fun getNoteForAppointment(appointmentId: String): Result<DoctorNote?> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("appointmentId", appointmentId)
                .limit(1)
                .get()
                .await()
            
            val note = snapshot.documents.firstOrNull()?.let { doc ->
                doc.data?.let { DoctorNote.fromFirestore(it, doc.id) }
            }
            
            Result.success(note)
        } catch (e: Exception) {
            android.util.Log.e("DoctorNoteRepo", "Failed to fetch note: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all notes written by current doctor
     */
    suspend fun getNotesWrittenByDoctor(): Result<List<DoctorNote>> {
        return try {
            val currentUser = Firebase.auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("doctorUid", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val notes = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { DoctorNote.fromFirestore(it, doc.id) }
            }
            
            Result.success(notes)
        } catch (e: Exception) {
            android.util.Log.e("DoctorNoteRepo", "Failed to fetch doctor's notes: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a note exists for an appointment
     */
    suspend fun hasNoteForAppointment(appointmentId: String): Boolean {
        return try {
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("appointmentId", appointmentId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
