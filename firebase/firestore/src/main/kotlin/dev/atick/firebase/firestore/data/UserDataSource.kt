package dev.atick.firebase.firestore.data

import com.google.firebase.firestore.FirebaseFirestore
import dev.atick.firebase.firestore.model.FirebaseAppUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Qualifier
import dev.atick.core.di.IoDispatcher

/**
 * Firestore access for /users collection.
 * NOTE: Use the SAME IO dispatcher qualifier you already use in this module
 * (e.g., @IoDispatcher). If your qualifier name differs, keep it consistent here.
 */
class UserDataSource @Inject constructor(
    private val db: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher // << issue Here
) {
    private val col get() = db.collection("users")

    suspend fun ensureUserDoc(
        uid: String,
        email: String,
        displayName: String
    ): FirebaseAppUser = withContext(io) {
        val ref = col.document(uid)
        val snap = ref.get().await()
        if (snap.exists()) {
            snap.toObject(FirebaseAppUser::class.java)!!.copy(uid = uid)
        } else {
            val now = System.currentTimeMillis()
            val fresh = FirebaseAppUser(
                uid = uid,
                email = email,
                displayName = displayName,
                role = "PATIENT",
                createdAt = now
            )
            ref.set(fresh).await()
            fresh
        }
    }

    suspend fun getUser(uid: String): FirebaseAppUser? = withContext(io) {
        col.document(uid).get().await().toObject(FirebaseAppUser::class.java)
    }

    suspend fun setRole(uid: String, role: String) = withContext(io) {
        val updates = mutableMapOf<String, Any?>(
            "role" to role,
            "approvedAt" to if (role == "DOCTOR") System.currentTimeMillis() else null
        )
        col.document(uid).update(updates).await()
    }
}
