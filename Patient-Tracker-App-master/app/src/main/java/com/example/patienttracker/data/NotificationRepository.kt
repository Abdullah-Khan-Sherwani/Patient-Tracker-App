package com.example.patienttracker.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Request FCM token and save to user profile.
     * Call this once during app initialization.
     */
    suspend fun initializeFCMToken() {
        try {
            android.util.Log.d("NotificationRepo", "Initializing FCM token")
            
            val token = Firebase.messaging.token.await()
            android.util.Log.d("NotificationRepo", "FCM Token obtained: $token")
            
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                db.collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .await()
                android.util.Log.d("NotificationRepo", "FCM token saved to user profile")
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Failed to initialize FCM token: ${e.message}", e)
        }
    }
    
    /**
     * Subscribe user to a notification topic (for group notifications).
     * Topics allow sending notifications to groups of users.
     */
    suspend fun subscribeToTopic(topic: String) {
        try {
            Firebase.messaging.subscribeToTopic(topic).await()
            android.util.Log.d("NotificationRepo", "Subscribed to topic: $topic")
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Failed to subscribe to topic: ${e.message}", e)
        }
    }
    
    /**
     * Unsubscribe user from a notification topic.
     */
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            Firebase.messaging.unsubscribeFromTopic(topic).await()
            android.util.Log.d("NotificationRepo", "Unsubscribed from topic: $topic")
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Failed to unsubscribe from topic: ${e.message}", e)
        }
    }
    
    /**
     * Create a notification for a patient
     */
    suspend fun createNotification(
        patientUid: String,
        title: String,
        message: String,
        type: String,
        appointmentId: String = ""
    ): String {
        try {
            android.util.Log.d("NotificationRepo", "Creating notification for patient: $patientUid, title: $title")
            
            val notification = hashMapOf(
                "patientUid" to patientUid,
                "title" to title,
                "message" to message,
                "type" to type,
                "appointmentId" to appointmentId,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )
            
            val docRef = db.collection("notifications").add(notification).await()
            android.util.Log.d("NotificationRepo", "Notification created with ID: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Failed to create notification: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Get all notifications for a patient, ordered by newest first
     */
    suspend fun getPatientNotifications(patientUid: String): List<Notification> {
        try {
            android.util.Log.d("NotificationRepo", "Fetching notifications for patient: $patientUid")
            
            val snapshot = db.collection("notifications")
                .whereEqualTo("patientUid", patientUid)
                .get()
                .await()
            
            android.util.Log.d("NotificationRepo", "Found ${snapshot.size()} notifications")
            
            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    val notification = doc.data?.let { Notification.fromFirestore(it, doc.id) }
                    android.util.Log.d("NotificationRepo", "Parsed notification: ${notification?.title}")
                    notification
                } catch (e: Exception) {
                    android.util.Log.e("NotificationRepo", "Failed to parse notification ${doc.id}: ${e.message}", e)
                    null
                }
            }.sortedByDescending { it.createdAt }
            
            android.util.Log.d("NotificationRepo", "Returning ${notifications.size} notifications")
            return notifications
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error fetching notifications: ${e.message}", e)
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Get unread notification count for a patient
     */
    suspend fun getUnreadCount(patientUid: String): Int {
        try {
            android.util.Log.d("NotificationRepo", "Fetching unread count for patient: $patientUid")
            
            val snapshot = db.collection("notifications")
                .whereEqualTo("patientUid", patientUid)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            android.util.Log.d("NotificationRepo", "Unread count: ${snapshot.size()}")
            return snapshot.size()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepo", "Error fetching unread count: ${e.message}", e)
            e.printStackTrace()
            return 0
        }
    }
    
    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }
    
    /**
     * Mark all notifications as read for a patient
     */
    suspend fun markAllAsRead(patientUid: String) {
        val snapshot = db.collection("notifications")
            .whereEqualTo("patientUid", patientUid)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        
        val batch = db.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }
    
    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String) {
        db.collection("notifications")
            .document(notificationId)
            .delete()
            .await()
    }
}
