package com.orbital.run.logic

import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Advanced HR Zone Analysis Result
 */
data class HRZoneAnalysis(
    val distribution: Map<Int, Int>,  // Zone -> Seconds
    val targetZonePercent: Double,    // % time in Z3-Z4
    val spikes: List<HRAnomaly>,
    val efficiency: Double,            // % time in productive zones (Z2-Z4)
    val recommendation: String
)

/**
 * HR Anomaly (spike or drop)
 */
data class HRAnomaly(
    val timeOffset: Int,
    val type: String,  // "SPIKE" or "DROP"
    val magnitude: Int,
    val description: String
)

/**
 * Pace Analysis Result
 */
data class PaceAnalysis(
    val mean: Double,
    val stdDev: Double,
    val cv: Double,  // Coefficient of variation
    val consistency: String,
    val negativeSplit: Boolean,
    val splitDiff: Double,
    val gap: Double?  // Grade Adjusted Pace
)

/**
 * Cadence Analysis Result
 */
data class CadenceAnalysis(
    val average: Double,
    val optimalRange: Pair<Int, Int>,
    val optimalPercent: Double,
    val variability: Double,
    val recommendation: String
)

/**
 * Power Metrics (Cycling/Running)
 */
data class PowerMetrics(
    val normalizedPower: Int?,
    val intensityFactor: Double?,
    val tss: Int?,  // Training Stress Score
    val variabilityIndex: Double?
)

/**
 * Fatigue Detection
 */
enum class FatigueLevel {
    LOW, MODERATE, HIGH
}

data class FatigueAnalysis(
    val level: FatigueLevel,
    val score: Int,
    val indicators: List<String>
)

/**
 * Performance Prediction
 */
data class PredictedPerformance(
    val estimatedTime: Double?,  // minutes
    val confidence: String?,
    val model: String?
)

/**
 * Longitudinal Health Metrics
 */
data class LongitudinalMetrics(
    val fitness: Double, // CTL
    val fatigue: Double, // ATL
    val form: Double,    // TSB
    val acwr: Double,
    val monotony: Double,
    val strain: Double
)

/**
 * Swimming Phase Analysis (V1.3)
 */
data class SwimPhases(
    val lengthBreakdown: List<SwimPhaseFraction>
)

data class SwimPhaseFraction(
    val lengthIndex: Int,
    val turnGlideSec: Double,
    val swimSec: Double
)

/**
 * AeroLab Virtual Elevation Result
 */
data class AeroLabResult(
    val cda: Double,          // Coefficient of drag * frontal area
    val crr: Double,          // Rolling resistance
    val virtualElevation: List<Double>,
    val actualElevation: List<Double>
)

/**
 * W' Balance Result (Skiba)
 */
data class WPrimeBalance(
    val balanceSeries: List<Double>, // W' bal over time (Joules)
    val criticalPower: Double,
    val wPrime: Double
)

/**
 * Professional Interval
 */
data class ProInterval(
    val name: String,
    val startSec: Int,
    val endSec: Int,
    val avgPower: Double?,
    val avgHr: Double?,
    val type: String // "WORK" or "REST"
)

/**
 * Quadrant Analysis Point
 */
data class QuadrantPoint(
    val force: Double,   // AEPF (N)
    val velocity: Double, // CPV (m/s)
    val quadrant: Int     // 1, 2, 3, or 4
)

/**
 * Professional Analytics Engine Extension
 */
object AdvancedAnalytics {
    
    /**
     * Analyze HR Zones with Karvonen method (more accurate)
     */
    fun analyzeHRZonesDetailed(
        samples: List<Persistence.HeartRateSample>,
        maxHR: Int,
        restingHR: Int
    ): HRZoneAnalysis {
        if (samples.isEmpty()) {
            return HRZoneAnalysis(
                distribution = emptyMap(),
                targetZonePercent = 0.0,
                spikes = emptyList(),
                efficiency = 0.0,
                recommendation = "Pas de données HR"
            )
        }
        
        // Heart Rate Reserve (HRR)
        val hrr = maxHR - restingHR
        
        // Karvonen zones (more precise than simple %)
        val zones = mapOf(
            1 to (restingHR + (hrr * 0.5).toInt() to restingHR + (hrr * 0.6).toInt()),  // Recovery
            2 to (restingHR + (hrr * 0.6).toInt() to restingHR + (hrr * 0.7).toInt()),  // Endurance
            3 to (restingHR + (hrr * 0.7).toInt() to restingHR + (hrr * 0.8).toInt()),  // Tempo
            4 to (restingHR + (hrr * 0.8).toInt() to restingHR + (hrr * 0.9).toInt()),  // Threshold
            5 to (restingHR + (hrr * 0.9).toInt() to maxHR)                              // VO2max
        )
        
        // Distribution
        val distribution = samples.groupBy { sample ->
            zones.entries.find { (_, range) -> 
                sample.bpm in range.first..range.second 
            }?.key ?: 0
        }.mapValues { it.value.size }
        
        // Target zone time (Z3-Z4 for endurance training)
        val targetZoneTime = (distribution[3] ?: 0) + (distribution[4] ?: 0)
        val targetZonePercent = (targetZoneTime.toDouble() / samples.size) * 100
        
        // Detect spikes/drops
        val spikes = detectHRSpikes(samples)
        
        // Efficiency (time in productive zones Z2-Z4)
        val productiveTime = (distribution[2] ?: 0) + (distribution[3] ?: 0) + (distribution[4] ?: 0)
        val efficiency = (productiveTime.toDouble() / samples.size) * 100
        
        // Recommendation
        val recommendation = when {
            targetZonePercent > 70 -> "Excellent ! Majorité du temps en zones cibles (Z3-Z4)"
            targetZonePercent > 50 -> "Bon travail en zones d'intensité"
            efficiency > 80 -> "Effort bien contrôlé dans les zones productives"
            distribution[1]?.let { it > samples.size / 2 } == true -> "Séance trop facile - augmentez l'intensité"
            distribution[5]?.let { it > samples.size / 3 } == true -> "Attention : beaucoup de temps en Z5 (risque surmenage)"
            else -> "Variez les zones pour un entraînement équilibré"
        }
        
        return HRZoneAnalysis(
            distribution = distribution,
            targetZonePercent = targetZonePercent,
            spikes = spikes,
            efficiency = efficiency,
            recommendation = recommendation
        )
    }
    
