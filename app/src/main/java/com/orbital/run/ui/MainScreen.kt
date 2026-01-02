package com.orbital.run.ui

import com.orbital.run.api.SyncManager
import com.orbital.run.logic.UpdateManager
import com.orbital.run.logic.UpdateInfo
// UpdateDialog is in the same package com.orbital.run.ui, no import needed
import com.orbital.run.BuildConfig
import android.net.Uri

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalFocusManager
import com.orbital.run.logic.*
import com.orbital.run.logic.formatDuration
import com.orbital.run.ui.theme.*
import com.orbital.run.social.SocialScreen // NEW IMPORT
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random
import kotlin.math.sin
import androidx.compose.ui.zIndex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner

// Use the new theme colors
val AppBg = AirSurface
val AppCardBg = AirWhite
val AppText = AirTextPrimary
val AirTextLight = AirTextSecondary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    // Start Values
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var restingHR by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var currentVol by remember { mutableStateOf("") }
    var goalDist by remember { mutableStateOf("") }
    var goalTime by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("") }

    var result by remember { mutableStateOf<TrainingPlanResult?>(null) }
    var currentView by remember { mutableStateOf(0) } // 0:Analyse, 1:Dashboard, 2:Plan(Run), 3:Nage(Swim), 4:Settings(Profile)
    
    // Integration State
    val connectedApps = remember { mutableStateMapOf<String, Boolean>() }
    var dataVersion by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Observe lifecycle to refresh Strava status when app resumes from OAuth
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reload Strava token and update state when app comes back from browser
                com.orbital.run.api.StravaAPI.loadToken(context)
                val wasConfigured = connectedApps["Strava"] == true
                val isConfigured = com.orbital.run.api.StravaAPI.isConfigured()
                connectedApps["Strava"] = isConfigured
                
                // If we just connected or coming back, trigger a background sync
                if (isConfigured || com.orbital.run.api.HealthConnectManager.hasAllPermissionsSync(context)) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        com.orbital.run.api.SyncManager.syncAll(context)
                        Persistence.recalculateRecords(context)
                        withContext(Dispatchers.Main) {
                            dataVersion++
                            connectedApps["Garmin"] = com.orbital.run.api.GarminAPI.status == com.orbital.run.api.GarminAPI.ConnectionStatus.CONNECTED
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    var onboardingComplete by remember { mutableStateOf(Persistence.isOnboardingComplete(context)) }
    
    var appToConnect by remember { mutableStateOf<String?>(null) }
    var showGarminLogin by remember { mutableStateOf(false) }
    var syncApp by remember { mutableStateOf<String?>(null) }
    var showNotifDialog by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }
    val savedSwims = remember { mutableStateListOf<Workout>() }
    val notifications = remember { mutableStateListOf<AppNotification>() }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // Health Connect Permission Launcher (Lifted to MainScreen)
    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = com.orbital.run.api.HealthConnectManager.getPermissionsContract()
    ) { _ ->
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val hasAll = com.orbital.run.api.HealthConnectManager.hasAllPermissions(context)
            if (hasAll) {
                Persistence.saveHealthConnectEnabled(context, true)
                connectedApps["Health Connect"] = true
                android.widget.Toast.makeText(context, "✅ Health Connect autorisé !", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper to regenerate
    fun generate(newProfile: UserProfile? = null) {
        val p = newProfile ?: UserProfile(
            age.toIntOrNull()?:30, isMale, weight.toDoubleOrNull()?:70.0, restingHR.toIntOrNull()?:60,
            currentVol.toDoubleOrNull()?:30.0, goalDist.toDoubleOrNull()?:10.0, goalTime.toDoubleOrNull()?:60.0,
            System.currentTimeMillis() + ((durationWeeks.toIntOrNull()?:12) * 7 * 24 * 60 * 60 * 1000L)
        )
        result = OrbitalAlgorithm.calculate(p)
        Persistence.saveProfile(context, p)
    }
    
    // Unified Auto-Sync at Startup
    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) {
            withContext(Dispatchers.IO) {
                com.orbital.run.api.StravaAPI.loadToken(context)
                
                // Trigger sync only if at least one service is active
                if (com.orbital.run.api.StravaAPI.isConfigured() || com.orbital.run.api.HealthConnectManager.hasAllPermissionsSync(context)) {
                    val count = com.orbital.run.api.SyncManager.syncAll(context)
                    if (count > 0) {
                        Persistence.recalculateRecords(context)
                        withContext(Dispatchers.Main) {
                            dataVersion++
                        }
                    }
                }
            }
        }
    }

    // Load Profile & Swims Data at Startup
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val saved = Persistence.loadProfile(context)
            withContext(Dispatchers.Main) {
                if (saved != null) {
                    age = saved.age.toString()
                    weight = saved.weightKg.toString()
                    restingHR = saved.restingHeartRate.toString()
                    isMale = saved.isMale
                    currentVol = saved.currentWeeklyDistanceKm.toString()
                    generate(saved)
                }
            }
            
            val swims = Persistence.loadSwims(context)
            withContext(Dispatchers.Main) {
                savedSwims.clear()
                savedSwims.addAll(swims)
            }
            
            
            // Initial check for connected apps
            val stravaAuth = com.orbital.run.api.StravaAPI.isConfigured()
            // Updated check: uses both permissions AND enabled flag
            val hcAuth = com.orbital.run.api.HealthConnectManager.isIntegrationEnabled(context)
            
            // Restore Garmin Session
            val garminEmail = Persistence.loadGarminEmail(context)
            if (garminEmail != null && garminEmail.isNotBlank()) {
                com.orbital.run.api.GarminAPI.restoreSession(garminEmail)
            }
            
            withContext(Dispatchers.Main) {
                connectedApps["Strava"] = stravaAuth
                connectedApps["Health Connect"] = hcAuth
                // Garmin depends on live status which might update later during sync, 
                // but efficiently check if we have email and no error yet.
                connectedApps["Garmin"] = com.orbital.run.api.GarminAPI.status == com.orbital.run.api.GarminAPI.ConnectionStatus.CONNECTED
            }

        }
        
        // Check for updates
        withContext(Dispatchers.IO) {
            val info = UpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
            withContext(Dispatchers.Main) {
                if (info != null) {
                    updateInfo = info
                }
            }
        }
    }
    
    fun generateRecovery() {
        if (result != null) {
            result = OrbitalAlgorithm.generateRecoveryPlan(result!!.userProfile)
            showCelebration = false
        }
    }

    Scaffold(
        containerColor = AppBg,
        topBar = { 
            Column {
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.height(12.dp))
                if (onboardingComplete) TopBar(notifications.size) { showNotifDialog = true } 
            }
        },
        bottomBar = { if (result != null && onboardingComplete) BottomNav(currentView) { currentView = it } }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!onboardingComplete) {
                if (result == null) {
                    // Initial Input Form
                    InputForm(
                        age, { age = it }, weight, { weight = it }, restingHR, { restingHR = it },
                        isMale, { isMale = !isMale }, currentVol, { currentVol = it },
                        goalDist, { goalDist = it }, goalTime, { goalTime = it }, durationWeeks, { durationWeeks = it },
                        onGenerate = { generate() }
                    )
                } else {
                    // Step 2: Sync Onboarding
                    val isStravaConnected = com.orbital.run.api.StravaAPI.isConfigured()
                    // Simplified HC check for UI
                    SyncOnboardingScreen(
                        onConnectStrava = { appToConnect = "Strava" },
                        onConnectHealthConnect = { 
                            // This usually triggers permissions in MainActivity, 
                            // for UI feedback we can toggle a dummy state or wait for next frame
                            appToConnect = "Health Connect" 
                        },
                        onConnectGarmin = { showGarminLogin = true },
                        onConnectPolar = { com.orbital.run.api.PolarAPI.openAuthorizationPage(context) },
                        onConnectSuunto = { com.orbital.run.api.SuuntoAPI.openAuthorizationPage(context) },
                        onFinish = {
                            Persistence.setOnboardingComplete(context, true)
                            // Hard restart for clean state
                            val intent = android.content.Intent(context, com.orbital.run.MainActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            context.startActivity(intent)
                        },
                        isStravaConnected = isStravaConnected,
                        isHealthConnectConnected = connectedApps["Health Connect"] ?: false,
                        isGarminConnected = connectedApps["Garmin"] ?: false,
                        isPolarConnected = connectedApps["Polar Flow"] ?: false,
                        isSuuntoConnected = connectedApps["Suunto App"] ?: false
                    )
                }
            } else {
                // Views
                AnimatedContent(targetState = currentView, label = "ViewTransition") { view ->
                    when (view) {
                        0 -> AnalyticsScreen(
                                context = context,
                                onNavigateToRecap = { currentView = 4 },
                                trainingPlan = result
                             )
                        1 -> DashboardScreen(trainingPlan = result!!)
                        2 -> PlanScreen(
                                trainingPlan = result!!,
                                context = context,
                                onActivatePlan = { result = it },
                                connectedApps = connectedApps, 
                                onSyncWorkout = { workout -> shareWorkout(context, workout) },
                                onFinishPlan = { showCelebration = true }
                             )
                        3 -> SwimScreen(
                           savedSwims = savedSwims,
                           onSave = { w -> 
                               savedSwims.add(0, w)
                               Persistence.saveSwims(context, savedSwims)
                           },
                           onSync = { syncApp = "Garmin" },
                           connectedApps = connectedApps,
                           onSyncWithApp = { w -> shareWorkout(context, w) },
                           onDeleteSwim = { w -> 
                               savedSwims.remove(w)
                               Persistence.saveSwims(context, savedSwims)
                           }
                        ) 
                        4 -> RecapScreen(context, dataVersion, onNavigateToSettings = { currentView = 5 })
                        5 -> {
                            androidx.activity.compose.BackHandler { currentView = 4 }
                            ProfileSettingsScreen(
                                result!!.userProfile, 
                                connectedApps,
                                onConnect = { appToConnect = it },
                                onDisconnect = { app ->
                                     connectedApps[app] = false
                                     if (app == "Garmin") {
                                         com.orbital.run.api.GarminAPI.disconnect(context)
                                     }
                                     if (app == "Health Connect") {
                                         Persistence.saveHealthConnectEnabled(context, false)
                                     }
                                },
                                onUpdateProfile = { updated -> 
                                    age = updated.age.toString()
                                    weight = updated.weightKg.toString()
                                    generate(updated)
                                },
                                onResetData = {
                                    com.orbital.run.logic.Persistence.clearHistory(context)
                                    // Optionally clear state? MainScreen reload?
                                    // For now, reload swims
                                    savedSwims.clear()
                                },
                                onCheckForUpdate = {
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        val info = UpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
                                        withContext(Dispatchers.Main) {
                                            if (info != null) {
                                                updateInfo = info
                                            } else {
                                                android.widget.Toast.makeText(context, "Votre application est à jour (v${BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onBack = { currentView = 4 }
                            )
                        }

                    }

                }
            }
            
            // Dialogs
            if (appToConnect != null) {
                AppConnectDialog(
                    appName = appToConnect!!,
                    onDismiss = { appToConnect = null },
                    onConnect = {
                        // Ouvrir le flux OAuth selon l'app
                        when(appToConnect) {
                            "Garmin Connect" -> {
                                // Now using Vercel Proxy -> Show Dialog
                                showGarminLogin = true
                                appToConnect = null
                            }
                            "Strava" -> {
                                com.orbital.run.api.StravaAPI.openAuthorizationPage(context)
                            }
                            "Health Connect" -> {
                                try {
                                    hcPermissionLauncher.launch(com.orbital.run.api.HealthConnectManager.getPermissionsToRequest())
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Erreur: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            "Polar Flow" -> com.orbital.run.api.PolarAPI.openAuthorizationPage(context)
                            "Suunto App" -> com.orbital.run.api.SuuntoAPI.openAuthorizationPage(context)
                        }
                        // State will be updated by lifecycle observer when app resumes
                        appToConnect = null
                    }
                )
            }
            
            if (syncApp != null) {
                SyncDialog(
                    appName = syncApp!!,
                    onDismiss = { syncApp = null }
                )
            }
            
            if (showGarminLogin) {
                GarminLoginDialog(
                    onDismiss = { showGarminLogin = false },
                    onLogin = { email, pass ->
                        com.orbital.run.api.GarminAPI.login(email, pass) { success, msg ->
                             showGarminLogin = false
                             if (success) {
                                 // Update State 
                                 connectedApps["Garmin"] = true
                                 Persistence.saveGarminEmail(context, email)
                                 Persistence.saveGarminPassword(context, pass) // Ensure password is saved
                                 kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                     android.widget.Toast.makeText(context, "Garmin connecté !", android.widget.Toast.LENGTH_SHORT).show()
                                 }
                             } else {
                                 kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                     android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                                 }
                             }
                        }
                    }
                )
            }

            
            // Update Dialog
            if (updateInfo != null) {
                UpdateDialog(
                    updateInfo = updateInfo!!,
                    onUpdate = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo!!.downloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Impossible d'ouvrir le lien", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { updateInfo = null }
                )
            }
            
            // Notification Dialog
            if (showNotifDialog) {
                NotificationDialog(
                    notifications = notifications,
                    onDismiss = { showNotifDialog = false },
                    onClear = { notifications.clear() }
                )
            }
            
            // Celebration Overlay
            if (showCelebration) {
                // Confetti Particles
                InternalConfetti(modifier = Modifier.zIndex(10f))
                
                AlertDialog(
                    onDismissRequest = { showCelebration = false },
                    title = { Text("Félicitations !", color = AirPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                    text = { Text("Vous avez terminé votre programme et votre course. C'est une immense réussite !\n\nQue voulez-vous faire maintenant ?", color = AppText) },
                    confirmButton = {
                        Button(onClick = { generateRecovery() }, colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)) {
                            Text("RÉCUPÉRATION (2 SA)")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            result = null // Reset to input
                            showCelebration = false 
                        }) {
                            Text("NOUVEAU DÉFI", color = AirTextPrimary)
                        }
                    },
                    containerColor = AirWhite,
                    modifier = Modifier.zIndex(11f)
                )
            }
            // ...
        }
    }
}
// ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    trainingPlan: TrainingPlanResult, 
    context: android.content.Context,
    onActivatePlan: (TrainingPlanResult) -> Unit,
    connectedApps: Map<String, Boolean> = emptyMap(), 
    onSyncWorkout: (Workout) -> Unit = {}, 
    @Suppress("UNUSED_PARAMETER") onFinishPlan: () -> Unit = {}
) {
    // Plan Manager State
    var plans by remember { mutableStateOf<List<Persistence.StoredPlan>>(emptyList()) }
    var showGenerator by remember { mutableStateOf(false) }
    var refreshPlans by remember { mutableStateOf(0) }
    var selectedPlan by remember { mutableStateOf<Persistence.StoredPlan?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Persistence.StoredPlan?>(null) }
    
    LaunchedEffect(refreshPlans) {
        plans = withContext(Dispatchers.IO) { Persistence.loadRunPlans(context) }
    }
    
    if (showGenerator) {
        RunPlanGenerator(
            userProfile = trainingPlan.userProfile,
            onCancel = { showGenerator = false },
            onGenerate = { newRes, title, goal ->
                 val stored = Persistence.StoredPlan(title = title, createdDate = System.currentTimeMillis(), goal = goal, result = newRes)
                 Persistence.saveRunPlan(context, stored)
                 refreshPlans++
                 // onActivatePlan(newRes) // Don't auto-activate globally? Or yes? User said "Only selection displays"
                 // Implementation: Just save.
                 selectedPlan = stored
                 showGenerator = false
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Compact header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mes Plans", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppText)
                    OutlinedButton(
                        onClick = { showGenerator = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nouveau", fontSize = 12.sp)
                    }
                }
            }
            
            // Compact saved plans list
            if (plans.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plans.forEach { p ->
                            val isSelected = (selectedPlan?.id == p.id)
                            Card(
                                onClick = { 
                                    selectedPlan = if (selectedPlan == p) null else p
                                    if (selectedPlan != null) onActivatePlan(p.result)
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if(isSelected) AirPrimary.copy(alpha=0.15f) else Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = if(isSelected) BorderStroke(2.dp, AirPrimary) else BorderStroke(1.dp, AirSurface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).width(160.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            p.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if(isSelected) AirPrimary else AppText,
                                            maxLines = 1
                                        )
                                        Text(
                                            p.goal,
                                            fontSize = 10.sp,
                                            color = AirTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = p },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = AirTextLight, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Selected Plan Content
            if (selectedPlan != null) {
                item {
                    Text("${selectedPlan!!.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AirPrimary, modifier = Modifier.padding(horizontal = 16.dp))
                }
                items(selectedPlan!!.result.weeklyPlan) { week -> WeekCard(week, trainingPlan, connectedApps, onSyncWorkout) }
            } else {
                 item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AirSurface.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.DirectionsRun, null, tint = AirTextLight, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aucun plan sélectionné", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppText)
                            Text("Créez un nouveau plan", fontSize = 12.sp, color = AirTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                 }
             }
            }
        }
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { planToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Supprimer le plan ?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer \"${planToDelete.title}\" ?\n\nCette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        Persistence.deleteRunPlan(context, planToDelete.id)
                        refreshPlans++
                        if (selectedPlan?.id == planToDelete.id) selectedPlan = null
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunPlanGenerator(
    userProfile: UserProfile,
    onCancel: () -> Unit,
    onGenerate: (TrainingPlanResult, String, String) -> Unit
) {
    var goalDist by remember { mutableStateOf("10.0") }
    var goalTime by remember { mutableStateOf("60.0") }
    var raceDateMillis by remember { mutableStateOf(System.currentTimeMillis() + (12 * 7 * 24 * 60 * 60 * 1000L)) } // Default 12 weeks
    var vol by remember { mutableStateOf(userProfile.currentWeeklyDistanceKm.toString()) }
    var title by remember { mutableStateOf("Mon Plan") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Calculate weeks from race date
    val weeksUntilRace = remember(raceDateMillis) {
        val now = System.currentTimeMillis()
        val diffMillis = raceDateMillis - now
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
        (diffDays / 7).toInt().coerceAtLeast(1)
    }
    
    Column(modifier = Modifier.fillMaxSize().background(AppBg).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nouveau Plan Running", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppText)
        AirCard("Paramètres") {
            AirInput(title, { title = it }, "Nom du Plan", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            Spacer(modifier = Modifier.height(16.dp))
            AirInput(vol, { vol = it }, "Volume Actuel (km/semaine)")
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AirInput(goalDist, { goalDist = it }, "Objectif Dist (km)", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                 AirInput(goalTime, { goalTime = it }, "Objectif Temps (min)", Modifier.weight(1f))
            }
            
            // Race Date Picker Button
            Spacer(modifier = Modifier.height(16.dp))
            Text("Date de Course", fontSize = 13.sp, color = AirTextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AirSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏁 ${formatDate(raceDateMillis)}",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$weeksUntilRace semaines",
                        color = AirPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Show calculated target pace
            Spacer(modifier = Modifier.height(12.dp))
            val targetPace = if (goalDist.toDoubleOrNull() != null && goalTime.toDoubleOrNull() != null) {
                val dist = goalDist.toDouble()
                val time = goalTime.toDouble()
                if (dist > 0) {
                    val pace = time / dist
                    val min = pace.toInt()
                    val sec = ((pace - min) * 60).toInt()
                    "$min:${sec.toString().padStart(2, '0')}/km"
                } else "--"
            } else "--"
            
Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AirPrimary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🎯 Allure Cible", fontSize = 13.sp, color = AirTextSecondary)
                    Text(targetPace, fontSize = 20.sp, color = AirPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp)) { Text("Annuler") }
            Button(
                onClick = {
                    val p = userProfile.copy(
                        currentWeeklyDistanceKm = vol.toDoubleOrNull() ?: 30.0,
                        goalDistanceKm = goalDist.toDoubleOrNull() ?: 10.0,
                        goalTimeMinutes = goalTime.toDoubleOrNull() ?: 60.0,
                        raceDateMillis = raceDateMillis
                    )
                    val res = OrbitalAlgorithm.calculate(p)
                    onGenerate(res, title, "${goalDist}km en ${goalTime}min")
                },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
            ) {
                Text("GÉNÉRER")
            }
        }
    }
    
    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = raceDateMillis
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        raceDateMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = AirPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(ms))
}

@Composable
fun TopBar(notifCount: Int, onNotif: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Updated: Only Title
        Text("DrawRun", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AirPrimary, letterSpacing = 1.sp)
        
        Box {
            IconButton(onClick = onNotif) {
                Icon(Icons.Default.Notifications, null, tint = if(notifCount > 0) AirAccent else AirTextLight)
            }
            if(notifCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp).background(AirAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$notifCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomNav(current: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = AirWhite) {
        NavigationBarItem(
            selected = current == 0, onClick = { onSelect(0) },
            icon = { Icon(Icons.Default.ShowChart, null) }, 
            label = { Text("Analyse") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AirPrimary, selectedTextColor = AirPrimary, indicatorColor = AirSurface)
        )
        NavigationBarItem(
            selected = current == 1, onClick = { onSelect(1) },
            icon = { Icon(Icons.Rounded.Speed, null) },
            label = { Text("Stats") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AirPrimary, selectedTextColor = AirPrimary, indicatorColor = AirSurface)
        )
        NavigationBarItem(
            selected = current == 2, onClick = { onSelect(2) },
            icon = { Icon(Icons.Default.CalendarMonth, null) },
            label = { Text("Run") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AirPrimary, selectedTextColor = AirPrimary, indicatorColor = AirSurface)
        )
        NavigationBarItem(
            selected = current == 3, onClick = { onSelect(3) },
            icon = { Icon(Icons.Default.Pool, null) },
            label = { Text("Nage") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AirPrimary, selectedTextColor = AirPrimary, indicatorColor = AirSurface)
        )
        NavigationBarItem(
            selected = current == 4, onClick = { onSelect(4) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profil") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AirPrimary, selectedTextColor = AirPrimary, indicatorColor = AirSurface)
        )
    }
}

@Composable
fun InputForm(
    age: String, onAge: (String) -> Unit, weight: String, onWeight: (String) -> Unit,
    hr: String, onHr: (String) -> Unit, isMale: Boolean, onGender: () -> Unit,
    // Removed Goals
    @Suppress("UNUSED_PARAMETER") _currentVol: String, @Suppress("UNUSED_PARAMETER") _onVol: (String) -> Unit, @Suppress("UNUSED_PARAMETER") _goalDist: String, @Suppress("UNUSED_PARAMETER") _onGoalDist: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") _goalTime: String, @Suppress("UNUSED_PARAMETER") _onGoalTime: (String) -> Unit, @Suppress("UNUSED_PARAMETER") _duration: String, @Suppress("UNUSED_PARAMETER") _onDuration: (String) -> Unit,
    onGenerate: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Initialisation", fontSize = 24.sp, fontWeight = FontWeight.Light, color = AppText)
        AirCard("Physiologie") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AirInput(age, onAge, "Âge", Modifier.weight(1f))
                AirInput(weight, onWeight, "Poids (kg)", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AirInput(hr, onHr, "FC Repos", Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(AirSurface).clickable { onGender() }, contentAlignment = Alignment.Center) {
                    Text(if (isMale) "Homme" else "Femme", color = AppText, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Removed Objectif Principal inputs from Onboarding
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)) {
            Text("COMMENCER", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(trainingPlan: TrainingPlanResult) {
    val analysisVm: AnalysisViewModel = viewModel()
    val coachInsight by analysisVm.coachInsight.collectAsState()
    var selectedMetric by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        
        CoachInsightSection(insight = coachInsight)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CircularGauge(title = "VMA", value = String.format("%.1f", trainingPlan.vma), unit = "km/h", percent = (trainingPlan.vma.toFloat()/22f), color = AirSecondary) {
                selectedMetric = "VMA"
            }
            CircularGauge(title = "VO2Max", value = String.format("%.0f", trainingPlan.vo2max), unit = "ml/kg/min", percent = (trainingPlan.vo2max.toFloat()/80f), color = AirPrimary) {
                selectedMetric = "VO2Max"
            }
            CircularGauge(title = "FCM", value = "${trainingPlan.fcm}", unit = "bpm", percent = (trainingPlan.fcm.toFloat()/220f), color = AirAccent) {
                selectedMetric = "FCM"
            }
        }
        
        Column {
            Text("Prédictions de Course", color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                trainingPlan.racePredictions.forEach { pred ->
                    RaceTimeCard(pred.distanceName, pred.formattedTime)
                }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        val pagerState = rememberPagerState { 3 }
        val scope = rememberCoroutineScope()

        TabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, contentColor = AirPrimary, indicator = { tabPositions ->
              Box(
                 Modifier
                     .fillMaxWidth()
                     .wrapContentSize(Alignment.BottomStart)
                     .offset(x = tabPositions[pagerState.currentPage].left)
                     .width(tabPositions[pagerState.currentPage].width)
                     .height(3.dp)
                     .background(AirPrimary)
             )
        }) {
            Tab(selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0) } }, text = { Text("Cardio", fontSize = 11.sp, maxLines = 1, softWrap = false) })
            Tab(selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1) } }, text = { Text("Allure", fontSize = 11.sp, maxLines = 1, softWrap = false) })
            Tab(selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.animateScrollToPage(2) } }, text = { Text("Puissance", fontSize = 11.sp, maxLines = 1, softWrap = false) })
        }
        
        HorizontalPager(state = pagerState, modifier = Modifier.height(280.dp), verticalAlignment = Alignment.Top) { page ->
             Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                  when(page) {
                    0 -> trainingPlan.hrZones.forEach { ZoneBar(it.label, "${it.min}-${it.max}", it.max.toFloat()/trainingPlan.fcm, getZoneColor(it.id)) }
                    1 -> trainingPlan.speedZones.forEach { 
                         ZoneBar("Z${it.id}", "${formatPace(it.maxSpeedKmh)} - ${formatPace(it.minSpeedKmh)} /km", it.id/5f, getZoneColor(it.id)) 
                    }
                    2 -> trainingPlan.powerZones.forEach { ZoneBar("Z${it.id}", "${it.minWatts}-${it.maxWatts}W", it.id/5f, getZoneColor(it.id)) }
                }
             }
        }
        
        // Simulator
        AirCard("Simulateur") {
            var simDist by remember { mutableStateOf("") }
            // State to hold results
            var resultTime by remember { mutableStateOf("") }
            var resultPace by remember { mutableStateOf("") }
            var resultZone by remember { mutableStateOf<Int?>(null) }
            var resultTimeZones by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
            val focusManager = LocalFocusManager.current
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = simDist, 
                    onValueChange = { simDist = it }, 
                    label = { Text("Dist (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val dist = simDist.toDoubleOrNull()
                        if (dist != null && dist > 0) {
                             val timeMin = OrbitalAlgorithm.estimateRaceTime(trainingPlan.vma, dist)
                             val hours = (timeMin / 60).toInt()
                             val mins = (timeMin % 60).toInt()
                             resultTime = if(hours > 0) String.format("%dh%02d", hours, mins) else "${mins}min"
                             
                             val paceMinPerKm = timeMin / dist
                             val paceMin = paceMinPerKm.toInt()
                             val paceSec = ((paceMinPerKm - paceMin) * 60).toInt()
                             resultPace = "%d:%02d".format(paceMin, paceSec)
                             
                             val speedKmh = dist / (timeMin / 60)
                             val zone = trainingPlan.speedZones.find { speedKmh >= it.minSpeedKmh && speedKmh <= it.maxSpeedKmh } ?: 
                                        if(speedKmh < trainingPlan.speedZones.first().minSpeedKmh) trainingPlan.speedZones.first() else trainingPlan.speedZones.last()
                             resultZone = zone.id
                             
                             // Calculate Time Ranges for all zones
                             resultTimeZones = trainingPlan.speedZones.map { z ->
                                 val tMin = dist / z.maxSpeedKmh * 60
                                 val tMax = dist / z.minSpeedKmh * 60
                                 
                                 fun fmt(m: Double): String {
                                     val h = (m/60).toInt()
                                     val mn = (m%60).toInt()
                                     return if(h>0) String.format("%dh%02d", h, mn) else "${mn}min"
                                 }
                                 z.id to "${fmt(tMin)} - ${fmt(tMax)}"
                             }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("SIMULER")
                }
            }
            
            if (resultTimeZones.isNotEmpty()) {
                 Spacer(modifier = Modifier.height(16.dp))
                 Divider(color = AirSurface, thickness = 1.dp)
                 Spacer(modifier = Modifier.height(8.dp))
                 Text("Zones de Temps pour cette distance :", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AirTextPrimary)
                 Spacer(modifier = Modifier.height(4.dp))
                 
                 resultTimeZones.forEach { (id, range) ->
                     Row(
                         modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Box(modifier = Modifier.size(8.dp).background(getZoneColor(id), CircleShape))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Zone $id", color = AppText, fontSize = 12.sp)
                        }
                        Text(range, color = AppText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                     }
                 }
            }
        }
    }

    selectedMetric?.let { metric ->
        MetricExplanationDialog(metric) { selectedMetric = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwimScreen(
    savedSwims: List<Workout> = emptyList(),
    onSave: (Workout) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onSync: () -> Unit = {},

    connectedApps: Map<String, Boolean> = emptyMap(),
    onSyncWithApp: (Workout) -> Unit = {},
    onDeleteSwim: (Workout) -> Unit = {}
) {
    // Multi-select state
    var selectedStyles by remember { mutableStateOf(listOf(SwimStyle.MIXED)) }
    
    var targetType by remember { mutableStateOf("Distance") } // Distance, Temps
    var targetValue by remember { mutableStateOf("2500") } // m or min
    var sessionType by remember { mutableStateOf(SwimSessionType.ENDURANCE) }
    
    var generatedWorkout by remember { mutableStateOf<Workout?>(null) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Generateur Header
        item {
            AirCard("Générateur Natation") {
                Text("Séances sur mesure", color = AirTextLight, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Style Selector
                Text("Nages (Sélection multiple)", color = AirTextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    SwimStyle.values().forEach { s ->
                        val isSelected = selectedStyles.contains(s)
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedStyles = if (isSelected) {
                                    if (selectedStyles.size > 1) selectedStyles - s else selectedStyles // Keep at least one
                                } else {
                                    selectedStyles + s
                                }
                            },
                            label = { Text(s.label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                
                // Session Type Selector
                Text("Type de Séance", color = AirTextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    SwimSessionType.values().forEach { t ->
                        FilterChip(
                            selected = sessionType == t,
                            onClick = { sessionType = t },
                            label = { Text(t.label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                // Target Control
                Text("Type d'objectif", color = AirTextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                     FilterChip(selected = targetType == "Distance", onClick = { targetType = "Distance"; targetValue = "2500" }, label = { Text("Distance (m)") }, modifier = Modifier.padding(end = 8.dp))
                     FilterChip(selected = targetType == "Temps", onClick = { targetType = "Temps"; targetValue = "45" }, label = { Text("Temps (min)") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { if(it.all { c -> c.isDigit() }) targetValue = it },
                    label = { Text(if(targetType == "Distance") "Mètres (ex: 2500)" else "Minutes (ex: 45)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val v = targetValue.toIntOrNull() ?: if(targetType=="Distance") 2500 else 45
                        generatedWorkout = SwimAlgorithm.generateSession(selectedStyles, targetType, v, sessionType)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AirSecondary)
                ) {
                    Text("GÉNÉRER LA SÉANCE", color = AppText)
                }
            }
        }
        

        
        // Result
        if (generatedWorkout != null) {
            item {
                Text("VOTRE SÉANCE DU JOUR", color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
            }
            
            // Coaching Card
            item {
                val coachTip = when(sessionType) {
                    SwimSessionType.ENDURANCE -> "Focus : Régularité et glisse. Ne cherche pas la vitesse, mais l'efficience."
                    SwimSessionType.SPEED -> "Focus : Intensité maximale sur les séries. Récupération active très souple."
                    SwimSessionType.TECHNIQUE -> "Focus : Qualité du mouvement. Prends ton temps sur les éducatifs."
                    SwimSessionType.RECOVERY -> "Focus : Relâchement complet. Sens l'eau sans aucune tension."
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AirPrimary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AirPrimary.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TipsAndUpdates, null, tint = AirPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(coachTip, color = AppText, fontSize = 14.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                AirCard {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Surface(
                                 shape = CircleShape,
                                 color = AirSecondary.copy(alpha = 0.2f),
                                 modifier = Modifier.size(48.dp)
                             ) {
                                 Icon(Icons.Rounded.Waves, contentDescription = null, tint = AirSecondary, modifier = Modifier.padding(12.dp))
                             }
                             Spacer(modifier = Modifier.width(16.dp))
                             Column {
                                 Text(generatedWorkout!!.title, color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                 Text("${String.format("%.1f", generatedWorkout!!.totalDistanceKm * 1000)}m • ${formatDuration(generatedWorkout!!.totalDurationMin)}", color = AirTextLight, fontSize = 14.sp)
                             }
                         }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        generatedWorkout!!.steps.forEachIndexed { index, step ->
                             // Phase headers detection (simple)
                             if (step.description.contains("Échauffement") || step.description.contains("Educatif 1") || step.description.contains("Pyramide") || step.description.contains("Sprint") || step.description.contains("Retour au calme")) {
                                 val phaseLabel = when {
                                     step.description.contains("Échauffement") -> "ÉCHAUFFEMENT"
                                     step.description.contains("Educatif") -> "TECHNIQUE"
                                     step.description.contains("Retour au calme") -> "COOLDOWN"
                                     else -> "CORPS DE SÉANCE"
                                 }
                                 if (index > 0) Spacer(Modifier.height(16.dp))
                                 Text(phaseLabel, color = AirPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                 Spacer(Modifier.height(8.dp))
                             }

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when(step.targetZone) {
                                        1 -> ZoneGrey
                                        2 -> ZoneBlue
                                        3 -> ZoneGreen
                                        4 -> ZoneOrange
                                        5 -> ZoneRed
                                        else -> AirSurface
                                    },
                                    modifier = Modifier.width(4.dp).height(42.dp)
                                ) {}
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(step.description, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(step.durationOrDist, color = AirTextLight, fontSize = 13.sp)
                                }
                            }
                        }
                        
                        // Actions
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                             OutlinedButton(
                                 onClick = { onSave(generatedWorkout!!) },
                                 modifier = Modifier.weight(1f),
                                 shape = RoundedCornerShape(12.dp)
                             ) {
                                 Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text("Sauver")
                             }
                             Button(
                                 onClick = { onSyncWithApp(generatedWorkout!!) },
                                 modifier = Modifier.weight(1f),
                                 colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
                                 shape = RoundedCornerShape(12.dp)
                             ) {
                                 Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text("Sync")
                             }
                        }
                     }
                }
            }
        }
        
        // Saved List
        if (savedSwims.isNotEmpty()) {
            item {
                Text("Mes Séances Enregistrées", color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
            }
            items(savedSwims) { swim ->
                ExpandableWorkoutItem(
                    workout = swim,
                    connectedApps = connectedApps,
                    onSync = { onSyncWithApp(swim) },
                    onDelete = { onDeleteSwim(swim) }
                )
            }
        }
    }
}

@Composable
fun ProfileSettingsScreen(
    currentProfile: UserProfile, 
    connectedApps: Map<String, Boolean>,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onResetData: () -> Unit,
    onCheckForUpdate: () -> Unit, // Added
    onBack: () -> Unit
) {
    // Local editable state
    var editMode by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) } // Forces refresh of lists accessing static APIs
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Load persisted token on init
    LaunchedEffect(Unit) {
        com.orbital.run.api.StravaAPI.loadToken(context)
        refreshTrigger++ // Force refresh after load
    }
    
    // Fields
    var age by remember { mutableStateOf(currentProfile.age.toString()) }
    var weight by remember { mutableStateOf(currentProfile.weightKg.toString()) }
    var hr by remember { mutableStateOf(currentProfile.restingHeartRate.toString()) }
    var vol by remember { mutableStateOf(currentProfile.currentWeeklyDistanceKm.toString()) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        // Header with Back Button
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paramètres & Profil", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppText)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats Card
        AirCard("Mes Données") {
             if (editMode) {
                 Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AirInput(age, { age = it }, "Âge", Modifier.weight(1f))
                    AirInput(weight, { weight = it }, "Poids", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                AirInput(hr, { hr = it }, "FC Repos")
                Spacer(modifier = Modifier.height(12.dp))
                AirInput(vol, { vol = it }, "Volume Actuel (km)")
             } else {
                 Text("Âge: ${currentProfile.age} ans", color = AppText)
                 Text("Poids: ${currentProfile.weightKg} kg", color = AppText)
                 Text("FC Repos: ${currentProfile.restingHeartRate} bpm", color = AppText)
             }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (editMode) {
                    val newProfile = currentProfile.copy(
                        age = age.toIntOrNull() ?: currentProfile.age,
                        weightKg = weight.toDoubleOrNull() ?: currentProfile.weightKg,
                        restingHeartRate = hr.toIntOrNull() ?: currentProfile.restingHeartRate,
                        currentWeeklyDistanceKm = vol.toDoubleOrNull() ?: currentProfile.currentWeeklyDistanceKm
                    )
                    onUpdateProfile(newProfile)
                    editMode = false
                } else {
                    editMode = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(editMode) AirAccent else AirPrimary)
        ) {
            Text(if(editMode) "SAUVEGARDER & RECALCULER" else "MODIFIER MON PROFIL")
        }



        Spacer(modifier = Modifier.height(24.dp))
        
        // PR Removed as per user request (Redundant with Recap)
        // PersonalRecordsCard(prs)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Applications & Montres", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
        Text("Connectez vos comptes pour la synchronisation", color = AirTextLight, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Health Connect Permission Launcher
        var hcAvailable by remember { mutableStateOf<Boolean?>(null) }
        var hcHasPermissions by remember { mutableStateOf(false) }
        
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = com.orbital.run.api.HealthConnectManager.getPermissionsContract()
        ) { _ ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                hcHasPermissions = com.orbital.run.api.HealthConnectManager.hasAllPermissions(context)
                if (hcHasPermissions) {
                    Persistence.saveHealthConnectEnabled(context, true) // Added persistence
                    // Also update main app state if possible, though it might be reactive via Persistence check on resume
                    android.widget.Toast.makeText(context, "✅ Health Connect autorisé !", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            hcAvailable = com.orbital.run.api.HealthConnectManager.isAvailable(context)
            if (hcAvailable == true) {
                hcHasPermissions = com.orbital.run.api.HealthConnectManager.hasAllPermissions(context)
            }
        }
        
        // Garmin Connect API Integration
        // GarminInstallCard() // Removed per user request
        
        Spacer(modifier = Modifier.height(16.dp))
        
        key(refreshTrigger) {
        val apps = listOf(
            "Garmin Connect",
            "Strava",
            "Polar Flow",
            "Suunto App",
            "Health Connect"
        )
        
        apps.forEach { appName ->
            // Special handling for Health Connect
            if (appName == "Health Connect") {
                if (hcAvailable == false) {
                    return@forEach // Skip if not available
                }
                
                AppItem(
                    name = "Health Connect",
                    isLinked = hcHasPermissions && com.orbital.run.logic.Persistence.loadHealthConnectEnabled(context),
                    isConfigured = com.orbital.run.logic.Persistence.loadHealthConnectEnabled(context),
                    subtitle = "Sync: Garmin, autres wearables",
                    onConnect = {
                        try {
                            permissionLauncher.launch(com.orbital.run.api.HealthConnectManager.getPermissionsToRequest())
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Erreur: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    onDisconnect = {
                        android.widget.Toast.makeText(context, "Gérez les permissions dans les paramètres Android", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Standard apps
                val isLinked = when(appName) {
                    "Garmin Connect" -> com.orbital.run.api.GarminIntent.isGarminConnectInstalled(context)
                    "Strava" -> connectedApps["Strava"] == true
                    "Polar Flow" -> connectedApps["Polar Flow"] == true
                    "Suunto App" -> connectedApps["Suunto App"] == true
                    else -> connectedApps[appName] == true
                }
                
                val isConfigured = when(appName) {
                    "Garmin Connect" -> true
                    "Strava" -> com.orbital.run.api.StravaAPI.isConfigured()
                    "Polar Flow" -> com.orbital.run.api.PolarAPI.isConfigured()
                    "Suunto App" -> com.orbital.run.api.SuuntoAPI.isConfigured()
                    else -> false
                }

                AppItem(
                    name = appName,
                    isLinked = isLinked,
                    isConfigured = isConfigured,
                    onConnect = { onConnect(appName) },
                    onDisconnect = { 
                        when(appName) {
                            "Strava" -> com.orbital.run.api.StravaAPI.disconnect(context)
                            "Polar Flow" -> com.orbital.run.api.PolarAPI.disconnect()
                            "Suunto App" -> com.orbital.run.api.SuuntoAPI.disconnect()
                        }
                        onDisconnect(appName)
                        refreshTrigger++
                    }
                )
            }
        }
        } // end key


        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                onResetData()
                android.widget.Toast.makeText(context, "Données réinitialisées 🧹", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = ZoneRed),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("RÉINITIALISER TOUTES LES DONNÉES", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text("Attention: Supprime tout l'historique et les activités.", color = ZoneRed, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Manual Update Check
        OutlinedButton(
            onClick = onCheckForUpdate,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, AirPrimary)
        ) {
            Icon(Icons.Default.SystemUpdate, null, tint = AirPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rechercher une mise à jour", color = AirPrimary)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PersonalRecordsCard(prs: com.orbital.run.logic.Persistence.PersonalRecords) {
    AirCard("Records Personnels (Estimés)") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PRItem("1 km", prs.best1k?.duration)
            PRItem("5 km", prs.best5k?.duration)
            PRItem("10 km", prs.best10k?.duration)
            PRItem("Semi", prs.bestHalf?.duration)
            PRItem("Marathon", prs.bestMarathon?.duration)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Plus Long", fontSize = 12.sp, color = AirTextLight)
                Text("${String.format("%.1f", prs.longestRunKm?.distance ?: 0.0)} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppText)
            }
        }
    }
}

@Composable
fun PRItem(label: String, seconds: Long?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = AirTextLight)
        Text(
            if (seconds != null) com.orbital.run.logic.formatDuration(seconds.toInt() / 60) else "--",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppText
        )
    }
}

/* Removed per user request
@Composable
fun GarminInstallCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = AirSecondary.copy(alpha=0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Watch, null, tint = AirSecondary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Cadrans Garmin", fontWeight = FontWeight.Bold, color = AirPrimary)
                Text("Installez nos DataFields exclusifs", fontSize = 12.sp, color = AppText)
            }
        }
    }
}
*/

@Composable
fun AppItem(
    name: String, 
    isLinked: Boolean, 
    isConfigured: Boolean, 
    onConnect: () -> Unit, 
    onDisconnect: () -> Unit,
    subtitle: String? = null
) {
    val isConnected = isLinked || isConfigured
    
    // App-specific colors and icons
    val appColor = when {
        name.contains("Garmin") -> Color(0xFF007CC3)  // Garmin blue
        name.contains("Strava") -> Color(0xFFFC4C02)  // Strava orange
        name.contains("Polar") -> Color(0xFFE2001A)   // Polar red
        name.contains("Suunto") -> Color(0xFF00D7D7)  // Suunto cyan
        name.contains("Health") -> Color(0xFF34C759)  // Health green
        else -> AirPrimary
    }
    
    val appIcon = when {
        name.contains("Garmin") -> Icons.Default.Watch
        name.contains("Strava") -> Icons.Default.DirectionsRun
        name.contains("Polar") -> Icons.Default.FavoriteBorder
        name.contains("Suunto") -> Icons.Default.Explore
        name.contains("Health") -> Icons.Default.Phone
        else -> Icons.Default.Apps
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AirWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Name + Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App Icon
                Surface(
                    shape = CircleShape,
                    color = appColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        appIcon, 
                        contentDescription = null, 
                        tint = appColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        name, 
                        fontWeight = FontWeight.Bold, 
                        color = AppText,
                        fontSize = 15.sp
                    )
                    
                    // Status Badge
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Check specifically for Garmin Auth Error
                        val isGarminError = name.contains("Garmin") && 
                                          com.orbital.run.api.GarminAPI.status == com.orbital.run.api.GarminAPI.ConnectionStatus.AUTH_ERROR
                        
                        val statusColor = if (isGarminError) ZoneRed else if (isConnected) ZoneGreen else AirTextLight
                        val statusText = if (isGarminError) "Erreur Auth" else if (isConnected) "Connecté" else "Non connecté" // Fix: "Non connecté" was implicitly handled by "Déconnecté" in previous code, but "Non connecté" is better here. preserving "Déconnecté" if that's what was there. valid: "Déconnecté"
                        
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isGarminError) ZoneRed.copy(alpha = 0.15f) else if (isConnected) ZoneGreen.copy(alpha = 0.15f) else AirSurface
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                if (isConnected || isGarminError) {
                                    Icon(
                                        if (isGarminError) Icons.Default.Warning else Icons.Default.CheckCircle, 
                                        contentDescription = null,
                                        tint = if (isGarminError) ZoneRed else ZoneGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isGarminError) ZoneRed else if (isConnected) ZoneGreen else AirTextLight
                                )
                            }
                        }
                    }
                    
                    // Optional subtitle
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            fontSize = 10.sp,
                            color = AirTextLight,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Right: Action Button
            if (isConnected && (!name.contains("Garmin") || com.orbital.run.api.GarminAPI.status != com.orbital.run.api.GarminAPI.ConnectionStatus.AUTH_ERROR)) {
                OutlinedButton(
                    onClick = onDisconnect,
                    border = BorderStroke(1.dp, ZoneRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ZoneRed
                    )
                ) {
                    Text("Déconnecter", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = if(name.contains("Garmin") && com.orbital.run.api.GarminAPI.status == com.orbital.run.api.GarminAPI.ConnectionStatus.AUTH_ERROR) ZoneRed else appColor)
                ) {
                    Text(if(name.contains("Garmin") && com.orbital.run.api.GarminAPI.status == com.orbital.run.api.GarminAPI.ConnectionStatus.AUTH_ERROR) "RE-CONNECTER" else "Connecter", fontSize = 12.sp)
                }
            }
        }
    }
}



// Dialogs & Helpers
@Composable
fun AppConnectDialog(appName: String, onDismiss: () -> Unit, onConnect: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AirWhite,
        title = { Text("Connexion $appName", color = AppText) },
        text = { Text("Autoriser DrawRun à envoyer des entraînements vers votre compte $appName ?", color = AirTextSecondary) },
        confirmButton = { Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)) { Text("AUTORISER") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULER", color = AirTextLight) } }
    )
}

@Composable
fun SyncDialog(appName: String, onDismiss: () -> Unit) {
     AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AirWhite,
        icon = { Icon(Icons.Default.Check, null, tint = ZoneGreen, modifier = Modifier.size(48.dp)) },
        title = { Text("Simulation d'Envoi", color = AirPrimary) },
        text = { Text("Mode Démo : L'entraînement a été envoyé VIRTUELLEMENT vers $appName.\n\nDans une vraie application, cela utiliserait l'API officielle Garmin. Ici, aucune donnée n'est transférée.", color = AirTextSecondary) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)) { Text("COMPRIS") } }
    )
}

// --- Light & Airy Components ---

@Composable
fun AirCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AirWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if(title != null) {
                Text(title, color = AirTextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirInput(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AirPrimary,
            unfocusedBorderColor = AirSurface,
            focusedContainerColor = AirSurface,
            unfocusedContainerColor = AirSurface,
            focusedLabelColor = AirPrimary,
            focusedTextColor = AppText,
            unfocusedTextColor = AppText
        ),
        modifier = modifier, keyboardOptions = keyboardOptions, shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun CircularGauge(title: String, value: String, unit: String, percent: Float, color: Color, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp).clickable(onClick = onClick)) {
            Canvas(modifier = Modifier.size(80.dp)) {
                drawArc(color = AirSurface, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 20f, cap = StrokeCap.Round))
                drawArc(color = color, startAngle = -90f, sweepAngle = 360f * percent.coerceIn(0f, 1f), useCenter = false, style = Stroke(width = 20f, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(unit, color = AirTextLight, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = AirTextLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RaceTimeCard(title: String, time: String) {
    Card(colors = CardDefaults.cardColors(containerColor = AirPrimary), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(130.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = AirWhite.copy(alpha=0.8f), fontSize = 12.sp)
            Text(time, color = AirWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ZoneBar(label: String, range: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(36.dp).background(AirSurface, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp)) {
        Text(label, color = AirTextLight, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.weight(1f).height(6.dp).background(Color.White, CircleShape)) { Box(modifier = Modifier.fillMaxWidth(percent.coerceIn(0f, 1f)).height(6.dp).background(color, CircleShape)) }
        Spacer(modifier = Modifier.width(12.dp))
        Text(range, color = AppText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeekCard(week: TrainingWeek, trainingPlan: TrainingPlanResult, connectedApps: Map<String, Boolean> = emptyMap(), onSyncWorkout: (Workout) -> Unit = {}){
    val totalWeeks = trainingPlan.weeklyPlan.size
    val weeksUntilRace = totalWeeks - week.weekNumber + 1
    
    // Calculate week dates from race date
    val raceDateMillis = trainingPlan.userProfile.raceDateMillis
    val weekStartMillis = raceDateMillis - (weeksUntilRace * 7 * 24 * 60 * 60 * 1000L)
    val weekEndMillis = weekStartMillis + (6 * 24 * 60 * 60 * 1000L)
    
    // Format dates
    val calendar = java.util.Calendar.getInstance(java.util.Locale.FRENCH)
    calendar.timeInMillis = weekStartMillis
    val startDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val startMonthShort = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.FRENCH)?.lowercase() ?: ""
    
    calendar.timeInMillis = weekEndMillis
    val endDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val endMonthShort = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.FRENCH)?.lowercase() ?: ""
    
    val weekRange = if (startMonthShort == endMonthShort) {
        "$startDay-$endDay $startMonthShort"
    } else {
        "$startDay $startMonthShort - $endDay $endMonthShort"
    }
    
    // Phase colors
    val phaseColor = when {
        week.phase.contains("Préparation", ignoreCase = true) -> Color(0xFF2196F3) // Blue
        week.phase.contains("Affûtage", ignoreCase = true) -> Color(0xFFFF9800) // Orange
        week.phase.contains("Construction", ignoreCase = true) -> Color(0xFF4CAF50) // Green
        week.phase.contains("Spécifique", ignoreCase = true) -> Color(0xFF9C27B0) // Purple
        else -> AirPrimary
    }
    
    // Compact card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(weekRange, color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Semaine ${week.weekNumber}/$totalWeeks · ${week.totalDistance.toInt()} km", 
                        color = AirTextSecondary, fontSize = 11.sp)
                }
                Surface(
                    color = phaseColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        week.phase, 
                        color = phaseColor, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            week.workouts.forEach { w ->
                ExpandableWorkoutItem(w, trainingPlan, connectedApps, { onSyncWorkout(w) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ExpandableWorkoutItem(
    workout: Workout, 
    trainingPlan: TrainingPlanResult? = null, 
    @Suppress("UNUSED_PARAMETER") connectedApps: Map<String, Boolean> = emptyMap(),
    onSync: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Column(modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, AirSurface, RoundedCornerShape(12.dp))
        .clip(RoundedCornerShape(12.dp))
        .clickable {
            // Regular click expands, but we'll add a button for detail dialog
            expanded = !expanded
        }
        .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if(workout.type == WorkoutType.SWIMMING) AirSecondary else AirPrimary, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, color = AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${String.format("%.1f", workout.totalDistanceKm)} km • ${formatDuration(workout.totalDurationMin)}", color = AirTextLight, fontSize = 12.sp)
            }
            
            // Share/Sync Button - Always visible
            IconButton(onClick = { 
                onSync()
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Partager", tint = AirPrimary)
            }
            
            // Delete Button
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AirTextLight)
                }
            }
            
            Icon(Icons.Default.ArrowDropDown, null, tint = AirTextLight, modifier = Modifier.rotate(rotation))
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AirSurface))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distance", fontSize = 10.sp, color = AirTextLight)
                    Text("${String.format("%.1f", workout.totalDistanceKm)} km", 
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AirPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Temps", fontSize = 10.sp, color = AirTextLight)
                    Text(formatDuration(workout.totalDurationMin), 
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AirPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val calories = (workout.totalDistanceKm * 60).toInt()
                    Text("Calories", fontSize = 10.sp, color = AirTextLight)
                    Text("≈$calories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AirPrimary)
                }
            }
            
            // Target Pace highlight
            workout.steps.firstOrNull { it.targetPace != null }?.targetPace?.let { pace ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AirPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🎯 Allure Cible", fontSize = 12.sp, color = AirTextLight, fontWeight = FontWeight.Medium)
                    Text(pace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AirPrimary)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AirSurface))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Détail des étapes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(modifier = Modifier.height(8.dp))
            
            workout.steps.forEach { step ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         if(step.targetZone != null) {
                             Box(modifier = Modifier.width(4.dp).height(24.dp).background(getZoneColor(step.targetZone), CircleShape))
                         } else {
                             Box(modifier = Modifier.width(4.dp).height(24.dp).background(Color.LightGray, CircleShape))
                         }
                         Spacer(modifier = Modifier.width(12.dp))
                         Column {
                             Text(step.description, color = AppText, fontSize = 13.sp)
                             Text(step.durationOrDist, color = AirTextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                         }
                    }
                    
                    if (step.targetZone != null && trainingPlan != null) {
                        val zId = step.targetZone
                        val hr = trainingPlan.hrZones.find { it.id == zId }
                        val sp = trainingPlan.speedZones.find { it.id == zId }
                        val pw = trainingPlan.powerZones.find { it.id == zId }
                        
                        // Only show range if we have info
                        if (hr != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.padding(start = 16.dp)) {
                                Text(
                                    "FC: ${hr.min}-${hr.max} bpm" + 
                                    (if(sp != null) " | Allure: ${formatPace(sp.maxSpeedKmh)}-${formatPace(sp.minSpeedKmh)} /km" else "") +
                                    (if(pw != null) " | Puis: ${pw.minWatts}-${pw.maxWatts} W" else ""),
                                    color = AirTextLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getZoneColor(id: Int): Color {
    return when(id) {
        1 -> ZoneGrey // Z1 Recup
        2 -> ZoneBlue // Z2 Endurance
        3 -> ZoneGreen // Z3 Tempo
        4 -> ZoneOrange // Z4 Seuil
        5 -> ZoneRed // Z5 VMA
        else -> ZoneGrey
    }
}

// Helper for pace
fun formatPace(speedKmh: Double): String {
    if (speedKmh <= 0) return "-:--"
    val paceMin = 60.0 / speedKmh
    val min = paceMin.toInt()
    val sec = ((paceMin - min) * 60).toInt()
    return "%d:%02d".format(min, sec)
}

fun shareWorkout(context: android.content.Context, workout: Workout) {
    // PRIORITÉ 1: Garmin Connect via Intent (AUCUNE CLÉ API REQUISE)
    if (com.orbital.run.api.GarminIntent.isGarminConnectInstalled(context)) {
        android.widget.Toast.makeText(context, "📤 Ouverture Garmin Connect...", android.widget.Toast.LENGTH_SHORT).show()
        com.orbital.run.api.GarminIntent.sendWorkout(context, workout) { success, msg ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (!success) {
                    android.widget.Toast.makeText(context, "❌ $msg", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        return
    }
    
    // PRIORITÉ 2: Services en mode démo (Strava, Polar, Suunto)
    when {
        com.orbital.run.api.StravaAPI.isAuthenticated() -> {
            android.widget.Toast.makeText(context, "📤 Strava...", android.widget.Toast.LENGTH_SHORT).show()
            com.orbital.run.api.StravaAPI.uploadWorkout(workout) { s, m -> 
                android.os.Handler(android.os.Looper.getMainLooper()).post { 
                    android.widget.Toast.makeText(context, if(s) "✅ $m" else "❌ $m", android.widget.Toast.LENGTH_LONG).show() 
                } 
            }
            return
        }
        com.orbital.run.api.PolarAPI.isAuthenticated() -> {
            android.widget.Toast.makeText(context, "📤 Polar...", android.widget.Toast.LENGTH_SHORT).show()
            com.orbital.run.api.PolarAPI.uploadWorkout(workout) { s, m -> 
                android.os.Handler(android.os.Looper.getMainLooper()).post { 
                    android.widget.Toast.makeText(context, if(s) "✅ $m" else "❌ $m", android.widget.Toast.LENGTH_LONG).show() 
                } 
            }
            return
        }
        com.orbital.run.api.SuuntoAPI.isAuthenticated() -> {
            android.widget.Toast.makeText(context, "📤 Suunto...", android.widget.Toast.LENGTH_SHORT).show()
            com.orbital.run.api.SuuntoAPI.uploadWorkout(workout) { s, m -> 
                android.os.Handler(android.os.Looper.getMainLooper()).post { 
                    android.widget.Toast.makeText(context, if(s) "✅ $m" else "❌ $m", android.widget.Toast.LENGTH_LONG).show() 
                } 
            }
            return
        }
    }
    
    // PRIORITÉ 3: Aucun service - proposer installation ou export JSON
    android.widget.Toast.makeText(context, "⚠️ Installez Garmin Connect pour l'envoi automatique\n\nExport JSON en cours...", android.widget.Toast.LENGTH_LONG).show()
    exportAsJson(context, workout)
}

// Fonction de fallback pour export JSON/Texte
private fun exportAsJson(context: android.content.Context, workout: Workout) {
    // Créer un résumé texte enrichi
    val textSummary = buildString {
        appendLine("🏃 ${workout.title}")
        appendLine("=".repeat(50))
        appendLine()
        appendLine("📊 Vue d'ensemble:")
        appendLine("  • Distance: ${String.format("%.1f", workout.totalDistanceKm)} km")
        appendLine("  • Durée: ${formatDuration(workout.totalDurationMin)}")
        appendLine("  • Type: ${if(workout.type == WorkoutType.SWIMMING) "Natation" else "Course"}")
        appendLine()
        appendLine("📋 Détail des séries:")
        workout.steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. ${step.description}")
            appendLine("   ⏱ ${step.durationOrDist}")
            step.targetZone?.let { 
                appendLine("   🎯 Zone $it") 
            }
        }
        appendLine()
        appendLine("=".repeat(50))
        appendLine("✨ Généré par DrawRun")
    }
    
    // Créer un JSON pour import potentiel
    val jsonData = buildString {
        appendLine("{")
        appendLine("  \"workout\": {")
        appendLine("    \"title\": \"${workout.title}\",")
        appendLine("    \"type\": \"${if(workout.type == WorkoutType.SWIMMING) "swimming" else "running"}\",")
        appendLine("    \"totalDistance\": ${workout.totalDistanceKm},")
        appendLine("    \"totalDuration\": ${workout.totalDurationMin},")
        appendLine("    \"steps\": [")
        workout.steps.forEachIndexed { index, step ->
            appendLine("      {")
            appendLine("        \"order\": ${index + 1},")
            appendLine("        \"description\": \"${step.description}\",")
            appendLine("        \"duration\": \"${step.durationOrDist}\",")
            appendLine("        \"zone\": ${step.targetZone ?: "null"}")
            appendLine("      }${if(index < workout.steps.size - 1) "," else ""}")
        }
        appendLine("    ]")
        appendLine("  }")
        appendLine("}")
    }
    
    val fileName = "${workout.title.replace(" ", "_")}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
    
    try {
        // Sauvegarder le fichier JSON
        val file = java.io.File(context.cacheDir, fileName)
        file.writeText(jsonData)
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Option 1: Partager le JSON
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, textSummary)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val shareIntent = Intent.createChooser(sendIntent, "Partager l'entraînement")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Fallback: partager juste le texte
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textSummary)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Partager l'entraînement")
        context.startActivity(shareIntent)
    }
}



data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    var color: Color,
    var size: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float
)

@Composable
fun InternalConfetti(modifier: Modifier = Modifier) {
    val particles = remember {
        List(400) { id ->
            Particle(
                id = id,
                x = 500f, 
                y = -100f, 
                color = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan).random(),
                size = Random.nextFloat() * 12f + 4f,
                velocityX = (Random.nextFloat() - 0.5f) * 25f,
                velocityY = Random.nextFloat() * 15f + 5f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15f
            )
        }
    }

    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                time = (frameTime - startTime) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEachIndexed { index, p ->
            
            p.x += p.velocityX
            p.y += p.velocityY
            p.velocityY += 0.2f // Gravity
            p.rotation += p.rotationSpeed
            
            p.x += sin(time * 5f + index) * 2f

            if (p.y > height + 50) {
                p.y = -50f
                p.x = Random.nextFloat() * width
                p.velocityY = Random.nextFloat() * 10f + 5f
            }

            withTransform({
                translate(p.x, p.y)
                rotate(p.rotation)
            }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.size / 2, -p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f) 
                )
            }
        }
    }
}
