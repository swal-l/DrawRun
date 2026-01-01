package com.orbital.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.clip
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.*
import com.orbital.run.api.SyncManager
import com.orbital.run.api.StravaAPI
import com.orbital.run.api.HealthConnectManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import androidx.compose.ui.graphics.Brush


@Composable
fun AnalyticsScreen(
    context: android.content.Context, 
    @Suppress("UNUSED_PARAMETER") onNavigateToRecap: () -> Unit,
    trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null
) {
    val analysisVm: AnalysisViewModel = viewModel()
    val pmcPoints by analysisVm.pmcPoints.collectAsState()
    val sportBreakdown by analysisVm.sportBreakdown.collectAsState()
    val coachInsight by analysisVm.coachInsight.collectAsState()

    var history by remember { mutableStateOf(Persistence.loadHistory(context).sortedByDescending { it.date }) }
    var isSyncing by remember { mutableStateOf(false) }
    var selectedActivityId by remember { mutableStateOf<String?>(null) }
    var selectedMetric by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    if (selectedActivityId != null) {
        ActivityDetailScreen(
            activityId = selectedActivityId!!,
            context = context,
            onBack = { selectedActivityId = null },
            trainingPlan = trainingPlan
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    coroutineScope.launch {
                        if (isSyncing) return@launch
                        isSyncing = true
                        
                        try {
                            // ✅ CRITICAL: Load token before checking status
                            StravaAPI.loadToken(context)
                            
                            val count = try {
                                SyncManager.syncAll(context)
                            } catch (e: Exception) { 
                                e.printStackTrace()
                                0
                            }
                            
                            val message = when {
                                count > 0 -> "✅ $count nouvelles activités synchronisées !"
                                !StravaAPI.isAuthenticated() && !HealthConnectManager.hasAllPermissionsSync(context) -> "⚠️ Connectez Strava ou Health Connect"
                                !StravaAPI.isAuthenticated() -> "⚠️ Strava non connecté"
                                !HealthConnectManager.hasAllPermissionsSync(context) -> "⚠️ Health Connect non autorisé"
                                else -> "ℹ️ Déjà à jour - 0 nouvelles"
                            }
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            
                            // ✅ Reload history AND recalculate records after sync
                            val newHistory = withContext(Dispatchers.IO) { Persistence.loadHistory(context) }
                            history = newHistory.sortedByDescending { it.date }
                            
                            // ✅ CRITICAL: Recalculate personal records after sync
                            withContext(Dispatchers.IO) {
                                Persistence.recalculateRecords(context)
                            }
                            
                            analysisVm.refresh()
                        } finally {
                            isSyncing = false
                        }
                    }
                },
                containerColor = AirPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.Sync, "Sync")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("Vos tendances de forme et santé synthétisées", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp), color = AirTextPrimary)
            }



            item { PerformanceTrendSection(pmcPoints) { selectedMetric = it } }
            item { SportBreakdownSection(sportBreakdown) }

            // HISTORY SECTION
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Historique", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AirTextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("${history.size} séances", fontSize = 12.sp, color = AirTextLight)
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    // FILTERS
                    var selectedFilter by remember { mutableStateOf("Tout") }
                
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tout", "Course", "Natation").forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                color = if (isSelected) AirPrimary else Color.Transparent,
                                shape = RoundedCornerShape(50),
                                border = if (!isSelected) BorderStroke(1.dp, AirTextLight) else null,
                                modifier = Modifier.clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = filter, 
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) Color.White else AirTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // GROUPING LOGIC
                    val filteredList = history.filter { 
                        when(selectedFilter) {
                            "Course" -> it.type == WorkoutType.RUNNING
                            "Natation" -> it.type == WorkoutType.SWIMMING
                            else -> true
                        }
                    }
                    
                    val groupedHistory = remember(filteredList) {
                        filteredList.groupBy { activity ->
                            // Calculate week start date
                            val calendar = Calendar.getInstance(Locale.FRANCE)
                            calendar.timeInMillis = activity.date
                            calendar.firstDayOfWeek = Calendar.MONDAY
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.clear(Calendar.MINUTE)
                            calendar.clear(Calendar.SECOND)
                            calendar.clear(Calendar.MILLISECOND)
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            val startOfWeek = calendar.time
                            
                            calendar.add(Calendar.DAY_OF_YEAR, 6)
                            val endOfWeek = calendar.time
                            
                            val fmt = SimpleDateFormat("dd MMM", Locale.FRANCE)
                            "Semaine du ${fmt.format(startOfWeek)} au ${fmt.format(endOfWeek)}"
                        }
                    }

                    // RENDER GROUPED LIST
                   groupedHistory.forEach { (weekLabel, activities) ->
                       // Week Header
                       Text(
                           weekLabel.replaceFirstChar { it.uppercase() }, 
                           color = AirTextSecondary, 
                           fontSize = 13.sp, 
                           fontWeight = FontWeight.Bold,
                           modifier = Modifier.padding(vertical = 8.dp)
                       )
                       
                       activities.forEach { activity ->
                           ActivityRow(activity = activity, onClick = { selectedActivityId = activity.id })
                           Spacer(Modifier.height(8.dp))
                       }
                       Spacer(Modifier.height(12.dp))
                   }
                   
                   if (filteredList.isEmpty()) {
                       Text("Aucune activité trouvée pour ce filtre.", color = AirTextLight, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                   }
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    selectedMetric?.let { metric ->
        MetricExplanationDialog(metric) { selectedMetric = null }
    }
}

@Composable
fun CoachInsightSection(insight: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(AirPrimary, Color(0xFF673AB7), Color(0xFFE91E63)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    insight,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )
            }
        }
    }
}



