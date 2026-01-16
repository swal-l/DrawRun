package com.orbital.run.data.local.database

import android.content.Context
import androidx.room.Room
import com.orbital.run.data.local.dao.ActivityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependency injection.
 *
 * Provides singleton instances of:
 * - DrawRunDatabase
 * - All DAOs (ActivityDao, etc.)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provide singleton Room database instance.
     *
     * Database is created on first access and reused throughout app lifecycle.
     */
    @Provides
    @Singleton
    fun provideDrawRunDatabase(
        @ApplicationContext context: Context
    ): DrawRunDatabase {
        return Room.databaseBuilder(
            context,
            DrawRunDatabase::class.java,
            DrawRunDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()  // TODO: Replace with proper migrations in production
            .build()
    }
    
    /**
     * Provide ActivityDao from database.
     */
    @Provides
    @Singleton
    fun provideActivityDao(database: DrawRunDatabase): ActivityDao {
        return database.activityDao()
    }
    
    // TODO: Add other DAO providers as entities are created
    // @Provides
    // @Singleton
    // fun provideTrainingPlanDao(database: DrawRunDatabase): TrainingPlanDao {
    //     return database.trainingPlanDao()
    // }
}
