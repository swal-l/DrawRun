package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.orbital.run.logic.Persistence

object SuuntoManager {
    fun connect(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cloudapi-oauth.suunto.com/oauth/authorize?client_id=YOUR_CLIENT_ID&response_type=code&redirect_uri=drawrun://suunto_callback"))
        context.startActivity(intent)
        Persistence.saveSuuntoEnabled(context, true)
    }

    fun disconnect(context: Context) {
        Persistence.saveSuuntoEnabled(context, false)
    }

    fun isConnected(context: Context): Boolean {
        return Persistence.loadSuuntoEnabled(context)
    }
}
