package com.orbital.run.logic

import kotlin.math.roundToInt
import kotlin.math.pow

// --- Data Classes ---
data class UserProfile(
    val age: Int,
    val isMale: Boolean, 
    val weightKg: Double,
    val restingHeartRate: Int,
    val currentWeeklyDistanceKm: Double,
    val goalDistanceKm: Double,
    val goalTimeMinutes: Double,
    val raceDateMillis: Long  // Race date instead of program weeks
) {
    // Calculate weeks from today to race
    val programDurationWeeks: Int
        get() {
            val now = System.currentTimeMillis()
            val diffMillis = raceDateMillis - now
            val diffDays = diffMillis / (1000 * 60 * 60 * 24)
            return (diffDays / 7).toInt().coerceAtLeast(1)
        }
    
    // Calculate target pace from goal
    val targetPaceMinPerKm: Double
        get() = goalTimeMinutes / goalDistanceKm
    
    val targetSpeedKmh: Double
        get() = 60.0 / targetPaceMinPerKm
}

data class HeartRateZone(val id: Int, val min: Int, val max: Int, val label: String)
data class SpeedZone(val id: Int, val minSpeedKmh: Double, val maxSpeedKmh: Double)
data class PowerZone(val id: Int, val minWatts: Int, val maxWatts: Int)

enum class TrainingPhase {
    BASE,      // Foncier - Endurance building
    BUILD,     // Développement - Threshold & VO2max  
    SPECIFIC,  // Spécifique - Target pace work
    TAPER      // Affûtage - Race prep
}

data class WorkoutStep(
    val description: String, 
    val durationOrDist: String, 
    val targetZone: Int?, // 1-5, null=Rest
    val targetPace: String? = null // "5:30/km" for target pace workouts
)

data class Workout(
    val type:  WorkoutType,
    val title: String,
    val totalDistanceKm: Double, // 0 for swim usually or km
    val totalDurationMin: Int, 
    val steps: List<WorkoutStep>,
    // New Metrics (Optional)
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgCadence: Int? = null,
    val totalStrokes: Int? = null,
    val swolf: Int? = null,
    val rpe: Int? = null,
    val elevationGain: Int? = null,
    val calories: Int? = null,
    val summaryPolyline: String? = null,
    val externalId: String? = null,
    // Scientific Fields
    val avgWatts: Int? = null,
    val weightedAvgWatts: Int? = null,
    val kilojoules: Float? = null,
    val sufferScore: Int? = null,
    val deviceWatts: Boolean = false,
    val avgTemp: Double? = null,
    val avgAltitude: Int? = null
)

enum class WorkoutType {
    EASY_RUN, LONG_RUN, INTERVALS, RECOVERY, SWIMMING, RUNNING, CYCLING
}

data class TrainingWeek(
    val weekNumber: Int,
    val phase: String, 
    val totalDistance: Double,
    val workouts: List<Workout>
)

data class RacePrediction(
    val distanceName: String,
    val timeMinutes: Double,
    val formattedTime: String
)

data class TrainingPlanResult(
    val userProfile: UserProfile, 
    val fcm: Int,
    val vma: Double,
    val vo2max: Double, 
    val hrZones: List<HeartRateZone>,
    val speedZones: List<SpeedZone>,
    val powerZones: List<PowerZone>,
    val volumePicSafe: Double,
    val volumePicPerformance: Double,
    val phaseDurations: Triple<Double, Double, Double>,
    val weeklyPlan: List<TrainingWeek>,
    val racePredictions: List<RacePrediction>
)

fun formatDuration(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val h = minutes / 60
    val m = minutes % 60
    return if (m > 0) "${h}h $m" else "${h}h"
}


enum class SwimStyle(val label: String) {
    CRAWL("Crawl"),
    BREASTSTROKE("Brasse"),
    BACKSTROKE("Dos"),
    BUTTERFLY("Papillon"),
    MIXED("4 Nages"),
    DRILLS("Éducatifs")
}

