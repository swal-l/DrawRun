package com.orbital.run.domain.models

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Personal records across all activity types.
 *
 * Contains best performances for standard distances.
 * Optimized for fast UI rendering and comparison.
 */
data class PersonalRecords(
    val running: RunningRecords,
    val swimming: SwimmingRecords
) {
    /**
     * All records as a flat list for iteration.
     */
    val allRecords: List<Record>
        get() = running.allRecords + swimming.allRecords
    
    /**
     * Most recent record achievement.
     */
    val mostRecentRecord: Record?
        get() = allRecords.maxByOrNull { it.achievedAt }
}

/**
 * Running-specific records.
 */
data class RunningRecords(
    val best1k: Record?,
    val best5k: Record?,
    val best10k: Record?,
    val bestHalfMarathon: Record?,
    val bestMarathon: Record?,
    val longestRun: Record?
) {
    val allRecords: List<Record>
        get() = listOfNotNull(best1k, best5k, best10k, bestHalfMarathon, bestMarathon, longestRun)
}

/**
 * Swimming-specific records.
 */
data class SwimmingRecords(
    val best100m: Record?,
    val best200m: Record?,
    val best400m: Record?,
    val best800m: Record?,
    val best1500m: Record?,
    val longestSwim: Record?
) {
    val allRecords: List<Record>
        get() = listOfNotNull(best100m, best200m, best400m, best800m, best1500m, longestSwim)
}

/**
 * Individual personal record.
 *
 * @property distance Record distance
 * @property duration Time achieved
 * @property achievedAt When this record was set
 * @property activityId Reference to the activity
 */
data class Record(
    val distance: Distance,
    val duration: Duration,
    val achievedAt: Instant,
    val activityId: String
) {
    val pace: Duration
        get() {
            val secondsPerKm = (duration.seconds /distance.kilometers).toLong()
            return Duration.ofSeconds(secondsPerKm)
        }
}

/**
 * Performance Management Chart (PMC) data point.
 *
 * Represents training load metrics for a single day.
 * Designed for efficient charting with pre-computed values.
 *
 * @property date Day this point represents
 * @property fitness Chronic Training Load (CTL) - 42-day rolling average
 * @property fatigue Acute Training Load (ATL) - 7-day rolling average
 * @property form Training Stress Balance (TSB) = CTL - ATL
 * @property trainingLoad Daily training load (TSS)
 */
data class PMCPoint(
    val date: LocalDate,
    val fitness: Double,
    val fatigue: Double,
    val form: Double,
    val trainingLoad: Double
) {
    /**
     * Whether athlete is in optimal training state.
     */
    val isOptimalState: Boolean
        get() = form in -10.0..5.0 && fitness > 40.0
    
    /**
     * Risk level for this day.
     */
    val riskLevel: RiskLevel
        get() = when {
            fatigue > fitness * 1.3 -> RiskLevel.HIGH
            fatigue > fitness * 1.1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
}

/**
 * Training risk level.
 */
enum class RiskLevel(val displayName: String, val color: String) {
    LOW("Faible", "#4CAF50"),
    MEDIUM("Modéré", "#FF9800"),
    HIGH("Élevé", "#F44336")
}

/**
 * Aggregated statistics for a time period.
 *
 * Pre-computed for performance. Suitable for dashboard cards.
 * Follows Stripe's metric design: simple, clear, comparable.
 *
 * @property period Time range for these stats
 * @property totalDistance Sum of distances
 * @property totalDuration Sum of durations
 * @property activityCount Number of activities
 * @property averageHeartRate Mean HR across all activities
 * @property totalElevationGain Sum of elevation
 */
data class PeriodStatistics(
    val period: TimePeriod,
    val totalDistance: Distance,
    val totalDuration: Duration,
    val activityCount: Int,
    val averageHeartRate: Int?,
    val totalElevationGain: Int?
) {
    /**
     * Average distance per activity.
     */
    val averageDistance: Distance
        get() = if (activityCount > 0) {
            Distance(totalDistance.meters / activityCount)
        } else Distance(0.0)
    
    /**
     * Average duration per activity.
     */
    val averageDuration: Duration
        get() = if (activityCount > 0) {
            Duration.ofSeconds(totalDuration.seconds / activityCount)
        } else Duration.ZERO
    
    /**
     * Overall average pace.
     */
    val averagePace: Duration?
        get() = if (totalDistance.kilometers > 0) {
            val secondsPerKm = (totalDuration.seconds / totalDistance.kilometers).toLong()
            Duration.ofSeconds(secondsPerKm)
        } else null
}

/**
 * Time period for aggregations.
 */
data class TimePeriod(
    val start: LocalDate,
    val end: LocalDate
) {
    val durationDays: Long
        get() = java.time.Period.between(start, end).days.toLong()
    
    companion object {
        fun last7Days(): TimePeriod {
            val end = LocalDate.now()
            val start = end.minusDays(7)
            return TimePeriod(start, end)
        }
        
        fun last30Days(): TimePeriod {
            val end = LocalDate.now()
            val start = end.minusDays(30)
            return TimePeriod(start, end)
        }
        
        fun currentWeek(): TimePeriod {
            val today = LocalDate.now()
            val start = today.with(java.time.DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            return TimePeriod(start, end)
        }
    }
}

/**
 * Volume by sport type (for pie/bar charts).
 *
 * Optimized for chart rendering: pre-formatted, color-coded, sorted.
 *
 * @property type Activity type
 * @property distance Total distance
 * @property activityCount Number of activities
 * @property percentage Percentage of total volume
 */
data class VolumeByType(
    val type: ActivityType,
    val distance: Distance,
    val activityCount: Int,
    val percentage: Double
) {
    /**
     * Color for chart rendering.
     */
    val chartColor: String
        get() = when (type) {
            ActivityType.RUNNING, ActivityType.EASY_RUN, ActivityType.LONG_RUN, 
            ActivityType.INTERVALS, ActivityType.RECOVERY -> "#2196F3"
            ActivityType.SWIMMING -> "#00BCD4"
            ActivityType.CYCLING -> "#FF9800"
        }
}

/**
 * Heart rate zone distribution for a set of activities.
 *
 * Shows time spent in each zone (percentage-based for charts).
 *
 * @property zoneNumber Zone identifier (1-5)
 * @property zoneName Human-readable name
 * @property duration Total time in this zone
 * @property percentage Percentage of total duration
 */
data class ZoneDistribution(
    val zoneNumber: Int,
    val zoneName: String,
    val duration: Duration,
    val percentage: Double
) {
    /**
     * Color for chart rendering.
     */
    val chartColor: String
        get() = when (zoneNumber) {
            1 -> "#9E9E9E" // Recovery
            2 -> "#4CAF50" // Endurance
            3 -> "#FFEB3B" // Tempo
            4 -> "#FF9800" // Threshold
            5 -> "#F44336" // VO2max
            else -> "#000000"
        }
}

/**
 * Trend direction for comparison.
 */
enum class Trend(val emoji: String) {
    UP("↗️"),
    DOWN("↘️"),
    STABLE("→")
}