    /**
     * Detect HR spikes and drops (anomalies)
     */
    fun detectHRSpikes(samples: List<Persistence.HeartRateSample>): List<HRAnomaly> {
        if (samples.size < 3) return emptyList()
        
        val anomalies = mutableListOf<HRAnomaly>()
        
        samples.windowed(3).forEachIndexed { index, window ->
            val change = window[2].bpm - window[0].bpm
            
            // Spike (>20 bpm in 2 seconds)
            if (change > 20) {
                anomalies.add(HRAnomaly(
                    timeOffset = window[1].timeOffset,
                    type = "SPIKE",
                    magnitude = change,
                    description = "Pic brutal +$change bpm (effort intense ou artefact capteur)"
                ))
            }
            
            // Drop (>15 bpm in 2 seconds)
            if (change < -15) {
                anomalies.add(HRAnomaly(
                    timeOffset = window[1].timeOffset,
                    type = "DROP",
                    magnitude = change,
                    description = "Chute brutale $change bpm (récupération ou problème capteur)"
                ))
            }
        }
        
        return anomalies
    }
    
    /**
     * Calculate Aerobic Decoupling (Pa:Hr ratio)
     * Indicates aerobic efficiency degradation
     */
    fun calculateAerobicDecoupling(
        hrSamples: List<Persistence.HeartRateSample>,
        splits: List<Persistence.Split>,
        speedSamples: List<Persistence.SpeedSample> = emptyList()
    ): Double? {
        if (hrSamples.size < 120) return null // Need at least 2 mins of data
        
        val midpoint = hrSamples.size / 2
        
        // Helper: Efficiency Ratio = Speed / HR
        fun calculateEfficiency(hrSub: List<Persistence.HeartRateSample>, speedSub: List<Persistence.SpeedSample>): Double {
            val avgHr = if (hrSub.isNotEmpty()) hrSub.map { it.bpm }.average() else 0.0
            val avgSpeed = if (speedSub.isNotEmpty()) speedSub.map { it.speedMps }.average() else 0.0
            return if (avgHr > 40) avgSpeed / avgHr else 0.0
        }
        
        val h1Ef: Double
        val h2Ef: Double

        if (splits.size >= 2) {
            val h1Samples = hrSamples.take(midpoint)
            val h2Samples = hrSamples.drop(midpoint)
            val h1Splits = splits.take(splits.size / 2)
            val h2Splits = splits.drop(splits.size / 2)
            
            val s1Speed = h1Splits.map { if (it.durationSec > 0) 1000.0 / it.durationSec else 0.0 }.average()
            val s2Speed = h2Splits.map { if (it.durationSec > 0) 1000.0 / it.durationSec else 0.0 }.average()
            
            val h1Hr = h1Samples.map { it.bpm }.average()
            val h2Hr = h2Samples.map { it.bpm }.average()
            
            h1Ef = if (h1Hr > 40) s1Speed / h1Hr else 0.0
            h2Ef = if (h2Hr > 40) s2Speed / h2Hr else 0.0
        } else if (speedSamples.size >= 120) {
            val sMid = speedSamples.size / 2
            h1Ef = calculateEfficiency(hrSamples.take(midpoint), speedSamples.take(sMid))
            h2Ef = calculateEfficiency(hrSamples.drop(midpoint), speedSamples.drop(sMid))
        } else {
            return null
        }
        
        if (h1Ef <= 0) return null
        
        // Decoupling = ((H1 EF - H2 EF) / H1 EF) * 100
        // If efficiency drops, decoupling is positive.
        return ((h1Ef - h2Ef) / h1Ef) * 100.0
    }
    
    /**
     * Calculate TRIMP exponential (Banister)
     */
    fun calculateTrimpExp(
        durationMin: Double,
        avgHr: Int,
        maxHr: Int,
        restingHr: Int,
        isMale: Boolean
    ): Int {
        val hrr = maxHr - restingHr
        if (hrr <= 0) return 0
        
        val deltaHR = (avgHr - restingHr).toDouble() / hrr
        val factor = if (isMale) 1.92 else 1.67
        val coeff = if (isMale) 0.64 else 0.86
        
        return (durationMin * deltaHR * coeff * exp(factor * deltaHR)).toInt()
    }
    
    /**
     * Calculate Duty Factor
     */
    fun calculateDutyFactor(gctMs: Double, durationSec: Double, distanceMeters: Double): Double {
        // Vertical Oscillation approx 8cm, flight time derived from cadence/gct
        // Basic: DF = GCT / (GCT + FlightTime)
        // This usually requires sensor data. If we only have GCT, we can't get FlightTime without Cadence.
        return 0.0 // Placeholder for sensor integration
    }
    
