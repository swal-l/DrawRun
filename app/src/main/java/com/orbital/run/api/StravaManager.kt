package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object StravaManager {
    // Client ID/Secret are injected via BuildConfig from local.properties
    
    fun connect(context: Context) {
        val clientId = com.orbital.run.BuildConfig.STRAVA_CLIENT_ID
        // Use localhost as recommended for mobile apps to pass Strava's domain validation
        val redirectUri = "http://localhost/strava_callback" 
        val scope = "activity:read_all,activity:write"
        
        val url = "https://www.strava.com/oauth/mobile/authorize" +
                "?client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&response_type=code" +
                "&approval_prompt=auto" +
                "&scope=$scope"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        
        // Try to force opening in the Strava app if installed
        val stravaPackage = "com.strava"
        val pm = context.packageManager
        try {
            pm.getPackageInfo(stravaPackage, 0) // Check if installed
            intent.setPackage(stravaPackage)
        } catch (e: Exception) {
            // Strava not installed, fallback to browser (default behavior)
        }

        context.startActivity(intent)
    }

    /**
     * Handle the redirect from Strava.
     * URI format: http://localhost/strava_callback?state=&code=AUTHORIZATION_CODE&scope=...
     * OR drawrun://strava_callback (legacy)
     */
    fun handleAuthCallback(context: Context, uri: Uri) {
        // Accept both localhost (standard) and custom scheme (backup)
        val isStravaCallback = (uri.scheme == "http" && uri.host == "localhost" && uri.path?.startsWith("/strava_callback") == true) ||
                               (uri.toString().startsWith("drawrun://strava_callback"))

        if (isStravaCallback) {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            
            if (code != null) {
                // Success!
                Persistence.saveStravaEnabled(context, true)
                android.widget.Toast.makeText(context, "Strava connecté avec succès !", android.widget.Toast.LENGTH_LONG).show()
                Persistence.saveStravaAuthCode(context, code) // Save code if needed for future token exchange (though we are client-side here)
            } else if (error != null) {
                android.widget.Toast.makeText(context, "Erreur Strava: $error", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun disconnect(context: Context) {
        Persistence.saveStravaEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadStravaEnabled(context)
    }
    // --- Token Management ---
    private fun exchangeToken(context: Context, code: String): String? {
        val clientId = com.orbital.run.BuildConfig.STRAVA_CLIENT_ID
        val clientSecret = com.orbital.run.BuildConfig.STRAVA_CLIENT_SECRET
        
        return try {
            val url = java.net.URL("https://www.strava.com/oauth/token")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            
            val params = "client_id=$clientId&client_secret=$clientSecret&code=$code&grant_type=authorization_code"
            conn.outputStream.write(params.toByteArray())
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONObject(response)
                val access = json.getString("access_token")
                val refresh = json.getString("refresh_token")
                Persistence.saveStravaTokens(context, access, refresh)
                access
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun refreshToken(context: Context): String? {
        val (_, refreshToken) = Persistence.loadStravaTokens(context)
        if (refreshToken == null) return null
        
        val clientId = com.orbital.run.BuildConfig.STRAVA_CLIENT_ID
        val clientSecret = com.orbital.run.BuildConfig.STRAVA_CLIENT_SECRET
        
        return try {
            val url = java.net.URL("https://www.strava.com/oauth/token")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            
            val params = "client_id=$clientId&client_secret=$clientSecret&refresh_token=$refreshToken&grant_type=refresh_token"
            conn.outputStream.write(params.toByteArray())
            
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = org.json.JSONObject(response)
                val access = json.getString("access_token")
                val refresh = json.getString("refresh_token") // Strava might rotate refresh tokens
                Persistence.saveStravaTokens(context, access, refresh)
                access
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Sync Logic ---
    fun syncActivities(context: Context): Int {
        if (!isConnected(context)) return 0
        
        // 1. Get Valid Token
        var (accessToken, _) = Persistence.loadStravaTokens(context)
        
        // Initial Exchange (if no token but we have a code)
        if (accessToken == null) {
            val code = Persistence.loadStravaAuthCode(context) ?: return 0
            accessToken = exchangeToken(context, code)
        }
        
        if (accessToken == null) return 0
        
        fun doFetch(token: String): Int {
            try {
                // Fetch last 200 activities (Strava max per page)
                val url = java.net.URL("https://www.strava.com/api/v3/athlete/activities?per_page=200")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                
                if (conn.responseCode == 401) {
                    return -1 // Signal to refresh
                }
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonArray = org.json.JSONArray(response)
                    val activities = mutableListOf<Persistence.CompletedActivity>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        activities.add(mapStravaActivity(item))
                    }
                    
                    Persistence.saveHistoryBatch(context, activities)
                    return activities.size
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        }

        // 2. Attempt Fetch
        var result = doFetch(accessToken)
        
        // 3. Handle Expiration (401)
        if (result == -1) {
            val newToken = refreshToken(context)
            if (newToken != null) {
                result = doFetch(newToken)
            } else {
                return 0 // Failed to refresh
            }
        }
        
        return if (result == -1) 0 else result
    }

    private fun mapStravaActivity(json: org.json.JSONObject): Persistence.CompletedActivity {
        val id = json.getLong("id").toString()
        val name = json.optString("name", "Activité Strava")
        val distance = json.optDouble("distance", 0.0) / 1000.0
        val movingTime = json.optInt("moving_time", 0) / 60
        val typeStr = json.optString("type", "Run")
        
        // Date parsing (ISO 8601)
        val dateStr = json.optString("start_date")
        // Simple approximation or verify needed format
        val date = try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) { System.currentTimeMillis() }

        val type = when {
            typeStr.contains("Swim", true) -> com.orbital.run.logic.WorkoutType.SWIMMING
            typeStr.contains("Ride", true) || typeStr.contains("Cycle", true) -> com.orbital.run.logic.WorkoutType.CYCLING
            else -> com.orbital.run.logic.WorkoutType.RUNNING
        }
        
        val map = json.optJSONObject("map")
        val polyline = map?.optString("summary_polyline")

        return Persistence.CompletedActivity(
            id = "strava_$id", // Prefix to avoid collision unless we merge by externalId
            date = date,
            durationMin = movingTime,
            distanceKm = distance,
            type = type,
            title = name,
            source = "Strava",
            externalId = id,
            summaryPolyline = polyline,
            // Additional Metrics
            avgHeartRate = json.optDouble("average_heartrate").takeIf { !it.isNaN() }?.toInt(),
            maxHeartRate = json.optDouble("max_heartrate").takeIf { !it.isNaN() }?.toInt(),
            elevationGain = json.optDouble("total_elevation_gain").toInt(),
            avgWatts = json.optDouble("average_watts").takeIf { !it.isNaN() }?.toInt()
        )
    }
}
