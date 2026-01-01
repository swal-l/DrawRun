package com.orbital.run.logic

import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.logic.LongitudinalMetrics
import com.orbital.run.logic.Persistence
import com.orbital.run.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class AdviceType {
    RUN_TECHNIQUE,
    PPG_CIRCUIT,
    SWIM_GLIDE,
    SWIM_PPG,
    SWIM_CATCH,
    RECOVERY_STRETCH
}

data class Insight(
    val title: String, 
    val content: String, 
    val icon: ImageVector,
    val isPositive: Boolean = true,
    val adviceType: AdviceType? = null
)

data class ScientificMetrics(
    val efficiencyFactor: Double? = null,
    val runningEconomyText: String? = null,
    val runningEffectiveness: Double? = null,
    val contextTemp: Double? = null,
    val contextAlt: Int? = null,
    
    val distancePerStroke: Double? = null,
    val swimEfficiencyText: String? = null,
    val computedSwolf: Int? = null,
    val strokeIndex: Double? = null,
    
    val zoneDistribution: List<Float> = emptyList(),
    
    val trimp: Int? = null,
    val acwr: Double? = null,
    val rTss: Int? = null,
    val rss: Int? = null,
    val rFtpW: Double? = null,
    val enduranceIndex: Double? = null
)

object AnalysisEngine {
    
    fun calculateScience(activity: Persistence.CompletedActivity, trainingPlan: TrainingPlanResult? = null): ScientificMetrics {
        val metrics = if (activity.type == WorkoutType.RUNNING) {
            calculateRunScience(activity, trainingPlan)
        } else if (activity.type == WorkoutType.SWIMMING) {
            calculateSwimScience(activity)
        } else {
            ScientificMetrics()
        }
        
        // Post-process Zones if not already done
        return metrics.copy(zoneDistribution = calculateZones(activity))
    }
    
    private fun calculateZones(a: Persistence.CompletedActivity): List<Float> {
        // If we have detailed splits with HR, use them.
        // Otherwise, rely on average or return empty.
        val hrMax = a.maxHeartRate ?: 190
        val z1Limit = (hrMax * 0.60).toInt()
        val z2Limit = (hrMax * 0.70).toInt()
        val z3Limit = (hrMax * 0.80).toInt()
        val z4Limit = (hrMax * 0.90).toInt()
        
        if (a.splits.isNotEmpty()) {
            val counts = IntArray(5) { 0 }
            var total = 0
            a.splits.forEach { split ->
                val hr = split.avgHr
                if (hr != null) {
                    when {
                        hr < z1Limit -> counts[0]++ // Z1
                        hr < z2Limit -> counts[1]++ // Z2
                        hr < z3Limit -> counts[2]++ // Z3
                        hr < z4Limit -> counts[3]++ // Z4
                        else -> counts[4]++ // Z5
                    }
                    total++
                }
            }
            if (total > 0) {
                return counts.map { it.toFloat() / total }
            }
        }
        
        // Fallback: Gaussian roughly around Avg HR? 
        // Or just flat if unknown.
        // Let's return empty to indicate "No Detailed Data"
        return emptyList()
    }
    
