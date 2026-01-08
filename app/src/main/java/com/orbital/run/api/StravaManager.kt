package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object StravaManager {
    // Client ID/Secret would normally be here
    
    fun connect(context: Context) {
        // Direct Strava OAuth URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.strava.com/oauth/authorize?client_id=12345&response_type=code&redirect_uri=drawrun://strava_callback&scope=activity:read_all"))
        context.startActivity(intent)
        Persistence.saveStravaEnabled(context, true)
    }

    fun disconnect(context: Context) {
        Persistence.saveStravaEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadStravaEnabled(context)
    }
}
