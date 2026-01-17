package com.ez2bg.anotherthread

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object FileUploadService {
    // Allowed file extensions for upload
    private val allowedExtensions = setOf(
        // Documents
        "pdf", "doc", "docx", "txt", "rtf", "odt",
        // Images
        "jpg", "jpeg", "png", "gif", "webp", "svg", "bmp",
        // Data
        "json", "xml", "csv",
        // Archives
        "zip", "tar", "gz"
    )

    // Base directory for all files
    private val baseFileDir: File
        get() = File(System.getenv("FILE_DIR") ?: "data/files")

    // Directory for user uploads
    private val uploadsDir: File
        get() = File(baseFileDir, "uploads").also { it.mkdirs() }

    /**
     * Save an uploaded file to the uploads directory.
     * Returns the relative URL path to the saved file, or null if the file type is not allowed.
     */
    suspend fun saveUploadedFile(
        filename: String,
        fileBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val extension = filename.substringAfterLast('.', "").lowercase()

            if (extension.isEmpty() || extension !in allowedExtensions) {
                throw IllegalArgumentException("File type '.$extension' is not allowed. Allowed types: ${allowedExtensions.joinToString(", ") { ".$it" }}")
            }

            // Generate unique filename to avoid collisions
            val uniqueId = UUID.randomUUID().toString().take(8)
            val safeFilename = filename.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalFilename = "${uniqueId}_$safeFilename"

            val uploadFile = File(uploadsDir, finalFilename)
            uploadFile.writeBytes(fileBytes)

            // Return the relative path that can be served
            "/files/uploads/$finalFilename"
        }
    }

    /**
     * List all uploaded files
     */
    fun listUploadedFiles(): List<UploadedFileInfo> {
        if (!uploadsDir.exists()) return emptyList()
        return uploadsDir.listFiles()
            ?.filter { it.isFile }
            ?.map { file ->
                UploadedFileInfo(
                    filename = file.name,
                    url = "/files/uploads/${file.name}",
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    /**
     * Delete an uploaded file
     */
    fun deleteUploadedFile(filename: String): Boolean {
        val file = File(uploadsDir, filename)
        return if (file.exists() && file.parentFile == uploadsDir) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Check if a file extension is allowed
     */
    fun isExtensionAllowed(extension: String): Boolean {
        return extension.lowercase() in allowedExtensions
    }

    /**
     * Get the list of allowed extensions
     */
    fun getAllowedExtensions(): Set<String> = allowedExtensions
}

data class UploadedFileInfo(
    val filename: String,
    val url: String,
    val size: Long,
    val lastModified: Long
)
