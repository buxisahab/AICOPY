package com.musifly.android.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.OutputStream
import java.io.Serializable

/**
 * Simplified storage helper for downloaded files
 * Supports both legacy file access and modern MediaStore API for Android 10+
 */
class StoredFileHelper : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
        const val INVALID_TAG = "invalid"
        private const val TAG = "StoredFileHelper"
    }
    
    private var uri: Uri? = null
    private var file: File? = null
    private var tag: String = ""
    private var invalid = false
    @Transient
    private var context: Context? = null
    
    /**
     * Create from File (legacy)
     */
    constructor(file: File, tag: String = "") {
        this.file = file
        this.tag = tag
        this.uri = Uri.fromFile(file)
    }
    
    /**
     * Create from Uri (SAF or MediaStore)
     * For content:// URIs (MediaStore/SAF), we use ContentResolver for all operations
     * and do NOT try to access them as direct files
     */
    constructor(context: Context, uri: Uri, tag: String = "") {
        this.context = context
        this.uri = uri
        this.tag = tag
        // Only try to get file path for file:// URIs, NOT for content:// URIs
        // MediaStore URIs (content://media/...) should only be accessed via ContentResolver
        if (uri.scheme == "file") {
            try {
                val path = uri.path
                if (path != null) {
                    this.file = File(path)
                }
            } catch (e: Exception) {
                // No direct file access
            }
        }
        // For content:// URIs, file stays null and we use ContentResolver
    }
    
    fun getName(): String {
        return file?.name ?: uri?.lastPathSegment?.substringAfterLast('/') ?: "unknown"
    }
    
    fun getUri(): Uri {
        return uri ?: Uri.EMPTY
    }
    
    fun getTag(): String = tag
    
    fun length(): Long {
        // Try direct file access first
        file?.let {
            if (it.exists()) return it.length()
        }
        
        // Try using ContentResolver
        context?.let { ctx ->
            uri?.let { u ->
                try {
                    ctx.contentResolver.openFileDescriptor(u, "r")?.use { pfd ->
                        return pfd.statSize
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting file length", e)
                }
            }
        }
        
        return 0L
    }
    
    fun existsAsFile(): Boolean {
        // Try direct file access first
        file?.let {
            if (it.exists()) return true
        }
        
        // Try using ContentResolver
        context?.let { ctx ->
            uri?.let { u ->
                try {
                    ctx.contentResolver.openFileDescriptor(u, "r")?.use {
                        return true
                    }
                } catch (e: Exception) {
                    // File doesn't exist or no access
                }
            }
        }
        
        return false
    }
    
    fun delete(): Boolean {
        // Try direct file deletion first
        file?.let {
            if (it.exists() && it.delete()) {
                return true
            }
        }
        
        // Try using ContentResolver
        context?.let { ctx ->
            uri?.let { u ->
                try {
                    val deleted = ctx.contentResolver.delete(u, null, null)
                    return deleted > 0
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file", e)
                }
            }
        }
        
        return false
    }
    
    fun isInvalid(): Boolean = invalid
    
    fun invalidate() {
        invalid = true
        tag = INVALID_TAG
    }
    
    /**
     * Get OutputStream for writing to this file
     * Works with both direct file access and MediaStore/SAF
     */
    fun openOutputStream(): OutputStream? {
        // Try direct file access first (for legacy or app-specific storage)
        file?.let {
            try {
                return it.outputStream()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open direct output stream", e)
            }
        }
        
        // Try using ContentResolver (for MediaStore/SAF)
        context?.let { ctx ->
            uri?.let { u ->
                try {
                    return ctx.contentResolver.openOutputStream(u)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open ContentResolver output stream", e)
                }
            }
        }
        
        return null
    }
    
    fun create(): Boolean {
        if (file == null) return false
        return try {
            // Ensure parent directory exists
            val parent = file!!.parentFile
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    Log.e(TAG, "Failed to create parent directory: ${parent.absolutePath}")
                    return false
                }
            }
            
            // Check if parent is writable
            if (parent != null && !parent.canWrite()) {
                Log.e(TAG, "Parent directory is not writable: ${parent.absolutePath}")
                return false
            }
            
            // If file exists, return true (file already created)
            if (file!!.exists()) {
                Log.d(TAG, "File already exists: ${file!!.absolutePath}")
                return true
            }
            
            // Create new file
            val created = file!!.createNewFile()
            if (!created) {
                Log.e(TAG, "createNewFile() returned false for: ${file!!.absolutePath}")
            }
            created
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating file: ${file!!.absolutePath}", e)
            false
        }
    }
    
    fun isDirect(): Boolean = file != null
    
    /**
     * Set context (needed after deserialization since context is transient)
     */
    fun setContext(ctx: Context) {
        this.context = ctx
    }
    
    /**
     * Finalize the download - clears IS_PENDING flag for MediaStore files on Android 10+
     * This makes the file visible to other apps and file managers
     */
    fun finalizeDownload(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri?.let { u ->
                try {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    val updated = ctx.contentResolver.update(u, contentValues, null, null)
                    Log.d(TAG, "Finalized download, cleared IS_PENDING flag: $updated rows updated for $u")
                    return updated > 0
                } catch (e: Exception) {
                    Log.e(TAG, "Error finalizing download (clearing IS_PENDING)", e)
                }
            }
        }
        // For direct file access or older Android, nothing special needed
        return true
    }
    
    /**
     * Copy content from a source file to this storage location
     * Works with both direct file access and MediaStore/SAF URIs
     */
    fun copyFromFile(sourceFile: java.io.File): Boolean {
        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file does not exist: ${sourceFile.absolutePath}")
            return false
        }
        
        try {
            val outputStream = openOutputStream()
            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream for copy")
                return false
            }
            
            outputStream.use { out ->
                sourceFile.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                    Log.d(TAG, "Copied $totalBytes bytes from ${sourceFile.name}")
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file", e)
            return false
        }
    }
    
    fun getType(): String {
        val name = getName()
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0) {
            name.substring(dotIndex + 1)
        } else {
            "mp4"
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoredFileHelper) return false
        return uri == other.uri && file == other.file
    }
    
    override fun hashCode(): Int {
        var result = uri?.hashCode() ?: 0
        result = 31 * result + (file?.hashCode() ?: 0)
        return result
    }
    
    override fun toString(): String {
        return "StoredFileHelper(name=${getName()}, uri=$uri, file=$file)"
    }
}