    private fun calculateRunScience(a: Persistence.CompletedActivity, tp: TrainingPlanResult? = null): ScientificMetrics {
        // --- 1. PRE-PROCESS DATA Fallbacks ---
        // Calculate averages from samples if summary fields are missing
        val effectiveAvgHr = a.avgHeartRate ?: if(a.heartRateSamples.isNotEmpty()) a.heartRateSamples.map { it.bpm }.average().toInt() else null
        val effectiveAvgWatts = a.avgWatts ?: if(a.powerSamples.isNotEmpty()) a.powerSamples.map { it.watts }.average().toInt() else null
        val temp = a.avgTemp
        
        // Temp Normalization: If temp > 15C, HR increases by ~1bpm per 1C.
        val standardHr = if (effectiveAvgHr != null && temp != null && temp > 15) {
             effectiveAvgHr - (temp - 15).toInt() // Remove drift caused by heat
        } else effectiveAvgHr
 
        // --- 2. TRIMP (Training Impulse) ---
        val trimp = if (a.sufferScore != null) a.sufferScore else {
             if (effectiveAvgHr != null && a.durationMin > 0) {
                 val hrMax = a.maxHeartRate ?: 190
                 val ratio = effectiveAvgHr.toDouble() / hrMax.toDouble()
                 (a.durationMin * ratio * Math.exp(1.92 * ratio)).toInt()
             } else null
        }
        
        // --- 3. EFFICIENCY FACTOR (EF) ---
        val durationMins = a.durationMin.toDouble()
        val distMeters = a.distanceKm * 1000.0
        val speedMmin = if (durationMins > 0) distMeters / durationMins else 0.0
        
        val efhr = standardHr ?: effectiveAvgHr
        val ef = if (efhr != null && efhr > 0) speedMmin / efhr.toDouble() else null
        
        // --- 4. RUNNING EFFECTIVENESS (RE) ---
        var re: Double? = null
        if (effectiveAvgWatts != null && effectiveAvgWatts > 0) {
            val speedMs = speedMmin / 60.0
            val wKg = effectiveAvgWatts.toDouble() / 70.0 
            re = speedMs / wKg // kg/N roughly
        }
 
        // --- 5. rTSS Calculation ---
        val speedMs = if (durationMins > 0) distMeters / (durationMins * 60.0) else 0.0
        val thresholdMps = speedMs * 0.95 // Guess threshold
        val rTss = AdvancedAnalytics.calculateRTSS(durationMins * 60.0, speedMs, thresholdMps)
 
        // --- 6. RSS Calculation (Power) ---
        var rss: Int? = null
        var estimatedRftpW: Double? = null
        if (effectiveAvgWatts != null && effectiveAvgWatts > 0) {
            // Estimate rFTPw: 1.04 * weight * VMA (in m/s)
            val weight = tp?.userProfile?.weightKg ?: 70.0
            val vmaMps = (tp?.vma ?: 12.0) / 3.6
            estimatedRftpW = a.criticalPower ?: (1.04 * weight * vmaMps)
            
            rss = AdvancedAnalytics.calculateRSS(
                durationSec = durationMins * 60.0,
                avgPower = effectiveAvgWatts.toDouble(),
                rFTPw = estimatedRftpW
            )
        }
 
        return ScientificMetrics(
            trimp = trimp,
            efficiencyFactor = ef,
            runningEconomyText = "Efficacité: ${String.format("%.2f", ef ?: 0.0)} w/bpm",
            runningEffectiveness = re,
            rTss = rTss,
            rss = rss,
            rFtpW = estimatedRftpW,
            enduranceIndex = calculateEnduranceIndex(a, tp)
        )
    }

    private fun calculateEnduranceIndex(a: Persistence.CompletedActivity, tp: TrainingPlanResult?): Double? {
        // Formula: IE = (100 - %VMA) / ln(7) - ln(t) ? 
        // Image says: IE = (100 - %VMA) / ln(7/t) ?? No, image says ln(7/t) OR ln(t/7) ?
        // Image: IE = (100 - %VMA) / ln(7/t)
        // Wait, if t > 7, ln(7/t) is negative. 
        // 100 - %VMA is usually positive (running slower than VMA).
        // So IE would be negative. 
        // Peronnet & Thibault usually define IE as negative (slope). 
        // Let's check formula carefully.
        // Image text: "Défini par Péronnet et Thibault... pente de décroissance... (négatif)"
        // "Plus cet indice est proche de zéro et plus l'athlète est endurant."
        // Formula in image: IE = (100 - %VMA) / ln ( 7 ) / t  <-- NO
        // Formula in image: IE = (100 - %VMA) / ln ( 7 / t )  OR  ln ( t / 7 ) ?
        // Let's re-read the image text in my mind or zoom in.
        // Image: IE = (100 - %VMA) / ln( 7 / t ) 
        // Let's test:
        // Run 60 min at 12kmh. VMA 15. %VMA = 80%.
        // 100 - 80 = 20.
        // ln(7/60) = ln(0.116) = -2.15.
        // IE = 20 / -2.15 = -9.3.
        // Run 60 min at 14kmh. %VMA = 93%.
        // 100 - 93 = 7.
        // IE = 7 / -2.15 = -3.25 (Closer to zero -> better endurance).
        // This makes sense. IE is typically between -10 (poor) and -3 (elite).
        
        if (a.durationMin <= 0 || a.distanceKm <= 0) return null
        val vma = tp?.vma ?: return null // Need VMA
        
        val speedKmh = a.distanceKm / (a.durationMin / 60.0)
        val pctVma = (speedKmh / vma) * 100
        val t = a.durationMin
        
        // Avoid division by zero if t approx 7 or log issues
        // Actually if t = 7, ln(1) = 0 -> Infinity.
        if (pctVma > 100) return null // Sprinted faster than VMA?? Not useful for endurance index.
        
        // If duration is very short < 7 min, ln(7/t) > 0. Then IE > 0.
        // The formula usually applies for t > 7 min (since 7 min is time limit at 100% VMA).
        if (t < 7) return null 

        val denominator = kotlin.math.ln(7.0 / t)
        if (denominator == 0.0) return null
        
        return (100.0 - pctVma) / denominator
    }
    
