package com.orbital.run.social

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orbital.run.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// Color Constants for Premium Feel
private val SocialCardBg = Color(0xFF1E2129)
private val AccentGold = Color(0xFFFFD700)
private val AccentTeal = Color(0xFF00E5FF)

@Composable
fun SocialScreen(
    viewModel: SocialViewModel = viewModel()
) {
    val feed by viewModel.feed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirSurface) // Dark theme background
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Communauté",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(feed) { activity ->
                SocialActivityCard(
                    activity = activity,
                    onDraw = { viewModel.onDraw(activity.id) }
                )
            }
        }
    }
}

@Composable
fun SocialActivityCard(
    activity: SocialActivity,
    onDraw: () -> Unit
) {
    var showDrawExplosion by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SocialCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDraw()
                            showDrawExplosion = true
                        }
                    )
                }
        ) {
            // Header: User Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentTeal, AirPrimary)))
                ) {
                    Text(
                        text = activity.user.name.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activity.user.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (activity.user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = formatTimeAgo(activity.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Content: Title & Stats
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (activity.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB0B3B8)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBadge(
                        value = "${activity.distanceKm}",
                        unit = "km",
                        label = "Distance"
                    )
                    StatBadge(
                        value = formatDuration(activity.durationMin),
                        unit = "",
                        label = "Durée"
                    )
                    StatBadge(
                        value = activity.paceMinPerKm ?: "--",
                        unit = "",
                        label = "Allure"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Map / Visual Placeholder (Gradient for now)
            // Map / Visual Placeholder (Gradient for now)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF2B303B))
            ) {
                 // Enhanced Pseudo-Map
                 Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Dark Map Background (Grid lines)
                    val width = size.width
                    val height = size.height
                    val gridC = Color.White.copy(alpha = 0.05f)
                    val step = 50.dp.toPx()
                    
                    // Vertical Grid
                    var x = 0f
                    while (x < width) {
                        drawLine(gridC, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                        x += step
                    }
                    // Horizontal Grid
                    var y = 0f
                    while (y < height) {
                        drawLine(gridC, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                        y += step
                    }

                    // 2. The "Route" (Gradient Path)
                    // Generate a random-looking path based on activity ID hash to be consistent
                    val seed = activity.id.hashCode()
                    val pathColor = Brush.linearGradient(
                        colors = listOf(AccentTeal, AccentGold),
                        start = Offset(0f, height),
                        end = Offset(width, 0f)
                    )
                    
                    val path = androidx.compose.ui.graphics.Path()
                    // Start roughly bottom left
                    var currentX = width * 0.1f
                    var currentY = height * 0.8f
                    path.moveTo(currentX, currentY)
                    
                    // Generate 4-5 segments
                    val random = java.util.Random(seed.toLong())
                    for (i in 0..4) {
                        val nextX = currentX + (width * 0.8f / 5) + (random.nextFloat() - 0.5f) * width * 0.1f
                        val nextY = currentY + (random.nextFloat() - 0.5f) * height * 0.4f - (height * 0.1f) // Tend upwards
                        
                        // Bezier for smoothness
                        val c1x = currentX + (nextX - currentX) / 2
                        val c1y = currentY
                        val c2x = currentX + (nextX - currentX) / 2
                        val c2y = nextY
                        
                        path.cubicTo(c1x, c1y, c2x, c2y, nextX, nextY)
                        
                        currentX = nextX
                        currentY = nextY
                    }
                    
                    // Stroke
                    drawPath(
                        path = path,
                        brush = pathColor,
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                    
                    // Start/End Points
                    drawCircle(AccentTeal, 6f, Offset(width * 0.1f, height * 0.8f))
                    drawCircle(AccentGold, 6f, Offset(currentX, currentY))
                 }
                 
                 // Overlay info
                 if (showDrawExplosion) {
                     DrawExplosion(modifier = Modifier.align(Alignment.Center)) {
                         showDrawExplosion = false
                     }
                 }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Draw" Button (Like)
                val drawColor = if (activity.isDrawnByMe) AccentGold else Color.Gray
                
                // Animated Scale for button
                val scale by animateFloatAsState(
                    targetValue = if (activity.isDrawnByMe) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                IconButton(onClick = { 
                    onDraw() 
                    if (!activity.isDrawnByMe) showDrawExplosion = true
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit, // Pen/Draw icon
                        contentDescription = "Draw",
                        tint = drawColor,
                        modifier = Modifier.scale(scale)
                    )
                }
                Text(
                    text = "${activity.drawCount} Draws",
                    color = drawColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Rounded.Comment,
                    contentDescription = "Comments",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${activity.commentCount}",
                    color = Color.Gray
                )
            }
            
            // Comment Input Section (Functional UI)
            Divider(color = Color.White.copy(alpha=0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(1.dp, Color.White.copy(alpha=0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ){
                   Text("M", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) // Me
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Ajouter un commentaire...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatBadge(value: String, unit: String, label: String) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun DrawExplosion(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    // A simple one-shot animation of particles
    val particles = remember { List(8) { Random.nextFloat() to Random.nextFloat() } }
    val anim = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(600))
        onFinished()
    }
    
    Canvas(modifier = modifier.size(100.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * anim.value
        
        // Draw Pen Icon shape (simplified as a stroke)
        if (anim.value < 0.5f) {
           drawCircle(AccentGold, radius = radius * 0.5f, center = center, alpha = 1f - anim.value * 2)
        }

        // Particles
        particles.forEach { (angleBase, speedBase) ->
            val angle = angleBase * 360f
            val dist = radius * (0.8f + speedBase * 0.4f)
            val rad = Math.toRadians(angle.toDouble())
            val x = center.x + (dist * Math.cos(rad)).toFloat()
            val y = center.y + (dist * Math.sin(rad)).toFloat()
            
            drawCircle(
                color = AccentGold,
                radius = 8f * (1f - anim.value),
                center = Offset(x, y)
            )
        }
    }
}

private fun formatTimeAgo(instant: java.time.Instant): String {
    // Simplified formatter
    val now = java.time.Instant.now()
    val diff = java.time.Duration.between(instant, now)
    return when {
        diff.toMinutes() < 60 -> "Il y a ${diff.toMinutes()} min"
        diff.toHours() < 24 -> "Il y a ${diff.toHours()} h"
        else -> "Il y a ${diff.toDays()} j"
    }
}

private fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "${m}min"
}
