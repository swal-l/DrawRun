package com.orbital.run.data.local.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Room TypeConverters for custom types.
 *
 * Converts domain types (Instant, Duration, LocalDate) to primitive types
 * that SQLite can store (Long, String).
 *
 * For complex types (Lists), uses Kotlinx Serialization for performance.
 */
class Converters {
    
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // ========================
    // INSTANT (Timestamps)
    // ========================
    
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
    
    @TypeConverter
    fun longToInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.ofEpochMilli(it) }
    }
    
    // ========================
    // DURATION
    // ========================
    
    @TypeConverter
    fun durationToLong(duration: Duration?): Long? {
        return duration?.seconds
    }
    
    @TypeConverter
    fun longToDuration(seconds: Long?): Duration? {
        return seconds?.let { Duration.ofSeconds(it) }
    }
    
    // ========================
    // LOCALDATE
    // ========================
    
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }
    
    @TypeConverter
    fun stringToLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { 
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // ========================
    // GENERIC JSON CONVERSION
    // ========================
    
    /**
     * Convert any serializable object to JSON string.
     *
     * Used for storing complex types like List<Split>, List<HeartRateSample>, etc.
     */
    inline fun <reified T> toJson(value: T?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    /**
     * Convert JSON string back to typed object.
     */
    inline fun <reified T> fromJson(jsonString: String?): T? {
        return jsonString?.let {
            try {
                json.decodeFromString<T>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // ========================
    // LIST<FLOAT> (Zone Distribution)
    // ========================
    
    @TypeConverter
    fun floatListToJson(list: List<Float>?): String? {
        return toJson(list)
    }
    
    @TypeConverter
    fun jsonToFloatList(json: String?): List<Float>? {
        return fromJson<List<Float>>(json)
    }
    
    // ========================
    // LIST<STRING> (for future use)
    // ========================
    
    @TypeConverter
    fun stringListToJson(list: List<String>?): String? {
        return toJson(list)
    }
    
    @TypeConverter
    fun jsonToStringList(json: String?): List<String>? {
        return fromJson<List<String>>(json)
    }
}
