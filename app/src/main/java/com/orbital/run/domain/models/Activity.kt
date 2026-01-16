package com.orbital.run.domain.models

import java.time.Duration
import java.time.Instant

/**
 * Represents a completed athletic activity (run, swim, bike, etc.).
 *
 * This is a pure domain model with no Android dependencies, suitable for business logic
 * and testing. All timestamps use [Instant] for timezone safety, durations use [Duration]
 * for type safety, and metrics are organized by category.
 *
 * ## Design Philosophy
 * Inspired by Stripe's API design:
 * - Explicit property names (no abbreviations)
 * - Grouped by concern (core → metrics → samples)
 * - Immutable by default (only title is mutable for user editing)
 * - Nullable for optional data (avoiding magic values)
 *
 * @property id Unique identifier (UUIDv4)
 * @property completedAt When the activity occurred (UTC)
 * @property type Activity type (running, swimming, cycling, etc.)
 * @property title User-editable title
 * @property distance Distance in kilometers
 * @property duration Total duration (includes pauses)
 * @property source Data source (Strava, Health Connect, manual entry)
 */
data class Activity(
    // ========================
    // CORE IDENTIFIERS
    // ========================
    
    val id: String,
    val completedAt: Instant,
    val type: ActivityType,
    var title: String, // Mutable for user editing
    
    // ========================
    // BASIC METRICS
    // ========================
    
    /** Distance in kilometers */
    val distance: Distance,
    
    /** Total duration including pauses */
    val duration: Duration,
    
    /** Where this activity came from */
    val source: DataSource,
    
    // ========================
    // CARDIOVASCULAR METRICS
    // ========================
    
    /** Average heart rate in beats per minute */
    val averageHeartRate: Int? = null,
    
    /** Maximum heart rate in beats per minute */
    val maxHeartRate: Int? = null,
    
    /** Heart rate variability (RMSSD) in milliseconds */
    val heartRateVariability: Double? = null,
    
    // ========================
    // MOVEMENT METRICS
    // ========================
    
    /** Average cadence in steps/strokes per minute */
    val averageCadence: Int? = null,
    
    /** Total swim strokes (swimming only) */
    val totalStrokes: Int? = null,
    
    /** SWOLF score: strokes + seconds per pool length (swimming) */
    val swolf: Int? = null,
    
    // ========================
    // POWER METRICS
    // ========================
    
    /** Average power in watts */
    val averagePower: Int? = null,
    
    /** Normalized/weighted power in watts */
    val normalizedPower: Int? = null,
    
    /** Total energy in kilojoules */
    val totalEnergy: Float? = null,
    
    /** Whether power data comes from a device (true) or estimated (false) */
    val hasPowerMeter: Boolean = false,
    
    /** Critical power threshold (watts) */
    val criticalPower: Double? = null,
    
    // ========================
    // ENVIRONMENTAL METRICS
    // ========================
    
    /** Cumulative elevation gain in meters */
    val elevationGain: Int? = null,
    
    /** Average altitude in meters above sea level */
    val averageAltitude: Int? = null,
    
    /** Average temperature in Celsius */
    val averageTemperature: Double? = null,
    
    // ========================
    // RUNNING BIOMECHANICS
    // ========================
    
    /** Ground contact time in milliseconds */
    val groundContactTime: Double? = null,
    
    /** Ground contact time balance (% left foot) */
    val groundContactBalance: Double? = null,
    
    /** Vertical oscillation in centimeters */
    val verticalOscillation: Double? = null,
    
    /** Vertical ratio (oscillation / stride length %) */
    val verticalRatio: Double? = null,
    
    /** Leg spring stiffness (kN/m) */
    val legSpringStiffness: Double? = null,
    
    /** Duty factor (ground contact / stride duration) */
    val dutyFactor: Double? = null,
    
    // ========================
    // SWIMMING METRICS
    // ========================
    
    /** Stroke index (distance per stroke × speed) */
    val strokeIndex: Double? = null,
    
    /** Intracyclic velocity variation */
    val velocityVariation: Double? = null,
    
    /** Index of coordination */
    val coordinationIndex: Double? = null,
    
    /** Average turn time for 5m */
    val turnTime: Double? = null,
    
    /** Breakout speed after underwater phase */
    val breakoutSpeed: Double? = null,
    
    // ========================
    // EFFICIENCY METRICS
    // ========================
    
    /** Efficiency factor (NP ÷ avg HR) */
    val efficiencyFactor: Double? = null,
    
    /** Running effectiveness (scientific metric) */
    val runningEffectiveness: Double? = null,
    
    /** Aerodynamic power (watts) */
    val aerodynamicPower: Double? = null,
    
    // ========================
    // TRAINING LOAD METRICS
    // ========================
    
    /** Subjective effort (1-10 RPE scale) */
    val perceivedExertion: Int? = null,
    
    /** Algorithmic suffer/stress score */
    val stressScore: Int? = null,
    
    /** Estimated calories burned */
    val caloriesBurned: Int? = null,
    
    /** Chronic Training Load (fitness) */
    val fitness: Double? = null,
    
    /** Acute Training Load (fatigue) */
    val fatigue: Double? = null,
    
    /** Training Stress Balance (form) */
    val form: Double? = null,
    
    /** Acute:Chronic Workload Ratio */
    val workloadRatio: Double? = null,
    
    /** Training monotony index */
    val monotony: Double? = null,
    
    /** Training strain index */
    val strain: Double? = null,
    
    // ========================
    // RESPIRATORY
    // ========================
    
    /** Average respiratory rate (breaths per minute) */
    val averageRespiratoryRate: Double? = null,
    
    // ========================
    // METADATA
    // ========================
    
    /** User notes/comments */
    var notes: String? = null,
    
    /** External ID from source platform (Strava, Garmin, etc.) */
    val externalId: String? = null,
    
    /** Encoded route polyline (Google Polyline format) */
    val routePolyline: String? = null,
    
    // ========================
    // SPLITS & ZONES
    // ========================
    
    /** Per-kilometer or per-lap splits */
    val splits: List<Split> = emptyList(),
    
    /** Time spent in each HR/power zone (% of total duration) */
    val zoneDistribution: List<Float> = emptyList(),
    
    // ========================
    // TIME SERIES DATA
    // ========================
    
    /** High-resolution heart rate data */
    val heartRateSamples: List<HeartRateSample> = emptyList(),
    
    /** Speed over time */
    val speedSamples: List<SpeedSample> = emptyList(),
    
    /** Power output over time */
    val powerSamples: List<PowerSample> = emptyList(),
    
    /** Cadence over time */
    val cadenceSamples: List<CadenceSample> = emptyList(),
    
    /** Elevation profile */
    val elevationSamples: List<ElevationSample> = emptyList(),
    
    /** Stride length over time (running) */
    val strideLengthSamples: List<BiomechanicSample> = emptyList(),
    
    /** Ground contact time over time (running) */
    val groundContactTimeSamples: List<BiomechanicSample> = emptyList(),
    
    /** Vertical oscillation over time (running) */
    val verticalOscillationSamples: List<BiomechanicSample> = emptyList(),
    
    /** Vertical ratio over time (running) */
    val verticalRatioSamples: List<BiomechanicSample> = emptyList(),
    
    /** Respiratory rate over time */
    val respiratorySamples: List<RespiratorySample> = emptyList(),
    
    /** GPS coordinates for route visualization */
    val route: List<GpsPoint>? = null
) {
    
    // Computed properties for convenience
    
    /** Duration in seconds */
    val durationSeconds: Long
        get() = duration.seconds
    
    /** Average pace in minutes per kilometer (running/swimming) */
    val averagePace: Duration?
        get() = if (distance.kilometers > 0) {
            Duration.ofSeconds((durationSeconds / distance.kilometers).toLong())
        } else null
    
    /** Average speed in kilometers per hour */
    val averageSpeed: Double
        get() = if (durationSeconds > 0) {
            (distance.kilometers / durationSeconds) * 3600
        } else 0.0
}

