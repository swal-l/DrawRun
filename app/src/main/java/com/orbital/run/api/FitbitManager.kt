package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for Fitbit Web API integration.
 * Handles OAuth2 flow and Activity upload/download.
 */
object FitbitManager {
    // These should ideally come from BuildConfig
    private const val AUTH_URL = "https://www.fitbit.com/oauth2/authorize"
    private const val TOKEN_URL = "https://api.fitbit.com/oauth2/token"
    private const val API_BASE = "https://api.fitbit.com/1"
    
    fun connect(context: Context) {
        val clientId = com.orbital.run.BuildConfig.FITBIT_CLIENT_ID
        if (clientId.isEmpty()) {
            android.widget.Toast.makeText(context, "Client ID Fitbit manquant", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val redirectUri = "drawrun://fitbit_callback"
        val scope = "activity profile"
        val expires = "31536000" // 1 year
        
        val url = "$AUTH_URL?response_type=token&client_id=$clientId&redirect_uri=$redirectUri&scope=$scope&expires_in=$expires"
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun handleAuthCallback(context: Context, uri: Uri) {
        // Implicit Grant Flow returns token in fragment
        val fragment = uri.fragment
        if (fragment != null && fragment.contains("access_token")) {
             val params = fragment.split("&").associate { 
                 val parts = it.split("=")
                 parts[0] to parts.getOrElse(1) { "" }
             }
             
             val token = params["access_token"]
             val userId = params["user_id"]
             
             if (token != null) {
                 saveToken(context, token)
                 if (userId != null) saveUserId(context, userId)
                 Persistence.saveFitbitEnabled(context, true)
                 android.widget.Toast.makeText(context, "Fitbit connecté ✅", android.widget.Toast.LENGTH_SHORT).show()
             }
        }
    }
    
    fun disconnect(context: Context) {
        saveToken(context, null)
        Persistence.saveFitbitEnabled(context, false)
    }
    
    fun isConnected(context: Context): Boolean {
        return Persistence.loadFitbitEnabled(context) && getToken(context) != null
    }

    private fun saveToken(context: Context, token: String?) {
        context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)
            .edit().putString("access_token", token).apply()
    }

    private fun getToken(context: Context): String? {
        return context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)
            .getString("access_token", null)
    }
    
    private fun saveUserId(context: Context, id: String) {
        context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)
            .edit().putString("user_id", id).apply()
    }
    
    // --- API Methods ---

    suspend fun uploadActivity(context: Context, activity: com.orbital.run.logic.Persistence.CompletedActivity): Boolean = withContext(Dispatchers.IO) {
        val token = getToken(context) ?: return@withContext false
        
        // Mock Implementation: In reality, we'd POST to /1/user/-/activities.json
        android.util.Log.d("FITBIT", "Uploading activity ${activity.id} to Fitbit...")
        // Thread.sleep(500) // Sim network
        return@withContext true
    }
    
    suspend fun downloadRecentActivities(context: Context): List<com.orbital.run.logic.Persistence.CompletedActivity> = withContext(Dispatchers.IO) {
        val token = getToken(context) ?: return@withContext emptyList()
        
        // Mock Implementation: GET /1/user/-/activities/list.json
        android.util.Log.d("FITBIT", "Downloading recent activities...")
        return@withContext emptyList()
    }
}
