package com.example.patienttracker.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing consultation fees.
 * Handles fetching and updating global and per-doctor consultation fees.
 * Enables admins to control pricing without hardcoding values.
 */
object ConsultationFeeRepository {
    
    private val db = Firebase.firestore
    private const val CONFIG_COLLECTION = "configuration"
    private const val FEES_DOCUMENT = "consultationFees"
    private const val TAG = "ConsultationFeeRepo"
    
    // Default fee if configuration doesn't exist
    private const val DEFAULT_GLOBAL_FEE = 1500

    /**
     * Get the global consultation fee.
     * Falls back to DEFAULT_GLOBAL_FEE if not configured.
     */
    suspend fun getGlobalFee(): Int {
        return try {
            Log.d(TAG, "Fetching global consultation fee")
            
            val doc = db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .get()
                .await()
            
            if (doc.exists()) {
                val fee = doc.getLong("global_fee")?.toInt() ?: DEFAULT_GLOBAL_FEE
                Log.d(TAG, "Global fee fetched: $fee")
                fee
            } else {
                Log.d(TAG, "Configuration document not found, using default fee: $DEFAULT_GLOBAL_FEE")
                DEFAULT_GLOBAL_FEE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching global fee: ${e.message}", e)
            DEFAULT_GLOBAL_FEE
        }
    }

    /**
     * Get fee for a specific doctor.
     * Returns per-doctor fee if set, otherwise returns global fee.
     */
    suspend fun getDoctorFee(doctorUid: String): Int {
        return try {
            Log.d(TAG, "Fetching fee for doctor: $doctorUid")
            
            val doc = db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .get()
                .await()
            
            if (doc.exists()) {
                // Check per-doctor fees map
                val perDoctorFees = doc.get("per_doctor") as? Map<*, *>
                if (perDoctorFees != null && doctorUid in perDoctorFees) {
                    val fee = (perDoctorFees[doctorUid] as? Number)?.toInt() ?: getGlobalFee()
                    Log.d(TAG, "Doctor fee fetched: $fee")
                    return fee
                }
            }
            
            // Fall back to global fee
            getGlobalFee()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching doctor fee: ${e.message}", e)
            getGlobalFee()
        }
    }

    /**
     * Update global consultation fee (Admin only).
     * Should only be called after verifying user is admin.
     */
    suspend fun updateGlobalFee(newFee: Int): Result<Unit> {
        return try {
            Log.d(TAG, "Updating global fee to: $newFee")
            
            // Validate fee
            if (newFee <= 0) {
                return Result.failure(IllegalArgumentException("Fee must be greater than 0"))
            }
            
            db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .set(mapOf("global_fee" to newFee), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Log.d(TAG, "Global fee updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating global fee: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update fee for a specific doctor (Admin only).
     * Should only be called after verifying user is admin.
     */
    suspend fun updateDoctorFee(doctorUid: String, newFee: Int): Result<Unit> {
        return try {
            Log.d(TAG, "Updating fee for doctor $doctorUid to: $newFee")
            
            // Validate fee
            if (newFee <= 0) {
                return Result.failure(IllegalArgumentException("Fee must be greater than 0"))
            }
            
            // Get existing per-doctor fees
            val doc = db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .get()
                .await()
            
            val perDoctorFees = if (doc.exists()) {
                (doc.get("per_doctor") as? MutableMap<String, Any>) ?: mutableMapOf()
            } else {
                mutableMapOf()
            }
            
            // Update the fee for this doctor
            perDoctorFees[doctorUid] = newFee
            
            db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .set(mapOf("per_doctor" to perDoctorFees), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Log.d(TAG, "Doctor fee updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating doctor fee: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all per-doctor fees configuration (Admin only).
     */
    suspend fun getAllPerDoctorFees(): Map<String, Int> {
        return try {
            Log.d(TAG, "Fetching all per-doctor fees")
            
            val doc = db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .get()
                .await()
            
            if (doc.exists()) {
                val perDoctorFees = doc.get("per_doctor") as? Map<String, Any> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                (perDoctorFees as? Map<String, Int>) ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all per-doctor fees: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Get fee configuration including both global and per-doctor fees
     */
    suspend fun getFeeConfiguration(): FeeConfiguration {
        return try {
            Log.d(TAG, "Fetching complete fee configuration")
            
            val doc = db.collection(CONFIG_COLLECTION)
                .document(FEES_DOCUMENT)
                .get()
                .await()
            
            if (doc.exists()) {
                val globalFee = doc.getLong("global_fee")?.toInt() ?: DEFAULT_GLOBAL_FEE
                val perDoctorFees = doc.get("per_doctor") as? Map<String, Any> ?: emptyMap()
                
                @Suppress("UNCHECKED_CAST")
                FeeConfiguration(
                    globalFee = globalFee,
                    perDoctorFees = (perDoctorFees as? Map<String, Int>) ?: emptyMap()
                )
            } else {
                FeeConfiguration(
                    globalFee = DEFAULT_GLOBAL_FEE,
                    perDoctorFees = emptyMap()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fee configuration: ${e.message}", e)
            FeeConfiguration(
                globalFee = DEFAULT_GLOBAL_FEE,
                perDoctorFees = emptyMap()
            )
        }
    }
}

/**
 * Data class representing fee configuration
 */
data class FeeConfiguration(
    val globalFee: Int,
    val perDoctorFees: Map<String, Int>
)
