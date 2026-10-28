package com.musifly.android.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue

/**
 * Batch Download Manager - Advanced batch downloading with queue management
 * 
 * Features:
 * - Batch download multiple songs at once
 * - Priority-based queue system
 * - Parallel download control (max concurrent downloads)
 * - Automatic retry with exponential backoff
 * - Progress aggregation across all downloads
 * - Smart queue management
 */
class BatchDownloadManager(
    private val context: Context,
    private val downloadManager: DownloadManager
) {
    
    companion object {
        private const val TAG = "BatchDownloadManager"
        private const val MAX_CONCURRENT_DOWNLOADS = 3
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
    }
    
    /**
     * Download priority levels
     */
    enum class Priority(val value: Int) {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        URGENT(4)
    }
    
    /**
     * Batch download item with priority
     */
    data class BatchItem(
        val mission: DownloadMission,
        val priority: Priority = Priority.NORMAL,
        val retryCount: Int = 0,
        val addedTime: Long = System.currentTimeMillis()
    ) : Comparable<BatchItem> {
        override fun compareTo(other: BatchItem): Int {
            // Higher priority first, then earlier added time
            return if (priority.value != other.priority.value) {
                other.priority.value - priority.value
            } else {
                addedTime.compareTo(other.addedTime)
            }
        }
    }
    
    /**
     * Batch download callbacks
     */
    interface BatchDownloadCallbacks {
        fun onBatchStarted(totalItems: Int)
        fun onItemStarted(item: BatchItem, position: Int, total: Int)
        fun onItemCompleted(item: BatchItem, position: Int, total: Int)
        fun onItemFailed(item: BatchItem, error: String, position: Int, total: Int)
        fun onBatchProgress(completed: Int, failed: Int, remaining: Int, total: Int)
        fun onBatchCompleted(completed: Int, failed: Int, total: Int)
    }
    
    private val batchQueue = PriorityBlockingQueue<BatchItem>()
    private val activeMissions = ConcurrentHashMap<String, BatchItem>()
    private val completedMissions = ConcurrentHashMap<String, Boolean>() // true = success, false = failed
    private var callbacks: BatchDownloadCallbacks? = null
    private var batchJob: Job? = null
    private var isProcessing = false
    
    /**
     * Add single item to batch queue
     */
    fun addToBatch(mission: DownloadMission, priority: Priority = Priority.NORMAL) {
        val item = BatchItem(mission, priority)
        batchQueue.offer(item)
        Log.d(TAG, "Added to batch: ${mission.title} with priority ${priority.name}")
    }
    
    /**
     * Add multiple items to batch queue
     */
    fun addMultipleToBatch(missions: List<DownloadMission>, priority: Priority = Priority.NORMAL) {
        missions.forEach { mission ->
            addToBatch(mission, priority)
        }
        Log.d(TAG, "Added ${missions.size} items to batch")
    }
    
    /**
     * Start batch download processing
     */
    fun startBatch(callbacks: BatchDownloadCallbacks? = null) {
        if (isProcessing) {
            Log.w(TAG, "Batch already processing")
            return
        }
        
        this.callbacks = callbacks
        val totalItems = batchQueue.size
        
        if (totalItems == 0) {
            Log.w(TAG, "No items in batch queue")
            return
        }
        
        isProcessing = true
        callbacks?.onBatchStarted(totalItems)
        
        batchJob = CoroutineScope(Dispatchers.IO).launch {
            processBatchQueue(totalItems)
        }
    }
    
    /**
     * Process batch queue with concurrent download limit
     */
    private suspend fun processBatchQueue(totalItems: Int) {
        val jobs = mutableListOf<Job>()
        var processedCount = 0
        
        while (batchQueue.isNotEmpty() && isProcessing) {
            // Wait if we've reached max concurrent downloads
            while (activeMissions.size >= MAX_CONCURRENT_DOWNLOADS && isProcessing) {
                delay(500)
            }
            
            if (!isProcessing) break
            
            val item = batchQueue.poll() ?: break
            processedCount++
            
            val job = CoroutineScope(Dispatchers.IO).launch {
                processItem(item, processedCount, totalItems)
            }
            
            jobs.add(job)
        }
        
        // Wait for all downloads to complete
        jobs.forEach { it.join() }
        
        // Report completion
        val completed = completedMissions.count { it.value }
        val failed = completedMissions.count { !it.value }
        
        callbacks?.onBatchCompleted(completed, failed, totalItems)
        isProcessing = false
        
        Log.d(TAG, "Batch completed: $completed successful, $failed failed out of $totalItems")
    }
    
    /**
     * Process individual batch item
     */
    private suspend fun processItem(item: BatchItem, position: Int, total: Int) {
        val missionId = item.mission.timestamp.toString()
        
        try {
            activeMissions[missionId] = item
            
            withContext(Dispatchers.Main) {
                callbacks?.onItemStarted(item, position, total)
            }
            
            // Start download
            downloadManager.startMission(item.mission)
            
            // Wait for completion or failure
            waitForCompletion(item.mission)
            
            if (item.mission.isFinished()) {
                completedMissions[missionId] = true
                withContext(Dispatchers.Main) {
                    callbacks?.onItemCompleted(item, position, total)
                }
            } else if (item.mission.errCode != DownloadMission.ERROR_NOTHING) {
                // Retry logic
                if (item.retryCount < MAX_RETRIES) {
                    Log.d(TAG, "Retrying ${item.mission.title} (attempt ${item.retryCount + 1})")
                    
                    // Exponential backoff
                    val delay = INITIAL_RETRY_DELAY * (1 shl item.retryCount)
                    delay(delay)
                    
                    // Re-add to queue with incremented retry count
                    val retryItem = item.copy(retryCount = item.retryCount + 1)
                    batchQueue.offer(retryItem)
                } else {
                    completedMissions[missionId] = false
                    withContext(Dispatchers.Main) {
                        callbacks?.onItemFailed(
                            item,
                            item.mission.errObject?.message ?: "Unknown error",
                            position,
                            total
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing batch item", e)
            completedMissions[missionId] = false
            
            withContext(Dispatchers.Main) {
                callbacks?.onItemFailed(item, e.message ?: "Unknown error", position, total)
            }
        } finally {
            activeMissions.remove(missionId)
            
            // Update progress
            val completed = completedMissions.count { it.value }
            val failed = completedMissions.count { !it.value }
            val remaining = batchQueue.size
            
            withContext(Dispatchers.Main) {
                callbacks?.onBatchProgress(completed, failed, remaining, total)
            }
        }
    }
    
    /**
     * Wait for mission to complete or fail
     */
    private suspend fun waitForCompletion(mission: DownloadMission) {
        while (!mission.isFinished() && 
               mission.errCode == DownloadMission.ERROR_NOTHING && 
               isProcessing) {
            delay(500)
        }
    }
    
    /**
     * Pause batch processing
     */
    fun pauseBatch() {
        isProcessing = false
        batchJob?.cancel()
        
        // Pause all active missions
        activeMissions.values.forEach { item ->
            downloadManager.pauseMission(item.mission)
        }
        
        Log.d(TAG, "Batch paused")
    }
    
    /**
     * Resume batch processing
     */
    fun resumeBatch() {
        if (!isProcessing && batchQueue.isNotEmpty()) {
            startBatch(callbacks)
        }
    }
    
    /**
     * Cancel batch and clear queue
     */
    fun cancelBatch() {
        isProcessing = false
        batchJob?.cancel()
        
        // Cancel all active missions
        activeMissions.values.forEach { item ->
            downloadManager.deleteMission(item.mission, false)
        }
        
        // Clear queues
        batchQueue.clear()
        activeMissions.clear()
        completedMissions.clear()
        
        Log.d(TAG, "Batch cancelled and cleared")
    }
    
    /**
     * Get batch statistics
     */
    fun getBatchStats(): BatchStats {
        return BatchStats(
            total = batchQueue.size + activeMissions.size + completedMissions.size,
            queued = batchQueue.size,
            active = activeMissions.size,
            completed = completedMissions.count { it.value },
            failed = completedMissions.count { !it.value }
        )
    }
    
    /**
     * Change priority of queued item
     */
    fun changePriority(mission: DownloadMission, newPriority: Priority): Boolean {
        val missionId = mission.timestamp.toString()
        
        // Remove from queue if exists
        val existingItem = batchQueue.find { it.mission.timestamp.toString() == missionId }
        if (existingItem != null) {
            batchQueue.remove(existingItem)
            val updatedItem = existingItem.copy(priority = newPriority)
            batchQueue.offer(updatedItem)
            Log.d(TAG, "Changed priority for ${mission.title} to ${newPriority.name}")
            return true
        }
        
        return false
    }
    
    /**
     * Remove item from queue
     */
    fun removeFromQueue(mission: DownloadMission): Boolean {
        val removed = batchQueue.removeIf { it.mission.timestamp == mission.timestamp }
        if (removed) {
            Log.d(TAG, "Removed ${mission.title} from batch queue")
        }
        return removed
    }
    
    /**
     * Batch statistics data class
     */
    data class BatchStats(
        val total: Int,
        val queued: Int,
        val active: Int,
        val completed: Int,
        val failed: Int
    ) {
        val remainingCount: Int get() = queued + active
        val completedPercentage: Float get() = if (total > 0) (completed.toFloat() / total * 100) else 0f
    }
}
