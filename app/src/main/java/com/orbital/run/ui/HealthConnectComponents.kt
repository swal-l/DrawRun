package com.orbital.run.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.run.api.SyncManager
import com.orbital.run.ui.theme.*

// Advanced Sync Onboarding Component
@Composable
fun SyncOnboardingScreen(
    context: android.content.Context,
    connectedApps: MutableMap<String, Boolean>,
    onConnectApp: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(20.dp))
        
        // Icon
        Icon(
            Icons.Rounded.Sync, // Was Favorite
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AirPrimary
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Title
        Text(
            "Connectez vos Services",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AirTextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Description
        Text(
            "Pour une analyse complète, centralisez vos données via Health Connect ou connectez vos comptes directement.",
            fontSize = 15.sp,
            color = AirTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(Modifier.height(32.dp))
        
        // 1. PRIMARY OPTION: HEALTH CONNECT
        val isHcConnected = connectedApps["Health Connect"] == true
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onConnectApp("Health Connect") },
            colors = CardDefaults.cardColors(containerColor = if(isHcConnected) ZoneGreen.copy(alpha=0.1f) else AirSurface),
            border = if(isHcConnected) BorderStroke(1.dp, ZoneGreen) else null,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = if(isHcConnected) ZoneGreen else AirPrimary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Health Connect (Recommandé)", fontWeight = FontWeight.Bold, color = AirTextPrimary, fontSize = 16.sp)
                    Text("Synchronise Garmin, Google Fit, Samsung...", fontSize = 12.sp, color = AirTextSecondary)
                }
                if(isHcConnected) Icon(Icons.Filled.CheckCircle, null, tint = ZoneGreen)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // 2. DIRECT CONNECTIONS GRID
        Text(
            "Autres connexions directes :", 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Bold, 
            color = AirTextLight, 
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(12.dp))
        
        val apps = listOf("Garmin", "Strava", "Fitbit", "Polar")
        
        // Grid Layout (2 columns)
        val chunked = apps.chunked(2)
        chunked.forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowApps.forEach { appName ->
                    val isConnected = connectedApps[appName] == true
                    val color = when(appName) {
                       "Garmin" -> Color(0xFF007CC3)
                       "Strava" -> Color(0xFFFC4C02)
                       else -> AirTextSecondary
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f).clickable { onConnectApp(appName) },
                        colors = CardDefaults.cardColors(containerColor = AirWhite),
                        border = if(isConnected) BorderStroke(2.dp, color.copy(alpha=0.6f)) else BorderStroke(1.dp, AirSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(appName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if(isConnected) color else AirTextPrimary)
                            }
                            if(isConnected) {
                                Text("Connecté", fontSize = 10.sp, color = color)
                            }
                        }
                    }
                }
                if(rowApps.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Finish Button
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "TERMINER & ANALYSER",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// Sync Result Dialog with Popup Feedback
@Composable
fun SyncResultDialog(
    result: SyncManager.SyncResult?,
    onDismiss: () -> Unit
) {
    if (result != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (result) {
                            is SyncManager.SyncResult.Success -> Icons.Rounded.CheckCircle
                            is SyncManager.SyncResult.NoNewData -> Icons.Rounded.Info
                            is SyncManager.SyncResult.Error -> Icons.Rounded.Error
                        },
                        contentDescription = null,
                        tint = when (result) {
                            is SyncManager.SyncResult.Success -> Color(0xFF4CAF50)
                            is SyncManager.SyncResult.NoNewData -> Color(0xFFFF9800)
                            is SyncManager.SyncResult.Error -> Color(0xFFF44336)
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        when (result) {
                            is SyncManager.SyncResult.Success -> "Synchronisation réussie"
                            is SyncManager.SyncResult.NoNewData -> "Aucune nouvelle donnée"
                            is SyncManager.SyncResult.Error -> "Erreur de synchronisation"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    when (result) {
                        is SyncManager.SyncResult.Success -> "${result.count} nouvelle(s) activité(s) synchronisée(s) depuis Health Connect"
                        is SyncManager.SyncResult.NoNewData -> "Toutes vos activités sont déjà à jour"
                        is SyncManager.SyncResult.Error -> "Erreur: ${result.message}"
                    }
                )
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            },
            containerColor = Color.White
        )
    }
}
