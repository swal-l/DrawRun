package com.orbital.run.presentation.screens.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.orbital.run.presentation.components.DrawRunTopBar
import com.orbital.run.presentation.components.StatCard
import com.orbital.run.presentation.components.charts.DrawRunLineChart
import kotlin.math.roundToInt

/**
 * Analytics screen with charts and insights.
 *
 * Inspired by Vercel Analytics clarity:
 * - Clean time range selector
 * - Prominent chart visualization
 * - Period statistics
 * - AI-like performance insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            DrawRunTopBar(
                title = "Analyses",
                actions = {
                    IconButton(onClick = { /* Export/Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partager"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                LoadingState(modifier = Modifier.padding(padding))
            }
            
            is AnalyticsUiState.Empty -> {
                EmptyState(
                    timeRange = state.timeRange,
                    modifier = Modifier.padding(padding)
                )
            }
            
            is AnalyticsUiState.Content -> {
                ContentState(
                    content = state,
                    selectedTimeRange = selectedTimeRange,
                    onTimeRangeSelected = { viewModel.selectTimeRange(it) },
                    modifier = Modifier.padding(padding)
                )
            }
            
            is AnalyticsUiState.Error -> {
                ErrorState(
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

// ========================
// STATE COMPOSABLES
// ========================

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    timeRange: TimeRange,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aucune donnée pour cette période",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeRange.getLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
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
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContentState(
    content: AnalyticsUiState.Content,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ========================
        // TIME RANGE SELECTOR
        // ========================
        
        item {
            TimeRangeSelector(
                selected = selectedTimeRange,
                onSelected = onTimeRangeSelected
            )
        }
        
        // ========================
        // CHART SECTION
        // ========================
        
        item {
            AnimatedContent(
                targetState = content.chartData,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith
                    fadeOut() + slideOutVertically()
                },
                label = "chart_animation"
            ) { chartData ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Distance par ${getChartGrouping(selectedTimeRange)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        DrawRunLineChart(
                            data = chartData,
                            yAxisLabel = { "${it.toInt()} km" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }
                }
            }
        }
        
        // ========================
        // PERIOD STATISTICS
        // ========================
        
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Statistiques",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Stats grid (2x2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Distance",
                        value = String.format("%.1f", content.stats.totalDistance),
                        unit = "km",
                        icon = Icons.Default.DirectionsRun,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        label = "Durée",
                        value = formatDuration(content.stats.totalDuration),
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Activités",
                        value = content.stats.activityCount.toString(),
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                    
                    content.stats.averagePace?.let { pace ->
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
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        // ========================
        // INSIGHTS SECTION
        // ========================
        
        if (content.insights.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Analyse de performance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    content.insights.forEach { insight ->
                        InsightCard(insight = insight)
                    }
                }
            }
        }
    }
}

// ========================
// COMPONENTS
// ========================

/**
 * Time range selector with chips.
 */
@Composable
private fun TimeRangeSelector(
    selected: TimeRange,
    onSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelected(range) },
                label = {
                    Text(
                        text = range.getLabel(),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Insight card showing performance comparison.
 */
@Composable
private fun InsightCard(
    insight: Insight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                InsightType.POSITIVE -> MaterialTheme.colorScheme.secondaryContainer
                InsightType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
                InsightType.NEGATIVE -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (insight.type) {
                    InsightType.POSITIVE -> Icons.Default.TrendingUp
                    InsightType.NEUTRAL -> Icons.Default.TrendingFlat
                    InsightType.NEGATIVE -> Icons.Default.TrendingDown
                },
                contentDescription = null,
                tint = when (insight.type) {
                    InsightType.POSITIVE -> MaterialTheme.colorScheme.secondary
                    InsightType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    InsightType.NEGATIVE -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = insight.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            insight.changePercent?.let { percent ->
                Text(
                    text = "${if (percent > 0) "+" else ""}$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (insight.type) {
                        InsightType.POSITIVE -> MaterialTheme.colorScheme.secondary
                        InsightType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
                        InsightType.NEGATIVE -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

// ========================
// HELPER FUNCTIONS
// ========================

private fun getChartGrouping(timeRange: TimeRange): String {
    return when (timeRange) {
        TimeRange.WEEK -> "jour"
        TimeRange.MONTH -> "jour"
        TimeRange.YEAR -> "semaine"
        TimeRange.ALL -> "mois"
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    
    return if (hours > 0) {
        "${hours}h${minutes.toString().padStart(2, '0')}"
    } else {
        "${minutes}min"
    }
}
