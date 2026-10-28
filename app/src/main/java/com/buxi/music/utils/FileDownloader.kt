package com.buxi.music.utils

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.buxi.music.utils.Toast
import androidx.core.app.NotificationCompat
import com.buxi.music.R
import com.buxi.music.db.MusicDatabase
import com.buxi.music.db.entities.DeviceDownloadEntity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object FileDownloader {
    
    private const val NOTIFICATION_CHANNEL_ID = "device_download_channel"
    private const val NOTIFICATION_ID = 2000
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Device Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of device downloads"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    suspend fun downloadFile(
        context: Context,
        fileUrl: String,
        fileName: String,
        songId: String,
        database: MusicDatabase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        maxRetries: Int = 2
    ) = withContext(Dispatchers.IO) {
        var notificationId = NOTIFICATION_ID + songId.hashCode()
        createNotificationChannel(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        var retryCount = 0
        var lastError: String? = null
        
        while (retryCount <= maxRetries) {
            try {
                if (retryCount > 0) {
                    android.util.Log.d("FileDownloader", "Retry attempt $retryCount/$maxRetries")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Retrying download... (Attempt ${retryCount + 1}/${maxRetries + 1})", Toast.LENGTH_SHORT).show()
                    }
                    delay((2000 * retryCount).toLong()) // Exponential backoff
                }
                
                downloadFileInternal(
                    context, fileUrl, fileName, songId, database, 
                    notificationId, notificationManager, onSuccess, onError
                )
                return@withContext // Success, exit
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                android.util.Log.e("FileDownloader", "Download attempt ${retryCount + 1} failed: $lastError", e)
                retryCount++
                
                if (retryCount > maxRetries) {
                    // All retries exhausted
                    notificationManager.cancel(notificationId)
                    withContext(Dispatchers.Main) {
                        onError(lastError)
                        Toast.makeText(
                            context,
                            "Download failed after ${maxRetries + 1} attempts: $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private suspend fun downloadFileInternal(
        context: Context,
        fileUrl: String,
        fileName: String,
        songId: String,
        database: MusicDatabase,
        notificationId: Int,
        notificationManager: NotificationManager,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            // Create Buxi_music directory in Downloads if it doesn't exist
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Buxi_music"
            )
            
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Create a unique filename with timestamp
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val cleanFileName = fileName.replace("[^a-zA-Z0-9\\s]".toRegex(), "_")
            val outputFileName = "${cleanFileName}_${timeStamp}_Buxi_Music.mp3"
            val outputFile = File(downloadDir, outputFileName)
            
            // Download the file with progress
            android.util.Log.d("FileDownloader", "Starting download: $fileName")
            android.util.Log.d("FileDownloader", "URL: ${fileUrl.take(100)}...")
            android.util.Log.d("FileDownloader", "Output: ${outputFile.absolutePath}")
            
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 60000 // Increased to 60 seconds
            connection.readTimeout = 120000 // Increased to 120 seconds for large files
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Encoding", "identity") // Prevent compression issues
            connection.setRequestProperty("Connection", "keep-alive")
            connection.connect()
            
            val responseCode = connection.responseCode
            android.util.Log.d("FileDownloader", "HTTP Response: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                connection.disconnect()
                throw Exception("Server error: HTTP $responseCode")
            }
            
            val fileLength = connection.contentLength
            android.util.Log.d("FileDownloader", "Content-Length: $fileLength bytes (${String.format("%.2f", fileLength / (1024.0 * 1024.0))} MB)")
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)
            
            val buffer = ByteArray(16 * 1024) // Increased buffer size for faster downloads
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgress = 0
            var lastUpdateTime = System.currentTimeMillis()
            
            // Show initial notification
            val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.download)
                .setContentTitle("Downloading to Device")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setProgress(100, 0, fileLength <= 0)
            
            notificationManager.notify(notificationId, notificationBuilder.build())
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                // Update progress notification (throttled to every 2 seconds or 5% progress)
                val currentTime = System.currentTimeMillis()
                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    if ((progress != lastProgress && progress % 5 == 0) || (currentTime - lastUpdateTime > 2000)) {
                        lastProgress = progress
                        lastUpdateTime = currentTime
                        val downloadedMB = String.format("%.1f", totalBytesRead / (1024.0 * 1024.0))
                        val totalMB = String.format("%.1f", fileLength / (1024.0 * 1024.0))
                        notificationBuilder
                            .setProgress(100, progress, false)
                            .setContentText("$progress% ($downloadedMB MB / $totalMB MB) - $fileName")
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
                } else {
                    // If fileLength is unknown, update every 2 seconds with downloaded size
                    if (currentTime - lastUpdateTime > 2000) {
                        lastUpdateTime = currentTime
                        val downloadedMB = String.format("%.1f", totalBytesRead / (1024.0 * 1024.0))
                        notificationBuilder
                            .setProgress(100, 0, true) // Indeterminate progress
                            .setContentText("Downloaded: $downloadedMB MB - $fileName")
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
                }
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            // Verify file was downloaded completely
            // Only check if fileLength is known and reasonable (> 0)
            if (fileLength > 0 && totalBytesRead < fileLength * 0.95) {
                // Allow 5% tolerance for header/metadata differences
                outputFile.delete()
                val expectedMB = String.format("%.2f", fileLength / (1024.0 * 1024.0))
                val gotMB = String.format("%.2f", totalBytesRead / (1024.0 * 1024.0))
                throw Exception("Download incomplete: Expected ${expectedMB} MB, got ${gotMB} MB")
            }
            
            // Additional verification: Check if file size is reasonable (at least 100KB for a song)
            if (totalBytesRead < 100 * 1024) {
                outputFile.delete()
                throw Exception("File too small (${totalBytesRead} bytes). This may not be a valid audio file.")
            }
            
            // Verify file has content
            if (totalBytesRead == 0L || !outputFile.exists() || outputFile.length() == 0L) {
                outputFile.delete()
                throw Exception("Downloaded file is empty")
            }
            
            // Notify the media scanner about the new file
            val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(outputFile)
            context.sendBroadcast(mediaScanIntent)
            
            // Save download record to database
            database.upsertDeviceDownload(
                DeviceDownloadEntity(
                    songId = songId,
                    filePath = outputFile.absolutePath,
                    downloadedAt = System.currentTimeMillis()
                )
            )
            
            // Log download success
            val fileSizeMB = String.format("%.2f", totalBytesRead / (1024.0 * 1024.0))
            android.util.Log.d("FileDownloader", "Download successful: $fileName ($fileSizeMB MB) -> ${outputFile.absolutePath}")
            
            // Show success notification
            notificationBuilder
                .setContentTitle("Download Complete")
                .setContentText("$fileName ($fileSizeMB MB)")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
            notificationManager.notify(notificationId, notificationBuilder.build())
            
            withContext(Dispatchers.Main) {
                onSuccess()
                Toast.makeText(
                    context,
                    "✓ Download complete!\nSong: $fileName\nSize: $fileSizeMB MB\nSaved to: Downloads/Buxi_music/",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cancel notification on error
            notificationManager.cancel(notificationId)
            // Re-throw to trigger retry mechanism
            throw e
        }
    }
    
    // Alternative method using Android's DownloadManager
    fun downloadWithDownloadManager(context: Context, fileUrl: String, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // Create Buxi_music directory in Downloads if it doesn't exist
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Buxi_music"
            )
            
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Create a unique filename with timestamp
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFileName = "${fileName}_${timeStamp}_Buxi_Music.mp3"
            
            val request = DownloadManager.Request(Uri.parse(fileUrl))
                .setTitle(fileName)
                .setDescription("Downloading $fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    "${Environment.DIRECTORY_DOWNLOADS}/Buxi_music",
                    outputFileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            downloadManager.enqueue(request)
            
            Toast.makeText(
                context,
                context.getString(R.string.download_started),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.download_failed, e.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
