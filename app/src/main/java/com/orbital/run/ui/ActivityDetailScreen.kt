package com.orbital.run.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.logic.AnalysisEngine
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.WorkoutType
import com.orbital.run.logic.ProInterval
import com.orbital.run.logic.Insight
import com.orbital.run.logic.AdviceType
import com.orbital.run.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String,
    context: android.content.Context,
    onBack: () -> Unit,
    trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null
) {
    // 1. Data State
    var history by remember { mutableStateOf(Persistence.loadHistory(context)) }
    val activity = history.find { it.id == activityId }

    if (activity == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Activité introuvable", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    // 2. UI State
    var isEditing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(activity.title) }
    var notes by remember { mutableStateOf(activity.notes ?: "") }
    var rpe by remember { mutableStateOf(activity.rpe ?: 0) }
    var selectedMetric by remember { mutableStateOf<String?>(null) }
    var showFullMap by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isEditing) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                 colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AirTextPrimary
                            )
                        }
                        Text(
                            text = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(Date(activity.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = AirTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = AirTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, "Partager", tint = AirPrimary)
                    }
                    if (isEditing) {
                        IconButton(onClick = {
                            val updated = activity.copy(title = title, notes = notes, rpe = if (rpe > 0) rpe else null)
                            Persistence.saveCompletedActivity(context, updated)
                            history = Persistence.loadHistory(context)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, "Sauvegarder", tint = ZoneGreen)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Modifier", tint = AirTextSecondary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Supprimer", tint = ZoneRed.copy(alpha = 0.7f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(12.dp))
            // 1. Interactive Map (Top)
            ActivityMapHero(activity, onMapClick = { showFullMap = true })

            // 2. Primary Metrics Banner (Below Map)
            PrimaryMetricsBanner(activity)

            // 3. Comprehensive Data Grid (Other Data)
            SecondaryDataGrid(activity, trainingPlan) { selectedMetric = it }
            
            // 4. Synchronization Info
            SyncDetailsSection(activity)
            
            // 5. Analyses & Insights Sections
            PerformanceInsightsSection(activity, trainingPlan)
            ProfessionalAnalysisSection(activity, trainingPlan)
            RunningQualitySection(activity, trainingPlan)
            VisualAnalysisSection(activity, trainingPlan) { selectedMetric = it }
 
            // 6. Laps & Splits
            SplitsSection(activity, trainingPlan)

            // 7. Social & Sharing
            // SocialContextSection(activity)

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(40.dp))
        }
    }

    // Dialogs
    if (showFullMap) {
        FullScreenMapDialog(activity, onDismiss = { showFullMap = false })
    }
    if (showShareDialog) {
        ShareActivityDialog(activity = activity, trainingPlan = trainingPlan, onDismiss = { showShareDialog = false })
    }
    selectedMetric?.let { metric ->
        MetricExplanationDialog(metric) { selectedMetric = null }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AirWhite,
            title = { Text("Supprimer l'activité ?", color = AppText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Êtes-vous sûr de vouloir supprimer cette activité ?",
                        color = AirTextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${activity.title} - ${String.format("%.1f", activity.distanceKm)} km",
                        color = AirTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Persistence.deleteActivity(context, activity.id)
                        showDeleteDialog = false
                        onBack()
                        android.widget.Toast.makeText(context, "Activité supprimée", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZoneRed)
                ) {
                    Text("SUPPRIMER", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("AN NULER", color = AirTextLight)
                }
            }
        )
    }
}

