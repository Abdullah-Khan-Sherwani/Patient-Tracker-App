package dev.atick.data.repository.profile

import dev.atick.firebase.firestore.data.DoctorRequestDataSource
import dev.atick.firebase.firestore.model.FirebaseDoctorRequest
import dev.atick.data.model.profile.DoctorRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun FirebaseDoctorRequest.toDomain() = DoctorRequest(
    id = id,
    uid = uid,
    fullName = fullName,
    specialization = specialization,
    licenseNumber = licenseNumber,
    status = status
)

class DoctorRequestRepository @Inject constructor(
    private val ds: DoctorRequestDataSource
) {
    suspend fun create(req: DoctorRequest): String =
        ds.create(
            FirebaseDoctorRequest(
                id = req.id,
                uid = req.uid,
                fullName = req.fullName,
                specialization = req.specialization,
                licenseNumber = req.licenseNumber,
                status = req.status
            )
        )

    fun observePending(): Flow<List<DoctorRequest>> =
        ds.observePending().map { list -> list.map { it.toDomain() } }

    suspend fun approve(id: String) = ds.setStatus(id, "APPROVED")
    suspend fun reject(id: String)  = ds.setStatus(id, "REJECTED")
}
