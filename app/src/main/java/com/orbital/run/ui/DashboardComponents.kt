
package com.orbital.run.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.orbital.run.ui.theme.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.font.FontStyle

// Helper for pace formatting
private fun formatPaceHelper(speedKmh: Double): String {
    if (speedKmh <= 0) return "--:--"
    val pace = 60 / speedKmh
    val min = pace.toInt()
    val sec = ((pace - min) * 60).toInt()
    return "%d:%02d".format(min, sec)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardSection(trainingPlan: com.orbital.run.logic.TrainingPlanResult) {
    val analysisVm: AnalysisViewModel = viewModel()
    val coachInsight by analysisVm.coachInsight.collectAsState()
    var selectedMetric by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        // CoachInsightSection(insight = coachInsight) // Already exists in AnalyticsScreen, so skipping duplication if I put it there.
        // Actually, AnalyticsScreen already has CoachInsightSection. I should probably merge them or just place the new stats *above* it.
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CircularGauge(title = "VMA", value = String.format("%.1f", trainingPlan.vma), unit = "km/h", percent = (trainingPlan.vma.toFloat()/22f), color = AirSecondary) {
                selectedMetric = "VMA"
            }
            CircularGauge(title = "VO2Max", value = String.format("%.0f", trainingPlan.vo2max), unit = "ml/kg/min", percent = (trainingPlan.vo2max.toFloat()/80f), color = AirPrimary) {
                selectedMetric = "VO2Max"
            }
            CircularGauge(title = "FCM", value = "${trainingPlan.fcm}", unit = "bpm", percent = (trainingPlan.fcm.toFloat()/220f), color = AirAccent) {
                selectedMetric = "FCM"
            }
        }
        
        Column {
            Text("Prédictions de Course", color = AirTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                trainingPlan.racePredictions.forEach { pred ->
                    RaceTimeCard(pred.distanceName, pred.formattedTime)
                }
            }
        }

        val pagerState = androidx.compose.foundation.pager.rememberPagerState { 3 }
        val scope = rememberCoroutineScope()

        TabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, contentColor = AirPrimary, indicator = { tabPositions ->
              Box(
                 Modifier
                     .fillMaxWidth()
                     .wrapContentSize(Alignment.BottomStart)
                     .offset(x = tabPositions[pagerState.currentPage].left)
                     .width(tabPositions[pagerState.currentPage].width)
                     .height(3.dp)
                     .background(AirPrimary)
             )
        }) {
            Tab(selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0) } }, text = { Text("Cardio", fontSize = 11.sp, maxLines = 1, softWrap = false) })
            Tab(selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1) } }, text = { Text("Allure", fontSize = 11.sp, maxLines = 1, softWrap = false) })
            Tab(selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.animateScrollToPage(2) } }, text = { Text("Puissance", fontSize = 11.sp, maxLines = 1, softWrap = false) })
        }
        
        androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.height(280.dp), verticalAlignment = Alignment.Top) { page ->
             Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                  when(page) {
                    0 -> trainingPlan.hrZones.forEach { ZoneBar(it.label, "${it.min}-${it.max}", it.max.toFloat()/trainingPlan.fcm, getZoneColor(it.id)) }
                    1 -> trainingPlan.speedZones.forEach { 
                         ZoneBar("Z${it.id}", "${formatPaceHelper(it.maxSpeedKmh)} - ${formatPaceHelper(it.minSpeedKmh)} /km", it.id/5f, getZoneColor(it.id)) 
                    }
                    2 -> trainingPlan.powerZones.forEach { ZoneBar("Z${it.id}", "${it.minWatts}-${it.maxWatts}W", it.id/5f, getZoneColor(it.id)) }
                }
             }
        }
        

    }

    selectedMetric?.let { metric ->
        MetricExplanationDialog(metric) { selectedMetric = null }
    }
}

// Helpers needed (copied from MainScreen or assumed available/to be added)
@Composable
fun CircularGauge(title: String, value: String, unit: String, percent: Float, color: Color, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp).clickable(onClick = onClick)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(color = color.copy(alpha = 0.2f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawArc(color = color, startAngle = 135f, sweepAngle = 270f * percent.coerceIn(0f, 1f), useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = AirTextSecondary)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary)
            Text(unit, fontSize = 10.sp, color = AirTextSecondary)
        }
    }
}

@Composable
fun RaceTimeCard(dist: String, time: String) {
    Card(colors = CardDefaults.cardColors(containerColor = AirSurface), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dist, fontSize = 12.sp, color = AirTextSecondary)
            Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary)
        }
    }
}

@Composable
fun ZoneBar(title: String, range: String, fill: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary)
            Spacer(Modifier.weight(1f))
            Text(range, fontSize = 12.sp, color = AirTextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha=0.1f))) {
            Box(Modifier.fillMaxWidth(fill.coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
        }
    }
}

fun getZoneColor(zoneId: Int): Color {
    return when(zoneId) {
        1 -> Color(0xFF9E9E9E) // Gray (Recup)
        2 -> Color(0xFF03A9F4) // Blue (Endurance)
        3 -> Color(0xFF4CAF50) // Green (Tempo)
        4 -> Color(0xFFFFC107) // Yellow (Seuil)
        5 -> Color(0xFFFF5252) // Red (VO2Max)
        else -> Color.Gray
    }
}
