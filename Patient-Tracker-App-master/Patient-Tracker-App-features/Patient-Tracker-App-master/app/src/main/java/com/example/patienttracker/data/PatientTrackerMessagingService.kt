package com.example.patienttracker.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.patienttracker.MainActivity
import com.example.patienttracker.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Messaging Service for handling push notifications.
 * Receives remote messages and displays them as system notifications.
 * Also saves notification tokens to user profile for targeted messaging.
 */
class PatientTrackerMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "patient_tracker_notifications"
        private const val CHANNEL_NAME = "Patient Tracker Notifications"
        private const val TAG = "PatientTrackerMessaging"
    }

    /**
     * Called when a new FCM token is generated.
     * Save it to the user's profile for targeted push notifications.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "FCM Token generated: $token")
        
        // Save token to user profile
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    Firebase.firestore.collection("users")
                        .document(currentUser.uid)
                        .update(mapOf("fcmToken" to token))
                        .await()
                    android.util.Log.d(TAG, "FCM token saved to user profile")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to save FCM token: ${e.message}", e)
            }
        }
    }

    /**
     * Handle incoming remote messages (push notifications).
     * Displays a system notification and optionally saves to Firestore.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle notification payloads
        remoteMessage.notification?.let {
            android.util.Log.d(TAG, "Message Notification Body: ${it.body}")
            
            val title = it.title ?: "Patient Tracker"
            val body = it.body ?: "You have a new notification"
            val appointmentId = remoteMessage.data["appointmentId"] ?: ""
            val notificationType = remoteMessage.data["type"] ?: "general"
            
            // Display system notification
            displayNotification(title, body, appointmentId, notificationType)
            
            // Optionally save to Firestore for in-app display
            saveNotificationToFirestore(title, body, notificationType, appointmentId)
        }

        // Handle data-only payloads (if notification payload is not present)
        if (remoteMessage.data.isNotEmpty()) {
            android.util.Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "Patient Tracker"
            val body = remoteMessage.data["message"] ?: "You have a new notification"
            val appointmentId = remoteMessage.data["appointmentId"] ?: ""
            val notificationType = remoteMessage.data["type"] ?: "general"
            
            if (remoteMessage.notification == null) {
                displayNotification(title, body, appointmentId, notificationType)
                saveNotificationToFirestore(title, body, notificationType, appointmentId)
            }
        }
    }

    /**
     * Display a system notification to the user.
     * Creates notification channel for Android 8+ (API 26+).
     */
    private fun displayNotification(
        title: String,
        body: String,
        appointmentId: String = "",
        type: String = "general"
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("appointmentId", appointmentId)
            putExtra("notificationType", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8+ (required for notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Patient Tracker"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notifications)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        // Display the notification (use a unique ID for each notification)
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        
        android.util.Log.d(TAG, "Notification displayed with ID: $notificationId")
    }

    /**
     * Save notification to Firestore so it appears in the in-app notification center.
     * Only saves if user is authenticated.
     */
    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        type: String,
        appointmentId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    val notification = hashMapOf(
                        "patientUid" to currentUser.uid,
                        "title" to title,
                        "message" to body,
                        "type" to type,
                        "appointmentId" to appointmentId,
                        "isRead" to false,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    Firebase.firestore.collection("notifications")
                        .add(notification)
                        .await()
                    
                    android.util.Log.d(TAG, "Notification saved to Firestore")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to save notification to Firestore: ${e.message}", e)
            }
        }
    }
}
