package com.orbital.run

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.orbital.run.ui.MainScreen
import com.orbital.run.ui.theme.AirSurface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Health Connect imports
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import android.util.Log

class MainActivity : ComponentActivity() {
    // Lazy initialization of Health Connect client
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(this) }

    // Required permissions set
    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    // Permission request launcher
    private val requestPermissions = registerForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(PERMISSIONS)) {
            readStepsData()
        } else {
            Log.e("HealthConnect", "Permissions missing")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Health Connect availability
        if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.e("HealthConnect", "Service unavailable on this device")
            // Continue without Health Connect features
        } else {
            checkAndRequestPermissions()
        }

        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AirSurface
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            if (uri.toString().startsWith("drawrun://strava_callback")) {
                com.orbital.run.api.StravaManager.handleAuthCallback(this, uri)
                // Clear intent to avoid re-triggering on rotate?
                // intent.data = null 
            }
        }
    }
    // Health Connect permission check
    private fun checkAndRequestPermissions() {
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                readStepsData()
            } else {
                // requestPermissions.launch(PERMISSIONS) 
                // Handled in SyncOnboardingScreen now
            }
        }
    }

    // Read steps data for today
    private fun readStepsData() {
        lifecycleScope.launch {
            try {
                val now = Instant.now()
                val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
                val response = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                    )
                )
                val steps = response[StepsRecord.COUNT_TOTAL] ?: 0
                Log.d("HealthConnect", "Pas aujourd'hui : $steps")
                // TODO: update UI with steps count
            } catch (e: Exception) {
                Log.e("HealthConnect", "Error reading steps: ${e.message}")
            }
        }
    }
}
