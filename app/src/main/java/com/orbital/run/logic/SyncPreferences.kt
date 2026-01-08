package com.orbital.run.logic

import android.content.Context

object SyncPreferences {
    private const val PREFS_NAME = "sync_preferences"
    private const val KEY_DAYS_BACK = "days_back"

    // Options disponibles
    enum class SyncPeriod(val days: Int, val label: String) {
        LAST_30_DAYS(30, "30 derniers jours"),
        LAST_60_DAYS(60, "60 derniers jours"),
        LAST_90_DAYS(90, "3 derniers mois"),
        LAST_180_DAYS(180, "6 derniers mois"),
        LAST_365_DAYS(365, "1 an"),
        ALL_TIME(3650, "Tout l'historique") // ~10 ans pour couvrir "tout"
    }

    /**
     * Récupère la période de synchronisation actuelle.
     * Par défaut : 30 jours.
     */
    fun getSyncPeriod(context: Context): SyncPeriod {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to ALL_TIME as requested
        val days = prefs.getInt(KEY_DAYS_BACK, SyncPeriod.ALL_TIME.days)
        
        return SyncPeriod.values().find { it.days == days } ?: SyncPeriod.ALL_TIME
    }

    /**
     * Sauvegarde la nouvelle période de synchronisation.
     */
    fun setSyncPeriod(context: Context, period: SyncPeriod) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAYS_BACK, period.days).apply()
    }

    /**
     * Helper pour obtenir directement le nombre de jours.
     */
    fun getDaysBack(context: Context): Int {
        return getSyncPeriod(context).days
    }
}
