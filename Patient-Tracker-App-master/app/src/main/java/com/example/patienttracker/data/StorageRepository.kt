package com.example.patienttracker.data

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Firebase Storage operations
 * Handles file uploads/downloads/deletions for health records
 */
object StorageRepository {
    
    private const val TAG = "StorageRepository"
    // Use the bucket from google-services.json
    private val storage: FirebaseStorage by lazy {
        val instance = FirebaseStorage.getInstance()
        Log.d(TAG, "Storage instance bucket: ${instance.reference.bucket}")
        instance
    }
    
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
            Log.d(TAG, "=== UPLOAD DEBUG START ===")
            Log.d(TAG, "File URI: $fileUri")
            Log.d(TAG, "Patient UID: $patientUid")
            Log.d(TAG, "File name: $fileName")
            Log.d(TAG, "Storage bucket: ${storage.reference.bucket}")
            Log.d(TAG, "Storage reference path: ${storage.reference.path}")
            
            val recordId = UUID.randomUUID().toString()
            val storagePath = "healthRecords/$patientUid/$recordId/$fileName"
            Log.d(TAG, "Full storage path: $storagePath")
            
            val storageRef: StorageReference = storage.reference.child(storagePath)
            Log.d(TAG, "Storage ref bucket: ${storageRef.bucket}")
            Log.d(TAG, "Storage ref path: ${storageRef.path}")
            
            // Upload file with progress tracking
            Log.d(TAG, "Starting putFile operation...")
            val uploadTask = storageRef.putFile(fileUri)
            
            // Add progress listener for debugging
            uploadTask.addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                Log.d(TAG, "Upload progress: $progress% (${snapshot.bytesTransferred}/${snapshot.totalByteCount})")
            }
            
            val taskSnapshot = uploadTask.await()
            Log.d(TAG, "Upload complete! Bytes transferred: ${taskSnapshot.bytesTransferred}")
            
            // Get download URL
            Log.d(TAG, "Getting download URL...")
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Download URL obtained: $downloadUrl")
            Log.d(TAG, "=== UPLOAD DEBUG END - SUCCESS ===")
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "=== UPLOAD DEBUG END - FAILED ===")
            Log.e(TAG, "Upload failed: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                Log.e(TAG, "Cause: ${e.cause?.message}")
                Log.e(TAG, "Cause type: ${e.cause?.javaClass?.simpleName}")
            }
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