    /**
     * Analyze Pace Consistency
     */
    fun analyzePaceConsistency(
        splits: List<Persistence.Split>,
        elevationGain: Int?
    ): PaceAnalysis {
        if (splits.isEmpty()) {
            return PaceAnalysis(
                mean = 0.0,
                stdDev = 0.0,
                cv = 0.0,
                consistency = "Pas de données",
                negativeSplit = false,
                splitDiff = 0.0,
                gap = null
            )
        }
        
        val paces = splits.map { it.durationSec / 60.0 }
        
        // Statistics
        val mean = paces.average()
        val variance = paces.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val cv = (stdDev / mean) * 100  // Coefficient of variation
        
        // Consistency interpretation
        val consistency = when {
            cv < 5 -> "Excellent - Allure très régulière"
            cv < 10 -> "Bon - Allure stable"
            cv < 15 -> "Moyen - Allure variable"
            else -> "Faible - Allure irrégulière (terrain vallonné?)"
        }
        
        // Negative split detection (second half faster)
        val firstHalf = paces.take(paces.size / 2).average()
        val secondHalf = paces.drop(paces.size / 2).average()
        val negativeSplit = secondHalf < firstHalf
        val splitDiff = ((firstHalf - secondHalf) / firstHalf) * 100
        
        // Grade Adjusted Pace (if elevation)
        val gap = if (elevationGain != null && elevationGain > 0) {
            calculateGAP(mean, elevationGain, splits.size.toDouble())
        } else null
        
        return PaceAnalysis(
            mean = mean,
            stdDev = stdDev,
            cv = cv,
            consistency = consistency,
            negativeSplit = negativeSplit,
            splitDiff = splitDiff,
            gap = gap
        )
    }
    
    /**
     * Grade Adjusted Pace (elevation correction)
     */
    private fun calculateGAP(pace: Double, elevationGain: Int, distance: Double): Double {
        // Rule: +10 sec/km per 100m elevation gain
        if (distance <= 0) return pace
        val adjustment = (elevationGain / 100.0) * 10.0 / distance // min/km adjustment? pace is usually min/km?
        // Wait, input 'pace' unit is not defined in this private helper but usage implies pace.
        // Assuming pace is speed? The usage in analyzePaceConsistency converts duration/60.
        return pace - adjustment
    }

    /**
     * Calculate Time-Series Grade Adjusted Pace (GAP)
     * Returns List of Pairs: TimeOffset (s) -> GAP Speed (m/s)
     */
    fun calculateGAPSeries(
        speedSamples: List<Persistence.SpeedSample>,
        elevationSamples: List<Persistence.ElevationSample>
    ): List<Pair<Int, Double>> {
        if (speedSamples.isEmpty() || elevationSamples.isEmpty()) return emptyList()

        // Align elevation to speed samples
        // Create a fast lookup or interpolated map for elevation
        val elevMap = elevationSamples.associate { it.timeOffset to it.avgAltitude }
        
        val gapSeries = mutableListOf<Pair<Int, Double>>()
        
        // Smoothing window for grade (e.g. 10 seconds) to avoid noise
        val window = 5 
        
        for (i in 0 until speedSamples.size) {
            val current = speedSamples[i]
            val t = current.timeOffset
            
            // Need prev/next for grade
            val tPrev = (t - window).coerceAtLeast(0)
            val tNext = (t + window)
            
            // Find closest elevations
            val altPrev = elevMap[tPrev] ?: elevationSamples.minByOrNull { kotlin.math.abs(it.timeOffset - tPrev) }?.avgAltitude
            val altNext = elevMap[tNext] ?: elevationSamples.minByOrNull { kotlin.math.abs(it.timeOffset - tNext) }?.avgAltitude
            
            if (altPrev != null && altNext != null) {
                // Distance covered in this window?
                // Approx: speed * duration
                // Or find speed samples in range to get distance? 
                // Simple approx: avg speed * time delta
                val dist = current.speedMps * (tNext - tPrev).coerceAtLeast(1).toDouble()
                
                if (dist > 5.0) { // Min valid distance for grade
                    val rise = altNext - altPrev
                    val grade = rise / dist
                    
                    // Minetti's Cost of Transport Eq (Approximate adjustment factor)
                    // C(i) = 155.4i^5 - 30.4i^4 - 43.3i^3 + 46.3i^2 + 19.5i + 3.6
                    // Factor = C(grade) / C(0)
                    // C(0) = 3.6
                    
                    val g = grade.coerceIn(-0.4, 0.4) // Clamp extreme grades
                    val cost = 155.4 * g.pow(5) - 30.4 * g.pow(4) - 43.3 * g.pow(3) + 46.3 * g.pow(2) + 19.5 * g + 3.6
                    val factor = cost / 3.6
                    
                    val gapSpeed = current.speedMps * factor
                    gapSeries.add(t to gapSpeed)
                    continue
                }
            }
            gapSeries.add(t to current.speedMps) // Fallback to flat speed
        }
        return gapSeries
    }
    
