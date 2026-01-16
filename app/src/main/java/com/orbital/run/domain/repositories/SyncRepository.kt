package com.orbital.run.domain.repositories

import com.orbital.run.domain.models.SyncResult
import com.orbital.run.domain.models.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing external service synchronization.
 */
interface SyncRepository {
    
    /**
     * Observe sync status for all services (reactive).
     *
     * Emits whenever connection state changes.
     */
    fun observeSyncStatus(): Flow<SyncStatus>
    
    /**
     * Get current sync status (one-time fetch).
     */
    suspend fun getSyncStatus(): SyncStatus
    
    /**
     * Sync all connected services.
     *
     * @return Combined result of all sync operations
     */
    suspend fun syncAllSources(): SyncResult
    
    /**
     * Sync Strava only.
     */
    suspend fun syncStrava(): SyncResult
    
    /**
     * Connect to Strava (initiates OAuth flow).
     */
    suspend fun connectToStrava()
    
    /**
     * Disconnect from Strava.
     */
    suspend fun disconnectFromStrava()
    
    /**
     * Check if Strava is connected.
     */
    suspend fun isStravaConnected(): Boolean
}
