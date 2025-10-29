package dev.atick.data.repository.profile

import android.util.Log
import dev.atick.firebase.firestore.data.UserDataSource
import dev.atick.firebase.firestore.model.FirebaseAppUser
import dev.atick.data.model.profile.AppUser
import javax.inject.Inject

private fun FirebaseAppUser.toDomain() = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    role = role,
    approved = approvedAt != null
)

class UserRepository @Inject constructor(
    private val ds: UserDataSource
) {
    companion object {
        private const val TAG = "FirestoreDebug"
    }

    suspend fun ensure(uid: String, email: String?, name: String?): AppUser {
        Log.d(TAG, "➡ ensureUserDoc called | uid=$uid | email=$email | name=$name")

        return try {
            val user = ds.ensureUserDoc(uid, email.orEmpty(), name.orEmpty())
            Log.d(TAG, "✅ Firestore write SUCCESS for uid=$uid (doc=${user.uid})")
            user.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore write FAILED for uid=$uid | ${e.message}", e)
            throw e
        }
    }

    suspend fun get(uid: String): AppUser? {
        Log.d(TAG, "ℹ getUser called | uid=$uid")
        return try {
            ds.getUser(uid)?.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "❌ getUser FAILED for uid=$uid | ${e.message}", e)
            null
        }
    }

    suspend fun setRole(uid: String, role: String) {
        Log.d(TAG, "⚙ setRole called | uid=$uid | role=$role")
        try {
            ds.setRole(uid, role)
            Log.d(TAG, "✅ Role updated for uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "❌ setRole FAILED for uid=$uid | ${e.message}", e)
        }
    }
}
