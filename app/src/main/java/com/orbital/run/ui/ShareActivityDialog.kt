package com.orbital.run.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orbital.run.logic.Persistence
import com.orbital.run.ui.theme.*
import com.orbital.run.logic.AdvancedAnalytics
import com.orbital.run.logic.AnalysisEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.BitmapFactory
import android.graphics.RectF

enum class ShareTemplate { MINIMAL, PROFESSIONAL, ADVENTURE }
enum class ShareBackground { GRADIENT, MAP, COLOR }

data class ShareSettings(
    val template: ShareTemplate = ShareTemplate.PROFESSIONAL,
    val background: ShareBackground = ShareBackground.GRADIENT,
    val bgColor: Color = Color(0xFF0277BD),
    val showMap: Boolean = true,
    val metrics: List<String> = listOf("DISTANCE", "TIME", "PACE", "HR", "POWER", "RSS")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareActivityDialog(
    activity: Persistence.CompletedActivity,
    trainingPlan: com.orbital.run.logic.TrainingPlanResult? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var settings by remember { mutableStateOf(ShareSettings()) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    // Generate preview whenever settings change
    LaunchedEffect(settings) {
        isGenerating = true
        generatedBitmap = generateAdvancedShareImage(context, activity, trainingPlan, settings)
        isGenerating = false
    }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top: Preview (50%)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF8F9FA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = AirPrimary)
                    } else if (generatedBitmap != null) {
                        Image(
                            bitmap = generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Bottom: Settings (50%)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text("Personnaliser", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(12.dp))

                    // 1. Templates
                    ShareSectionHeader("TEMPLATE")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShareTemplate.values().forEach { tmpl ->
                            FilterChip(
                                selected = settings.template == tmpl,
                                onClick = { settings = settings.copy(template = tmpl) },
                                label = { Text(tmpl.name, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 2. Background
                    ShareSectionHeader("FOND")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShareBackground.values().forEach { bg ->
                            FilterChip(
                                selected = settings.background == bg,
                                onClick = { settings = settings.copy(background = bg) },
                                label = { Text(bg.name, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (settings.background == ShareBackground.GRADIENT || settings.background == ShareBackground.COLOR) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Color(0xFF0277BD), Color(0xFFD32F2F), Color(0xFF388E3C),
                                Color(0xFF7B1FA2), Color(0xFFFF6F00), Color(0xFF212121),
                                Color(0xFF00BFA5), Color(0xFFEC407A)
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(if (settings.bgColor == color) 2.dp else 0.dp, AirPrimary, CircleShape)
                                        .clickable { settings = settings.copy(bgColor = color) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3. Metrics
                    ShareSectionHeader("MÉTRIQUES")
                    val availableMetrics = listOf("DISTANCE", "TIME", "PACE", "HR", "POWER", "RSS")
                    FlowRow(mainAxisSpacing = 4.dp, crossAxisSpacing = 4.dp) {
                        availableMetrics.forEach { metric ->
                            val isSelected = settings.metrics.contains(metric)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newList = if (isSelected) settings.metrics - metric else settings.metrics + metric
                                    settings = settings.copy(metrics = newList)
                                },
                                label = { Text(metric, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(16.dp))

                    // Share Button
                    Button(
                        onClick = {
                            scope.launch {
                                generatedBitmap?.let { bmp ->
                                    shareSimpleBitmap(context, bmp)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
                        shape = RoundedCornerShape(16.dp),
                        enabled = generatedBitmap != null && !isGenerating
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PARTAGER", fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Annuler", color = AirTextLight)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareSectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = AirTextSecondary, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
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

        val totalHeight = rows.sumOf { r -> r.maxOf { it.height } } + (rows.size - 1) * crossAxisSpacing.roundToPx()
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val maxHeight = row.maxOf { it.height }
                row.forEach { p ->
                    p.place(x, y)
                    x += p.width + mainAxisSpacing.roundToPx()
                }
                y += maxHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

suspend fun generateAdvancedShareImage(
    context: Context, 
    activity: Persistence.CompletedActivity, 
    trainingPlan: com.orbital.run.logic.TrainingPlanResult?,
    settings: ShareSettings
): Bitmap? {
    return try {
        withContext(Dispatchers.Default) {
            val width = 1080
            val height = 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Background
            drawModernBackground(canvas, activity, width, height, settings)

            // 2. Content Overlay based on Template
            when (settings.template) {
                ShareTemplate.MINIMAL -> drawMinimalTemplate(canvas, activity, width, height, settings)
                ShareTemplate.PROFESSIONAL -> drawProfessionalTemplate(canvas, activity, trainingPlan, width, height, settings)
                ShareTemplate.ADVENTURE -> drawAdventureTemplate(canvas, activity, width, height, settings)
            }

            // 3. Brand Footer
            drawBrandFooter(canvas, width, height)

            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun drawModernBackground(canvas: Canvas, activity: Persistence.CompletedActivity, width: Int, height: Int, settings: ShareSettings) {
    val bgPaint = Paint().apply { isAntiAlias = true }
    
    when (settings.background) {
        ShareBackground.COLOR -> {
            canvas.drawColor(settings.bgColor.hashCode())
        }
        ShareBackground.GRADIENT -> {
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(settings.bgColor.hashCode(), AndroidColor.BLACK),
                null, Shader.TileMode.CLAMP
            )
            bgPaint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }
        ShareBackground.MAP -> {
            drawMapBackground(canvas, activity, width, height)
            // Add dark overlay for readability
            val overlayPaint = Paint().apply {
                color = AndroidColor.argb(120, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        }
    }
}

fun drawMinimalTemplate(canvas: Canvas, activity: Persistence.CompletedActivity, width: Int, height: Int, settings: ShareSettings) {
    val paint = Paint().apply {
        color = AndroidColor.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Centered Distance
    paint.textSize = 240f
    val distStr = String.format("%.2f", activity.distanceKm)
    canvas.drawText(distStr, width / 2f, height / 2f + 40f, paint)

    paint.textSize = 60f
    paint.typeface = Typeface.DEFAULT
    paint.letterSpacing = 0.1f
    canvas.drawText("KILOMÈTRES", width / 2f, height / 2f + 120f, paint)

    // Secondary line
    paint.textSize = 45f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val timeStr = shareFormatDuration(activity.durationMin)
    val paceMinPerKm = if (activity.distanceKm > 0) activity.durationMin / activity.distanceKm else 0.0
    val paceStr = shareFormatPace(paceMinPerKm)
    canvas.drawText("$timeStr  •  $paceStr/km", width / 2f, height / 2f + 200f, paint)
}

fun drawProfessionalTemplate(
    canvas: Canvas, 
    activity: Persistence.CompletedActivity, 
    trainingPlan: com.orbital.run.logic.TrainingPlanResult?,
    width: Int, 
    height: Int, 
    settings: ShareSettings
) {
    val paint = Paint().apply {
        color = AndroidColor.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    // Activity Title & Date
    paint.textSize = 40f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(activity.title.uppercase(), 80f, 100f, paint)
    
    paint.textSize = 30f
    paint.color = AndroidColor.argb(180, 255, 255, 255)
    val dateStr = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.FRENCH).format(java.util.Date(activity.date))
    canvas.drawText(dateStr, 80f, 145f, paint)

    // Grid of metrics
    val science = AnalysisEngine.calculateScience(activity, trainingPlan)
    val list = mutableListOf<Pair<String, String>>()
    if (settings.metrics.contains("DISTANCE")) list.add("DISTANCE" to "${String.format("%.2f", activity.distanceKm)} km")
    if (settings.metrics.contains("TIME")) list.add("TEMPS" to shareFormatDuration(activity.durationMin))
    if (settings.metrics.contains("PACE")) {
        val pace = if (activity.distanceKm > 0) activity.durationMin / activity.distanceKm else 0.0
        list.add("ALLURE" to "${shareFormatPace(pace)}/km")
    }
    if (settings.metrics.contains("HR")) list.add("CARDIO" to "${activity.avgHeartRate ?: "--"} bpm")
    if (settings.metrics.contains("POWER")) list.add("PUISSANCE" to "${activity.avgWatts ?: "--"} W")
    if (settings.metrics.contains("RSS")) {
        val rss = science.rss ?: science.rTss ?: 0
        list.add("STRESS (RSS)" to "$rss pts")
    }

    // Draw Grid
    val colWidth = (width - 160f) / 2
    val rowHeight = 180f
    val startY = 250f

    list.forEachIndexed { i, (label, value) ->
        val col = i % 2
        val row = i / 2
        val x = 80f + col * colWidth
        val y = startY + row * rowHeight

        // Background card effect (glassmorphism style)
        val cardPaint = Paint().apply {
            color = AndroidColor.argb(40, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + colWidth - 20f, y + rowHeight - 40f), 24f, 24f, cardPaint)

        paint.color = AndroidColor.argb(200, 255, 255, 255)
        paint.textSize = 28f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(label, x + 30f, y + 50f, paint)

        paint.color = AndroidColor.WHITE
        paint.textSize = 48f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 30f, y + 110f, paint)
    }
}

fun drawAdventureTemplate(canvas: Canvas, activity: Persistence.CompletedActivity, width: Int, height: Int, settings: ShareSettings) {
    // Already has map background if selected
    // Just add main stats at bottom in a slick bar
    val barPaint = Paint().apply {
        color = AndroidColor.argb(200, 0, 0, 0)
        isAntiAlias = true
    }
    
    val barHeight = 220f
    canvas.drawRect(0f, height - barHeight - 120f, width.toFloat(), height - 120f, barPaint)

    val paint = Paint().apply {
        color = AndroidColor.WHITE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val stats = listOf(
        "${String.format("%.2f", activity.distanceKm)} KM",
        shareFormatDuration(activity.durationMin),
        "+${activity.elevationGain ?: 0} M"
    )

    val segmentWidth = width / 3f
    stats.forEachIndexed { i, text ->
        paint.textSize = 42f
        canvas.drawText(text, segmentWidth * i + segmentWidth / 2f, height - barHeight / 2f - 110f, paint)
    }
}

fun drawBrandFooter(canvas: Canvas, width: Int, height: Int) {
    val paint = Paint().apply {
        color = AndroidColor.WHITE
        isAntiAlias = true
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    // Draw "O R B I T A L" with spacing
    val brand = "O R B I T A L   B E L T"
    paint.textAlign = Paint.Align.CENTER
    paint.letterSpacing = 0.4f
    canvas.drawText(brand, width / 2f, height - 60f, paint)
}

private fun shareFormatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "${m}min"
}

private fun shareFormatPace(paceMinPerKm: Double): String {
    val min = paceMinPerKm.toInt()
    val sec = ((paceMinPerKm - min) * 60).toInt()
    return "$min:${sec.toString().padStart(2, '0')}"
}

fun drawMapBackground(canvas: Canvas, activity: Persistence.CompletedActivity, width: Int, height: Int) {
    // Dark map-like background
    canvas.drawColor(android.graphics.Color.parseColor("#1a1a1a"))
    
    // Decode polyline - use existing function from ActivityDetailScreen
    val points = com.orbital.run.ui.decodePolyline(activity.summaryPolyline ?: "")
    if (points.isEmpty()) return
    
    // Calculate bounds
    var minLat = 90.0; var maxLat = -90.0
    var minLng = 180.0; var maxLng = -180.0
    for (p in points) {
        minLat = minOf(minLat, p.first)
        maxLat = maxOf(maxLat, p.first)
        minLng = minOf(minLng, p.second)
        maxLng = maxOf(maxLng, p.second)
    }
    
    // Add padding to bounds
    val latPadding = (maxLat - minLat) * 0.1
    val lngPadding = (maxLng - minLng) * 0.1
    minLat -= latPadding; maxLat += latPadding
    minLng -= lngPadding; maxLng += lngPadding
    
    // Draw route
    val routePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#00BFA5")
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }
    
    val path = android.graphics.Path()
    points.forEachIndexed { i, (lat, lng) ->
        val x = ((lng - minLng) / (maxLng - minLng) * width * 0.8 + width * 0.1).toFloat()
        val y = ((maxLat - lat) / (maxLat - minLat) * height * 0.6 + height * 0.2).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, routePaint)
}

fun shareSimpleBitmap(context: Context, bitmap: Bitmap) {
    try {
        val path = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "DrawRun_${System.currentTimeMillis()}",
            "Shared from DrawRun"
        )
        val uri = Uri.parse(path)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager avec..."))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
