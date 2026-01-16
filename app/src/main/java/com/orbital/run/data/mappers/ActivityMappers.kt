package com.orbital.run.data.mappers

import com.orbital.run.data.local.entities.ActivityEntity
import com.orbital.run.domain.models.*
import java.time.Instant
import java.time.Duration
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun ActivityEntity.toDomain(): Activity {
    return Activity(
        id = id,
        completedAt = Instant.ofEpochMilli(completedAtEpochMilli),
        type = try { ActivityType.valueOf(type) } catch (e: Exception) { ActivityType.RUNNING },
        title = title,
        distance = Distance(distanceMeters),
        duration = Duration.ofSeconds(durationSeconds),
        source = try { DataSource.valueOf(source) } catch (e: Exception) { DataSource.MANUAL },
        
        // Metrics
        averageHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        heartRateVariability = heartRateVariability,
        averageCadence = avgCadence,
        totalStrokes = totalStrokes,
        swolf = swolf,
        averagePower = avgPower,
        normalizedPower = normalizedPower,
        totalEnergy = totalEnergy,
        hasPowerMeter = hasPowerMeter,
        criticalPower = criticalPower,
        elevationGain = elevationGain,
        averageAltitude = avgAltitude,
        averageTemperature = avgTemperature,
        
        // Biomechanics
        groundContactTime = groundContactTime,
        groundContactBalance = groundContactBalance,
        verticalOscillation = verticalOscillation,
        verticalRatio = verticalRatio,
        legSpringStiffness = legSpringStiffness,
        dutyFactor = dutyFactor,
        
        // Swimming
        strokeIndex = strokeIndex,
        velocityVariation = velocityVariation,
        coordinationIndex = coordinationIndex,
        turnTime = turnTime,
        breakoutSpeed = breakoutSpeed,
        
        // Efficiency
        efficiencyFactor = efficiencyFactor,
        runningEffectiveness = runningEffectiveness,
        aerodynamicPower = aerodynamicPower,
        
        // Load
        perceivedExertion = perceivedExertion,
        stressScore = stressScore,
        caloriesBurned = caloriesBurned,
        fitness = fitness,
        fatigue = fatigue,
        form = form,
        workloadRatio = workloadRatio,
        monotony = monotony,
        strain = strain,
        
        // Resp
        averageRespiratoryRate = avgRespiratoryRate,
        
        // Meta
        notes = notes,
        externalId = externalId,
        routePolyline = routePolyline,
        
        // Collections (JSON)
        splits = splitsJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        zoneDistribution = zoneDistributionJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        heartRateSamples = heartRateSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        speedSamples = speedSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        powerSamples = powerSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        cadenceSamples = cadenceSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        elevationSamples = elevationSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        strideLengthSamples = strideLengthSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        groundContactTimeSamples = gctSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        verticalOscillationSamples = verticalOscillationSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        verticalRatioSamples = verticalRatioSamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        respiratorySamples = respiratorySamplesJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
        route = routeJson?.let { try { json.decodeFromString(it) } catch(e: Exception) { null } }
    )
}

fun Activity.toEntity(): ActivityEntity {
    return ActivityEntity(
        id = id,
        completedAtEpochMilli = completedAt.toEpochMilli(),
        type = type.name,
        title = title,
        distanceMeters = distance.meters,
        durationSeconds = duration.seconds,
        source = source.name,
        
        avgHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        heartRateVariability = heartRateVariability,
        avgCadence = averageCadence,
        totalStrokes = totalStrokes,
        swolf = swolf,
        avgPower = averagePower,
        normalizedPower = normalizedPower,
        totalEnergy = totalEnergy,
        hasPowerMeter = hasPowerMeter,
        criticalPower = criticalPower,
        elevationGain = elevationGain,
        avgAltitude = averageAltitude,
        avgTemperature = averageTemperature,
        
        groundContactTime = groundContactTime,
        groundContactBalance = groundContactBalance,
        verticalOscillation = verticalOscillation,
        verticalRatio = verticalRatio,
        legSpringStiffness = legSpringStiffness,
        dutyFactor = dutyFactor,
        
        strokeIndex = strokeIndex,
        velocityVariation = velocityVariation,
        coordinationIndex = coordinationIndex,
        turnTime = turnTime,
        breakoutSpeed = breakoutSpeed,
        
        efficiencyFactor = efficiencyFactor,
        runningEffectiveness = runningEffectiveness,
        aerodynamicPower = aerodynamicPower,
        
        perceivedExertion = perceivedExertion,
        stressScore = stressScore,
        caloriesBurned = caloriesBurned,
        fitness = fitness,
        fatigue = fatigue,
        form = form,
        workloadRatio = workloadRatio,
        monotony = monotony,
        strain = strain,
        
        avgRespiratoryRate = averageRespiratoryRate,
        
        notes = notes,
        externalId = externalId,
        routePolyline = routePolyline,
        
        splitsJson = if (splits.isNotEmpty()) json.encodeToString(splits) else null,
        zoneDistributionJson = if (zoneDistribution.isNotEmpty()) json.encodeToString(zoneDistribution) else null,
        heartRateSamplesJson = if (heartRateSamples.isNotEmpty()) json.encodeToString(heartRateSamples) else null,
        speedSamplesJson = if (speedSamples.isNotEmpty()) json.encodeToString(speedSamples) else null,
        powerSamplesJson = if (powerSamples.isNotEmpty()) json.encodeToString(powerSamples) else null,
        cadenceSamplesJson = if (cadenceSamples.isNotEmpty()) json.encodeToString(cadenceSamples) else null,
        elevationSamplesJson = if (elevationSamples.isNotEmpty()) json.encodeToString(elevationSamples) else null,
        strideLengthSamplesJson = if (strideLengthSamples.isNotEmpty()) json.encodeToString(strideLengthSamples) else null,
        gctSamplesJson = if (groundContactTimeSamples.isNotEmpty()) json.encodeToString(groundContactTimeSamples) else null,
        verticalOscillationSamplesJson = if (verticalOscillationSamples.isNotEmpty()) json.encodeToString(verticalOscillationSamples) else null,
        verticalRatioSamplesJson = if (verticalRatioSamples.isNotEmpty()) json.encodeToString(verticalRatioSamples) else null,
        respiratorySamplesJson = if (respiratorySamples.isNotEmpty()) json.encodeToString(respiratorySamples) else null,
        routeJson = route?.let { json.encodeToString(it) }
    )
}


