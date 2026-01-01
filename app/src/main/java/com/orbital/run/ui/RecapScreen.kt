package com.orbital.run.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import com.orbital.run.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orbital.run.logic.AdvancedAnalytics

@Composable
fun RecapScreen(context: android.content.Context, dataVersion: Int = 0, onNavigateToSettings: () -> Unit) {
    val analysisVm: AnalysisViewModel = viewModel()
    val globalSummary by analysisVm.globalSummary.collectAsState()
    
    var history by remember { mutableStateOf(Persistence.loadHistory(context)) }
    var prs by remember { mutableStateOf(Persistence.loadPersonalRecords(context)) }
    
    // Force Recalculate PRs on load or when data updates
    LaunchedEffect(dataVersion) {
        withContext(Dispatchers.IO) {
            history = Persistence.loadHistory(context)
            prs = Persistence.recalculateRecords(context)
        }
    }

    var selectedActivityId by remember { mutableStateOf<String?>(null) }

    if (selectedActivityId != null) {
        ActivityDetailScreen(
            activityId = selectedActivityId!!,
            context = context,
            onBack = { selectedActivityId = null }
        )
        return
    }

    // Filters
    var timeFilter by remember { mutableStateOf("Mois") } // Semaine, Mois, Année, Tout
    var sportFilter by remember { mutableStateOf("Tout") } // Tout, Run, Swim

    // Logic Calculation based on Filters
    // ... (Simplified for UI Demo)
    val filteredHistory = history.filter { 
        val matchesSport = when(sportFilter) {
            "Run" -> it.type == WorkoutType.RUNNING
            "Swim" -> it.type == WorkoutType.SWIMMING
            else -> true
        }
        val cal = Calendar.getInstance()
        val matchesTime = when(timeFilter) {
            "Semaine" -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                it.date >= cal.timeInMillis
            }
            "Mois" -> {
                cal.add(Calendar.MONTH, -1)
                it.date >= cal.timeInMillis
            }
            else -> true
        }
        matchesSport && matchesTime
    }

    val totalDist = filteredHistory.sumOf { it.distanceKm }
    val totalTime = filteredHistory.sumOf { it.durationMin }
    val totalElev = filteredHistory.sumOf { it.elevationGain ?: 0 }

    Scaffold(
        containerColor = AirSurface,
        topBar = {
            Column(Modifier.background(AirSurface).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bilan Global", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary)
                        Text("Votre tableau de bord de carrière", fontSize = 14.sp, color = AirTextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, null, tint = AirTextPrimary)
                    }
                }
                
                // Filters Row
                Spacer(Modifier.height(16.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashFilterChip(selected = timeFilter == "Semaine", onClick = { timeFilter = "Semaine" }, label = "Semaine")
                    DashFilterChip(selected = timeFilter == "Mois", onClick = { timeFilter = "Mois" }, label = "Mois")
                    DashFilterChip(selected = timeFilter == "Tout", onClick = { timeFilter = "Tout" }, label = "Tout")
                    Spacer(Modifier.width(8.dp))
                    DashFilterChip(selected = sportFilter == "Tout", onClick = { sportFilter = "Tout" }, label = "Tout")
                    DashFilterChip(selected = sportFilter == "Run", onClick = { sportFilter = "Run" }, label = "🏃")
                    DashFilterChip(selected = sportFilter == "Swim", onClick = { sportFilter = "Swim" }, label = "🏊")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 0. GLOBAL STATS (From Analysis)
            item { GlobalStatsSection(globalSummary) }

            // 1. WEEKLY / MONTHLY FOCUS
            item {
                SectionHeader("Activité Récente")
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            // Circular Gauge
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                                CircularProgressIndicator(
                                    progress = (totalDist / 50.0).coerceIn(0.0, 1.0).toFloat(), // Mock Goal 50km
                                    modifier = Modifier.fillMaxSize(),
                                    color = AirPrimary,
                                    strokeWidth = 8.dp
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${String.format("%.1f", totalDist)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text("km", fontSize = 12.sp, color = AirTextSecondary)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                StatRow("Durée", "${totalTime / 60}h ${totalTime % 60}m")
                                StatRow("Dénivelé", "${totalElev} m")
                                Spacer(Modifier.height(8.dp))
                                Text("▲ 10% vs période préc.", color = ZoneGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }


            item {
                SectionHeader("Records Personnels 🏆")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    fun navigateToActivity(id: String) {
                        if (id.isNotEmpty()) {
                            selectedActivityId = id
                        }
                    }

                    prs.best1k?.let { r -> item { RecordBadge("1 km", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best5k?.let { r -> item { RecordBadge("5 km", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best10k?.let { r -> item { RecordBadge("10 km", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), onClick = { navigateToActivity(r.activityId) }) } }
                    prs.bestHalf?.let { r -> item { RecordBadge("Semi", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), onClick = { navigateToActivity(r.activityId) }) } }
                    prs.bestMarathon?.let { r -> item { RecordBadge("Marathon", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), onClick = { navigateToActivity(r.activityId) }) } }
                    prs.longestRunKm?.let { r -> item { RecordBadge("Plus long", String.format("%.1f km", r.distance), onClick = { navigateToActivity(r.activityId) }) } }
                    
                    // Swim
                    prs.best100mSwim?.let { r -> item { RecordBadge("100m Nat", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best200mSwim?.let { r -> item { RecordBadge("200m Nat", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best400mSwim?.let { r -> item { RecordBadge("400m Nat", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best800mSwim?.let { r -> item { RecordBadge("800m Nat", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                    prs.best1500mSwim?.let { r -> item { RecordBadge("1500m Nat", formatDurationRefined((r.duration / 60).toInt(), (r.duration % 60).toInt()), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                    prs.longestSwimDist?.let { r -> item { RecordBadge("Plus longue nage", String.format("%.1f km", r.distance), color = AirSecondary, onClick = { navigateToActivity(r.activityId) }) } }
                }
            }

            // 4. HEATMAP
            item {
                SectionHeader("Assiduité")
                Text("30 derniers jours", fontSize = 12.sp, color = AirTextSecondary)
                Spacer(Modifier.height(8.dp))
                
                // Real Heatmap Calculation
                val today = System.currentTimeMillis()
                val dayMillis = 24 * 3600 * 1000L
                val last30Days = (0..29).map { i ->
                    val date = today - (29 - i) * dayMillis
                    val cal1 = Calendar.getInstance().apply { timeInMillis = date }
                    
                    val hasActivity = history.any { 
                         val cal2 = Calendar.getInstance().apply { timeInMillis = it.date }
                         cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                    }
                    hasActivity
                }
                
                // Calculate Streak
                var streak = 0
                val checkCal = Calendar.getInstance()
                // Check yesterday backwards
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
                
                // If today has activity, streak starts at 1, loop back
                // Simple logic:
                var currentStreak = 0
                val todayCal = Calendar.getInstance()
                var hasToday = history.any { 
                     val c = Calendar.getInstance().apply { timeInMillis = it.date }
                     c.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                }
                if (hasToday) currentStreak++
                
                for (i in 1..365) {
                    val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val active = history.any { 
                         val c = Calendar.getInstance().apply { timeInMillis = it.date }
                         c.get(Calendar.YEAR) == d.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
                    }
                    if (active) currentStreak++ else break
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    last30Days.forEach { active ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) AirPrimary else AirSurface)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("🔥 Série en cours : $currentStreak jours", fontWeight = FontWeight.Bold, color = AirTextPrimary)
            }

            // 5. DISTRIBUTION
            item {
                SectionHeader("Répartition Sport")
                
                val runDist = filteredHistory.filter { it.type == WorkoutType.RUNNING }.sumOf { it.distanceKm }
                val swimDist = filteredHistory.filter { it.type == WorkoutType.SWIMMING }.sumOf { it.distanceKm }
                val totalDistSum = (runDist + swimDist).coerceAtLeast(0.001) // Avoid div by zero
                
                val runPct = (runDist / totalDistSum).toFloat()
                val swimPct = (swimDist / totalDistSum).toFloat()
                
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Dynamic Donut Canvas
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val strokeWidth = 20f
                        // Info: DrawArc angles start from 3 o'clock (0deg). -90 is 12 o'clock.
                        
                        // Background
                        drawArc(AirSurface, 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
                        
                        // Run Arc (Primary)
                        if (runPct > 0) {
                            drawArc(AirPrimary, -90f, 360f * runPct, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
                        }
                        
                        // Swim Arc (Secondary)
                        if (swimPct > 0) {
                            drawArc(AirSecondary, -90f + (360f * runPct), 360f * swimPct, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        val runStr = "%.0f%%".format(runPct * 100)
                        val swimStr = "%.0f%%".format(swimPct * 100)
                        
                        RecapLegendItem("Running", runStr, AirPrimary)
                        RecapLegendItem("Natation", swimStr, AirSecondary)
                    }
                }
            }

            
            // 7. TOTALS
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AirPrimary), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ODOMÈTRE TOTAL", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp, letterSpacing = 2.sp)
                        Text("${String.format("%.0f", history.sumOf { it.distanceKm })} km", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${history.size} Activités", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// COMPONENTS

// COMPONENTS

@Composable
fun GlobalStatsSection(summary: AdvancedAnalytics.GlobalSummary?) {
    if (summary == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AirSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Bilan Global", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AirTextPrimary)
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlobalStatItem("Activités", "${summary.count}")
                GlobalStatItem("Distance", "${summary.totalDistanceKm.toInt()} km")
                GlobalStatItem("Heures", "${summary.totalDurationH.toInt()} h")
            }
            Spacer(Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha=0.5f))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlobalStatItem("D+ Total", "${summary.totalElevationM} m")
                GlobalStatItem("Eddington", "${summary.eddingtonNumber}")
                GlobalStatItem("Série Max", "${summary.maxStreakDays} j")
            }
        }
    }
}

@Composable
fun GlobalStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AirPrimary)
        Text(label, fontSize = 19.sp, color = AirTextSecondary, fontWeight = FontWeight.Medium) // Adjusted font size
    }
}

@Composable
fun DashFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        color = if (selected) AirPrimary else Color.Transparent,
        shape = RoundedCornerShape(50),
        border = if (!selected) BorderStroke(1.dp, AirTextLight) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label, 
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) Color.White else AirTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
fun StatRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(AirPrimary, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, color = AirTextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
        Text(value, color = AirTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordBadge(title: String, value: String, color: Color = Color(0xFFFFD700), onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, color = AirTextSecondary, maxLines = 1, textAlign = TextAlign.Center)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun RecapLegendItem(label: String, pct: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text("$label ($pct)", fontSize = 12.sp, color = AirTextSecondary)
    }
}

