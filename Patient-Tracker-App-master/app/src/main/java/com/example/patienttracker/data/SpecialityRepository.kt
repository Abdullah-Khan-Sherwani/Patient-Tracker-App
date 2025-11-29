package com.example.patienttracker.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing doctor specialities.
 * Fetches from Firestore and provides autocomplete suggestions.
 */
object SpecialityRepository {
    
    private const val TAG = "SpecialityRepository"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_SPECIALITIES = "specialities"
    
    // In-memory cache for specialities
    private var cachedSpecialities: List<String> = emptyList()
    private var lastFetchTime: Long = 0
    private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes cache
    
    // Predefined specialities matching Doctor Catalogue
    private val predefinedSpecialities = listOf(
        "General Physician",
        "Cardiologist",
        "Dermatologist",
        "Pediatrician",
        "Neurologist",
        "Psychiatrist",
        "ENT Specialist",
        "Orthopedic",
        "Gynecologist",
        "Dentist",
        "Urologist",
        "Oncologist",
        "Radiologist"
    )
    
    /**
     * Get all unique specialities from Firestore.
     * First tries to get from dedicated specialities collection,
     * then falls back to extracting from doctor profiles.
     */
    suspend fun getAllSpecialities(forceRefresh: Boolean = false): List<String> {
        // Return cached if still valid
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedSpecialities.isNotEmpty() && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return cachedSpecialities
        }
        
        return try {
            val db = Firebase.firestore
            val specialities = mutableSetOf<String>()
            
            // First, try to get from dedicated specialities collection
            try {
                val specialitiesSnapshot = db.collection(COLLECTION_SPECIALITIES).get().await()
                specialitiesSnapshot.documents.forEach { doc ->
                    doc.getString("name")?.let { specialities.add(it) }
                }
            } catch (e: Exception) {
                Log.d(TAG, "No specialities collection, falling back to users")
            }
            
            // Also get specialities from existing doctors
            val doctorsSnapshot = db.collection(COLLECTION_USERS)
                .whereEqualTo("role", "doctor")
                .get()
                .await()
            
            doctorsSnapshot.documents.forEach { doc ->
                // Try specialities array first
                @Suppress("UNCHECKED_CAST")
                val specialitiesList = doc.get("specialities") as? List<String>
                if (specialitiesList != null) {
                    specialities.addAll(specialitiesList)
                } else {
                    // Fall back to comma-separated speciality field
                    doc.getString("speciality")?.let { spec ->
                        spec.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                            specialities.add(it)
                        }
                    }
                }
            }
            
            // Add default specialities if collection is empty
            if (specialities.isEmpty()) {
                specialities.addAll(predefinedSpecialities)
            } else {
                // Always include predefined ones
                specialities.addAll(predefinedSpecialities)
            }
            
            // Update cache
            cachedSpecialities = specialities.toList().sorted()
            lastFetchTime = now
            
            Log.d(TAG, "Loaded ${cachedSpecialities.size} specialities")
            cachedSpecialities
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching specialities: ${e.message}")
            // Return defaults on error
            predefinedSpecialities
        }
    }
    
    /**
     * Get speciality suggestions based on query string.
     * Case-insensitive matching.
     */
    suspend fun getSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        
        val allSpecialities = getAllSpecialities()
        val queryLower = query.lowercase().trim()
        
        return allSpecialities.filter { speciality ->
            speciality.lowercase().contains(queryLower)
        }.take(10) // Limit to 10 suggestions
    }
    
    /**
     * Add a new speciality to Firestore for future use.
     */
    suspend fun addSpeciality(speciality: String): Result<Unit> {
        return try {
            val db = Firebase.firestore
            val docRef = db.collection(COLLECTION_SPECIALITIES).document()
            docRef.set(mapOf(
                "name" to speciality.trim(),
                "createdAt" to com.google.firebase.Timestamp.now()
            )).await()
            
            // Invalidate cache
            lastFetchTime = 0
            
            Log.d(TAG, "Added new speciality: $speciality")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding speciality: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clear the cache to force refresh on next fetch
     */
    fun clearCache() {
        cachedSpecialities = emptyList()
        lastFetchTime = 0
    }
}
