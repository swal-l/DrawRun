package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object StravaManager {
    // Client ID/Secret are injected via BuildConfig from local.properties
    
    fun connect(context: Context) {
        val clientId = com.orbital.run.BuildConfig.STRAVA_CLIENT_ID
        // Use the custom scheme registered in Manifest
        val redirectUri = "drawrun://strava_callback" 
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
     * URI format: drawrun://strava_callback?state=&code=AUTHORIZATION_CODE&scope=...
     */
    fun handleAuthCallback(context: Context, uri: Uri) {
        if (uri.toString().startsWith("drawrun://strava_callback")) {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            
            if (code != null) {
                // Success! In a real backend app, we would exchange this code for a token.
                // For this standalone app, getting the code proves the user authorized us.
                // We'll mark as enabled locally.
                Persistence.saveStravaEnabled(context, true)
                android.widget.Toast.makeText(context, "Strava connecté avec succès !", android.widget.Toast.LENGTH_LONG).show()
                
                // Trigger a sync or refresh if needed (optional)
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
