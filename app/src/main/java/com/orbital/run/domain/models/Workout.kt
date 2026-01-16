package com.orbital.run.domain.models

import java.time.Duration

/**
 * Pre-defined workout template (primarily for swimming).
 *
 * Unlike [PlannedWorkout] which is part of a training plan,
 * this represents a standalone workout session that users can save and reuse.
 *
 * @property type Activity type
 * @property title Workout name
 * @property targetDistance Total planned distance (if distance-based)
 * @property targetDuration Total planned duration (if time-based)
 * @property intervals Structured workout steps
 */
data class WorkoutTemplate(
    val id: String,
    val type: ActivityType,
    val title: String,
    val targetDistance: Distance?,
    val targetDuration: Duration?,
    val intervals: List<WorkoutInterval>,
    val sessionType: SwimSessionType? = null, // Primarily for swimming
    val styles: List<SwimStyle> = emptyList() // Swim styles used
) {
    /**
     * Total prescribed distance (sum of all distance-based intervals).
     */
    val totalPrescribedDistance: Distance
        get() {
            val meters = intervals.sumOf { interval ->
                when (val target = interval.target) {
                    is IntervalTarget.Distance -> target.meters
                    else -> 0.0
                }
            }
            return Distance(meters)
        }
}

/**
 * Swimming session type for workout planning.
 */
enum class SwimSessionType(val displayName: String, val emoji: String) {
    ENDURANCE("Endurance", "🏊"),
    SPEED("Vitesse", "⚡"),
    TECHNIQUE("Technique", "🎯"),
    MIXED("Mixte", "🔀");
    
    val description: String
        get() = when (this) {
            ENDURANCE -> "Focus : Régularité et glisse. Ne cherche pas la vitesse, mais l'efficience."
            SPEED -> "Focus : Intensité maximale sur les séries. Récupération active très souple."
            TECHNIQUE -> "Focus : Qualité du mouvement. Prends ton temps sur les éducatifs."
            MIXED -> "Combinaison d'objectifs : endurance, vitesse, et technique."
        }
}

/**
 * Swimming stroke style.
 */
enum class SwimStyle(val displayName: String, val emoji: String) {
    FREESTYLE("Crawl", "🏊"),
    BACKSTROKE("Dos", "🔄"),
    BREASTSTROKE("Brasse", "🐸"),
    BUTTERFLY("Papillon", "🦋"),
    MIXED("Mixte", "🔀"),
    INDIVIDUAL_MEDLEY("4 Nages", "🎨");
    
    val isTechnical: Boolean
        get() = this in setOf(BUTTERFLY, INDIVIDUAL_MEDLEY)
}

/**
 * Saved gear/equipment for tracking wear.
 *
 * @property id Unique identifier
 * @property name User-assigned name (e.g., "Nike Vaporfly")
 * @property type Equipment category
 * @property currentDistance Accumulated distance
 * @property maxDistance When to replace
 * @property isActive Whether currently in use
 */
data class Gear(
    val id: String,
    val name: String,
    val type: GearType,
    val currentDistance: Distance,
    val maxDistance: Distance,
    val isActive: Boolean = true
) {
    /**
     * Remaining usable distance.
     */
    val remainingDistance: Distance
        get() = Distance((maxDistance.meters - currentDistance.meters).coerceAtLeast(0.0))
    
    /**
     * Wear percentage (0-100).
     */
    val wearPercentage: Double
        get() = (currentDistance.meters / maxDistance.meters * 100).coerceAtMost(100.0)
    
    /**
     * Whether gear needs replacement soon.
     */
    val needsReplacement: Boolean
        get() = wearPercentage >= 90.0
}

/**
 * Type of athletic gear.
 */
enum class GearType(val displayName: String) {
    RUNNING_SHOES("Chaussures running"),
    TRAIL_SHOES("Chaussures trail"),
    BIKE("Vélo"),
    SWIM_GEAR("Équipement natation")
}