// ========================
// ENUMS
// ========================

/**
 * Type of athletic activity.
 *
 * Modeled after Strava's activity type taxonomy.
 */
enum class ActivityType(val displayName: String) {
    RUNNING("Course à pied"),
    SWIMMING("Natation"),
    CYCLING("Cyclisme"),
    
    // Training-specific types (from training plans)
    EASY_RUN("Sortie facile"),
    LONG_RUN("Sortie longue"),
    INTERVALS("Fractionnés"),
    RECOVERY("Récupération");
    
    val isRunning: Boolean
        get() = this in setOf(RUNNING, EASY_RUN, LONG_RUN, INTERVALS, RECOVERY)
    
    val isSwimming: Boolean
        get() = this == SWIMMING
    
    val isCycling: Boolean
        get() = this == CYCLING
}

/**
 * Where the activity data originated.
 */
enum class DataSource(val displayName: String) {
    MANUAL("Saisie manuelle"),
    STRAVA("Strava"),
    HEALTH_CONNECT("Health Connect"),
    GARMIN("Garmin Connect"),
    POLAR("Polar Flow"),
    SUUNTO("Suunto App"),
    FITBIT("Fitbit"),
    WITHINGS("Withings");
    
    val isExternal: Boolean
        get() = this != MANUAL
}

