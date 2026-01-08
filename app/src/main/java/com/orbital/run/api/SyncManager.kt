package com.orbital.run.api

import android.content.Context
import com.orbital.run.logic.Persistence
import kotlinx.coroutines.*

/**
 * Simplified sync manager using only Health Connect.
 * All activity data comes from Health Connect which aggregates data from Garmin, Strava, etc.
 */
object SyncManager {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Main entry point for synchronization.
     * General function to sync ALL sources (currently only HC).
     */
    suspend fun syncAll(context: Context, onProgress: ((Int, Int) -> Unit)? = null): Int {
        android.util.Log.d("SYNC", "=== Début syncAll (Multi-Sources) ===")
        
        var totalNew = 0
        
        // 1. Health Connect (Aggregation principale)
        if (HealthConnectManager.isAvailable(context) && HealthConnectManager.hasAllPermissions(context)) {
            totalNew += syncHealthConnect(context, onProgress)
        }
        
        // 2. Fitbit Direct
        if (FitbitManager.isConnected(context)) {
            val fitbitActivities = FitbitManager.downloadRecentActivities(context)
            totalNew += saveActivitiesIfNew(context, fitbitActivities, "FITBIT")
        }
        
        // 3. Withings Direct
        if (WithingsManager.isConnected(context)) {
            val withingsActivities = WithingsManager.downloadRecentActivities(context)
            totalNew += saveActivitiesIfNew(context, withingsActivities, "WITHINGS")
        }
        
        // 4. Strava Sync
        if (com.orbital.run.api.StravaManager.isConnected(context)) {
            val stravaCount = com.orbital.run.api.StravaManager.syncActivities(context)
            totalNew += stravaCount
            android.util.Log.d("SYNC", "Strava: $stravaCount activiés récupérées")
        }
        
        android.util.Log.d("SYNC", "=== FIN SYNC: $totalNew activités traitées ===")
        return totalNew
    }

    suspend fun syncHealthConnect(context: Context, onProgress: ((Int, Int) -> Unit)? = null): Int = withContext(Dispatchers.IO) {
        val daysBack = com.orbital.run.logic.SyncPreferences.getDaysBack(context)
        
        var totalSaved = 0
        
        // Use streaming batch loader
        HealthConnectManager.syncRecentActivities(context, daysBack, onProgress) { batch ->
            // Save batch immediately
            val savedCount = saveActivitiesIfNew(context, batch, "HEALTH_CONNECT")
            totalSaved += savedCount
        }
        
        return@withContext totalSaved
    }
    
    // Core of "The Cake": Merging Data
    private fun saveActivitiesIfNew(context: Context, activities: List<Persistence.CompletedActivity>, source: String): Int {
        val history = Persistence.loadHistory(context).toMutableList()
        var newCount = 0
        var mergedCount = 0
        
        activities.forEach { act ->
            if (Persistence.isBlacklisted(context, act.id)) return@forEach
            
            // Deduplication Logic: Find existing matching activity
            val index = history.indexOfFirst { 
                it.externalId == act.externalId || 
                it.id == act.id ||
                (kotlin.math.abs(it.date - act.date) < 300000 && // 5 min window
                 kotlin.math.abs(it.distanceKm - act.distanceKm) < 0.2) // 200m diff
            }
            
            if (index == -1) {
                // New Activity
                history.add(0, act)
                newCount++
                android.util.Log.d("SYNC", "  → Nouvelle ($source): ${act.title}")
            } else {
                // Merge Data (The Cake)
                // Existing activity + New Data -> Richer Activity
                val existing = history[index]
                val merged = Persistence.mergeActivities(existing, act)
                
                if (merged != existing) {
                    history[index] = merged
                    mergedCount++
                    android.util.Log.d("SYNC", "  ⊕ Fusion ($source): ${act.title}")
                }
            }
        }
        
        if (newCount > 0 || mergedCount > 0) {
            // Sort by date descending
            history.sortByDescending { it.date }
            Persistence.saveHistoryList(context, history) // Assuming this is public or we use saveHistoryBatch
        }
        return newCount // Return new count for UI notification
    }

    /**
     * EXPORT: Push an activity to all connected services
     * This ensures "Sync on all services" requirement.
     */
    suspend fun syncToAll(context: Context, activity: Persistence.CompletedActivity) {
        withContext(Dispatchers.IO) {
            // Strava Export
            if (com.orbital.run.api.StravaManager.isConnected(context)) {
                // StravaManager.upload(context, activity) // Stub
            }
            // Fitbit Export
            if (FitbitManager.isConnected(context)) {
                FitbitManager.uploadActivity(context, activity)
            }
            // Others...
        }
    }
    
    /**
     * Trigger manual sync with UI feedback.
     */
    suspend fun manualSync(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            val count = syncAll(context)
            if (count > 0) {
                Persistence.recalculateRecords(context)
                SyncResult.Success(count)
            } else {
                SyncResult.NoNewData
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "Erreur sync manuel: ${e.message}", e)
            SyncResult.Error(e.message ?: "Erreur inconnue")
        }
    }
    
    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        object NoNewData : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}
