package com.orbital.run.social

import com.orbital.run.logic.WorkoutType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.temporal.ChronoUnit
// but using object Singleton for simplicity as seen in other managers.

object SocialRepository {
    
    private val _feed = MutableStateFlow<List<SocialActivity>>(emptyList())
    val feed = _feed.asStateFlow()

    init {
        // Initialize with Mock Data
        generateMockData()
    }

    private fun generateMockData() {
        val users = listOf(
            SocialUser("u1", "Sophie D.", "@sophie_run", isVerified = true),
            SocialUser("u2", "Thomas B.", "@tom_b"),
            SocialUser("u3", "Léa M.", "@leam"),
            SocialUser("u4", "Alex R.", "@arunner")
        )

        val now = Instant.now()

        val activities = listOf(
            SocialActivity(
                id = "a1",
                user = users[0],
                timestamp = now.minus(2, ChronoUnit.HOURS),
                title = "Sortie longue du dimanche ☀️",
                description = "Sensations incroyables aujourd'hui ! Le travail de VMA commence à payer. Préparation pour le Marathon de Paris en bonne voie.",
                type = SocialActivityType.RUN,
                distanceKm = 21.1,
                durationMin = 115, // 1h55
                paceMinPerKm = "5:27 /km",
                mapImageUrl = "mock_map_1", 
                drawCount = 42,
                commentCount = 5,
                isDrawnByMe = true
            ),
            SocialActivity(
                id = "a2",
                user = users[1],
                timestamp = now.minus(5, ChronoUnit.HOURS),
                title = "Récupération légère",
                description = "Petit footing pour faire tourner les jambes après la grosse séance d'hier.",
                type = SocialActivityType.RUN,
                distanceKm = 8.5,
                durationMin = 45,
                paceMinPerKm = "5:17 /km",
                drawCount = 12,
                commentCount = 0,
                isDrawnByMe = false
            ),
            SocialActivity(
                id = "a3",
                user = users[2],
                timestamp = now.minus(1, ChronoUnit.DAYS),
                title = "Vélo sur les quais",
                description = "Le vent de face au retour était terrible 💨",
                type = SocialActivityType.CYCLE,
                distanceKm = 45.0,
                durationMin = 90,
                paceMinPerKm = "30.0 km/h",
                drawCount = 28,
                commentCount = 3,
                isDrawnByMe = false
            )
        )
        
        _feed.value = activities
    }

    suspend fun refreshFeed() {
        // Simulate network delay
        delay(1000)
        // In a real app, fetch from API. Here we just reset/shuffle mock data if we wanted.
    }

    fun toggleDraw(activityId: String) {
        _feed.update { currentList ->
            currentList.map { activity ->
                if (activity.id == activityId) {
                    val newIsDrawn = !activity.isDrawnByMe
                    val newCount = if (newIsDrawn) activity.drawCount + 1 else activity.drawCount - 1
                    activity.copy(isDrawnByMe = newIsDrawn, drawCount = newCount)
                } else {
                    activity
                }
            }
        }
    }
}
