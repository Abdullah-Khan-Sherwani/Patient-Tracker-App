package dev.atick.data.repository.profile

import dev.atick.firebase.firestore.data.UserDataSource
import dev.atick.firebase.firestore.model.FirebaseAppUser
import dev.atick.data.model.profile.AppUser
import javax.inject.Inject

private fun FirebaseAppUser.toDomain() = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    role = role,
    approved = approvedAt != null // Always True???
)

class UserRepository @Inject constructor(
    private val ds: UserDataSource
) {
    suspend fun ensure(uid: String, email: String?, name: String?): AppUser =
        ds.ensureUserDoc(uid, email.orEmpty(), name.orEmpty()).toDomain()

    suspend fun get(uid: String): AppUser? =
        ds.getUser(uid)?.toDomain()

    suspend fun setRole(uid: String, role: String) {
        ds.setRole(uid, role)
    }
}
