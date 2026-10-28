package com.musifly.android.utils

import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.spotify.SpotifyPlaylistImporter
import com.musifly.android.spotify.models.ImportState as SpotifyImportState
import com.musifly.android.sync.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Spotify Import Manager
 * Orchestrates the import of Spotify playlists into the app
 * Uses the new SpotifyPlaylistImporter with Spotify API integration
 */
object SpotifyImportManager {
    
    private const val TAG = "SpotifyImportManager"
    
    /**
     * Import progress callback
     */
    interface ImportCallback {
        fun onProgress(current: Int, total: Int, currentTrack: String)
        fun onTrackImported(track: SpotifyPlaylistScraper.SpotifyTrack, success: Boolean)
        fun onComplete(playlist: PlaylistEntity, importedCount: Int, totalCount: Int)
        fun onError(error: String)
    }
    
    /**
     * Import result data class
     */
    data class ImportResult(
        val success: Boolean,
        val playlist: PlaylistEntity? = null,
        val importedCount: Int = 0,
        val totalCount: Int = 0,
        val errorMessage: String? = null,
        val failedTracks: List<SpotifyPlaylistScraper.SpotifyTrack> = emptyList()
    )
    
    /**
     * Import a Spotify playlist using the new SpotifyPlaylistImporter
     * 
     * @param playlistUrl The Spotify playlist URL or ID
     * @param database The app's database
     * @param playlistRepository Optional PlaylistRepository for Firestore sync
     * @param callback Progress callback
     * @return Import result
     */
    suspend fun importPlaylist(
        playlistUrl: String,
        database: MusicDatabase,
        playlistRepository: PlaylistRepository? = null,
        callback: ImportCallback? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("Starting import for: $playlistUrl")
            
            // Create the importer with PlaylistRepository for Firestore sync
            val importer = SpotifyPlaylistImporter(database, playlistRepository)
            
            // Track results
            var resultPlaylist: PlaylistEntity? = null
            var lastState: SpotifyImportState? = null
            
            // Start import and collect states
            importer.import(playlistUrl).collect { state ->
                lastState = state
                
                // Update progress
                callback?.onProgress(
                    current = state.importedItems,
                    total = state.totalItems,
                    currentTrack = state.message
                )
                
                // Check for completion
                if (state.isDone && !state.isFailed) {
                    // Get the created playlist using the ID from the state
                    if (state.playlistId != null) {
                        resultPlaylist = database.playlist(state.playlistId).firstOrNull()?.playlist
                        
                        Timber.tag(TAG).d("Import complete: ${state.importedItems}/${state.totalItems} tracks")
                        
                        resultPlaylist?.let { playlist ->
                            callback?.onComplete(
                                playlist = playlist,
                                importedCount = state.importedItems,
                                totalCount = state.totalItems
                            )
                        }
                    } else {
                        Timber.tag(TAG).w("Import completed but no playlist ID in state")
                    }
                }
                
                // Check for errors
                if (state.isFailed) {
                    val error = state.message
                    Timber.tag(TAG).e("Import failed: $error")
                    callback?.onError(error)
                }
            }
            
            // Return result based on last state
            if (lastState?.isDone == true && !lastState!!.isFailed) {
                ImportResult(
                    success = true,
                    playlist = resultPlaylist,
                    importedCount = lastState!!.importedItems,
                    totalCount = lastState!!.totalItems,
                    failedTracks = emptyList() // Failed tracks are tracked internally
                )
            } else {
                ImportResult(
                    success = false,
                    errorMessage = lastState?.message ?: "Import failed"
                )
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Import failed with exception")
            val error = e.message ?: "Unknown error occurred"
            callback?.onError(error)
            ImportResult(false, errorMessage = error)
        }
    }
}

