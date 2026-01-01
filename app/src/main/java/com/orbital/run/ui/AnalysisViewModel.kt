package com.orbital.run.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import android.util.Log
import com.orbital.run.api.HealthConnectManager
import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(getApplication()) }
    private val _todaySteps = MutableStateFlow<Long?>(null)
    val todaySteps: StateFlow<Long?> = _todaySteps

    private val _weeklySteps = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val weeklySteps: StateFlow<List<Pair<String, Long>>> = _weeklySteps

    private val _avgHeartRate = MutableStateFlow<Double?>(null)
    val avgHeartRate: StateFlow<Double?> = _avgHeartRate

    private val _totalCalories = MutableStateFlow<Double?>(null)
    val totalCalories: StateFlow<Double?> = _totalCalories

    private val _pmcPoints = MutableStateFlow<List<AdvancedAnalytics.PMCPoint>>(emptyList())
    val pmcPoints: StateFlow<List<AdvancedAnalytics.PMCPoint>> = _pmcPoints

    private val _sportBreakdown = MutableStateFlow<Map<WorkoutType, Double>>(emptyMap())
    val sportBreakdown: StateFlow<Map<WorkoutType, Double>> = _sportBreakdown
    
    private val _coachInsight = MutableStateFlow<String>("Chargement de vos analyses...")
    val coachInsight: StateFlow<String> = _coachInsight

    private val _globalSummary = MutableStateFlow<AdvancedAnalytics.GlobalSummary?>(null)
    val globalSummary: StateFlow<AdvancedAnalytics.GlobalSummary?> = _globalSummary
    
    private val _hcSdkStatus = MutableStateFlow<Int>(0)
    val hcSdkStatus: StateFlow<Int> = _hcSdkStatus

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<android.app.Application>()
            
            // SDK Status
            val status = com.orbital.run.api.HealthConnectManager.checkAvailability(context)
            _hcSdkStatus.value = status
            
            // Health Data
            if (status == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                fetchTodaySteps()
                fetchWeeklySteps()
                fetchAvgHeartRate()
                fetchTotalCalories()
            }
            
            val history = Persistence.loadHistory(context)
            
            // Analytics
            val points = AdvancedAnalytics.calculateDailyPMC(history)
            _pmcPoints.value = points
            
            val breakdown = history.groupBy { it.type }
                .mapValues { it.value.sumOf { act -> act.distanceKm } }
            _sportBreakdown.value = breakdown
            
            // COACH INSIGHT ENGINE
            generateInsight(history)
        }
    }

    private fun generateInsight(history: List<Persistence.CompletedActivity>) {
        if (history.isEmpty()) {
            _coachInsight.value = "Commencez à bouger pour recevoir des conseils personnalisés !"
            return
        }
        
        val metrics = AdvancedAnalytics.calculateLongitudinal(history)
        val tsb = metrics.form
        val acwr = metrics.acwr
        
        val insight = when {
            acwr > 1.5 -> "⚠️ Attention au surmenage. Votre charge d'entraînement augmente trop vite. Risque de blessure élevé."
            acwr < 0.8 && history.size > 5 -> "📉 Votre condition physique décline. Essayez d'augmenter progressivement votre volume."
            tsb < -20 -> "🔥 Fatigue importante détectée. Une journée de repos ou une séance très légère serait bénéfique."
            tsb > 10 && acwr > 1.0 -> "✅ Forme optimale ! Vous êtes prêt pour une grosse séance ou un test de performance."
            else -> "💪 Entraînement équilibré. Continuez sur cette lancée pour maintenir votre progression."
        }
        _coachInsight.value = insight
        
        // GLOBAL SUMMARY
        val summary = AdvancedAnalytics.calculateGlobalSummary(history)
        _globalSummary.value = summary
    }

    private fun fetchTodaySteps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
                val response = healthConnectClient.aggregate(
                    androidx.health.connect.client.request.AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                _todaySteps.value = response[StepsRecord.COUNT_TOTAL] ?: 0L
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error fetching today steps: ${e.message}")
            }
        }
    }

    private fun fetchWeeklySteps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val start = now.minus(7, ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now)
                )
                val records = healthConnectClient.readRecords(request)
                // Group by day
                val map = mutableMapOf<String, Long>()
                records.records.forEach { rec ->
                    val date = ZonedDateTime.ofInstant(rec.startTime, java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                    map[date] = (map[date] ?: 0L) + rec.count
                }
                // Ensure 7 days list
                val list = (0..6).map { i ->
                    val date = ZonedDateTime.now().minus(i.toLong(), ChronoUnit.DAYS).toLocalDate().toString()
                    date to (map[date] ?: 0L)
                }.reversed()
                _weeklySteps.value = list
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error fetching weekly steps: ${e.message}")
            }
        }
    }

    private fun fetchAvgHeartRate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val start = now.minus(30, ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now)
                )
                val records = healthConnectClient.readRecords(request)
                if (records.records.isNotEmpty()) {
                    val avg = records.records.flatMap { it.samples }.map { it.beatsPerMinute }.average()
                    _avgHeartRate.value = avg
                }
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error fetching heart rate: ${e.message}")
            }
        }
    }

    private fun fetchTotalCalories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Instant.now()
                val start = now.minus(30, ChronoUnit.DAYS)
                val request = ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now)
                )
                val records = healthConnectClient.readRecords(request)
                val total = records.records.sumOf { it.energy.inKilocalories }
                _totalCalories.value = total
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Error fetching calories: ${e.message}")
            }
        }
    }
}
