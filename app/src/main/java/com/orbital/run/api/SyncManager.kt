package com.orbital.run.api

import android.content.Context
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centraized manager for orchestrating synchronization between Strava, Health Connect and local storage.
 * Optimized for performance using parallel fetching and background processing.
 */
object SyncManager {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Main entry point for full synchronization.
     * Triggers Strava and Health Connect sync in parallel.
     */
    suspend fun syncAll(context: Context): Int = withContext(Dispatchers.IO) {
        android.util.Log.d("SYNC", "=== Début syncAll ===")
        
        // ✅ CRITICAL: Load Strava token before checking auth status
        StravaAPI.loadToken(context)
        
        android.util.Log.d("SYNC", "Strava authentifié: ${StravaAPI.isAuthenticated()}")
        android.util.Log.d("SYNC", "Health Connect autorisé: ${HealthConnectManager.hasAllPermissionsSync(context)}")
        android.util.Log.d("SYNC", "HealthConnect permissions: ${HealthConnectManager.hasAllPermissions(context)}")
        
        val totalSynced = AtomicInteger(0)
        
        val stravaJob = async {
            try {
                if (StravaAPI.isAuthenticated()) {
                    val result = syncStrava(context)
                    totalSynced.addAndGet(result)
                }
                Unit // Force return Unit
            } catch (e: Exception) {
                android.util.Log.e("SYNC", "Erreur Strava: ${e.message}")
                Unit
            }
        }
        
        val healthJob = async {
            try {
                if (HealthConnectManager.isAvailable(context) && HealthConnectManager.hasAllPermissions(context)) {
                    val result = syncHealthConnect(context)
                    totalSynced.addAndGet(result)
                }
                Unit
            } catch (e: Exception) {
               android.util.Log.e("SYNC", "Erreur HC: ${e.message}")
               Unit
            }
        }
        
        val garminJob = async {
            try {
                if (GarminAPI.isConfigured()) {
                    val result = syncGarmin(context)
                    totalSynced.addAndGet(result)
                }
                Unit
            } catch (e: Exception) {
                android.util.Log.e("SYNC", "Erreur Garmin: ${e.message}")
                Unit
            }
        }
        
        awaitAll(stravaJob, healthJob, garminJob)
        android.util.Log.d("SYNC", "=== FIN SYNC: ${totalSynced.get()} nouvelles activités ===")
        totalSynced.get()
    }

    /**
     * Synchronizes Strava activities with high-performance stream fetching.
     */
    suspend fun syncStrava(context: Context): Int = withContext(Dispatchers.IO) {
        android.util.Log.d("SYNC", "--- Sync Strava START ---")
        // Pass context to enable token refresh
        val activitiesResult = StravaAPI.fetchActivities(30, context)
        
        if (activitiesResult.isFailure) {
            android.util.Log.e("SYNC", "❌ Échec récupération Strava: ${activitiesResult.exceptionOrNull()?.message}")
            return@withContext 0
        }
        
        val workouts = activitiesResult.getOrNull() ?: return@withContext 0
        android.util.Log.d("SYNC", "✅ Strava a retourné ${workouts.size} activités")
        
        val history = Persistence.loadHistory(context)
        android.util.Log.d("SYNC", "📊 Historique local: ${history.size} activités")
        
        val toSave = mutableListOf<Persistence.CompletedActivity>()

        // 1. Filter workouts that actually need syncing
        val needsSync = workouts.filter { w ->
            val parts = w.externalId?.split("|") ?: return@filter false
            val stravaId = parts[0]
            val dateStr = parts.getOrNull(1) ?: ""
            val dateMillis = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(dateStr)?.time ?: 0L
            } catch (e: Exception) { 0L }

            val existing = history.find { 
                it.externalId == stravaId || it.id == stravaId ||
                (kotlin.math.abs(it.date - dateMillis) < 300000 && kotlin.math.abs(it.distanceKm - w.totalDistanceKm) < 0.1)
            }
            
            // ✅ DO NOT sync if deleted (blacklisted)
            val isDeleted = Persistence.isBlacklisted(context, stravaId)
            if (isDeleted) {
                android.util.Log.d("SYNC", "🚫 Activité $stravaId ignorée (supprimée par l'utilisateur)")
                return@filter false
            }
            
            // ✅ IMPROVED LOGIC: Sync if new OR incomplete
            // Avoid re-syncing over DrawRun activities that were explicitly deleted or are already complete.
            existing == null ||  // New activity
            (existing.source == "DrawRun" && (existing.summaryPolyline.isNullOrEmpty() || existing.heartRateSamples.isEmpty())) || // Only overwrite "DrawRun" if it lacks data
            existing.summaryPolyline.isNullOrEmpty() ||  // Enrich with GPS data
            existing.heartRateSamples.isEmpty()  // Enrich with detailed HR data
        }
        
