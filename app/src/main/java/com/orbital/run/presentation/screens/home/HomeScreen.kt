package com.orbital.run.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbital.run.data.migration.MigrationProgress
import com.orbital.run.presentation.components.*
import com.orbital.run.ui.theme.DataLarge
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Home/Dashboard screen.
 *
 * Shows:
 * - Weekly statistics (distance, duration, activity count)
 * - Recent activities list
 * - Migration progress (if in progress)
 *
 * Uses clean dashboard aesthetic inspired by Linear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onActivityClick: (String) -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            DrawRunTopBar(
                title = "DrawRun",
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                },
                onNavigationClick = onNavigationClick
            )
        },
        modifier = modifier
    ) { padding ->
        // Render UI based on state
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }
            
            is HomeUiState.Migrating -> {
                MigrationState(
                    progress = state.progress,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is HomeUiState.Content -> {
                ContentState(
                    weeklyStats = state.weeklyStats,
                    recentActivities = state.recentActivities,
                    lastActivity = state.lastActivity,
                    onActivityClick = onActivityClick,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is HomeUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// ========================
// STATE COMPOSABLES
// ========================

/**
 * Loading state with centered spinner.
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Migration in progress state.
 */
@Composable
private fun MigrationState(
    progress: MigrationProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (progress) {
            is MigrationProgress.Validating -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Validation des données...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            is MigrationProgress.Migrating -> {
                // Progress percentage
                Text(
                    text = "${progress.percentage}%",
                    style = DataLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = progress.percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Migration des activités...",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "${progress.current} / ${progress.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            is MigrationProgress.Verifying -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Vérification de l'intégrité...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            else -> {
                // Idle or Complete - should not reach here
            }
        }
    }
}

/**
 * Content state with dashboard layout.
 */
@Composable
private fun ContentState(
    weeklyStats: WeeklyStats,
    recentActivities: List<com.orbital.run.domain.models.Activity>,
    lastActivity: com.orbital.run.domain.models.Activity?,
    onActivityClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ========================
        // WELCOME HEADER
        // ========================
        
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = getTodayDate(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // ========================
        // WEEKLY STATS GRID (2x2)
        // ========================
        
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cette semaine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                // First row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distance
                    StatCard(
                        label = "Distance",
                        value = String.format("%.1f", weeklyStats.totalDistance),
                        unit = "km",
                        trend = weeklyStats.distanceTrend,
                        icon = Icons.Default.DirectionsRun,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Duration
                    StatCard(
                        label = "Durée",
                        value = String.format("%.1f", weeklyStats.totalHours),
                        unit = "h",
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Second row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Activity count
                    StatCard(
                        label = "Activités",
                        value = weeklyStats.activityCount.toString(),
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Average pace
                    weeklyStats.averagePace?.let { pace ->
                        val minutes = pace / 60
                        val seconds = pace % 60
                        StatCard(
                            label = "Allure moy.",
                            value = "$minutes:${seconds.toString().padStart(2, '0')}",
                            unit = "/km",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    } ?: run {
                        // Empty card to maintain grid
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        // ========================
        // RECENT ACTIVITIES
        // ========================
        
        item {
            Text(
                text = "Activités récentes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (recentActivities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune activité pour le moment",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentActivities) { activity ->
                ActivitySummaryItem(
                    activity = activity,
                    onClick = { onActivityClick(activity.id) }
                )
            }
        }
    }
}

/**
 * Error state with retry button.
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Erreur",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DrawRunButton(
            text = "Réessayer",
            onClick = onRetry,
            icon = Icons.Default.Refresh
        )
    }
}

// ========================
// HELPER FUNCTIONS
// ========================

/**
 * Get contextual greeting based on time of day.
 */
private fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when (hour) {
        in 0..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        else -> "Bonsoir"
    }
}

/**
 * Get formatted today's date.
 */
private fun getTodayDate(): String {
    val today = java.time.LocalDate.now()
    return today.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
}
