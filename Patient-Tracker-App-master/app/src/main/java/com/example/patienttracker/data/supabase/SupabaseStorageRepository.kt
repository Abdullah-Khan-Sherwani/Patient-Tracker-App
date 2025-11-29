package com.example.patienttracker.data.supabase

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Supabase Storage operations
 * Handles file uploads for health records to Supabase Storage
 */
object SupabaseStorageRepository {
    
    private const val TAG = "SupabaseStorageRepo"
    private const val BUCKET_NAME = "medical-reports"
    
    /**
     * Upload a health record file to Supabase Storage
     * 
     * @param patientId Patient's UID
     * @param dependentId Optional dependent ID (null if uploading for self)
     * @param fileUri Local file URI
     * @param context Android context for content resolver
     * @return Result containing the public download URL on success
     */
    suspend fun uploadRecord(
        patientId: String,
        dependentId: String?,
        fileUri: Uri,
        context: Context
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== SUPABASE UPLOAD START ===")
            Log.d(TAG, "Patient ID: $patientId")
            Log.d(TAG, "Dependent ID: ${dependentId ?: "self"}")
            Log.d(TAG, "File URI: $fileUri")
            
            // Get file info from content resolver
            val contentResolver = context.contentResolver
            
            // Get file name and type
            var fileName = "file"
            var fileType = "application/octet-stream"
            
            contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "file"
                    }
                }
            }
            
            contentResolver.getType(fileUri)?.let { type ->
                fileType = type
            }
            
            // Generate unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val extension = getFileExtension(fileName, fileType)
            val uniqueFileName = "${fileName.substringBeforeLast(".")}_$timestamp.$extension"
            
            // Build folder path: /patientId/{dependentId or "self"}/filename
            val folderPath = if (dependentId.isNullOrBlank()) {
                "$patientId/self"
            } else {
                "$patientId/$dependentId"
            }
            val fullPath = "$folderPath/$uniqueFileName"
            
            Log.d(TAG, "File name: $fileName")
            Log.d(TAG, "File type: $fileType")
            Log.d(TAG, "Full storage path: $fullPath")
            
            // Read file bytes
            val inputStream = contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))
            
            val fileBytes = inputStream.use { it.readBytes() }
            Log.d(TAG, "File size: ${fileBytes.size} bytes")
            
            // Upload to Supabase Storage
            val storage = SupabaseProvider.client.storage
            val bucket = storage.from(BUCKET_NAME)
            
            Log.d(TAG, "Uploading to bucket: $BUCKET_NAME")
            
            bucket.upload(
                path = fullPath,
                data = fileBytes,
                upsert = false
            )
            
            Log.d(TAG, "Upload complete!")
            
            // Get public URL
            val publicUrl = bucket.publicUrl(fullPath)
            Log.d(TAG, "Public URL: $publicUrl")
            Log.d(TAG, "=== SUPABASE UPLOAD END - SUCCESS ===")
            
            Result.success(publicUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "=== SUPABASE UPLOAD END - FAILED ===")
            Log.e(TAG, "Upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a file from Supabase Storage
     * 
     * @param fileUrl Public URL of the file to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRecord(fileUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Extract path from URL
            // URL format: https://<project>.supabase.co/storage/v1/object/public/<bucket>/<path>
            val pathRegex = Regex("storage/v1/object/public/$BUCKET_NAME/(.+)")
            val match = pathRegex.find(fileUrl)
            val filePath = match?.groupValues?.get(1)
                ?: return@withContext Result.failure(Exception("Invalid file URL format"))
            
            Log.d(TAG, "Deleting file: $filePath")
            
            val storage = SupabaseProvider.client.storage
            val bucket = storage.from(BUCKET_NAME)
            
            bucket.delete(filePath)
            
            Log.d(TAG, "File deleted successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get file extension from filename or mime type
     */
    private fun getFileExtension(fileName: String, mimeType: String): String {
        // Try to get from filename first
        val fileExt = fileName.substringAfterLast(".", "")
        if (fileExt.isNotBlank() && fileExt.length <= 5) {
            return fileExt.lowercase()
        }
        
        // Fall back to mime type
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
    }
}
