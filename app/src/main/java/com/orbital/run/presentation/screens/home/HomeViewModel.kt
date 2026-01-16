package com.orbital.run.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.repositories.ActivityRepository
import com.orbital.run.data.migration.LegacyJsonMigrator
import com.orbital.run.data.migration.MigrationProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for Home/Dashboard screen.
 *
 * Manages:
 * - Weekly statistics (distance, duration, activity count)
 * - Recent activities list
 * - Migration progress (if in progress)
 * - Loading/Error states
 *
 * Exposes single UI state flow for clean state management.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val migrator: LegacyJsonMigrator
) : ViewModel() {
    
    // ========================
    // UI STATE
    // ========================
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    
    /**
     * Single source of truth for UI state.
     *
     * Observe this in HomeScreen to render appropriate UI.
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeData()
    }
    
    // ========================
    // DATA OBSERVATION
    // ========================
    
    /**
     * Observe activities and migration progress.
     *
     * Combines multiple flows into single UI state.
     */
    private fun observeData() {
        viewModelScope.launch {
            combine(
                activityRepository.observeActivities(),
                migrator.progressFlow
            ) { activities, migrationProgress ->
                // If migration in progress, show migration UI
                if (migrationProgress !is MigrationProgress.Idle && 
                    migrationProgress !is MigrationProgress.Complete) {
                    return@combine HomeUiState.Migrating(migrationProgress)
                }
                
                // Calculate weekly stats
                val weeklyStats = calculateWeeklyStats(activities)
                
                // Get recent activities (last 5)
                val recentActivities = activities.take(5)
                
                // Build content state
                HomeUiState.Content(
                    weeklyStats = weeklyStats,
                    recentActivities = recentActivities,
                    lastActivity = activities.firstOrNull()
                )
            }
            .catch { error ->
                _uiState.value = HomeUiState.Error(
                    message = error.message ?: "Erreur inconnue"
                )
            }
            .collect { state ->
                _uiState.value = state
            }
        }
    }
    
    // ========================
    // STATISTICS CALCULATION
    // ========================
    
    /**
     * Calculate statistics for current week.
     */
    private fun calculateWeeklyStats(activities: List<Activity>): WeeklyStats {
        // Get start of current week (Monday)
        val today = LocalDate.now()
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
        val startOfWeekInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        // Filter activities from this week
        val weekActivities = activities.filter { activity ->
            activity.completedAt.isAfter(startOfWeekInstant)
        }
        
        // Calculate totals
        val totalDistance = weekActivities.sumOf { it.distance.kilometers }
        val totalDuration = weekActivities.sumOf { it.duration.seconds }
        val activityCount = weekActivities.size
        
        // Calculate previous week for comparison
        val previousWeekStart = startOfWeek.minusWeeks(1)
        val previousWeekEnd = startOfWeek.minusDays(1)
        val previousWeekStartInstant = previousWeekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val previousWeekEndInstant = previousWeekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
        
        val previousWeekActivities = activities.filter { activity ->
            activity.completedAt.isAfter(previousWeekStartInstant) &&
            activity.completedAt.isBefore(previousWeekEndInstant)
        }
        
        val previousWeekDistance = previousWeekActivities.sumOf { it.distance.kilometers }
        
        // Calculate trend (percentage change)
        val distanceTrend = if (previousWeekDistance > 0) {
            ((totalDistance - previousWeekDistance) / previousWeekDistance * 100).toFloat()
        } else if (totalDistance > 0) {
            100f  // Infinite improvement
        } else {
            0f
        }
        
        return WeeklyStats(
            totalDistance = totalDistance,
            totalDuration = totalDuration,
            activityCount = activityCount,
            distanceTrend = distanceTrend
        )
    }
    
    // ========================
    // USER ACTIONS
    // ========================
    
    /**
     * Refresh data manually.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            observeData()
        }
    }
    
    /**
     * Retry after error.
     */
    fun retry() {
        refresh()
    }
}

// ========================
// UI STATE SEALED CLASS
// ========================

/**
 * UI state for Home screen.
 *
 * Uses sealed class for exhaustive when statements and type-safe state handling.
 */
sealed class HomeUiState {
    
    /** Initial loading state */
    object Loading : HomeUiState()
    
    /** Migration in progress - show migration UI */
    data class Migrating(val progress: MigrationProgress) : HomeUiState()
    
    /** Content ready to display */
    data class Content(
        val weeklyStats: WeeklyStats,
        val recentActivities: List<Activity>,
        val lastActivity: Activity?
    ) : HomeUiState()
    
    /** Error occurred */
    data class Error(val message: String) : HomeUiState()
}

/**
 * Weekly statistics data class.
 */
data class WeeklyStats(
    val totalDistance: Double,  // km
    val totalDuration: Long,    // seconds
    val activityCount: Int,
    val distanceTrend: Float    // percentage change from last week
) {
    /**
     * Average pace in seconds per km.
     */
    val averagePace: Long?
        get() = if (totalDistance > 0) {
            (totalDuration / totalDistance).toLong()
        } else null
    
    /**
     * Total duration in hours.
     */
    val totalHours: Double
        get() = totalDuration / 3600.0
}