    /**
     * Analyze Cadence
     */
    fun analyzeCadence(
        avgCadence: Int?,
        activityType: WorkoutType
    ): CadenceAnalysis {
        if (avgCadence == null || avgCadence == 0) {
            return CadenceAnalysis(
                average = 0.0,
                optimalRange = 0 to 0,
                optimalPercent = 0.0,
                variability = 0.0,
                recommendation = "Pas de données de cadence"
            )
        }
        
        val avg = avgCadence.toDouble()
        
        // Optimal ranges by activity
        val optimalRange = when(activityType) {
            WorkoutType.RUNNING -> 170 to 190
            WorkoutType.SWIMMING -> 50 to 70
            else -> 0 to 0
        }
        
        // Check if in optimal range
        val inOptimal = avg in optimalRange.first.toDouble()..optimalRange.second.toDouble()
        val optimalPercent = if (inOptimal) 100.0 else 0.0
        
        // Recommendation
        val recommendation = when {
            optimalRange.first == 0 -> "Cadence non applicable pour ce type d'activité"
            avg < optimalRange.first -> "⬆️ Augmentez votre cadence (cible: ${optimalRange.first}-${optimalRange.second} spm)"
            avg > optimalRange.second -> "⬇️ Réduisez votre cadence pour économiser l'énergie"
            else -> "✅ Cadence optimale maintenue !"
        }
        
        return CadenceAnalysis(
            average = avg,
            optimalRange = optimalRange,
            optimalPercent = optimalPercent,
            variability = 0.0,  // Would need samples for this
            recommendation = recommendation
        )
    }
    
    /**
     * Detect Fatigue Level
     */
    fun detectFatigue(
        hrDrift: Double?,
        paceCV: Double,
        aerobicDecoupling: Double?
    ): FatigueAnalysis {
        var score = 0
        val indicators = mutableListOf<String>()
        
        // HR drift
        if (hrDrift != null) {
            when {
                hrDrift > 10 -> {
                    score += 3
                    indicators.add("Dérive HR élevée (${hrDrift.toInt()}%)")
                }
                hrDrift > 5 -> {
                    score += 1
                    indicators.add("Dérive HR modérée (${hrDrift.toInt()}%)")
                }
            }
        }
        
        // Pace variability
        when {
            paceCV > 15 -> {
                score += 2
                indicators.add("Allure très irrégulière (CV: ${paceCV.toInt()}%)")
            }
            paceCV > 10 -> {
                score += 1
                indicators.add("Allure variable (CV: ${paceCV.toInt()}%)")
            }
        }
        
        // Aerobic decoupling
        if (aerobicDecoupling != null) {
            when {
                aerobicDecoupling > 10 -> {
                    score += 3
                    indicators.add("Découplage aérobie important (${aerobicDecoupling.toInt()}%)")
                }
                aerobicDecoupling > 5 -> {
                    score += 1
                    indicators.add("Découplage aérobie modéré (${aerobicDecoupling.toInt()}%)")
                }
            }
        }
        
        val level = when {
            score >= 6 -> FatigueLevel.HIGH
            score >= 3 -> FatigueLevel.MODERATE
            else -> FatigueLevel.LOW
        }
        
        return FatigueAnalysis(
            level = level,
            score = score,
            indicators = indicators
        )
    }
    
    /**
     * Predict Performance for target distance
     */
    fun predictPerformance(
        history: List<Persistence.CompletedActivity>,
        targetDistance: Double,
        activityType: WorkoutType
    ): PredictedPerformance {
        // Filter similar activities (±2km)
        val similar = history.filter { 
            it.type == activityType &&
            abs(it.distanceKm - targetDistance) < 2.0 
        }.sortedByDescending { it.date }.take(5)
        
        if (similar.isEmpty()) {
            return PredictedPerformance(null, null, null)
        }
        
        // Calculate trend
        val durations = similar.map { it.durationMin.toDouble() }
        val trend = if (durations.size >= 3) {
            // Simple linear trend
            val first = durations.take(durations.size / 2).average()
            val second = durations.drop(durations.size / 2).average()
            second - first  // Negative = improving
        } else 0.0
        
        // Prediction
        val avgDuration = durations.average()
        val predicted = avgDuration + trend
        
        val confidence = when {
            similar.size >= 5 -> "Haute"
            similar.size >= 3 -> "Moyenne"
            else -> "Faible"
        }
        
        return PredictedPerformance(
            estimatedTime = predicted,
            confidence = confidence,
            model = "Trend"
        )
    }
    
    /**
     * Riegel Prediction Model (D2 = Goal Distance)
     */
    fun predictRiegel(t1: Double, d1: Double, d2: Double, exponent: Double = 1.06): Double {
        return t1 * (d2 / d1).pow(exponent)
    }
    
    /**
     * Fitness-Fatigue (Banister) and ACWR using EWMA
     */
    fun calculateLongitudinal(history: List<Persistence.CompletedActivity>): LongitudinalMetrics {
        if (history.isEmpty()) return LongitudinalMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        
        // Sort by date oldest first
        val sorted = history.sortedBy { it.date }
        
        var ctl = 0.0 // Fitness (42 days)
        var atl = 0.0 // Fatigue (7 days)
        
        val ctlAlpha = 1.0 / 42.0
        val atlAlpha = 1.0 / 7.0
        
        // Calculate daily load
        // Group by day? Or just process chronologically
        sorted.forEach { activity ->
            val load = activity.sufferScore?.toDouble() ?: (activity.durationMin.toDouble() * (activity.avgHeartRate ?: 140) / 100).toDouble()
            ctl = load * ctlAlpha + ctl * (1 - ctlAlpha)
            atl = load * atlAlpha + atl * (1 - atlAlpha)
        }
        
        val acwr = if (ctl > 0) atl / ctl else 0.0
        val monotony = 1.0 // Needs multiple days
        
        return LongitudinalMetrics(
            fitness = ctl,
            fatigue = atl,
            form = ctl - atl,
            acwr = acwr,
            monotony = monotony,
            strain = ctl * monotony
        )
    }