enum class SwimSessionType(val label: String) {
    ENDURANCE("Endurance"),
    TECHNIQUE("Technique"),
    SPEED("Vitesse"),
    RECOVERY("Récupération")
}

object SwimAlgorithm { 

    private fun parseDuration(s: String): Int {
        if (s.contains("min") || s.contains("s")) {
             return s.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
        // Fallback: Estimate from distance if only distance provided (e.g. "200m")
        // Assume 2:30/100m for calculation safety (inc rest)
        if (s.contains("m")) {
             val dist = s.filter { it.isDigit() }.toIntOrNull() ?: 0
             return (dist.toDouble() / 100.0 * 2.5).toInt()
        }
        return 0
    }

    private fun parseDist(descr: String): Double { // km
        // Support complex patterns like "10x 50m" or "Pyramide 100/200/300m"
        val lowercase = descr.lowercase()
        if (lowercase.contains("x")) {
            val parts = lowercase.split("x")
            val count = parts[0].trim().filter { it.isDigit() }.toIntOrNull() ?: 1
            val dist = parts[1].trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            return (count * dist) / 1000.0
        }
        
        // Look for "Xm" pattern in description
        val parts = descr.split(" ", "/", "(", ")")
        var totalMeters = 0
        parts.forEach { p ->
            val clean = p.trim().lowercase()
            if (clean.endsWith("m") && clean.dropLast(1).all { it.isDigit() }) {
                totalMeters += clean.dropLast(1).toIntOrNull() ?: 0
            } else if (clean.all { it.isDigit() } && clean.isNotEmpty()) {
                // Potential part of a chain like "100/200/300"
                totalMeters += clean.toInt()
            }
        }
        return totalMeters / 1000.0
    }

    fun generateSession(styles: List<SwimStyle>, targetType: String, targetValue: Int, sessionType: SwimSessionType): Workout {
        val isDistance = targetType == "Distance" 
        // If target is Time (min), estimate distance: 2:30/100m avg pace -> 150 min -> 6000m? No.
        // 1 hour = 60 min. 2.5 min/100m -> 2400m/hr.
        val targetDistMeters = if (isDistance) targetValue else (targetValue / 2.5 * 100).toInt()
        
        val steps = mutableListOf<WorkoutStep>()
        var currentDist = 0
        var currentDuration = 0
        
        // --- 1. WARMUP (15-20%) ---
        val warmupDist = (targetDistMeters * 0.15).coerceIn(200.0, 600.0).roundTo(50)
        val warmupTime = (warmupDist / 100 * 2.5).toInt() + 2 // +2 rest
        
        steps.add(WorkoutStep("Échauffement mixte (Nage complète / Educatif)", "${warmupDist}m", 1))
        currentDist += warmupDist
        currentDuration += warmupTime
        
        // --- 2. PRE-SET / DRILLS (10%) ---
        // Constant 200-400m of drills
        val drillDist = (targetDistMeters * 0.10).coerceIn(100.0, 400.0).roundTo(50)
        val drillTime = (drillDist / 100 * 3.0).toInt() // Slower
        
        val techStyle = if (styles.contains(SwimStyle.MIXED)) SwimStyle.CRAWL else styles.random()
        steps.add(WorkoutStep("Educatifs Techique (${techStyle.label})", "${drillDist}m", 2))
        currentDist += drillDist
        currentDuration += drillTime

        // --- 3. MAIN SET (60-65%) ---
        val cooldownDist = (targetDistMeters * 0.10).coerceIn(100.0, 300.0).roundTo(50)
        val mainSetTarget = targetDistMeters - currentDist - cooldownDist
        
        // Round to nearest 50 or 100 depending on distance
        val usableMainDist = (mainSetTarget / 50).toInt() * 50
        
        // Generate Pattern based on Session Type
        val mainSteps = when(sessionType) {
            SwimSessionType.ENDURANCE -> {
                // Long reps: 200s, 400s, 800s
                val repDist = if(usableMainDist >= 1500) 400 else 200
                val reps = usableMainDist / repDist
                val remainder = usableMainDist % repDist
                
                val subSteps = mutableListOf<WorkoutStep>()
                if (reps > 0) subSteps.add(WorkoutStep("${reps}x ${repDist}m Régulier (Repos 20s)", "${(reps*repDist)/100*2 + reps} min", 3))
                if (remainder > 0) subSteps.add(WorkoutStep("${remainder}m Souple", "${(remainder/100*2.5).toInt()} min", 2))
                subSteps
            }
            SwimSessionType.SPEED -> {
                // Short reps: 25s, 50s, 100s
                val repDist = 50
                val reps = usableMainDist / repDist
                // Break into sets of max 8-10 reps
                val setSize = 8
                val sets = reps / setSize
                val extraReps = reps % setSize
                
                val subSteps = mutableListOf<WorkoutStep>()
                if (sets > 0) subSteps.add(WorkoutStep("${sets} séries de ${setSize}x ${repDist}m Vite !! (R: 45s / R_Set: 2min)", "${sets*12} min", 5))
                if (extraReps > 0) subSteps.add(WorkoutStep("${extraReps}x ${repDist}m Progressif 1-$extraReps", "${extraReps*2} min", 4))
                subSteps
            }
            SwimSessionType.TECHNIQUE -> {
               // Medium reps with focus
               val repDist = 100
               val reps = usableMainDist / repDist
               listOf(WorkoutStep("${reps}x ${repDist}m (Focus Glisse/Amplitude)", "${(reps*repDist)/100*2.5 + reps} min", 2))
            }
            else -> listOf(WorkoutStep("Nage continue en endurance", "${usableMainDist}m", 2))
        }
        steps.addAll(mainSteps)
        currentDist += usableMainDist
        currentDuration += (usableMainDist / 100 * 2.2).toInt() // approx

        // --- 4. COOLDOWN ---
        val finalCooldown = targetDistMeters - currentDist // Absorb rounding errors
        if (finalCooldown > 0) {
             val cdTime = (finalCooldown / 100 * 2.5).toInt()
             steps.add(WorkoutStep("Retour au calme (Dos double bras / Brasse)", "${finalCooldown}m", 1))
             currentDist += finalCooldown
             currentDuration += cdTime
        }

        val finalTitle = "Natation ${sessionType.label} ($currentDist m)"
        
        return Workout(
            type = WorkoutType.SWIMMING,
            title = finalTitle,
            totalDistanceKm = currentDist / 1000.0,
            totalDurationMin = currentDuration,
            steps = steps
        )
    }

    private fun Double.roundTo(step: Int): Int {
        return (this / step).roundToInt() * step
    }
}





object OrbitalAlgorithm {

