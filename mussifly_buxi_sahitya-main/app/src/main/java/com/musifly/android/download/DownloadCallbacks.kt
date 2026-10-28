package com.musifly.android.download

/**
 * Callback interface for download progress and state changes.
 * Provides detailed callbacks for UI updates during the entire download lifecycle.
 */
interface DownloadCallbacks {
    
    /**
     * Called when the correct YouTube video is found via search
     * 
     * @param result The search result containing video URL and metadata
     */
    fun onCorrectVideoFound(result: YouTubeSearchEngine.SearchResult) {}
    
    /**
     * Called when stream extraction begins
     * 
     * @param videoUrl The YouTube video URL being processed
     */
    fun onStreamExtractionStarted(videoUrl: String) {}
    
    /**
     * Called when streams are successfully extracted
     * 
     * @param hasVideoStream True if video stream is available
     * @param hasAudioStream True if audio stream is available
     * @param requiresMerge True if video and audio need to be merged
     * @param videoQuality Video quality (e.g., "1080p", "720p")
     * @param audioQuality Audio quality (e.g., "128kbps", "256kbps")
     */
    fun onStreamsExtracted(
        hasVideoStream: Boolean,
        hasAudioStream: Boolean,
        requiresMerge: Boolean,
        videoQuality: String,
        audioQuality: String
    ) {}
    
    /**
     * Called when download mission is initialized
     * 
     * @param missionId Unique mission identifier
     * @param fileName Output file name
     * @param totalSize Total download size in bytes
     */
    fun onDownloadInitialized(missionId: String, fileName: String, totalSize: Long) {}
    
    /**
     * Called when download starts
     * 
     * @param missionId Mission identifier
     */
    fun onDownloadStarted(missionId: String) {}
    
    /**
     * Called during video download progress
     * 
     * @param downloadedBytes Bytes downloaded so far
     * @param totalBytes Total bytes to download
     * @param percentage Progress percentage (0-100)
     */
    fun onVideoDownloadProgress(downloadedBytes: Long, totalBytes: Long, percentage: Int) {}
    
    /**
     * Called during audio download progress
     * 
     * @param downloadedBytes Bytes downloaded so far
     * @param totalBytes Total bytes to download
     * @param percentage Progress percentage (0-100)
     */
    fun onAudioDownloadProgress(downloadedBytes: Long, totalBytes: Long, percentage: Int) {}
    
    /**
     * Called for overall download progress (video + audio combined)
     * 
     * @param downloadedBytes Total bytes downloaded (video + audio)
     * @param totalBytes Total bytes to download
     * @param percentage Overall progress percentage (0-100)
     */
    fun onOverallProgress(downloadedBytes: Long, totalBytes: Long, percentage: Int) {}
    
    /**
     * Called when video download completes
     */
    fun onVideoDownloadComplete() {}
    
    /**
     * Called when audio download completes
     */
    fun onAudioDownloadComplete() {}
    
    /**
     * Called when both downloads complete and merging starts
     * 
     * @param videoFile Path to downloaded video file
     * @param audioFile Path to downloaded audio file
     * @param outputFile Path to output merged file
     */
    fun onMergingStarted(videoFile: String, audioFile: String, outputFile: String) {}
    
    /**
     * Called during merge progress (if available)
     * 
     * @param percentage Merge progress percentage (0-100)
     */
    fun onMergingProgress(percentage: Int) {}
    
    /**
     * Called when merging completes successfully
     * 
     * @param outputFile Path to final merged file
     */
    fun onMergingComplete(outputFile: String) {}
    
    /**
     * Called when the entire download mission completes successfully
     * 
     * @param missionId Mission identifier
     * @param outputFile Path to final output file
     * @param fileSize Final file size in bytes
     */
    fun onComplete(missionId: String, outputFile: String, fileSize: Long) {}
    
    /**
     * Called when download is paused
     * 
     * @param missionId Mission identifier
     */
    fun onPaused(missionId: String) {}
    
    /**
     * Called when download is resumed
     * 
     * @param missionId Mission identifier
     */
    fun onResumed(missionId: String) {}
    
    /**
     * Called when download is cancelled
     * 
     * @param missionId Mission identifier
     */
    fun onCancelled(missionId: String) {}
    
    /**
     * Called when an error occurs
     * 
     * @param missionId Mission identifier (may be null if error before mission creation)
     * @param errorMessage Error description
     * @param exception The exception that occurred
     */
    fun onError(missionId: String?, errorMessage: String, exception: Exception?) {}
}

/**
 * Simple callback implementation with lambdas for common use cases
 */
class SimpleDownloadCallback(
    private val onProgress: ((Int) -> Unit)? = null,
    private val onComplete: ((String) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null
) : DownloadCallbacks {
    
    override fun onOverallProgress(downloadedBytes: Long, totalBytes: Long, percentage: Int) {
        onProgress?.invoke(percentage)
    }
    
    override fun onComplete(missionId: String, outputFile: String, fileSize: Long) {
        onComplete?.invoke(outputFile)
    }
    
    override fun onError(missionId: String?, errorMessage: String, exception: Exception?) {
        onError?.invoke(errorMessage)
    }
}