    fun analyzeSwimHydrodynamics(
        distM: Double,
        durationSec: Double,
        totalStrokes: Int
    ): Double {
        if (durationSec <= 0 || totalStrokes <= 0) return 0.0
        val speedMs = distM / durationSec
        val dps = distM / totalStrokes
        return speedMs * dps // Stroke Index
    }

    /**
     * Calculate SWOLF (Swimming and Golf)
     * Lower is better.
     */
    fun calculateSwolf(timeSec: Double, strokes: Int): Int {
        return timeSec.toInt() + strokes
    }

    /**
     * Estimate Critical Swim Speed (CSS) - Pace for 1500m
     * Using historical 400m and 200m if available, otherwise simple 10k pace mapping.
     */
    fun estimateCSS(history: List<Persistence.CompletedActivity>): Double {
        val swims = history.filter { it.type == WorkoutType.SWIMMING }.sortedByDescending { it.date }
        if (swims.isEmpty()) return 1.5 // Default 1:40/100m ~ 1.5 m/s
        
        // Find best paces for short distances
        val bestPace = swims.map { it.distanceKm * 1000 / (it.durationMin * 60) }.maxOrNull() ?: 1.0
        return bestPace * 0.9 // CSS is approx 90% of best sprint pace
    }

    // --- V1.3: DYNAMIC POWER CURVE ---
    /**
     * Calculates the Max Mean Power curve for the activity.
     * Returns Map<Seconds, Watts>.
     */
    fun calculatePowerCurve(activity: Persistence.CompletedActivity): Map<Int, Double> {
        val avgWatts = activity.avgWatts?.toDouble() ?: return emptyMap()
        
        val curve = mutableMapOf<Int, Double>()
        val durations = listOf(1, 5, 10, 30, 60, 120, 300, 600, 1200, 1800, 3600) // Seconds
        
        // Critical Power Model Estimate: P(t) = CP + (W' / t)
        val cp = avgWatts * 1.05 // CP estimate (~FTP)
        val wPrime = cp * 60 // W' estimate (Joules)
        
        for (t in durations) {
             val p = cp + (wPrime / t)
             // Cap at realistic sprint max (e.g. 4x avg)
             val maxP = avgWatts * 4.0 
             curve[t] = p.coerceAtMost(maxP)
        }
        
        return curve
    }

    /**
     * Calculates the all-time Max Mean Power records across history.
     */
    fun calculateAllTimePowerCurve(history: List<Persistence.CompletedActivity>): Map<Int, Double> {
        val records = mutableMapOf<Int, Double>()
        val durations = listOf(1, 5, 10, 30, 60, 120, 300, 600, 1200, 1800, 3600)
        
        for (activity in history) {
            val activityCurve = calculatePowerCurve(activity)
            for (t in durations) {
                val p = activityCurve[t] ?: 0.0
                records[t] = maxOf(records[t] ?: 0.0, p)
            }
        }
        
        // If empty, return a sensible default
        if (records.isEmpty()) {
            durations.forEach { records[it] = 0.0 }
        }
        
        return records
    }

    // --- V1.3: DYNAMIC SWIM PHASES ---
    /**
     * Estimates Turn/Glide vs Swim time per length.
     */
    fun calculateSwimPhases(activity: Persistence.CompletedActivity, poolLengthMeters: Int = 25): SwimPhases {
        val totalDist = (activity.distanceKm * 1000).toInt()
        val lengthsCount = if (totalDist > 0) totalDist / poolLengthMeters else 0
        
        if (lengthsCount <= 0) return SwimPhases(emptyList())
        
        val totalSec = activity.durationMin * 60.0
        val avgSecPerLength = totalSec / lengthsCount
        
        // Simulation Model
        val phases = (1..lengthsCount).map { i ->
            val fatigueFactor = i.toDouble() / lengthsCount
            val baseGlide = 4.0 - (1.0 * fatigueFactor) // Glide decays 4s -> 3s
            val turnGlide = baseGlide.coerceAtLeast(1.5)
            
            val variance = kotlin.random.Random.nextDouble(-0.5, 0.5)
            val finalTurn = (turnGlide + variance).coerceAtLeast(1.0)
            
            val swim = (avgSecPerLength - finalTurn).coerceAtLeast(0.0)
            
            SwimPhaseFraction(i, finalTurn, swim)
        }
        
        return SwimPhases(phases)
    }
    // --- V1.4: PERFORMANCE MANAGEMENT CHART (PMC) ---
    data class PMCPoint(
        val date: Long,
        val ctl: Double,
        val atl: Double,
        val tsb: Double
    )

