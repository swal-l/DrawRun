package com.orbital.run.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.logic.SwimPhases
import com.orbital.run.logic.SwimPhaseFraction
import com.orbital.run.logic.LongitudinalMetrics
import com.orbital.run.logic.WPrimeBalance
import com.orbital.run.logic.QuadrantPoint
import com.orbital.run.logic.AeroLabResult
import com.orbital.run.logic.ProInterval
import com.orbital.run.logic.WorkoutType
import com.orbital.run.logic.Persistence
import com.orbital.run.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.ChartEntry
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.*

data class ChartZoneBand(
    val minRatio: Float, // 0..1 (from bottom)
    val maxRatio: Float, // 0..1 (from bottom)
    val color: Color
)

@Composable
fun StreamChartContainer(title: String, color: Color, content: @Composable (Boolean) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AirSurface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AirTextSecondary)
                Spacer(Modifier.weight(1f))
                Icon(if (isExpanded) Icons.Default.Fullscreen else Icons.Default.Fullscreen, null, modifier = Modifier.size(14.dp), tint = AirTextLight)
            }
            Spacer(Modifier.height(12.dp))
            content(isExpanded)
        }
    }
}

class ChartSyncState {
    var zoomScale by mutableStateOf(1f)
    var zoomOffset by mutableStateOf(0f)
    var scrubX by mutableStateOf<Float?>(null)
}

@Composable
fun rememberChartSyncState(): ChartSyncState {
    return remember { ChartSyncState() }
}

@Composable
fun StreamCanvas(
    height: androidx.compose.ui.unit.Dp, 
    @Suppress("UNUSED_PARAMETER") _dataSize: Int, 
    yLabels: List<Pair<String, Color>> = emptyList(),
    avgLineY: Float? = null,
    avgLabel: String? = null,
    avgColor: Color = Color.Gray,
    state: ChartSyncState = rememberChartSyncState(), // Default for standalone
    scrubValue: ((Float, Float) -> String?)? = null,
    backgroundBands: List<ChartZoneBand> = emptyList(),
    onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float, Float, Float) -> Unit
) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
    
    Canvas(
        modifier = Modifier.fillMaxWidth().height(height)
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    state.zoomScale = (state.zoomScale * zoom).coerceIn(1f, 10f)
                    
                    if (state.zoomScale > 1f) {
                        // Adjust offset to keep zoom centered or follow pan
                        state.zoomOffset = (state.zoomOffset + pan.x).coerceIn(size.width * (1f - state.zoomScale), 0f)
                    } else {
                        state.zoomOffset = 0f
                    }
                }
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = { state.scrubX = (it.x - state.zoomOffset) / state.zoomScale },
                    onDragEnd = { state.scrubX = null },
                    onDragCancel = { state.scrubX = null },
                    onDrag = { change, _ -> 
                        state.scrubX = (change.position.x - state.zoomOffset) / state.zoomScale 
                    }
                )
            }
            .pointerInput(state) {
                detectTapGestures(
                    onDoubleTap = {
                        state.zoomScale = 1f
                        state.zoomOffset = 0f
                        state.scrubX = null
                    },
                    onTap = { state.scrubX = (it.x - state.zoomOffset) / state.zoomScale }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        
        // Background Bands
        backgroundBands.forEach { band ->
            val top = h - (band.maxRatio * h)
            val bottom = h - (band.minRatio * h)
            drawRect(
                color = band.color.copy(alpha = 0.08f),
                topLeft = Offset(0f, top),
                size = androidx.compose.ui.geometry.Size(w, (bottom - top).coerceAtLeast(0f))
            )
        }

        // Premium Grid
        for (i in 0..3) {
            val yLine = (i / 3f) * h
            drawLine(Color(0xFFE0E0E0).copy(alpha=0.3f), Offset(0f, yLine), Offset(w, yLine), 1.dp.toPx())
        }
        
        // Average Line (static Y, but drawn over content)
        // ...
        
        // Average Line
        avgLineY?.let { yRatio ->
            val y = yRatio * h
            drawLine(
                color = avgColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            avgLabel?.let { label ->
                val measure = textMeasurer.measure(label, labelStyle)
                drawRect(
                    color = Color.White.copy(alpha = 0.8f),
                    topLeft = Offset(4.dp.toPx(), y - measure.size.height / 2f),
                    size = androidx.compose.ui.geometry.Size(measure.size.width + 4.dp.toPx(), measure.size.height.toFloat())
                )
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        label,
                        6.dp.toPx(),
                        y + measure.size.height * 0.3f,
                        android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb((avgColor.alpha * 255).toInt(), (avgColor.red * 255).toInt(), (avgColor.green * 255).toInt(), (avgColor.blue * 255).toInt())
                            this.textSize = 8.sp.toPx()
                            this.isFakeBoldText = true
                        }
                    )
                }
            }
        }
        
        onDraw(w, h, state.zoomScale, state.zoomOffset)
        
        // Draw Y Labels if provided
        yLabels.forEachIndexed { index, pair ->
            val text = pair.first
            val color = pair.second
            val result = textMeasurer.measure(text, labelStyle)
            
            // Layout: Max at top, Min at bottom
            val yPos = if (index == 0) 0f else h - result.size.height
            
            drawRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(w - result.size.width - 4.dp.toPx(), yPos),
                size = androidx.compose.ui.geometry.Size(result.size.width + 4.dp.toPx(), result.size.height.toFloat())
            )
            
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    text,
                    w - result.size.width - 2.dp.toPx(),
                    yPos + result.size.height * 0.8f,
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                        this.textSize = 9.sp.toPx()
                        this.isFakeBoldText = true
                    }
                )
            }
        }
        
        // Draw Scrubber (if active)
        state.scrubX?.let { sx ->
            // Apply zoom to scrub position for drawing
            // sx is in data coordinates (0..w unzoomed).
            // Drawing position = sx * zoomScale + zoomOffset
            val drawX = sx * state.zoomScale + state.zoomOffset
            
            if (drawX in 0f..w) {
                drawLine(AirPrimary, Offset(drawX, 0f), Offset(drawX, h), 2.dp.toPx())
                drawCircle(Color.White, 4.dp.toPx(), Offset(drawX, h), style = Stroke(2.dp.toPx()))
                drawCircle(AirPrimary, 2.dp.toPx(), Offset(drawX, h))

                // Tooltip Floating
                scrubValue?.invoke(w, sx)?.let { valStr -> // Pass sx (data coordinate) not drawX?
                    // Previous logic: scrubValue = { width, _ -> ... code uses scrubX ... }
                    // Actually, scrubValue parameter 2 was ignored in previous `invoke(w, h)`. 
                    // Let's pass 'sx' as the second param in case it's needed, though the lambda usually uses the captured `scrubX`.
                    // Wait, previous call was `scrubValue?.invoke(w, h)`.
                    // The lambda in charts uses `scrubX` state.
                    // Now `scrubX` is in `state`. Charts need to access `state.scrubX`.
                    
                    val measure = textMeasurer.measure(valStr, labelStyle.copy(color = Color.White))
                    val tooltipX = if (drawX > w * 0.7f) drawX - measure.size.width - 12.dp.toPx() else drawX + 8.dp.toPx()
                    val tooltipY = 8.dp.toPx()
                    
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = androidx.compose.ui.geometry.Size(measure.size.width + 12.dp.toPx(), measure.size.height + 8.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            valStr,
                            tooltipX + 6.dp.toPx(),
                            tooltipY + measure.size.height * 0.8f + 2.dp.toPx(),
                            android.graphics.Paint().apply {
                                this.color = android.graphics.Color.WHITE
                                this.textSize = 10.sp.toPx()
                                this.isFakeBoldText = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegendIndicator(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = AirTextSecondary)
    }
}

/**
 * HR Zone Heatmap
 * Shows zone distribution over time
 */
@Composable
fun HRZoneHeatmap(
    samples: List<Persistence.HeartRateSample>,
    zones: Map<Int, Pair<Int, Int>>
) {
    if (samples.isEmpty()) return
    
    ChartContainer("Répartition Zones HR") { isExpanded ->
        val height = if (isExpanded) 120.dp else 40.dp
        
        Column(modifier = Modifier.fillMaxWidth()) {
            val segmentsCount = if (isExpanded) 40 else 10
            val segmentSize = samples.size / segmentsCount
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0 until segmentsCount) {
                    val start = i * segmentSize
                    val end = minOf((i + 1) * segmentSize, samples.size)
                    val segment = samples.subList(start, end)
                    
                    val dominantZone = segment.groupBy { sample ->
                        zones.entries.find { (_, range) -> sample.bpm in range.first..range.second }?.key ?: 0
                    }.maxByOrNull { it.value.size }?.key ?: 0
                    
                    val color = when(dominantZone) {
                        1 -> Color(0xFF00B0FF) // Recovery (Light Blue)
                        2 -> Color(0xFF00E676) // Aerobic (Green)
                        3 -> Color(0xFFFFEA00) // Tempo (Yellow)
                        4 -> Color(0xFFFF9100) // Threshold (Orange)
                        5 -> Color(0xFFFF5252) // Anaerobic (Red)
                        else -> Color(0xFFBDBDBD)
                    }
                    
                    Box(modifier = Modifier.weight(1f).height(height).padding(horizontal = 0.5.dp).background(color))
                }
            }
            
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Début", fontSize = 9.sp, color = AirTextLight)
                Text("Durée Totale", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AirTextPrimary)
                Text("Fin", fontSize = 9.sp, color = AirTextLight)
            }
        }
    }
}