    private fun calculateSwimScience(a: Persistence.CompletedActivity): ScientificMetrics {
        // 1. DPS
        var dps: Double? = null
        var siResult: Double? = null
        var swolf: Int? = a.swolf
        
        if (a.distanceKm > 0) {
            val distM = a.distanceKm * 1000
            
            // Calc DPS
            if (a.totalStrokes != null && a.totalStrokes > 0) {
                dps = distM / a.totalStrokes
            }
            
            // 2. Stroke Index = Speed (m/s) * DPS
            val durationSec = a.durationMin * 60
            if (durationSec > 0) {
                val speedMs = distM / durationSec
                if (dps != null) {
                    siResult = speedMs * dps
                }
                
                // 3. Fallback SWOLF
                if (swolf == null && a.totalStrokes != null) {
                    val poolLen = 25.0
                    val lengths = distM / poolLen
                    if (lengths >= 1) {
                         val timePerLen = durationSec / lengths
                         val strokesPerLen = a.totalStrokes / lengths
                         swolf = (timePerLen + strokesPerLen).toInt()
                    }
                }
            }
        }
        
        // Override with V1.3 Logic if possible
        val finalSi = if (a.distanceKm > 0 && a.durationMin > 0 && a.totalStrokes != null) {
            AdvancedAnalytics.analyzeSwimHydrodynamics(
                a.distanceKm * 1000.0,
                a.durationMin * 60.0,
                a.totalStrokes
            )
        } else siResult
        
        return ScientificMetrics(
            distancePerStroke = dps,
            strokeIndex = finalSi,
            computedSwolf = swolf,
            swimEfficiencyText = when {
                swolf == null -> "Technique natation..."
                swolf < 35 -> "Glisse Exceptionnelle (SI: ${String.format("%.2f", finalSi ?: 0.0)})"
                swolf < 45 -> "Bonne technique de bras."
                else -> "Cherchez plus d'allonge pour réduire le SWOLF."
            },
            rTss = (finalSi?.times(10.0))?.toInt() ?: (a.durationMin * 60 / 60) // Dummy Swim TSS using SI or Duration
        )
    }
    


