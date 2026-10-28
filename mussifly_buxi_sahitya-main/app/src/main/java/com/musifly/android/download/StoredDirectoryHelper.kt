package com.musifly.android.download

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Directory helper for managing download directories
 * Adapted from NewPipe's StoredDirectoryHelper
 */
class StoredDirectoryHelper(
    private val context: Context,
    private val uri: Uri,
    private val tag: String
) {
    
    private var directory: File? = null
    
    init {
        // Try to get directory from URI
        try {
            val path = uri.path
            if (path != null) {
                directory = File(path)
                if (!directory!!.exists()) {
                    directory!!.mkdirs()
                }
            }
        } catch (e: Exception) {
            // SAF uri, no direct file access
        }
    }
    
    /**
     * Create a new file in this directory
     */
    fun createFile(name: String, mimeType: String): StoredFileHelper? {
        return try {
            val file = File(directory, name)
            file.parentFile?.mkdirs()
            StoredFileHelper(file, tag)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a unique file in this directory
     */
    fun createUniqueFile(name: String, mimeType: String): StoredFileHelper? {
        var fileName = name
        var counter = 1
        
        while (File(directory, fileName).exists()) {
            val dotIndex = name.lastIndexOf('.')
            fileName = if (dotIndex > 0) {
                "${name.substring(0, dotIndex)}_$counter${name.substring(dotIndex)}"
            } else {
                "${name}_$counter"
            }
            counter++
        }
        
        return createFile(fileName, mimeType)
    }
    
    fun getUri(): Uri = uri
    
    fun getTag(): String = tag
    
    fun exists(): Boolean = directory?.exists() ?: false
    
    fun canWrite(): Boolean = directory?.canWrite() ?: false
}
