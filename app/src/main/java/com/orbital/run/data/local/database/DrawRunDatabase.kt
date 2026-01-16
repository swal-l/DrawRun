package com.orbital.run.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orbital.run.data.local.dao.ActivityDao
import com.orbital.run.data.local.entities.ActivityEntity

/**
 * Main Room database for DrawRun application.
 *
 * Contains all entities and provides DAO access.
 * Version 1 for initial release with legacy JSON migration support.
 *
 * Future migrations will be handled via Room's migration framework.
 */
@Database(
    entities = [
        ActivityEntity::class,
        // TrainingPlanEntity::class,  // TODO: Add in next phase
        // GearEntity::class,           // TODO: Add in next phase
        // PersonalRecordsEntity::class // TODO: Add in next phase
    ],
    version = 1,
    exportSchema = true  // Export schema for migration testing
)
@TypeConverters(Converters::class)
abstract class DrawRunDatabase : RoomDatabase() {
    
    /**
     * Activity DAO for completed activities.
     */
    abstract fun activityDao(): ActivityDao
    
    // TODO: Add other DAOs as entities are created
    // abstract fun trainingPlanDao(): TrainingPlanDao
    // abstract fun gearDao(): GearDao
    
    companion object {
        const val DATABASE_NAME = "drawrun_database"
    }
}
