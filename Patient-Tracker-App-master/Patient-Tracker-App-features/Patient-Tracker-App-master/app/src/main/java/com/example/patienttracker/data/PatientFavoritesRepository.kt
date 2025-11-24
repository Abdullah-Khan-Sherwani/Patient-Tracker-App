package com.example.patienttracker.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object PatientFavoritesRepository {
    
    private val db = Firebase.firestore
    
    /**
     * Add a doctor to patient's favorites
     */
    suspend fun addFavoriteDoctor(doctorId: String): Result<Boolean> {
        return try {
            val currentUser = Firebase.auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            db.collection("users")
                .document(currentUser.uid)
                .update("favoriteDoctorIds", FieldValue.arrayUnion(doctorId))
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Remove a doctor from patient's favorites
     */
    suspend fun removeFavoriteDoctor(doctorId: String): Result<Boolean> {
        return try {
            val currentUser = Firebase.auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            db.collection("users")
                .document(currentUser.uid)
                .update("favoriteDoctorIds", FieldValue.arrayRemove(doctorId))
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Get list of favorite doctor IDs for current patient
     */
    suspend fun getFavoriteDoctorIds(): List<String> {
        return try {
            val currentUser = Firebase.auth.currentUser
            if (currentUser == null) {
                return emptyList()
            }
            
            val userDoc = db.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            (userDoc.get("favoriteDoctorIds") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Check if a specific doctor is favorited
     */
    suspend fun isDoctorFavorited(doctorId: String): Boolean {
        val favorites = getFavoriteDoctorIds()
        return favorites.contains(doctorId)
    }
    
    /**
     * Toggle favorite status for a doctor
     */
    suspend fun toggleFavorite(doctorId: String): Result<Boolean> {
        return try {
            val isFavorited = isDoctorFavorited(doctorId)
            if (isFavorited) {
                removeFavoriteDoctor(doctorId)
            } else {
                addFavoriteDoctor(doctorId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