        android.util.Log.d("SYNC", "🔍 Après filtrage: ${needsSync.size}/${workouts.size} activités à synchroniser")
        needsSync.take(5).forEach {
            android.util.Log.d("SYNC", "  → ${it.title} (${it.totalDistanceKm}km, ${it.externalId?.split("|")?.getOrNull(1)})")
        }

        // 2. Fetch streams in small throttled batches (5 at a time) to avoid memory/network spikes
        needsSync.chunked(5).forEach { batch ->
            val deferred = batch.map { w ->
                async {
                    val parts = w.externalId!!.split("|")
                    val stravaId = parts[0]
                    val dateStr = parts[1]
                    val dateMillis = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.parse(dateStr)?.time ?: 0L
                    } catch (e: Exception) { 0L }

                    val streams = try { StravaAPI.fetchAllActivityStreams(stravaId) } catch (e: Exception) { StravaAPI.FullStreams() }
                    
                    Persistence.CompletedActivity(
                        id = stravaId,
                        date = dateMillis,
                        type = w.type,
                        title = w.title,
                        distanceKm = w.totalDistanceKm,
                        durationMin = w.totalDurationMin,
                        source = "Strava",
                        avgHeartRate = w.avgHeartRate,
                        maxHeartRate = w.maxHeartRate,
                        avgCadence = w.avgCadence,
                        avgWatts = w.avgWatts,
                        calories = w.calories,
                        totalStrokes = w.totalStrokes,
                        swolf = w.swolf,
                        elevationGain = w.elevationGain,
                        externalId = stravaId,
                        heartRateSamples = streams.hr,
                        powerSamples = streams.watts,
                        cadenceSamples = streams.cadence,
                        speedSamples = streams.speed,
                        elevationSamples = streams.alt,
                        summaryPolyline = w.summaryPolyline
                    )
                }
            }
            toSave.addAll(deferred.awaitAll())
        }

        // 3. One single batch save to Disk
        if (toSave.isNotEmpty()) {
            android.util.Log.d("SYNC", "💾 Sauvegarde de ${toSave.size} activités...")
            Persistence.saveHistoryBatch(context, toSave)
            val newCount = toSave.count { act -> history.none { it.externalId == act.externalId } }
            android.util.Log.d("SYNC", "✅ ${newCount} nouvelles activités Strava ajoutées")
            newCount
        } else {
            android.util.Log.d("SYNC", "ℹ️ Aucune activité Strava à synchroniser")
            0
        }
    }

    /**
     * Synchronizes Health Connect data.
     */
    suspend fun syncHealthConnect(context: Context): Int = withContext(Dispatchers.IO) {
        val hcActivities = HealthConnectManager.syncRecentActivities(context, 30)
        val history = Persistence.loadHistory(context)
        val toSave = mutableListOf<Persistence.CompletedActivity>()
        
        hcActivities.forEach { act ->
            // ✅ Check blacklist for Health Connect too
            if (Persistence.isBlacklisted(context, act.id) || (act.externalId != null && Persistence.isBlacklisted(context, act.externalId))) {
                return@forEach
            }
            // Even if exists, we might want to merge/update
            toSave.add(act)
        }
        
        if (toSave.isNotEmpty()) {
            Persistence.saveHistoryBatch(context, toSave)
            // Return count of truly new ones
            toSave.count { act -> history.none { it.externalId == act.externalId || (kotlin.math.abs(it.date - act.date) < 300000 && kotlin.math.abs(it.distanceKm - act.distanceKm) < 0.1) } }
        } else {
            0
        }
    }
    /**
     * Synchronizes Garmin activities (via Vercel Proxy).
     */
    suspend fun syncGarmin(context: Context): Int = withContext(Dispatchers.IO) {
        android.util.Log.d("SYNC", "--- Sync Garmin START ---")
        val activities = GarminAPI.fetchActivities(30, context)
        
        if (activities == null) {
             android.util.Log.e("SYNC", "❌ Échec récupération Garmin (voir logs API)")
             return@withContext 0
        }
        
        android.util.Log.d("SYNC", "✅ Garmin a retourné ${activities.size} activités")
        
        val history = Persistence.loadHistory(context)
        val toSave = mutableListOf<Persistence.CompletedActivity>()
        
        activities.forEach { act ->
             // Deduplicate by externalId (Garmin ID) or fuzzy match
             val existing = history.find { 
                 (act.externalId != null && it.externalId == act.externalId) || 
                 (kotlin.math.abs(it.date - act.date) < 300000 && kotlin.math.abs(it.distanceKm - act.distanceKm) < 0.1)
             }
             
             if (existing == null && !Persistence.isBlacklisted(context, act.externalId ?: "")) {
                 toSave.add(act)
             }
        }
        
        if (toSave.isNotEmpty()) {
             android.util.Log.d("SYNC", "💾 Sauvegarde de ${toSave.size} activités Garmin...")
             Persistence.saveHistoryBatch(context, toSave)
             return@withContext toSave.size
        }
        
        return@withContext 0
    }
}
// Note: syncAll must be updated too (replaced below)
