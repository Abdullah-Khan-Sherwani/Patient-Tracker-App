package com.example.patienttracker.util

import android.util.Patterns

/**
 * Utility object for input validation across the app.
 */
object ValidationUtils {
    
    /**
     * Email validation pattern following RFC 5322 standard.
     * Also accepts Kotlin Android Patterns.EMAIL_ADDRESS for compatibility.
     */
    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )
    
    /**
     * Validate email format.
     * @param email The email string to validate
     * @return true if email format is valid, false otherwise
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        
        val trimmedEmail = email.trim()
        
        // First check with Android's built-in pattern
        if (Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return true
        }
        
        // Fallback to our regex
        return EMAIL_REGEX.matches(trimmedEmail)
    }
    
    /**
     * Validate phone number format.
     * Allows various formats with optional country code.
     */
    fun isValidPhone(phone: String): Boolean {
        if (phone.isBlank()) return true // Phone is optional in some forms
        
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        
        // Check for valid phone pattern (optional + followed by 10-15 digits)
        return cleaned.matches(Regex("^\\+?[0-9]{10,15}$"))
    }
    
    /**
     * Validate password strength.
     * @return Pair of (isValid, errorMessage)
     */
    fun validatePassword(password: String): Pair<Boolean, String?> {
        if (password.length < 8) {
            return false to "Password must be at least 8 characters"
        }
        if (!password.any { it.isUpperCase() }) {
            return false to "Password must contain at least one uppercase letter"
        }
        if (!password.any { it.isLowerCase() }) {
            return false to "Password must contain at least one lowercase letter"
        }
        if (!password.any { it.isDigit() }) {
            return false to "Password must contain at least one number"
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            return false to "Password must contain at least one special character"
        }
        return true to null
    }
    
    /**
     * Validate date of birth format (DD/MM/YYYY).
     */
    fun isValidDateOfBirth(dob: String): Boolean {
        if (dob.isBlank()) return false
        return dob.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))
    }
}
