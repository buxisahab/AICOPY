package com.musifly.android.download

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * COMPLETE INTEGRATION EXAMPLE
 * 
 * This file demonstrates how to integrate the Smart Music Downloader
 * into your Activity or Fragment with full UI callbacks.
 * 
 * Features Demonstrated:
 * 1. YouTube Search-based download (finds correct video automatically)
 * 2. Direct URL download
 * 3. Progress tracking with UI updates
 * 4. Error handling
 * 5. Download state management
 */

// ========== EXAMPLE 1: Activity Integration ==========

/**
 * Example: Download from Activity with Progress Updates
 * 
 * Usage in your Activity:
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Example download button click
 *         downloadButton.setOnClickListener {
 *             downloadSongExample(
 *                 context = this,
 *                 songTitle = "Shape of You",
 *                 artistName = "Ed Sheeran"
 *             )
 *         }
 *     }
 * }
 * ```
 */
fun androidx.fragment.app.FragmentActivity.downloadSongExample(
    context: Context,
    songTitle: String,
    artistName: String
) {
    lifecycleScope.launch {
        val downloader = SmartMusicDownloader(context)
        
        // Create detailed callbacks
        val callbacks = object : DownloadCallbacks {
            override fun onCorrectVideoFound(result: YouTubeSearchEngine.SearchResult) {
                Log.d("Download", "Found: ${result.title} by ${result.uploaderName}")
                runOnUiThread {
                    Toast.makeText(context, "Found: ${result.title}", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onStreamsExtracted(
                hasVideoStream: Boolean,
                hasAudioStream: Boolean,
                requiresMerge: Boolean,
                videoQuality: String,
                audioQuality: String
            ) {
                Log.d("Download", "Quality: $videoQuality video, $audioQuality audio")
                runOnUiThread {
                    Toast.makeText(
                        context,
                        "Quality: $videoQuality + $audioQuality",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            override fun onDownloadInitialized(missionId: String, fileName: String, totalSize: Long) {
                val sizeMB = totalSize / 1024 / 1024
                Log.d("Download", "Starting: $fileName ($sizeMB MB)")
                runOnUiThread {
                    Toast.makeText(context, "Downloading $sizeMB MB...", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onOverallProgress(downloadedBytes: Long, totalBytes: Long, percentage: Int) {
                Log.d("Download", "Progress: $percentage%")
                // Update UI progress bar
                runOnUiThread {
                    // progressBar.progress = percentage
                    // progressText.text = "$percentage%"
                }
            }
            
            override fun onMergingStarted(videoFile: String, audioFile: String, outputFile: String) {
                Log.d("Download", "Merging video and audio...")
                runOnUiThread {
                    Toast.makeText(context, "Processing...", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onComplete(missionId: String, outputFile: String, fileSize: Long) {
                Log.d("Download", "Complete: $outputFile")
                runOnUiThread {
                    Toast.makeText(context, "Download Complete!", Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onError(missionId: String?, errorMessage: String, exception: Exception?) {
                Log.e("Download", "Error: $errorMessage", exception)
                runOnUiThread {
                    Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Start download with callbacks
        val missionId = downloader.downloadSong(
            songTitle = songTitle,
            artistName = artistName,
            videoQuality = SmartMusicDownloader.VideoQuality.Q_1080P,
            audioQuality = SmartMusicDownloader.AudioQuality.VERY_HIGH,
            callbacks = callbacks
        )
        
        if (missionId != null) {
            Log.d("Download", "Mission started: $missionId")
        } else {
            Log.e("Download", "Failed to start download")
        }
    }
}

// ========== EXAMPLE 2: Simple Download with Minimal Callbacks ==========

/**
 * Example: Simple download with lambda callbacks
 * 
 * Usage:
 * ```kotlin
 * downloadSongSimple(
 *     context = this,
 *     songTitle = "Blinding Lights",
 *     artistName = "The Weeknd"
 * )
 * ```
 */
fun androidx.fragment.app.FragmentActivity.downloadSongSimple(
    context: Context,
    songTitle: String,
    artistName: String
) {
    lifecycleScope.launch {
        val downloader = SmartMusicDownloader(context)
        
        // Simple callback with lambdas
        val callbacks = SimpleDownloadCallback(
            onProgress = { percentage ->
                runOnUiThread {
                    // Update progress bar
                    // progressBar.progress = percentage
                    Log.d("Download", "Progress: $percentage%")
                }
            },
            onComplete = { filePath ->
                runOnUiThread {
                    Toast.makeText(context, "Downloaded: $filePath", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
        
        downloader.downloadSong(
            songTitle = songTitle,
            artistName = artistName,
            callbacks = callbacks
        )
    }
}

// ========== EXAMPLE 3: Download from Direct YouTube URL ==========

/**
 * Example: Download directly from YouTube URL (no search needed)
 * 
 * Usage:
 * ```kotlin
 * downloadFromUrlExample(
 *     context = this,
 *     videoUrl = "https://youtube.com/watch?v=dQw4w9WgXcQ",
 *     title = "Never Gonna Give You Up"
 * )
 * ```
 */
fun androidx.fragment.app.FragmentActivity.downloadFromUrlExample(
    context: Context,
    videoUrl: String,
    title: String
) {
    lifecycleScope.launch {
        val downloader = SmartMusicDownloader(context)
        
        val callbacks = SimpleDownloadCallback(
            onProgress = { percentage ->
                Log.d("Download", "$title: $percentage%")
            },
            onComplete = { filePath ->
                runOnUiThread {
                    Toast.makeText(context, "Saved: $filePath", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }
        )
        
        downloader.downloadFromUrl(
            videoUrl = videoUrl,
            title = title,
            videoQuality = SmartMusicDownloader.VideoQuality.Q_720P,
            callbacks = callbacks
        )
    }
}

// ========== EXAMPLE 4: Download with Quality Selection Dialog ==========

/**
 * Example: Show quality selection dialog before download
 * 
 * Usage:
 * ```kotlin
 * showQualitySelectionDialog(
 *     context = this,
 *     songTitle = "Bohemian Rhapsody",
 *     artistName = "Queen"
 * )
 * ```
 */
fun androidx.fragment.app.FragmentActivity.showQualitySelectionDialog(
    context: Context,
    songTitle: String,
    artistName: String
) {
    // Quality options
    val qualities = SmartMusicDownloader.VideoQuality.values()
    val qualityNames = qualities.map { it.displayName }.toTypedArray()
    
    // Show dialog
    androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle("Select Video Quality")
        .setItems(qualityNames) { _, which ->
            val selectedQuality = qualities[which]
            
            // Start download with selected quality
            lifecycleScope.launch {
                val downloader = SmartMusicDownloader(context)
                downloader.downloadSong(
                    songTitle = songTitle,
                    artistName = artistName,
                    videoQuality = selectedQuality,
                    callbacks = SimpleDownloadCallback(
                        onProgress = { percentage ->
                            Log.d("Download", "$percentage%")
                        },
                        onComplete = { filePath ->
                            runOnUiThread {
                                Toast.makeText(context, "Complete!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onError = { error ->
                            runOnUiThread {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                )
            }
        }
        .show()
}

// ========== EXAMPLE 5: Batch Download Multiple Songs ==========

/**
 * Example: Download multiple songs with progress tracking
 * 
 * Usage:
 * ```kotlin
 * downloadPlaylist(
 *     context = this,
 *     songs = listOf(
 *         "Shape of You" to "Ed Sheeran",
 *         "Blinding Lights" to "The Weeknd",
 *         "Levitating" to "Dua Lipa"
 *     )
 * )
 * ```
 */
fun androidx.fragment.app.FragmentActivity.downloadPlaylist(
    context: Context,
    songs: List<Pair<String, String>>
) {
    lifecycleScope.launch {
        val downloader = SmartMusicDownloader(context)
        var completed = 0
        val total = songs.size
        
        songs.forEach { (title, artist) ->
            val callbacks = SimpleDownloadCallback(
                onComplete = {
                    completed++
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Downloaded $completed/$total: $title",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Failed: $title - $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            
            downloader.downloadSong(
                songTitle = title,
                artistName = artist,
                videoQuality = SmartMusicDownloader.VideoQuality.Q_720P,
                callbacks = callbacks
            )
        }
    }
}

// ========== EXAMPLE 6: Search Preview Before Download ==========

/**
 * Example: Show search results before downloading
 * 
 * Usage:
 * ```kotlin
 * searchAndPreviewExample(
 *     context = this,
 *     songTitle = "Imagine",
 *     artistName = "John Lennon"
 * )
 * ```
 */
fun androidx.fragment.app.FragmentActivity.searchAndPreviewExample(
    context: Context,
    songTitle: String,
    artistName: String
) {
    lifecycleScope.launch {
        // Search for video
        val searchResult = YouTubeSearchEngine.findCorrectVideoWithFallback(
            songTitle = songTitle,
            artistName = artistName
        )
        
        if (searchResult == null) {
            runOnUiThread {
                Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }
        
        // Show preview dialog
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Download Confirmation")
                .setMessage("""
                    Title: ${searchResult.title}
                    Uploader: ${searchResult.uploaderName}
                    Duration: ${searchResult.duration / 60}:${searchResult.duration % 60}
                    Views: ${searchResult.viewCount / 1_000_000}M
                    Verified: ${if (searchResult.isVerified) "✓" else "✗"}
                """.trimIndent())
                .setPositiveButton("Download") { _, _ ->
                    // Start download
                    lifecycleScope.launch {
                        val downloader = SmartMusicDownloader(context)
                        downloader.downloadFromUrl(
                            videoUrl = searchResult.videoUrl,
                            title = searchResult.title,
                            callbacks = SimpleDownloadCallback(
                                onComplete = {
                                    runOnUiThread {
                                        Toast.makeText(context, "Complete!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        )
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
