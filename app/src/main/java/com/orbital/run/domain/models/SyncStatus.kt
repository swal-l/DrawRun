package com.orbital.run.domain.models

import java.time.Instant

/**
 * Synchronization status for all external data sources.
 *
 * Tracks connection state, last sync times, and errors for each service.
 */
data class SyncStatus(
    val strava: ServiceConnection,
    val healthConnect: ServiceConnection,
    val garmin: ServiceConnection,
    val polar: ServiceConnection,
    val suunto: ServiceConnection
) {
    /**
     * At least one service is connected.
     */
    val hasAnyConnection: Boolean
        get() = listOf(strava, healthConnect, garmin, polar, suunto)
            .any { it.state == ConnectionState.CONNECTED }
    
    /**
     * List of all connected services.
     */
    val connectedServices: List<Pair<DataSource, ServiceConnection>>
        get() = buildList {
            if (strava.state == ConnectionState.CONNECTED) add(DataSource.STRAVA to strava)
            if (healthConnect.state == ConnectionState.CONNECTED) add(DataSource.HEALTH_CONNECT to healthConnect)
            if (garmin.state == ConnectionState.CONNECTED) add(DataSource.GARMIN to garmin)
            if (polar.state == ConnectionState.CONNECTED) add(DataSource.POLAR to polar)
            if (suunto.state == ConnectionState.CONNECTED) add(DataSource.SUUNTO to suunto)
        }
}

/**
 * Connection status for a single external service.
 *
 * @property state Current connection state
 * @property lastSyncAt Last successful sync timestamp
 * @property lastError Most recent error, if any
 * @property activitiesSynced Total number of activities imported
 */
data class ServiceConnection(
    val state: ConnectionState,
    val lastSyncAt: Instant?,
    val lastError: SyncError?,
    val activitiesSynced: Int = 0
) {
    /**
     * Whether this service is ready to sync.
     */
    val isReady: Boolean
        get() = state == ConnectionState.CONNECTED
    
    /**
     * Whether sync is currently in progress.
     */
    val isSyncing: Boolean
        get() = state == ConnectionState.SYNCING
    
    /**
     * Time since last successful sync.
     */
    val timeSinceLastSync: java.time.Duration?
        get() = lastSyncAt?.let { java.time.Duration.between(it, Instant.now()) }
}

/**
 * Connection state for an external service.
 */
enum class ConnectionState(val displayName: String) {
    DISCONNECTED("Déconnecté"),
    CONNECTING("Connexion..."),
    CONNECTED("Connecté"),
    SYNCING("Synchronisation..."),
    ERROR("Erreur"),
    DISABLED("Désactivé");
    
    val isActive: Boolean
        get() = this in setOf(CONNECTED, SYNCING)
}

/**
 * Error that occurred during sync.
 *
 * @property type Category of error
 * @property message Human-readable error description
 * @property occurredAt When the error happened
 * @property isRetryable Whether retrying might succeed
 */
data class SyncError(
    val type: SyncErrorType,
    val message: String,
    val occurredAt: Instant,
    val isRetryable: Boolean
)

/**
 * Category of sync errors.
 */
enum class SyncErrorType(val displayName: String) {
    NETWORK("Erreur réseau"),
    AUTHENTICATION("Authentification échouée"),
    RATE_LIMIT("Limite de taux atteinte"),
    PERMISSION_DENIED("Permission refusée"),
    DATA_FORMAT("Format de données invalide"),
    SERVER_ERROR("Erreur serveur"),
    UNKNOWN("Erreur inconnue");
    
    val requiresUserAction: Boolean
        get() = this in setOf(AUTHENTICATION, PERMISSION_DENIED)
}

/**
 * Result of a sync operation.
 */
sealed class SyncResult {
    data class Success(
        val activitiesAdded: Int,
        val activitiesMerged: Int,
        val duration: java.time.Duration
    ) : SyncResult()
    
    data class Failure(
        val error: SyncError
    ) : SyncResult()
    
    object Skipped : SyncResult()
}
