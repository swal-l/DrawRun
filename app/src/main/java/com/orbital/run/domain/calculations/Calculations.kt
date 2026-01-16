package com.orbital.run.domain.calculations

import com.orbital.run.domain.models.Distance
import com.orbital.run.domain.models.Duration
import kotlin.math.roundToInt

/**
 * Utility functions for sports performance calculations.
 *
 * All calculations are pure functions with no side effects.
 */

/**
 * Calculate pace in seconds per kilometer.
 *
 * @param distance Distance covered
 * @param duration Time taken
 * @return Pace in seconds per km, or null if distance is zero
 */
fun calculatePace(distance: Distance, duration: Duration): Long? {
    val km = distance.kilometers
    if (km <= 0.0) return null
    
    return (duration.seconds / km).roundToInt().toLong()
}

/**
 * Calculate speed in km/h.
 *
 * @param distance Distance covered
 * @param duration Time taken
 * @return Speed in km/h, or null if duration is zero
 */
fun calculateSpeed(distance: Distance, duration: Duration): Double? {
    val hours = duration.seconds / 3600.0
    if (hours <= 0.0) return null
    
    return distance.kilometers / hours
}

/**
 * Format pace as MM:SS string.
 *
 * @param paceSecondsPerKm Pace in seconds per km
 * @return Formatted string like "5:23"
 */
fun formatPace(paceSecondsPerKm: Long): String {
    val minutes = paceSecondsPerKm / 60
    val seconds = paceSecondsPerKm % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * Format duration as HH:MM:SS or MM:SS.
 *
 * @param duration Duration to format
 * @return Formatted string
 */
fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Convert distance to display format with appropriate unit.
 *
 * @param distance Distance to format
 * @return Formatted string like "5.2 km" or "850 m"
 */
fun formatDistance(distance: Distance): String {
    val km = distance.kilometers
    return if (km < 1.0) {
        "${distance.meters.roundToInt()} m"
    } else {
        String.format("%.1f km", km)
    }
}
