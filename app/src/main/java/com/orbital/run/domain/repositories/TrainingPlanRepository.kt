package com.orbital.run.domain.repositories

import com.orbital.run.domain.models.TrainingPlan
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing training plans.
 */
interface TrainingPlanRepository {
    
    /**
     * Observe all saved training plans (reactive).
     */
    fun observePlans(): Flow<List<TrainingPlan>>
    
    /**
     * Get all saved training plans (one-time fetch).
     */
    suspend fun getAllPlans(): List<TrainingPlan>
    
    /**
     * Get a single plan by ID.
     */
    suspend fun getPlanById(id: String): TrainingPlan?
    
    /**
     * Save a new plan or update existing.
     */
    suspend fun savePlan(plan: TrainingPlan)
    
    /**
     * Delete a plan.
     */
    suspend fun deletePlan(id: String)
    
    /**
     * Clear all saved plans.
     */
    suspend fun clearAllPlans()
}
