package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object PolarManager {
    fun connect(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://flow.polar.com/oauth2/authorization?client_id=YOUR_CLIENT_ID&response_type=code"))
        context.startActivity(intent)
        Persistence.savePolarEnabled(context, true)
    }

    fun disconnect(context: Context) {
        Persistence.savePolarEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadPolarEnabled(context)
    }
}
