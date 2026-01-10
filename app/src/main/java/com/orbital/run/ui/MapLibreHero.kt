package com.orbital.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.orbital.run.logic.Persistence
import com.orbital.run.ui.theme.AirPrimary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.utils.ColorUtils

@Composable
fun MapLibreHero(
    activity: Persistence.CompletedActivity,
    isInteractive: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize MapLibre (Safe to call multiple times)
    remember { MapLibre.getInstance(context) }

    val decodedPoints = remember(activity.summaryPolyline) {
        if (activity.summaryPolyline.isNullOrEmpty()) emptyList()
        else decodePolyline(activity.summaryPolyline)
    }

    if (decodedPoints.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.LightGray))
        return
    }

    // Hosted MapView state
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Lifecycle observer for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { mapView?.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView?.onPause() }
            override fun onStart(owner: LifecycleOwner) { mapView?.onStart() }
            override fun onStop(owner: LifecycleOwner) { mapView?.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView?.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Note: onDestroy is handled by lifecycle, but we clear refs
            mapView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mv.onCreate(null)
                    mv.getMapAsync { map ->
                        mapLibreMap = map
                        
                        // Use OpenFreeMap Liberty Style (Free, No Key, Nice OSM Data)
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                            
                            // 1. Setup 3D Territory (Relief/Topography)
                            // Using AWS Terrarium (Free global DEM)
                            /*
                            try {
                                val terrainSourceId = "aws-terrain-source"
                                val terrainSource = org.maplibre.android.style.sources.RasterDemSource(
                                    terrainSourceId,
                                    "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
                                ).apply {
                                    // withTileSize(256) // Method missing?
                                    // withEncoding(org.maplibre.android.style.sources.RasterDemSource.ENCODING_TERRARIUM)
                                }
                                style.addSource(terrainSource)
                                
                                // Enable Terrain
                                // val terrain = org.maplibre.android.style.layers.Terrain(terrainSourceId)
                                // terrain.exaggeration = 1.2f // Slight exaggeration for better effect
                                // style.terrain = terrain
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            */
                            
                            // 2. Draw Track Polyline
                            val points = decodedPoints.map { LatLng(it.first, it.second) }
                            
                            // Using standard addPolyline for simplicity and reliability
                            map.addPolyline(
                                org.maplibre.android.annotations.PolylineOptions()
                                    .addAll(points)
                                    .color(android.graphics.Color.parseColor("#007AFF")) // Electric Blue
                                    .width(6f)
                            )
                            
                            // 3. Camera Animation (3D Flyover)
                            if (points.isNotEmpty()) {
                                val bounds = LatLngBounds.Builder().includes(points).build()
                                
                                // Reset camera
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(bounds.center)
                                    .zoom(10.0)
                                    .tilt(0.0)
                                    .build()
                                
                                // Smooth animation sequence
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000, object : MapLibreMap.CancelableCallback {
                                    override fun onFinish() {
                                        // Then tilt and rotate slightly for 3D effect
                                        val currentTarget = map.cameraPosition.target
                                        val currentZoom = map.cameraPosition.zoom
                                        
                                        val tiltedPos = CameraPosition.Builder()
                                            .target(currentTarget)
                                            .zoom(currentZoom + 0.5) // Zoom in slightly
                                            .tilt(60.0) // Deep tilt to see mountains
                                            .bearing(45.0) // Diagonal angle
                                            .build()
                                            
                                        map.animateCamera(CameraUpdateFactory.newCameraPosition(tiltedPos), 2000)
                                    }
                                    override fun onCancel() {}
                                })
                            }
                            
                            // UI Settings
                            map.uiSettings.isAttributionEnabled = true // Keep attribution for generic OpenFreeMap/OSM
                            map.uiSettings.attributionGravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
                            map.uiSettings.setAttributionMargins(16, 0, 0, 16)
                            map.uiSettings.isLogoEnabled = false
                            map.uiSettings.isCompassEnabled = false
                            map.uiSettings.isTiltGesturesEnabled = isInteractive
                            map.uiSettings.isRotateGesturesEnabled = isInteractive
                            map.uiSettings.isZoomGesturesEnabled = isInteractive
                            map.uiSettings.isScrollGesturesEnabled = isInteractive
                        }
                    }
                    mapView = mv
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Fullscreen/Close Button for 3D Mode
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(0.8f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, "Fermer 3D", tint = AirPrimary)
            }
        }
        
        // 3D Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            androidx.compose.material3.Text("Mode 3D (MapLibre)", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
    }
}