@Composable
fun ActivityMapHero(activity: Persistence.CompletedActivity, onMapClick: () -> Unit) {
    val context = LocalContext.current
    var is3DMode by remember { mutableStateOf(false) } // State for 3D Toggle

    val points = remember(activity.summaryPolyline) {
        if (activity.summaryPolyline.isNullOrEmpty()) emptyList()
        else decodePolyline(activity.summaryPolyline)
    }

    LaunchedEffect(Unit) {
        val appCtx = context.applicationContext
        Configuration.getInstance().load(appCtx, appCtx.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "OrbitalBelt/1.0"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(AirSurface)
            .clip(RoundedCornerShape(24.dp))
            .background(AirSurface)
            .clickable { onMapClick() }
    ) {
        // Toggle between 2D (Osmdroid) and 3D (MapLibre)
        if (is3DMode) {
             MapLibreHero(activity = activity, isInteractive = false)
        } else if (points.isNotEmpty()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false) 
                        controller.setZoom(15.0)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        onResume() 
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    val geoPoints = points.map { GeoPoint(it.first, it.second) }
                    val line = Polyline().apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.parseColor("#007AFF")
                        outlinePaint.strokeWidth = 14f
                    }
                    mapView.overlays.add(line)
                    mapView.invalidate()
                    mapView.post {
                        try {
                            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                            mapView.zoomToBoundingBox(boundingBox, true, 80)
                        } catch (e: Exception) {}
                    }
                }
            )
        }
            
            // Fullscreen Indicator (Existing)
            Surface(
                color = Color.White.copy(0.9f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp)
            ) {
                IconButton(onClick = onMapClick) {
                    Icon(Icons.Default.Fullscreen, null, tint = AirPrimary, modifier = Modifier.size(24.dp))
                }
            }

            // 3D Toggle Button
            Surface(
                color = if (is3DMode) AirPrimary else Color.White.copy(0.9f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(40.dp)
            ) {
                IconButton(onClick = { is3DMode = !is3DMode }) {
                    Icon(
                        Icons.Rounded.Layers, // Using Layers icon for 3D switch
                        contentDescription = "Basculer 3D",
                        tint = if (is3DMode) Color.White else AirPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        
        if (points.isEmpty() && !is3DMode) {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Map, null, modifier = Modifier.size(56.dp), tint = AirTextLight)
                    Text("Carte non chargée (Tracé GPS manquant)", color = AirTextSecondary, fontWeight = FontWeight.Medium)
                    if (activity.summaryPolyline == null) {
                        Text("Synchronisez avec Strava pour récupérer la carte", fontSize = 11.sp, color = AirTextLight)
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryMetricsBanner(activity: Persistence.CompletedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AirSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val pace = if (activity.distanceKm > 0) activity.durationMin / activity.distanceKm else 0.0
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                Text("${String.format("%.2f", activity.distanceKm)} km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AirPrimary)
            }
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = AirSurface)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TEMPS", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                Text(formatDurationRefined(activity.durationMin), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AirPrimary)
            }
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = AirSurface)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ALLURE", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                // Compute Pace or Speed depending on type (e.g. Swim vs Run)
                if (activity.type == WorkoutType.SWIMMING) {
                    // For swimming, speed is m/s. 
                    // Calculate min/100m.
                    // Wait, pace is min/km. Pace/10 is min/100m. 
                    // formatDurationRefined takes minutes as Double? No, it takes Double minutes.
                    // But 1:30/100m is 1.5 min. 
                    // Let's manually format.
                    val pMin = pace / 10.0
                    val min = pMin.toInt()
                    val sec = ((pMin - min) * 60).toInt()
                    Text(String.format("%d:%02d/100m", min, sec), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AirPrimary)
                } else {
                    Text(formatPaceClean(pace), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = AirPrimary)
                }
            }
        }
    }
}

