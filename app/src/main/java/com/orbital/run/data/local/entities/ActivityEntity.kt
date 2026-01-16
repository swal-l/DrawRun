package com.orbital.run.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing completed activities.
 *
 * Design principles:
 * - Primitive types for SQLite compatibility (Long, Double, String)
 * - Complex types (splits, samples, route) stored as JSON strings
 * - Indexed on completed_at (for sorting) and external_id (for deduplication)
 * - Column names use snake_case (Room convention)
 *
 * Maps to domain.models.Activity via ActivityMapper.kt
 */
@Entity(
    tableName = "activities",
    indices = [
        Index(value = ["completed_at"], name = "idx_completed_at"),
        Index(value = ["external_id"], name = "idx_external_id", unique = true),
        Index(value = ["type"], name = "idx_type"),
        Index(value = ["source"], name = "idx_source")
    ]
)
data class ActivityEntity(
    
    // ========================
    // PRIMARY KEY
    // ========================
    
    @PrimaryKey
    val id: String,
    
    // ========================
    // CORE FIELDS
    // ========================
    
    @ColumnInfo(name = "completed_at")
    val completedAtEpochMilli: Long,
    
    @ColumnInfo(name = "type")
    val type: String,  // ActivityType.name
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,
    
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,
    
    @ColumnInfo(name = "source")
    val source: String,  // DataSource.name
    
    // ========================
    // CARDIOVASCULAR METRICS
    // ========================
    
    @ColumnInfo(name = "avg_heart_rate")
    val avgHeartRate: Int?,
    
    @ColumnInfo(name = "max_heart_rate")
    val maxHeartRate: Int?,
    
    @ColumnInfo(name = "heart_rate_variability")
    val heartRateVariability: Double?,
    
    // ========================
    // MOVEMENT METRICS
    // ========================
    
    @ColumnInfo(name = "avg_cadence")
    val avgCadence: Int?,
    
    @ColumnInfo(name = "total_strokes")
    val totalStrokes: Int?,
    
    @ColumnInfo(name = "swolf")
    val swolf: Int?,
    
    // ========================
    // POWER METRICS
    // ========================
    
    @ColumnInfo(name = "avg_power")
    val avgPower: Int?,
    
    @ColumnInfo(name = "normalized_power")
    val normalizedPower: Int?,
    
    @ColumnInfo(name = "total_energy")
    val totalEnergy: Float?,
    
    @ColumnInfo(name = "has_power_meter")
    val hasPowerMeter: Boolean,
    
    @ColumnInfo(name = "critical_power")
    val criticalPower: Double?,
    
    // ========================
    // ENVIRONMENTAL METRICS
    // ========================
    
    @ColumnInfo(name = "elevation_gain")
    val elevationGain: Int?,
    
    @ColumnInfo(name = "avg_altitude")
    val avgAltitude: Int?,
    
    @ColumnInfo(name = "avg_temperature")
    val avgTemperature: Double?,
    
    // ========================
    // RUNNING BIOMECHANICS
    // ========================
    
    @ColumnInfo(name = "ground_contact_time")
    val groundContactTime: Double?,
    
    @ColumnInfo(name = "ground_contact_balance")
    val groundContactBalance: Double?,
    
    @ColumnInfo(name = "vertical_oscillation")
    val verticalOscillation: Double?,
    
    @ColumnInfo(name = "vertical_ratio")
    val verticalRatio: Double?,
    
    @ColumnInfo(name = "leg_spring_stiffness")
    val legSpringStiffness: Double?,
    
    @ColumnInfo(name = "duty_factor")
    val dutyFactor: Double?,
    
    // ========================
    // SWIMMING METRICS
    // ========================
    
    @ColumnInfo(name = "stroke_index")
    val strokeIndex: Double?,
    
    @ColumnInfo(name = "velocity_variation")
    val velocityVariation: Double?,
    
    @ColumnInfo(name = "coordination_index")
    val coordinationIndex: Double?,
    
    @ColumnInfo(name = "turn_time")
    val turnTime: Double?,
    
    @ColumnInfo(name = "breakout_speed")
    val breakoutSpeed: Double?,
    
    // ========================
    // EFFICIENCY METRICS
    // ========================
    
    @ColumnInfo(name = "efficiency_factor")
    val efficiencyFactor: Double?,
    
    @ColumnInfo(name = "running_effectiveness")
    val runningEffectiveness: Double?,
    
    @ColumnInfo(name = "aerodynamic_power")
    val aerodynamicPower: Double?,
    
    // ========================
    // TRAINING LOAD METRICS
    // ========================
    
    @ColumnInfo(name = "perceived_exertion")
    val perceivedExertion: Int?,
    
    @ColumnInfo(name = "stress_score")
    val stressScore: Int?,
    
    @ColumnInfo(name = "calories_burned")
    val caloriesBurned: Int?,
    
    @ColumnInfo(name = "fitness")
    val fitness: Double?,
    
    @ColumnInfo(name = "fatigue")
    val fatigue: Double?,
    
    @ColumnInfo(name = "form")
    val form: Double?,
    
    @ColumnInfo(name = "workload_ratio")
    val workloadRatio: Double?,
    
    @ColumnInfo(name = "monotony")
    val monotony: Double?,
    
    @ColumnInfo(name = "strain")
    val strain: Double?,
    
    // ========================
    // RESPIRATORY
    // ========================
    
    @ColumnInfo(name = "avg_respiratory_rate")
    val avgRespiratoryRate: Double?,
    
    // ========================
    // METADATA
    // ========================
    
    @ColumnInfo(name = "notes")
    val notes: String?,
    
    @ColumnInfo(name = "external_id")
    val externalId: String?,
    
    @ColumnInfo(name = "route_polyline")
    val routePolyline: String?,
    
    // ========================
    // COMPLEX TYPES (JSON)
    // ========================
    
    /**
     * List<Split> serialized as JSON
     */
    @ColumnInfo(name = "splits_json")
    val splitsJson: String?,
    
    /**
     * List<Float> zone distribution serialized as JSON
     */
    @ColumnInfo(name = "zone_distribution_json")
    val zoneDistributionJson: String?,
    
    /**
     * List<HeartRateSample> serialized as JSON
     */
    @ColumnInfo(name = "heart_rate_samples_json")
    val heartRateSamplesJson: String?,
    
    /**
     * List<SpeedSample> serialized as JSON
     */
    @ColumnInfo(name = "speed_samples_json")
    val speedSamplesJson: String?,
    
    /**
     * List<PowerSample> serialized as JSON
     */
    @ColumnInfo(name = "power_samples_json")
    val powerSamplesJson: String?,
    
    /**
     * List<CadenceSample> serialized as JSON
     */
    @ColumnInfo(name = "cadence_samples_json")
    val cadenceSamplesJson: String?,
    
    /**
     * List<ElevationSample> serialized as JSON
     */
    @ColumnInfo(name = "elevation_samples_json")
    val elevationSamplesJson: String?,
    
    /**
     * List<BiomechanicSample> for stride length
     */
    @ColumnInfo(name = "stride_length_samples_json")
    val strideLengthSamplesJson: String?,
    
    /**
     * List<BiomechanicSample> for ground contact time
     */
    @ColumnInfo(name = "gct_samples_json")
    val gctSamplesJson: String?,
    
    /**
     * List<BiomechanicSample> for vertical oscillation
     */
    @ColumnInfo(name = "vertical_oscillation_samples_json")
    val verticalOscillationSamplesJson: String?,
    
    /**
     * List<BiomechanicSample> for vertical ratio
     */
    @ColumnInfo(name = "vertical_ratio_samples_json")
    val verticalRatioSamplesJson: String?,
    
    /**
     * List<RespiratorySample> serialized as JSON
     */
    @ColumnInfo(name = "respiratory_samples_json")
    val respiratorySamplesJson: String?,
    
    /**
     * List<GpsPoint> serialized as JSON (route coordinates)
     */
    @ColumnInfo(name = "route_json")
    val routeJson: String?
)