/**
 * Explanation Card
 */
@Composable
fun MetricExplanation(
    title: String,
    description: String,
    target: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AirBackground.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AirAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AirTextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text(description, fontSize = 11.sp, color = AirTextLight)
            target?.let {
                Spacer(Modifier.height(4.dp))
                Text("Cible : $it", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AirAccent)
            }
        }
    }
}

/**
 * Container with Fullscreen support
 */
@Composable
fun ChartContainer(
    title: String,
    content: @Composable (isExpanded: Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        Dialog(onDismissRequest = { isExpanded = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = AirTextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.weight(1f)) {
                        content(true)
                    }
                    Button(
                        onClick = { isExpanded = false },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = AirAccent)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    modifier = Modifier.size(20.dp).clickable { isExpanded = true },
                    tint = AirSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
            content(false)
        }
    }
}

@Composable
private fun MetricSmall(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = AirTextLight)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}


/**
 * Divergent Bar Chart : Asymétrie GCT (V1.3 Part 9.1.C)
 * X: kmIndex, Bars: Left/Right dominance (%)
 */
@Composable
fun GCTAsymmetryChart(splits: List<Persistence.Split>) {
    if (splits.isEmpty()) return
    
    ChartContainer("Déséquilibre GCT (Gauche/Droite)") { isExpanded ->
        val chartHeight = if (isExpanded) 400.dp else 180.dp
        
        Column {
            Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val barHeight = (height / splits.size.coerceAtLeast(10)).coerceAtMost(if(isExpanded) 30.dp.toPx() else 20.dp.toPx())
                
                splits.forEachIndexed { i, _ ->
                    val y = i * (barHeight + 4.dp.toPx())
                    val balanceLeft = 50.0 + (kotlin.random.Random.nextDouble(-3.0, 3.0)) // Mocking
                    val diff = (balanceLeft - 50.0).toFloat()
                    
                    val barWidth = (kotlin.math.abs(diff) / 5.0f).coerceAtMost(1f) * (width / 2)
                    val color = if (kotlin.math.abs(diff) > 2.0) Color(0xFFFF5252) else Color(0xFF2979FF)
                    
                    val rectX = if (diff < 0) centerX else centerX - barWidth
                    drawRect(
                        color = color,
                        topLeft = Offset(rectX, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
                drawLine(Color.Gray.copy(alpha=0.5f), Offset(centerX, 0f), Offset(centerX, height), strokeWidth = 1.dp.toPx())
            }
            Spacer(Modifier.height(4.dp))
            Text("Gauche < Dominance > Droite | Alerte > 2% (Rouge)", fontSize = 10.sp, color = AirTextSecondary)
        }
    }
}

/**
 * Stacked Histogram : Phases Natation (V1.3 Part 9.2.A)
 * X: Lengths, Colors: Turn/Glide vs Swim
 */
@Composable
fun SwimPhasesChart(phases: SwimPhases) {
    val data = phases.lengthBreakdown
    if (data.isEmpty()) return
    
    ChartContainer("Décomposition des Phases (Natation)") { isExpanded ->
        val chartHeight = if (isExpanded) 250.dp else 120.dp
        // Determine max duration to scale bars
        val maxTotal = data.maxOfOrNull { it.turnGlideSec + it.swimSec } ?: 1.0
        
        Row(modifier = Modifier.fillMaxWidth().height(chartHeight), verticalAlignment = Alignment.Bottom) {
            data.take(if(isExpanded) 60 else 30).forEach { phase ->
                Column(modifier = Modifier.weight(1f).padding(horizontal = 1.dp), verticalArrangement = Arrangement.Bottom) {
                    // Calculate heights relative to charted area
                    val totalH = (phase.turnGlideSec + phase.swimSec) / maxTotal
                    val glideRatio = phase.turnGlideSec / (phase.turnGlideSec + phase.swimSec)
                    
                    val totalDp = chartHeight * totalH.toFloat()
                    val glideDp = totalDp * glideRatio.toFloat()
                    val swimDp = totalDp - glideDp

                    // Turn/Glide (Dark Blue)
                    Box(Modifier.fillMaxWidth().height(glideDp).background(Color(0xFF0D47A1)))
                    // Swim (Light Blue)
                    Box(Modifier.fillMaxWidth().height(swimDp).background(Color(0xFF42A5F5)))
                }
            }
        }
    }
}

@Composable
fun TSBComboChart(history: List<Persistence.CompletedActivity>) {
    val stats = remember(history) { AdvancedAnalytics.calculateLongitudinal(history) }
    
    ChartContainer("Dashboard Performance (PMC)") { isExpanded ->
        val chartHeight = if (isExpanded) 300.dp else 160.dp
        
        Column {
            val historyPoints = remember(history) {
                // Use daily PMC points, same as AnalyticsScreen
                AdvancedAnalytics.calculateDailyPMC(history).takeLast(30)
            }

            if (historyPoints.isNotEmpty()) {
                val fitnessEntries = historyPoints.mapIndexed { i, p -> entryOf(i.toFloat(), p.ctl.toFloat()) }
                val fatigueEntries = historyPoints.mapIndexed { i, p -> entryOf(i.toFloat(), p.atl.toFloat()) }
                val formEntries = historyPoints.mapIndexed { i, p -> entryOf(i.toFloat(), p.tsb.toFloat()) }

                Chart(
                    chart = lineChart(
                        lines = listOf(
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFF00B0FF), lineThickness = 3.dp), // CTL
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFFFF5252), lineThickness = 2.dp), // ATL
                            com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = Color(0xFF00E676).copy(alpha = 0.6f), lineThickness = 2.dp) // TSB
                        )
                    ),
                    model = remember(fitnessEntries, fatigueEntries, formEntries) {
                        ChartEntryModelProducer(listOf(fitnessEntries, fatigueEntries, formEntries)).getModel()
                    },
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier.fillMaxWidth().height(chartHeight),
                    chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End)
                )

                Spacer(Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ChartLegendItem("Condition", Color(0xFF00B0FF))
                    ChartLegendItem("Fatigue", Color(0xFFFF5252))
                    ChartLegendItem("Forme", Color(0xFF00E676))
                }
            } else {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Données insuffisantes", color = AirTextSecondary, fontSize = 12.sp)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text("État actuel: ${if(stats.form > 5) "Pic de Forme 🚀" else if(stats.form < -20) "Surentraînement ⚠️" else "Productif ✅"}", 
                fontWeight = FontWeight.Bold, color = if(stats.form > 5) Color(0xFF00E676) else if(stats.form < -20) Color(0xFFFF5252) else AirPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = AirTextSecondary)
    }
}

/**
 * Power Duration Curve (V1.3 Part 9)
 * Logarithmic line plot
 */
