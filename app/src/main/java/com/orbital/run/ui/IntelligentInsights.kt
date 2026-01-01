package com.orbital.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.run.logic.*
import com.orbital.run.ui.theme.*

/**
 * Intelligent Insights Section
 * Shows fatigue detection, predictions, and recommendations
 */
@Composable
fun ExpertInsightsSection(
    activity: Persistence.CompletedActivity,
    history: List<Persistence.CompletedActivity>
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Analyses Algorithmiques ⚡",
            style = MaterialTheme.typography.titleMedium,
            color = AirTextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(12.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                
                // === FATIGUE DETECTION ===
                if (activity.heartRateSamples.isNotEmpty() && activity.splits.isNotEmpty()) {
                    val hrDrift = AnalysisEngine.calculateHRDriftPrecise(activity.heartRateSamples)
                    val paceAnalysis = AdvancedAnalytics.analyzePaceConsistency(
                        activity.splits,
                        activity.elevationGain
                    )
                    val aerobicDecoupling = AdvancedAnalytics.calculateAerobicDecoupling(
                        activity.heartRateSamples,
                        activity.splits
                    )
                    
                    val fatigueAnalysis = AdvancedAnalytics.detectFatigue(
                        hrDrift,
                        paceAnalysis.cv,
                        aerobicDecoupling
                    )
                    
                    FatigueIndicator(fatigueAnalysis)
                    
                    if (fatigueAnalysis.indicators.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        fatigueAnalysis.indicators.forEach { indicator ->
                            Text(
                                "• $indicator",
                                fontSize = 11.sp,
                                color = AirTextSecondary
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Divider(color = AirSurface, thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                }
                
                // === PACE ANALYSIS ===
                if (activity.splits.isNotEmpty()) {
                    val paceAnalysis = AdvancedAnalytics.analyzePaceConsistency(
                        activity.splits,
                        activity.elevationGain
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Speed,
                            null,
                            tint = ZoneBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Analyse Allure", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Régularité", fontSize = 11.sp, color = AirTextLight)
                            Text(
                                paceAnalysis.consistency.split(" - ")[0],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    paceAnalysis.cv < 5 -> ZoneGreen
                                    paceAnalysis.cv < 10 -> ZoneBlue
                                    else -> ZoneOrange
                                }
                            )
                        }
                        
                        if (paceAnalysis.negativeSplit) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Negative Split", fontSize = 11.sp, color = AirTextLight)
                                Text(
                                    "✅ ${paceAnalysis.splitDiff.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ZoneGreen
                                )
                            }
                        }
                        
                        if (paceAnalysis.gap != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GAP", fontSize = 11.sp, color = AirTextLight)
                                Text(
                                    formatPaceHelper(paceAnalysis.gap),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Divider(color = AirSurface, thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                }
                
                // === CADENCE ANALYSIS ===
                if (activity.avgCadence != null && activity.avgCadence > 0) {
                    val cadenceAnalysis = AdvancedAnalytics.analyzeCadence(
                        activity.avgCadence,
                        activity.type
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.DirectionsRun,
                            null,
                            tint = ZoneGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cadence", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        cadenceAnalysis.recommendation,
                        fontSize = 12.sp,
                        color = AirTextSecondary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider(color = AirSurface, thickness = 1.dp)
                    Spacer(Modifier.height(16.dp))
                }
                
                // === PERFORMANCE PREDICTION ===
                if (history.size >= 3) {
                    val prediction = AdvancedAnalytics.predictPerformance(
                        history,
                        activity.distanceKm,
                        activity.type
                    )
                    
                    if (prediction.estimatedTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.TrendingUp,
                                null,
                                tint = AirPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Prédiction Performance", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        val currentTime = activity.durationMin
                        val predictedTime = prediction.estimatedTime.toInt()
                        val diff = currentTime - predictedTime
                        
                        Text(
                            "Temps prédit: ${formatDuration(predictedTime)} (confiance: ${prediction.confidence})",
                            fontSize = 12.sp,
                            color = AirTextSecondary
                        )
                        
                        if (diff != 0) {
                            val diffText = if (diff > 0) {
                                "⚠️ ${diff}min plus lent que prévu"
                            } else {
                                "🎉 ${-diff}min plus rapide que prévu !"
                            }
                            
                            Text(
                                diffText,
                                fontSize = 12.sp,
                                color = if (diff > 0) ZoneOrange else ZoneGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fatigue Level Indicator
 */
@Composable
fun FatigueIndicator(analysis: FatigueAnalysis) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when(analysis.level) {
                FatigueLevel.LOW -> Icons.Rounded.CheckCircle
                FatigueLevel.MODERATE -> Icons.Rounded.Warning
                FatigueLevel.HIGH -> Icons.Rounded.Error
            },
            null,
            tint = when(analysis.level) {
                FatigueLevel.LOW -> ZoneGreen
                FatigueLevel.MODERATE -> ZoneOrange
                FatigueLevel.HIGH -> ZoneRed
            },
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Column {
            Text(
                "Niveau de Fatigue",
                fontSize = 11.sp,
                color = AirTextLight
            )
            Text(
                when(analysis.level) {
                    FatigueLevel.LOW -> "Faible - Bonne récupération"
                    FatigueLevel.MODERATE -> "Modéré - Surveillez la récupération"
                    FatigueLevel.HIGH -> "Élevé - Repos recommandé"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when(analysis.level) {
                    FatigueLevel.LOW -> ZoneGreen
                    FatigueLevel.MODERATE -> ZoneOrange
                    FatigueLevel.HIGH -> ZoneRed
                }
            )
        }
    }
}

/**
 * Helper: Format pace
 */
private fun formatPaceHelper(minPerKm: Double): String {
    val min = minPerKm.toInt()
    val sec = ((minPerKm - min) * 60).toInt()
    return "$min'${sec.toString().padStart(2, '0')}\""
}

/**
 * Helper: Format duration
 */
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h${mins}min" else "${mins}min"
}