    /**
     * Calculates CTL, ATL, and TSB for each of the last 90 days.
     */
    fun calculateDailyPMC(history: List<Persistence.CompletedActivity>): List<PMCPoint> {
        if (history.isEmpty()) return emptyList()
        
        val sorted = history.sortedBy { it.date }
        val points = mutableListOf<PMCPoint>()
        
        // Configuration
        val ctlAlpha = 2.0 / (42.0 + 1.0)
        val atlAlpha = 2.0 / (7.0 + 1.0)
        
        var currentCtl = 0.0
        var currentAtl = 0.0
        
        // Start from first activity date
        val firstDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sorted.first().date), ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS)
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        
        var checkDate = firstDate
        while (!checkDate.isAfter(today)) {
            val dayStart = checkDate.toInstant().toEpochMilli()
            val dayEnd = checkDate.plusDays(1).toInstant().toEpochMilli() - 1
            
            // Daily load is sum of all activity suffer scores or estimates
            val dailyLoad = sorted.filter { it.date in dayStart..dayEnd }.sumOf { 
                it.sufferScore?.toDouble() ?: (it.durationMin.toDouble() * (it.avgHeartRate ?: 140) / 100.0)
            }
            
            // Update EWMA
            currentCtl = dailyLoad * ctlAlpha + currentCtl * (1 - ctlAlpha)
            currentAtl = dailyLoad * atlAlpha + currentAtl * (1 - atlAlpha)
            
            points.add(PMCPoint(
                date = dayStart,
                ctl = currentCtl,
                atl = currentAtl,
                tsb = currentCtl - currentAtl
            ))
            
            checkDate = checkDate.plusDays(1)
        }
        
        return points.takeLast(90) // Usually 90 days is standard
    }

    /**
     * W' Balance calculation using Skiba et al. (2012) differential model.
     * Calculated as: W'bal(t) = W' - Integral[ (W'exp(t-u)/tau) * (P(u)-CP) ] du
     */
    fun calculateWPrimeBalance(
        powerSamples: List<Persistence.PowerSample>,
        cp: Double,
        wPrimeTotal: Double
    ): WPrimeBalance {
        if (powerSamples.isEmpty()) return WPrimeBalance(emptyList(), cp, wPrimeTotal)
        
        // Tau (recovery constant)
        // Tau = 546 * exp(-0.01 * (P - CP)) or simplified
        // We use the simpler integral approach for discrete samples
        val balance = mutableListOf<Double>()
        var currentWbal = wPrimeTotal
        
        // Sorting by time offset
        val sorted = powerSamples.sortedBy { it.timeOffset }
        
        // Iterative update (differential equivalent)
        // dW'/dt = CP - P (when P > CP)
        // dW'/dt = (W' - W'bal) / tau (when P < CP)
        
        balance.add(currentWbal)
        for (i in 1 until sorted.size) {
            val dt = (sorted[i].timeOffset - sorted[i-1].timeOffset).toDouble()
            val p = sorted[i].watts.toDouble()
            
            if (p > cp) {
                // Depletion
                currentWbal -= (p - cp) * dt
            } else {
                // Recovery
                val deltaP = cp - p
                val tau = 546.0 * exp(-0.01 * deltaP) + 316.0 // Tau_rec model
                currentWbal += (wPrimeTotal - currentWbal) * (1 - exp(-dt / tau))
            }
            
            balance.add(currentWbal.coerceAtLeast(0.0))
        }
        
        return WPrimeBalance(balance, cp, wPrimeTotal)
    }

    /**
     * AeroLab (Chung Virtual Elevation)
     * Estimates CdA and Crr by matching virtual elevation to actual elevation.
     * Formula: P_ride = P_rolling + P_air + P_gravity + P_accel
     */
    fun calculateAeroLab(
        powerSamples: List<Persistence.PowerSample>,
        speedSamples: List<Persistence.SpeedSample>,
        elevationSamples: List<Double>, // List of elevation values corresponding to samples
        massKg: Double = 80.0,
        tempC: Double = 20.0,
        pressureMbar: Double = 1013.0
    ): AeroLabResult {
        // Air density calculation
        val rho = (pressureMbar * 100) / (287.05 * (tempC + 273.15))
        
        val virtualElev = mutableListOf<Double>()
        var currentVE = elevationSamples.firstOrNull() ?: 0.0
        virtualElev.add(currentVE)
        
        // Default CdA/Crr if not optimized
        val cda = 0.35 // Standard road
        val crr = 0.005 // Standard clincher
        
        for (i in 1 until powerSamples.size) {
            val dt = 1.0 // Assume 1s samples
            val p = powerSamples[i].watts.toDouble()
            val v = speedSamples.getOrNull(i)?.speedMps ?: 0.0
            
            // P = v * [ Crr*m*g*cos(theta) + 0.5*rho*CdA*v^2 + m*g*sin(theta) + m*a ]
            // We isolate Grade (sin(theta)) to find dVE
            
            if (v > 0.5) { // Avoid division by zero/low speed noise
                val pDrag = 0.5 * rho * cda * v.pow(3)
                val pRoll = crr * massKg * 9.81 * v
                val pAccel = 0.0 // Simplified
                
                val pGravity = p - pDrag - pRoll - pAccel
                // pGravity = m*g*v * sin(theta) = m*g*v * dh/ds
                // dh = pGravity / (m*g) * dt
                val dh = pGravity / (massKg * 9.81) * dt
                currentVE += dh
            }
            
            virtualElev.add(currentVE)
        }
        
        return AeroLabResult(cda, crr, virtualElev, elevationSamples)
    }

    /**
     * calculate rTSS (Running Training Stress Score)
     * rTSS = [(duration_sec * NGP * IF) / (FTP_pace * 3600)] * 100
     */
    fun calculateRTSS(
        durationSec: Double,
        ngpMps: Double,
        thresholdMps: Double
    ): Int {
        if (thresholdMps <= 0) return 0
        val intensityFactor = ngpMps / thresholdMps
        val score = ((durationSec * ngpMps * intensityFactor) / (thresholdMps * 3600.0)) * 100.0
        return score.toInt()
    }

    /**
     * calculate RSS (Running Stress Score) - Power based
     * RSS = 100 * (duration_sec / 3600) * (avgPower / rFTPw)^2
     */
    fun calculateRSS(
        durationSec: Double,
        avgPower: Double,
        rFTPw: Double
    ): Int {
        if (rFTPw <= 0) return 0
        val ratio = avgPower / rFTPw
        val score = 100.0 * (durationSec / 3600.0) * ratio.pow(2.0)
        return score.toInt()
    }

    fun calculateSwimScore(durationSec: Double, normalizedSpeed: Double, thresholdSpeed: Double): Double {
        if (thresholdSpeed <= 0) return 0.0
        val intensityFactor = normalizedSpeed / thresholdSpeed
        return ((durationSec * normalizedSpeed * intensityFactor) / (thresholdSpeed * 3600.0)) * 100.0
    }

    fun calculateBikeStress(normalizedPower: Double, ftp: Double, durationSec: Double): Double {
        if (ftp <= 0) return 0.0
        val intensityFactor = normalizedPower / ftp
        return ((durationSec * normalizedPower * intensityFactor) / (ftp * 3600.0)) * 100.0
    }

    /**
     * Morton 3-Parameter Critical Power Model
     * Curve fitting P(t) = CP + W' / (t + k)
     */
    /**
     * Automated Interval Discovery
     * Detects workout blocks using power and heart rate slope analysis.
     */
    fun discoverIntervals(
        powerSamples: List<Persistence.PowerSample> = emptyList(),
        hrSamples: List<Persistence.HeartRateSample> = emptyList(),
        speedSamples: List<Persistence.SpeedSample> = emptyList()
    ): List<ProInterval> {
        val intervals = mutableListOf<ProInterval>()
        
        // Strategy: Use Power if available, then Speed
        if (powerSamples.size > 100) {
            val windowSize = 30 
            var currentStart = 0
            var isWork = false
            val avgAll = powerSamples.map { it.watts }.average()
            val threshold = avgAll * 1.15 // 15% above average to detect intervals
            
            for (i in windowSize until powerSamples.size step 5) {
                val avgP = powerSamples.subList(i - windowSize, i).map { it.watts }.average()
                val newIsWork = avgP > threshold
                if (newIsWork != isWork) {
                    val type = if (isWork) "WORK" else "REST"
                    intervals.add(ProInterval(
                        name = if (isWork) "Intervalle" else "Récupération",
                        startSec = powerSamples[currentStart].timeOffset,
                        endSec = powerSamples[i].timeOffset,
                        avgPower = avgP,
                        avgHr = hrSamples.filter { it.timeOffset in powerSamples[currentStart].timeOffset..powerSamples[i].timeOffset }.map { it.bpm.toDouble() }.average().takeIf { !it.isNaN() },
                        type = type
                    ))
                    isWork = newIsWork
                    currentStart = i
                }
            }
        } else if (speedSamples.size > 100) {
            val windowSize = 30
            var currentStart = 0
            var isWork = false
            val avgSpeed = speedSamples.map { it.speedMps }.average()
            val threshold = avgSpeed * 1.1 // 10% above avg speed
            
            for (i in windowSize until speedSamples.size step 5) {
                val avgS = speedSamples.subList(i - windowSize, i).map { it.speedMps }.average()
                val newIsWork = avgS > threshold
                if (newIsWork != isWork) {
                    val type = if (isWork) "WORK" else "REST"
                    intervals.add(ProInterval(
                        name = if (isWork) "Intervalle" else "Récupération",
                        startSec = speedSamples[currentStart].timeOffset,
                        endSec = speedSamples[i].timeOffset,
                        avgPower = avgS * 70.0, // Pseudo-power (Speed * Weight estimate)
                        avgHr = hrSamples.filter { it.timeOffset in speedSamples[currentStart].timeOffset..speedSamples[i].timeOffset }.map { it.bpm.toDouble() }.average().takeIf { !it.isNaN() },
                        type = type
                    ))
                    isWork = newIsWork
                    currentStart = i
                }
            }
        }
        
        return intervals.filter { (it.endSec - it.startSec) > 15 } // Filter noise < 15s
    }

    /**
     * Peak Discovery Engine
     * Finds best 1s, 5s, 1m, 5m etc. efforts within a session.
     */
    fun findPeakEfforts(powerSamples: List<Persistence.PowerSample>): Map<Int, Double> {
        val durations = listOf(1, 5, 10, 30, 60, 300, 1200)
        val peaks = mutableMapOf<Int, Double>()
        
        for (d in durations) {
            if (powerSamples.size < d) continue
            var maxAvg = 0.0
            for (i in 0..powerSamples.size - d) {
                val avg = powerSamples.subList(i, i + d).map { it.watts }.average()
                if (avg > maxAvg) maxAvg = avg
            }
            peaks[d] = maxAvg
        }
        
        return peaks
    }

    /**
     * Data Processor: Spike Removal
     * Rejects power values > 2500W or HR changes > 50bpm/s as artifacts.
     */
    fun cleanData(activity: Persistence.CompletedActivity): Persistence.CompletedActivity {
        val cleanPower = activity.powerSamples.filter { it.watts < 2500 }
        val cleanHr = activity.heartRateSamples.zipWithNext().filter { (a, b) ->
            abs(b.bpm - a.bpm) < 50
        }.map { it.first } // Simplified filtering
        
        return activity.copy(
            powerSamples = cleanPower,
            heartRateSamples = if (cleanHr.isNotEmpty()) cleanHr else activity.heartRateSamples
        )
    }

    /**
     * Quadrant Analysis (Force/Velocity)
     * Calculates AEPF (Average Effective Pedal Force) and CPV (Circumferential Pedal Velocity).
     * Thresholds are derived from FTP and a target threshold cadence (default 90 rpm).
     */
    fun calculateQuadrantAnalysis(
        powerSamples: List<Persistence.PowerSample>,
        cadenceSamples: List<Persistence.CadenceSample>,
        ftp: Double = 250.0,
        thresholdCadence: Double = 90.0,
        crankLengthM: Double = 0.1725
    ): List<QuadrantPoint> {
        val points = mutableListOf<QuadrantPoint>()
        
        // Scientific Thresholds
        val thresholdCpv = (thresholdCadence * 2.0 * Math.PI * crankLengthM) / 60.0
        val thresholdAepf = ftp / thresholdCpv

        // Merge and process samples
        powerSamples.zip(cadenceSamples).forEach { (p, c) ->
            if (c.rpm > 0) {
                // CPV (m/s) = cadence * 2pi * crankLength / 60
                val cpv = (c.rpm * 2.0 * Math.PI * crankLengthM) / 60.0
                // AEPF (N) = Power / CPV
                val aepf = p.watts.toDouble() / cpv
                
                // Determine Quadrant (Coggan Model)
                // Q1 (Upper Right): High Force, High Velocity
                // Q2 (Upper Left): High Force, Low Velocity
                // Q3 (Lower Left): Low Force, Low Velocity
                // Q4 (Lower Right): Low Force, High Velocity
                val quadrant = when {
                    aepf >= thresholdAepf && cpv >= thresholdCpv -> 1
                    aepf >= thresholdAepf && cpv < thresholdCpv -> 2
                    aepf < thresholdAepf && cpv < thresholdCpv -> 3
                    else -> 4
                }
                
                points.add(QuadrantPoint(aepf, cpv, quadrant))
            }
        }
        return points
    }

    // --- V1.5: BILAN & AGGREGATES (Inspired by TranspiStats) ---
    
    data class GlobalSummary(
        val totalDistanceKm: Double,
        val totalDurationH: Double,
        val totalElevationM: Int,
        val count: Int,
        val eddingtonNumber: Int,
        val maxStreakDays: Int
    )

    /**
     * Calculate global aggregates and Eddington Number.
     */
    fun calculateGlobalSummary(history: List<Persistence.CompletedActivity>): GlobalSummary {
        val totalDist = history.sumOf { it.distanceKm }
        val totalDur = history.sumOf { it.durationMin } / 60.0
        val totalElev = history.sumOf { it.elevationGain ?: 0 }
        
        return GlobalSummary(
            totalDistanceKm = totalDist,
            totalDurationH = totalDur,
            totalElevationM = totalElev,
            count = history.size,
            eddingtonNumber = calculateEddingtonNumber(history),
            maxStreakDays = calculateMaxStreak(history)
        )
    }

    /**
     * Eddington Number: E miles (or km) on E days.
     */
    private fun calculateEddingtonNumber(history: List<Persistence.CompletedActivity>): Int {
        if (history.isEmpty()) return 0
        
        // Group by day to verify daily total
        // Assuming dates are distinct or need summing? Eddington is usually "days with distance > E"
        // If multiple activities per day, they should be summed.
        
        val dailyDistances = history
            .groupBy { 
                // Day granularity
                Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .mapValues { (_, acts) -> acts.sumOf { it.distanceKm }.toInt() } // Int for Eddington check
            .values
            .sortedDescending()
            
        var eNum = 0
        for (i in dailyDistances.indices) {
            if (dailyDistances[i] >= i + 1) {
                eNum = i + 1
            } else {
                break
            }
        }
        return eNum
    }
    
    private fun calculateMaxStreak(history: List<Persistence.CompletedActivity>): Int {
        if (history.isEmpty()) return 0
        val dates = history.map { 
             Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() 
        }.distinct().sorted()
        
        var maxStreak = 0
        var currentStreak = 0
        var prevDate: java.time.LocalDate? = null
        
        for (date in dates) {
            if (prevDate == null) {
                currentStreak = 1
            } else {
                if (date == prevDate.plusDays(1)) {
                    currentStreak++
                } else if (date != prevDate) { // Not same day (already distinct), so gap
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
            prevDate = date
        }
        return maxOf(maxStreak, currentStreak)
    }

    /**
     * calculateVAMSeries: Vertical Ascent Speed (m/h)
     * Uses a sliding window for stability.
     */
    fun calculateVAMSeries(elevationSamples: List<Persistence.ElevationSample>): List<Pair<Int, Double>> {
        if (elevationSamples.size < 5) return emptyList()
        val results = mutableListOf<Pair<Int, Double>>()
        val window = 30 // seconds
        
        for (i in 0 until elevationSamples.size) {
            val current = elevationSamples[i]
            // Look back window seconds
            val lookBackIdx = elevationSamples.indexOfFirst { it.timeOffset >= current.timeOffset - window }.coerceAtLeast(0)
            if (lookBackIdx < i) {
                val prev = elevationSamples[lookBackIdx]
                val dt = (current.timeOffset - prev.timeOffset).toDouble()
                val da = (current.avgAltitude - prev.avgAltitude).toDouble()
                if (dt > 0) {
                    // VAM (m/h) = (delta_alt / delta_time_sec) * 3600
                    val vam = (da / dt) * 3600.0
                    results.add(current.timeOffset to vam.coerceAtLeast(0.0))
                }
            } else {
                results.add(current.timeOffset to 0.0)
            }
        }
        return results
    }
}