    fun analyze(activity: Persistence.CompletedActivity, trainingPlan: TrainingPlanResult? = null): List<Insight> {
        val insights = mutableListOf<Insight>()
        
        // Calculate science metrics FIRST to use in insights
        val science = calculateScience(activity, trainingPlan)
        
        // 1. V1.3 COACHING IA (Running)
        if (activity.type == WorkoutType.RUNNING) {
            // Use actual EF (Efficiency Factor) for insights
            science.efficiencyFactor?.let { ef ->
                when {
                    ef < 3.0 -> insights.add(Insight(
                        "Efficacité Faible",
                        "Facteur d'efficacité: ${"%.2f".format(ef)}. Travaillez l'endurance fondamentale pour améliorer votre économie de course.",
                        Icons.Default.Warning,
                        isPositive = false,
                        adviceType = AdviceType.RUN_TECHNIQUE
                    ))
                    ef > 5.0 -> insights.add(Insight(
                        "Excellente Économie",
                        "EF: ${"%.2f".format(ef)}. Votre efficacité aérobie est optimale !",
                        Icons.Default.Speed,
                        isPositive = true,
                        adviceType = AdviceType.RUN_TECHNIQUE
                    ))
                    else -> {} // Normal range, no insight needed
                }
            }
            
            // Use actual RE (Running Effectiveness) for insights
            science.runningEffectiveness?.let { re ->
                when {
                    re < 0.8 -> insights.add(Insight(
                        "Efficacité Biomécanique Faible",
                        "RE: ${"%.2f".format(re)}. Travaillez votre technique de course pour mieux convertir la puissance en vitesse.",
                        Icons.Default.Warning,
                        isPositive = false,
                        adviceType = AdviceType.RUN_TECHNIQUE
                    ))
                    re > 1.2 -> insights.add(Insight(
                        "Excellent Rendement Biomécanique",
                        "RE: ${"%.2f".format(re)}. Vous convertissez efficacement votre puissance en vitesse.",
                        Icons.Default.Speed,
                        isPositive = true
                    ))
                    else -> {} // Normal range, no insight needed
                }
            }
            
            // Use actual rTSS for load insights
            science.rTss?.let { rtss ->
                when {
                    rtss > 200 -> insights.add(Insight(
                        "Charge Très Élevée",
                        "rTSS: $rtss. Séance très intense, prévoir 48-72h de récupération.",
                        Icons.Default.Warning,
                        isPositive = false,
                        adviceType = AdviceType.RECOVERY_STRETCH
                    ))
                    rtss > 100 -> insights.add(Insight(
                        "Charge Modérée à Élevée",
                        "rTSS: $rtss. Bon stimulus d'entraînement.",
                        Icons.Default.Timer,
                        isPositive = true
                    ))
                    else -> {} // Low load, no insight needed
                }
            }
            
            activity.avgGctMs?.let { gct ->
                if (gct > 240) {
                     insights.add(Insight(
                         "Temps de Contact Élevé",
                         "Tu restes trop longtemps au sol ($gct ms). Travaille ta réactivité pour être plus 'élastique'.",
                         Icons.Default.Warning,
                         isPositive = false,
                         adviceType = AdviceType.RUN_TECHNIQUE
                     ))
                }
            }
            
            activity.verticalRatio?.let { vr ->
                if (vr > 8.0) {
                     insights.add(Insight(
                         "Ratio Vertical Élevé",
                         "Tu rebondis trop ! Augmente ta cadence pour économiser ton énergie.",
                         Icons.Default.Warning,
                         isPositive = false,
                         adviceType = AdviceType.RUN_TECHNIQUE
                     ))
                }
            }
        }
        
        // 2. V1.3 COACHING IA (Swimming)
        if (activity.type == WorkoutType.SWIMMING) {
             val swolfToUse = activity.swolf ?: science.computedSwolf
             swolfToUse?.let { s ->
                 if (s > 45) {
                     insights.add(Insight(
                         "Technique Dégradée",
                         "Ton SWOLF est élevé ($s). Concentre-toi sur la glisse en fin de séance.",
                         Icons.Default.Warning,
                         isPositive = false,
                         adviceType = AdviceType.SWIM_GLIDE
                     ))
                 }
             }
             
             activity.breakoutSpeed?.let { bs ->
                 val swimSpeed = (activity.distanceKm * 1000) / (activity.durationMin * 60)
                  if (bs < swimSpeed) {
                      insights.add(Insight(
                          "Rupture de Coulée Précoce (Breakout)",
                          "V1.3 : Tu restes trop longtemps sous l'eau ou ta vitesse de coulée est trop faible. Reprends la nage plus tôt pour ne pas casser ton élan.",
                          Icons.Default.Warning,
                          isPositive = false,
                          adviceType = AdviceType.SWIM_CATCH
                      ))
                  }
             }
        }
        
        // 3. HEAT IMPACT (V1.3)
        activity.avgTemp?.let { t ->
            if (t > 25.0) {
                insights.add(Insight(
                    "Impact Thermique",
                    "V1.3 : Au-dessus de 25°C, ton cœur bat plus vite pour refroidir ton corps. Ton allure réelle est sous-estimée de ~3%.",
                    Icons.Default.Warning
                ))
            }
        }
        
        // Existing logic...
        val paceMinPerKm = if (activity.distanceKm > 0) activity.durationMin / activity.distanceKm else 0.0
        val speedKmh = if (activity.durationMin > 0) activity.distanceKm / (activity.durationMin / 60.0) else 0.0
        
        if (activity.type == WorkoutType.RUNNING || activity.source.contains("Run", ignoreCase = true)) {
            analyzeRunPace(paceMinPerKm, speedKmh, trainingPlan, insights)
            analyzeRunBiomechanics(activity, insights)
            analyzeRunPhysiology(activity, insights)
        } else if (activity.type == WorkoutType.SWIMMING || activity.source.contains("Swim", ignoreCase = true)) {
            analyzeSwimPace(speedKmh, insights)
            analyzeSwimEfficiency(activity, insights)
        }
        
        // 2. Load Advice (General)
        if (activity.durationMin > 90) {
             insights.add(Insight(
                 "Volume Élevé",
                 "Séance longue (>1h30). Hydratation et sommeil sont prioritaires ce soir.",
                 Icons.Default.Warning,
                 isPositive = false,
                 adviceType = AdviceType.RECOVERY_STRETCH
             ))
        }
        
        // 3. RPE Analysis (Subjective)
        activity.rpe?.let { rpe ->
            if (rpe > 8) {
                insights.add(Insight("Ressenti Difficile", "RPE $rpe/10. Une séance très intense qui nécessite 48h de récupération.", Icons.Default.Favorite))
            } else if (rpe < 4) {
                 insights.add(Insight("Aisance Respiratoire", "RPE $rpe/10. Excellent pour la récupération active.", Icons.Default.Favorite))
            }
            Unit
        }

        return insights
    }
    
