package com.musifly.android.spotify

import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.models.MediaMetadata
import com.musifly.android.spotify.api.SpotifyApi
import com.musifly.android.spotify.matcher.YouTubeMatcher
import com.musifly.android.spotify.models.ImportState
import com.musifly.android.spotify.utils.SpotifyUrlExtractor
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

/**
 * SpotifyPlaylistImporter - Main class for importing Spotify playlists
 * Equivalent to Bloomee's ExternalMediaImporter.sfyPlaylistImporter()
 * 
 * Integrated with OpenTune's Room database and Firestore sync
 * 
 * Usage:
 * ```
 * val importer = SpotifyPlaylistImporter(database, playlistRepository)
 * importer.import(spotifyUrl).collect { state ->
 *     // Update UI with state
 *     println("${state.importedItems}/${state.totalItems}: ${state.message}")
 * }
 * ```
 */
class SpotifyPlaylistImporter(
    private val database: MusicDatabase,
    private val playlistRepository: PlaylistRepository? = null,
    private val spotifyApi: SpotifyApi = SpotifyApi(),
    private val youTubeMatcher: YouTubeMatcher = YouTubeMatcher()
) {
    
    /**
     * Import a Spotify playlist from URL
     * Returns a Flow that emits ImportState updates as the import progresses
     * 
     * @param url Spotify playlist URL (e.g., "https://open.spotify.com/playlist/...")
     * @param playlistId Optional playlist ID (if already extracted)
     * @return Flow of ImportState updates
     */
    fun import(
        url: String,
        playlistId: String? = null
    ): Flow<ImportState> = flow {
        // Extract playlist ID
        val extractedPlaylistId = playlistId ?: SpotifyUrlExtractor.extractPlaylistId(url)
        
        if (extractedPlaylistId == null) {
            emit(ImportState(
                totalItems = 0,
                importedItems = 0,
                failedItems = 0,
                isDone = false,
                isFailed = true,
                message = "Invalid Playlist URL"
            ))
            return@flow
        }
        
        try {
            // Step 1: Get access token and fetch playlist data
            emit(ImportState(
                totalItems = 0,
                importedItems = 0,
                failedItems = 0,
                isDone = false,
                isFailed = false,
                message = "Getting Spotify playlist..."
            ))
            
            val playlist = spotifyApi.getAllTracksOfPlaylist(extractedPlaylistId)
            val totalItems = playlist.tracks.size
            
            if (totalItems == 0) {
                emit(ImportState(
                    totalItems = 0,
                    importedItems = 0,
                    failedItems = 0,
                    isDone = false,
                    isFailed = true,
                    message = "Playlist is empty!!"
                ))
                return@flow
            }
            
            // Step 2: Create playlist (use PlaylistRepository if logged in for Firestore sync)
            val playlistName = "${playlist.name} (Spotify)"
            val playlistId: String
            
            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                // Use PlaylistRepository to create user playlist (syncs to Firestore)
                val createResult = playlistRepository.createPlaylist(playlistName)
                playlistId = createResult.getOrElse {
                    // Fallback to local-only if Firestore creation fails
                    Timber.tag("SpotifyImporter").w(it, "Failed to create playlist in Firestore, creating locally")
                    val fallbackPlaylist = PlaylistEntity(
                        name = playlistName,
                        createdAt = LocalDateTime.now(),
                        lastUpdateTime = LocalDateTime.now(),
                        isEditable = true,
                        bookmarkedAt = LocalDateTime.now()
                    )
                    withContext(Dispatchers.IO) {
                        database.query { insert(fallbackPlaylist) }
                    }
                    fallbackPlaylist.id
                }
                Timber.tag("SpotifyImporter").d("✅ Created user playlist in Firestore: $playlistId - $playlistName")
            } else {
                // Guest mode: Create local playlist only
                val newPlaylist = PlaylistEntity(
                    name = playlistName,
                    createdAt = LocalDateTime.now(),
                    lastUpdateTime = LocalDateTime.now(),
                    isEditable = true,
                    bookmarkedAt = LocalDateTime.now()
                )
                withContext(Dispatchers.IO) {
                    database.query { insert(newPlaylist) }
                }
                playlistId = newPlaylist.id
                Timber.tag("SpotifyImporter").d("✅ Created local playlist (guest mode): $playlistId - $playlistName")
            }
            
            // Step 3: Process each track
            var importedCount = 0
            var failedCount = 0
            val importedSongIds = mutableListOf<String>()
            
            for ((index, track) in playlist.tracks.withIndex()) {
                try {
                    // Create search query: "Title Artist1, Artist2"
                    val query = track.query
                    
                    // Match to YouTube Music
                    val mediaMetadata = youTubeMatcher.match(query, useStringMatcher = false)
                    
                        if (mediaMetadata != null) {
                        // Add to database
                        withContext(Dispatchers.IO) {
                            addMediaItemToDatabase(mediaMetadata, playlistId)
                        }
                        
                        // Add to playlist using repository if logged in (syncs to Firestore)
                        if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                            playlistRepository.addSongToPlaylist(playlistId, mediaMetadata.id).fold(
                                onSuccess = {
                                    Timber.tag("SpotifyImporter").d("✅ Added song to Firestore playlist: ${mediaMetadata.title}")
                                },
                                onFailure = { e ->
                                    Timber.tag("SpotifyImporter").w(e, "⚠️ Failed to add song to Firestore playlist: ${mediaMetadata.title}")
                                }
                            )
                        }
                        
                        importedSongIds.add(mediaMetadata.id)
                        importedCount++
                        emit(ImportState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            failedItems = failedCount,
                            isDone = false,
                            isFailed = false,
                            message = "Importing($importedCount/$totalItems): ${mediaMetadata.title}"
                        ))
                    } else {
                        failedCount++
                        // Continue to next track even if this one failed
                    }
                } catch (e: Exception) {
                    failedCount++
                    // Continue to next track on error
                    e.printStackTrace()
                }
            }
            
            // Step 4: Emit completion
            // Verify final song count
            val finalSongCount = database.playlistSongs(playlistId).firstOrNull()?.size ?: 0
            Timber.tag("SpotifyImporter").d("✅ Import complete - Playlist: $playlistId, Songs in DB: $finalSongCount, Imported: $importedCount")
            
            emit(ImportState(
                totalItems = totalItems,
                importedItems = importedCount,
                failedItems = failedCount,
                isDone = true,
                isFailed = false,
                message = "Imported Playlist: ${playlist.name}",
                playlistId = playlistId // Include playlist ID for retrieval
            ))
            
        } catch (e: Exception) {
            emit(ImportState(
                totalItems = 0,
                importedItems = 0,
                failedItems = 0,
                isDone = false,
                isFailed = true,
                message = "Failed to import Playlist: ${e.message}"
            ))
            e.printStackTrace()
        }
    }
    
    /**
     * Add media item to database and playlist
     */
    private suspend fun addMediaItemToDatabase(
        mediaMetadata: MediaMetadata,
        playlistId: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Check if song already exists
            val existingSong = database.getSongById(mediaMetadata.id)
            
            if (existingSong == null) {
                // Insert new song with artists
                val songEntity = mediaMetadata.toSongEntity().copy(
                    inLibrary = LocalDateTime.now()
                )
                
                database.transaction {
                    insert(songEntity)
                    
                    // Insert artists
                    mediaMetadata.artists.forEachIndexed { index, artist ->
                        // Check if artist exists, if not create it
                        val artistEntity = com.musifly.android.db.entities.ArtistEntity(
                            id = artist.id ?: com.musifly.android.db.entities.ArtistEntity.generateArtistId(),
                            name = artist.name
                        )
                        insert(artistEntity)
                        
                        // Map song to artist
                        insert(
                            com.musifly.android.db.entities.SongArtistMap(
                                songId = mediaMetadata.id,
                                artistId = artistEntity.id,
                                position = index
                            )
                        )
                    }
                    
                    // Insert album if present
                    mediaMetadata.album?.let { album ->
                        val albumEntity = com.musifly.android.db.entities.AlbumEntity(
                            id = album.id,
                            title = album.title,
                            thumbnailUrl = mediaMetadata.thumbnailUrl,
                            songCount = 0, // Placeholder - Spotify import doesn't provide this
                            duration = 0 // Placeholder - Spotify import doesn't provide this
                        )
                        insert(albumEntity)
                        
                        insert(
                            com.musifly.android.db.entities.SongAlbumMap(
                                songId = mediaMetadata.id,
                                albumId = album.id,
                                index = 0 // Position within album
                            )
                        )
                    }
                }
            }
            
            // Add song to playlist
            // Get current song count for position (must be done outside transaction as it's a suspend function)
            val currentSongs = database.playlistSongs(playlistId).first()
            val position = currentSongs.size
            
            database.transaction {
                // Insert playlist song mapping
                insert(
                    com.musifly.android.db.entities.PlaylistSongMap(
                        playlistId = playlistId,
                        songId = mediaMetadata.id,
                        position = position
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Import a single Spotify track
     * Equivalent to Bloomee's ExternalMediaImporter.sfyMediaImporter()
     */
    suspend fun importTrack(url: String): MediaMetadata? {
        val trackId = SpotifyUrlExtractor.extractTrackId(url)
        if (trackId == null) {
            return null
        }
        
        return try {
            val trackData = spotifyApi.getTrackDetails(trackId)
            val title = trackData["name"] as? String ?: return null
            val artists = (trackData["artists"] as? List<*>)?.mapNotNull { artist ->
                (artist as? Map<*, *>)?.get("name") as? String
            } ?: emptyList()
            val artistsString = artists.joinToString(", ")
            val query = "$title $artistsString".trim()
            
            youTubeMatcher.match(query)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