@Composable
fun SecondaryDataGrid(activity: Persistence.CompletedActivity, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null, onMetricClick: (String) -> Unit) {
    val science = remember(activity) { AnalysisEngine.calculateScience(activity, trainingPlan) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("DONNÉES SUPPLÉMENTAIRES", style = MaterialTheme.typography.labelSmall, color = AirTextLight, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataGridItem("Cardio", "${activity.avgHeartRate ?: "--"}", "BPM", Icons.Rounded.MonitorHeart, ZoneRed, Modifier.weight(1f)) { onMetricClick("FC") }
            
            // Charge (RSS/rTSS) - ALWAYS VISIBLE
            if (activity.avgWatts != null || activity.powerSamples.isNotEmpty()) {
                val ftpSub = science.rFtpW?.let { "FTP: ${it.toInt()}W" } ?: ""
                DataGridItem("Charge (RSS)", "${science.rss ?: 0}", "PTS", Icons.Rounded.Assessment, AirAccent, Modifier.weight(1f), subLabel = ftpSub) { onMetricClick("RSS") }
            } else {
                DataGridItem("Charge (rTSS)", "${science.rTss ?: 0}", "PTS", Icons.Rounded.Assessment, AirAccent, Modifier.weight(1f)) { onMetricClick("RTSS") }
            }

            if (activity.type == WorkoutType.SWIMMING && activity.totalStrokes != null) {
                DataGridItem("Mouvements", "${activity.totalStrokes}", "total", Icons.Rounded.Waves, ZoneOrange, Modifier.weight(1f))
            } else {
                DataGridItem("Dénivelé", "+${activity.elevationGain ?: 0}", "m", Icons.Rounded.Landscape, ZoneOrange, Modifier.weight(1f)) { onMetricClick("Altitude") }
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataGridItem("Cadence", "${activity.avgCadence ?: "--"}", "SPM", Icons.Rounded.DirectionsRun, AirSecondary, Modifier.weight(1f)) { onMetricClick("CADENCE") }
            
            if (activity.type == WorkoutType.SWIMMING && activity.swolf != null) {
                DataGridItem("SWOLF", "${activity.swolf}", "score", Icons.Rounded.Speed, AirAccent, Modifier.weight(1f))
            } else {
                DataGridItem("Puissance", "${activity.avgWatts ?: "--"}", "W", Icons.Rounded.Bolt, AirAccent, Modifier.weight(1f)) { onMetricClick("WATT") }
            }
            
            // VAM Moyenne or Temp
            val avgVam = remember(activity) {
                val vamSeries = AdvancedAnalytics.calculateVAMSeries(activity.elevationSamples)
                if (vamSeries.isNotEmpty()) vamSeries.map { it.second }.average() else null
            }
            if (avgVam != null && avgVam > 100) {
                DataGridItem("VAM Moy.", "${avgVam.toInt()}", "m/h", Icons.Rounded.TrendingUp, Color(0xFF00BCD4), Modifier.weight(1f)) { onMetricClick("VAM") }
            } else {
                DataGridItem("Temp.", "${activity.avgTemp?.toInt() ?: "--"}°", "C", Icons.Rounded.Thermostat, Color(0xFF00BCD4), Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // RPE / Feeling
            val rpeVal = activity.rpe ?: 0
            val (rpeLabel, rpeColor) = when(rpeVal) {
                in 1..3 -> "FACIL" to ZoneGreen
                in 4..6 -> "MODÉRÉ" to ZoneOrange
                in 7..8 -> "DUR" to ZoneRed
                in 9..10 -> "MAX" to Color(0xFFB71C1C)
                else -> "-" to AirTextLight
            }
            DataGridItem("Ressenti", if (rpeVal > 0) "$rpeVal/10" else "--", rpeLabel, Icons.Rounded.SentimentSatisfied, rpeColor, Modifier.weight(1f))
            
            // Show Temp here if VAM took its spot above
            val avgVamCheck = remember(activity) {
                val vamSeries = AdvancedAnalytics.calculateVAMSeries(activity.elevationSamples)
                if (vamSeries.isNotEmpty()) vamSeries.map { it.second }.average() else null
            }
            if (avgVamCheck != null && avgVamCheck > 100) {
                DataGridItem("Temp.", "${activity.avgTemp?.toInt() ?: "--"}°", "C", Icons.Rounded.Thermostat, Color(0xFF78909C), Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            Spacer(Modifier.weight(1f))
        }

        // Running Dynamics Row
        if (activity.strideLengthSamples.isNotEmpty() || activity.gctSamples.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                
                // Stride Length
                val avgStride = if (activity.strideLengthSamples.isNotEmpty()) activity.strideLengthSamples.map { it.value }.average() else 0.0
                val maxStride = if (activity.strideLengthSamples.isNotEmpty()) activity.strideLengthSamples.maxOf { it.value } else 0.0
                if (activity.strideLengthSamples.isNotEmpty()) {
                    DataGridItem(
                        "Foulée", 
                        String.format("%.2f", avgStride), 
                        "m", 
                        Icons.Rounded.Straighten, 
                        Color(0xFF6200EA), 
                        Modifier.weight(1f),
                        subLabel = "Max: ${String.format("%.2f", maxStride)}"
                    )
                } else {
                     Spacer(Modifier.weight(1f))
                }

                // GCT
                val avgGct = if (activity.gctSamples.isNotEmpty()) activity.gctSamples.map { it.value }.average() else 0.0
                val minGct = if (activity.gctSamples.isNotEmpty()) activity.gctSamples.minOf { it.value } else 0.0 // Min is better for GCT
                if (activity.gctSamples.isNotEmpty()) {
                    DataGridItem(
                        "GCT", 
                        "${avgGct.toInt()}", 
                        "ms", 
                        Icons.Rounded.Timer, 
                        Color(0xFFFF9800), 
                        Modifier.weight(1f),
                        subLabel = "Min: ${minGct.toInt()}" // Min contact time is "best" usually, unless sprinting
                    )
                } else {
                     Spacer(Modifier.weight(1f))
                }
                
                // Vertical Oscillation
                 val avgVo = if (activity.verticalOscillationSamples.isNotEmpty()) activity.verticalOscillationSamples.map { it.value }.average() else 0.0
                 val maxVo = if (activity.verticalOscillationSamples.isNotEmpty()) activity.verticalOscillationSamples.maxOf { it.value } else 0.0
                 if (activity.verticalOscillationSamples.isNotEmpty()) {
                    DataGridItem(
                        "Osc. Vert.", 
                        String.format("%.1f", avgVo), 
                        "cm", 
                        Icons.Rounded.Height, 
                        Color(0xFF039BE5), 
                        Modifier.weight(1f),
                        subLabel = "Max: ${String.format("%.1f", maxVo)}"
                    )
                } else {
                     Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DataGridItem(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, subLabel: String = "", onClick: (() -> Unit)? = null) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AirSurface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
                Spacer(Modifier.width(6.dp))
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontSize = 9.sp)
                if (onClick != null) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Info, null, modifier = Modifier.size(10.dp), tint = AirTextLight.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(2.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = AirTextLight, fontSize = 9.sp, modifier = Modifier.padding(bottom = 2.dp))
                
                if (subLabel.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Text(subLabel, style = MaterialTheme.typography.labelSmall, color = AirTextLight.copy(alpha = 0.7f), fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        }
    }
}

@Composable
fun SyncDetailsSection(activity: Persistence.CompletedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (activity.source.contains("Strava", true)) Icons.Default.Sync else Icons.Default.CloudDone
            val tint = if (activity.source.contains("Strava", true)) Color(0xFFFC4C02) else AirPrimary
            
            Surface(
                color = tint.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = tint)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Source de Synchronisation", style = MaterialTheme.typography.labelSmall, color = AirTextLight)
                Text(
                    text = if (activity.source == "DrawRun") "Application Native (DrawRun)" else activity.source,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AirTextPrimary
                )
            }
            Spacer(Modifier.weight(1f))
            if (activity.externalId != null) {
                Text("ID: ${activity.externalId.take(8)}...", style = MaterialTheme.typography.labelSmall, color = AirTextLight)
            }
        }
    }
}


@Composable
fun MetricPremiumCard(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
                Spacer(Modifier.width(8.dp))
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = AirTextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium, color = AirTextSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceInsightsSection(activity: Persistence.CompletedActivity, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null) {
    val insights = remember(activity, trainingPlan) { AnalysisEngine.analyze(activity, trainingPlan) }
    var selectedInsight by remember { mutableStateOf<Insight?>(null) }
    
    if (insights.isNotEmpty()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Analyses Physio & Conseils", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            insights.take(4).forEach { insight: Insight ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    onClick = { if (insight.adviceType != null) selectedInsight = insight },
                    colors = CardDefaults.cardColors(containerColor = if (insight.isPositive) AirPrimary.copy(0.05f) else ZoneRed.copy(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (insight.isPositive) AirPrimary.copy(0.1f) else ZoneRed.copy(0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(insight.icon, null, modifier = Modifier.size(16.dp), tint = if (insight.isPositive) AirPrimary else ZoneRed)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(insight.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(insight.content, style = MaterialTheme.typography.bodySmall, color = AirTextSecondary)
                        }
                        if (insight.adviceType != null) {
                            Icon(Icons.Default.ChevronRight, null, tint = AirTextSecondary.copy(0.3f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    
    selectedInsight?.let { insight ->
        AdviceDetailDialog(insight) { selectedInsight = null }
    }
}

@Composable
fun AdviceDetailDialog(insight: Insight, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(insight.icon, null, tint = AirPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(insight.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                
                Text(insight.content, style = MaterialTheme.typography.bodyLarge, color = AirTextPrimary)
                
                Spacer(Modifier.height(24.dp))
                
                // Visual Content based on AdviceType
                when(insight.adviceType) {
                    AdviceType.RECOVERY_STRETCH -> {
                        Column {
                            AdviceHeader("Routine Récupération", Icons.Default.Timer)
                            Spacer(Modifier.height(12.dp))
                            AdviceList(listOf(
                                "Étirement mollets : 30s statique",
                                "Massage plantaire (balle de tennis)",
                                "Hydratation (500ml eau riche en sels)",
                                "Repos total pendant 48h si douleur"
                            ))
                        }
                    }
                    else -> {}
                }



                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
                ) {
                    Text("J'ai compris")
                }
            }
        }
    }
}

@Composable
fun VisualAnalysisSection(activity: Persistence.CompletedActivity, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null, onMetricClick: (String?) -> Unit) {
    val syncState = rememberChartSyncState() // Hoist state for synchronization
    // Check for Swim
    val isSwim = activity.type == WorkoutType.SWIMMING
        
    Column(Modifier.padding(16.dp)) {
        Text("Performance Durant l'Activité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // 1. Individual Charts First
        if (activity.heartRateSamples.isNotEmpty()) {
            HrChart(activity.heartRateSamples, trainingPlan, state = syncState)
            Spacer(Modifier.height(16.dp))
        }

        if (activity.speedSamples.isNotEmpty()) {
            PaceChart(activity.speedSamples, trainingPlan, state = syncState, isSwim = isSwim)
            Spacer(Modifier.height(16.dp))
        }

        if (!isSwim && activity.powerSamples.isNotEmpty()) {
            PowerChart(activity.powerSamples, trainingPlan, state = syncState)
            Spacer(Modifier.height(16.dp))
        }

        if (!isSwim && activity.elevationSamples.isNotEmpty()) {
            AltitudeChart(activity.elevationSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
            
            val vamSeries = remember(activity) { AdvancedAnalytics.calculateVAMSeries(activity.elevationSamples) }
            if (vamSeries.isNotEmpty()) {
                VamChart(vamSeries, state = syncState)
                Spacer(Modifier.height(16.dp))
            }
        }

        val showGap = !isSwim && activity.type == WorkoutType.RUNNING // Gap only for run
        if (showGap && activity.elevationSamples.isNotEmpty()) {
            // Compute GAP (approx) or use logic
            val gapSeries = AdvancedAnalytics.calculateGAPSeries(activity.speedSamples, activity.elevationSamples) 
            if (gapSeries.isNotEmpty()) {
                GapChart(gapSeries, state = syncState)
                Spacer(Modifier.height(16.dp))
            }
        }

        if (activity.cadenceSamples.isNotEmpty()) {
            CadenceChart(activity.cadenceSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
        }

        // Running Dynamics Charts
        if (activity.strideLengthSamples.isNotEmpty()) {
            StrideLengthChart(activity.strideLengthSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
        }
        if (activity.gctSamples.isNotEmpty()) {
            GCTChart(activity.gctSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
        }
        if (activity.verticalOscillationSamples.isNotEmpty()) {
            VerticalOscillationChart(activity.verticalOscillationSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
        }
        if (activity.verticalRatioSamples.isNotEmpty()) {
            VerticalRatioChart(activity.verticalRatioSamples, state = syncState)
            Spacer(Modifier.height(16.dp))
        }

        // 2. Advanced Analytics (Quadrant/Aero)
        if (activity.type == WorkoutType.CYCLING && activity.powerSamples.isNotEmpty() && activity.cadenceSamples.isNotEmpty()) {
            val qPoints = remember(activity, trainingPlan) { 
                val ftpEstimate = trainingPlan?.powerZones?.getOrNull(3)?.minWatts?.toDouble() 
                    ?: (trainingPlan?.vma?.let { vma -> 
                        val weight = trainingPlan.userProfile.weightKg
                        (vma * 0.9 / 3.6 * weight * 0.98) 
                       } ?: 250.0)

                AdvancedAnalytics.calculateQuadrantAnalysis(
                    powerSamples = activity.powerSamples, 
                    cadenceSamples = activity.cadenceSamples,
                    ftp = ftpEstimate
                ) 
            }
            QuadrantAnalysisChart(qPoints) { onMetricClick(it) }
            Spacer(Modifier.height(16.dp))
        }

        if (activity.type == WorkoutType.CYCLING && activity.powerSamples.isNotEmpty()) {
             val aeroResult = remember(activity) {
                AdvancedAnalytics.calculateAeroLab(
                    activity.powerSamples, 
                    activity.speedSamples, 
                    activity.elevationSamples.map { it.avgAltitude.toDouble() }.ifEmpty { listOf(0.0) }
                )
            }
            AeroLabChart(aeroResult, state = syncState)
            Spacer(Modifier.height(16.dp))
        }

        // 3. Consolidated Overlay at the VERY END
        Text("Exploration Interactive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        InteractiveOverlaidChart(
            hrSamples = activity.heartRateSamples,
            speedSamples = activity.speedSamples,
            powerSamples = if (isSwim) emptyList() else activity.powerSamples,
            altSamples = if (isSwim) emptyList() else activity.elevationSamples,
            cadenceSamples = activity.cadenceSamples,
            trainingPlan = trainingPlan,
            vamSamples = if (isSwim) emptyList() else remember(activity) { AdvancedAnalytics.calculateVAMSeries(activity.elevationSamples) },
            gapSamples = if (isSwim) emptyList() else remember(activity) { AdvancedAnalytics.calculateGAPSeries(activity.speedSamples, activity.elevationSamples) },
            onMetricClick = { _ ->
                // Show educational dialog or handle click
            },
            state = syncState,
            isSwim = isSwim
        )
    }
}

@Composable
fun GapChart(gapSeries: List<Pair<Int, Double>>, state: ChartSyncState = rememberChartSyncState()) {
    
    val maxG = gapSeries.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val minG = gapSeries.minOfOrNull { it.second }?.toFloat() ?: 0f
    val rangeG = (maxG - minG).coerceAtLeast(0.1f)
    
    StreamChartContainer("ALLURE AJUSTÉE PENTE (GAP)", Color(0xFF673AB7)) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = gapSeries.size,
            yLabels = listOf(formatPace(maxG.toDouble() * 3.6) to Color(0xFF673AB7), formatPace(minG.toDouble() * 3.6) to Color(0xFF673AB7)),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (gapSeries.size - 1)).toInt().coerceIn(0, gapSeries.size - 1)
                    "${formatPace(gapSeries[idx].second * 3.6)} /km"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            gapSeries.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (gapSeries.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val y = h2 - ((s.second.toFloat() - minG) / rangeG) * h2
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color(0xFF673AB7), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun AltitudeChart(samples: List<Persistence.ElevationSample>, state: ChartSyncState = rememberChartSyncState()) {
    
    val minAlt = samples.minOfOrNull { it.avgAltitude }?.toFloat() ?: 0f
    val maxAlt = samples.maxOfOrNull { it.avgAltitude }?.toFloat() ?: 100f
    val altRange = (maxAlt - minAlt).coerceAtLeast(10f)

    StreamChartContainer("PROFIL D'ALTITUDE", ZoneOrange) { expanded ->
        val h = if (expanded) 240.dp else 150.dp
        
        StreamCanvas(
            height = h,
            _dataSize = samples.size,
            yLabels = listOf("${maxAlt.toInt()}m" to ZoneOrange, "${minAlt.toInt()}m" to ZoneOrange),
            state = state,
            scrubValue = { width, _ ->
                val sx = state.scrubX
                if (sx != null) {
                    val idx = (sx / width * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
                    "${samples[idx].avgAltitude.toInt()} m"
                } else null
            }
        ) { w, h2, zScale, zOffset ->
            val path = Path()
            path.moveTo(zOffset, h2)
            
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val normalizedY = 1f - ((s.avgAltitude.toFloat() - minAlt) / altRange)
                val y = normalizedY * h2
                path.lineTo(x, y)
            }
            val lastX = ((samples.size - 1).toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale + zOffset
            path.lineTo(lastX, h2)
            path.close()
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(ZoneOrange.copy(alpha = 0.5f), Color.Transparent),
                    startY = 0f,
                    endY = h2
                )
            )
            
            // Stroke
            val strokePath = Path()
            samples.forEachIndexed { i, s ->
                val x = ((i.toFloat() / (samples.size - 1).coerceAtLeast(1)) * w * zScale) + zOffset
                val normalizedY = 1f - ((s.avgAltitude.toFloat() - minAlt) / altRange)
                val y = normalizedY * h2
                if (i == 0) strokePath.moveTo(x, y) else strokePath.lineTo(x, y)
            }
            drawPath(strokePath, ZoneOrange, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun ProfessionalAnalysisSection(activity: Persistence.CompletedActivity, @Suppress("UNUSED_PARAMETER") trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null) {
    if (activity.type == WorkoutType.SWIMMING) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, null, tint = AirAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Laboratoire de Nage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AirTextPrimary)
            }
            Spacer(Modifier.height(16.dp))

            SwimmingAnalysisSection(activity)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SwimmingAnalysisSection(activity: Persistence.CompletedActivity) {
    val science = remember(activity) { AnalysisEngine.calculateScience(activity) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AirPrimary.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AirPrimary.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Waves, null, tint = AirPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Analyse de Nage Professionnelle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AirPrimary)
            }
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("SWOLF", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                    Text("${science.computedSwolf ?: "--"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text("INDEX (SI)", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                    Text(String.format("%.2f", science.strokeIndex ?: 0.0), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text("DPS", style = MaterialTheme.typography.labelSmall, color = AirTextSecondary)
                    Text(String.format("%.1f m", science.distancePerStroke ?: 0.0), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(science.swimEfficiencyText ?: "", style = MaterialTheme.typography.bodySmall, color = AirTextPrimary)
        }
    }
    
    science.strokeIndex?.let { si ->
        Spacer(Modifier.height(16.dp))
        val speedMs = if (activity.durationMin > 0) (activity.distanceKm * 1000) / (activity.durationMin * 60) else 0.0
        SwimEfficiencyChart(si, speedMs)
    }
}










@Composable
fun AdviceHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AirPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Black, fontSize = 13.sp, color = AirPrimary, letterSpacing = 0.5.sp)
    }
}

@Composable
fun AdviceList(items: List<String>) {
    items.forEach { item ->
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = ZoneGreen, modifier = Modifier.size(14.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text(item, style = MaterialTheme.typography.bodySmall, color = AirTextPrimary)
        }
    }
}


// Helper for display
data class EnrichedSplit(
    val split: Persistence.Split,
    val gapPace: Double?, // min/km
    val speedZone: String?,
    val powerZone: String?,
    val hrZone: String?,
    val vam: Double? = null 
)

@Composable
fun SplitsSection(activity: Persistence.CompletedActivity, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null) {
    // 1. Prepare Data (Splits + Derived Metrics for Table)
    val enrichedSplits = remember(activity, trainingPlan) {
        // Base Splits (from JSON or Recalculated)
        val baseSplits = if (activity.splits.isNotEmpty()) {
            activity.splits.sortedBy { it.kmIndex }
        } else {
            val dist = if (activity.type == WorkoutType.SWIMMING) (activity.distanceKm * 10).toInt() else activity.distanceKm.toInt()
            com.orbital.run.ui.generateConstantSplits(activity, dist)
        }
        
        // Enrich them
        baseSplits.mapIndexed { index, split ->
            // GAP Calculation
            // We need elevation change during this split.
            // Estimate start/end time of split based on duration accumulation
            // (Assuming splits are ordered and cover time sequentially)
            // Note: This is an estimation if we don't have absolute timestamps on splits, 
            // but usually splits are contiguous.
            val startTime = baseSplits.take(index).sumOf { it.durationSec }
            val endTime = startTime + split.durationSec
            
            // Elevation Change (End - Start)
            // Use samples if available
            val elevChange = if (activity.elevationSamples.isNotEmpty()) {
                val startAlt = activity.elevationSamples.minByOrNull { kotlin.math.abs(it.timeOffset - startTime) }?.avgAltitude
                val endAlt = activity.elevationSamples.minByOrNull { kotlin.math.abs(it.timeOffset - endTime) }?.avgAltitude
                if (startAlt != null && endAlt != null) endAlt - startAlt else 0.0
            } else 0.0
            
            // GAP Calculation (Grade Adjusted Pace)
            // Industry standard adjustments:
            // - Uphill: +15-20 seconds for every 10 meters of positive elevation gain
            // - Downhill: -5-10 seconds for every 10 meters of negative elevation gain
            // Using conservative middle values: +18s per 10m up, -8s per 10m down
            val paceVal = split.durationSec / 60.0 // pace in min/km
            val gap = if (elevChange != 0.0) {
                // Calculate adjustment in seconds
                val adjSec = if (elevChange > 0) {
                    // Uphill: add time (slower actual pace = faster equivalent flat pace)
                    // +18 seconds per 10 meters = 1.8 seconds per meter
                    elevChange * 1.8
                } else {
                    // Downhill: subtract time (faster actual pace = slower equivalent flat pace)
                    // -8 seconds per 10 meters = 0.8 seconds per meter
                    elevChange * 0.8 // negative * positive = negative value
                }
                // Subtract adjustment from pace (positive adj makes GAP faster/lower, negative makes it slower/higher)
                (paceVal - (adjSec / 60.0)).coerceIn(2.0, 15.0) // Clamp between 2:00 and 15:00 min/km
            } else paceVal
            
            // Zones lookup
            // Pace Zone (SpeedZone uses Kmh)
            val speedMps = if (split.durationSec > 0) 1000.0 / split.durationSec else 0.0
            val speedKmh = speedMps * 3.6
            val sZone = trainingPlan?.speedZones?.find { speedKmh >= it.minSpeedKmh && speedKmh <= it.maxSpeedKmh }?.id?.let { "Z$it" }
            
            // Power Zone (PowerZone uses minWatts/maxWatts)
            val pZone = if (split.avgWatts != null) {
                trainingPlan?.powerZones?.find { split.avgWatts >= it.minWatts && split.avgWatts <= it.maxWatts }?.id?.let { "Z$it" }
            } else null
            
            // HR Zone (HeartRateZone uses min/max)
            val hZone = if (split.avgHr != null) {
                trainingPlan?.hrZones?.find { split.avgHr >= it.min && split.avgHr <= it.max }?.label?.replace("Zone ", "Z")
            } else null
            
            // VAM Calculation for split
            val vam = if (elevChange > 0 && split.durationSec > 0) {
                (elevChange / split.durationSec) * 3600.0
            } else 0.0

            EnrichedSplit(split, gap, sZone, pZone, hZone, vam)
        }
    }

    if (enrichedSplits.isNotEmpty()) {
        val isSwim = activity.type == WorkoutType.SWIMMING
        val title = if (isSwim) "Détails par 100m" else "Détails par Kilomètre"
        
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            val hasWatts = enrichedSplits.any { it.split.avgWatts != null }
            val hasCadence = enrichedSplits.any { it.split.avgCadence != null }
            val hasGap = enrichedSplits.any { kotlin.math.abs((it.gapPace ?: 0.0) - (it.split.durationSec/60.0)) > 0.05 }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                // Horizontally Scrollable
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp)) {
                    Column {
                        // Header
                        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("KM", Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold)
                            Text("TEMPS", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold)
                            Text("ALLURE", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold)
                            
                            if (hasGap) Text("GAP", Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold)
                            
                            Text("Z.ALR", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            
                            Text("FC", Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            Text("Z.FC", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            
                            if (hasWatts) {
                                Text("W", Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Z.PWR", Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                            if (hasCadence) {
                                Text("CAD", Modifier.width(45.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                            Text("VAM", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, color = AirTextSecondary, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                        Divider(color = AirSurface, thickness = 1.dp)
                        
                        // Rows
                        enrichedSplits.forEachIndexed { index, item ->
                            val split = item.split
                            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // KM
                                Text("${split.kmIndex}", Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = AirTextPrimary)
                                
                                // Time
                                val min = split.durationSec / 60
                                val sec = split.durationSec % 60
                                Text(String.format("%d:%02d", min, sec), Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
                                
                                // Pace
                                val pace = split.durationSec / 60.0
                                Text(formatPaceClean(pace), Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                
                                // GAP
                                if (hasGap) {
                                    val gapP = item.gapPace ?: pace
                                    val isFaster = gapP < pace
                                    Text(
                                        formatPaceClean(gapP), 
                                        Modifier.width(60.dp), 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFaster) ZoneGreen else AirTextSecondary
                                    )
                                }
                                
                                // Z.VIT
                                val svColor = if (item.speedZone?.startsWith("Z") == true) getZoneColor(item.speedZone.last().digitToIntOrNull() ?: 1) else AirTextLight
                                Text(item.speedZone ?: "-", Modifier.width(40.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = svColor)
                                
                                // FC
                                Text("${split.avgHr ?: "-"}", Modifier.width(45.dp), textAlign = TextAlign.End, color = AirTextPrimary)
                                // Z.FC
                                val zcColor = if (item.hrZone?.startsWith("Z") == true) getZoneColor(item.hrZone.last().digitToIntOrNull() ?: 1) else AirTextLight
                                Text(item.hrZone ?: "-", Modifier.width(40.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold, color = zcColor, fontSize = 11.sp)

                                // Watts
                                if (hasWatts) {
                                    Text("${split.avgWatts ?: "-"}", Modifier.width(45.dp), textAlign = TextAlign.End, color = AirAccent)
                                    // Z.PWR
                                    val pColor = if (item.powerZone?.startsWith("Z") == true) getZoneColor(item.powerZone.last().digitToIntOrNull() ?: 1) else AirTextLight
                                    Text(item.powerZone ?: "-", Modifier.width(45.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = pColor, fontSize = 11.sp)
                                }
                                
                                // Cadence
                                if (hasCadence) {
                                    Text("${split.avgCadence ?: "-"}", Modifier.width(45.dp), textAlign = TextAlign.End, color = AirTextPrimary)
                                }

                                // VAM
                                val vamVal = item.vam ?: 0.0
                                Text("${vamVal.toInt()}", Modifier.width(50.dp), textAlign = TextAlign.End, color = if (vamVal > 800) ZoneRed else if (vamVal > 400) ZoneOrange else AirTextPrimary, fontWeight = if (vamVal > 0) FontWeight.Bold else FontWeight.Normal)
                            }
                            if (index < enrichedSplits.size - 1) Divider(color = AirSurface, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}





@Composable
fun FullScreenMapDialog(activity: Persistence.CompletedActivity, onDismiss: () -> Unit) {
    // Reuse MapLibreHero for Fullscreen as it supports interaction better for 3D
    // Or stick to 2D vs 3D choice? 
    // Let's allow switching inside Fullscreen too or just default to the current mode?
    // For simplicity, let's keep it simple: A dedicated Fullscreen Dialog that remembers state?
    // User requested "change mode of map".
    
    var is3DMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (is3DMode) {
                    MapLibreHero(activity = activity, isInteractive = true, onClose = onDismiss)
                    
                    // Toggle back to 2D
                     IconButton(
                        onClick = { is3DMode = false },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp, 80.dp, 16.dp, 16.dp).background(Color.White.copy(0.8f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Layers, "2D", tint = AirPrimary)
                    }

                } else {
                    // Regular OSM 2D Map
                    val points = remember(activity.summaryPolyline) {
                        if (activity.summaryPolyline.isNullOrEmpty()) emptyList()
                        else decodePolyline(activity.summaryPolyline)
                    }
                    
                    if (points.isNotEmpty()) {
                         AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(16.0)
                                }
                            },
                            update = { mapView ->
                                mapView.overlays.clear()
                                val geoPoints = points.map { GeoPoint(it.first, it.second) }
                                val line = Polyline().apply {
                                    setPoints(geoPoints)
                                    outlinePaint.color = android.graphics.Color.parseColor("#007AFF")
                                    outlinePaint.strokeWidth = 12f
                                }
                                mapView.overlays.add(line)
                                mapView.invalidate()
                                mapView.post {
                                    try {
                                        val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                                        mapView.zoomToBoundingBox(boundingBox, true, 80)
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
                    
                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(0.8f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = AirTextPrimary)
                    }
                    
                    // Toggle to 3D
                    IconButton(
                        onClick = { is3DMode = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).background(AirPrimary, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Layers, "3D", tint = Color.White)
                    }
                }
            }
        }
    }
}


@Composable
fun RunningQualitySection(activity: Persistence.CompletedActivity, trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null) {
    if (activity.type != WorkoutType.RUNNING) return
    
    val science = remember(activity) { AnalysisEngine.calculateScience(activity, trainingPlan) }
    var selectedMetric by remember { mutableStateOf<String?>(null) }
    
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = AirSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Qualité de Course", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AirTextPrimary)
        }
        Spacer(Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {


                // 1. Efficiency Factor (EF)
                QualityItem(
                    label = "Facteur d'Efficacité (EF)",
                    value = if(science.efficiencyFactor != null && science.efficiencyFactor > 0) String.format("%.2f", science.efficiencyFactor) else "--",
                    description = "Puissance aérobie (Mètres / BPM). Plus il est haut, plus vous êtes efficace.",
                    color = AirPrimary,
                    onClick = { selectedMetric = "EF" }
                )
                
                Divider(Modifier.padding(vertical = 16.dp), color = AirSurface, thickness = 1.dp)
                
                // 3. Running Effectiveness (RE)
                QualityItem(
                    label = "Running Effectiveness",
                    value = if(science.runningEffectiveness != null && science.runningEffectiveness > 0) String.format("%.2f", science.runningEffectiveness) else "--",
                    description = "Capacité à convertir la puissance en vitesse. Cible > 1.00.",
                    color = AirAccent,
                    onClick = { selectedMetric = "RE" }
                )

                if (science.enduranceIndex != null) {
                    Divider(Modifier.padding(vertical = 16.dp), color = AirSurface, thickness = 1.dp)
                    QualityItem(
                        label = "Indice d'Endurance (IE)",
                        value = String.format("%.1f", science.enduranceIndex),
                        description = "Indice de Péronnet. Cible > -8.0.",
                        color = Color(0xFF00B0FF),
                        onClick = { selectedMetric = "IE" }
                    )
                }
            }
        }
    }
    
    // Explanation Dialog
    selectedMetric?.let { metric ->
        MetricExplanationDialog(metric) { selectedMetric = null }
    }
}

@Composable
fun SocialContextSection(activity: Persistence.CompletedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AirWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Share, null, tint = AirPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Social & Partage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AirTextPrimary)
            }
            Spacer(Modifier.height(16.dp))

            // Latest Draws (Mock)
            val mockDrawers = listOf("Sophie D.", "Thomas B.", "Léa M.")
            if (mockDrawers.isNotEmpty()) {
                Text("Derniers Draws", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AirTextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Overlapping avatars
                    Box(Modifier.width((24 * mockDrawers.size + 12).dp)) {
                        mockDrawers.forEachIndexed { index, name ->
                            Box(
                                modifier = Modifier
                                    .padding(start = (index * 18).dp)
                                    .size(24.dp)
                                    .background(AirPrimary, CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.first().toString(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = "Bravo de ${mockDrawers.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AirTextPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Divider(Modifier.padding(vertical = 16.dp), color = AirSurface)
            }

            // Privacy Controls
            Text("Options de visibilité", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AirTextSecondary)
            Spacer(Modifier.height(8.dp))
            
            SharingToggle("Afficher sur le fil Social", true)
            SharingToggle("Inclure la carte", true)
            SharingToggle("Inclure les données cardiaques", false)
        }
    }
}

@Composable
fun SharingToggle(label: String, initialChecked: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = AirTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AirPrimary,
                uncheckedThumbColor = AirTextSecondary,
                uncheckedTrackColor = AirSurface
            ),
            modifier = Modifier.scale(0.7f)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityItem(label: String, value: String, description: String, color: Color, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AirTextPrimary)
                Spacer(Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = AirTextSecondary, lineHeight = 16.sp)
        }
    }
}


// Global Helpers (Restored)
fun formatDurationRefined(min: Int, sec: Int = 0): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) String.format("%dh %02dm", h, m) else String.format("%02d:%02d", m, sec)
}

fun formatPaceClean(minPerKm: Double): String {
    val m = minPerKm.toInt()
    val s = ((minPerKm - m) * 60).toInt()
    return String.format("%d'%02d\"", m, s)
}

fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
    val poly = ArrayList<Pair<Double, Double>>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(Pair(lat / 1E5, lng / 1E5))
    }
    return poly
}
