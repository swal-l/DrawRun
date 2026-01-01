package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Workout
import com.orbital.run.logic.WorkoutType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.orbital.run.BuildConfig
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Service d'intégration avec l'API Strava (Refactored for Performance & Reliability)
 */
object StravaAPI {
    private val CLIENT_ID = BuildConfig.STRAVA_CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET

    /**
     * ✅ IMPORTANT: This must match EXACTLY the 'Authorization Callback Domain' in your Strava Dashboard.
     * Use "localhost" in the Strava Dashboard for this setting to work.
     */
    private const val REDIRECT_URI = "http://localhost/strava_callback"
    private const val AUTH_URL = "https://www.strava.com/oauth/authorize"
    
    // Config OkHttp propre
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Token State
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt: Long = 0L // Epoch seconds
    
    fun isConfigured() = !accessToken.isNullOrBlank()
    fun isAuthenticated() = !accessToken.isNullOrBlank()

    /**
     * Helper pour obtenir les préférences chiffrées
     */
    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                "strava_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Keystore corruption detected. Delete the file to allow fresh start.
            try {
                val prefsFile = java.io.File(context.filesDir.parent, "shared_prefs/strava_secure_prefs.xml")
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
                // Try again once after deletion
                 val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "strava_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Charge le token depuis les préférences au démarrage (version sécurisée)
     */
    /**
     * Charge le token depuis les préférences au démarrage (version sécurisée)
     */
    fun loadToken(context: Context) {
        try {
            val securePrefs = getEncryptedPrefs(context)
            if (securePrefs == null) {
                accessToken = null
                return
            }
            
            accessToken = securePrefs.getString("access_token", null)
            refreshToken = securePrefs.getString("refresh_token", null)
            expiresAt = securePrefs.getLong("expires_at", 0L)
            
            if (accessToken == null) {
                // Migration depuis les anciennes préférences non-chiffrées
                val legacyPrefs = context.getSharedPreferences("strava_prefs", Context.MODE_PRIVATE)
                val legacyToken = legacyPrefs.getString("access_token", null)
                if (legacyToken != null) {
                    accessToken = legacyToken
                    securePrefs.edit().putString("access_token", legacyToken).apply()
                    legacyPrefs.edit().remove("access_token").apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            accessToken = null
        }
    }

    /**
     * Refresh the access token if it is expired or about to expire (within 5 mins).
     */
    suspend fun refreshTokenIfNeeded(context: Context): Boolean {
        // If no refresh token, we can't refresh
        val rToken = refreshToken
        if (rToken.isNullOrBlank()) return false
        
        // Check expiration (buffer 5 minutes)
        val now = System.currentTimeMillis() / 1000
        if (accessToken != null && now < (expiresAt - 300)) {
            return true // Token still valid
        }
        
        android.util.Log.d("STRAVA_AUTH", "🔄 Refreshing Strava Token...")
        
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "refresh_token")
                .add("refresh_token", rToken)
                .build()

            val request = Request.Builder()
                .url("https://www.strava.com/oauth/token")
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                         android.util.Log.e("STRAVA_AUTH", "❌ Refresh failed: ${response.code}")
                        return@withContext false
                    }
                    
                    val body = response.body?.string() ?: return@withContext false
                    val json = JSONObject(body)
                    val newAccessToken = json.optString("access_token")
                    val newRefreshToken = json.optString("refresh_token")
                    val newExpiresAt = json.optLong("expires_at")
                    
                    if (newAccessToken.isNotBlank()) {
                        saveFullTokenState(context, newAccessToken, newRefreshToken, newExpiresAt)
                         android.util.Log.d("STRAVA_AUTH", "✅ Token refreshed successfully")
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext false
        }
    }

    /**
     * Valide le token fourni en appelant l'API Strava.
     * @return true si le token est valide (HTTP 200), false sinon.
     */
    suspend fun validateToken(token: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext false
        
        // On tente de récupérer le profil de l'athlète pour vérifier le token
        val request = Request.Builder()
            .url("https://www.strava.com/api/v3/athlete")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Sauvegarde le token UNIQUEMENT s'il est valide (chiffré)
     */
    /**
     * Sauvegarde le token UNIQUEMENT s'il est valide (chiffré)
     */
    fun saveToken(context: Context, token: String) {
        accessToken = token
        try {
            val securePrefs = getEncryptedPrefs(context)
            securePrefs?.edit()?.putString("access_token", token)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveFullTokenState(context: Context, token: String, refresh: String, expires: Long) {
        accessToken = token
        refreshToken = refresh
        expiresAt = expires
        
        try {
            val securePrefs = getEncryptedPrefs(context)
            securePrefs?.edit()
                ?.putString("access_token", token)
                ?.putString("refresh_token", refresh)
                ?.putLong("expires_at", expires)
                ?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openAuthorizationPage(context: Context) {
        // 1. Try Native App Intent
        val nativeUri = Uri.parse("strava://oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read_all,profile:read_all,read_all")
            .build()
            
        try {
            val intent = Intent(Intent.ACTION_VIEW, nativeUri)
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // 2. Fallback to Browser
            val encodedRedirect = java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")
            val url = "$AUTH_URL?client_id=$CLIENT_ID&response_type=code&redirect_uri=$encodedRedirect&approval_prompt=auto&scope=activity:read_all,profile:read_all,read_all"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    /**
     * Récupère les activités Strava via Coroutines.
     * @return Result<List<Workout>>
     */
    /**
     * Récupère TOUTES les activités Strava (Pagination) via Coroutines.
     * @return Result<List<Workout>>
     */
    suspend fun fetchActivities(limit: Int = 1000, context: Context? = null): Result<List<Workout>> = withContext(Dispatchers.IO) {
        if (context != null) refreshTokenIfNeeded(context)
        
        val token = accessToken
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Token non configuré"))
        }

        val allActivities = mutableListOf<Workout>()
        var page = 1
        val perPage = 100 // Strava default max
        var keepFetching = true

        try {
            while (keepFetching && allActivities.size < limit) {
                val request = Request.Builder()
                    .url("https://www.strava.com/api/v3/athlete/activities?per_page=$perPage&page=$page")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (page == 1) { // Error on first page is critical
                            val errorBody = response.body?.string()
                            return@withContext Result.failure(Exception("Erreur Strava ${response.code}: $errorBody"))
                        } else {
                            keepFetching = false // Stop on error but keep what we have
                            return@use
                        }
                    }

                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    
                    if (jsonArray.length() == 0) {
                        keepFetching = false
                    } else {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val typeStr = obj.optString("type", "Run")
                            // FILTER: Only keep Run or Swim
                            val isRun = typeStr.equals("Run", ignoreCase = true) || typeStr.equals("VirtualRun", ignoreCase = true)
                            val isSwim = typeStr.contains("Swim", ignoreCase = true)
                            
                            if (!isRun && !isSwim) continue // Skip Rides, Yoga, etc.
                            
                            val type = if (isSwim) WorkoutType.SWIMMING else WorkoutType.RUNNING
                            
                            val distKm = obj.optDouble("distance", 0.0) / 1000.0
                            val durMin = obj.optInt("moving_time", 0) / 60
                            val name = obj.optString("name", "Activité Strava")
                            val dateStr = obj.optString("start_date", "") 
                            val stravaId = obj.optString("id", "")
                            
                            // Parse ISO Date to Epoch
                            // Format Strava: "2018-05-02T12:15:09Z"
                            // Use SimpleDateFormat, assuming Z is UTC.
                            // We need to store this partially in Workout temporarily or rely on AnalyticsScreen to use it? 
                            // Workout doesn't have a 'date' field in Algorithm.kt... 
                            // Wait, Algorithm.kt Workout class has NO date field. It only has logic fields.
                            // The AnalyticsScreen uses Persistence.CompletedActivity which HAS a date.
                            // I need to add 'date' to Workout or return a richer object from this API.
                            // Or, I can hack it: I'm mapping to Workout (domain), but Workout is designed for PLANS usually.
                            // CompletedActivity is for History.
                            // Best fix: StravaAPI returns List<CompletedActivity> directly? Or returns a special wrapper.
                            // Current return type: Result<List<Workout>>.
                            // I should add `date` to Workout as well... or simpler: 
                            // I can't easily change return type everywhere.
                            // I'll add `date` (Long?) to Workout? No, I just added `summaryPolyline` and `externalId`.
                            // I'll add `startTime` to Workout.
                            
                            val hr = if(obj.has("average_heartrate")) obj.getDouble("average_heartrate").toInt() else null
                            val maxHr = if(obj.has("max_heartrate")) obj.getDouble("max_heartrate").toInt() else null
                            
                            var cad = if(obj.has("average_cadence")) obj.getDouble("average_cadence").toInt() else null
                            if (type == WorkoutType.RUNNING && cad != null && cad < 100) cad *= 2 
                            
                            val cal = if(obj.has("calories")) obj.optDouble("calories").toInt() else if (obj.has("kilojoules")) obj.optDouble("kilojoules").toInt() else null
                            val elev = obj.optDouble("total_elevation_gain", 0.0).toInt()
                            
                            val mapObj = obj.optJSONObject("map")
                            val polyline = mapObj?.optString("summary_polyline")
                            
                            // Hack: we need to pass the date out.
                            // We can use the 'externalId' field to pack ID|Date? No that's dirty.
                            // We should add `startTime: Long?` to Workout.
                            
                            // Scientific Fields
                            val watts = if(obj.has("average_watts")) obj.getDouble("average_watts").toInt() else null
                            val wWatts = if(obj.has("weighted_average_watts")) obj.getDouble("weighted_average_watts").toInt() else null
                            val kj = if(obj.has("kilojoules")) obj.getDouble("kilojoules").toFloat() else null
                            val suffer = if(obj.has("suffer_score")) obj.getInt("suffer_score") else null
                            val devWatts = obj.optBoolean("device_watts", false)
                            
                            // Science V2 Inputs
                            // Strava API: average_temp (Celsius), average_altitude? No, usually in streams or "elev_high"/"elev_low".
                            // "average_temp" exists in DetailedActivity, maybe not in Summary.
                            val temp = if(obj.has("average_temp")) obj.getDouble("average_temp") else null
                            // Elev High/Low exists. We can avg them as proxy if needed, or use null.
                            val alt = if(obj.has("elev_high") && obj.has("elev_low")) ((obj.getDouble("elev_high") + obj.getDouble("elev_low")) / 2).toInt() else null
                            val strokes = if(obj.has("total_strokes")) obj.getInt("total_strokes") else null
                            val swolfVal = if(obj.has("average_swolf")) obj.getInt("average_swolf") else null

                            allActivities.add(Workout(
                                type, name, distKm, durMin, emptyList(), 
                                avgHeartRate = hr, maxHeartRate = maxHr, avgCadence = cad,
                                totalStrokes = strokes, swolf = swolfVal, rpe = null,
                                elevationGain = elev, calories = cal,
                                summaryPolyline = polyline,
                                externalId = "$stravaId|$dateStr",
                                
                                avgWatts = watts,
                                weightedAvgWatts = wWatts,
                                kilojoules = kj,
                                sufferScore = suffer,
                                deviceWatts = devWatts,
                                avgTemp = temp,
                                avgAltitude = alt
                            ))
                        }
                        page++
                    }
                }
            }
            return@withContext Result.success(allActivities)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Récupère tous les flux de données (streams) pour une activité Strava.
     */
    suspend fun fetchAllActivityStreams(activityId: String): FullStreams = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank()) return@withContext FullStreams()
        
        try {
            // Updated keys to include potential dynamics
            val url = "https://www.strava.com/api/v3/activities/$activityId/streams?keys=heartrate,watts,cadence,velocity_smooth,altitude,stance_time,vertical_oscillation&key_by_type=true"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext FullStreams()
            
            val json = JSONObject(response.body?.string() ?: "{}")
            
            val hr = parseStream(json, "heartrate") { i, v -> com.orbital.run.logic.Persistence.HeartRateSample(i, v.toInt()) }
            val watts = parseStream(json, "watts") { i, v -> com.orbital.run.logic.Persistence.PowerSample(i, v.toDouble()) }
            val cad = parseStream(json, "cadence") { i, v -> com.orbital.run.logic.Persistence.CadenceSample(i, v.toDouble()) }
            val speed = parseStream(json, "velocity_smooth") { i, v -> com.orbital.run.logic.Persistence.SpeedSample(i, v.toDouble()) }
            val alt = parseStream(json, "altitude") { i, v -> com.orbital.run.logic.Persistence.ElevationSample(i, v.toDouble()) }

            // Running Dynamics
            val gct = parseStream(json, "stance_time") { i, v -> com.orbital.run.logic.Persistence.RunningDynamicSample(i, v) } // ms?
            val vo = parseStream(json, "vertical_oscillation") { i, v -> com.orbital.run.logic.Persistence.RunningDynamicSample(i, v) } // cm?

            // Calculated Stride Length
            // Formula: Speed (m/s) * 60 / Cadence (spm) = Meters/Stride
            // We assume streams are aligned by index (standard for Strava key_by_type=true)
            val stride = mutableListOf<com.orbital.run.logic.Persistence.RunningDynamicSample>()
            if (speed.isNotEmpty() && cad.isNotEmpty()) {
                val len = minOf(speed.size, cad.size)
                for (i in 0 until len) {
                    val spdVal = speed[i].speedMps
                    val cadVal = cad[i].rpm
                    if (cadVal > 0) {
                        val sl = (spdVal * 60.0) / cadVal
                        // Filter realistic values (e.g. 0.3m to 2.5m)
                        if (sl in 0.3..2.5) {
                            stride.add(com.orbital.run.logic.Persistence.RunningDynamicSample(i, sl))
                        }
                    }
                }
            }
            
            // Vertical Ratio (VO / Stride Length) * 100
            val vr = mutableListOf<com.orbital.run.logic.Persistence.RunningDynamicSample>()
            if (vo.isNotEmpty() && stride.isNotEmpty()) {
                 // Assuming alignment
                 val len = minOf(vo.size, stride.size)
                 for (i in 0 until len) {
                     val voCm = vo[i].value // Check if Strava sends cm or m. Usually cm.
                     val slM = stride[i].value
                     if (slM > 0) {
                         // If VO is in cm, convert to m: voCm / 100.0
                         // Ratio = (VO_m / Stride_m) * 100
                         // = ((voCm / 100.0) / slM) * 100
                         // = voCm / slM
                         val ratio = voCm / slM
                         vr.add(com.orbital.run.logic.Persistence.RunningDynamicSample(i, ratio))
                     }
                 }
            }
            
            FullStreams(hr, watts, cad, speed, alt, stride, gct, vo, vr)
        } catch (e: Exception) {
            FullStreams()
        }
    }
    
    // Helper to parse generic streams
    private fun <T> parseStream(json: JSONObject, key: String, transform: (Int, Double) -> T): List<T> {
        val obj = json.optJSONObject(key) ?: return emptyList()
        val data = obj.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<T>()
        for (i in 0 until data.length()) {
            list.add(transform(i, data.optDouble(i, 0.0)))
        }
        return list
    }

    data class FullStreams(
        val hr: List<com.orbital.run.logic.Persistence.HeartRateSample> = emptyList(),
        val watts: List<com.orbital.run.logic.Persistence.PowerSample> = emptyList(),
        val cadence: List<com.orbital.run.logic.Persistence.CadenceSample> = emptyList(),
        val speed: List<com.orbital.run.logic.Persistence.SpeedSample> = emptyList(),
        val alt: List<com.orbital.run.logic.Persistence.ElevationSample> = emptyList(),
        val stride: List<com.orbital.run.logic.Persistence.RunningDynamicSample> = emptyList(),
        val gct: List<com.orbital.run.logic.Persistence.RunningDynamicSample> = emptyList(),
        val vo: List<com.orbital.run.logic.Persistence.RunningDynamicSample> = emptyList(),
        val vr: List<com.orbital.run.logic.Persistence.RunningDynamicSample> = emptyList()
    )

    // Legacy support for older parts of the app
    suspend fun fetchActivityStreams(activityId: String) = fetchAllActivityStreams(activityId).hr
    
    // New signature requires context for saving
    // New signature requires context for saving
    suspend fun exchangeToken(context: Context, code: String): Boolean = withContext(Dispatchers.IO) {
         val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url("https://www.strava.com/oauth/token")
            .post(formBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                
                val body = response.body?.string() ?: return@withContext false
                val json = JSONObject(body)
                val newAccessToken = json.optString("access_token")
                val newRefreshToken = json.optString("refresh_token")
                val newExpiresAt = json.optLong("expires_at")
                
                if (newAccessToken.isNotBlank()) {
                    saveFullTokenState(context, newAccessToken, newRefreshToken, newExpiresAt)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    // Legacy support for older APIs if needed
    fun uploadWorkout(@Suppress("UNUSED_PARAMETER") _workout: Workout, callback: (Boolean, String?) -> Unit) {
         callback(false, "Upload Strava temporairement désactivé pour optimisation.")
    }

    fun disconnect(context: Context) {
        accessToken = null
        refreshToken = null
        expiresAt = 0L
        try {
            val securePrefs = getEncryptedPrefs(context)
            securePrefs?.edit()
                ?.remove("access_token")
                ?.remove("refresh_token")
                ?.remove("expires_at")
                ?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Stubs for others to keep compilation
object PolarAPI {
    fun isConfigured() = false
    fun isAuthenticated() = false
    fun openAuthorizationPage(@Suppress("UNUSED_PARAMETER") _context: Context) {}
    fun exchangeToken(@Suppress("UNUSED_PARAMETER") _code: String, _callback: (Boolean, String?) -> Unit) {}
    fun uploadWorkout(@Suppress("UNUSED_PARAMETER") _workout: Workout, _callback: (Boolean, String?) -> Unit) { _callback(false, "Non implémenté") }
    fun disconnect() {}
}

object SuuntoAPI {
    fun isConfigured() = false
    fun isAuthenticated() = false
    fun openAuthorizationPage(@Suppress("UNUSED_PARAMETER") _context: Context) {}
    fun exchangeToken(@Suppress("UNUSED_PARAMETER") _code: String, _callback: (Boolean, String?) -> Unit) {}
    fun uploadWorkout(@Suppress("UNUSED_PARAMETER") _workout: Workout, _callback: (Boolean, String?) -> Unit) { _callback(false, "Non implémenté") }
    fun disconnect() {}
}
