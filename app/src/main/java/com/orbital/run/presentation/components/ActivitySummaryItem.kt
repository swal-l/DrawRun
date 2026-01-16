package com.orbital.run.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbital.run.domain.models.Activity
import com.orbital.run.domain.models.ActivityType
import com.orbital.run.ui.theme.DataMedium
import com.orbital.run.ui.theme.DataSmall
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * Activity summary item for lists and recent activity displays.
 *
 * Features:
 * - Strong visual hierarchy (date → title → metrics)
 * - Sport-specific icon with color
 * - Clean, scannable layout
 * - Tap to view details
 *
 * @param activity Activity to display
 * @param onClick Click handler for navigation to detail
 * @param showDate Whether to show completion date (default true)
 */
@Composable
fun ActivitySummaryItem(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp  // Flat design
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport icon with colored background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getActivityColor(activity.type).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActivityIcon(activity.type),
                    contentDescription = null,
                    tint = getActivityColor(activity.type),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date (if shown)
                if (showDate) {
                    Text(
                        text = formatActivityDate(activity),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Title
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                // Metrics row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    MetricChip(
                        label = formatDistance(activity.distance.kilometers),
                        icon = Icons.Default.DirectionsRun
                    )
                    
                    // Duration
                    MetricChip(
                        label = formatDuration(activity.duration.seconds),
                        icon = Icons.Default.Schedule
                    )
                    
                    // Heart rate (if available)
                    activity.averageHeartRate?.let { hr ->
                        MetricChip(
                            label = "$hr bpm",
                            icon = Icons.Default.Favorite
                        )
                    }
                }
            }
            
            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Small metric chip for activity summary.
 */
@Composable
private fun MetricChip(
    label: String,
    icon: ImageVector
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ========================
// HELPER FUNCTIONS
// ========================

/**
 * Get icon for activity type.
 */
private fun getActivityIcon(type: ActivityType): ImageVector {
    return when {
        type.isRunning -> Icons.Default.DirectionsRun
        type.isSwimming -> Icons.Default.Pool
        type.isCycling -> Icons.Default.DirectionsBike
        else -> Icons.Default.FitnessCenter
    }
}

/**
 * Get color for activity type.
 */
@Composable
private fun getActivityColor(type: ActivityType): androidx.compose.ui.graphics.Color {
    return when {
        type.isRunning -> MaterialTheme.colorScheme.primary
        type.isSwimming -> MaterialTheme.colorScheme.secondary
        type.isCycling -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Format activity date relative to today.
 */
private fun formatActivityDate(activity: Activity): String {
    val now = java.time.LocalDate.now()
    val activityDate = activity.completedAt
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(activityDate, now)
    
    return when (daysBetween.toInt()) {
        0 -> "Aujourd'hui"
        1 -> "Hier"
        in 2..6 -> "${daysBetween.toInt()} jours"
        else -> activityDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

/**
 * Format distance with appropriate unit.
 */
private fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).roundToInt()} m"
    } else {
        String.format("%.1f km", km)
    }
}

/**
 * Format duration as HH:MM or MM:SS.
 */
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d h", hours, minutes)
    } else {
        String.format("%d:%02d min", minutes, secs)
    }
}
