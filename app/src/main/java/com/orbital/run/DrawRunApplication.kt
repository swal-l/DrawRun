package com.orbital.run

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt integration
 * Sprint 1 - Phase 1: Hilt Setup
 */
@HiltAndroidApp
class DrawRunApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // TODO: Initialize any app-wide services here
        // e.g., logging, crash reporting, etc.
    }
}
