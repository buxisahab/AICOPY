package com.musifly.android.download

import java.io.File
import java.io.Serializable

/**
 * Base class for download missions
 * Adapted from NewPipe's Mission class
 */
abstract class Mission(
    /**
     * Source URL of the resource
     */
    open var source: String? = null,
    
    /**
     * Length of the current resource
     */
    open var length: Long = 0,
    
    /**
     * Creation timestamp (and unique identifier)
     */
    open var timestamp: Long = 0,
    
    /**
     * Storage helper for the downloaded file
     */
    @Transient
    open var storage: StoredFileHelper? = null,

    /**
     * ID of the song in the database (YouTube ID or UUID)
     */
    open var songId: String? = null
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
    }

    abstract val finished: Boolean
    
    /**
     * Whether the mission is currently running
     */
    abstract val running: Boolean
    
    /**
     * Number of bytes downloaded
     */
    abstract val done: Long

    /**
     * Metadata file
     */
    @Transient
    var metadata: File? = null
    
    /**
     * Delete the downloaded file
     */
    open fun delete(): Boolean {
        metadata?.delete()
        return storage?.delete() ?: false
    }
    
    /**
     * Get download progress as percentage (0-100)
     */
    abstract fun getProgress(): Float
    
    /**
     * Indicate if this mission is deleted whatever is stored
     */
    fun isDeleted(): Boolean {
        return storage?.isInvalid() ?: true
    }
}
