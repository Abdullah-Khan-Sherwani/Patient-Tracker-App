package com.example.patienttracker.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Firebase Storage operations
 * Handles file uploads/downloads/deletions for health records
 */
object StorageRepository {
    
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    /**
     * Upload a health record file to Firebase Storage
     * @param fileUri Local file URI
     * @param patientUid Patient's UID
     * @param fileName Original file name
     * @return Download URL of uploaded file
     */
    suspend fun uploadHealthRecord(
        fileUri: Uri,
        patientUid: String,
        fileName: String
    ): Result<String> {
        return try {
            val recordId = UUID.randomUUID().toString()
            val storagePath = "healthRecords/$patientUid/$recordId/$fileName"
            val storageRef: StorageReference = storage.reference.child(storagePath)
            
            // Upload file
            val uploadTask = storageRef.putFile(fileUri).await()
            
            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a health record file from Firebase Storage
     * @param fileUrl Download URL of the file to delete
     */
    suspend fun deleteHealthRecord(fileUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get download URL for a file
     * @param storagePath Path in Firebase Storage
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String> {
        return try {
            val storageRef = storage.reference.child(storagePath)
            val url = storageRef.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get file metadata
     * @param fileUrl Download URL
     */
    suspend fun getFileMetadata(fileUrl: String): Result<FileMetadata> {
        return try {
            val storageRef = storage.getReferenceFromUrl(fileUrl)
            val metadata = storageRef.metadata.await()
            
            Result.success(
                FileMetadata(
                    name = metadata.name ?: "",
                    size = metadata.sizeBytes,
                    contentType = metadata.contentType ?: "",
                    createdTime = metadata.creationTimeMillis
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    data class FileMetadata(
        val name: String,
        val size: Long,
        val contentType: String,
        val createdTime: Long
    )
}
