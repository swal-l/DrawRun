package com.orbital.run.api

import android.content.Context
import com.orbital.run.logic.Persistence
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance Optimizations for Strava/Garmin Integration
 */
object SyncOptimizer {
    
    // ===== INTELLIGENT CACHE =====
    
    private data class CachedActivity(
        val activity: Persistence.CompletedActivity,
        val timestamp: Long
    )
    
    private val activityCache = ConcurrentHashMap<String, CachedActivity>()
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    /**
     * Get activity from cache if valid
     */
    fun getCached(activityId: String): Persistence.CompletedActivity? {
        val cached = activityCache[activityId]
        return if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
            cached.activity
        } else {
            activityCache.remove(activityId)
            null
        }
    }
    
    /**
     * Put activity in cache
     */
    fun putCache(activityId: String, activity: Persistence.CompletedActivity) {
        activityCache[activityId] = CachedActivity(activity, System.currentTimeMillis())
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        activityCache.clear()
    }
    
    // ===== INCREMENTAL SYNC =====
    
    /**
     * Get last sync timestamp from preferences
     */
    fun getLastSyncTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_sync", 0L)
    }
    
    /**
     * Save last sync timestamp
     */
    fun saveLastSyncTimestamp(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sync", timestamp).apply()
    }
    
    /**
     * Sync only new activities (incremental)
     */
    suspend fun syncIncrementalStrava(context: Context): List<Persistence.CompletedActivity> = withContext(Dispatchers.IO) {
        val lastSync = getLastSyncTimestamp(context)
        val afterTimestamp = lastSync / 1000 // Convert to Unix timestamp
        
        // Fetch only activities after last sync
        val result = StravaAPI.fetchActivities(limit = 100)
        
        if (result.isFailure) return@withContext emptyList()
        
        val activities = result.getOrNull() ?: emptyList()
        
        // Filter by timestamp (Strava API doesn't support 'after' in all versions)
        // We'll fetch all and filter client-side for now
        val newActivities = activities.filter { workout ->
            // Parse date from externalId if available
            val dateStr = workout.externalId?.split("|")?.getOrNull(1)
            if (dateStr != null) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(dateStr)
                    date?.time ?: 0L > lastSync
                } catch (e: Exception) {
                    true // Include if can't parse
                }
            } else true
        }
        
        // Update last sync timestamp
        saveLastSyncTimestamp(context, System.currentTimeMillis())
        
        // Convert to CompletedActivity (simplified, would need full conversion)
        emptyList()
    }
    
    // ===== DATA COMPRESSION =====
    
    /**
     * Compress HR samples (reduce storage by ~80%)
     * Keep 1 sample every 5 seconds instead of every second
     */
    fun compressHRSamples(samples: List<Persistence.HeartRateSample>): List<Persistence.HeartRateSample> {
        if (samples.size < 100) return samples
        
        // Keep every 5th sample
        return samples.filterIndexed { index, _ -> index % 5 == 0 }
    }
    
    /**
     * Decompress HR samples (interpolate)
     */
    fun decompressHRSamples(compressed: List<Persistence.HeartRateSample>): List<Persistence.HeartRateSample> {
        if (compressed.size < 2) return compressed
        
        val decompressed = mutableListOf<Persistence.HeartRateSample>()
        
        compressed.zipWithNext().forEach { (a, b) ->
            decompressed.add(a)
            
            // Interpolate 4 points between a and b
            val step = (b.bpm - a.bpm) / 5.0
            for (i in 1..4) {
                decompressed.add(Persistence.HeartRateSample(
                    timeOffset = a.timeOffset + i,
                    bpm = (a.bpm + step * i).toInt()
                ))
            }
        }
        
        // Add last sample
        decompressed.add(compressed.last())
        
        return decompressed
    }
    
    // ===== BATCH PROCESSING =====
    
    /**
     * Fetch streams for multiple activities in parallel
     */
    suspend fun batchFetchStreams(
        activityIds: List<String>
    ): Map<String, List<Persistence.HeartRateSample>> = coroutineScope {
        activityIds.map { id ->
            async {
                try {
                    id to StravaAPI.fetchActivityStreams(id)
                } catch (e: Exception) {
                    id to emptyList()
                }
            }
        }.awaitAll().toMap()
    }
    
    /**
     * Smart sync: Check cache first, then fetch only missing
     */
    suspend fun smartSync(
        context: Context,
        activityIds: List<String>
    ): List<Persistence.CompletedActivity> {
        val cached = mutableListOf<Persistence.CompletedActivity>()
        val toFetch = mutableListOf<String>()
        
        // Check cache
        activityIds.forEach { id ->
            val cachedActivity = getCached(id)
            if (cachedActivity != null) {
                cached.add(cachedActivity)
            } else {
                toFetch.add(id)
            }
        }
        
        // Fetch missing (batch)
        val fetched: List<Persistence.CompletedActivity> = if (toFetch.isNotEmpty()) {
            // Would fetch from API here
            emptyList()
        } else emptyList()
        
        return cached + fetched
    }
    
    // ===== PERFORMANCE METRICS =====
    
    private var syncStartTime = 0L
    private var activitiesSynced = 0
    private var bytesTransferred = 0L
    
    /**
     * Start performance tracking
     */
    fun startPerformanceTracking() {
        syncStartTime = System.currentTimeMillis()
        activitiesSynced = 0
        bytesTransferred = 0L
    }
    
    /**
     * Record activity synced
     */
    fun recordActivitySynced(bytes: Long = 0L) {
        activitiesSynced++
        bytesTransferred += bytes
    }
    
    /**
     * Get performance stats
     */
    fun getPerformanceStats(): String {
        val duration = (System.currentTimeMillis() - syncStartTime) / 1000.0
        val avgTime = if (activitiesSynced > 0) duration / activitiesSynced else 0.0
        val kbTransferred = bytesTransferred / 1024.0
        
        return """
            Sync Performance:
            - Duration: ${duration.toInt()}s
            - Activities: $activitiesSynced
            - Avg time/activity: ${String.format("%.1f", avgTime)}s
            - Data transferred: ${String.format("%.1f", kbTransferred)} KB
        """.trimIndent()
    }
}
