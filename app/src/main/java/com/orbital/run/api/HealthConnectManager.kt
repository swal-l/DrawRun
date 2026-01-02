package com.orbital.run.api

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Manager for Health Connect integration.
 * Reads exercise data from Garmin (via Health Connect) and other sources.
 */
object HealthConnectManager {
    
    // Required permissions
    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(PowerRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        "android.permission.health.READ_EXERCISE_ROUTE",
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
    )
    
    /**
     * Check if Health Connect is available on this device.
     */
    suspend fun checkAvailability(context: Context): Int {
        return try {
            HealthConnectClient.getSdkStatus(context)
        } catch (e: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }
    
    /**
     * Check if Health Connect is available (simplified boolean).
     */
    suspend fun isAvailable(context: Context): Boolean {
        return checkAvailability(context) == HealthConnectClient.SDK_AVAILABLE
    }
    
    /**
     * Synchronous version for UI checks.
     */
    fun isAvailableSync(context: Context): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get HealthConnectClient instance.
     */
    fun getClient(context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }
    
    /**
     * Check current permission status.
     */
    suspend fun checkPermissions(context: Context): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getClient(context)
                client.permissionController.getGrantedPermissions()
            } catch (e: Exception) {
                emptySet()
            }
        }
    }
    
    /**
     * Check if all required permissions are granted.
     */
    suspend fun hasAllPermissions(context: Context): Boolean {
        val granted = checkPermissions(context)
        android.util.Log.d("HC_PERMS", "Granted permissions: ${granted.size}/${PERMISSIONS.size}")
        granted.forEach { android.util.Log.d("HC_PERMS", "  ✓ $it") }
        PERMISSIONS.filter { !granted.contains(it) }.forEach {
            android.util.Log.w("HC_PERMS", "  ✗ MISSING: $it")
        }
        val hasAll = PERMISSIONS.all { granted.contains(it) }
        android.util.Log.d("HC_PERMS", "Has all permissions: $hasAll")
        return hasAll
    }
    
    /**
     * Synchronous version for UI - runs async check in background.
     * Use this in onClick handlers to avoid suspend function issues.
     */
    fun hasAllPermissionsSync(context: Context): Boolean {
        return try {
            kotlinx.coroutines.runBlocking {
                hasAllPermissions(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("HC_PERMS", "Error checking permissions: ${e.message}")
            false
        }
    }

    /**
     * Checks if the integration is effectively enabled by the user AND has permissions.
     */
    fun isIntegrationEnabled(context: Context): Boolean {
        return Persistence.loadHealthConnectEnabled(context) && hasAllPermissionsSync(context)
    }
    
    /**
     * Request Health Connect permissions using the proper contract.
     * Returns a contract that should be used with rememberLauncherForActivityResult.
     */
    fun getPermissionsContract() = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    
    /**
     * Get the set of permissions to request.
     */
    fun getPermissionsToRequest() = PERMISSIONS

    /**
     * Get intent to open Health Connect settings.
     * Handles both legacy APK and Android 14 system settings.
     */
    fun getSettingsIntent(context: Context): Intent {
        val status = try { HealthConnectClient.getSdkStatus(context) } catch(e: Exception) { HealthConnectClient.SDK_UNAVAILABLE }
        
        return if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // Redirect to Play Store for legacy APK update
            Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                setPackage("com.android.vending")
            }
        } else if (android.os.Build.VERSION.SDK_INT >= 34) {
            // Android 14+ System Settings
            Intent("android.intent.action.VIEW_PERMISSION_USAGE").apply {
                addCategory("android.intent.category.HEALTH_PERMISSIONS")
            }
        } else {
            // Legacy SDK Intent
            Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
        }
    }
    
    /**
     * Read exercise sessions from Health Connect.
     */
    suspend fun readExerciseSessions(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<ExerciseSessionRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getClient(context)
                val request = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(endTime)
                    )
                )
                val response = client.readRecords(request)
                response.records
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Read heart rate samples for a specific time range.
     */
    suspend fun readHeartRateSamples(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<HeartRateRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getClient(context)
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(endTime)
                    )
                )
                val response = client.readRecords(request)
                response.records
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Convert ExerciseSessionRecord to CompletedActivity.
     */
    fun mapToCompletedActivity(
        session: ExerciseSessionRecord,
        distanceRecord: DistanceRecord? = null,
        caloriesRecord: TotalCaloriesBurnedRecord? = null,
        heartRateRecords: List<HeartRateRecord> = emptyList(),
        speedRecords: List<SpeedRecord> = emptyList(),
        powerRecords: List<PowerRecord> = emptyList(),
        stepsRecords: List<StepsRecord> = emptyList(),
        route: ExerciseRoute? = null
    ): Persistence.CompletedActivity {
        // Determine activity type
        val type = when (session.exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> WorkoutType.RUNNING
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> WorkoutType.SWIMMING
            else -> WorkoutType.RUNNING
        }
        
        val startTime = session.startTime.toEpochMilli()
        val endTime = session.endTime.toEpochMilli()
        val durationMin = ((endTime - startTime) / 60000).toInt()
        
        // Distance and Calories
        val distanceKm = distanceRecord?.distance?.inKilometers ?: 0.0
        val calories = caloriesRecord?.energy?.inKilocalories?.toInt()
        
        // Detailed HR Samples
        val detailedSamples = heartRateRecords.flatMap { record ->
            record.samples.map { sample ->
                Persistence.HeartRateSample(
                    timeOffset = ((sample.time.toEpochMilli() - startTime) / 1000).toInt(),
                    bpm = sample.beatsPerMinute.toInt()
                )
            }
        }.sortedBy { it.timeOffset }

        // Speed Samples
        val speedSamples = speedRecords.flatMap { record ->
            record.samples.map { sample ->
                Persistence.SpeedSample(
                    timeOffset = ((sample.time.toEpochMilli() - startTime) / 1000).toInt(),
                    speedMps = sample.speed.inMetersPerSecond
                )
            }
        }.sortedBy { it.timeOffset }

        // Power Samples
        val powerSamples = powerRecords.flatMap { record ->
            record.samples.map { sample ->
                Persistence.PowerSample(
                    timeOffset = ((sample.time.toEpochMilli() - startTime) / 1000).toInt(),
                    watts = sample.power.inWatts
                )
            }
        }.sortedBy { it.timeOffset }

        // Cadence Samples (Approximated from Steps if needed, but using StepsRecord intervals)
        val cadenceSamples = stepsRecords.map { record ->
            val durationSec = (record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 1000.0
            val stepsPerMin = if (durationSec > 0) (record.count / (durationSec / 60.0)) else 0.0
            Persistence.CadenceSample(
                timeOffset = ((record.startTime.toEpochMilli() - startTime) / 1000).toInt(),
                rpm = stepsPerMin
            )
        }.sortedBy { it.timeOffset }

        // Heart Rate (average from samples)
        val avgHr = if (detailedSamples.isNotEmpty()) {
            detailedSamples.map { it.bpm }.average().toInt()
        } else null
        
        val maxHr = if (detailedSamples.isNotEmpty()) {
            detailedSamples.maxOfOrNull { it.bpm }
        } else null
        
        // Extract Polyline from Route
        val polyline = route?.let { r ->
            if (r.route.isNotEmpty()) {
                encodePolyline(r.route.map { it.latitude to it.longitude })
            } else null
        }
        
        // V1.4: Extract Power/Speed if record available
        val power = if (session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING) {
             null 
        } else null
        
        // Source metadata
        val source = session.metadata.dataOrigin.packageName
        val isGarmin = source.contains("garmin", ignoreCase = true)
        val sourceLabel = if (isGarmin) "Garmin (Health Connect)" else "Health Connect"
        
        val externalId = "HC_${session.metadata.id}_${startTime}"
        
        return Persistence.CompletedActivity(
            id = java.util.UUID.randomUUID().toString(),
            date = startTime,
            type = type,
            title = session.title ?: if (type == WorkoutType.RUNNING) "Course" else "Natation",
            distanceKm = distanceKm,
            durationMin = durationMin,
            source = sourceLabel,
            avgHeartRate = avgHr,
            maxHeartRate = maxHr,
            avgCadence = if (cadenceSamples.isNotEmpty()) cadenceSamples.map { it.rpm }.average().toInt() else null,
            calories = calories,
            elevationGain = null,
            notes = null,
            splits = emptyList(),
            zoneDistribution = emptyList(),
            avgWatts = if (powerSamples.isNotEmpty()) powerSamples.map { it.watts }.average().toInt() else null,
            externalId = externalId,
            heartRateSamples = detailedSamples,
            speedSamples = speedSamples,
            powerSamples = powerSamples,
            cadenceSamples = cadenceSamples,
            summaryPolyline = polyline
        )
    }

    /**
     * Helper to encode points to Google Polyline format.
     */
    private fun encodePolyline(points: List<Pair<Double, Double>>): String {
        val result = StringBuilder()
        var lastLat = 0
        var lastLng = 0
        for (point in points) {
            val lat = (point.first * 1E5).toInt()
            val lng = (point.second * 1E5).toInt()
            encodeValue(lat - lastLat, result)
            encodeValue(lng - lastLng, result)
            lastLat = lat
            lastLng = lng
        }
        return result.toString()
    }

    private fun encodeValue(value: Int, result: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            result.append(((0x20 or (v and 0x1f)) + 63).toChar())
            v = v shr 5
        }
        result.append((v + 63).toChar())
    }
    
    /**
     * Sync recent activities from Health Connect.
     */
    suspend fun syncRecentActivities(context: Context, daysBack: Int = 30): List<Persistence.CompletedActivity> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (daysBack * 24 * 60 * 60 * 1000L)
        
        val sessions = readExerciseSessions(context, startTime, endTime)
        
        return sessions.mapNotNull { session ->
            try {
                val sTime = session.startTime.toEpochMilli()
                val eTime = session.endTime.toEpochMilli()
                
                val hrRecords = readHeartRateSamples(context, sTime, eTime)
                
                // Fetch extra data
                val client = getClient(context)
                val timeFilter = TimeRangeFilter.between(Instant.ofEpochMilli(sTime), Instant.ofEpochMilli(eTime))
                
                // Use aggregate for more accurate Distance and Calories
                val aggregateMetrics = setOf(
                    DistanceRecord.DISTANCE_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                )
                val aggregateResponse = client.aggregate(
                    AggregateRequest(
                        metrics = aggregateMetrics,
                        timeRangeFilter = timeFilter
                    )
                )
                
                val totalDistanceKm = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0
                val totalCalories = aggregateResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt()
                
                // Create dummy records for compatibility with mapToCompletedActivity if needed, 
                // but let's update mapToCompletedActivity interface or just use these values.
                
                val speedRecords = client.readRecords(ReadRecordsRequest(SpeedRecord::class, timeFilter)).records
                val powerRecords = client.readRecords(ReadRecordsRequest(PowerRecord::class, timeFilter)).records
                val stepsRecords = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeFilter)).records
                
                // Robust Route Retrieval (Directly from session property in alpha11+)
                val routeResult = when (val result = session.exerciseRouteResult) {
                    is ExerciseRouteResult.Data -> result.exerciseRoute
                    else -> null
                }
                
                mapToCompletedActivity(
                    session = session,
                    heartRateRecords = hrRecords,
                    distanceRecord = null, // We'll pass totals separately or keep passing records
                    caloriesRecord = null,
                    speedRecords = speedRecords,
                    powerRecords = powerRecords,
                    stepsRecords = stepsRecords,
                    route = routeResult
                ).copy(
                    distanceKm = if (totalDistanceKm > 0.0) totalDistanceKm else 0.0,
                    calories = totalCalories
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
