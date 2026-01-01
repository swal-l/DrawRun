package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
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
    // ⚠️ REMPLACER PAR VOS VRAIES CLÉS API
    // Obtenez-les sur: https://developer.garmin.com/gc-developer-program/overview
    private const val CONSUMER_KEY = "YOUR_GARMIN_CONSUMER_KEY"
    private const val CONSUMER_SECRET = "YOUR_GARMIN_CONSUMER_SECRET"
    
    private const val BASE_URL = "https://apis.garmin.com"
    private const val REQUEST_TOKEN_URL = "$BASE_URL/oauth-service/oauth/request_token"
    private const val AUTHORIZE_URL = "https://connect.garmin.com/oauthConfirm"
    private const val ACCESS_TOKEN_URL = "$BASE_URL/oauth-service/oauth/access_token"
    private const val WORKOUT_URL = "$BASE_URL/workout-service/workout"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Tokens OAuth stockés
    private var accessToken: String? = null
    private var accessTokenSecret: String? = null
    
    /**
     * Vérifie si l'API est configurée avec des clés valides
     */
    fun isConfigured(): Boolean {
        return CONSUMER_KEY != "YOUR_GARMIN_CONSUMER_KEY" && 
               CONSUMER_SECRET != "YOUR_GARMIN_CONSUMER_SECRET"
    }
    
    /**
     * Vérifie si l'utilisateur est connecté à Garmin
     */
    fun isAuthenticated(): Boolean {
        return accessToken != null && accessTokenSecret != null
    }
    
    /**
     * Étape 1: Obtenir un Request Token
     */
    fun getRequestToken(callback: (String?, String?) -> Unit) {
        if (!isConfigured()) {
            callback(null, "API non configurée. Ajoutez vos clés Garmin.")
            return
        }
        
        Thread {
            try {
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = generateNonce()
                
                val params = mutableMapOf(
                    "oauth_consumer_key" to CONSUMER_KEY,
                    "oauth_nonce" to nonce,
                    "oauth_signature_method" to "HMAC-SHA1",
                    "oauth_timestamp" to timestamp,
                    "oauth_version" to "1.0"
                )
                
                val signature = generateSignature("POST", REQUEST_TOKEN_URL, params, CONSUMER_SECRET, "")
                params["oauth_signature"] = signature
                
                val authHeader = buildAuthHeader(params)
                
                val request = Request.Builder()
                    .url(REQUEST_TOKEN_URL)
                    .header("Authorization", authHeader)
                    .post("".toRequestBody())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val params = parseOAuthResponse(body)
                    val token = params["oauth_token"]
                    callback(token, null)
                } else {
                    callback(null, "Erreur: ${response.code}")
                }
            } catch (e: Exception) {
                callback(null, "Erreur réseau: ${e.message}")
            }
        }.start()
    }
    
    /**
     * Étape 2: Ouvrir le navigateur pour autorisation utilisateur
     */
    fun openAuthorizationPage(context: Context, requestToken: String) {
        val url = "$AUTHORIZE_URL?oauth_token=$requestToken"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
    
    /**
     * Étape 3: Échanger le Request Token contre un Access Token
     */
    fun getAccessToken(requestToken: String, verifier: String, callback: (Boolean, String?) -> Unit) {
        Thread {
            try {
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = generateNonce()
                
                val params = mutableMapOf(
                    "oauth_consumer_key" to CONSUMER_KEY,
                    "oauth_token" to requestToken,
                    "oauth_verifier" to verifier,
                    "oauth_nonce" to nonce,
                    "oauth_signature_method" to "HMAC-SHA1",
                    "oauth_timestamp" to timestamp,
                    "oauth_version" to "1.0"
                )
                
                val signature = generateSignature("POST", ACCESS_TOKEN_URL, params, CONSUMER_SECRET, "")
                params["oauth_signature"] = signature
                
                val authHeader = buildAuthHeader(params)
                
                val request = Request.Builder()
                    .url(ACCESS_TOKEN_URL)
                    .header("Authorization", authHeader)
                    .post("".toRequestBody())
                    .build()
                
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val responseParams = parseOAuthResponse(body)
                    accessToken = responseParams["oauth_token"]
                    accessTokenSecret = responseParams["oauth_token_secret"]
                    callback(true, null)
                } else {
                    callback(false, "Erreur: ${response.code}")
                }
            } catch (e: Exception) {
                callback(false, "Erreur: ${e.message}")
            }
        }.start()
    }
    
    /**
     * Envoyer un workout vers Garmin Connect
     */
    fun uploadWorkout(workout: Workout, callback: (Boolean, String?) -> Unit) {
        if (!isAuthenticated()) {
            callback(false, "Non connecté à Garmin. Connectez-vous d'abord.")
            return
        }
        
        Thread {
            try {
                val workoutJson = convertWorkoutToGarminFormat(workout)
                
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = generateNonce()
                
                val params = mutableMapOf(
                    "oauth_consumer_key" to CONSUMER_KEY,
                    "oauth_token" to accessToken!!,
                    "oauth_nonce" to nonce,
                    "oauth_signature_method" to "HMAC-SHA1",
                    "oauth_timestamp" to timestamp,
                    "oauth_version" to "1.0"
                )
                
                val signature = generateSignature("POST", WORKOUT_URL, params, CONSUMER_SECRET, accessTokenSecret!!)
                params["oauth_signature"] = signature
                
                val authHeader = buildAuthHeader(params)
                
                val requestBody = workoutJson.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url(WORKOUT_URL)
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    callback(true, "Workout envoyé avec succès !")
                } else {
                    callback(false, "Erreur ${response.code}: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                callback(false, "Erreur d'envoi: ${e.message}")
            }
        }.start()
    }
    
    /**
     * Convertit un Workout DrawRun au format Garmin JSON
     */
    private fun convertWorkoutToGarminFormat(workout: Workout): String {
        val json = JSONObject()
        json.put("workoutName", workout.title)
        json.put("description", "Généré par DrawRun")
        json.put("sportType", JSONObject().apply {
            put("sportTypeId", if (workout.type == WorkoutType.SWIMMING) 5 else 1) // 1=Running, 5=Swimming
            put("sportTypeKey", if (workout.type == WorkoutType.SWIMMING) "swimming" else "running")
        })
        
        val steps = JSONArray()
        workout.steps.forEachIndexed { index, step ->
            val stepJson = JSONObject()
            stepJson.put("type", "ExecutableStepDTO")
            stepJson.put("stepId", index + 1)
            stepJson.put("stepOrder", index + 1)
            stepJson.put("stepType", JSONObject().apply {
                put("stepTypeId", 1)
                put("stepTypeKey", "interval")
            })
            
            // Durée
            if (step.durationOrDist.contains("min")) {
                val mins = step.durationOrDist.filter { it.isDigit() }.toIntOrNull() ?: 1
                stepJson.put("endCondition", JSONObject().apply {
                    put("conditionTypeId", 2) // Time
                    put("conditionTypeKey", "time")
                })
                stepJson.put("endConditionValue", mins * 60.0) // En secondes
            } else {
                stepJson.put("endCondition", JSONObject().apply {
                    put("conditionTypeId", 3) // Distance
                    put("conditionTypeKey", "distance")
                })
                stepJson.put("endConditionValue", 1000.0) // 1km par défaut
            }
            
            // Zone cible
            step.targetZone?.let { zone ->
                stepJson.put("targetType", JSONObject().apply {
                    put("workoutTargetTypeId", 4) // Heart Rate Zone
                    put("workoutTargetTypeKey", "heart.rate.zone")
                })
                stepJson.put("targetValueOne", zone.toDouble())
                stepJson.put("targetValueTwo", zone.toDouble())
            }
            
            steps.put(stepJson)
        }
        
        json.put("workoutSegments", JSONArray().apply {
            put(JSONObject().apply {
                put("segmentOrder", 1)
                put("sportType", json.getJSONObject("sportType"))
                put("workoutSteps", steps)
            })
        })
        
        return json.toString()
    }
    
    // Utilitaires OAuth
    
    private fun generateNonce(): String {
        return System.currentTimeMillis().toString() + (Math.random() * 1000000).toInt()
    }
    
    private fun generateSignature(
        method: String,
        url: String,
        params: Map<String, String>,
        consumerSecret: String,
        tokenSecret: String
    ): String {
        val sortedParams = params.toSortedMap()
        val paramString = sortedParams.map { "${percentEncode(it.key)}=${percentEncode(it.value)}" }
            .joinToString("&")
        
        val baseString = "$method&${percentEncode(url)}&${percentEncode(paramString)}"
        val signingKey = "${percentEncode(consumerSecret)}&${percentEncode(tokenSecret)}"
        
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(signingKey.toByteArray(), "HmacSHA1"))
        val signature = mac.doFinal(baseString.toByteArray())
        
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }
    
    private fun buildAuthHeader(params: Map<String, String>): String {
        val headerParams = params.map { "${percentEncode(it.key)}=\"${percentEncode(it.value)}\"" }
            .joinToString(", ")
        return "OAuth $headerParams"
    }
    
    private fun parseOAuthResponse(response: String): Map<String, String> {
        return response.split("&").associate {
            val parts = it.split("=")
            parts[0] to (parts.getOrNull(1) ?: "")
        }
    }
    
    private fun percentEncode(s: String): String {
        return Uri.encode(s, "~")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
    
    /**
     * Déconnexion
     */
    fun disconnect() {
        accessToken = null
        accessTokenSecret = null
    }
}