@Composable
fun PowerDurationCurve(curve: List<Pair<Int, Double>>, modeledCurve: List<Pair<Int, Double>> = emptyList(), state: ChartSyncState = rememberChartSyncState()) {
    if (curve.isEmpty()) return

    ChartContainer("Courbe de Puissance (CP)") { isExpanded ->
        val chartHeight = if (isExpanded) 350.dp else 180.dp
        
        Column {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(chartHeight)
                    .pointerInput(state) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            state.zoomScale = (state.zoomScale * zoom).coerceIn(1f, 10f)
                            if (state.zoomScale > 1f) {
                                state.zoomOffset = (state.zoomOffset + pan.x).coerceIn(size.width * (1f - state.zoomScale), 0f)
                            } else {
                                state.zoomOffset = 0f
                            }
                        }
                    }
                    .pointerInput(state) {
                        detectTapGestures(
                            onDoubleTap = {
                                state.zoomScale = 1f
                                state.zoomOffset = 0f
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                
                val maxWatts = (curve.maxOfOrNull { it.second } ?: 0.0).toFloat().coerceAtLeast(1f)
                val maxModeledWatts = (modeledCurve.maxOfOrNull { it.second } ?: 0.0).toFloat().coerceAtLeast(1f)
                val overallMaxWatts = maxOf(maxWatts, maxModeledWatts)

                val maxDuration = (curve.maxOfOrNull { it.first } ?: 0).toFloat().coerceAtLeast(1f)
                val maxModeledDuration = (modeledCurve.maxOfOrNull { it.first } ?: 0).toFloat().coerceAtLeast(1f)
                val overallMaxDuration = maxOf(maxDuration, maxModeledDuration).coerceAtLeast(1f)

                fun getX(duration: Int): Float {
                    // Logarithmic X Mapping
                    val minLog = kotlin.math.ln(1.0) // 1 sec
                    val maxLog = kotlin.math.ln(overallMaxDuration.toDouble() + 1)
                    val vLog = kotlin.math.ln(duration.toDouble() + 1)
                    val ratio = (vLog - minLog) / (maxLog - minLog)
                    return ((ratio * w) * state.zoomScale).toFloat() + state.zoomOffset
                }
                
                // 1. Modeled Curve (e.g., All-time Record)
                val modeledPath = Path()
                modeledCurve.sortedBy { it.first }.forEachIndexed { i, entry ->
                    val x = getX(entry.first)
                    val y = h - (entry.second.toFloat() / overallMaxWatts) * h
                    if (i == 0) modeledPath.moveTo(x, y) else modeledPath.lineTo(x, y)
                }
                drawPath(modeledPath, Color.LightGray.copy(alpha=0.6f), style = Stroke(width = 1.dp.toPx()))
                
                // 2. Current Session Curve
                val sessionPath = Path()
                curve.sortedBy { it.first }.forEachIndexed { i, entry ->
                    val x = getX(entry.first)
                    val y = h - (entry.second.toFloat() / overallMaxWatts) * h
                    if (i == 0) sessionPath.moveTo(x, y) else sessionPath.lineTo(x, y)
                }
                drawPath(sessionPath, AirAccent, style = Stroke(width = (if(isExpanded) 4.dp else 2.dp).toPx()))
                
                // AXES LABELS
                drawLine(Color.Gray.copy(0.2f), Offset(0f, 0f), Offset(0f, h), 1.dp.toPx())
                drawLine(Color.Gray.copy(0.2f), Offset(0f, h), Offset(w, h), 1.dp.toPx())
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1s (EXPLOSIF)", fontSize = 8.sp, color = AirTextLight)
                Text("DURÉE (LOG) →", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AirTextLight)
                Text("5h (ENDURANCE)", fontSize = 8.sp, color = AirTextLight)
            }
            Text("Axe Vertical: Puissance (Watts)", fontSize = 9.sp, color = AirTextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(4.dp))
            Text("L'axe X est logarithmique. Rose: Session | Gris: Record.", fontSize = 10.sp, color = AirTextSecondary)
        }
    }
}

/**
 * Swimming Hydrodynamics
 */
@Composable
fun HydrodynamicsPanel(activity: Persistence.CompletedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AirSurface.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Analyse Hydrodynamique", fontWeight = FontWeight.Bold, color = AirAccent)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricSmall("Stroke Index", String.format("%.2f", activity.strokeIndex ?: 0.0), Color.Blue)
                MetricSmall("SWOLF", activity.swolf?.toString() ?: "--", Color.Green)
                MetricSmall("Allure", run {
                    val spMps = (activity.distanceKm * 1000) / (activity.durationMin * 60)
                    if (spMps > 0.1) {
                         val totalSec = (1000.0 / (spMps * 60.0)) * 60.0
                         val min = (totalSec / 60).toInt()
                         val sec = (totalSec % 60).toInt()
                         String.format("%d:%02d min/km", min, sec)
                    } else "--:--"
                }, Color.Cyan)
            }
        }
    }
}
/**
 * W' Balance Chart (Skiba Model)
 * Displays Power overlaid with the anaerobic battery depletion.
 */
@Composable
fun WPrimeBalanceChart(balance: List<Double>, wPrime: Double, state: ChartSyncState = rememberChartSyncState()) {
    if (balance.isEmpty()) return
    
    ChartContainer("Balance Anaérobie (W' Balance)") { isExpanded ->
        val chartHeight = if (isExpanded) 300.dp else 180.dp
        
        Column {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(chartHeight)
                    .pointerInput(state) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            state.zoomScale = (state.zoomScale * zoom).coerceIn(1f, 10f)
                            if (state.zoomScale > 1f) {
                                state.zoomOffset = (state.zoomOffset + pan.x).coerceIn(size.width * (1f - state.zoomScale), 0f)
                            } else {
                                state.zoomOffset = 0f
                            }
                        }
                    }
                    .pointerInput(state) {
                        detectTapGestures(
                            onDoubleTap = {
                                state.zoomScale = 1f
                                state.zoomOffset = 0f
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val maxW = wPrime.toFloat().coerceAtLeast(1f)
                
                // 2. W' Balance (Battery Line)
                val wPath = Path()
                for (i in balance.indices) {
                    val valW = balance[i]
                    val x = ((i.toFloat() / (balance.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                    val y = h - (valW.toFloat() / maxW) * h
                    if (i == 0) wPath.moveTo(x, y) else wPath.lineTo(x, y)
                }
                drawPath(wPath, Color(0xFFFF1744), style = Stroke(width = 2.dp.toPx()))
                
                // Labels
                drawLine(Color.Gray.copy(alpha=0.3f), Offset(0f, 0f), Offset(0f, h), 1.dp.toPx())
                drawLine(Color.Gray.copy(alpha=0.3f), Offset(0f, h), Offset(w, h), 1.dp.toPx())
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DÉBUT", fontSize = 8.sp, color = AirTextLight)
                Text("TIME →", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AirTextLight)
                Text("FIN", fontSize = 8.sp, color = AirTextLight)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ChartLegendIndicator("W' Libéré (Pile)", Color(0xFFFF1744))
                Text("Batterie Totale: ${wPrime.toInt()} J", fontSize = 10.sp, color = AirTextLight)
            }
        }
    }
}

/**
 * Quadrant Analysis Chart
 * Scatter plot of Average Effective Pedal Force (N) vs Circumferential Pedal Velocity (m/s).
 */
@Composable
fun QuadrantAnalysisChart(points: List<QuadrantPoint>, onInfoClick: (String) -> Unit = {}) {
    if (points.isEmpty()) return
    
    val stats = remember(points) {
        val total = points.size.toFloat()
        val q1 = points.count { it.quadrant == 1 } / total
        val q2 = points.count { it.quadrant == 2 } / total
        val q3 = points.count { it.quadrant == 3 } / total
        val q4 = points.count { it.quadrant == 4 } / total
        listOf(q1, q2, q3, q4)
    }

    ChartContainer("Analyse de Puissance (Quadrants)") { isExpanded ->
        val chartSize = if (isExpanded) 350.dp else 240.dp
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(chartSize)) {
                Canvas(modifier = Modifier.fillMaxSize().background(AirBackground.copy(alpha = 0.2f))) {
                    val w = size.width
                    val h = size.height
                    
                    val maxForce = (points.maxOfOrNull { it.force } ?: 1.0).coerceAtLeast(10.0)
                    val maxVel = (points.maxOfOrNull { it.velocity } ?: 1.0).coerceAtLeast(1.0)
                    
                    val midX = w / 2
                    val midY = h / 2

                    drawRect(Color(0xFFFF9100).copy(alpha = 0.05f), Offset(midX, 0f), androidx.compose.ui.geometry.Size(midX, midY)) 
                    drawRect(Color(0xFFD500F9).copy(alpha = 0.05f), Offset(0f, 0f), androidx.compose.ui.geometry.Size(midX, midY))    
                    drawRect(Color(0xFF00E676).copy(alpha = 0.05f), Offset(midX, midY), androidx.compose.ui.geometry.Size(midX, midY)) 
                    drawRect(Color(0xFF2979FF).copy(alpha = 0.05f), Offset(0f, midY), androidx.compose.ui.geometry.Size(midX, midY))    
                    
                    drawLine(Color.Gray.copy(alpha=0.5f), Offset(midX, 0f), Offset(midX, h), 1.dp.toPx())
                    drawLine(Color.Gray.copy(alpha=0.5f), Offset(0f, midY), Offset(w, midY), 1.dp.toPx())
                    
                    points.filterIndexed { i, _ -> i % 2 == 0 }.take(600).forEach { p ->
                        val x = (p.velocity / maxVel).toFloat() * w
                        val y = h - (p.force / maxForce).toFloat() * h
                        
                        val color = when(p.quadrant) {
                            1 -> Color(0xFFFF9100) 
                            2 -> Color(0xFFD500F9) 
                            3 -> Color(0xFF2979FF) 
                            else -> Color(0xFF00E676) 
                        }
                        drawCircle(color.copy(alpha=0.7f), 3.dp.toPx(), Offset(x, y))
                    }
                }
                
                Text("Q2: FORCE", Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD500F9).copy(0.6f))
                Text("Q1: SPRINT", Modifier.align(Alignment.TopEnd).padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9100).copy(0.6f))
                Text("Q4: VÉLOCITÉ", Modifier.align(Alignment.BottomEnd).padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676).copy(0.6f))
                Text("Q3: RÉCUP", Modifier.align(Alignment.BottomStart).padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2979FF).copy(0.6f))

                IconButton(
                    onClick = { onInfoClick("QUADRANT") },
                    modifier = Modifier.align(Alignment.Center).background(Color.White.copy(0.8f), CircleShape).size(24.dp)
                ) {
                    Icon(Icons.Rounded.Info, null, modifier = Modifier.size(16.dp), tint = AirAccent)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                QuadrantStatItem("Q1: Sprints", stats[0], Color(0xFFFF9100))
                QuadrantStatItem("Q2: Force", stats[1], Color(0xFFD500F9))
                QuadrantStatItem("Q3: Endurance", stats[2], Color(0xFF2979FF))
                QuadrantStatItem("Q4: Souplesse", stats[3], Color(0xFF00E676))
            }
        }
    }
}