    // --- RUNNING ANALYSIS ---
    
    private fun analyzeRunPace(pace: Double, speedKmh: Double, trainingPlan: TrainingPlanResult?, list: MutableList<Insight>) {
        val paceStr = formatPace(pace)
        
        // Use actual training zones if available
        if (trainingPlan != null) {
            val zone = trainingPlan.speedZones.find { speedKmh >= it.minSpeedKmh && speedKmh <= it.maxSpeedKmh }
            when (zone?.id) {
                1 -> {} // Z1 - Recovery, no insight needed
                2 -> list.add(Insight("Allure Zone 2", "Allure $paceStr/km en endurance fondamentale. Parfait pour construire la base aérobie.", Icons.Default.Speed, adviceType = AdviceType.RUN_TECHNIQUE))
                3 -> list.add(Insight("Allure Zone 3", "Allure $paceStr/km en tempo. Zone d'entraînement productive.", Icons.Default.Speed, adviceType = AdviceType.RUN_TECHNIQUE))
                4 -> list.add(Insight("Allure Seuil (Z4)", "Allure $paceStr/km au seuil lactique. Excellent travail de qualité.", Icons.Default.Speed, adviceType = AdviceType.RUN_TECHNIQUE))
                5 -> list.add(Insight("Allure VO2max (Z5)", "Allure $paceStr/km en zone VO2max. Séance très intense.", Icons.Default.Speed, isPositive = true, adviceType = AdviceType.RUN_TECHNIQUE))
            }
        } else {
            // Fallback to generic pace analysis if no training plan
            if (pace < 4.5) {
                list.add(Insight("Allure Rapide", "Allure moyenne $paceStr/km. Rythme élevé.", Icons.Default.Speed, adviceType = AdviceType.RUN_TECHNIQUE))
            }
        }
    }

    private fun analyzeRunBiomechanics(a: Persistence.CompletedActivity, list: MutableList<Insight>) {
        a.avgCadence?.let { cad ->
            if (cad < 165) {
                list.add(Insight(
                    "Cadence Faible ($cad)", 
                    "Attention : < 165 ppm augmente l'impact au sol et le risque de blessure (genoux/tibias). Essayez de raccourcir la foulée.", 
                    Icons.Default.Warning,
                    isPositive = false,
                    adviceType = AdviceType.RUN_TECHNIQUE
                ))
            } else if (cad > 175) {
                 list.add(Insight("Cadence Optimale ($cad)", "Excellente fréquence (>175 ppm). Cela réduit le coût énergétique et les chocs.", Icons.Default.Speed, adviceType = AdviceType.RUN_TECHNIQUE))
            }
            Unit
        }
    }

    private fun analyzeRunPhysiology(a: Persistence.CompletedActivity, list: MutableList<Insight>) {
        a.avgHeartRate?.let { hr ->
            // Estimation basique: MaxHR = 220-age (approx 185 pour 35 ans)
            // Z1-Z2 < 145 bpm
            if (hr < 145) {
                 list.add(Insight("Endurance Fondamentale", "FC Moy $hr bpm. Parfait pour le développement mitochondrial et la récupération.", Icons.Default.Favorite, adviceType = AdviceType.RECOVERY_STRETCH))
            } else if (hr > 170) {
                 list.add(Insight("Haute Intensité", "FC Moy $hr bpm. Séance très sollicitante pour le cœur (Zone 4/5).", Icons.Default.Favorite, adviceType = AdviceType.PPG_CIRCUIT))
            }
            Unit
        }
    }

    // --- SWIMMING ANALYSIS ---

