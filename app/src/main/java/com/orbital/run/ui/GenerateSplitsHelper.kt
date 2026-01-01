package com.orbital.run.ui

import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import kotlin.math.roundToInt

// Helper for Synthetic Splits (Fallback)
// Now smarter: Uses speed samples if available to determine exact km boundaries
fun generateConstantSplits(activity: Persistence.CompletedActivity, count: Int): List<Persistence.Split> {
    val isSwim = activity.type == WorkoutType.SWIMMING
    val distanceInterval = if (isSwim) 100.0 else 1000.0
    
    if (activity.speedSamples.isEmpty()) {
        // Fallback: Constant pace
        val durationSecTotal = (activity.durationMin * 60).coerceAtLeast(1)
        val distUnit = if (isSwim) (activity.distanceKm * 10.0) else activity.distanceKm // units of interval
        val secPerUnit = durationSecTotal.toDouble() / distUnit.coerceAtLeast(0.01)
        
        return (1..count).map { i ->
            val startTime = ((i - 1) * secPerUnit).toInt()
            val endTime = (i * secPerUnit).toInt()
            generateSplitFromTimeRange(activity, i, startTime, endTime, secPerUnit.toInt())
        }
    } else {
        // Smart Generation using Speed Samples
        val splits = mutableListOf<Persistence.Split>()
        val sortedSpeed = activity.speedSamples.sortedBy { it.timeOffset }
        
        var currentDist = 0.0
        var splitStartTime = sortedSpeed.firstOrNull()?.timeOffset ?: 0
        var currentSplitIndex = 1
        var prevTime = splitStartTime
        
        // We iterate samples to find where interval is crossed
        for (i in sortedSpeed.indices) {
            val s = sortedSpeed[i]
            val dt = (s.timeOffset - prevTime).coerceAtLeast(0) // Time since last sample
            
            // Assuming speedMps applies to interval. Simple integration.
            if (dt > 0) {
                 val distSeg = s.speedMps * dt
                 currentDist += distSeg
            }
            
            // Check if we crossed a boundary
            if (currentDist >= currentSplitIndex * distanceInterval) {
                // We reached a split
                val splitEndTime = s.timeOffset
                val duration = (splitEndTime - splitStartTime).coerceAtLeast(1)
                
                splits.add(generateSplitFromTimeRange(activity, currentSplitIndex, splitStartTime, splitEndTime, duration))
                
                splitStartTime = splitEndTime
                currentSplitIndex++
            }
            prevTime = s.timeOffset
        }
        
        return splits.take(count).ifEmpty { 
             // Safety fallback
             val durationSecTotal = (activity.durationMin * 60).coerceAtLeast(1)
             val distUnit = if (isSwim) (activity.distanceKm * 10.0) else activity.distanceKm
             val secPerUnit = durationSecTotal.toDouble() / distUnit.coerceAtLeast(0.01)
             (1..count).map { i ->
                val startTime = ((i - 1) * secPerUnit).toInt()
                val endTime = (i * secPerUnit).toInt()
                generateSplitFromTimeRange(activity, i, startTime, endTime, secPerUnit.toInt())
            }
        }
    }
}

private fun generateSplitFromTimeRange(
    activity: Persistence.CompletedActivity, 
    index: Int, 
    startTime: Int, 
    endTime: Int, 
    durationSec: Int
): Persistence.Split {
    val segmentHr = activity.heartRateSamples
        .filter { it.timeOffset in startTime..endTime }
        .map { it.bpm }
        .average()
        .takeIf { !it.isNaN() }?.toInt() ?: activity.avgHeartRate

    val segmentWatts = activity.powerSamples
        .filter { it.timeOffset in startTime..endTime }
        .map { it.watts }
        .average()
        .takeIf { !it.isNaN() }?.toInt() ?: activity.avgWatts
        
    val segmentCad = activity.cadenceSamples
        .filter { it.timeOffset in startTime..endTime }
        .map { it.rpm }
        .average()
        .takeIf { !it.isNaN() }?.toInt() ?: activity.avgCadence

    return Persistence.Split(
        kmIndex = index,
        durationSec = durationSec,
        avgHr = segmentHr,
        avgWatts = segmentWatts,
        avgCadence = segmentCad
    )
}
