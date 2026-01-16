package com.orbital.run.presentation.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.models.ActivityType
import com.orbital.run.presentation.components.ActivitySummaryItem
import com.orbital.run.presentation.components.DrawRunTopBar
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * History screen showing all activities with monthly grouping.
 *
 * Features:
 * - Sticky month headers
 * - Swipe-to-delete actions
 * - Sport type filters
 * - Search functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onActivityClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            DrawRunTopBar(
                title = "Historique",
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sport type filters
            SportFilterRow(
                selected = selectedFilter,
                onSelected = { viewModel.selectFilter(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Divider()
            
            // Activities list with monthly grouping
            if (activities.isEmpty()) {
                EmptyState()
            } else {
                ActivitiesList(
                    activities = activities,
                    onActivityClick = onActivityClick,
                    onDeleteActivity = { viewModel.deleteActivity(it) }
                )
            }
        }
    }
}

/**
 * Sport type filter chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SportFilterRow(
    selected: ActivityFilter,
    onSelected: (ActivityFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filter.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(filter.label)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Activities list with sticky month headers.
 */
@Composable
private fun ActivitiesList(
    activities: List<Activity>,
    onActivityClick: (String) -> Unit,
    onDeleteActivity: (Activity) -> Unit
) {
    // Group activities by month
    val groupedActivities = activities
        .groupBy { activity ->
            val date = activity.completedAt
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            YearMonth.from(date)
        }
        .toSortedMap(reverseOrder())  // Newest first
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedActivities.forEach { (month, monthActivities) ->
            // Sticky month header
            item(key = "header_$month") {
                MonthHeader(month = month)
            }
            
            // Activities for this month
            items(
                items = monthActivities,
                key = { it.id }
            ) { activity ->
                SwipeToDeleteItem(
                    onDelete = { onDeleteActivity(activity) }
                ) {
                    ActivitySummaryItem(
                        activity = activity,
                        onClick = { onActivityClick(activity.id) },
                        showDate = true
                    )
                }
            }
        }
    }
}

/**
 * Month header with sticky behavior.
 */
@Composable
private fun MonthHeader(
    month: YearMonth,
    modifier: Modifier = Modifier
) {
    Text(
        text = month.format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
        ).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/**
 * Swipe-to-delete wrapper.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == DismissValue.DismissedToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        dismissContent = { content() }
    )
}

/**
 * Empty state when no activities.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aucune activité",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========================
// FILTER ENUM
// ========================

enum class ActivityFilter(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val predicate: (Activity) -> Boolean
) {
    ALL(
        label = "Tout",
        icon = null,
        predicate = { true }
    ),
    RUNNING(
        label = "Course",
        icon = Icons.Default.DirectionsRun,
        predicate = { it.type.isRunning }
    ),
    CYCLING(
        label = "Vélo",
        icon = Icons.Default.DirectionsBike,
        predicate = { it.type.isCycling }
    ),
    SWIMMING(
        label = "Natation",
        icon = Icons.Default.Pool,
        predicate = { it.type.isSwimming }
    )
}
