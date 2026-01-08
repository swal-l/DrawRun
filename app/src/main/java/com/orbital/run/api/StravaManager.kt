package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object StravaManager {
    // Client ID/Secret are injected via BuildConfig from local.properties
    
    fun connect(context: Context) {
        val clientId = com.orbital.run.BuildConfig.STRAVA_CLIENT_ID
        // In a real scenario without a backend, we'd use localhost redirect or custom scheme.
        // Assuming "drawrun://strava_callback" is/will be set up in Manifest if we wanted full flow.
        // For now, we open the auth page so the user sees it works with their ID.
        
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
        
        // We optimistically set it to enabled for UI feedback since we don't have the Callback Activity set up yet.
        Persistence.saveStravaEnabled(context, true)
    }

    fun disconnect(context: Context) {
        Persistence.saveStravaEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadStravaEnabled(context)
    }
}
