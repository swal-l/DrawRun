package com.orbital.run.logic

import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.exp

data class FitnessStatus(
    val date: Long,
    val ctl: Double, // Chronic Training Load (Fitness)
    val atl: Double, // Acute Training Load (Fatigue)
    val tsb: Double, // Training Stress Balance (Form)
    val acwr: Double? // Acute:Chronic Workload Ratio
)

object PerformanceEngine {

    /**
     * Calcule la charge (TRIMP ou Load) d'une activité.
     * Pour l'instant, on utilise Suffer Score (Strava) ou une approximation TRIMP.
     */
    fun calculateLoad(activity: Persistence.CompletedActivity): Double {
        // Priority 1: Strava Suffer Score
        if (activity.sufferScore != null) return activity.sufferScore.toDouble()
        
        // Priority 2: Simple HR * Duration approximation
        // TSS approximation: (DurationSec * NormPower * IF) / (FTP * 3600) * 100
        // HR Approximation: Zoning.
        // Simple fallback: Duration(mins) * (RPE/10 normally, but we assume moderate intensity if unknown)
        // Let's use duration * 0.7 as base points/min roughly? 
        // Or better: AnalysisEngine.trimp
        
        val science = AnalysisEngine.calculateScience(activity)
        if (science.trimp != null) return science.trimp.toDouble()
        
        return activity.durationMin.toDouble() // 1 point per minute fallback
    }

    /**
     * Compute fitness stats for a target date based on history.
     */
    fun computeStatsForDate(history: List<Persistence.CompletedActivity>, targetDate: Long): FitnessStatus {
        // Sort history by date ascending
        // Filter history BEFORE targetDate (inclusive? No, usually Form is state BEFORE the workout or calculated EOD)
        // Let's calculate EOD state including the workout, or Morning state?
        // Usually TSB is 'Yesterday CTL - Yesterday ATL'. Form is "Start of day".
        
        // 1. Convert to Daily Loads
        // Map: DayEpoch -> Load
        
        val sorted = history.filter { it.date <= targetDate }
                            .sortedBy { it.date }
        
        if (sorted.isEmpty()) return FitnessStatus(targetDate, 0.0, 0.0, 0.0, 0.0)

        // Time Constants
        val decayCtl = 42.0
        val decayAtl = 7.0
        
        // Rolling Calculation
        var ctl = 0.0
        var atl = 0.0
        
        // We simulate day by day from first activity to target date?
        // Efficient way: Exp avg update.
        //Banister uses e^(-1/tau).

        val firstDate = sorted.first().date
        val lastDate = targetDate
        
        // Day loop
        val oneDayMs = TimeUnit.DAYS.toMillis(1)
        var currentProfileDate = firstDate
        
        // Map activities to days for faster lookup
        val activityMap = sorted.groupBy { 
            // Normalize to Day Start
            it.date / oneDayMs 
        }

        while (currentProfileDate <= lastDate) {
            val dayIndex = currentProfileDate / oneDayMs
            val acts = activityMap[dayIndex] ?: emptyList()
            val dailyLoad = acts.sumOf { calculateLoad(it) }
            
            // Banister Formula EWMA
            val alphaCtl = 1.0 / decayCtl
            val alphaAtl = 1.0 / decayAtl
            
            ctl = ctl * (1 - alphaCtl) + dailyLoad * alphaCtl
            atl = atl * (1 - alphaAtl) + dailyLoad * alphaAtl
            
            currentProfileDate += oneDayMs
        }

        val tsb = ctl - atl
        
        // ACWR: Load 7d / Load 28d
        // We use Exp Avg approximation: ATL / CTL (Ratio)
        val acwr = if (ctl > 10) atl / ctl else null 
        
        return FitnessStatus(targetDate, ctl, atl, tsb, acwr)
    }
    
    // Helpers
}
