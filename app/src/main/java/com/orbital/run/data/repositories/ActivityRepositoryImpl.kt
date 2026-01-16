package com.orbital.run.data.repositories

import com.orbital.run.data.local.dao.ActivityDao
import com.orbital.run.data.mappers.toDomain
import com.orbital.run.data.mappers.toEntity
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.models.ActivityType
import com.orbital.run.domain.repositories.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val activityDao: ActivityDao
) : ActivityRepository {

    override fun observeActivities(): Flow<List<Activity>> {
        return activityDao.observeActivities().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeActivitiesByType(type: ActivityType): Flow<List<Activity>> {
        return activityDao.observeActivitiesByType(type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllActivities(limit: Int): List<Activity> {
        return activityDao.getAllActivities(limit).map { it.toDomain() }
    }

    override suspend fun getActivityById(id: String): Activity? {
        return activityDao.getActivityById(id)?.toDomain()
    }

    override suspend fun saveActivity(activity: Activity) {
        activityDao.insertActivity(activity.toEntity())
    }

    override suspend fun saveActivities(activities: List<Activity>): Int {
        val entities = activities.map { it.toEntity() }
        val ids = activityDao.insertActivities(entities)
        return ids.size
    }

    override suspend fun deleteActivity(id: String) {
        activityDao.deleteActivityById(id)
    }

    override suspend fun searchActivities(query: String): List<Activity> {
        return activityDao.searchActivities(query).map { it.toDomain() }
    }

    override suspend fun getActivitiesInRange(start: Instant, end: Instant): List<Activity> {
        return activityDao.getActivitiesInRange(start.toEpochMilli(), end.toEpochMilli()).map { it.toDomain() }
    }

    override suspend fun clearAllActivities() {
        activityDao.clearAllActivities()
    }
}
