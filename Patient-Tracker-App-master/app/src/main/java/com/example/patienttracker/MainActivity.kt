package com.example.patienttracker

import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.patienttracker.data.NotificationRepository
import com.example.patienttracker.ui.navigation.AppNavHost
import com.example.patienttracker.ui.theme.PatientTrackerTheme
import com.example.patienttracker.ui.viewmodel.ThemeViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Optional Firestore smoke test (debug builds only, without BuildConfig)
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            val db = Firebase.firestore
            db.collection("debug")
                .add(
                    mapOf(
                        "hello" to "world",
                        "launchedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener { Log.d(TAG, "✅ Firestore write OK") }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Firestore write failed", e) }
        }

        // Initialize FCM token when user is authenticated
        lifecycleScope.launch {
            if (Firebase.auth.currentUser != null) {
                try {
                    NotificationRepository().initializeFCMToken()
                    Log.d(TAG, "FCM token initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize FCM token", e)
                }
            }
        }

        // Initialize ThemeViewModel with app context
        val themeViewModel = ThemeViewModel(this)

        setContent {
            // Read isDarkMode from state - Compose will recompose when it changes
            val isDarkMode = themeViewModel.isDarkMode.value
            
            PatientTrackerTheme(darkTheme = isDarkMode) {
                AppNavHost(context = this@MainActivity, themeViewModel = themeViewModel)
            }
        }
    }

    companion object {
        private const val TAG = "Firestore"
    }
}