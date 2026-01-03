package com.orbital.run.logic

import android.content.Context

import java.io.File

object Persistence {
    private const val PREFS_NAME = "drawrun_prefs"
    private const val HISTORY_FILE = "activity_history.json"
    private const val BLACKLIST_FILE = "deleted_activities.json"
    
    fun saveProfile(context: Context, p: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putInt("age", p.age)
            putBoolean("isMale", p.isMale)
            putFloat("weight", p.weightKg.toFloat())
            putInt("hr", p.restingHeartRate)
            putFloat("vol", p.currentWeeklyDistanceKm.toFloat())
            putFloat("goalDist", p.goalDistanceKm.toFloat())
            putFloat("goalTime", p.goalTimeMinutes.toFloat())
            putInt("weeks", p.programDurationWeeks)
            apply()
        }
    }
    
    fun loadProfile(context: Context): UserProfile? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("age")) return null
        
        return UserProfile(
            age = prefs.getInt("age", 30),
            isMale = prefs.getBoolean("isMale", true),
            weightKg = prefs.getFloat("weight", 70f).toDouble(),
            restingHeartRate = prefs.getInt("hr", 60),
            currentWeeklyDistanceKm = prefs.getFloat("vol", 30f).toDouble(),
            goalDistanceKm = prefs.getFloat("goalDist", 10f).toDouble(),
            goalTimeMinutes = prefs.getFloat("goalTime", 60f).toDouble(),
            raceDateMillis = prefs.getLong("raceDate", System.currentTimeMillis() + (prefs.getInt("weeks", 12) * 7 * 24 * 60 * 60 * 1000L))
        )
    }

    fun isOnboardingComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_complete", false)
    }

    fun setOnboardingComplete(context: Context, complete: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
    }

    fun saveGarminEmail(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("garmin_email", email).apply()
    }

    fun loadGarminEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("garmin_email", null)
    }

    fun saveGarminPassword(context: Context, pass: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("garmin_pass", pass).apply()
    }

    fun loadGarminPassword(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("garmin_pass", null)
    }

    fun saveHealthConnectEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hc_enabled", enabled).apply()
    }

    fun loadHealthConnectEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("hc_enabled", false)
    }

    fun saveSwims(context: Context, workouts: List<Workout>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        workouts.forEach { w ->
            val obj = org.json.JSONObject()
            obj.put("type", w.type.name)
            obj.put("title", w.title)
            obj.put("dist", w.totalDistanceKm)
            obj.put("dur", w.totalDurationMin)
            
            val stepsArr = org.json.JSONArray()
            w.steps.forEach { s ->
                val sObj = org.json.JSONObject()
                sObj.put("desc", s.description)
                sObj.put("durDist", s.durationOrDist)
                sObj.put("zone", s.targetZone ?: 0) // Handle null
                stepsArr.put(sObj)
            }
            obj.put("steps", stepsArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_swims", jsonArray.toString()).apply()
    }

    fun loadSwims(context: Context): List<Workout> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("saved_swims", null) ?: return emptyList()
        val list = mutableListOf<Workout>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val type = WorkoutType.valueOf(obj.getString("type"))
                val title = obj.getString("title")
                val dist = obj.getDouble("dist")
                val dur = obj.getInt("dur")
                
                val steps = mutableListOf<WorkoutStep>()
                val stepsArr = obj.getJSONArray("steps")
                for (j in 0 until stepsArr.length()) {
                    val sObj = stepsArr.getJSONObject(j)
                    val z = sObj.optInt("zone", 0)
                    steps.add(WorkoutStep(
                        sObj.getString("desc"),
                        sObj.getString("durDist"),
                        if (z == 0) null else z
                    ))
                }
                list.add(Workout(type, title, dist, dur, steps))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    // -------------------------------------------------------------
    // SAVED PLANS (RUNNING)
    // -------------------------------------------------------------
    
    data class StoredPlan(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val createdDate: Long,
        val goal: String, // e.g. "Marathon - 3h30"
        val result: TrainingPlanResult
    )

    fun saveRunPlan(context: Context, plan: StoredPlan) {
        val current = loadRunPlans(context).toMutableList()
        current.add(0, plan) // Newest first
        
        // Serialize manually strictly to avoid Gson dependency issues if any
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        
        current.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("date", p.createdDate)
            obj.put("goal", p.goal)
            
            // Persist Stats
            obj.put("vma", p.result.vma)
            obj.put("fcm", p.result.fcm)
            obj.put("vo2max", p.result.vo2max)
            obj.put("vol_safe", p.result.volumePicSafe)
            obj.put("vol_perf", p.result.volumePicPerformance)
            obj.put("p1", p.result.phaseDurations.first)
            obj.put("p2", p.result.phaseDurations.second)
            obj.put("p3", p.result.phaseDurations.third)
            
            // User Profile
            val upObj = org.json.JSONObject()
            upObj.put("age", p.result.userProfile.age)
            upObj.put("isMale", p.result.userProfile.isMale)
            upObj.put("weight", p.result.userProfile.weightKg)
            upObj.put("hr", p.result.userProfile.restingHeartRate)
            upObj.put("vol", p.result.userProfile.currentWeeklyDistanceKm)
            upObj.put("g_dist", p.result.userProfile.goalDistanceKm)
            upObj.put("g_time", p.result.userProfile.goalTimeMinutes)
            upObj.put("weeks", p.result.userProfile.programDurationWeeks)
            obj.put("profile", upObj)

            val weeksArr = org.json.JSONArray()
            p.result.weeklyPlan.forEach { week ->
                val wObj = org.json.JSONObject()
                wObj.put("num", week.weekNumber)
                wObj.put("phase", week.phase)
                wObj.put("vol", week.totalDistance)
                
                val wWorkouts = org.json.JSONArray()
                week.workouts.forEach { wo ->
                    val woObj = org.json.JSONObject()
                    woObj.put("t", wo.type.name)
                    woObj.put("ti", wo.title)
                    woObj.put("d", wo.totalDistanceKm)
                    woObj.put("du", wo.totalDurationMin)
                    // Steps
                     val stepsArr = org.json.JSONArray()
                    wo.steps.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("desc", s.description)
                        sObj.put("dd", s.durationOrDist)
                        sObj.put("z", s.targetZone ?: 0)
                        stepsArr.put(sObj)
                    }
                    woObj.put("s", stepsArr)
                    wWorkouts.put(woObj)
                }
                wObj.put("wos", wWorkouts)
                weeksArr.put(wObj)
            }
            obj.put("weeks", weeksArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_run_plans", jsonArray.toString()).apply()
    }
    
    fun loadRunPlans(context: Context): List<StoredPlan> {
         val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
         val raw = prefs.getString("saved_run_plans", null) ?: return emptyList()
         val list = mutableListOf<StoredPlan>()
         try {
             val arr = org.json.JSONArray(raw)
             for(i in 0 until arr.length()){
                 val obj = arr.getJSONObject(i)
                 val id = obj.getString("id")
                 val title = obj.getString("title")
                 val date = obj.getLong("date")
                 val goal = obj.optString("goal", "Objectif")
                 
                 // Restore Stats
                 val vma = obj.optDouble("vma", 12.0)
                 val fcm = obj.optInt("fcm", 180)
                 val vo2max = obj.optDouble("vo2max", 45.0)
                 val volSafe = obj.optDouble("vol_safe", 30.0)
                 val volPerf = obj.optDouble("vol_perf", 35.0)
                 val p1 = obj.optDouble("p1", 1.0)
                 val p2 = obj.optDouble("p2", 1.0)
                 val p3 = obj.optDouble("p3", 1.0)
                 
                 // Restore Profile
                 val pObj = obj.optJSONObject("profile")
                 val profile = if(pObj != null) {
                     UserProfile(
                         pObj.optInt("age", 30), pObj.optBoolean("isMale", true),
                         pObj.optDouble("weight", 70.0), pObj.optInt("hr", 60),
                         pObj.optDouble("vol", 30.0), pObj.optDouble("g_dist", 10.0),
                         pObj.optDouble("g_time", 60.0),
                         System.currentTimeMillis() + (pObj.optInt("weeks", 12) * 7 * 24 * 60 * 60 * 1000L)
                     )
                 } else {
                     UserProfile(30, true, 70.0, 60, 30.0, 10.0, 60.0, System.currentTimeMillis() + (12 * 7 * 24 * 60 * 60 * 1000L))
                 }

                 val weeks = mutableListOf<TrainingWeek>()
                 val wArr = obj.getJSONArray("weeks")
                 for(k in 0 until wArr.length()){
                     val wObj = wArr.getJSONObject(k)
                     val workouts = mutableListOf<Workout>()
                     val woArr = wObj.getJSONArray("wos")
                     for(x in 0 until woArr.length()){
                         val wo = woArr.getJSONObject(x)
                         val steps = mutableListOf<WorkoutStep>()
                         val sArr = wo.getJSONArray("s")
                         for(y in 0 until sArr.length()){
                             val s = sArr.getJSONObject(y)
                             steps.add(WorkoutStep(s.getString("desc"), s.getString("dd"), if(s.getInt("z")==0) null else s.getInt("z")))
                         }
                         workouts.add(Workout(WorkoutType.valueOf(wo.getString("t")), wo.getString("ti"), wo.getDouble("d"), wo.getInt("du"), steps))
                     }
                     weeks.add(TrainingWeek(wObj.getInt("num"), wObj.getString("phase"), wObj.getDouble("vol"), workouts))
                 }
                 
                 // Reconstruct Full Result
                 val hrZones = OrbitalAlgorithm.calculateHRZones(profile.restingHeartRate, fcm)
                 val speedZones = OrbitalAlgorithm.calculateSpeedZones(vma)
                 val powerZones = OrbitalAlgorithm.calculatePowerZones(speedZones, profile.weightKg)
                 val predictions = OrbitalAlgorithm.predictRaceTimes(vma)
                 
                 val res = TrainingPlanResult(profile, fcm, vma, vo2max, hrZones, speedZones, powerZones, volSafe, volPerf, Triple(p1,p2,p3), weeks, predictions)
                 
                 list.add(StoredPlan(id, title, date, goal, res))
             }
         } catch(e: Exception){ e.printStackTrace() }
         return list
    }
    
    fun deleteRunPlan(context: Context, planId: String) {
        val current = loadRunPlans(context).filter { it.id != planId }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Quick re-save logic by clearing and re-adding? No, just copy save logic logic or refactor.
        // Refactor: We need a helper to save list.
        // Copy-paste logic for safety in this constrained edit.
        val jsonArray = org.json.JSONArray()
        current.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("date", p.createdDate)
            obj.put("goal", p.goal)
            val weeksArr = org.json.JSONArray()
            p.result.weeklyPlan.forEach { week ->
                val wObj = org.json.JSONObject()
                wObj.put("num", week.weekNumber)
                wObj.put("phase", week.phase)
                wObj.put("vol", week.totalDistance)
                val wWorkouts = org.json.JSONArray()
                week.workouts.forEach { wo ->
                    val woObj = org.json.JSONObject()
                    woObj.put("t", wo.type.name)
                    woObj.put("ti", wo.title)
                    woObj.put("d", wo.totalDistanceKm)
                    woObj.put("du", wo.totalDurationMin)
                     val stepsArr = org.json.JSONArray()
                    wo.steps.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("desc", s.description)
                        sObj.put("dd", s.durationOrDist)
                        sObj.put("z", s.targetZone ?: 0)
                        stepsArr.put(sObj)
                    }
                    woObj.put("s", stepsArr)
                    wWorkouts.put(woObj)
                }
                wObj.put("wos", wWorkouts)
                weeksArr.put(wObj)
            }
            obj.put("weeks", weeksArr)
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_run_plans", jsonArray.toString()).apply()
    }

    // -------------------------------------------------------------
    // ANALYTICS & HISTORY
    // -------------------------------------------------------------

    data class CompletedActivity(
        val id: String = java.util.UUID.randomUUID().toString(),
        val date: Long, // Epoch millis
        val type: WorkoutType,
        var title: String, // Mutable for editing
        val distanceKm: Double,
        val durationMin: Int,
        val source: String = "DrawRun", // "DrawRun", "Strava", etc.
        
        // New Professional Metrics (Null if not available)
        val avgHeartRate: Int? = null,
        val maxHeartRate: Int? = null,
        val avgCadence: Int? = null, // spm
        val totalStrokes: Int? = null,
        val swolf: Int? = null,
        val rpe: Int? = null, // 1-10
        
        // Extended Data for "Story"
        val calories: Int? = null,
        val elevationGain: Int? = null, // meters
        var notes: String? = null,
        val splits: List<Split> = emptyList(), // Detailed Km/Lap splits
        val zoneDistribution: List<Float> = emptyList(),
        // New Scientific Fields
        val avgWatts: Int? = null,
        val weightedAvgWatts: Int? = null,
        val kilojoules: Float? = null,
        val sufferScore: Int? = null,
        val deviceWatts: Boolean = false, // True if power meter, False if estimated
        val summaryPolyline: String? = null, // Encoded Google Polyline
        val externalId: String? = null, // Strava ID to prevent duplicates
        
        // Science V2 (Labo)
        val avgTemp: Double? = null,
        val avgAltitude: Int? = null,
        val efficiencyFactor: Double? = null,
        val runningEffectiveness: Double? = null,
        
        // HR Samples for Detailed Analysis
        val hrVariability: Double? = null,
        
        // V1.3 Biomechanics & Power
        val avgGctMs: Double? = null,
        val gctBalanceLeft: Double? = null,
        val dutyFactor: Double? = null,
        val legSpringStiffness: Double? = null,
        val verticalRatio: Double? = null,
        val airPowerAvg: Double? = null,
        val criticalPower: Double? = null,
        
        // V1.3 Swimming Extended
        val strokeIndex: Double? = null,
        val ivv: Double? = null,
        val idc: Double? = null,
        val turnTime5m: Double? = null,
        val breakoutSpeed: Double? = null,
        
        // V1.3 Longitudinal
        val ctl: Double? = null, // Fitness
        val atl: Double? = null, // Fatigue
        val tsb: Double? = null, // Form
        val acwr: Double? = null,
        val monotony: Double? = null,
        val strain: Double? = null,

        // High Granularity Samples (V1.5)
        val heartRateSamples: List<HeartRateSample> = emptyList(),
        val speedSamples: List<SpeedSample> = emptyList(),
        val powerSamples: List<PowerSample> = emptyList(),
        val cadenceSamples: List<CadenceSample> = emptyList(),
        val elevationSamples: List<ElevationSample> = emptyList(),
        
        // V1.6 Running Dynamics Series
        val strideLengthSamples: List<RunningDynamicSample> = emptyList(),
        val gctSamples: List<RunningDynamicSample> = emptyList(),
        val verticalOscillationSamples: List<RunningDynamicSample> = emptyList(),
        val verticalRatioSamples: List<RunningDynamicSample> = emptyList(),
        
        // V2.0 GPS Route & Respiratory
        val gpsCoordinates: List<GpsCoordinate>? = null,
        val avgRespiratoryRate: Double? = null, // Respirations per minute
        val respiratorySamples: List<RespiratorySample> = emptyList()
    ) {
        val durationSec get() = durationMin * 60
    }
    
    // GPS Coordinate for route display
    data class GpsCoordinate(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val altitude: Double? = null,
        val accuracy: Double? = null
    )
    
    // Heart Rate Sample for time-series analysis
    data class HeartRateSample(
        val timeOffset: Int,  // Seconds since activity start
        val bpm: Int          // Beats per minute at this time
    )

    data class SpeedSample(
        val timeOffset: Int,
        val speedMps: Double
    )

    data class PowerSample(
        val timeOffset: Int,
        val watts: Double
    )

    data class CadenceSample(
        val timeOffset: Int,
        val rpm: Double
    )

    data class ElevationSample(
        val timeOffset: Int,
        val avgAltitude: Double
    )
    
    // Respiratory Rate Sample
    data class RespiratorySample(
        val timeOffset: Int,
        val rpm: Double  // Respirations per minute
    )
    
    // New Running Dynamics Sample (Generic for Stride, GCT, VO, VR)
    data class RunningDynamicSample(
        val timeOffset: Int,
        val value: Double
    )

    data class RecordDetails(
        val duration: Long,
        val activityId: String,
        val date: Long,
        val distance: Double
    )

    data class PersonalRecords(
        // Running
        val best1k: RecordDetails? = null,
        val best5k: RecordDetails? = null,
        val best10k: RecordDetails? = null,
        val bestHalf: RecordDetails? = null,
        val bestMarathon: RecordDetails? = null,
        val longestRunKm: RecordDetails? = null,
        
        // Swimming
        val best100mSwim: RecordDetails? = null,
        val best200mSwim: RecordDetails? = null,
        val best400mSwim: RecordDetails? = null,
        val best800mSwim: RecordDetails? = null,
        val best1500mSwim: RecordDetails? = null,
        val longestSwimDist: RecordDetails? = null
    )
    
    data class Gear(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val type: String, // "Shoe", "Bike"
        val currentDistanceKm: Double,
        val maxDistanceKm: Double,
        val isActive: Boolean = true
    )

    data class Split(
        val kmIndex: Int, // 1, 2, 3...
        val durationSec: Int,
        val avgHr: Int? = null,
        val avgWatts: Int? = null,
        val avgCadence: Int? = null,
        val gctMs: Double? = null
    )

    fun saveCompletedActivity(context: Context, activity: CompletedActivity) {
        saveHistoryBatch(context, listOf(activity))
        updatePersonalRecords(context, activity)
    }

    fun saveHistoryBatch(context: Context, activities: List<CompletedActivity>) {
        if (activities.isEmpty()) return
        val history = loadHistory(context).toMutableList()
        activities.forEach { activity ->
            val index = history.indexOfFirst { it.id == activity.id || (activity.externalId != null && it.externalId == activity.externalId) }
            if (index >= 0) {
                history[index] = mergeActivities(history[index], activity)
            } else {
                history.add(0, activity)
            }
        }
        // Increase total history limit to 500 since we're using files now
        val finalHistory = history.sortedByDescending { it.date }.take(500)
        saveHistoryList(context, finalHistory)
    }

    private fun saveHistoryList(context: Context, list: List<CompletedActivity>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        
        list.forEachIndexed { index, a ->
            val obj = org.json.JSONObject()
            
            // Critical Optimization: Strip high-resolution samples for older activities
            // Only keep samples for the 10 most recent activities
            val keepSamples = index < 10
            
            obj.put("id", a.id)
            obj.put("date", a.date)
            obj.put("type", a.type.name)
            obj.put("title", a.title)
            obj.put("dist", a.distanceKm)
            obj.put("dur", a.durationMin)
            obj.put("src", a.source)
            
            // New fields
            a.avgTemp?.let { v -> obj.put("temp", v) }
            a.avgAltitude?.let { v -> obj.put("alt", v) }
            a.efficiencyFactor?.let { v -> obj.put("ef", v) }
            a.runningEffectiveness?.let { v -> obj.put("re", v) }

            a.avgHeartRate?.let { v -> obj.put("hr_avg", v) }
            a.maxHeartRate?.let { v -> obj.put("hr_max", v) }
            a.avgCadence?.let { v -> obj.put("cad", v) }
            a.totalStrokes?.let { v -> obj.put("strokes", v) }
            a.swolf?.let { v -> obj.put("swolf", v) }
            a.rpe?.let { v -> obj.put("rpe", v) }
            a.calories?.let { v -> obj.put("cal", v) }
            a.elevationGain?.let { v -> obj.put("elev", v) }
            a.notes?.let { v -> obj.put("notes", v) }
            a.summaryPolyline?.let { v -> obj.put("poly", v) }
            a.externalId?.let { v -> obj.put("ext_id", v) }
            
            // New Fields
            a.avgWatts?.let { v -> obj.put("watts", v) }
            a.weightedAvgWatts?.let { v -> obj.put("w_watts", v) }
            a.kilojoules?.let { v -> obj.put("kj", v.toDouble()) }
            a.sufferScore?.let { v -> obj.put("suffer", v) }
            obj.put("dev_watts", a.deviceWatts)
            
            if (keepSamples) {
                // V1.3 Serialization
                a.avgGctMs?.let { obj.put("gct", it) }
                a.gctBalanceLeft?.let { obj.put("gct_b", it) }
                a.dutyFactor?.let { obj.put("duty", it) }
                a.legSpringStiffness?.let { obj.put("lss", it) }
                a.verticalRatio?.let { obj.put("v_ratio", it) }
                a.airPowerAvg?.let { obj.put("air_p", it) }
                a.criticalPower?.let { obj.put("cp", it) }
                a.strokeIndex?.let { obj.put("si", it) }
                a.ivv?.let { obj.put("ivv", it) }
                a.idc?.let { obj.put("idc", it) }
                a.ctl?.let { obj.put("ctl", it) }
                a.atl?.let { obj.put("atl", it) }
                a.acwr?.let { obj.put("acwr", it) }
                
                // HR Samples Serialization
                if (a.heartRateSamples.isNotEmpty()) {
                    val hrArr = org.json.JSONArray()
                    a.heartRateSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("b", s.bpm)
                        hrArr.put(sObj)
                    }
                    obj.put("hr_samples", hrArr)
                }

                // Speed Samples Serialization
                if (a.speedSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.speedSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("v", s.speedMps)
                        arr.put(sObj)
                    }
                    obj.put("speed_samples", arr)
                }

                // Power Samples Serialization
                if (a.powerSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.powerSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("w", s.watts)
                        arr.put(sObj)
                    }
                    obj.put("power_samples", arr)
                }

                // Cadence Samples Serialization
                if (a.cadenceSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.cadenceSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("c", s.rpm)
                        arr.put(sObj)
                    }
                    obj.put("cad_samples", arr)
                }

                // Elevation Samples Serialization
                if (a.elevationSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.elevationSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("a", s.avgAltitude)
                        arr.put(sObj)
                    }
                    obj.put("elev_samples", arr)
                }

                // Running Dynamics Serialization
                if (a.strideLengthSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.strideLengthSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("v", s.value)
                        arr.put(sObj)
                    }
                    obj.put("stride_samples", arr)
                }
                if (a.gctSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.gctSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("v", s.value)
                        arr.put(sObj)
                    }
                    obj.put("gct_samples", arr)
                }
                if (a.verticalOscillationSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.verticalOscillationSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("v", s.value)
                        arr.put(sObj)
                    }
                    obj.put("vo_samples", arr)
                }
                if (a.verticalRatioSamples.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    a.verticalRatioSamples.forEach { s ->
                        val sObj = org.json.JSONObject()
                        sObj.put("t", s.timeOffset)
                        sObj.put("v", s.value)
                        arr.put(sObj)
                    }
                    obj.put("vr_samples", arr)
                }
            }
            
            // Serialize Splits (Important even for old activities)
            if (a.splits.isNotEmpty()) {
                val splitsArr = org.json.JSONArray()
                a.splits.forEach { s ->
                    val sObj = org.json.JSONObject()
                    sObj.put("k", s.kmIndex)
                    sObj.put("t", s.durationSec)
                    s.avgHr?.let { h -> sObj.put("h", h) }
                    s.avgWatts?.let { w -> sObj.put("w", w) }
                    s.avgCadence?.let { c -> sObj.put("c", c) }
                    s.gctMs?.let { g -> sObj.put("g", g) }
                    splitsArr.put(sObj)
                }
                obj.put("splits", splitsArr)
            }
            
            // Serialize Zones
            if (a.zoneDistribution.isNotEmpty()) {
                val zArr = org.json.JSONArray()
                a.zoneDistribution.forEach { zArr.put(it) }
                obj.put("zones", zArr)
            }
            
            jsonArray.put(obj)
        }
        
        try {
            val file = File(context.filesDir, HISTORY_FILE)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadHistory(context: Context): List<CompletedActivity> {
        val file = File(context.filesDir, HISTORY_FILE)
        
        // Migration from SharedPreferences if file doesn't exist
        val jsonStr = if (file.exists()) {
            try { file.readText() } catch (e: Exception) { null }
        } else {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val legacy = prefs.getString("history_activities", null)
            if (legacy != null) {
                // Perform one-time migration
                try {
                    file.writeText(legacy)
                    prefs.edit().remove("history_activities").apply()
                } catch (e: Exception) {}
            }
            legacy
        } ?: return emptyList()

        val list = mutableListOf<CompletedActivity>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)
                    
                    // Parse Splits
                    val splits = mutableListOf<Split>()
                    val splitsArr = obj.optJSONArray("splits")
                    if (splitsArr != null) {
                        for (j in 0 until splitsArr.length()) {
                            val sObj = splitsArr.getJSONObject(j)
                            splits.add(Split(
                                kmIndex = sObj.getInt("k"),
                                durationSec = sObj.getInt("t"),
                                avgHr = if (sObj.has("h")) sObj.getInt("h") else null,
                                avgWatts = if (sObj.has("w")) sObj.getInt("w") else null,
                                avgCadence = if (sObj.has("c")) sObj.getInt("c") else null,
                                gctMs = if (sObj.has("g")) sObj.getDouble("g") else null
                            ))
                        }
                    }
                    
                    // Parse Zones
                    val zones = mutableListOf<Float>()
                    val zArr = obj.optJSONArray("zones")
                    if (zArr != null) {
                        for (j in 0 until zArr.length()) zones.add(zArr.getDouble(j).toFloat())
                    }

                    list.add(CompletedActivity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        date = obj.getLong("date"),
                        type = WorkoutType.valueOf(obj.getString("type")),
                        title = obj.getString("title"),
                        distanceKm = obj.getDouble("dist"),
                        durationMin = obj.getInt("dur"),
                        source = obj.optString("src", "DrawRun"),
                        
                        avgHeartRate = if(obj.has("hr_avg")) obj.getInt("hr_avg") else null,
                        maxHeartRate = if(obj.has("hr_max")) obj.getInt("hr_max") else null,
                        avgCadence = if(obj.has("cad")) obj.getInt("cad") else null,
                        totalStrokes = if(obj.has("strokes")) obj.getInt("strokes") else null,
                        swolf = if(obj.has("swolf")) obj.getInt("swolf") else null,
                        rpe = if(obj.has("rpe")) obj.getInt("rpe") else null,
                        calories = if(obj.has("cal")) obj.getInt("cal") else null,
                        elevationGain = if(obj.has("elev")) obj.getInt("elev") else null,
                        notes = obj.optString("notes", null),
                        summaryPolyline = obj.optString("poly", null),
                        externalId = obj.optString("ext_id", null),
                        splits = splits,
                        zoneDistribution = zones,
                        
                        avgWatts = if(obj.has("watts")) obj.getInt("watts") else null,
                        weightedAvgWatts = if(obj.has("w_watts")) obj.getInt("w_watts") else null,
                        kilojoules = if(obj.has("kj")) obj.getDouble("kj").toFloat() else null,
                        sufferScore = if(obj.has("suffer")) obj.getInt("suffer") else null,
                        deviceWatts = obj.optBoolean("dev_watts", false),
                        
                        avgTemp = if(obj.has("temp")) obj.getDouble("temp") else null,
                        avgAltitude = if(obj.has("alt")) obj.getInt("alt") else null,
                        efficiencyFactor = if(obj.has("ef")) obj.getDouble("ef") else null,
                        runningEffectiveness = if(obj.has("re")) obj.getDouble("re") else null,
                        
                        avgGctMs = if(obj.has("gct")) obj.getDouble("gct") else null,
                        gctBalanceLeft = if(obj.has("gct_b")) obj.getDouble("gct_b") else null,
                        dutyFactor = if(obj.has("duty")) obj.getDouble("duty") else null,
                        legSpringStiffness = if(obj.has("lss")) obj.getDouble("lss") else null,
                        verticalRatio = if(obj.has("v_ratio")) obj.getDouble("v_ratio") else null,
                        airPowerAvg = if(obj.has("air_p")) obj.getDouble("air_p") else null,
                        criticalPower = if(obj.has("cp")) obj.getDouble("cp") else null,
                        strokeIndex = if(obj.has("si")) obj.getDouble("si") else null,
                        ivv = if(obj.has("ivv")) obj.getDouble("ivv") else null,
                        idc = if(obj.has("idc")) obj.getDouble("idc") else null,
                        ctl = if(obj.has("ctl")) obj.getDouble("ctl") else null,
                        atl = if(obj.has("atl")) obj.getDouble("atl") else null,
                        acwr = if(obj.has("acwr")) obj.getDouble("acwr") else null,
                        
                        heartRateSamples = mutableListOf<HeartRateSample>().apply {
                            val hrArr = obj.optJSONArray("hr_samples")
                            if (hrArr != null) {
                                for (j in 0 until hrArr.length()) {
                                    val sObj = hrArr.getJSONObject(j)
                                    add(HeartRateSample(sObj.getInt("t"), sObj.getInt("b")))
                                }
                            }
                        },
                        speedSamples = mutableListOf<SpeedSample>().apply {
                            val arr = obj.optJSONArray("speed_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(SpeedSample(sObj.getInt("t"), sObj.getDouble("v")))
                                }
                            }
                        },
                        powerSamples = mutableListOf<PowerSample>().apply {
                            val arr = obj.optJSONArray("power_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(PowerSample(sObj.getInt("t"), sObj.getDouble("w")))
                                }
                            }
                        },
                        cadenceSamples = mutableListOf<CadenceSample>().apply {
                            val arr = obj.optJSONArray("cad_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(CadenceSample(sObj.getInt("t"), sObj.getDouble("c")))
                                }
                            }
                        },
                        elevationSamples = mutableListOf<ElevationSample>().apply {
                            val arr = obj.optJSONArray("elev_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(ElevationSample(sObj.getInt("t"), sObj.getDouble("a")))
                                }
                            }
                        },
                        strideLengthSamples = mutableListOf<RunningDynamicSample>().apply {
                            val arr = obj.optJSONArray("stride_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(RunningDynamicSample(sObj.getInt("t"), sObj.getDouble("v")))
                                }
                            }
                        },
                        gctSamples = mutableListOf<RunningDynamicSample>().apply {
                            val arr = obj.optJSONArray("gct_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(RunningDynamicSample(sObj.getInt("t"), sObj.getDouble("v")))
                                }
                            }
                        },
                        verticalOscillationSamples = mutableListOf<RunningDynamicSample>().apply {
                            val arr = obj.optJSONArray("vo_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(RunningDynamicSample(sObj.getInt("t"), sObj.getDouble("v")))
                                }
                            }
                        },
                        verticalRatioSamples = mutableListOf<RunningDynamicSample>().apply {
                            val arr = obj.optJSONArray("vr_samples")
                            if (arr != null) {
                                for (j in 0 until arr.length()) {
                                    val sObj = arr.getJSONObject(j)
                                    add(RunningDynamicSample(sObj.getInt("t"), sObj.getDouble("v")))
                                }
                            }
                        }

                    ))
                } catch (e: Exception) { e.printStackTrace() }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    
    
    /**
     * Removes DrawRun activities without externalId that may be blocking sync.
     * These are likely training plan workouts that were incorrectly saved as completed activities.
     */
    fun cleanOrphanDrawRunActivities(context: Context): Int {
        val history = loadHistory(context).toMutableList()
        val before = history.size
        history.removeAll { it.source == "DrawRun" && it.externalId == null }
        val removed = before - history.size
        if (removed > 0) {
            saveHistoryList(context, history)
        }
        return removed
    }
    
    fun deleteActivity(context: Context, activityId: String) {
        val history = loadHistory(context).toMutableList()
        val activity = history.find { it.id == activityId }
        
        // Add both internal ID and external ID to blacklist
        if (activity != null) {
            addToBlacklist(context, activity.id)
            activity.externalId?.let { addToBlacklist(context, it) }
        } else {
            // Fallback for ID-only delete if called from UI without full object
            addToBlacklist(context, activityId)
        }
        
        history.removeAll { it.id == activityId }
        saveHistoryList(context, history)
    }

    // --- Blacklist Management ---
    
    fun addToBlacklist(context: Context, id: String) {
        val list = loadBlacklist(context).toMutableSet()
        list.add(id)
        saveBlacklist(context, list)
    }
    
    fun isBlacklisted(context: Context, id: String?): Boolean {
        if (id == null) return false
        return loadBlacklist(context).contains(id)
    }
    
    private fun loadBlacklist(context: Context): Set<String> {
        val file = File(context.filesDir, BLACKLIST_FILE)
        if (!file.exists()) return emptySet()
        return try {
            val arr = org.json.JSONArray(file.readText())
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) set.add(arr.getString(i))
            set
        } catch (e: Exception) { emptySet() }
    }
    
    private fun saveBlacklist(context: Context, list: Set<String>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { arr.put(it) }
            val file = File(context.filesDir, BLACKLIST_FILE)
            file.writeText(arr.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("history_activities")
            .remove("saved_swims")
            .remove("saved_run_plans")
            .apply()
        
        try {
            val file = File(context.filesDir, HISTORY_FILE)
            if (file.exists()) file.delete()
        } catch (e: Exception) {}
    }
    
    // PR Logic
    private fun loadRecordDetails(prefs: android.content.SharedPreferences, key: String): RecordDetails? {
        if (!prefs.contains("${key}_duration")) return null
        return RecordDetails(
            duration = prefs.getLong("${key}_duration", 0),
            activityId = prefs.getString("${key}_id", "") ?: "",
            date = prefs.getLong("${key}_date", 0),
            distance = prefs.getFloat("${key}_dist", 0f).toDouble()
        )
    }

    fun loadPersonalRecords(context: Context): PersonalRecords {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PersonalRecords(
            best1k = loadRecordDetails(prefs, "pr_1k"),
            best5k = loadRecordDetails(prefs, "pr_5k"),
            best10k = loadRecordDetails(prefs, "pr_10k"),
            bestHalf = loadRecordDetails(prefs, "pr_half"),
            bestMarathon = loadRecordDetails(prefs, "pr_mara"),
            longestRunKm = loadRecordDetails(prefs, "pr_longest"),
            
            best100mSwim = loadRecordDetails(prefs, "pr_swim_100"),
            best200mSwim = loadRecordDetails(prefs, "pr_swim_200"),
            best400mSwim = loadRecordDetails(prefs, "pr_swim_400"),
            best800mSwim = loadRecordDetails(prefs, "pr_swim_800"),
            best1500mSwim = loadRecordDetails(prefs, "pr_swim_1500"),
            longestSwimDist = loadRecordDetails(prefs, "pr_longest_swim")
        )
    }

    private fun updatePersonalRecords(context: Context, activity: CompletedActivity) {
        val currentPRs = loadPersonalRecords(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        var updated = false

        fun updatePR(current: RecordDetails?, targetDist: Double, key: String, tolerance: Double = 0.05): Boolean {
            if (activity.distanceKm >= targetDist * (1.0 - tolerance) && activity.distanceKm <= targetDist * (1.0 + tolerance)) {
                val durationSec = activity.durationMin * 60L
                if (current == null || durationSec < current.duration) {
                    prefs.putLong("${key}_duration", durationSec)
                    prefs.putString("${key}_id", activity.id)
                    prefs.putLong("${key}_date", activity.date)
                    prefs.putFloat("${key}_dist", activity.distanceKm.toFloat())
                    return true
                }
            }
            return false
        }

        if (activity.type == WorkoutType.RUNNING) {
            // Longest Run
            if (activity.distanceKm < 100.0 && (currentPRs.longestRunKm == null || activity.distanceKm > currentPRs.longestRunKm.distance)) {
                prefs.putLong("pr_longest_duration", activity.durationMin * 60L)
                prefs.putString("pr_longest_id", activity.id)
                prefs.putLong("pr_longest_date", activity.date)
                prefs.putFloat("pr_longest_dist", activity.distanceKm.toFloat())
                updated = true
            }
            
            if (updatePR(currentPRs.best1k, 1.0, "pr_1k")) updated = true
            if (updatePR(currentPRs.best5k, 5.0, "pr_5k")) updated = true
            if (updatePR(currentPRs.best10k, 10.0, "pr_10k")) updated = true
            if (updatePR(currentPRs.bestHalf, 21.0975, "pr_half", 0.02)) updated = true
            if (updatePR(currentPRs.bestMarathon, 42.195, "pr_mara", 0.02)) updated = true
            
        } else if (activity.type == WorkoutType.SWIMMING) {
            // Longest Swim
            if (currentPRs.longestSwimDist == null || activity.distanceKm > currentPRs.longestSwimDist.distance) {
                prefs.putLong("pr_longest_swim_duration", activity.durationMin * 60L)
                prefs.putString("pr_longest_swim_id", activity.id)
                prefs.putLong("pr_longest_swim_date", activity.date)
                prefs.putFloat("pr_longest_swim_dist", activity.distanceKm.toFloat())
                updated = true
            }

            if (updatePR(currentPRs.best100mSwim, 0.1, "pr_swim_100")) updated = true
            if (updatePR(currentPRs.best200mSwim, 0.2, "pr_swim_200")) updated = true
            if (updatePR(currentPRs.best400mSwim, 0.4, "pr_swim_400")) updated = true
            if (updatePR(currentPRs.best800mSwim, 0.8, "pr_swim_800")) updated = true
            if (updatePR(currentPRs.best1500mSwim, 1.5, "pr_swim_1500")) updated = true
        }

        if (updated) prefs.apply()
        updateGearUsage(context, activity)
    }

    // ------------------------------------------------
    // GEAR LOGIC
    // ------------------------------------------------
    fun loadGear(context: Context): List<Gear> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("user_gear", null) ?: return emptyList()
        val list = mutableListOf<Gear>()
        try {
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Gear(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    currentDistanceKm = obj.getDouble("cur"),
                    maxDistanceKm = obj.getDouble("max"),
                    isActive = obj.optBoolean("active", true)
                ))
            }
        } catch (e: Exception) {}
        return list
    }
    
    fun saveGear(context: Context, gears: List<Gear>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        gears.forEach { g ->
            val obj = org.json.JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("type", g.type)
            obj.put("cur", g.currentDistanceKm)
            obj.put("max", g.maxDistanceKm)
            obj.put("active", g.isActive)
            jsonArray.put(obj)
        }
        prefs.edit().putString("user_gear", jsonArray.toString()).apply()
    }
    
    private fun updateGearUsage(context: Context, activity: CompletedActivity) {
        if (activity.type == WorkoutType.RUNNING) {
            val gears = loadGear(context).toMutableList()
            // Naive: Update first active running shoe. Better: Activity has selectedGearId
            // For now, assume default shoe (first active shoe)
            val shoeIndex = gears.indexOfFirst { it.type == "Shoe" && it.isActive }
            if (shoeIndex >= 0) {
                val s = gears[shoeIndex]
                gears[shoeIndex] = s.copy(currentDistanceKm = s.currentDistanceKm + activity.distanceKm)
                saveGear(context, gears)
            }
        }
    }

    // ------------------------------------------------
    // RECALCULATE RECORDS (Fix for missing PRs)
    // ------------------------------------------------
    fun recalculateRecords(context: Context): PersonalRecords {
        val history = loadHistory(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        
        // Use mutable map to track best durations
        val bestDurations = mutableMapOf<String, Pair<Long, CompletedActivity>>()
        var longestRun: Pair<Double, CompletedActivity>? = null
        var longestSwim: Pair<Double, CompletedActivity>? = null
        
        fun checkAndStore(key: String, duration: Long, activity: CompletedActivity) {
            val current = bestDurations[key]
            if (current == null || duration < current.first) {
                bestDurations[key] = duration to activity
            }
        }

        history.forEach { a ->
            if (a.type == WorkoutType.RUNNING) {
                if (longestRun == null || a.distanceKm > longestRun!!.first) {
                    longestRun = a.distanceKm to a
                }
                
                val dur = a.durationMin * 60L
                if (a.distanceKm >= 0.95 && a.distanceKm <= 1.05) checkAndStore("pr_1k", dur, a)
                if (a.distanceKm >= 4.75 && a.distanceKm <= 5.25) checkAndStore("pr_5k", dur, a)
                if (a.distanceKm >= 9.5 && a.distanceKm <= 10.5) checkAndStore("pr_10k", dur, a)
                if (a.distanceKm >= 20.5 && a.distanceKm <= 21.5) checkAndStore("pr_half", dur, a)
                if (a.distanceKm >= 41.0 && a.distanceKm <= 43.0) checkAndStore("pr_mara", dur, a)

                // Splits logic for running records
                val splits = a.splits.sortedBy { it.kmIndex }
                fun checkSplits(km: Int): Long? {
                    if (splits.size < km) return null
                    var minTime = Long.MAX_VALUE
                    for (i in 0..splits.size - km) {
                        var sum = 0L
                        var valid = true
                        val startIdx = splits[i].kmIndex
                        for (j in 0 until km) {
                            if (splits[i + j].kmIndex != startIdx + j) { valid = false; break }
                            sum += splits[i + j].durationSec
                        }
                        if (valid && sum < minTime) minTime = sum
                    }
                    return if (minTime != Long.MAX_VALUE) minTime else null
                }

                checkSplits(1)?.let { checkAndStore("pr_1k", it, a) }
                checkSplits(5)?.let { checkAndStore("pr_5k", it, a) }
                checkSplits(10)?.let { checkAndStore("pr_10k", it, a) }
                checkSplits(21)?.let { checkAndStore("pr_half", it, a) }
                checkSplits(42)?.let { checkAndStore("pr_mara", it, a) }

            } else if (a.type == WorkoutType.SWIMMING) {
                if (longestSwim == null || a.distanceKm > longestSwim!!.first) {
                    longestSwim = a.distanceKm to a
                }
                val dur = a.durationMin * 60L
                if (a.distanceKm >= 0.095 && a.distanceKm <= 0.105) checkAndStore("pr_swim_100", dur, a)
                if (a.distanceKm >= 0.19 && a.distanceKm <= 0.21) checkAndStore("pr_swim_200", dur, a)
                if (a.distanceKm >= 0.38 && a.distanceKm <= 0.42) checkAndStore("pr_swim_400", dur, a)
                if (a.distanceKm >= 0.76 && a.distanceKm <= 0.84) checkAndStore("pr_swim_800", dur, a)
                if (a.distanceKm >= 1.42 && a.distanceKm <= 1.58) checkAndStore("pr_swim_1500", dur, a)
            }
        }
        
        // Helper to save RecordDetails
        fun saveDetails(key: String, pair: Pair<Long, CompletedActivity>?) {
            if (pair == null) return
            prefs.putLong("${key}_duration", pair.first)
            prefs.putString("${key}_id", pair.second.id)
            prefs.putLong("${key}_date", pair.second.date)
            prefs.putFloat("${key}_dist", pair.second.distanceKm.toFloat())
        }

        bestDurations.forEach { (key, pair) -> saveDetails(key, pair) }
        
        longestRun?.let { (dist, act) ->
            prefs.putLong("pr_longest_duration", act.durationMin * 60L)
            prefs.putString("pr_longest_id", act.id)
            prefs.putLong("pr_longest_date", act.date)
            prefs.putFloat("pr_longest_dist", dist.toFloat())
        }
        longestSwim?.let { (dist, act) ->
            prefs.putLong("pr_longest_swim_duration", act.durationMin * 60L)
            prefs.putString("pr_longest_swim_id", act.id)
            prefs.putLong("pr_longest_swim_date", act.date)
            prefs.putFloat("pr_longest_swim_dist", dist.toFloat())
        }

        prefs.apply()
        return loadPersonalRecords(context)
    }

    // ------------------------------------------------
    // MERGER LOGIC
    // ------------------------------------------------
    fun mergeActivities(base: CompletedActivity, other: CompletedActivity): CompletedActivity {
        // Prefer metadata from Strava or richer source
        val priorityOther = other.source.contains("Strava", ignoreCase = true) || 
                           (other.summaryPolyline != null && base.summaryPolyline == null)
        
        val primary = if (priorityOther) other else base
        val secondary = if (priorityOther) base else other

        return primary.copy(
            // Detailed data merging
            avgHeartRate = primary.avgHeartRate ?: secondary.avgHeartRate,
            maxHeartRate = primary.maxHeartRate ?: secondary.maxHeartRate,
            calories = primary.calories ?: secondary.calories,
            elevationGain = primary.elevationGain ?: secondary.elevationGain,
            avgWatts = primary.avgWatts ?: secondary.avgWatts,
            
            // Keep the best summary polyline
            summaryPolyline = primary.summaryPolyline ?: secondary.summaryPolyline,
            
            heartRateSamples = if (primary.heartRateSamples.size > secondary.heartRateSamples.size) 
                               primary.heartRateSamples else secondary.heartRateSamples,
            
            speedSamples = if (primary.speedSamples.size > secondary.speedSamples.size) 
                               primary.speedSamples else secondary.speedSamples,

            powerSamples = if (primary.powerSamples.size > secondary.powerSamples.size) 
                               primary.powerSamples else secondary.powerSamples,

            cadenceSamples = if (primary.cadenceSamples.size > secondary.cadenceSamples.size) 
                               primary.cadenceSamples else secondary.cadenceSamples,

            elevationSamples = if (primary.elevationSamples.size > secondary.elevationSamples.size)
                               primary.elevationSamples else secondary.elevationSamples,

            splits = if (primary.splits.size > secondary.splits.size) primary.splits else secondary.splits,
            
            // Source tracking
            source = if (primary.source == secondary.source) primary.source else "${primary.source} + ${secondary.source}"
        )
    }
}
