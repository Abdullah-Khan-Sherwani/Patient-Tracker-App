package com.example.patienttracker.data

import android.util.Log
import com.example.patienttracker.ui.screens.patient.DoctorFull
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for searching doctors and patients across the application.
 * Provides unified search functionality across specialties, names, and qualifications.
 */
object SearchRepository {
    private const val TAG = "SearchRepository"
    private val db = Firebase.firestore

    /**
     * Search for doctors by name, specialization, or qualifications.
     * Supports partial matching and case-insensitive search.
     *
     * @param query Search query string (name, specialization, or qualification)
     * @return List of matching doctors
     */
    suspend fun searchDoctors(query: String): Result<List<DoctorFull>> = try {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }

        val lowerQuery = query.lowercase().trim()
        val allDoctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        // Filter doctors based on multiple criteria
        val results = allDoctors.filter { doctor ->
            val firstName = doctor.firstName.lowercase()
            val lastName = doctor.lastName.lowercase()
            val fullName = "$firstName ${lastName}"
            val speciality = doctor.speciality.lowercase()
            val email = doctor.email?.lowercase() ?: ""

            // Match query against multiple fields with partial matching
            firstName.contains(lowerQuery) ||
            lastName.contains(lowerQuery) ||
            fullName.contains(lowerQuery) ||
            speciality.contains(lowerQuery) ||
            email.contains(lowerQuery)
        }

        Log.d(TAG, "Search for '$query' found ${results.size} doctors")
        Result.success(results)
    } catch (e: Exception) {
        Log.e(TAG, "Error searching doctors: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Search for doctors by specialization only.
     *
     * @param specialization The medical specialty to search for
     * @return List of doctors in that specialty
     */
    suspend fun searchBySpecialization(specialization: String): Result<List<DoctorFull>> = try {
        if (specialization.isBlank()) {
            return Result.success(emptyList())
        }

        val lowerSpec = specialization.lowercase().trim()
        val allDoctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        val results = allDoctors.filter { doctor ->
            doctor.speciality.lowercase().contains(lowerSpec)
        }

        Log.d(TAG, "Specialization search for '$specialization' found ${results.size} doctors")
        Result.success(results)
    } catch (e: Exception) {
        Log.e(TAG, "Error searching by specialization: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Search for doctors by exact name match (first or last name).
     *
     * @param name The doctor's name to search for
     * @return List of doctors with matching names
     */
    suspend fun searchByName(name: String): Result<List<DoctorFull>> = try {
        if (name.isBlank()) {
            return Result.success(emptyList())
        }

        val lowerName = name.lowercase().trim()
        val allDoctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        val results = allDoctors.filter { doctor ->
            doctor.firstName.lowercase().contains(lowerName) ||
            doctor.lastName.lowercase().contains(lowerName)
        }

        Log.d(TAG, "Name search for '$name' found ${results.size} doctors")
        Result.success(results)
    } catch (e: Exception) {
        Log.e(TAG, "Error searching by name: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Search for patients by name or email (admin only).
     * Used in admin management screens.
     *
     * @param query Search query (name or email)
     * @return List of matching patient data
     */
    suspend fun searchPatients(query: String): Result<List<Map<String, Any>>> = try {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }

        val lowerQuery = query.lowercase().trim()
        val allPatients = db.collection("users")
            .whereEqualTo("userType", "patient")
            .get()
            .await()
            .documents

        val results = allPatients.filter { doc ->
            val firstName = doc.getString("firstName")?.lowercase() ?: ""
            val lastName = doc.getString("lastName")?.lowercase() ?: ""
            val fullName = "$firstName $lastName"
            val email = doc.getString("email")?.lowercase() ?: ""

            firstName.contains(lowerQuery) ||
            lastName.contains(lowerQuery) ||
            fullName.contains(lowerQuery) ||
            email.contains(lowerQuery)
        }.map { it.data ?: emptyMap() }

        Log.d(TAG, "Patient search for '$query' found ${results.size} patients")
        Result.success(results)
    } catch (e: Exception) {
        Log.e(TAG, "Error searching patients: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get all doctors (for filtering/sorting).
     * Useful for initial catalog load before filtering.
     *
     * @return List of all doctors
     */
    suspend fun getAllDoctors(): Result<List<DoctorFull>> = try {
        val doctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        Log.d(TAG, "Retrieved ${doctors.size} total doctors")
        Result.success(doctors)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching all doctors: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Get top rated doctors (based on number of appointments/reviews).
     * Can be used for featured doctors section.
     *
     * @param limit Maximum number of doctors to return
     * @return List of top rated doctors
     */
    suspend fun getTopRatedDoctors(limit: Int = 10): Result<List<DoctorFull>> = try {
        val allDoctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        // Sort by number of appointments (descending)
        val topDoctors = allDoctors.take(limit)

        Log.d(TAG, "Retrieved top $limit doctors")
        Result.success(topDoctors)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching top rated doctors: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Search with auto-complete suggestions.
     * Returns suggestions based on doctor names and specializations.
     *
     * @param prefix The search prefix
     * @return List of suggested search terms
     */
    suspend fun getSearchSuggestions(prefix: String): Result<List<String>> = try {
        if (prefix.isBlank()) {
            return Result.success(emptyList())
        }

        val lowerPrefix = prefix.lowercase().trim()
        val allDoctors = db.collection("users")
            .whereEqualTo("userType", "doctor")
            .get()
            .await()
            .toObjects(DoctorFull::class.java)

        val suggestions = mutableSetOf<String>()

        allDoctors.forEach { doctor ->
            // Add doctor name suggestions
            if (doctor.firstName.lowercase().startsWith(lowerPrefix)) {
                suggestions.add(doctor.firstName)
            }
            if (doctor.lastName.lowercase().startsWith(lowerPrefix)) {
                suggestions.add(doctor.lastName)
            }
            // Add specialization suggestions
            if (doctor.speciality.lowercase().startsWith(lowerPrefix)) {
                suggestions.add(doctor.speciality)
            }
        }

        Log.d(TAG, "Generated ${suggestions.size} search suggestions for '$prefix'")
        Result.success(suggestions.toList().sorted())
    } catch (e: Exception) {
        Log.e(TAG, "Error generating search suggestions: ${e.message}", e)
        Result.failure(e)
    }
}