    private fun calculateFCM(age: Int, isMale: Boolean): Int = (208.754 - 0.734 * age).roundToInt()
    private fun calculateVO2Max(fcm: Int, restingHR: Int): Double = (9.2 + (1.9 * (fcm.toDouble() / restingHR))) * (fcm.toDouble() / restingHR)
    private fun calculateVMA(vo2Max: Double): Double = (vo2Max - 2.209) / 3.163

    fun estimateRaceTime(vma: Double, distanceKm: Double): Double {
        val baseDistKm = 2.0
        val baseTimeMin = (baseDistKm / vma) * 60.0
        return baseTimeMin * (distanceKm / baseDistKm).pow(1.06)
    }

    fun calculateHRZones(restingHR: Int, fcm: Int): List<HeartRateZone> {
        val hrr = fcm - restingHR
        val percentages = listOf(0.50 to 0.60, 0.60 to 0.70, 0.70 to 0.80, 0.80 to 0.90, 0.90 to 1.00)
        return percentages.mapIndexed { index, (minPct, maxPct) ->
            HeartRateZone(index + 1, (restingHR + hrr * minPct).roundToInt(), (restingHR + hrr * maxPct).roundToInt(), "Zone ${index + 1}")
        }
    }

    fun calculateSpeedZones(vma: Double): List<SpeedZone> {
        return listOf(
            SpeedZone(1, vma * 0.60, vma * 0.70),
            SpeedZone(2, vma * 0.70, vma * 0.80),
            SpeedZone(3, vma * 0.80, vma * 0.90),
            SpeedZone(4, vma * 0.90, vma),
            SpeedZone(5, vma, vma * 1.15)
        )
    }
    
