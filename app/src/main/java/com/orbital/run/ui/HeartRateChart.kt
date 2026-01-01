package com.orbital.run.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.run.logic.Persistence
import com.orbital.run.ui.theme.*

// Heart Rate Chart Composable for detailed HR visualization
@Composable
fun HeartRateChart(samples: List<Persistence.HeartRateSample>) {
    if (samples.isEmpty()) {
        Text(
            "Pas de données HR détaillées disponibles",
            fontSize = 12.sp,
            color = AirTextLight,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val maxBpm = samples.maxOf { it.bpm }.toFloat()
            val minBpm = samples.minOf { it.bpm }.toFloat()
            val range = maxBpm - minBpm
            
            if (range == 0f) return@Canvas
            
            // Create path for HR line
            val path = androidx.compose.ui.graphics.Path()
            samples.forEachIndexed { index, sample ->
                val x = (index.toFloat() / samples.size) * size.width
                val y = size.height - ((sample.bpm - minBpm) / range) * size.height
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Draw HR line
            drawPath(
                path = path,
                color = androidx.compose.ui.graphics.Color.Red,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            // Draw zone background colors (optional)
            // Zone 1 (60%): Blue, Zone 2 (70%): Green, etc.
        }
        
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Début: ${samples.first().bpm} bpm",
                fontSize = 10.sp,
                color = AirTextLight
            )
            Text(
                "Max: ${samples.maxOf { it.bpm }} bpm",
                fontSize = 10.sp,
                color = androidx.compose.ui.graphics.Color.Red,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Fin: ${samples.last().bpm} bpm",
                fontSize = 10.sp,
                color = AirTextLight
            )
        }
    }
}
