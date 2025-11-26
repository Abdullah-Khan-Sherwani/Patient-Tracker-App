package com.example.patienttracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

object DependentRepository {
    private val db = Firebase.firestore

    suspend fun getDependentsForParent(parentUid: String): List<Dependent> {
        return try {
            val snapshot = db.collection("users").document(parentUid)
                .collection("dependents")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc -> Dependent.fromMap(doc.id, doc.data) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addDependent(parentUid: String, dependent: Dependent): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val data = dependent.toMap().toMutableMap()
            data["createdAt"] = dependent.createdAt
            data["updatedAt"] = dependent.updatedAt
            db.collection("users").document(parentUid)
                .collection("dependents")
                .document(id)
                .set(data)
                .await()
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun removeDependent(parentUid: String, dependentId: String): Result<Unit> {
        return try {
            db.collection("users").document(parentUid)
                .collection("dependents")
                .document(dependentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateDependent(parentUid: String, dependent: Dependent): Result<Unit> {
        return try {
            val data = dependent.toMap().toMutableMap()
            data["updatedAt"] = dependent.updatedAt
            db.collection("users").document(parentUid)
                .collection("dependents")
                .document(dependent.dependentId)
                .set(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
