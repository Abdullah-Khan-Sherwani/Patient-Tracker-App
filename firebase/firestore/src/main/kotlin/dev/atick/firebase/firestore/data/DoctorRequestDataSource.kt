package dev.atick.firebase.firestore.data

import com.google.firebase.firestore.FirebaseFirestore
import dev.atick.firebase.firestore.model.FirebaseDoctorRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Firestore access for /doctor_requests collection.
 */
class DoctorRequestDataSource @Inject constructor(
    private val db: FirebaseFirestore,
    private val io: CoroutineDispatcher // use your existing @IoDispatcher binding
) {
    private val col get() = db.collection("doctor_requests")

    suspend fun create(req: FirebaseDoctorRequest): String = withContext(io) {
        val doc = if (req.id.isBlank()) col.document() else col.document(req.id)
        val now = System.currentTimeMillis()
        doc.set(req.copy(id = doc.id, createdAt = now)).await()
        doc.id
    }

    fun observePending(): Flow<List<FirebaseDoctorRequest>> = callbackFlow {
        val reg = col.whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(FirebaseDoctorRequest::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun setStatus(id: String, status: String) = withContext(io) {
        col.document(id).update(
            mapOf(
                "status" to status,
                "decidedAt" to System.currentTimeMillis()
            )
        ).await()
    }
}