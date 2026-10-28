package com.musifly.android.download

/**
 * Represents a completed download
 * Adapted from NewPipe's FinishedMission
 */
class FinishedMission : Mission {
    
    companion object {
        private const val serialVersionUID = 1L
    }
    
    
    /**
     * Download kind (v=video, a=audio, s=subtitle)
     */
    var kind: Char = 'v'
    
    /**
     * Finished missions are always finished
     */
    override val finished: Boolean = true
    
    /**
     * Finished missions are not running
     */
    override val running: Boolean = false
    
    /**
     * For finished missions, done equals length
     */
    override val done: Long
        get() = length
    
    /**
     * Finished missions are at 100% progress
     */
    override fun getProgress(): Float = 100f
    
    // Metadata for UI display
    var title: String? = null
    var artist: String? = null
    var thumbnailUrl: String? = null
    var quality: String? = null
    
    constructor() : super(null, 0, 0, null, null)
    
    /**
     * Create from a completed DownloadMission
     */
    constructor(mission: DownloadMission) : super(
        mission.source,
        mission.length,
        mission.timestamp,
        mission.storage,
        mission.songId
    ) {
        this.kind = mission.kind
        // Copy metadata
        this.title = mission.title
        this.artist = mission.artist
        this.thumbnailUrl = mission.thumbnailUrl
        this.quality = mission.quality
    }
}
