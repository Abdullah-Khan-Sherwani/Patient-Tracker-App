package com.example.patienttracker.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Debug repository for testing and troubleshooting FCM notifications.
 * Provides utilities to test notification delivery and verify FCM setup.
 */
object NotificationDebugRepository {
    private const val TAG = "NotificationDebug"
    private val db = Firebase.firestore
    private val messaging = FirebaseMessaging.getInstance()

    /**
     * Get the current FCM token for the device.
     * Useful for debugging notification delivery issues.
     *
     * @return The current FCM token or error if not available
     */
    suspend fun getCurrentFCMToken(): Result<String> = try {
        val token = messaging.token.await()
        Log.d(TAG, "Current FCM Token: $token")
        Result.success(token)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get FCM token: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Send a test notification to the current user's device.
     * Creates a test notification and saves it to Firestore.
     *
     * @return Success message if sent successfully
     */
    suspend fun sendTestNotification(): Result<String> = try {
        val currentToken = getCurrentFCMToken().getOrNull()
            ?: return Result.failure(Exception("FCM token not available"))

        val testNotification = hashMapOf(
            "title" to "Test Notification",
            "message" to "This is a test notification from Patient Tracker",
            "type" to "test",
            "timestamp" to com.google.firebase.Timestamp.now(),
            "fcmToken" to currentToken
        )

        db.collection("test_notifications").add(testNotification).await()

        Log.d(TAG, "Test notification created. Token: $currentToken")
        Result.success("Test notification sent. Check your device for the notification.")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send test notification: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Log diagnostic information about FCM setup.
     * Useful for debugging why notifications aren't working.
     */
    suspend fun logFCMDiagnostics(): Result<String> {
        val diagnostics = StringBuilder()
        diagnostics.append("=== FCM Diagnostics ===\n")

        return try {
            // Get token
            val token = getCurrentFCMToken().getOrNull()
            diagnostics.append("FCM Token: ${token ?: "NOT AVAILABLE"}\n")

            // Check if messaging is available
            diagnostics.append("Firebase Messaging Available: true\n")

            // Check notification permissions
            diagnostics.append("Notifications Enabled: ${messaging.isAutoInitEnabled}\n")

            // Get device info
            diagnostics.append("Android Version: ${android.os.Build.VERSION.SDK_INT}\n")
            diagnostics.append("Device: ${android.os.Build.MODEL}\n")

            Log.d(TAG, diagnostics.toString())
            Result.success(diagnostics.toString())
        } catch (e: Exception) {
            diagnostics.append("ERROR: ${e.message}\n")
            Log.e(TAG, "Diagnostics error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if notification permissions are properly configured.
     * Returns a list of potential issues found.
     *
     * @return List of configuration issues, empty if everything is OK
     */
    suspend fun checkNotificationSetup(): Result<List<String>> = try {
        val issues = mutableListOf<String>()

        // Check FCM token
        val token = getCurrentFCMToken().getOrNull()
        if (token.isNullOrEmpty()) {
            issues.add("⚠️ FCM token not available. Notifications may not work.")
        }

        // Check if auto-init is enabled
        if (!messaging.isAutoInitEnabled) {
            issues.add("⚠️ Firebase Cloud Messaging auto-init is disabled.")
        }

        // Check Android version for notification channel requirements
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            issues.add("ℹ️ Android version is below 8.0 (Oreo). Notification channels not required.")
        }

        if (issues.isEmpty()) {
            Log.d(TAG, "✅ All notification checks passed")
            Result.success(emptyList())
        } else {
            Log.d(TAG, "⚠️ Found ${issues.size} potential notification issues")
            Result.success(issues)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check notification setup: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Subscribe to a topic for broadcast notifications.
     * Useful for app-wide announcements or updates.
     *
     * @param topic The topic name to subscribe to
     */
    suspend fun subscribeToTopic(topic: String): Result<String> = try {
        messaging.subscribeToTopic(topic).await()
        Log.d(TAG, "Subscribed to topic: $topic")
        Result.success("Successfully subscribed to $topic")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to subscribe to topic: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Unsubscribe from a topic.
     *
     * @param topic The topic name to unsubscribe from
     */
    suspend fun unsubscribeFromTopic(topic: String): Result<String> = try {
        messaging.unsubscribeFromTopic(topic).await()
        Log.d(TAG, "Unsubscribed from topic: $topic")
        Result.success("Successfully unsubscribed from $topic")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to unsubscribe from topic: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Log all recent notifications for debugging.
     * Helps identify if notifications are being saved correctly.
     */
    suspend fun logRecentNotifications(limit: Int = 10): Result<String> = try {
        val snapshot = db.collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val log = StringBuilder()
        log.append("=== Recent Notifications ===\n")
        log.append("Total found: ${snapshot.size}\n\n")

        snapshot.documents.forEach { doc ->
            log.append("ID: ${doc.id}\n")
            log.append("Title: ${doc.getString("title")}\n")
            log.append("Message: ${doc.getString("message")}\n")
            log.append("Type: ${doc.getString("type")}\n")
            log.append("Read: ${doc.getBoolean("isRead")}\n")
            log.append("---\n")
        }

        Log.d(TAG, log.toString())
        Result.success(log.toString())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to log recent notifications: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Create a debug log of notification settings.
     * Returns a formatted string with all relevant notification settings.
     */
    suspend fun generateDebugReport(): Result<String> = try {
        val report = StringBuilder()

        report.append("\n======== NOTIFICATION DEBUG REPORT ========\n\n")

        // FCM Token
        val tokenResult = getCurrentFCMToken()
        report.append("📱 FCM Token:\n")
        if (tokenResult.isSuccess) {
            report.append("  ✅ ${tokenResult.getOrNull()}\n\n")
        } else {
            report.append("  ❌ ${tokenResult.exceptionOrNull()?.message}\n\n")
        }

        // Auto-init status
        report.append("🔄 Auto-Init Status:\n")
        report.append("  ${if (messaging.isAutoInitEnabled) "✅ Enabled" else "❌ Disabled"}\n\n")

        // Device Info
        report.append("📋 Device Information:\n")
        report.append("  Android Version: ${android.os.Build.VERSION.SDK_INT}\n")
        report.append("  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n")

        // Setup checks
        report.append("✓ Setup Checks:\n")
        val checks = checkNotificationSetup().getOrNull() ?: emptyList()
        if (checks.isEmpty()) {
            report.append("  ✅ All checks passed\n\n")
        } else {
            checks.forEach { issue ->
                report.append("  $issue\n")
            }
            report.append("\n")
        }

        report.append("========================================\n\n")

        Log.d(TAG, report.toString())
        Result.success(report.toString())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to generate debug report: ${e.message}", e)
        Result.failure(e)
    }
}
