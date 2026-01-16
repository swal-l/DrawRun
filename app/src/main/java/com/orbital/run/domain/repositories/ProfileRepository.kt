package com.orbital.run.domain.repositories

import com.orbital.run.domain.models.UserProfile

/**
 * Repository for user profile and settings.
 *
 * Single profile per user (not multi-user).
 */
interface ProfileRepository {
    
    /**
     * Get current user profile.
     *
     * @return Profile if exists, null if user hasn't completed onboarding
     */
    suspend fun getUserProfile(): UserProfile?
    
    /**
     * Save user profile (creates or updates).
     */
    suspend fun saveUserProfile(profile: UserProfile)
    
    /**
     * Check if onboarding is complete.
     */
    suspend fun isOnboardingComplete(): Boolean
    
    /**
     * Mark onboarding as complete.
     */
    suspend fun setOnboardingComplete(complete: Boolean)
    
    /**
     * Clear all user data (profile, activities, plans, etc.).
     *
     * Dangerous operation - should require confirmation.
     */
    suspend fun clearAllData()
}
