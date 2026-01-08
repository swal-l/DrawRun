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
}
