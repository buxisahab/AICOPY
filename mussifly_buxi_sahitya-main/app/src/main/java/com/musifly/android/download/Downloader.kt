package com.musifly.android.download

import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Multi-threaded Chunk Downloader
 * Replicates NewPipe's high-speed downloader logic using Range requests and RandomAccessFile.
 */
class Downloader(
    val url: String,
    val file: File,
    private val threadCount: Int = 2,
    private val onProgress: (downloaded: Long, total: Long) -> Unit
) {

    companion object {
        private const val TAG = "Downloader"
        private const val BUFFER_SIZE = 8192
        private const val MAX_RETRIES = 5  // Increased from 3 to 5 for better resilience
        private const val BASE_CONNECT_TIMEOUT = 30000
        private const val BASE_READ_TIMEOUT = 30000
    }

    private var totalSize: Long = 0
    private val downloadedSize = AtomicLong(0)
    private val isRunning = AtomicBoolean(false)
    private val threads = CopyOnWriteArrayList<Thread>()
    private val workers = CopyOnWriteArrayList<Worker>() // Promoted to class level
    private val error = AtomicReference<Exception?>(null)
    private val workerCompleted = AtomicInteger(0)
    private val stateFile by lazy { File(file.absolutePath + ".state") }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        error.set(null)
        workerCompleted.set(0)
        downloadedSize.set(0)
        workers.clear() // Clear previous workers

        try {
            // 1. Fetch content length
            totalSize = fetchContentLength()
            Log.d(TAG, "Content length: $totalSize bytes for ${file.name}")
            
            if (totalSize <= 0) {
                // Fallback to single thread if length unknown
                Log.w(TAG, "Unknown content length, falling back to single thread")
                downloadSingleThread()
                return
            }

            // 2. Prepare file
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()
            
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(totalSize)
            raf.close()

            // 3. Calculate chunks and load state
            val chunkSize = totalSize / threadCount
            val resumeStates = loadState()
            var initialDownloaded = 0L

            for (i in 0 until threadCount) {
                val start = i * chunkSize
                val end = if (i == threadCount - 1) totalSize - 1 else (start + chunkSize - 1)
                
                // Determine resume position
                val savedPos = resumeStates[i]
                val resumePos = if (savedPos != null && savedPos >= start && savedPos <= end + 1) {
                    savedPos
                } else {
                    start
                }
                
                initialDownloaded += (resumePos - start)
                workers.add(Worker(i, start, end, resumePos))
            }
            
            // Initialize downloaded size with what we already have
            downloadedSize.set(initialDownloaded)
            Log.d(TAG, "Resuming download. Already downloaded: $initialDownloaded bytes")

            // 4. Start threads
            workers.forEach { worker ->
                val t = thread(start = true, name = "Downloader-${worker.id}") {
                    worker.run()
                }
                threads.add(t)
            }

            // 5. Wait for completion
            threads.forEach { it.join() }

            val finalError = error.get()
            if (finalError != null) {
                Log.e(TAG, "Download failed with error from worker", finalError)
                throw finalError
            }
            
            val completedWorkers = workerCompleted.get()
            val downloaded = downloadedSize.get()
            
            if (completedWorkers < threadCount) {
                throw Exception("Download incomplete: Only $completedWorkers/$threadCount workers completed. Downloaded: $downloaded / $totalSize")
            }
            
            if (downloaded < totalSize) {
                // Check if file size matches what we downloaded
                val actualFileSize = file.length()
                Log.w(TAG, "Size mismatch - Downloaded: $downloaded, Expected: $totalSize, File size: $actualFileSize")
                
                // Allow small discrepancies (less than 1KB) due to rounding or server differences
                if (downloaded < totalSize - 1024) {
                    throw Exception("Download incomplete: $downloaded / $totalSize bytes (file size: $actualFileSize)")
                } else {
                    Log.d(TAG, "Small size discrepancy ignored: $downloaded vs $totalSize")
                }
            }
            
            // Success! Delete state file
            if (stateFile.exists()) {
                stateFile.delete()
            }
            
            Log.d(TAG, "Download finished: ${file.absolutePath} (${downloaded} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            stop() // This will save state
            throw e
        } finally {
            isRunning.set(false)
        }
    }

    fun stop() {
        isRunning.set(false)
        // Force close connections to stop downloading immediately
        workers.forEach { it.disconnect() }
        threads.forEach { it.interrupt() }
        threads.clear()
        saveState() // Save progress when stopped
    }
    
    private fun saveState() {
        try {
            if (workers.isEmpty()) return
            
            val sb = StringBuilder()
            workers.forEach { worker ->
                sb.append("${worker.id}:${worker.getCurrentPosition()}\n")
            }
            
            stateFile.writeText(sb.toString())
            Log.d(TAG, "Saved download state to ${stateFile.name}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save download state: ${e.message}")
        }
    }
    
    private fun loadState(): Map<Int, Long> {
        val states = mutableMapOf<Int, Long>()
        try {
            if (!stateFile.exists()) return states
            
            stateFile.readLines().forEach { line ->
                val parts = line.split(":")
                if (parts.size == 2) {
                    val id = parts[0].toIntOrNull()
                    val pos = parts[1].toLongOrNull()
                    if (id != null && pos != null) {
                        states[id] = pos
                    }
                }
            }
            Log.d(TAG, "Loaded download state for ${states.size} workers")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load download state: ${e.message}")
        }
        return states
    }

    private fun fetchContentLength(): Long {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Range", "bytes=0-0")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
        connection.connect()
        
        val responseCode = connection.responseCode
        if (responseCode != 200 && responseCode != 206) {
             connection.disconnect()
             throw Exception("HTTP $responseCode when fetching length")
        }
        
        // Content-Range: bytes 0-0/123456
        val contentRange = connection.getHeaderField("Content-Range")
        val length = if (contentRange != null) {
            contentRange.substringAfterLast('/').toLongOrNull() ?: connection.contentLengthLong
        } else {
            connection.contentLengthLong
        }
        
        connection.disconnect()
        
        if (length <= 0) {
            Log.e(TAG, "⚠️ Content-Length is 0 or unknown - URL may be expired or inaccessible")
            throw java.io.FileNotFoundException("Content-Length = 0 (URL expired or inaccessible)")
        }
        
        return length
    }

    private fun downloadSingleThread() {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
        
        // Simple resume support for single thread
        var startPos = 0L
        if (file.exists() && file.length() > 0) {
            startPos = file.length()
            connection.setRequestProperty("Range", "bytes=$startPos-")
            downloadedSize.set(startPos)
        }
        
        val input = BufferedInputStream(connection.inputStream)
        val output = java.io.FileOutputStream(file, true) // Append mode
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        
        while (isRunning.get()) {
            bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
            downloadedSize.addAndGet(bytesRead.toLong())
            onProgress(downloadedSize.get(), -1) // Total unknown
        }
        
        output.close()
        input.close()
        connection.disconnect()
    }

    private inner class Worker(val id: Int, val start: Long, val end: Long, val resumePos: Long) {
        // Volatile to ensure visibility across threads for state saving
        @Volatile private var currentPos = resumePos
        private var connection: HttpURLConnection? = null
        
        fun getCurrentPosition(): Long = currentPos
        
        fun disconnect() {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // Ignore errors during disconnect
            }
        }

        fun run() {
            var retry = 0
            var success = false
            val expectedBytes = end - start + 1
            var totalBytesDownloaded = 0L
            
            Log.d(TAG, "Worker $id starting: bytes $currentPos-$end (target: $start-$end)")
            
            // If already finished
            if (currentPos > end) {
                Log.d(TAG, "Worker $id already finished")
                workerCompleted.incrementAndGet()
                return
            }
            
            while (!success && retry < MAX_RETRIES && isRunning.get()) {
                var raf: RandomAccessFile? = null
                var input: BufferedInputStream? = null
                
                try {
                    // Adaptive timeout - increase with each retry
                    val connectTimeout = BASE_CONNECT_TIMEOUT + (retry * 10000)
                    val readTimeout = BASE_READ_TIMEOUT + (retry * 10000)
                    
                    Log.d(TAG, "Worker $id attempt ${retry + 1}/$MAX_RETRIES at position $currentPos (timeout: ${connectTimeout}ms)")
                    
                    connection = URL(url).openConnection() as HttpURLConnection
                    connection?.connectTimeout = connectTimeout
                    connection?.readTimeout = readTimeout
                    connection?.setRequestProperty("Range", "bytes=$currentPos-$end")
                    connection?.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
                    connection?.setRequestProperty("Accept", "*/*")
                    connection?.setRequestProperty("Connection", "keep-alive")
                    
                    val responseCode = connection?.responseCode ?: -1
                    Log.d(TAG, "Worker $id got response: $responseCode")
                    
                    if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                        throw Exception("HTTP $responseCode: Server doesn't support Range requests or returned error")
                    }
                    
                    // Verify the content length if available
                    val availableBytes = connection?.contentLengthLong ?: -1L
                    if (availableBytes > 0) {
                        val remainingBytes = end - currentPos + 1
                        Log.d(TAG, "Worker $id: Server reports $availableBytes bytes available, expecting $remainingBytes bytes")
                    }
                    
                    raf = RandomAccessFile(file, "rw")
                    raf.seek(currentPos)
                    
                    input = BufferedInputStream(connection?.inputStream, BUFFER_SIZE)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var chunkBytesRead = 0L
                    var lastProgressLog = System.currentTimeMillis()
                    
                    while (isRunning.get()) {
                        bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        
                        raf.write(buffer, 0, bytesRead)
                        currentPos += bytesRead
                        chunkBytesRead += bytesRead
                        totalBytesDownloaded += bytesRead
                        
                        val total = downloadedSize.addAndGet(bytesRead.toLong())
                        onProgress(total, totalSize)
                        
                        // Log progress every 5 seconds
                        val now = System.currentTimeMillis()
                        if (now - lastProgressLog > 5000) {
                            Log.d(TAG, "Worker $id progress: $chunkBytesRead/$expectedBytes bytes (${currentPos}/${end + 1})")
                            lastProgressLog = now
                        }
                        
                        // Check if we've gone beyond expected range
                        if (currentPos > end + 1) {
                            Log.w(TAG, "Worker $id exceeded range: $currentPos > ${end + 1}")
                            break
                        }
                    }
                    
                    // Verify completion
                    if (currentPos >= end + 1) {
                        Log.d(TAG, "Worker $id completed successfully: $chunkBytesRead bytes downloaded")
                        success = true
                    } else if (!isRunning.get()) {
                        // Manual stop - clean exit
                        Log.d(TAG, "Worker $id stopped manually")
                        break // Exit loop, don't retry
                    } else {
                        // Connection closed before completing
                        val remaining = end - currentPos + 1
                        Log.w(TAG, "Worker $id connection closed prematurely: $remaining bytes remaining")
                        // Don't set success, will retry
                    }
                    
                } catch (e: Exception) {
                    if (!isRunning.get()) {
                        // Ignore errors if we are stopping
                        Log.d(TAG, "Worker $id stopped manually (caught exception)")
                        break
                    }
                    
                    val remaining = end - currentPos + 1
                    Log.w(TAG, "Worker $id failed at position $currentPos (${remaining} bytes remaining), retry ${retry + 1}/$MAX_RETRIES: ${e.message}", e)
                    retry++
                    
                    if (retry < MAX_RETRIES && isRunning.get()) {
                        // Exponential backoff with jitter
                        val backoffMs = (1000 * retry) + (Math.random() * 500).toLong()
                        Log.d(TAG, "Worker $id backing off for ${backoffMs}ms before retry")
                        Thread.sleep(backoffMs)
                    }
                } finally {
                    // Always close resources
                    try {
                        input?.close()
                    } catch (e: Exception) {
                        // ignore
                    }
                    
                    try {
                        raf?.close()
                    } catch (e: Exception) {
                        // ignore
                    }
                    
                    try {
                        connection?.disconnect()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            
            if (!success && isRunning.get()) {
                val remaining = end - currentPos + 1
                val errorMsg = "Worker $id failed after $MAX_RETRIES retries. Downloaded $totalBytesDownloaded of ~$expectedBytes bytes (position: $currentPos/${end + 1}, remaining: $remaining bytes)"
                Log.e(TAG, errorMsg)
                error.compareAndSet(null, Exception(errorMsg))
            } else if (success) {
                workerCompleted.incrementAndGet()
                Log.d(TAG, "Worker $id finished successfully")
            }
        }
    }
}
