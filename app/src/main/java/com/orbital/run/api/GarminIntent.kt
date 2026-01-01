package com.orbital.run.api

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.orbital.run.logic.Workout
import com.orbital.run.logic.WorkoutType

/**
 * Gestionnaire d'envoi vers Garmin Connect
 * 
 * 🎭 MODE DÉMO ACTIVÉ
 * Simule l'envoi vers Garmin Connect
 */
object GarminIntent {
    
    private const val GARMIN_PACKAGE = "com.garmin.android.apps.connectmobile"
    
    /**
     * Vérifie si Garmin Connect est installé
     */
    fun isGarminConnectInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GARMIN_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Ouvre le Play Store pour installer Garmin Connect
     */
    fun openPlayStoreForGarmin(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$GARMIN_PACKAGE")
        }
        context.startActivity(intent)
    }
    
    /**
     * Envoie un workout vers Garmin Connect (MODE DÉMO)
     */
    fun sendWorkout(context: Context, workout: Workout, callback: (Boolean, String?) -> Unit) {
        // 🎭 MODE DÉMO - Simuler un envoi réussi
        Thread {
            Thread.sleep(1500) // Simuler délai réseau
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(true, "✅ [DÉMO] Workout '${workout.title}' envoyé vers Garmin Connect !")
            }
        }.start()
    }
}
