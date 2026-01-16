package com.orbital.run.presentation.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.repositories.ActivityRepository
import com.orbital.run.presentation.components.charts.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel for Analytics screen.
 *
 * Manages:
 * - Time range filters (7D, 30D, 12M, All)
 * - Chart data with intelligent sampling
 * - Period statistics and insights
 * - Performance comparison with previous period
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {
    
    companion object {
        private const val MAX_CHART_POINTS = 50  // Sampling threshold
    }
    
    // ========================
    // STATE
    // ========================
    
    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        observeData()
    }
    
    // ========================
    // DATA OBSERVATION
    // ========================
    
    /**
     * Observe activities and compute analytics based on selected time range.
     */
    private fun observeData() {
        viewModelScope.launch {
            combine(
                activityRepository.observeActivities(),
                selectedTimeRange
            ) { activities, timeRange ->
                computeAnalytics(activities, timeRange)
            }
            .catch { error ->
                _uiState.value = AnalyticsUiState.Error(
                    message = error.message ?: "Erreur inconnue"
                )
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Compute full analytics for given time range.
     */
    private fun computeAnalytics(
        allActivities: List<Activity>,
        timeRange: TimeRange
    ): AnalyticsUiState {
        
        // Filter activities for current period
        val periodStart = timeRange.getStartDate()
        val currentPeriodActivities = allActivities.filter { activity ->
            activity.completedAt.isAfter(periodStart)
        }
        
        if (currentPeriodActivities.isEmpty()) {
            return AnalyticsUiState.Empty(timeRange)
        }
        
        // Filter activities for previous period (comparison)
        val previousPeriodStart = timeRange.getPreviousPeriodStart()
        val previousPeriodActivities = allActivities.filter { activity ->
            activity.completedAt.isAfter(previousPeriodStart) &&
            activity.completedAt.isBefore(periodStart)
        }
        
        // Calculate stats
        val currentStats = calculatePeriodStats(currentPeriodActivities)
        val previousStats = calculatePeriodStats(previousPeriodActivities)
        
        // Generate chart data with sampling
        val chartData = generateChartData(currentPeriodActivities, timeRange)
        
        // Generate insights
        val insights = generateInsights(currentStats, previousStats, timeRange)
        
        return AnalyticsUiState.Content(
            timeRange = timeRange,
            chartData = chartData,
            stats = currentStats,
            insights = insights
        )
    }
    
    // ========================
    // CHART DATA GENERATION
    // ========================
    
    /**
     * Generate chart data with intelligent sampling.
     *
     * If data points exceed MAX_CHART_POINTS, applies sampling to maintain performance.
     */
    private fun generateChartData(
        activities: List<Activity>,
        timeRange: TimeRange
    ): List<ChartPoint> {
        
        // Group activities by time bucket
        val grouped = when (timeRange) {
            TimeRange.WEEK -> groupByDay(activities)
            TimeRange.MONTH -> groupByDay(activities)
            TimeRange.YEAR -> groupByWeek(activities)
            TimeRange.ALL -> groupByMonth(activities)
        }
        
        // Apply sampling if too many points
        val sampled = if (grouped.size > MAX_CHART_POINTS) {
            sampleData(grouped, MAX_CHART_POINTS)
        } else {
            grouped
        }
        
        return sampled
    }
    
    /**
     * Group activities by day.
     */
    private fun groupByDay(activities: List<Activity>): List<ChartPoint> {
        return activities
            .groupBy { 
                it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .entries
            .sortedBy { it.key }
            .map { (date, dayActivities) ->
                ChartPoint(
                    value = dayActivities.sumOf { it.distance.kilometers }.toFloat(),
                    label = formatDayLabel(date)
                )
            }
    }
    
    /**
     * Group activities by week.
     */
    private fun groupByWeek(activities: List<Activity>): List<ChartPoint> {
        return activities
            .groupBy { 
                val date = it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            }
            .entries
            .sortedBy { it.key }
            .map { (weekStart, weekActivities) ->
                ChartPoint(
                    value = weekActivities.sumOf { it.distance.kilometers }.toFloat(),
                    label = "Sem. ${weekStart.get(java.time.temporal.WeekFields.ISO.weekOfYear())}"
                )
            }
    }
    
    /**
     * Group activities by month.
     */
    private fun groupByMonth(activities: List<Activity>): List<ChartPoint> {
        return activities
            .groupBy { 
                val date = it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                date.withDayOfMonth(1)
            }
            .entries
            .sortedBy { it.key }
            .map { (monthStart, monthActivities) ->
                ChartPoint(
                    value = monthActivities.sumOf { it.distance.kilometers }.toFloat(),
                    label = monthStart.month.getDisplayName(
                        java.time.format.TextStyle.SHORT,
                        java.util.Locale.FRENCH
                    )
                )
            }
    }
    
    /**
     * Sample data to reduce points while preserving shape.
     *
     * Uses simple uniform sampling - can be upgraded to LTTB (Largest Triangle Three Buckets).
     */
    private fun sampleData(
        data: List<ChartPoint>,
        targetSize: Int
    ): List<ChartPoint> {
        if (data.size <= targetSize) return data
        
        val step = data.size.toFloat() / targetSize
        return (0 until targetSize).map { i ->
            val index = (i * step).roundToInt().coerceIn(0, data.lastIndex)
            data[index]
        }
    }
    
    // ========================
    // STATISTICS CALCULATION
    // ========================
    
    /**
     * Calculate comprehensive statistics for a period.
     */
    private fun calculatePeriodStats(activities: List<Activity>): PeriodStats {
        if (activities.isEmpty()) {
            return PeriodStats(
                totalDistance = 0.0,
                totalDuration = 0L,
                totalElevation = 0,
                activityCount = 0,
                averagePace = null,
                averageHeartRate = null
            )
        }
        
        val totalDistance = activities.sumOf { it.distance.kilometers }
        val totalDuration = activities.sumOf { it.duration.seconds }
        val totalElevation = activities.sumOf { it.elevationGain ?: 0 }
        
        val averagePace = if (totalDistance > 0) {
            (totalDuration / totalDistance).toLong()
        } else null
        
        val activitiesWithHr = activities.filter { it.averageHeartRate != null }
        val averageHeartRate = if (activitiesWithHr.isNotEmpty()) {
            activitiesWithHr.mapNotNull { it.averageHeartRate }.average().roundToInt()
        } else null
        
        return PeriodStats(
            totalDistance = totalDistance,
            totalDuration = totalDuration,
            totalElevation = totalElevation,
            activityCount = activities.size,
            averagePace = averagePace,
            averageHeartRate = averageHeartRate
        )
    }
    
    // ========================
    // INSIGHTS GENERATION
    // ========================
    
    /**
     * Generate performance insights comparing current and previous periods.
     */
    private fun generateInsights(
        current: PeriodStats,
        previous: PeriodStats,
        timeRange: TimeRange
    ): List<Insight> {
        val insights = mutableListOf<Insight>()
        
        // Distance comparison
        if (previous.totalDistance > 0) {
            val distanceChange = current.totalDistance - previous.totalDistance
            val distanceChangePercent = (distanceChange / previous.totalDistance * 100).roundToInt()
            
            val periodName = when (timeRange) {
                TimeRange.WEEK -> "la semaine dernière"
                TimeRange.MONTH -> "le mois dernier"
                TimeRange.YEAR -> "l'année dernière"
                TimeRange.ALL -> "la période précédente"
            }
            
            if (distanceChange > 0) {
                insights.add(
                    Insight(
                        text = "Vous avez couru ${String.format("%.1f", distanceChange)} km de plus que $periodName",
                        type = InsightType.POSITIVE,
                        changePercent = distanceChangePercent
                    )
                )
            } else if (distanceChange < 0) {
                insights.add(
                    Insight(
                        text = "Votre volume a diminué de ${String.format("%.1f", -distanceChange)} km par rapport à $periodName",
                        type = InsightType.NEUTRAL,
                        changePercent = distanceChangePercent
                    )
                )
            }
        }
        
        // Pace comparison
        if (current.averagePace != null && previous.averagePace != null) {
            val paceImprovement = previous.averagePace - current.averagePace
            if (paceImprovement > 5) {  // More than 5 seconds improvement
                val minutes = paceImprovement / 60
                val seconds = paceImprovement % 60
                insights.add(
                    Insight(
                        text = "Votre allure s'est améliorée de ${minutes}min${seconds}s par km",
                        type = InsightType.POSITIVE,
                        changePercent = null
                    )
                )
            }
        }
        
        // Activity frequency
        if (current.activityCount >= 3 && timeRange == TimeRange.WEEK) {
            insights.add(
                Insight(
                    text = "Belle constance ! ${current.activityCount} activités cette semaine",
                    type = InsightType.POSITIVE,
                    changePercent = null
                )
            )
        }
        
        return insights
    }
    
    // ========================
    // USER ACTIONS
    // ========================
    
    /**
     * Change selected time range.
     */
    fun selectTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
    }
    
    // ========================
    // HELPER FUNCTIONS
    // ========================
    
    private fun formatDayLabel(date: LocalDate): String {
        return date.dayOfMonth.toString()
    }
}

// ========================
// UI STATE
// ========================

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    
    data class Empty(val timeRange: TimeRange) : AnalyticsUiState()
    
    data class Content(
        val timeRange: TimeRange,
        val chartData: List<ChartPoint>,
        val stats: PeriodStats,
        val insights: List<Insight>
    ) : AnalyticsUiState()
    
    data class Error(val message: String) : AnalyticsUiState()
}

// ========================
// DATA CLASSES
// ========================

/**
 * Time range filter options.
 */
enum class TimeRange {
    WEEK,
    MONTH,
    YEAR,
    ALL;
    
    fun getStartDate(): Instant {
        val now = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        return when (this) {
            WEEK -> now.minusDays(7).toInstant()
            MONTH -> now.minusDays(30).toInstant()
            YEAR -> now.minusMonths(12).toInstant()
            ALL -> Instant.EPOCH
        }
    }
    
    fun getPreviousPeriodStart(): Instant {
        val now = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
        return when (this) {
            WEEK -> now.minusDays(14).toInstant()
            MONTH -> now.minusDays(60).toInstant()
            YEAR -> now.minusMonths(24).toInstant()
            ALL -> Instant.EPOCH
        }
    }
    
    fun getLabel(): String = when (this) {
        WEEK -> "7 jours"
        MONTH -> "30 jours"
        YEAR -> "12 mois"
        ALL -> "Tout"
    }
}

/**
 * Statistics for a time period.
 */
data class PeriodStats(
    val totalDistance: Double,      // km
    val totalDuration: Long,         // seconds
    val totalElevation: Int,         // meters
    val activityCount: Int,
    val averagePace: Long?,          // seconds per km
    val averageHeartRate: Int?       // bpm
)

/**
 * Performance insight.
 */
data class Insight(
    val text: String,
    val type: InsightType,
    val changePercent: Int?
)

enum class InsightType {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}
