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
    fun isConfigured(): Boolean = true 
    fun getRequestToken(callback: (String?, String?) -> Unit) { callback(null, "use_login_dialog") }
    fun openAuthorizationPage(context: Context, token: String) { /* No-op */ }
}