@Composable
fun PerformanceTrendSection(points: List<AdvancedAnalytics.PMCPoint>, onMetricClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AirSurface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Analytics, null, tint = AirPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gestion de la Performance (PMC)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AirTextPrimary)
            }
            Text("Analyse de la charge d'entraînement (Banister Model)", fontSize = 12.sp, color = AirTextLight)
            
            Spacer(Modifier.height(20.dp))
            
            if (points.isNotEmpty()) {
                val currentP = points.last()
                
                // 1. TOP METRICS ROW
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PMCMetricCard("Condition", "${currentP.ctl.toInt()}", "CTL", Color(0xFF00B0FF), Modifier.weight(1f)) {
                        onMetricClick("CTL")
                    }
                    PMCMetricCard("Fatigue", "${currentP.atl.toInt()}", "ATL", Color(0xFFFF5252), Modifier.weight(1f)) {
                        onMetricClick("ATL")
                    }
                    
                    val tsbColor = when {
                        currentP.tsb > 5 -> Color(0xFF00E676) // Z2 Green
                        currentP.tsb < -20 -> Color(0xFFFF5252) // Z5 Red
                        else -> Color(0xFFFFEA00) // Z3 Yellow
                    }
                    PMCMetricCard("Forme", "${currentP.tsb.toInt()}", "TSB", tsbColor, Modifier.weight(1f)) {
                        onMetricClick("TSB")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 2. THE CHART
                val (fitnessEntries, fatigueEntries, formEntries) = remember(points) {
                    Triple(
                        points.mapIndexed { i, p -> com.patrykandpatrick.vico.core.entry.FloatEntry(i.toFloat(), p.ctl.toFloat()) },
                        points.mapIndexed { i, p -> com.patrykandpatrick.vico.core.entry.FloatEntry(i.toFloat(), p.atl.toFloat()) },
                        points.mapIndexed { i, p -> com.patrykandpatrick.vico.core.entry.FloatEntry(i.toFloat(), p.tsb.toFloat()) }
                    )
                }
                
                Chart(
                    chart = lineChart(
                        lines = listOf(
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFF00B0FF), lineThickness = 3.dp),
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFFFF5252), lineThickness = 2.dp),
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFFFFEA00).copy(alpha = 0.4f), lineThickness = 1.dp)
                        )
                    ),
                    chartModelProducer = remember(fitnessEntries, fatigueEntries, formEntries) {
                        com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer(listOf(fitnessEntries, fatigueEntries, formEntries))
                    },
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _ ->
                            "${value.toInt()}" 
                        }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            val index = value.toInt()
                            val point = points.getOrNull(index)
                            if (point != null) {
                                SimpleDateFormat("dd/MM", Locale.FRANCE).format(Date(point.date))
                            } else ""
                        }
                    ),
                    modifier = Modifier.height(180.dp),
                    chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End)
                )

                Spacer(Modifier.height(20.dp))
                
                // 3. INSIGHT BOX
                val (trendText, statusColor) = when {
                    currentP.tsb > 15 -> "Phase de Freshness : Idéal pour une course." to Color(0xFF00E676)
                    currentP.tsb > 5 -> "Phase d'affûtage : Votre corps récupère bien." to Color(0xFF00E676)
                    currentP.tsb < -30 -> "Surcharge Critique : Risque élevé de blessure, repos obligatoire." to Color(0xFFFF5252)
                    currentP.tsb < -10 -> "Phase de Charge : Votre forme s'améliore mais la fatigue monte." to Color(0xFFFF9100)
                    else -> "Phase de Maintien : Équilibre entre charge et fatigue." to Color(0xFF00B0FF)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = trendText,
                        fontSize = 12.sp,
                        color = AirTextSecondary,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

            } else {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Données insuffisantes pour l'analyse PMC", color = AirTextLight, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PMCMetricCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AirTextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 10.sp, color = AirTextLight, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
fun AnalyticsLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = AirTextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SportBreakdownSection(breakdown: Map<WorkoutType, Double>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AirSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Volume par Sport", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AirTextPrimary)
            Spacer(Modifier.height(20.dp))
            val total = breakdown.values.sum().coerceAtLeast(1.0)
            breakdown.forEach { (type, dist) ->
                val percent = (dist / total).toFloat()
                Column(Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if(type == WorkoutType.SWIMMING) "🏊 Natation" else "🏃 Course", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(String.format("%.1f km", dist), fontSize = 13.sp, color = AirTextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = percent,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if(type == WorkoutType.SWIMMING) AirSecondary else AirPrimary,
                        trackColor = Color.White
                    )
                }
            }
        }
    }
}


@Composable
fun ActivityRow(activity: Persistence.CompletedActivity, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AirSurface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier.size(48.dp).background(if(activity.type == WorkoutType.SWIMMING) AirSecondary.copy(alpha=0.1f) else AirPrimary.copy(alpha=0.1f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(if(activity.type == WorkoutType.SWIMMING) "🏊" else "🏃", fontSize = 22.sp) }
                
                // Mock Draw Badge (Mocking logic based on ID hash for demo)
                val mockDraws = (activity.id.hashCode() % 20).takeIf { it > 0 }
                if (mockDraws != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(Color(0xFFFFD700), CircleShape) // Gold
                            .border(2.dp, Color.White, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.Black, modifier = Modifier.size(8.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("$mockDraws", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, fontWeight = FontWeight.Bold, color = AirTextPrimary, fontSize = 15.sp)
                Text(SimpleDateFormat("EEEE dd MMMM", Locale.FRANCE).format(Date(activity.date)).replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = AirTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${String.format("%.1f", activity.distanceKm)} km", fontWeight = FontWeight.Black, color = AirPrimary, fontSize = 16.sp)
                Text("${activity.durationMin} min", fontSize = 12.sp, color = AirTextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }


}

