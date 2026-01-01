package com.orbital.run.social

import java.time.Instant

data class SocialUser(
    val id: String,
    val name: String,
    val handle: String, // @username
    val avatarUrl: String? = null,
    val isVerified: Boolean = false
)

data class SocialActivity(
    val id: String,
    val user: SocialUser,
    val timestamp: Instant,
    val title: String,
    val description: String?,
    val type: SocialActivityType,
    
    // Stats
    val distanceKm: Double,
    val durationMin: Int,
    val paceMinPerKm: String?, // Formatted "5:30 /km"
    
    // Media
    val mapImageUrl: String? = null,
    val photoUrls: List<String> = emptyList(),
    
    // Engagement
    val drawCount: Int = 0, // "Likes" are called "Draws"
    val commentCount: Int = 0,
    val isDrawnByMe: Boolean = false
)

enum class SocialActivityType {
    RUN, CYCLE, SWIM, HIKE, WORKOUT
}

data class Comment(
    val id: String,
    val user: SocialUser,
    val content: String,
    val timestamp: Instant
)
