package com.orbital.run.domain.calculations

import com.orbital.run.domain.models.Distance
import com.orbital.run.domain.models.Duration
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for calculation utilities.
 *
 * Tests pace, speed, and formatting functions for correctness.
 */
class CalculationsTest {
    
    @Test
    fun `calculatePace returns correct pace for 5km in 25 minutes`() {
        // 5km in 25 minutes = 5:00 min/km = 300 seconds/km
        val distance = Distance(meters = 5000.0)
        val duration = Duration(seconds = 1500) // 25 * 60
        
        val pace = calculatePace(distance, duration)
        
        assertEquals(300L, pace)
    }
    
    @Test
    fun `calculatePace returns null for zero distance`() {
        val distance = Distance(meters = 0.0)
        val duration = Duration(seconds = 1000)
        
        val pace = calculatePace(distance, duration)
        
        assertNull(pace)
    }
    
    @Test
    fun `calculateSpeed returns correct speed for 10km in 1 hour`() {
        // 10km in 1 hour = 10 km/h
        val distance = Distance(meters = 10000.0)
        val duration = Duration(seconds = 3600)
        
        val speed = calculateSpeed(distance, duration)
        
        assertEquals(10.0, speed!!, 0.01)
    }
    
    @Test
    fun `calculateSpeed returns null for zero duration`() {
        val distance = Distance(meters = 5000.0)
        val duration = Duration(seconds = 0)
        
        val speed = calculateSpeed(distance, duration)
        
        assertNull(speed)
    }
    
    @Test
    fun `formatPace returns correct format for 5 min 23 sec per km`() {
        val paceSeconds = 323L // 5:23
        
        val formatted = formatPace(paceSeconds)
        
        assertEquals("5:23", formatted)
    }
    
    @Test
    fun `formatPace pads seconds with zero`() {
        val paceSeconds = 305L // 5:05
        
        val formatted = formatPace(paceSeconds)
        
        assertEquals("5:05", formatted)
    }
    
    @Test
    fun `formatDuration returns HH MM SS for durations over 1 hour`() {
        val duration = Duration(seconds = 3723) // 1:02:03
        
        val formatted = formatDuration(duration)
        
        assertEquals("1:02:03", formatted)
    }
    
    @Test
    fun `formatDuration returns MM SS for durations under 1 hour`() {
        val duration = Duration(seconds = 723) // 12:03
        
        val formatted = formatDuration(duration)
        
        assertEquals("12:03", formatted)
    }
    
    @Test
    fun `formatDistance returns km for distances over 1km`() {
        val distance = Distance(meters = 5234.0) // 5.234 km
        
        val formatted = formatDistance(distance)
        
        assertEquals("5.2 km", formatted)
    }
    
    @Test
    fun `formatDistance returns meters for distances under 1km`() {
        val distance = Distance(meters = 850.0)
        
        val formatted = formatDistance(distance)
        
        assertEquals("850 m", formatted)
    }
    
    @Test
    fun `calculatePace handles fractional kilometers correctly`() {
        // 4.5km in 22:30 = 5:00 min/km
        val distance = Distance(meters = 4500.0)
        val duration = Duration(seconds = 1350) // 22:30
        
        val pace = calculatePace(distance, duration)
        
        assertEquals(300L, pace)
    }
    
    @Test
    fun `calculateSpeed handles marathon distance correctly`() {
        // Marathon 42.195km in 3:30:00 = 12.056 km/h
        val distance = Distance(meters = 42195.0)
        val duration = Duration(seconds = 12600) // 3.5 hours
        
        val speed = calculateSpeed(distance, duration)
        
        assertEquals(12.056, speed!!, 0.01)
    }
}