// ========================
// VALUE OBJECTS
// ========================

/**
 * Distance value object with type safety.
 */
data class Distance(val meters: Double) {
    val kilometers: Double
        get() = meters / 1000.0
    
    companion object {
        fun fromKilometers(km: Double) = Distance(km * 1000)
    }
}

// ========================
// NESTED DATA CLASSES
// ========================

/**
 * Per-kilometer or per-lap split information.
 *
 * @property index Split number (1-indexed)
 * @property duration Time for this split
 * @property averageHeartRate Optional HR for this split
 * @property averagePower Optional power for this split
 * @property averageCadence Optional cadence for this split
 * @property groundContactTime Optional GCT for this split
 */
data class Split(
    val index: Int,
    val duration: Duration,
    val averageHeartRate: Int? = null,
    val averagePower: Int? = null,
    val averageCadence: Int? = null,
    val groundContactTime: Double? = null
)

/**
 * GPS coordinate for route visualization.
 *
 * @property latitude Latitude in decimal degrees
 * @property longitude Longitude in decimal degrees
 * @property timestamp When this point was recorded
 * @property altitude Elevation in meters above sea level
 * @property accuracy GPS accuracy in meters
 */
data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant,
    val altitude: Double? = null,
    val accuracy: Double? = null
)

/**
 * Heart rate measurement at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property beatsPerMinute Heart rate value
 */
data class HeartRateSample(
    val secondsOffset: Int,
    val beatsPerMinute: Int
)

/**
 * Speed measurement at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property metersPerSecond Speed value
 */
data class SpeedSample(
    val secondsOffset: Int,
    val metersPerSecond: Double
)

/**
 * Power output at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property watts Power value
 */
data class PowerSample(
    val secondsOffset: Int,
    val watts: Double
)

/**
 * Cadence measurement at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property stepsPerMinute Cadence value (steps or strokes)
 */
data class CadenceSample(
    val secondsOffset: Int,
    val stepsPerMinute: Double
)

/**
 * Elevation measurement at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property metersAboveSeaLevel Altitude value
 */
data class ElevationSample(
    val secondsOffset: Int,
    val metersAboveSeaLevel: Double
)

/**
 * Biomechanical measurement (stride, GCT, VO, VR) at a specific time.
 *
 * Generic sample for running dynamics metrics.
 *
 * @property secondsOffset Seconds since activity start
 * @property value Metric value (unit depends on type)
 */
data class BiomechanicSample(
    val secondsOffset: Int,
    val value: Double
)

/**
 * Respiratory rate measurement at a specific time.
 *
 * @property secondsOffset Seconds since activity start
 * @property breathsPerMinute Respiratory rate
 */
data class RespiratorySample(
    val secondsOffset: Int,
    val breathsPerMinute: Double
)
