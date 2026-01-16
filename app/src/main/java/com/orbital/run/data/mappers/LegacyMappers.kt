package com.orbital.run.data.mappers

import com.orbital.run.data.local.entities.ActivityEntity
import com.orbital.run.logic.Persistence
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Mappers for converting legacy Persistence.CompletedActivity to new ActivityEntity.
 *
 * Used during JSON migration to transform old data structure into Room entities.
 * Handles all field mappings including complex types serialization.
 */

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false  // Compact JSON for database storage
}

/**
 * Convert legacy CompletedActivity to Room entity.
 *
 * Transforms:
 * - date (Long) → completedAtEpochMilli (Long)
 * - durationMin (Int) → durationSeconds (Long)
 * - distanceKm (Double) → distanceMeters (Double)
 * - source (String) → normalized DataSource name
 * - type (WorkoutType) → ActivityType name
 * - Complex lists → JSON strings
 */
fun Persistence.CompletedActivity.toEntity(): ActivityEntity {
    return ActivityEntity(
        // Core fields
        id = this.id,
        completedAtEpochMilli = this.date,
        type = mapLegacyWorkoutType(this.type),
        title = this.title,
        distanceMeters = this.distanceKm * 1000,  // km → meters
        durationSeconds = this.durationMin * 60L,  // minutes → seconds
        source = normalizeLegacySource(this.source),
        
        // Cardiovascular
        avgHeartRate = this.avgHeartRate,
        maxHeartRate = this.maxHeartRate,
        heartRateVariability = this.hrVariability,
        
        // Movement
        avgCadence = this.avgCadence,
        totalStrokes = this.totalStrokes,
        swolf = this.swolf,
        
        // Power
        avgPower = this.avgWatts,
        normalizedPower = this.weightedAvgWatts,
        totalEnergy = this.kilojoules,
        hasPowerMeter = this.deviceWatts,
        criticalPower = this.criticalPower,
        
        // Environmental
        elevationGain = this.elevationGain,
        avgAltitude = this.avgAltitude,
        avgTemperature = this.avgTemp,
        
        // Running biomechanics
        groundContactTime = this.avgGctMs,
        groundContactBalance = this.gctBalanceLeft,
        verticalOscillation = null,  // Not in legacy
        verticalRatio = this.verticalRatio,
        legSpringStiffness = this.legSpringStiffness,
        dutyFactor = this.dutyFactor,
        
        // Swimming
        strokeIndex = this.strokeIndex,
        velocityVariation = this.ivv,
        coordinationIndex = this.idc,
        turnTime = this.turnTime5m,
        breakoutSpeed = this.breakoutSpeed,
        
        // Efficiency
        efficiencyFactor = this.efficiencyFactor,
        runningEffectiveness = this.runningEffectiveness,
        aerodynamicPower = this.airPowerAvg,
        
        // Training load
        perceivedExertion = this.rpe,
        stressScore = this.sufferScore,
        caloriesBurned = this.calories,
        fitness = this.ctl,
        fatigue = this.atl,
        form = this.tsb,
        workloadRatio = this.acwr,
        monotony = this.monotony,
        strain = this.strain,
        
        // Respiratory
        avgRespiratoryRate = this.avgRespiratoryRate,
        
        // Metadata
        notes = this.notes,
        externalId = this.externalId,
        routePolyline = this.summaryPolyline,
        
        // Complex types → JSON
        splitsJson = if (this.splits.isNotEmpty()) {
            json.encodeToString(this.splits.map { it.toSerializable() })
        } else null,
        
        zoneDistributionJson = if (this.zoneDistribution.isNotEmpty()) {
            json.encodeToString(this.zoneDistribution)
        } else null,
        
        heartRateSamplesJson = if (this.heartRateSamples.isNotEmpty()) {
            json.encodeToString(this.heartRateSamples.map { it.toSerializable() })
        } else null,
        
        speedSamplesJson = if (this.speedSamples.isNotEmpty()) {
            json.encodeToString(this.speedSamples.map { it.toSerializable() })
        } else null,
        
        powerSamplesJson = if (this.powerSamples.isNotEmpty()) {
            json.encodeToString(this.powerSamples.map { it.toSerializable() })
        } else null,
        
        cadenceSamplesJson = if (this.cadenceSamples.isNotEmpty()) {
            json.encodeToString(this.cadenceSamples.map { it.toSerializable() })
        } else null,
        
        elevationSamplesJson = if (this.elevationSamples.isNotEmpty()) {
            json.encodeToString(this.elevationSamples.map { it.toSerializable() })
        } else null,
        
        strideLengthSamplesJson = if (this.strideLengthSamples.isNotEmpty()) {
            json.encodeToString(this.strideLengthSamples.map { it.toSerializable() })
        } else null,
        
        gctSamplesJson = if (this.gctSamples.isNotEmpty()) {
            json.encodeToString(this.gctSamples.map { it.toSerializable() })
        } else null,
        
        verticalOscillationSamplesJson = if (this.verticalOscillationSamples.isNotEmpty()) {
            json.encodeToString(this.verticalOscillationSamples.map { it.toSerializable() })
        } else null,
        
        verticalRatioSamplesJson = if (this.verticalRatioSamples.isNotEmpty()) {
            json.encodeToString(this.verticalRatioSamples.map { it.toSerializable() })
        } else null,
        
        respiratorySamplesJson = if (this.respiratorySamples.isNotEmpty()) {
            json.encodeToString(this.respiratorySamples.map { it.toSerializable() })
        } else null,
        
        routeJson = this.gpsCoordinates?.let { coords ->
            if (coords.isNotEmpty()) {
                json.encodeToString(coords.map { it.toSerializable() })
            } else null
        }
    )
}

