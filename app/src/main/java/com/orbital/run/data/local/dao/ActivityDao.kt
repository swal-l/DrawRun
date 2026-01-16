package com.orbital.run.data.local.dao

import androidx.room.*
import com.orbital.run.data.local.entities.ActivityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Activity operations.
 *
 * Provides reactive queries (Flow) for UI observability and
 * suspend functions for one-time database operations.
 *
 * All queries are main-safe (Room handles threading internally).
 */
@Dao
interface ActivityDao {
    
    // ========================
    // REACTIVE QUERIES (Flow)
    // ========================
    
    /**
     * Observe all activities with automatic updates.
     *
     * Emits new list whenever activities are inserted, updated, or deleted.
     * Sorted by date (newest first).
     *
     * @param limit Maximum number of activities to return
     */
    @Query("""
        SELECT * FROM activities 
        ORDER BY completed_at DESC 
        LIMIT :limit
    """)
    fun observeActivities(limit: Int = 500): Flow<List<ActivityEntity>>
    
    /**
     * Observe activities of a specific type (running, swimming, etc.).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE type = :type 
        ORDER BY completed_at DESC
    """)
    fun observeActivitiesByType(type: String): Flow<List<ActivityEntity>>
    
    /**
     * Observe activities from a specific source (Strava, Health Connect, etc.).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE source = :source 
        ORDER BY completed_at DESC
    """)
    fun observeActivitiesBySource(source: String): Flow<List<ActivityEntity>>
    
    // ========================
    // ONE-TIME QUERIES (suspend)
    // ========================
    
    /**
     * Get all activities (one-time fetch).
     *
     * Use this for analytics calculations where you need
     * the entire dataset at once.
     */
    @Query("""
        SELECT * FROM activities 
        ORDER BY completed_at DESC 
        LIMIT :limit
    """)
    suspend fun getAllActivities(limit: Int = 500): List<ActivityEntity>
    
    /**
     * Get a single activity by its ID.
     *
     * @return Activity if found, null otherwise
     */
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: String): ActivityEntity?
    
    /**
     * Get activity by external ID (Strava ID, Garmin ID, etc.).
     *
     * Used for deduplication during sync.
     */
    @Query("SELECT * FROM activities WHERE external_id = :externalId")
    suspend fun getActivityByExternalId(externalId: String): ActivityEntity?
    
    /**
     * Get activities within a date range.
     *
     * @param startMillis Start timestamp (epoch milliseconds)
     * @param endMillis End timestamp (epoch milliseconds)
     */
    @Query("""
        SELECT * FROM activities 
        WHERE completed_at BETWEEN :startMillis AND :endMillis 
        ORDER BY completed_at DESC
    """)
    suspend fun getActivitiesInRange(
        startMillis: Long,
        endMillis: Long
    ): List<ActivityEntity>
    
    /**
     * Search activities by title (case-insensitive).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE title LIKE '%' || :query || '%' 
        ORDER BY completed_at DESC
    """)
    suspend fun searchActivities(query: String): List<ActivityEntity>
    
    /**
     * Check if an activity exists by ID.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM activities WHERE id = :id)")
    suspend fun activityExists(id: String): Boolean
    
    /**
     * Check if an activity exists by external ID.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM activities WHERE external_id = :externalId)")
    suspend fun activityExistsByExternalId(externalId: String): Boolean
    
    // ========================
    // WRITE OPERATIONS (suspend)
    // ========================
    
    /**
     * Insert a single activity.
     *
     * If activity with same ID exists, replaces it (REPLACE strategy).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)
    
    /**
     * Insert multiple activities in a single transaction.
     *
     * More efficient than inserting one-by-one.
     *
     * @return List of row IDs for inserted activities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>): List<Long>
    
    /**
     * Update an existing activity.
     *
     * Activity must exist (based on primary key).
     */
    @Update
    suspend fun updateActivity(activity: ActivityEntity)
    
    /**
     * Update multiple activities in a single transaction.
     */
    @Update
    suspend fun updateActivities(activities: List<ActivityEntity>)
    
    /**
     * Delete a specific activity.
     */
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)
    
    /**
     * Delete activity by ID.
     */
    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)
    
    /**
     * Delete multiple activities by IDs.
     */
    @Query("DELETE FROM activities WHERE id IN (:ids)")
    suspend fun deleteActivitiesByIds(ids: List<String>)
    
    /**
     * Clear all activities (dangerous operation).
     *
     * Use with caution - typically for account reset or testing.
     */
    @Query("DELETE FROM activities")
    suspend fun clearAllActivities()
    
    // ========================
    // AGGREGATE QUERIES
    // ========================
    
    /**
     * Get total number of activities.
     */
    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivityCount(): Int
    
    /**
     * Get count by activity type.
     */
    @Query("SELECT COUNT(*) FROM activities WHERE type = :type")
    suspend fun getActivityCountByType(type: String): Int
    
    /**
     * Get total distance across all activities (in meters).
     */
    @Query("SELECT COALESCE(SUM(distance_meters), 0) FROM activities")
    suspend fun getTotalDistance(): Double
    
    /**
     * Get total distance in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(distance_meters), 0) FROM activities 
        WHERE completed_at BETWEEN :startMillis AND :endMillis
    """)
    suspend fun getTotalDistanceInRange(startMillis: Long, endMillis: Long): Double
    
    /**
     * Get total duration across all activities (in seconds).
     */
    @Query("SELECT COALESCE(SUM(duration_seconds), 0) FROM activities")
    suspend fun getTotalDuration(): Long
    
    /**
     * Get total duration in a date range.
     */
    @Query("""
        SELECT COALESCE(SUM(duration_seconds), 0) FROM activities 
        WHERE completed_at BETWEEN :startMillis AND :endMillis
    """)
    suspend fun getTotalDurationInRange(startMillis: Long, endMillis: Long): Long
    
    /**
     * Get oldest activity timestamp.
     *
     * Useful for determining data range.
     */
    @Query("SELECT MIN(completed_at) FROM activities")
    suspend fun getOldestActivityTimestamp(): Long?
    
    /**
     * Get most recent activity timestamp.
     */
    @Query("SELECT MAX(completed_at) FROM activities")
    suspend fun getNewestActivityTimestamp(): Long?
    
    // ========================
    // SPECIALIZED QUERIES
    // ========================
    
    /**
     * Get activities with GPS data (for map view).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE route_json IS NOT NULL 
        ORDER BY completed_at DESC 
        LIMIT :limit
    """)
    suspend fun getActivitiesWithRoute(limit: Int = 100): List<ActivityEntity>
    
    /**
     * Get activities with heart rate data (for HR analysis).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE avg_heart_rate IS NOT NULL 
        ORDER BY completed_at DESC
    """)
    suspend fun getActivitiesWithHeartRate(): List<ActivityEntity>
    
    /**
     * Get activities with power data (for power analysis).
     */
    @Query("""
        SELECT * FROM activities 
        WHERE avg_power IS NOT NULL 
        ORDER BY completed_at DESC
    """)
    suspend fun getActivitiesWithPower(): List<ActivityEntity>
}
