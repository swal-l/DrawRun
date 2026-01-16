package com.orbital.run.domain.repositories

import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.models.ActivityType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing completed activities.
 *
 * Provides reactive data streams via Flow and async operations via suspend functions.
 * All implementations must handle threading internally (no blocking on main thread).
 */
interface ActivityRepository {
    
    /**
     * Observe all activities (reactive).
     *
     * Emits a new list whenever activities change (add, edit, delete, sync).
     * List is sorted by date (newest first).
     */
    fun observeActivities(): Flow<List<Activity>>
    
    /**
     * Observe activities of a specific type.
     */
    fun observeActivitiesByType(type: ActivityType): Flow<List<Activity>>
    
    /**
     * Get all activities (one-time fetch).
     *
     * @param limit Maximum number of activities to return
     */
    suspend fun getAllActivities(limit: Int = 500): List<Activity>
    
    /**
     * Get a single activity by ID.
     *
     * @return Activity if found, null otherwise
     */
    suspend fun getActivityById(id: String): Activity?
    
    /**
     * Save a new activity or update an existing one.
     *
     * If activity.id matches an existing activity, updates it.
     * Otherwise, creates a new activity.
     */
    suspend fun saveActivity(activity: Activity)
    
    /**
     * Save multiple activities in a batch (more efficient than individual saves).
     *
     * Handles deduplication internally based on ID and externalId.
     *
     * @return Number of activities actually saved (after deduplication)
     */
    suspend fun saveActivities(activities: List<Activity>): Int
    
    /**
     * Delete an activity permanently.
     *
     * Also adds to blacklist to prevent re-sync from external sources.
     */
    suspend fun deleteActivity(id: String)
    
    /**
     * Search activities by title.
     */
    suspend fun searchActivities(query: String): List<Activity>
    
    /**
     * Get activities within a date range.
     */
    suspend fun getActivitiesInRange(
        start: java.time.Instant,
        end: java.time.Instant
    ): List<Activity>
    
    /**
     * Clear all activities (dangerous operation).
     *
     * Used for account reset or testing.
     */
    suspend fun clearAllActivities()
}