    // Helper to format pace as "5:30/km"
    private fun formatTargetPace(paceMinPerKm: Double): String {
        val min = paceMinPerKm.toInt()
        val sec = ((paceMinPerKm - min) * 60).toInt()
        return "$min:${sec.toString().padStart(2, '0')}/km"
    }
    
    // Generate target pace workout based on goal distance
    private fun generateTargetPaceWorkout(user: UserProfile, qualityDist: Double, vma: Double): Workout {
        val targetPaceStr = formatTargetPace(user.targetPaceMinPerKm)
        
        return when {
            // 10K goal - repeat intervals at target pace
            user.goalDistanceKm <= 10.5 -> {
                Workout(WorkoutType.INTERVALS, "Allure 10K", qualityDist, 65,
                    listOf(
                        WorkoutStep("Échauffement", "15 min", 2),
                        WorkoutStep("4 x 2000m à $targetPaceStr", "35 min", null, targetPaceStr),
                        WorkoutStep("Retour au calme", "10 min", 1)
                    )
                )
            }
            // Half Marathon - longer intervals/tempo at target pace
            user.goalDistanceKm <= 22 -> {
                Workout(WorkoutType.INTERVALS, "Allure Semi-Marathon", qualityDist, 70,
                    listOf(
                        WorkoutStep("Échauffement", "15 min", 2),
                        WorkoutStep("3 x 5km à $targetPaceStr", "${(15*user.targetPaceMinPerKm).toInt()} min", null, targetPaceStr),
                        WorkoutStep("Retour au calme", "10 min", 1)
                    )
                )
            }
            // Marathon - long tempo run at target pace
            else -> {
                val tempoDist = (qualityDist * 1.5).coerceAtMost(20.0)
                Workout(WorkoutType.LONG_RUN, "Allure Marathon", tempoDist, (tempoDist * user.targetPaceMinPerKm).toInt(),
                    listOf(
                        WorkoutStep("Échauffement", "15 min", 2),
                        WorkoutStep("${tempoDist.toInt()}km à $targetPaceStr", "${(tempoDist*user.targetPaceMinPerKm).toInt()} min", null, targetPaceStr),
                        WorkoutStep("Retour au calme", "10 min", 1)
                    )
                )
            }
        }
    }

    fun calculatePowerZones(speedZones: List<SpeedZone>, weightKg: Double): List<PowerZone> {
        return speedZones.map { zone ->
            val minWatts = (zone.minSpeedKmh / 3.6 * weightKg * 0.98).roundToInt()
            val maxWatts = (zone.maxSpeedKmh / 3.6 * weightKg * 0.98).roundToInt()
            PowerZone(zone.id, minWatts, maxWatts)
        }
    }

    private fun calculateVolumePIC(currentVol: Double, programWeeks: Int, goalDist: Double, goalTime: Double, vma: Double): Pair<Double, Double> {
        val weeksForCalc = if (programWeeks > 3) programWeeks - 3 else 1
         val secureVol = currentVol * 1.10.pow(weeksForCalc.toDouble())
        val vTargetKmh = (goalDist / goalTime) * 60
        val A = 10 * (vTargetKmh / vma) - 5
        val perfVol = goalDist * (1 + (A / goalTime))
        return secureVol to perfVol
    }
    
