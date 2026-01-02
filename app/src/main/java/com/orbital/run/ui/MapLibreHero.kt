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
                    mv.onCreate(null) // Bundle?
                    mv.getMapAsync { map ->
                        mapLibreMap = map
                        // Use Demo Tiles (no key required for testing 3D)
                        // This style includes terrain DEM for 3D
                        map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                            
                            // 1. Draw Polyline
                            // Using Annotation Plugin would be best, but robust manual add works too?
                            // Let's use standard addPolyline for simplicity if available or LineManager
                            
                            // Add Track Source & Layer (Manual GeoJSON)
                            // For simplicity in Composable, we often use LineManager (part of plugins)
                            // But standard SDK has addPolyline on Map object for simple cases
                            
                            // Draw Track
                            val points = decodedPoints.map { LatLng(it.first, it.second) }
                            
                            map.addPolyline(
                                org.maplibre.android.annotations.PolylineOptions()
                                    .addAll(points)
                                    .color(android.graphics.Color.BLUE) // Fix: Use Int Color
                                    .width(5f)
                            )
                            
                            // 2. Camera Position (3D tilted)
                            val bounds = LatLngBounds.Builder().includes(points).build()
                            
                            // Animate to bounds first
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000, object : MapLibreMap.CancelableCallback {
                                override fun onFinish() {
                                    // Then Tilt
                                    val currentTarget = map.cameraPosition.target
                                    val currentZoom = map.cameraPosition.zoom
                                    
                                    val tiltedPos = CameraPosition.Builder()
                                        .target(currentTarget)
                                        .zoom(currentZoom) // Keep zoom fitting bounds
                                        .tilt(60.0) // 60 degrees tilt for 3D effect
                                        .bearing(0.0)
                                        .build()
                                        
                                    map.animateCamera(CameraUpdateFactory.newCameraPosition(tiltedPos), 1000)
                                }
                                override fun onCancel() {}
                            })
                            
                            // Enable 3D Terrain if supported by style
                            // The demo style usually has 3D buildings or terrain enabled
                        }
                        
                        // UI Settings
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isTiltGesturesEnabled = isInteractive
                        map.uiSettings.isRotateGesturesEnabled = isInteractive
                        map.uiSettings.isZoomGesturesEnabled = isInteractive
                        map.uiSettings.isScrollGesturesEnabled = isInteractive
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