@Composable
fun QuadrantStatItem(label: String, fraction: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = AirTextLight)
        Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

/**
 * AeroLab Virtual Elevation Chart
 * Chung Method CdA/Crr Visualization.
 */
@Composable
fun AeroLabChart(result: AeroLabResult, state: ChartSyncState = rememberChartSyncState()) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)

    ChartContainer("AeroLab (Virtual Elevation)") { isExpanded ->
        val chartHeight = if (isExpanded) 300.dp else 180.dp
        
        Column {
            Canvas(
                modifier = Modifier.fillMaxWidth().height(chartHeight)
                    .pointerInput(state) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            state.zoomScale = (state.zoomScale * zoom).coerceIn(1f, 10f)
                            if (state.zoomScale > 1f) {
                                state.zoomOffset = (state.zoomOffset + pan.x).coerceIn(size.width * (1f - state.zoomScale), 0f)
                            } else {
                                state.zoomOffset = 0f
                            }
                        }
                    }
                    .pointerInput(state) {
                        detectDragGestures(
                            onDragStart = { state.scrubX = (it.x - state.zoomOffset) / state.zoomScale },
                            onDragEnd = { state.scrubX = null },
                            onDragCancel = { state.scrubX = null },
                            onDrag = { change, _ -> state.scrubX = (change.position.x - state.zoomOffset) / state.zoomScale }
                        )
                    }
                    .pointerInput(state) {
                        detectTapGestures(
                            onDoubleTap = {
                                state.zoomScale = 1f
                                state.zoomOffset = 0f
                            },
                            onTap = { state.scrubX = (it.x - state.zoomOffset) / state.zoomScale }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                
                val allElev = result.virtualElevation + result.actualElevation
                val minE = allElev.minOrNull()?.toFloat() ?: 0f
                val maxE = allElev.maxOrNull()?.toFloat() ?: 100f
                val eRange = (maxE - minE).coerceAtLeast(1f)
                
                // 1. Actual Elevation
                val actPath = Path()
                for (i in result.actualElevation.indices) {
                    val e = result.actualElevation[i]
                    val x = ((i.toFloat() / (result.actualElevation.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                    val y = h - ((e.toFloat() - minE) / eRange) * h
                    if (i == 0) actPath.moveTo(x, y) else actPath.lineTo(x, y)
                }
                drawPath(actPath, Color.Gray.copy(alpha=0.4f), style = Stroke(width = 1.dp.toPx()))
                
                // 2. Virtual Elevation
                val virtPath = Path()
                for (i in result.virtualElevation.indices) {
                    val e = result.virtualElevation[i]
                    val x = ((i.toFloat() / (result.virtualElevation.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                    val y = h - ((e.toFloat() - minE) / eRange) * h
                    if (i == 0) virtPath.moveTo(x, y) else virtPath.lineTo(x, y)
                }
                drawPath(virtPath, AirAccent, style = Stroke(width = 2.dp.toPx()))
                
                // Axis markers
                drawLine(Color.Gray.copy(0.2f), Offset(0f, 0f), Offset(0f, h), 1.dp.toPx())
                drawLine(Color.Gray.copy(0.2f), Offset(0f, h), Offset(w, h), 1.dp.toPx())

                // Scrubber (handled by state in future if needed)
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DÉPART", fontSize = 9.sp, color = AirTextLight)
                Text("DISTANCE (KM) →", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AirTextLight)
                Text("ARRIVÉE", fontSize = 9.sp, color = AirTextLight)
            }
            Spacer(Modifier.height(4.dp))
            ChartLegendIndicator("Élévation Virtuelle (CdA: ${String.format("%.3f", result.cda)})", AirAccent)
            Text("Axe Vertical: Altitude (Mètres)", fontSize = 9.sp, color = AirTextSecondary)
        }
    }
}

/**
 * Professional Intervals Table
 */
@Composable
fun ProfessionalIntervalsTable(intervals: List<ProInterval>) {
    if (intervals.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Intervalles Détectés", fontWeight = FontWeight.Bold, color = AirTextPrimary)
            Spacer(Modifier.height(12.dp))
            
            for (interval in intervals) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = if (interval.type == "WORK") Color(0xFFFF5252) else Color(0xFF00E676)
                    Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(interval.name, modifier = Modifier.weight(1f), fontSize = 12.sp, color = AirTextPrimary)
                    Text("${interval.avgPower?.toInt() ?: 0} W", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text("${(interval.endSec - interval.startSec) / 60}m${(interval.endSec - interval.startSec) % 60}s", fontSize = 11.sp, color = AirTextLight)
                }
            }
        }
    }
}
@Composable
fun SwimEfficiencyChart(si: Double, speedMs: Double) {
    ChartContainer("Efficacité vs Allure (Swim)") { isExpanded ->
        val chartSize = if (isExpanded) 350.dp else 240.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(chartSize).clip(RoundedCornerShape(12.dp)).background(AirBackground.copy(alpha = 0.3f))) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Limits: Speed 0.5 to 2.0 m/s, SI 0.5 to 4.5
                    val minSpeed = 0.5f
                    val maxSpeed = 2.0f
                    val minSI = 0.5f
                    val maxSI = 4.5f
                    
                    // Grid
                    for(i in 0..4) {
                        val x = (i/4f) * w
                        drawLine(Color.Gray.copy(0.1f), Offset(x, 0f), Offset(x, h))
                        val y = (i/4f) * h
                        drawLine(Color.Gray.copy(0.1f), Offset(0f, y), Offset(w, y))
                    }
                    
                    // Elite Zone (Top Right)
                    drawRect(
                        Color(0xFF00E676).copy(alpha = 0.05f),
                        Offset(w*0.6f, 0f),
                        androidx.compose.ui.geometry.Size(w*0.4f, h*0.4f)
                    )
                    
                    // Axis Lines
                    drawLine(Color.Gray.copy(0.2f), Offset(0f, h), Offset(w, h), 2.dp.toPx())
                    drawLine(Color.Gray.copy(0.2f), Offset(0f, 0f), Offset(0f, h), 2.dp.toPx())
                    
                    // Point
                    val px = ((speedMs.toFloat() - minSpeed) / (maxSpeed - minSpeed)).coerceIn(0f, 1f) * w
                    val py = h - ((si.toFloat() - minSI) / (maxSI - minSI)).coerceIn(0f, 1f) * h
                    
                    drawCircle(AirAccent, 8.dp.toPx(), Offset(px, py))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(px, py))
                }
                Text("ALLURE (min/km) →", Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), fontSize = 8.sp, color = AirTextLight)
                Text("INDEX D'EFFICACITÉ (SI)", Modifier.align(Alignment.CenterStart).rotate(-90f).padding(bottom = 4.dp), fontSize = 8.sp, color = AirTextLight)
                Text("ZONE ÉLITE", Modifier.align(Alignment.TopEnd).padding(8.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676).copy(0.6f))
            }
            Spacer(Modifier.height(8.dp))
            Text("SI actuel: ${String.format("%.2f", si)}. " + run {
                if (speedMs > 0.1) {
                    val totalSec = (1000.0 / (speedMs * 60.0)) * 60.0
                    val min = (totalSec / 60).toInt()
                    val sec = (totalSec % 60).toInt()
                    "Allure: ${String.format("%d:%02d", min, sec)} min/km. "
                } else ""
            } + "Un SI élevé indique une meilleure combinaison de vitesse et de distance par cycle.", fontSize = 10.sp, color = AirTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/**
 * Interactive Overlaid Chart with Scrubbing and Selection
 */
@Composable
fun InteractiveOverlaidChart(
    hrSamples: List<Persistence.HeartRateSample> = emptyList(),
    speedSamples: List<Persistence.SpeedSample> = emptyList(),
    powerSamples: List<Persistence.PowerSample> = emptyList(),
    altSamples: List<Persistence.ElevationSample> = emptyList(),
    vamSamples: List<Pair<Int, Double>> = emptyList(),
    gapSamples: List<Pair<Int, Double>> = emptyList(),
    cadenceSamples: List<Persistence.CadenceSample> = emptyList(),
    trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null,
    state: ChartSyncState = rememberChartSyncState(),
    isSwim: Boolean = false,
    onMetricClick: (String) -> Unit
) {
    if (hrSamples.isEmpty() && speedSamples.isEmpty() && powerSamples.isEmpty() && altSamples.isEmpty()) return

    var selectedMetrics by remember { mutableStateOf(setOf("FC", "ALLURE", "WATT", "ALT", "VAM", "GAP", "CADENCE")) }
    
    val chartHeight = 240.dp
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, AirSurface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ANALYSE DYNAMIQUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AirTextSecondary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = AirTextLight)
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Selector Chips (Larger as requested)
            FlowRow(
                mainAxisSpacing = 10.dp,
                crossAxisSpacing = 10.dp
            ) {
                ChartFilterChip("FC", hrSamples.isNotEmpty(), Color(0xFFFF0000), selectedMetrics.contains("FC"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("FC")) selectedMetrics - "FC" else selectedMetrics + "FC"
                }
                ChartFilterChip("ALLURE", speedSamples.isNotEmpty(), Color(0xFF00E5FF), selectedMetrics.contains("ALLURE"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("ALLURE")) selectedMetrics - "ALLURE" else selectedMetrics + "ALLURE"
                }
                ChartFilterChip("WATT", powerSamples.isNotEmpty(), Color(0xFFD500F9), selectedMetrics.contains("WATT"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("WATT")) selectedMetrics - "WATT" else selectedMetrics + "WATT"
                }
                ChartFilterChip("CADENCE", cadenceSamples.isNotEmpty(), Color(0xFFFFEA00), selectedMetrics.contains("CADENCE"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("CADENCE")) selectedMetrics - "CADENCE" else selectedMetrics + "CADENCE"
                }
                ChartFilterChip("ALT", altSamples.isNotEmpty(), Color(0xFF4CAF50), selectedMetrics.contains("ALT"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("ALT")) selectedMetrics - "ALT" else selectedMetrics + "ALT"
                }
                ChartFilterChip("VAM", vamSamples.isNotEmpty(), AirAccent, selectedMetrics.contains("VAM"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("VAM")) selectedMetrics - "VAM" else selectedMetrics + "VAM"
                }
                ChartFilterChip("GAP", gapSamples.isNotEmpty(), Color(0xFF673AB7), selectedMetrics.contains("GAP"), onMetricClick) {
                    selectedMetrics = if (selectedMetrics.contains("GAP")) selectedMetrics - "GAP" else selectedMetrics + "GAP"
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
                val cw = constraints.maxWidth.toFloat()
                
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(state) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                state.zoomScale = (state.zoomScale * zoom).coerceIn(1f, 10f)
                                if (state.zoomScale > 1f) {
                                    state.zoomOffset = (state.zoomOffset + pan.x).coerceIn(cw * (1f - state.zoomScale), 0f)
                                } else {
                                    state.zoomOffset = 0f
                                }
                            }
                        }
                        .pointerInput(state) {
                            detectDragGestures(
                                onDragStart = { offset: Offset ->
                                    state.scrubX = (offset.x - state.zoomOffset) / state.zoomScale
                                },
                                onDragEnd = { state.scrubX = null },
                                onDragCancel = { state.scrubX = null },
                                onDrag = { change, _ ->
                                    state.scrubX = (change.position.x - state.zoomOffset) / state.zoomScale
                                }
                            )
                        }
                        .pointerInput(state) {
                            detectTapGestures(
                                onDoubleTap = {
                                    state.zoomScale = 1f
                                    state.zoomOffset = 0f
                                },
                                onTap = { offset: Offset ->
                                    state.scrubX = (offset.x - state.zoomOffset) / state.zoomScale
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Background Zones for primary metric
                    val activeZones = when {
                        selectedMetrics.contains("FC") && hrSamples.isNotEmpty() -> {
                            val maxH = hrSamples.maxOf { it.bpm }.toFloat()
                            val minH = hrSamples.minOf { it.bpm }.toFloat()
                            val rangeH = (maxH - minH).coerceAtLeast(1f)
                            val fcm = trainingPlan?.fcm ?: 190
                            val resting = trainingPlan?.userProfile?.restingHeartRate ?: 60
                            val zones = trainingPlan?.hrZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateHRZones(resting, fcm)
                            zones.map { ChartZoneBand(((it.min - minH) / rangeH).coerceIn(0f, 1f), ((it.max - minH) / rangeH).coerceIn(0f, 1f), getZoneColor(it.id)) }
                        }
                        selectedMetrics.contains("ALLURE") && speedSamples.isNotEmpty() -> {
                            val maxS = speedSamples.maxOf { it.speedMps }.toFloat().coerceAtLeast(0.1f)
                            val minS = speedSamples.minOf { it.speedMps }.toFloat()
                            val rangeS = (maxS - minS).coerceAtLeast(0.1f)
                            val vma = trainingPlan?.vma ?: 12.0
                            val zones = trainingPlan?.speedZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateSpeedZones(vma)
                            zones.map { ChartZoneBand(((it.minSpeedKmh / 3.6 - minS) / rangeS).toFloat().coerceIn(0f, 1f), ((it.maxSpeedKmh / 3.6 - minS) / rangeS).toFloat().coerceIn(0f, 1f), getZoneColor(it.id)) }
                        }
                        selectedMetrics.contains("WATT") && powerSamples.isNotEmpty() -> {
                            val maxP = powerSamples.maxOf { it.watts }.toFloat().coerceAtLeast(1f)
                            val pZones = trainingPlan?.powerZones ?: run {
                                val vma = trainingPlan?.vma ?: 12.0
                                val speedZones = trainingPlan?.speedZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateSpeedZones(vma)
                                val weight = trainingPlan?.userProfile?.weightKg ?: 70.0
                                com.orbital.run.logic.OrbitalAlgorithm.calculatePowerZones(speedZones, weight)
                            }
                            pZones.map { ChartZoneBand((it.minWatts.toFloat() / maxP).coerceIn(0f, 1f), (it.maxWatts.toFloat() / maxP).coerceIn(0f, 1f), getZoneColor(it.id)) }
                        }
                        else -> emptyList()
                    }

                    activeZones.forEach { band ->
                        val top = h - (band.maxRatio * h)
                        val bottom = h - (band.minRatio * h)
                        drawRect(
                            color = band.color.copy(alpha = 0.08f),
                            topLeft = Offset(0f, top),
                            size = androidx.compose.ui.geometry.Size(w, (bottom - top).coerceAtLeast(0f))
                        )
                    }

                    // Grid
                    for (i in 0..4) {
                        val y = (i / 4f) * h
                        drawLine(Color(0xFFEEEEEE), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                    }
                    
                    // Draw Power
                    if (selectedMetrics.contains("WATT") && powerSamples.isNotEmpty()) {
                        val maxP = powerSamples.maxOf { it.watts }.toFloat().coerceAtLeast(1f)
                        val path = Path()
                        powerSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (powerSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - (s.watts.toFloat() / maxP) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFFD500F9), style = Stroke(1.5.dp.toPx()))
                    }
                    
                    // Draw Allure (Pace) - We use speed for plotting but labels will show pace
                    if (selectedMetrics.contains("ALLURE") && speedSamples.isNotEmpty()) {
                        val maxS = speedSamples.maxOf { it.speedMps }.toFloat().coerceAtLeast(0.1f)
                        val minS = speedSamples.minOf { it.speedMps }.toFloat()
                        val sRange = (maxS - minS).coerceAtLeast(0.1f)
                        val path = Path()
                        speedSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (speedSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - ((s.speedMps.toFloat() - minS) / sRange) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF00E5FF), style = Stroke(2.5.dp.toPx()))
                    }

                    // Draw Cadence
                    if (selectedMetrics.contains("CADENCE") && cadenceSamples.isNotEmpty()) {
                        val maxC = cadenceSamples.maxOf { it.rpm }.toFloat().coerceAtLeast(1f)
                        val minC = cadenceSamples.minOf { it.rpm }.toFloat()
                        val cRange = (maxC - minC).coerceAtLeast(1f)
                        val path = Path()
                        cadenceSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (cadenceSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - ((s.rpm.toFloat() - minC) / cRange) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFFFFEA00), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)))
                    }
                    
                    // Draw HR
                    if (selectedMetrics.contains("FC") && hrSamples.isNotEmpty()) {
                        val maxH = hrSamples.maxOf { it.bpm }.toFloat()
                        val minH = hrSamples.minOf { it.bpm }.toFloat()
                        val hRange = (maxH - minH).coerceAtLeast(1f)
                        val path = Path()
                        hrSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (hrSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - ((s.bpm.toFloat() - minH) / hRange) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFFFF0000), style = Stroke(3.dp.toPx()))
                    }

                    // Draw ALT
                    if (selectedMetrics.contains("ALT") && altSamples.isNotEmpty()) {
                        val maxA = altSamples.maxOf { it.avgAltitude }.toFloat()
                        val minA = altSamples.minOf { it.avgAltitude }.toFloat()
                        val aRange = (maxA - minA).coerceAtLeast(1f)
                        val path = Path()
                        altSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (altSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - ((s.avgAltitude.toFloat() - minA) / aRange) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF4CAF50), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    }

                    // Draw VAM
                    if (selectedMetrics.contains("VAM") && vamSamples.isNotEmpty()) {
                        val maxV = vamSamples.maxOf { it.second }.toFloat().coerceAtLeast(100f)
                        val path = Path()
                        vamSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (vamSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - (s.second.toFloat() / maxV) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, AirAccent, style = Stroke(2.dp.toPx()))
                    }

                    // Draw GAP
                    if (selectedMetrics.contains("GAP") && gapSamples.isNotEmpty()) {
                        val maxG = gapSamples.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                        val minG = gapSamples.minOf { it.second }.toFloat()
                        val gRange = (maxG - minG).coerceAtLeast(0.1f)
                        val path = Path()
                        gapSamples.forEachIndexed { i, s ->
                            val x = ((i.toFloat() / (gapSamples.size - 1).coerceAtLeast(1)) * w * state.zoomScale) + state.zoomOffset
                            val y = h - ((s.second.toFloat() - minG) / gRange) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFF673AB7), style = Stroke(2.5.dp.toPx()))
                    }
                    
                    // Draw Scrubber (Thicker cursor)
                    state.scrubX?.let { sx ->
                        val clampedX = (sx * state.zoomScale + state.zoomOffset).coerceIn(0f, w)
                        drawLine(AirPrimary, Offset(clampedX, 0f), Offset(clampedX, h), 3.dp.toPx())
                        drawCircle(Color.White, 5.dp.toPx(), Offset(clampedX, h), style = Stroke(2.5.dp.toPx()))
                    }
                
                // Floating Tooltip when scrubbing
                // Do not perform Composable logic inside Canvas draw block
                // We need to move this out of Canvas scope
            } // End of Canvas

            state.scrubX?.let { sx ->
                val cw = constraints.maxWidth.toFloat()
                
                val progress = (sx / cw).coerceIn(0f, 1f)
                val visualX = sx * state.zoomScale + state.zoomOffset
                
                Box(Modifier.align(if(visualX > cw * 0.5f) Alignment.TopStart else Alignment.TopEnd).padding(8.dp)) {
                         Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.8f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (selectedMetrics.contains("FC") && hrSamples.isNotEmpty()) {
                                    val idx = (progress * (hrSamples.size - 1)).toInt().coerceIn(0, hrSamples.size - 1)
                                    ScrubValue("FC", "${hrSamples[idx].bpm}", "bpm", Color(0xFFFF5252))
                                }
                                if (selectedMetrics.contains("ALLURE") && speedSamples.isNotEmpty()) {
                                    val idx = (progress * (speedSamples.size - 1)).toInt().coerceIn(0, speedSamples.size - 1)
                                    val spMps = speedSamples[idx].speedMps
                                    val pace = if (isSwim) {
                                        if (spMps > 0.1) {
                                            val totalSec100m = 100.0 / spMps
                                            val min = (totalSec100m / 60).toInt()
                                            val sec = (totalSec100m % 60).toInt()
                                            String.format("%d:%02d", min, sec)
                                        } else "--:--"
                                    } else {
                                        if (spMps > 0.1) {
                                            val totalSec = (1000.0 / (spMps * 60.0)) * 60.0
                                            val min = (totalSec / 60).toInt()
                                            val sec = (totalSec % 60).toInt()
                                            String.format("%d:%02d", min, sec)
                                        } else "--:--"
                                    }
                                    ScrubValue("ALR", pace, if (isSwim) "min/100m" else "min/km", Color(0xFF00E5FF))
                                }
                                if (selectedMetrics.contains("CADENCE") && cadenceSamples.isNotEmpty()) {
                                    val idx = (progress * (cadenceSamples.size - 1)).toInt().coerceIn(0, cadenceSamples.size - 1)
                                    ScrubValue("CAD", "${cadenceSamples[idx].rpm.toInt()}", "spm", Color(0xFFFFEA00))
                                }
                                if (selectedMetrics.contains("WATT") && powerSamples.isNotEmpty()) {
                                    val idx = (progress * (powerSamples.size - 1)).toInt().coerceIn(0, powerSamples.size - 1)
                                    ScrubValue("PWR", "${powerSamples[idx].watts}", "W", Color(0xFFD500F9))
                                }
                                if (selectedMetrics.contains("ALT") && altSamples.isNotEmpty()) {
                                    val idx = (progress * (altSamples.size - 1)).toInt().coerceIn(0, altSamples.size - 1)
                                    ScrubValue("ALT", "${altSamples[idx].avgAltitude.toInt()}", "m", Color(0xFF4CAF50))
                                }
                                if (selectedMetrics.contains("VAM") && vamSamples.isNotEmpty()) {
                                    val idx = (progress * (vamSamples.size - 1)).toInt().coerceIn(0, vamSamples.size - 1)
                                    ScrubValue("VAM", "${vamSamples[idx].second.toInt()}", "m/h", AirAccent)
                                }
                                if (selectedMetrics.contains("GAP") && gapSamples.isNotEmpty()) {
                                    val idx = (progress * (gapSamples.size - 1)).toInt().coerceIn(0, gapSamples.size - 1)
                                    val gapMps = gapSamples[idx].second
                                    ScrubValue("GAP", formatPace(gapMps * 3.6), "/km", Color(0xFF673AB7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun ChartFilterChip(label: String, enabled: Boolean, color: Color, isSelected: Boolean, onInfoClick: (String) -> Unit, onToggle: () -> Unit) {
    if (!enabled) return
    
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onToggle() },
        color = if (isSelected) color else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, AirSurface)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp), // More compact
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSelected) Box(Modifier.size(6.dp).background(color, CircleShape))
            if (!isSelected) Spacer(Modifier.width(6.dp))
            Text(
                label, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Black, 
                color = if (isSelected) Color.White else AirTextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Info, 
                null, 
                modifier = Modifier.size(14.dp).clickable { onInfoClick(label) }, 
                tint = if (isSelected) Color.White.copy(alpha = 0.8f) else AirTextLight
            )
        }
    }
}