    fun predictRaceTimes(vma: Double): List<RacePrediction> {
        val baseDistKm = 2.0
        val baseTimeMin = (baseDistKm / vma) * 60.0
        val distances = listOf("5 km" to 5.0, "10 km" to 10.0, "Semi" to 21.0975, "Marathon" to 42.195)
        return distances.map { (name, dist) ->
            val predictedTimeMin = baseTimeMin * (dist / baseDistKm).pow(1.06)
            val hours = (predictedTimeMin / 60).toInt()
            val mins = (predictedTimeMin % 60).toInt()
            val formatted = if (hours > 0) String.format("%dh%02d", hours, mins) else String.format("%dmin", mins)
            RacePrediction(name, predictedTimeMin, formatted)
        }
    }

    private fun generateTrainingWeeks(user: UserProfile, startVolume: Double, peakVolume: Double, phases: Triple<Double, Double, Double>, vma: Double): List<TrainingWeek> {
        val weeks = mutableListOf<TrainingWeek>()
        val totalWeeks = user.programDurationWeeks
        
        // Professional 4-phase periodization
        val taperWeeks = (totalWeeks * 0.10).roundToInt().coerceAtLeast(1)
        val remaining = totalWeeks - taperWeeks
        val baseWeeks = (remaining * 0.40).roundToInt() // 35% of total ≈ 40% of remaining
        val buildWeeks = (remaining * 0.28).roundToInt() // 25% of total
        val specificWeeks = remaining - baseWeeks - buildWeeks // Rest goes to specific
        
        val volumeStep = (peakVolume - startVolume) / (totalWeeks - taperWeeks).coerceAtLeast(1)
        
        for (i in 1..totalWeeks) {
            // Determine phase
            val (phase, phaseName) = when {
                i <= baseWeeks -> TrainingPhase.BASE to "Phase 1: Foncier"
                i <= baseWeeks + buildWeeks -> TrainingPhase.BUILD to "Phase 2: Développement"
                i <= baseWeeks + buildWeeks + specificWeeks -> TrainingPhase.SPECIFIC to "Phase 3: Spécifique" 
                else -> TrainingPhase.TAPER to "Phase 4: Affûtage"
            }
            
            // Calculate volume for this week
            val weekVol = when (phase) {
                TrainingPhase.TAPER -> {
                    val taperProgress = (i - (totalWeeks - taperWeeks)) / taperWeeks.toDouble()
                    peakVolume * (1.0 - 0.3 * taperProgress) // -30% progressive
                }
                else -> (startVolume + (volumeStep * (i - 1))).coerceAtMost(peakVolume)
            }
            
            val numSessions = (2 + weekVol / 23).roundToInt().coerceIn(3, 7)
            
            val workouts = generateWorkoutsForWeek(weekVol, numSessions, phase, vma, user)
            weeks.add(TrainingWeek(i, phaseName, weekVol, workouts))
        }
        return weeks
    }

