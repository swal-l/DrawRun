package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.orbital.run.logic.Persistence
import com.orbital.run.logic.Workout
import com.orbital.run.logic.WorkoutType
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service d'intégration avec l'API Garmin Connect
 * 
 * IMPORTANT: Pour utiliser cette API, vous devez:
 * 1. Créer un compte développeur sur https://developer.garmin.com
 * 2. Créer une application et obtenir:
 *    - Consumer Key (API Key)
 *    - Consumer Secret
 * 3. Remplacer les valeurs ci-dessous par vos vraies clés
 */
object GarminAPI {
    private const val VERCEL_URL = "https://drawrunvercel-d2nnvlyct-lomics-projects.vercel.app/api/garmin_sync"
    
    // Credentials stockés (Sécurité: attention, stockés en clair ou base64 simple ici)
    private var userEmail: String? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Login Garmin peut être lent
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Vérifie si l'utilisateur est connecté (email présent)
     */
    fun isAuthenticated(): Boolean {
        return userEmail != null
    }

    /**
     * Connexion via le backend Vercel (Email/Pass)
     */
    fun login(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        val json = JSONObject()
        json.put("email", email)
        json.put("password", pass)
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(VERCEL_URL)
            .post(body)
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false, "Erreur réseau: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val respBody = response.body?.string()
                if (response.isSuccessful && respBody != null) {
                    try {
                        val respJson = JSONObject(respBody)
                        if (respJson.has("error")) {
                            callback(false, respJson.getString("error"))
                        } else {
                            // Succès
                            userEmail = email
                            callback(true, "Connecté avec succès !")
                            // Note: Les activités sont dans respJson.getJSONArray("data")
                            // On pourrait les parser ici si besoin immédiat
                        }
                    } catch (e: Exception) {
                        callback(false, "Erreur format: ${e.message}")
                    }
                } else {
                    callback(false, "Erreur ${response.code}: $respBody")
                }
            }
        })
    }
    
    /**
     * Déconnexion
     */
    fun disconnect() {
        userEmail = null
    }
    
    // Méthodes Legacy OAuth (désactivées ou redirigeant vers login)
    /**
     * Restaure une session via l'email stocké
     */
    fun restoreSession(email: String) {
        userEmail = email
    }

    /**
     * Tente de récupérer les dernières activités via le backend Vercel.
     * Nécessite que l'email (et password via Persistence) soient disponibles.
     */


// ...

    /**
     * Tente de récupérer les dernières activités via le backend Vercel.
     * Nécessite que l'email (et password via Persistence) soient disponibles.
     * @return List of activities or null if error (logged)
     */
    fun fetchActivities(limit: Int = 30, context: Context): List<Persistence.CompletedActivity>? {
        val email = userEmail ?: Persistence.loadGarminEmail(context)
        val password = Persistence.loadGarminPassword(context)

        if (email == null || password == null) {
            android.util.Log.e("GarminAPI", "Identifiants Garmin manquants")
            return null
        }
        
        // Ensure credential consistency
        userEmail = email

        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)
        
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(VERCEL_URL)
            .post(body)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("GarminAPI", "Erreur serveur Vercel: ${response.code}")
                    return null
                }
                
                val respBody = response.body?.string() ?: return null
                val respJson = JSONObject(respBody)
                
                if (respJson.has("error")) {
                   android.util.Log.e("GarminAPI", respJson.getString("error"))
                   return null
                }
                
                val data = respJson.optJSONArray("data")
                return parseActivities(data)
            }
        } catch (e: Exception) {
            android.util.Log.e("GarminAPI", "Exception: ${e.message}")
            return null
        }
    }
    
    private fun parseActivities(jsonArray: JSONArray?): List<Persistence.CompletedActivity> {
        val list = mutableListOf<Persistence.CompletedActivity>()
        if (jsonArray == null) return list
        
        for (i in 0 until jsonArray.length()) {
             val obj = jsonArray.getJSONObject(i)
             val typeKey = obj.optString("type", "running")
             val type = when {
                 typeKey.contains("running", true) -> WorkoutType.RUNNING
                 typeKey.contains("cycling", true) -> WorkoutType.CYCLING
                 typeKey.contains("swimming", true) -> WorkoutType.SWIMMING
                 else -> WorkoutType.RUNNING
             }
             
             // Date parsing "yyyy-MM-dd HH:mm:ss" usually from Garmin local time
             val startTimeStr = obj.optString("startTime") 
             val dateMillis = try {
                 java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(startTimeStr)?.time ?: System.currentTimeMillis()
             } catch (e: Exception) { System.currentTimeMillis() }
            
             list.add(Persistence.CompletedActivity(
                 id = obj.optString("id"),
                 externalId = obj.optString("id"), // Garmin Activity ID
                 date = dateMillis,
                 type = type,
                 title = obj.optString("name", "Activite Garmin"),
                 distanceKm = obj.optDouble("distance", 0.0) / 1000.0,
                 durationMin = (obj.optDouble("duration", 0.0) / 60.0).toInt(),
                 source = "Garmin",
                 avgHeartRate = obj.optInt("avgHr", 0).takeIf { it > 0 },
                 elevationGain = obj.optInt("elevation", 0).takeIf { it > 0 }
             ))
        }
        return list
    }

    // Méthodes Legacy OAuth (désactivées ou redirigeant vers login)
    fun isConfigured(): Boolean = isAuthenticated() 
    fun getRequestToken(callback: (String?, String?) -> Unit) { callback(null, "use_login_dialog") }
    fun openAuthorizationPage(context: Context, token: String) { /* No-op */ }
}
