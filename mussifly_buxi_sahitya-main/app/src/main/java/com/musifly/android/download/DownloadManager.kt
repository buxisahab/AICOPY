package com.musifly.android.download

import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.File
import java.util.*

/**
 * Download Manager - manages download queue and missions
 * Adapted from NewPipe's DownloadManager
 * 
 * Features:
 * - Persistent pause/resume across app restarts
 * - Multi-threaded downloading
 * - Queue management
 * - Network-aware downloading
 */
class DownloadManager(
    private val context: Context,
    private val handler: Handler,
    var mainStorageVideo: StoredDirectoryHelper?,
    var mainStorageAudio: StoredDirectoryHelper?
) {
    
    companion object {
        private const val TAG = "DownloadManager"
        const val TAG_AUDIO = "audio"
        const val TAG_VIDEO = "video"
        private const val DOWNLOADS_METADATA_FOLDER = "pending_downloads"
        
        /**
         * Pick available temporal directory
         */
        fun pickAvailableTemporalDir(context: Context): File {
            var dir = context.externalCacheDir
            if (isDirectoryAvailable(dir)) return dir!!
            
            dir = context.cacheDir
            if (isDirectoryAvailable(dir)) return dir!!
            
            throw RuntimeException("No temporal directories are available")
        }
        
        private fun isDirectoryAvailable(directory: File?): Boolean {
            return directory != null && directory.canWrite() && directory.exists()
        }
    }
    
    private val finishedMissionStore: FinishedMissionStore = FinishedMissionStore(context)
    private val pendingMissionStore: PendingMissionStore = PendingMissionStore(context)
    private val missionsPending = ArrayList<DownloadMission>()
    private val missionsFinished: ArrayList<FinishedMission>
    private val pendingMissionsDir: File
    
    private var lastNetworkStatus = NetworkState.Unavailable
    private var selfMissionsControl = false
    
    // Preferences
    var prefMaxRetry = 3
    var prefMeteredDownloads = false
    var prefQueueLimit = true
    
    init {
        missionsFinished = loadFinishedMissions()
        pendingMissionsDir = getPendingDir(context)
        loadPendingMissions()
        
        Log.d(TAG, "DownloadManager initialized: ${missionsPending.size} pending, ${missionsFinished.size} finished")
    }
    
    private fun getPendingDir(context: Context): File {
        var dir = context.getExternalFilesDir(DOWNLOADS_METADATA_FOLDER)
        if (testDir(dir)) return dir!!
        
        dir = File(context.filesDir, DOWNLOADS_METADATA_FOLDER)
        if (testDir(dir)) return dir!!
        
        throw RuntimeException("Path to pending downloads are not accessible")
    }
    
    private fun testDir(dir: File?): Boolean {
        if (dir == null) return false
        
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Cannot create directory: ${dir.absolutePath}")
                return false
            }
            
            val tmp = File(dir, ".tmp")
            if (!tmp.createNewFile()) return false
            return tmp.delete()
        } catch (e: Exception) {
            Log.e(TAG, "testDir() failed: ${dir.absolutePath}", e)
            return false
        }
    }
    
    /**
     * Load finished missions from database
     */
    private fun loadFinishedMissions(): ArrayList<FinishedMission> {
        val missions = finishedMissionStore.loadFinishedMissions()
        
        // Check if files exist
        for (i in missions.size - 1 downTo 0) {
            val mission = missions[i]
            if (mission.storage?.existsAsFile() != true) {
                Log.d(TAG, "Downloaded file removed: ${mission.storage?.getName()}")
                finishedMissionStore.deleteMission(mission)
                missions.removeAt(i)
            }
        }
        
        return missions
    }
    
    /**
     * Load pending missions from SQLite database
     * This restores paused downloads after app restart
     */
    private fun loadPendingMissions() {
        Log.d(TAG, "Loading pending downloads from database...")
        
        try {
            val savedMissions = pendingMissionStore.loadPendingMissions()
            
            for (mission in savedMissions) {
                // Setup context and handler
                mission.context = context
                mission.mHandler = handler
                mission.maxRetry = prefMaxRetry
                
                // IMPORTANT: Set context on StoredFileHelper for MediaStore URI access
                // Context is transient and not restored from database
                mission.storage?.setContext(context)
                
                // Setup state change callback for persistence
                setupMissionCallback(mission)
                
                // Mark as paused if it had progress
                if (mission.done > 0 || mission.savedDone > 0) {
                    mission.isPaused = true
                }
                
                // Create metadata file if needed
                mission.metadata = File(pendingMissionsDir, mission.timestamp.toString())
                if (!mission.metadata!!.exists()) {
                    try {
                        mission.metadata!!.createNewFile()
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not create metadata file: ${e.message}")
                    }
                }
                
                missionsPending.add(mission)
                Log.d(TAG, "Loaded mission: ${mission.title ?: mission.storage?.getName()} (done: ${mission.done}/${mission.length}, paused: ${mission.isPaused})")
            }
            
            if (missionsPending.size > 1) {
                missionsPending.sortBy { it.timestamp }
            }
            
            Log.d(TAG, "Loaded ${missionsPending.size} pending missions from database")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pending missions", e)
        }
        
        // Also check legacy file-based missions
        loadLegacyMissions()
    }
    
    /**
     * Load legacy file-based missions (for backwards compatibility)
     */
    private fun loadLegacyMissions() {
        val files = pendingMissionsDir.listFiles() ?: return
        
        for (file in files) {
            if (!file.isFile || file.name == ".tmp") continue
            
            // Check if this mission is already loaded from DB
            val timestamp = file.name.toLongOrNull() ?: continue
            val alreadyLoaded = missionsPending.any { it.timestamp == timestamp }
            
            if (!alreadyLoaded) {
                Log.d(TAG, "Found orphan metadata file: ${file.name} - cleaning up")
                file.delete()
            }
        }
    }
    
    /**
     * Setup callback to persist mission state changes
     */
    private fun setupMissionCallback(mission: DownloadMission) {
        mission.onStateChanged = { m ->
            handler.post {
                saveMissionState(m)
            }
        }
        
        // BUG 2 FIX: UI update callback - notify observers immediately
        mission.onUiStateChanged = { m ->
            handler.post {
                // Send progress message to trigger UI refresh
                handler.obtainMessage(DownloadManagerService.MESSAGE_PROGRESS, m).sendToTarget()
            }
        }
    }
    
    /**
     * Save mission state to database
     */
    private fun saveMissionState(mission: DownloadMission) {
        try {
            if (mission.isFinished()) {
                // Remove from pending, add to finished
                pendingMissionStore.deleteMission(mission)
            } else {
                // Update in pending
                pendingMissionStore.saveMission(mission)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save mission state", e)
        }
    }
    
    /**
     * Start a new download mission
     */
    fun startMission(mission: DownloadMission) {
        synchronized(this) {
            mission.timestamp = System.currentTimeMillis()
            mission.mHandler = handler
            mission.maxRetry = prefMaxRetry
            mission.context = context
            
            // Setup state change callback for persistence
            setupMissionCallback(mission)
            
            // Create metadata file
            while (true) {
                mission.metadata = File(pendingMissionsDir, mission.timestamp.toString())
                if (!mission.metadata!!.exists()) {
                    try {
                        if (!mission.metadata!!.createNewFile()) {
                            throw RuntimeException("Cannot create download metadata file")
                        }
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                    break
                }
                mission.timestamp = System.currentTimeMillis()
            }
            
            selfMissionsControl = true
            missionsPending.add(mission)
            
            // Save to database immediately
            pendingMissionStore.saveMission(mission)
            Log.d(TAG, "Started new mission: ${mission.title ?: mission.storage?.getName()}")
            
            if (mission.storage == null) {
                mission.errCode = DownloadMission.ERROR_FILE_CREATION
                mission.errObject = Exception("DownloadMission.storage == NULL")
                return
            }
            
            val start = !prefQueueLimit || getRunningMissionsCount() < 1
            
            if (canDownloadInCurrentNetwork() && start) {
                Log.d(TAG, "Starting mission immediately - network available")
                mission.start()
            } else if (start) {
                // Network state might not be detected yet, start anyway for new missions
                // The download will fail gracefully if there's no network
                Log.d(TAG, "Starting mission anyway - network state: $lastNetworkStatus, prefMetered: $prefMeteredDownloads")
                mission.start()
            } else {
                Log.d(TAG, "Mission queued - waiting for current downloads to finish")
            }
        }
    }
    
    /**
     * Resume a paused mission
     */
    fun resumeMission(mission: DownloadMission) {
        if (!mission.running && !mission.isFinished()) {
            Log.d(TAG, "Resuming mission: ${mission.title ?: mission.storage?.getName()} (done: ${mission.done}/${mission.length})")
            
            // Ensure context is set
            if (mission.context == null) {
                mission.context = context
            }
            if (mission.mHandler == null) {
                mission.mHandler = handler
            }
            
            // Ensure storage has context for MediaStore URIs
            mission.storage?.setContext(context)
            
            // Ensure callback is set
            if (mission.onStateChanged == null) {
                setupMissionCallback(mission)
            }
            
            mission.start()
            
            // Update in database
            pendingMissionStore.saveMission(mission)
        }
    }
    
    /**
     * Pause a running mission
     */
    fun pauseMission(mission: DownloadMission) {
        if (mission.running) {
            mission.updateEnqueuedState(false)
            mission.pause()
            
            // Save state to database immediately
            pendingMissionStore.saveMission(mission)
            Log.d(TAG, "Paused and saved mission: ${mission.title ?: mission.storage?.getName()} (done: ${mission.done}/${mission.length})")
        }
    }
    
    /**
     * Delete a mission
     */
    fun deleteMission(mission: Mission, alsoDeleteFile: Boolean) {
        synchronized(this) {
            when (mission) {
                is DownloadMission -> {
                    // Stop if running
                    if (mission.running) {
                        mission.pause()
                    }
                    
                    missionsPending.remove(mission)
                    
                    // Remove from database
                    pendingMissionStore.deleteMission(mission)
                    
                    // Delete state file if exists
                    val stateFile = mission.storage?.getUri()?.path?.let { File("$it.state") }
                    stateFile?.delete()
                    
                    Log.d(TAG, "Deleted pending mission: ${mission.title ?: mission.storage?.getName()}")
                }
                is FinishedMission -> {
                    missionsFinished.remove(mission)
                    finishedMissionStore.deleteMission(mission)
                    Log.d(TAG, "Deleted finished mission: ${mission.storage?.getName()}")
                }
            }
            
            if (alsoDeleteFile) {
                mission.delete()
            }
        }
    }
    
    /**
     * Forget a mission (remove from list but keep file)
     */
    fun forgetMission(storage: StoredFileHelper) {
        synchronized(this) {
            val mission = getAnyMission(storage) ?: return
            
            when (mission) {
                is DownloadMission -> missionsPending.remove(mission)
                is FinishedMission -> {
                    missionsFinished.remove(mission)
                    finishedMissionStore.deleteMission(mission)
                }
            }
            
            mission.storage = null
            mission.delete()
        }
    }
    
    /**
     * Try to recover a mission
     */
    fun tryRecover(mission: DownloadMission) {
        val mainStorage = getMainStorage(mission.storage?.getTag() ?: "")
        
        if (mission.storage?.isInvalid() == false && mission.storage?.create() == true) {
            return
        }
        
        mission.storage?.invalidate()
        
        if (mainStorage == null) return
        
        val newStorage = mainStorage.createFile(
            mission.storage?.getName() ?: "download",
            mission.storage?.getType() ?: "mp4"
        )
        
        if (newStorage != null) {
            mission.storage = newStorage
        }
    }
    
    /**
     * Get a pending mission by storage
     */
    private fun getPendingMission(storage: StoredFileHelper): DownloadMission? {
        return missionsPending.find { it.storage == storage }
    }
    
    /**
     * Get a finished mission index by storage
     */
    private fun getFinishedMissionIndex(storage: StoredFileHelper): Int {
        for (i in missionsFinished.indices) {
            if (missionsFinished[i].storage == storage) {
                if (storage.existsAsFile() && storage.length() > 0) {
                    return i
                } else {
                    Log.d(TAG, "Matched downloaded file removed: ${storage.getName()}")
                    finishedMissionStore.deleteMission(missionsFinished[i])
                    missionsFinished.removeAt(i)
                    return -1
                }
            }
        }
        return -1
    }
    
    /**
     * Get any mission (pending or finished) by storage
     */
    private fun getAnyMission(storage: StoredFileHelper): Mission? {
        synchronized(this) {
            val pending = getPendingMission(storage)
            if (pending != null) return pending
            
            val idx = getFinishedMissionIndex(storage)
            if (idx >= 0) return missionsFinished[idx]
        }
        return null
    }
    
    /**
     * Get count of running missions
     */
    fun getRunningMissionsCount(): Int {
        var count = 0
        synchronized(this) {
            for (mission in missionsPending) {
                if (mission.running && !mission.isPsFailed() && !mission.isFinished()) {
                    count++
                }
            }
        }
        return count
    }
    
    /**
     * Pause all missions
     */
    fun pauseAllMissions(force: Boolean) {
        synchronized(this) {
            for (mission in missionsPending) {
                if (!mission.running || mission.isPsRunning() || mission.isFinished()) continue
                
                if (force) {
                    mission.init = null
                    mission.threads = emptyArray()
                }
                
                mission.pause()
                
                // Save state to database
                pendingMissionStore.saveMission(mission)
            }
            
            Log.d(TAG, "Paused all missions")
        }
    }
    
    /**
     * Start all missions
     */
    fun startAllMissions() {
        synchronized(this) {
            for (mission in missionsPending) {
                if (mission.running || mission.isCorrupt() || mission.isFinished()) continue
                
                // Ensure context is set
                if (mission.context == null) {
                    mission.context = context
                }
                if (mission.mHandler == null) {
                    mission.mHandler = handler
                }
                
                mission.start()
            }
            
            Log.d(TAG, "Started all missions")
        }
    }
    
    /**
     * Set a mission as finished
     */
    fun setFinished(mission: DownloadMission) {
        synchronized(this) {
            missionsPending.remove(mission)
            missionsFinished.add(0, FinishedMission(mission))
            finishedMissionStore.addFinishedMission(mission)
            
            // Remove from pending database
            pendingMissionStore.deleteMission(mission)
            
            // Delete state file
            val stateFile = mission.storage?.getUri()?.path?.let { File("$it.state") }
            stateFile?.delete()
            
            Log.d(TAG, "Mission finished: ${mission.title ?: mission.storage?.getName()}")
        }
    }
    
    /**
     * Run missions from queue
     */
    fun runMissions(): Boolean {
        synchronized(this) {
            if (missionsPending.isEmpty()) return false
            if (!canDownloadInCurrentNetwork()) return false
            
            if (prefQueueLimit) {
                for (mission in missionsPending) {
                    if (!mission.isFinished() && mission.running) return true
                }
            }
            
            var flag = false
            for (mission in missionsPending) {
                if (mission.running || !mission.enqueued || mission.isFinished()) continue
                
                resumeMission(mission)
                if (mission.errCode != DownloadMission.ERROR_NOTHING) continue
                
                if (prefQueueLimit) return true
                flag = true
            }
            
            return flag
        }
    }
    
    /**
     * Check if can download in current network
     */
    private fun canDownloadInCurrentNetwork(): Boolean {
        if (lastNetworkStatus == NetworkState.Unavailable) return false
        return !(prefMeteredDownloads && lastNetworkStatus == NetworkState.MeteredOperating)
    }
    
    /**
     * Handle network connectivity state changes
     */
    fun handleConnectivityState(currentStatus: NetworkState, updateOnly: Boolean) {
        if (currentStatus == lastNetworkStatus) return
        
        lastNetworkStatus = currentStatus
        if (currentStatus == NetworkState.Unavailable) return
        
        if (!selfMissionsControl || updateOnly) return
        
        val isMetered = prefMeteredDownloads && lastNetworkStatus == NetworkState.MeteredOperating
        
        synchronized(this) {
            for (mission in missionsPending) {
                if (mission.isCorrupt() || mission.isPsRunning()) continue
                
                if (mission.running && isMetered) {
                    mission.pause()
                } else if (!mission.running && !isMetered && mission.enqueued) {
                    mission.start()
                    if (prefQueueLimit) break
                }
            }
        }
    }
    
    /**
     * Update maximum retry attempts for all missions
     */
    fun updateMaximumAttempts() {
        synchronized(this) {
            for (mission in missionsPending) {
                mission.maxRetry = prefMaxRetry
            }
        }
    }
    
    /**
     * Check if mission can be recovered
     */
    fun canRecoverMission(mission: DownloadMission?): Boolean {
        if (mission == null) return false
        return mission.errCode == DownloadMission.ERROR_PROGRESS_LOST ||
                mission.storage == null ||
                mission.storage?.existsAsFile() != true
    }
    
    /**
     * Check for existing mission
     */
    fun checkForExistingMission(storage: StoredFileHelper): MissionState {
        synchronized(this) {
            val pending = getPendingMission(storage)
            
            if (pending == null) {
                if (getFinishedMissionIndex(storage) >= 0) {
                    return MissionState.Finished
                }
            } else {
                return if (pending.isFinished()) {
                    MissionState.Finished
                } else {
                    if (pending.running) MissionState.PendingRunning else MissionState.Pending
                }
            }
        }
        
        return MissionState.None
    }
    
    /**
     * Get main storage by tag
     */
    private fun getMainStorage(tag: String): StoredDirectoryHelper? {
        return when (tag) {
            TAG_AUDIO -> mainStorageAudio
            TAG_VIDEO -> mainStorageVideo
            else -> {
                Log.w(TAG, "Unknown download category: $tag")
                null
            }
        }
    }
    
    /**
     * Forget all finished downloads
     */
    fun forgetFinishedDownloads() {
        synchronized(this) {
            for (mission in missionsFinished) {
                finishedMissionStore.deleteMission(mission)
            }
            missionsFinished.clear()
        }
    }
    
    /**
     * Get all pending missions
     */
    fun getPendingMissions(): List<DownloadMission> {
        synchronized(this) {
            return ArrayList(missionsPending)
        }
    }
    
    /**
     * Get all finished missions
     */
    fun getFinishedMissions(): List<FinishedMission> {
        synchronized(this) {
            return ArrayList(missionsFinished)
        }
    }
    
    /**
     * Save all pending missions to database
     * Call this when app is going to background
     */
    fun saveAllMissions() {
        synchronized(this) {
            for (mission in missionsPending) {
                if (!mission.isFinished()) {
                    pendingMissionStore.saveMission(mission)
                }
            }
            Log.d(TAG, "Saved all ${missionsPending.size} pending missions")
        }
    }
    
    /**
     * Get count of paused missions
     */
    fun getPausedMissionsCount(): Int {
        synchronized(this) {
            return missionsPending.count { it.isPausedState() }
        }
    }
    
    /**
     * Check if there are any resumable missions
     */
    fun hasResumableMissions(): Boolean {
        synchronized(this) {
            return missionsPending.any { it.canResume() }
        }
    }
}
