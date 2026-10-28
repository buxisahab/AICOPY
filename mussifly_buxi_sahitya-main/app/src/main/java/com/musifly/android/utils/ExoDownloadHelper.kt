package com.musifly.android.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Helper for Android DownloadManager integration
 */
object ExoDownloadHelper {

    private const val TAG = "ExoDownloadHelper"

    /**
     * Download using Android DownloadManager (opens in system download manager)
     */
    suspend fun downloadWithManager(
        context: Context,
        url: String,
        title: String,
        description: String,
        isVideo: Boolean,
        onDownloadId: (Long) -> Unit
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // Validate URL
            if (url.isBlank()) {
                throw Exception("Stream URL is empty")
            }
            
            // Check if URL is valid
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw Exception("Invalid stream URL format: $url")
            }
            
            Timber.tag("ExoDownloadHelper").d("Download URL: $url")
            Timber.tag("ExoDownloadHelper").d("Title: $title")
            
            val fileName = if (isVideo) {
                "${title.replace("[^a-zA-Z0-9\\s-]".toRegex(), "")}.mp4"
            } else {
                "${title.replace("[^a-zA-Z0-9\\s-]".toRegex(), "")}.mp3"
            }
            
            val folderPath = if (isVideo) "Musifly/video" else "Musifly/audio"
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription(description)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$folderPath/$fileName")
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                
                // Set MIME type
                if (isVideo) {
                    setMimeType("video/mp4")
                } else {
                    setMimeType("audio/mpeg")
                }
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            
            Timber.tag(TAG).d("Download started with ID: $downloadId")
            onDownloadId(downloadId)
            
            downloadId
        }
    }

    /**
     * Get download status from DownloadManager
     */
    fun getDownloadStatus(context: Context, downloadId: Long): DownloadStatus? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            
            val status = cursor.getInt(statusIndex)
            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
            val bytesTotal = cursor.getLong(bytesTotalIndex)
            val reason = cursor.getInt(reasonIndex)
            
            cursor.close()
            
            return DownloadStatus(
                status = status,
                bytesDownloaded = bytesDownloaded,
                bytesTotal = bytesTotal,
                reason = reason,
                progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
            )
        }
        
        cursor.close()
        return null
    }

    /**
     * Copy cached file to downloads folder
     */
    suspend fun copyCachedFileToDownloads(
        context: Context,
        cacheFile: File,
        title: String,
        isVideo: Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = if (isVideo) {
                "${title.replace("[^a-zA-Z0-9\\s-]".toRegex(), "")}.mp4"
            } else {
                "${title.replace("[^a-zA-Z0-9\\s-]".toRegex(), "")}.mp3"
            }
            
            val folderPath = if (isVideo) "Musifly/video" else "Musifly/audio"
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                folderPath
            )
            
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, fileName)
            
            // Copy file
            cacheFile.inputStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Timber.tag(TAG).d("Copied cache file to: ${outputFile.absolutePath}")
            outputFile
        }
    }

    data class DownloadStatus(
        val status: Int,
        val bytesDownloaded: Long,
        val bytesTotal: Long,
        val reason: Int,
        val progress: Int
    ) {
        fun isSuccessful() = status == DownloadManager.STATUS_SUCCESSFUL
        fun isFailed() = status == DownloadManager.STATUS_FAILED
        fun isRunning() = status == DownloadManager.STATUS_RUNNING
        fun isPaused() = status == DownloadManager.STATUS_PAUSED
        fun isPending() = status == DownloadManager.STATUS_PENDING
    }
}
