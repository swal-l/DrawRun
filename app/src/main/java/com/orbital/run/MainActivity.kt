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
        val data = intent?.data
        if (intent?.action == android.content.Intent.ACTION_VIEW && data != null) {
            // Check for both legacy/custom and standard http schemes
            val isStravaCallback = (data.scheme == "drawrun" && data.host == "strava_callback") ||
                                   (data.scheme == "http" && data.host == "localhost" && data.path?.startsWith("/strava_callback") == true)
            
            if (isStravaCallback) {
            val code = intent.data?.getQueryParameter("code")
            if (code != null) {
                lifecycleScope.launch {
                    val success = com.orbital.run.api.StravaAPI.exchangeToken(this@MainActivity, code)
                    if (success) {
                        android.widget.Toast.makeText(this@MainActivity, "Strava Connecté ! Redémarrage...", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Hard restart to ensure all states are refreshed and sync starts immediately
                        val restartIntent = android.content.Intent(this@MainActivity, com.orbital.run.MainActivity::class.java).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(restartIntent)
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "Echec Connexion Strava", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
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