    private fun generateWorkoutsForWeek(volume: Double, numSessions: Int, phase: TrainingPhase, vma: Double, user: UserProfile): List<Workout> {
        val workouts = mutableListOf<Workout>()
        val targetPaceStr = formatTargetPace(user.targetPaceMinPerKm)
        
        // 1. Long Run (anchor workout)
        val longRunDist = volume * 0.35
        val longRunDuration = (longRunDist / (vma * 0.65 / 60)).toInt().coerceAtLeast(45)
        
        val longRunSteps = when (phase) {
            TrainingPhase.SPECIFIC -> {
                // Include target pace segments in long run
                val warmupDist = 3.0
                val targetPaceDist = (longRunDist - 5.0).coerceAtLeast(5.0)
                listOf(
                    WorkoutStep("Échauffement", "${(warmupDist/(vma*0.6/60)).toInt()} min", 2),
                    WorkoutStep("Allure Objectif", "${(targetPaceDist*user.targetPaceMinPerKm).toInt()} min", null, targetPaceStr),
                    WorkoutStep("Retour au calme", "10 min", 1)
                )
            }
            else -> listOf(
                WorkoutStep("Échauffement progressif", "20 min", 1),
                WorkoutStep("Endurance fondamentale", "${(longRunDuration - 30).coerceAtLeast(10)} min", 2),
                WorkoutStep("Retour au calme", "10 min", 1)
            )
        }
        
        workouts.add(Workout(WorkoutType.LONG_RUN, "Sortie Longue", longRunDist, longRunDuration, longRunSteps))
        
        var remainingVol = volume - longRunDist
        var sessionsLeft = numSessions - 1
        
        // 2. Quality/Key Workout
        val qualityDist = volume * 0.20
        val qualityWorkout = when (phase) {
            TrainingPhase.BASE -> {
                // Fartlek & Hills for base building
                Workout(WorkoutType.INTERVALS, "Fartlek / Côtes", qualityDist, 50,
                    listOf(
                        WorkoutStep("Échauffement", "20 min", 1),
                        WorkoutStep("Fartlek ou Côtes", "20 min", 4),
                        WorkoutStep("Retour au calme", "10 min", 1)
                    )
                )
            }
            TrainingPhase.BUILD -> {
                // Threshold & VO2max work
                val buildType = (1..2).random()
                when (buildType) {
                    1 -> Workout(WorkoutType.INTERVALS, "Seuil (Tempo)", qualityDist, 60,
                        listOf(
                            WorkoutStep("Échauffement", "20 min", 1),
                            WorkoutStep("3x 8-10min Seuil (Z4)", "30 min", 4),
                            WorkoutStep("Retour au calme", "10 min", 1)
                        )
                    )
                    else -> Workout(WorkoutType.INTERVALS, "VMA (30/30)", qualityDist, 60,
                        listOf(
                            WorkoutStep("Échauffement", "20 min", 1),
                            WorkoutStep("Gammes", "5 min", 2),
                            WorkoutStep("2 séries 10x 30\"/30\"", "20 min", 5),
                            WorkoutStep("Retour au calme", "10 min", 1)
                        )
                    )
                }
            }
            TrainingPhase.SPECIFIC -> {
                // TARGET PACE WORK - KEY CHANGE!
                generateTargetPaceWorkout(user, qualityDist, vma)
            }
            TrainingPhase.TAPER -> {
                // Short intense efforts to maintain sharpness
                Workout(WorkoutType.INTERVALS, "Rappel Vitesse", qualityDist * 0.7, 45,
                    listOf(
                        WorkoutStep("Échauffement", "15 min", 1),
                        WorkoutStep("6x 400m allure 5K", "20 min", 5),
                        WorkoutStep("Retour au calme", "10 min", 1)
                    )
                )
            }
        }
        
        workouts.add(qualityWorkout)
        remainingVol -= qualityWorkout.totalDistanceKm
        sessionsLeft -= 1
        
        // 3.  Easy runs to fill remaining volume
        if (sessionsLeft > 0) {
            val baseEasyDist = remainingVol / sessionsLeft
            val easyDuration = (baseEasyDist / (vma * 0.65 / 60)).toInt().coerceAtLeast(30)
            
            for (k in 1..sessionsLeft) {
                val subType = when {
                    k == 1 -> "Footing & Lignes Droites"
                    k == 2 && phase != TrainingPhase.TAPER -> "Footing Progressif"
                    else -> "Footing Récupération"
                }
                
                val steps = when(subType) {
                    "Footing & Lignes Droites" -> listOf(
                        WorkoutStep("Endurance", "${easyDuration-5} min", 2),
                        WorkoutStep("5x 80m Lignes Droites", "5 min", 4)
                    )
                    "Footing Progressif" -> listOf(
                        WorkoutStep("Easy", "${easyDuration/2} min", 1),
                        WorkoutStep("Moyen (Finir allure marathon)", "${easyDuration/2} min", 2)
                    )
                    else -> listOf(
                        WorkoutStep("Footing très souple (Z1-Z2)", "$easyDuration min", 1)
                    )
                }
                
                workouts.add(Workout(
                    if(subType.contains("Récupération")) WorkoutType.RECOVERY else WorkoutType.EASY_RUN,
                    subType,
                    baseEasyDist,
                    easyDuration,
                    steps
                ))
            }
        }
        
        return workouts.sortedByDescending { it.totalDistanceKm }
    }
    