// ========================
// HELPER FUNCTIONS
// ========================

/**
 * Map legacy WorkoutType to new ActivityType string.
 */
private fun mapLegacyWorkoutType(type: com.orbital.run.logic.WorkoutType): String {
    return when (type) {
        com.orbital.run.logic.WorkoutType.RUNNING -> "RUNNING"
        com.orbital.run.logic.WorkoutType.SWIMMING -> "SWIMMING"
        com.orbital.run.logic.WorkoutType.CYCLING -> "CYCLING"
        com.orbital.run.logic.WorkoutType.EASY_RUN -> "EASY_RUN"
        com.orbital.run.logic.WorkoutType.LONG_RUN -> "LONG_RUN"
        com.orbital.run.logic.WorkoutType.INTERVALS -> "INTERVALS"
        com.orbital.run.logic.WorkoutType.RECOVERY -> "RECOVERY"
    }
}

/**
 * Normalize legacy source string to DataSource enum name.
 */
private fun normalizeLegacySource(source: String): String {
    return when (source.lowercase()) {
        "strava" -> "STRAVA"
        "health connect" -> "HEALTH_CONNECT"
        "garmin" -> "GARMIN"
        "polar" -> "POLAR"
        "suunto" -> "SUUNTO"
        "fitbit" -> "FITBIT"
        "withings" -> "WITHINGS"
        "drawrun", "" -> "MANUAL"
        else -> "MANUAL"
    }
}

// ========================
// SERIALIZABLE WRAPPERS
// ========================

/**
 * Serializable version of Split for JSON storage.
 */
@Serializable
data class SplitSerializable(
    val index: Int,
    val durationSec: Int,
    val avgHr: Int? = null,
    val avgWatts: Int? = null,
    val avgCadence: Int? = null,
    val gctMs: Double? = null
)

fun Persistence.Split.toSerializable() = SplitSerializable(
    index = this.kmIndex,
    durationSec = this.durationSec,
    avgHr = this.avgHr,
    avgWatts = this.avgWatts,
    avgCadence = this.avgCadence,
    gctMs = this.gctMs
)

/**
 * Serializable version of HeartRateSample.
 */
@Serializable
data class HeartRateSampleSerializable(
    val timeOffset: Int,
    val bpm: Int
)

fun Persistence.HeartRateSample.toSerializable() = HeartRateSampleSerializable(
    timeOffset = this.timeOffset,
    bpm = this.bpm
)

/**
 * Serializable version of SpeedSample.
 */
@Serializable
data class SpeedSampleSerializable(
    val timeOffset: Int,
    val speedMps: Double
)

fun Persistence.SpeedSample.toSerializable() = SpeedSampleSerializable(
    timeOffset = this.timeOffset,
    speedMps = this.speedMps
)

/**
 * Serializable version of PowerSample.
 */
@Serializable
data class PowerSampleSerializable(
    val timeOffset: Int,
    val watts: Double
)

fun Persistence.PowerSample.toSerializable() = PowerSampleSerializable(
    timeOffset = this.timeOffset,
    watts = this.watts
)

/**
 * Serializable version of CadenceSample.
 */
@Serializable
data class CadenceSampleSerializable(
    val timeOffset: Int,
    val rpm: Double
)

fun Persistence.CadenceSample.toSerializable() = CadenceSampleSerializable(
    timeOffset = this.timeOffset,
    rpm = this.rpm
)

/**
 * Serializable version of ElevationSample.
 */
@Serializable
data class ElevationSampleSerializable(
    val timeOffset: Int,
    val avgAltitude: Double
)

fun Persistence.ElevationSample.toSerializable() = ElevationSampleSerializable(
    timeOffset = this.timeOffset,
    avgAltitude = this.avgAltitude
)

/**
 * Serializable version of RunningDynamicSample.
 */
@Serializable
data class RunningDynamicSampleSerializable(
    val timeOffset: Int,
    val value: Double
)

fun Persistence.RunningDynamicSample.toSerializable() = RunningDynamicSampleSerializable(
    timeOffset = this.timeOffset,
    value = this.value
)

/**
 * Serializable version of RespiratorySample.
 */
@Serializable
data class RespiratorySampleSerializable(
    val timeOffset: Int,
    val rpm: Double
)

fun Persistence.RespiratorySample.toSerializable() = RespiratorySampleSerializable(
    timeOffset = this.timeOffset,
    rpm = this.rpm
)

/**
 * Serializable version of GpsCoordinate.
 */
@Serializable
data class GpsCoordinateSerializable(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null,
    val accuracy: Double? = null
)

fun Persistence.GpsCoordinate.toSerializable() = GpsCoordinateSerializable(
    latitude = this.latitude,
    longitude = this.longitude,
    timestamp = this.timestamp,
    altitude = this.altitude,
    accuracy = this.accuracy
)
