package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object GarminManager {
    private const val AUTH_URL = "https://connect.garmin.com/oauthConfirm?oauth_token=REQUEST_TOKEN_PLACEHOLDER" // Simplification
    // Note: True Garmin OAuth1.0a is complex and requires backend.
    // Assuming we had a "Legacy" implementation or fake one.
    // For now, I'll implement a structure that can be "Connected" via a web flow intent.

    fun connect(context: Context) {
        // Open Garmin Login Page (simulated or real if user has credentials)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://connect.garmin.com/modern/"))
        context.startActivity(intent)
        
        // Mark as "Pending" or "Connected" for logic simulation
        // In a real app without backend, we can't do full OAuth.
        // But user asked to "put back buttons".
        Persistence.saveGarminEnabled(context, true)
    }

    fun disconnect(context: Context) {
        Persistence.saveGarminEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadGarminEnabled(context)
    }
}