    fun generateRecoveryPlan(user: UserProfile): TrainingPlanResult {
        // Create a lightweight plan result for recovery
        val fcm = calculateFCM(user.age, user.isMale)
        val vo2Max = calculateVO2Max(fcm, user.restingHeartRate)
        val vma = calculateVMA(vo2Max)
        
        val hrZones = calculateHRZones(user.restingHeartRate, fcm)
        val speedZones = calculateSpeedZones(vma)
        val powerZones = calculatePowerZones(speedZones, user.weightKg)
        
        // 2 Weeks of Recovery
        val weeks = mutableListOf<TrainingWeek>()
        val startVol = user.currentWeeklyDistanceKm * 0.5
        
        for(i in 1..2) {
             val workouts = mutableListOf<Workout>()
             // 3 sessions max
             val sessions = 3
             val distPerSess = startVol / sessions
             
             workouts.add(Workout(WorkoutType.RECOVERY, "Récupération Active", distPerSess, (distPerSess/(vma*0.6/60)).toInt(), 
                listOf(WorkoutStep("Footing très souple", "${(distPerSess/(vma*0.6/60)).toInt()} min", 1))))
             workouts.add(Workout(WorkoutType.SWIMMING, "Nage Récupération", 1.0, 30, 
                listOf(WorkoutStep("Nage libre souple", "30 min", 1))))
             workouts.add(Workout(WorkoutType.EASY_RUN, "Footing Plaisir", distPerSess, (distPerSess/(vma*0.6/60)).toInt(), 
                listOf(WorkoutStep("Endurance fondamentale", "${(distPerSess/(vma*0.6/60)).toInt()} min", 2))))
             
             weeks.add(TrainingWeek(i, "Récupération Post-Course", startVol, workouts))
        }
        
        // Return dummy predictions or cleared ones
        return TrainingPlanResult(user, fcm, vma, vo2Max, hrZones, speedZones, powerZones, startVol, startVol, Triple(0.0,0.0,2.0), weeks, emptyList())
    }

    fun calculate(user: UserProfile): TrainingPlanResult {
        val fcm = calculateFCM(user.age, user.isMale)
        val vo2Max = calculateVO2Max(fcm, user.restingHeartRate)
        val vma = calculateVMA(vo2Max)
        
        val hrZones = calculateHRZones(user.restingHeartRate, fcm)
        val speedZones = calculateSpeedZones(vma)
        val powerZones = calculatePowerZones(speedZones, user.weightKg)
        
        val (volSafe, volPerf) = calculateVolumePIC(
            user.currentWeeklyDistanceKm,
            user.programDurationWeeks,
            user.goalDistanceKm,
            user.goalTimeMinutes,
            vma
        )
        
        val totalWeeks = user.programDurationWeeks.toDouble()
        val taperWeeks = if (totalWeeks > 2) 2.0 else 0.5
        val remaining = totalWeeks - taperWeeks
        val phases = Triple(remaining * 0.55, remaining * 0.45, taperWeeks)
        
        val weeklyPlan = generateTrainingWeeks(user, user.currentWeeklyDistanceKm, volSafe, phases, vma)
        val predictions = predictRaceTimes(vma)
        
        return TrainingPlanResult(user, fcm, vma, vo2Max, hrZones, speedZones, powerZones, volSafe, volPerf, phases, weeklyPlan, predictions)
    }
}