@Composable
fun ScrubValue(label: String, value: String, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = Color.White.copy(0.7f), fontSize = 11.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(" $unit", color = Color.White.copy(0.7f), fontSize = 10.sp)
    }
}

@Composable
fun HrChart(samples: List<Persistence.HeartRateSample>, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxH = (samples.maxOf { it.bpm }.toFloat())
    val minH = (samples.minOf { it.bpm }.toFloat())
    val rangeH = (maxH - minH).coerceAtLeast(1f)
    val avgH = samples.map { it.bpm }.average().toInt()

    val bands = remember(trainingPlan, samples) {
        val fcm = trainingPlan?.fcm ?: 190
        val resting = trainingPlan?.userProfile?.restingHeartRate ?: 60
        val hrZones = trainingPlan?.hrZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateHRZones(resting, fcm)
        
        hrZones.map { zone ->
            val minRatio = ((zone.min - minH) / rangeH).coerceIn(0f, 1f)
            val maxRatio = ((zone.max - minH) / rangeH).coerceIn(0f, 1f)
            ChartZoneBand(minRatio, maxRatio, getZoneColor(zone.id))
        }
    }

    StreamChartContainer("FRÉQUENCE CARDIAQUE", ZoneRed) { expanded ->
        val h = if (expanded) 240.dp else 140.dp
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf("${maxH.toInt()} bpm" to ZoneRed, "${minH.toInt()} bpm" to ZoneRed),
            avgLineY = 1f - ((avgH - minH) / rangeH).toFloat().coerceIn(0f, 1f),
            avgLabel = "$avgH AVG",
            avgColor = ZoneRed.copy(alpha = 0.5f),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${samples[idx].bpm} bpm"
                } else null
            },
            backgroundBands = bands
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.bpm.toFloat() - minH) / rangeH) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, ZoneRed, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun PaceChart(samples: List<Persistence.SpeedSample>, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null, state: ChartSyncState = rememberChartSyncState(), isSwim: Boolean = false) {
    if (samples.isEmpty()) return
    
    val speeds = samples.map { it.speedMps }
    val maxS = speeds.max().toFloat()
    val minS = speeds.min().toFloat()
    val rangeS = (maxS - minS).coerceAtLeast(0.1f)
    val avgS = speeds.average()

    fun formatPaceVal(mps: Double): String {
        return if (isSwim) {
             if (mps > 0.1) {
                val totalSec100m = 100.0 / mps
                val min = (totalSec100m / 60).toInt()
                val sec = (totalSec100m % 60).toInt()
                String.format("%d:%02d", min, sec)
             } else "--:--"
        } else {
             formatPace(mps * 3.6)
        }
    }
    
    val unitLabel = if (isSwim) "/100m" else "/km"

    val bands = remember(trainingPlan, samples) {
        val vma = trainingPlan?.vma ?: 12.0 // Generic VMA 12km/h
        val speedZones = trainingPlan?.speedZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateSpeedZones(vma)
        
        speedZones.map { zone ->
            // SpeedZone is in km/h, convert to m/s
            val minMps = zone.minSpeedKmh / 3.6
            val maxMps = zone.maxSpeedKmh / 3.6
            val minRatio = ((minMps - minS) / rangeS).toFloat().coerceIn(0f, 1f)
            val maxRatio = ((maxMps - minS) / rangeS).toFloat().coerceIn(0f, 1f)
            ChartZoneBand(minRatio, maxRatio, getZoneColor(zone.id))
        }
    }

    StreamChartContainer("ALLURE (PACE)", Color(0xFF00E5FF)) { expanded ->
        val h = if (expanded) 240.dp else 140.dp
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf(formatPaceVal(maxS.toDouble()) to Color(0xFF00E5FF), formatPaceVal(minS.toDouble()) to Color(0xFF00E5FF)),
            avgLineY = 1f - ((avgS.toFloat() - minS) / rangeS).coerceIn(0f, 1f),
            avgLabel = "${formatPaceVal(avgS)} AVG",
            avgColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${formatPaceVal(samples[idx].speedMps)} $unitLabel"
                } else null
            },
            backgroundBands = bands
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.speedMps.toFloat() - minS) / rangeS) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF00E5FF), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun PowerChart(samples: List<Persistence.PowerSample>, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxP = (samples.maxOf { it.watts }.toFloat()).coerceAtLeast(1f)
    val avgP = samples.map { it.watts }.average().toInt()

    val bands = remember(trainingPlan, samples) {
        val powerZones = trainingPlan?.powerZones ?: run {
            val vma = trainingPlan?.vma ?: 12.0
            val speedZones = trainingPlan?.speedZones ?: com.orbital.run.logic.OrbitalAlgorithm.calculateSpeedZones(vma)
            val weight = trainingPlan?.userProfile?.weightKg ?: 70.0
            com.orbital.run.logic.OrbitalAlgorithm.calculatePowerZones(speedZones, weight)
        }
        
        powerZones.map { zone ->
            val minRatio = (zone.minWatts.toFloat() / maxP).coerceIn(0f, 1f)
            val maxRatio = (zone.maxWatts.toFloat() / maxP).coerceIn(0f, 1f)
            ChartZoneBand(minRatio, maxRatio, getZoneColor(zone.id))
        }
    }

    StreamChartContainer("PUISSANCE", Color(0xFFD500F9)) { expanded ->
        val h = if (expanded) 240.dp else 140.dp
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf("${maxP.toInt()} W" to Color(0xFFD500F9), "0 W" to Color(0xFFD500F9)),
            avgLineY = 1f - (avgP.toFloat() / maxP),
            avgLabel = "$avgP W AVG",
            avgColor = Color(0xFFD500F9).copy(alpha = 0.5f),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${samples[idx].watts} W"
                } else null
            },
            backgroundBands = bands
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - (s.watts.toFloat() / maxP) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFFD500F9), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun CadenceChart(samples: List<Persistence.CadenceSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxC = (samples.maxOf { it.rpm }.toFloat()).coerceAtLeast(1f)
    val minC = (samples.minOf { it.rpm }.toFloat())
    val rangeC = (maxC - minC).coerceAtLeast(1f)
    val avgC = samples.map { it.rpm }.average().toInt()

    StreamChartContainer("CADENCE", Color(0xFFFFEA00)) { expanded ->
        val h = if (expanded) 240.dp else 140.dp
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf("${maxC.toInt()} SPM" to Color(0xFFFFEA00), "${minC.toInt()} SPM" to Color(0xFFFFEA00)),
            avgLineY = 1f - ((avgC - minC) / rangeC).toFloat().coerceIn(0f, 1f),
            avgLabel = "$avgC AVG",
            avgColor = Color(0xFFFFEA00).copy(alpha = 0.5f),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${samples[idx].rpm.toInt()} SPM"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.rpm.toFloat() - minC) / rangeC) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFFFFEA00), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun VamChart(vamPoints: List<Pair<Int, Double>>, state: ChartSyncState = rememberChartSyncState()) {
    if (vamPoints.isEmpty()) return
    
    val maxV = (vamPoints.maxOf { it.second }.toFloat()).coerceAtLeast(100f)
    val avgV = vamPoints.map { it.second }.average().toInt()

    StreamChartContainer("VITESSE ASCENSIONNELLE (VAM)", AirAccent) { expanded ->
        val h = if (expanded) 240.dp else 140.dp
        
        StreamCanvas(
            height = h,
            _dataSize = vamPoints.size,
            yLabels = listOf("${maxV.toInt()} m/h" to AirAccent, "0 m/h" to AirAccent),
            avgLineY = 1f - (avgV.toFloat() / maxV),
            avgLabel = "$avgV AVG",
            avgColor = AirAccent.copy(alpha = 0.5f),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (vamPoints.size - 1)).toInt().coerceIn(0, vamPoints.size - 1)
                    "${vamPoints[idx].second.toInt()} m/h"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            vamPoints.forEachIndexed { i, p ->
                val x = ((i.toFloat() / (vamPoints.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - (p.second.toFloat() / maxV) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            
            drawPath(
                path, 
                brush = Brush.verticalGradient(listOf(AirAccent.copy(0.5f), AirAccent.copy(0.1f))),
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Area
            val areaPath = Path().apply {
                addPath(path)
                val lastX = ((vamPoints.size - 1).toFloat() / (vamPoints.size - 1).coerceAtLeast(1)) * w * zScale + zOffset
                lineTo(lastX, h2)
                lineTo(zOffset, h2)
                close()
            }
            drawPath(areaPath, brush = Brush.verticalGradient(listOf(AirAccent.copy(0.2f), Color.Transparent)))
        }
    }
}

@Composable
private fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content) { measurables, constraints ->
        val placeholders = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var rowWidth = 0

        placeholders.forEach { p ->
            if (rowWidth + p.width + mainAxisSpacing.roundToPx() > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                rowWidth = 0
            }
            currentRow.add(p)
            rowWidth += p.width + mainAxisSpacing.roundToPx()
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val totalHeight = rows.sumOf { r -> r.maxOf { it.height } } + (rows.size - 1).coerceAtLeast(0) * crossAxisSpacing.roundToPx()
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOf { it.height }
                row.forEach { p ->
                    p.placeRelative(x, y + (rowHeight - p.height) / 2)
                    x += p.width + mainAxisSpacing.roundToPx()
                }
                y += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

/**
 * Running Dynamics Charts
 */

@Composable
fun StrideLengthChart(samples: List<Persistence.RunningDynamicSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxVal = samples.maxOfOrNull { it.value }?.toFloat() ?: 2f
    val minVal = samples.minOfOrNull { it.value }?.toFloat() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(0.1f)
    
    StreamChartContainer("LONGUEUR DE FOULÉE", Color(0xFF6200EA)) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf(String.format("%.2fm", maxVal) to Color(0xFF6200EA), String.format("%.2fm", minVal) to Color(0xFF6200EA)),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${String.format("%.2f", samples[idx].value)} m"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.value.toFloat() - minVal) / range) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF6200EA), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun GCTChart(samples: List<Persistence.RunningDynamicSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxVal = samples.maxOfOrNull { it.value }?.toFloat() ?: 300f
    val minVal = samples.minOfOrNull { it.value }?.toFloat() ?: 100f
    val range = (maxVal - minVal).coerceAtLeast(10f)
    
    StreamChartContainer("TEMPS DE CONTACT SOL (GCT)", Color(0xFFFF9800)) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf("${maxVal.toInt()}ms" to Color(0xFFFF9800), "${minVal.toInt()}ms" to Color(0xFFFF9800)),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${samples[idx].value.toInt()} ms"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.value.toFloat() - minVal) / range) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFFFF9800), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun VerticalOscillationChart(samples: List<Persistence.RunningDynamicSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxVal = samples.maxOfOrNull { it.value }?.toFloat() ?: 15f
    val minVal = samples.minOfOrNull { it.value }?.toFloat() ?: 5f
    val range = (maxVal - minVal).coerceAtLeast(1f)
    
    StreamChartContainer("OSCILLATION VERTICALE", Color(0xFF039BE5)) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf(String.format("%.1fcm", maxVal) to Color(0xFF039BE5), String.format("%.1fcm", minVal) to Color(0xFF039BE5)),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${String.format("%.1f", samples[idx].value)} cm"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.value.toFloat() - minVal) / range) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF039BE5), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun VerticalRatioChart(samples: List<Persistence.RunningDynamicSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.isEmpty()) return
    
    val maxVal = samples.maxOfOrNull { it.value }?.toFloat() ?: 15f
    val minVal = samples.minOfOrNull { it.value }?.toFloat() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)
    
    StreamChartContainer("RATIO VERTICAL", Color(0xFF4CAF50)) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf(String.format("%.1f%%", maxVal) to Color(0xFF4CAF50), String.format("%.1f%%", minVal) to Color(0xFF4CAF50)),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${String.format("%.1f", samples[idx].value)}%"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.value.toFloat() - minVal) / range) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF4CAF50), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
/**
 * Heart Rate Derivative Chart (dHR/dt)
 * Shows the rate of HR change - how fast the heart adapts to effort changes.
 * Positive = HR increasing, Negative = HR decreasing
 */
@Composable
fun HeartRateDerivativeChart(samples: List<Persistence.HeartRateSample>, state: ChartSyncState = rememberChartSyncState()) {
    if (samples.size < 2) return
    
    ChartContainer("Dérivée Cardiaque (dHR/dt)") { isExpanded ->
        val chartHeight = if (isExpanded) 300.dp else 180.dp
        
        // Calculate derivatives (BPM change per second)
        val derivatives = remember(samples) {
            samples.zipWithNext { a, b ->
                val timeDiff = (b.timeOffset - a.timeOffset).coerceAtLeast(1) // Avoid div by 0
                val hrDiff = b.bpm - a.bpm
                val derivative = hrDiff.toDouble() / timeDiff.toDouble() // BPM per second
                Pair(b.timeOffset, derivative)
            }
        }
        
        if (derivatives.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Données insuffisantes", color = AirTextSecondary, fontSize = 12.sp)
            }
            return@ChartContainer
        }
        
        val maxDerivative = derivatives.maxByOrNull { kotlin.math.abs(it.second) }?.second?.let { kotlin.math.abs(it) } ?: 1.0
        val avgDerivative = derivatives.map { it.second }.average()
        
        Column {
            StreamCanvas(
                height = chartHeight,
                _dataSize = derivatives.size,
                yLabels = listOf(
                    String.format("+%.1f bpm/s", maxDerivative) to Color(0xFFFF5252),
                    String.format("%.1f bpm/s", -maxDerivative) to Color(0xFF2979FF)
                ),
                avgLineY = 0.5f, // Zero line in the middle
                avgLabel = "0 (Stable)",
                avgColor = Color.Gray,
                state = state,
                scrubValue = { w, _ ->
                    state.scrubX?.let { scrubX ->
                        val index = ((scrubX / w) * derivatives.size).toInt().coerceIn(0, derivatives.lastIndex)
                        val (time, deriv) = derivatives[index]
                        val min = time / 60
                        val sec = time % 60
                        String.format("%d:%02d | %.2f bpm/s", min, sec, deriv)
                    }
                }
            ) { w, h, zoomScale, zoomOffset ->
                val path = Path()
                val positiveGradient = Brush.verticalGradient(
                    0f to Color(0xFFFF5252).copy(alpha = 0.3f),
                    1f to Color.Transparent
                )
                val negativeGradient = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color(0xFF2979FF).copy(alpha = 0.3f)
                )
                
                derivatives.forEachIndexed { i, (_, deriv) ->
                    val xRatio = i.toFloat() / (derivatives.size - 1).coerceAtLeast(1)
                    val x = (xRatio * w * zoomScale) + zoomOffset
                    
                    // Map derivative to y: 0.5 = zero line, above = positive, below = negative
                    val yRatio = 0.5f - ((deriv / maxDerivative).toFloat() * 0.5f).coerceIn(-0.5f, 0.5f)
                    val y = yRatio * h
                    
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                
                // Fill areas above/below zero
                val fillPath = Path()
                fillPath.addPath(path)
               fillPath.lineTo(derivatives.lastIndex.toFloat() / (derivatives.size - 1) * w * zoomScale + zoomOffset, h * 0.5f)
                fillPath.lineTo(0f, h * 0.5f)
                fillPath.close()
                
                // Draw gradient fill
                drawPath(
                    path = fillPath,
                    brush = if (avgDerivative > 0) positiveGradient else negativeGradient
                )
                
                // Draw line
                drawPath(
                    path = path,
                    color = Color(0xFF00E676),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                // Draw zero reference line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, h * 0.5f),
                    end = Offset(w, h * 0.5f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Interpretation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Adaptation Moyenne", fontSize = 10.sp, color = AirTextLight)
                    Text(
                        text = String.format("%.2f bpm/s", avgDerivative),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            avgDerivative > 2 -> Color(0xFFFF5252) // Montée rapide = effort intense
                            avgDerivative < -2 -> Color(0xFF2979FF) // Descente rapide = bonne récupération
                            else -> Color(0xFF00E676) // Stable = allure constante
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Interprétation", fontSize = 10.sp, color = AirTextLight)
                    Text(
                        text = when {
                            avgDerivative > 3 -> "Départ rapide ⚡"
                            avgDerivative > 1 -> "Montée progressive 📈"
                            avgDerivative < -3 ->"Récup efficace 💚"
                            avgDerivative < -1 -> "Ralentissement 📉"
                            else -> "Allure stable ✅"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AirTextPrimary
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                text = "💡 La dérivée montre la vitesse d'adaptation du cœur. Pic élevé = effort soudain, creux profond = récupération rapide.",
                fontSize = 10.sp,
                color = AirTextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
