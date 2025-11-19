package com.example.patienttracker.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ThemeViewModel(private val context: Context) : ViewModel() {
    private val sharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    // Initialize from saved preference or default to light mode
    val isDarkMode = mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", false))
    
    fun toggleDarkMode(isDark: Boolean) {
        isDarkMode.value = isDark
        // Persist preference
        sharedPreferences.edit().putBoolean("is_dark_mode", isDark).apply()
    }
}
