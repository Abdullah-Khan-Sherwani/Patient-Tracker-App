package com.example.patienttracker.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing health information state
 */
data class HealthInfoState(
    val bloodGroup: String? = null,
    val isBloodGroupLocked: Boolean = false,
    val height: String = "",
    val weight: String = "",
    val heightLastUpdated: Timestamp? = null,
    val weightLastUpdated: Timestamp? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

/**
 * Sealed class for UI events
 */
sealed class HealthInfoEvent {
    object SaveSuccess : HealthInfoEvent()
    data class Error(val message: String) : HealthInfoEvent()
}

/**
 * Mode for HealthInfoViewModel - determines which document to read/write
 */
sealed class HealthInfoMode {
    object Patient : HealthInfoMode()
    data class Dependent(val dependentId: String, val parentUid: String) : HealthInfoMode()
    data class DoctorReadOnly(val patientUid: String) : HealthInfoMode()
    data class DoctorReadOnlyDependent(val dependentId: String, val parentUid: String) : HealthInfoMode()
}

/**
 * ViewModel for managing health information
 * Supports patient self, dependent, and doctor read-only modes
 */
class HealthInfoViewModel(
    private val mode: HealthInfoMode = HealthInfoMode.Patient
) : ViewModel() {
    
    private val TAG = "HealthInfoViewModel"
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    
    private val _state = mutableStateOf(HealthInfoState())
    val state: State<HealthInfoState> = _state
    
    // For tracking unsaved changes
    private var originalHeight: String = ""
    private var originalWeight: String = ""
    
    private val _events = MutableSharedFlow<HealthInfoEvent>()
    val events = _events.asSharedFlow()
    
    // Blood group options
    val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    
    // Check if this is read-only mode (doctor view)
    val isReadOnly: Boolean = mode is HealthInfoMode.DoctorReadOnly || mode is HealthInfoMode.DoctorReadOnlyDependent
    
    init {
        loadHealthInfo()
    }
    
    /**
     * Get the document reference based on mode
     * Returns null only for Patient mode if user is not logged in
     */
    private fun getDocumentRef(): com.google.firebase.firestore.DocumentReference? {
        return when (mode) {
            is HealthInfoMode.Patient -> {
                val userId = auth.currentUser?.uid ?: return null
                db.collection("users").document(userId)
            }
            is HealthInfoMode.Dependent -> {
                db.collection("users").document(mode.parentUid)
                    .collection("dependents").document(mode.dependentId)
            }
            is HealthInfoMode.DoctorReadOnly -> {
                db.collection("users").document(mode.patientUid)
            }
            is HealthInfoMode.DoctorReadOnlyDependent -> {
                db.collection("users").document(mode.parentUid)
                    .collection("dependents").document(mode.dependentId)
            }
        }
    }
    
    /**
     * Load health information from Firestore
     */
    fun loadHealthInfo() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // Get the document reference based on mode
                val documentRef = when (mode) {
                    is HealthInfoMode.Patient -> {
                        val userId = auth.currentUser?.uid ?: run {
                            _state.value = _state.value.copy(isLoading = false, error = "User not logged in")
                            return@launch
                        }
                        db.collection("users").document(userId)
                    }
                    is HealthInfoMode.Dependent -> {
                        db.collection("users").document(mode.parentUid)
                            .collection("dependents").document(mode.dependentId)
                    }
                    is HealthInfoMode.DoctorReadOnly -> {
                        db.collection("users").document(mode.patientUid)
                    }
                    is HealthInfoMode.DoctorReadOnlyDependent -> {
                        Log.d(TAG, "Loading dependent health info: parentUid=${mode.parentUid}, dependentId=${mode.dependentId}")
                        db.collection("users").document(mode.parentUid)
                            .collection("dependents").document(mode.dependentId)
                    }
                }
                
                val document = documentRef.get().await()
                
                Log.d(TAG, "Document exists: ${document.exists()}, path: ${document.reference.path}")
                
                if (document.exists()) {
                    val bloodGroup = document.getString("bloodGroup")
                    val height = document.getString("height") ?: ""
                    val weight = document.getString("weight") ?: ""
                    val heightUpdated = document.getTimestamp("heightLastUpdated")
                    val weightUpdated = document.getTimestamp("weightLastUpdated")
                    
                    // Handle different DOB field names
                    val dob = document.getString("dateOfBirth") 
                        ?: document.getString("dob")  // For dependents
                        ?: ""
                    
                    // Get gender
                    val gender = document.getString("gender") ?: ""
                    
                    Log.d(TAG, "Loaded health info - bloodGroup: $bloodGroup, height: $height, weight: $weight, dob: $dob, gender: $gender")
                    
                    // Store original values for change detection
                    originalHeight = height
                    originalWeight = weight
                    
                    _state.value = _state.value.copy(
                        bloodGroup = bloodGroup,
                        isBloodGroupLocked = !bloodGroup.isNullOrEmpty(),
                        height = height,
                        weight = weight,
                        heightLastUpdated = heightUpdated,
                        weightLastUpdated = weightUpdated,
                        dateOfBirth = dob,
                        gender = gender,
                        isLoading = false
                    )
                } else {
                    Log.d(TAG, "Document does not exist")
                    _state.value = _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading health info: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Update height value
     */
    fun updateHeight(newHeight: String) {
        if (isReadOnly) return
        // Only allow numeric input
        if (newHeight.isEmpty() || newHeight.all { it.isDigit() || it == '.' }) {
            _state.value = _state.value.copy(height = newHeight)
        }
    }
    
    /**
     * Update weight value
     */
    fun updateWeight(newWeight: String) {
        if (isReadOnly) return
        // Only allow numeric input
        if (newWeight.isEmpty() || newWeight.all { it.isDigit() || it == '.' }) {
            _state.value = _state.value.copy(weight = newWeight)
        }
    }
    
    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        return _state.value.height != originalHeight || _state.value.weight != originalWeight
    }
    
    /**
     * Calculate age from date of birth
     * Supports both DD/MM/YYYY and YYYY-MM-DD formats
     */
    fun calculateAge(): Int? {
        val dob = _state.value.dateOfBirth ?: return null
        if (dob.isBlank()) return null
        
        return try {
            val (year, month, day) = if (dob.contains("/")) {
                // DD/MM/YYYY format (patient)
                val parts = dob.split("/")
                if (parts.size != 3) return null
                Triple(
                    parts[2].toIntOrNull() ?: return null,
                    parts[1].toIntOrNull() ?: return null,
                    parts[0].toIntOrNull() ?: return null
                )
            } else if (dob.contains("-")) {
                // YYYY-MM-DD format (dependent)
                val parts = dob.split("-")
                if (parts.size != 3) return null
                Triple(
                    parts[0].toIntOrNull() ?: return null,
                    parts[1].toIntOrNull() ?: return null,
                    parts[2].toIntOrNull() ?: return null
                )
            } else {
                return null
            }
            
            val today = Calendar.getInstance()
            val birthDate = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            
            var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            
            if (age < 0) null else age
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating age: ${e.message}")
            null
        }
    }
    
    /**
     * Save blood group (permanent, cannot be changed)
     */
    fun saveBloodGroup(bloodGroup: String) {
        if (isReadOnly) return
        val docRef = getDocumentRef() ?: return
        
        if (_state.value.isBloodGroupLocked) {
            Log.w(TAG, "Blood group is already locked")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            
            try {
                docRef.update(
                    mapOf(
                        "bloodGroup" to bloodGroup,
                        "bloodGroupSetAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                
                _state.value = _state.value.copy(
                    bloodGroup = bloodGroup,
                    isBloodGroupLocked = true,
                    isSaving = false
                )
                
                _events.emit(HealthInfoEvent.SaveSuccess)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving blood group: ${e.message}")
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message
                )
                _events.emit(HealthInfoEvent.Error(e.message ?: "Failed to save blood group"))
            }
        }
    }
    
    /**
     * Save height and weight changes
     */
    fun saveHeightAndWeight() {
        if (isReadOnly) return
        val docRef = getDocumentRef() ?: return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            
            try {
                val updates = mutableMapOf<String, Any>()
                val currentState = _state.value
                
                // Only update fields that have changed
                if (currentState.height != originalHeight) {
                    updates["height"] = currentState.height
                    updates["heightLastUpdated"] = FieldValue.serverTimestamp()
                }
                
                if (currentState.weight != originalWeight) {
                    updates["weight"] = currentState.weight
                    updates["weightLastUpdated"] = FieldValue.serverTimestamp()
                }
                
                if (updates.isNotEmpty()) {
                    docRef.update(updates).await()
                    
                    // Update original values
                    originalHeight = currentState.height
                    originalWeight = currentState.weight
                    
                    // Reload to get server timestamps
                    loadHealthInfo()
                    
                    _events.emit(HealthInfoEvent.SaveSuccess)
                }
                
                _state.value = _state.value.copy(isSaving = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving health info: ${e.message}")
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message
                )
                _events.emit(HealthInfoEvent.Error(e.message ?: "Failed to save"))
            }
        }
    }
    
    /**
     * Check if health profile is complete
     */
    fun isProfileComplete(): Boolean {
        val currentState = _state.value
        return !currentState.bloodGroup.isNullOrEmpty() &&
               currentState.height.isNotEmpty() &&
               currentState.weight.isNotEmpty()
    }
    
    /**
     * Format timestamp to readable date string
     */
    fun formatTimestamp(timestamp: Timestamp?): String? {
        if (timestamp == null) return null
        
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
    
    /**
     * Get the latest update timestamp (for doctor view)
     */
    fun getLatestUpdateTimestamp(): Timestamp? {
        val height = _state.value.heightLastUpdated
        val weight = _state.value.weightLastUpdated
        
        return when {
            height == null && weight == null -> null
            height == null -> weight
            weight == null -> height
            height.seconds > weight.seconds -> height
            else -> weight
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    /**
     * Factory for creating HealthInfoViewModel with parameters
     */
    class Factory(private val mode: HealthInfoMode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HealthInfoViewModel::class.java)) {
                return HealthInfoViewModel(mode) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