    private fun analyzeSwimPace(paceKmh: Double, list: MutableList<Insight>) {
        if (paceKmh > 3.0) list.add(Insight("Glisse Rapide", "Vitesse > 3 km/h. Très bonne hydrodynamique.", Icons.Default.Speed, adviceType = AdviceType.SWIM_GLIDE))
    }

    private fun analyzeSwimEfficiency(a: Persistence.CompletedActivity, list: MutableList<Insight>) {
        // SWOLF = Time(s) + Strokes per length
        // Lower is better
        if (a.swolf != null) {
            val s = a.swolf
            if (s < 35) list.add(Insight("SWOLF Excellent ($s)", "Score digne d'un nageur élite. Efficacité maximale.", Icons.Default.Speed, adviceType = AdviceType.SWIM_GLIDE))
            else if (s > 45) list.add(Insight("Efficacité Moyenne ($s)", "Essayez de réduire le nombre de coups de bras par longueur.", Icons.Default.Warning, isPositive = false, adviceType = AdviceType.SWIM_PPG))
            else list.add(Insight("Bon SWOLF ($s)", "Bon ratio glisse/fréquence.", Icons.Default.Speed, adviceType = AdviceType.SWIM_GLIDE))
        }
        
        // SPL (Strokes Per Length) - Assumption 25m pool
        if (a.totalStrokes != null && a.distanceKm > 0) {
            val lengths = (a.distanceKm * 1000) / 25f
            if (lengths > 0) {
                val spl = a.totalStrokes / lengths
                if (spl > 20) list.add(Insight("Technique ($spl coups/longueur)", "Vous nagez avec beaucoup de mouvements. Cherchez plus d'amplitude.", Icons.Default.Warning, isPositive = false, adviceType = AdviceType.SWIM_CATCH))
                else if (spl < 12) list.add(Insight("Grande Amplitude ($spl coups/longueur)", "Excellente glisse !", Icons.Default.Speed, adviceType = AdviceType.SWIM_GLIDE))
            }
        }
    }
    
    // --- HEART RATE ANALYSIS (NEW) ---
    
    /**
     * Calculate precise HR drift from samples (not estimation)
     * @return Percentage drift (positive = HR increased over time)
     */
    fun calculateHRDriftPrecise(samples: List<Persistence.HeartRateSample>): Double? {
        if (samples.size < 600) return null // Minimum 10 minutes
        
        val firstHalf = samples.take(samples.size / 2).map { it.bpm }.average()
        val secondHalf = samples.takeLast(samples.size / 2).map { it.bpm }.average()
        
        return ((secondHalf - firstHalf) / firstHalf) * 100
    }
    
    /**
     * Calculate time spent in each HR zone
     * @param maxHR Maximum heart rate (220 - age or measured)
     * @return Map of zone number to seconds spent in that zone
     */
    fun calculateHRZoneDistribution(
        samples: List<Persistence.HeartRateSample>,
        maxHR: Int
    ): Map<Int, Int> {
        val zones = samples.groupBy { sample ->
            when (sample.bpm.toDouble() / maxHR) {
                in 0.0..0.6 -> 1  // Recovery
                in 0.6..0.7 -> 2  // Aerobic
                in 0.7..0.8 -> 3  // Tempo
                in 0.8..0.9 -> 4  // Threshold
                else -> 5         // VO2 Max
            }
        }
        
        return zones.mapValues { it.value.size } // Size = seconds
    }
    
    /**
     * Calculate time spent in target HR zone
     * @return Seconds spent in target zone
     */
    fun timeInTargetZone(
        samples: List<Persistence.HeartRateSample>,
        targetMin: Int,
        targetMax: Int
    ): Int {
        return samples.count { it.bpm in targetMin..targetMax }
    }
    
    /**
     * Detect if HR effort was consistent or variable
     * @return "Régulier" if consistent, "Variable" if not
     */
    fun analyzeHRConsistency(hrVariability: Double?): String {
        return when {
            hrVariability == null -> "Inconnu"
            hrVariability < 5.0 -> "Très régulier"
            hrVariability < 10.0 -> "Régulier"
            hrVariability < 15.0 -> "Variable"
            else -> "Très variable"
        }
    }
    
    private fun formatPace(minPerKm: Double): String {
        val m = minPerKm.toInt()
        val s = ((minPerKm - m) * 60).toInt()
        return String.format("%d'%02d\"", m, s)
    }
}
