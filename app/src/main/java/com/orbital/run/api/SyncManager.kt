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
     * Syncs only from Health Connect.
     */
    suspend fun syncAll(context: Context): Int = withContext(Dispatchers.IO) {
        android.util.Log.d("SYNC", "=== Début syncAll (Health Connect uniquement) ===")
        
        if (!HealthConnectManager.isAvailable(context)) {
            android.util.Log.w("SYNC", "Health Connect non disponible")
            return@withContext 0
        }
        
        if (!HealthConnectManager.hasAllPermissions(context)) {
            android.util.Log.w("SYNC", "Health Connect: permissions manquantes")
            return@withContext 0
        }
        
        val count = syncHealthConnect(context)
        android.util.Log.d("SYNC", "=== FIN SYNC: $count nouvelles activités ===")
        count
    }

    /**
     * Synchronizes Health Connect data.
     * Fetches all exercise sessions with detailed metrics.
     */
    suspend fun syncHealthConnect(context: Context): Int = withContext(Dispatchers.IO) {
        android.util.Log.d("SYNC", "--- Sync Health Connect START ---")
        
        try {
            val hcActivities = HealthConnectManager.syncRecentActivities(context, 30)
            android.util.Log.d("SYNC", "✅ Health Connect a retourné ${hcActivities.size} activités")
            
            val history = Persistence.loadHistory(context)
            val toSave = mutableListOf<Persistence.CompletedActivity>()
            
            hcActivities.forEach { act ->
                // Check blacklist
                if (Persistence.isBlacklisted(context, act.id) || 
                    (act.externalId != null && Persistence.isBlacklisted(context, act.externalId))) {
                    android.util.Log.d("SYNC", "🚫 Activité ${act.id} ignorée (blacklist)")
                    return@forEach
                }
                
                // Check if already exists (fuzzy match by date + distance)
                val existing = history.find { 
                    it.externalId == act.externalId || 
                    it.id == act.id ||
                    (kotlin.math.abs(it.date - act.date) < 300000 && 
                     kotlin.math.abs(it.distanceKm - act.distanceKm) < 0.1)
                }
                
                if (existing == null) {
                    toSave.add(act)
                    android.util.Log.d("SYNC", "  → Nouvelle: ${act.title} (${act.distanceKm}km)")
                }
            }
            
            if (toSave.isNotEmpty()) {
                android.util.Log.d("SYNC", "💾 Sauvegarde de ${toSave.size} activités...")
                Persistence.saveHistoryBatch(context, toSave)
                android.util.Log.d("SYNC", "✅ ${toSave.size} nouvelles activités ajoutées")
                return@withContext toSave.size
            } else {
                android.util.Log.d("SYNC", "ℹ️ Aucune nouvelle activité à synchroniser")
                return@withContext 0
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "❌ Erreur Health Connect: ${e.message}", e)
            return@withContext 0
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
