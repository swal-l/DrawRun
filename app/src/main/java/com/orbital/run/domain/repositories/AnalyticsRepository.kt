package com.orbital.run.domain.repositories

import com.orbital.run.domain.models.*

/**
 * Repository for analytics and performance calculations.
 *
 * Handles heavy computations for PMC, personal records, and statistics.
 * Implementations should cache results for performance.
 */
interface AnalyticsRepository {
    
    /**
     * Calculate Performance Management Chart data.
     *
     * @param activities Activities to analyze
     * @return Daily PMC points (fitness, fatigue, form)
     */
    suspend fun calculatePMC(activities: List<Activity>): List<PMCPoint>
    
    /**
     * Get current personal records.
     */
    suspend fun getPersonalRecords(): PersonalRecords
    
    /**
     * Update personal records based on a new activity.
     *
     * Checks if activity sets any new records and updates accordingly.
     */
    suspend fun updatePersonalRecords(activity: Activity)
    
    /**
     * Recalculate all personal records from scratch.
     *
     * Expensive operation - only call after bulk imports or data cleanup.
     */
    suspend fun recalculateAllPersonalRecords()
    
    /**
     * Get statistics for a time period.
     */
    suspend fun getStatistics(period: TimePeriod): PeriodStatistics
    
    /**
     * Get volume breakdown by activity type.
     */
    suspend fun getVolumeByType(activities: List<Activity>): List<VolumeByType>
    
    /**
     * Get heart rate zone distribution.
     */
    suspend fun getZoneDistribution(activities: List<Activity>): List<ZoneDistribution>
}
