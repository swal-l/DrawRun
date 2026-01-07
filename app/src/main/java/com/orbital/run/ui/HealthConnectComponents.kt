package com.orbital.run.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

// Simplified Health Connect Onboarding Component
@Composable
fun SimpleHealthConnectOnboarding(
    onConnectHealthConnect: () -> Unit,
    onFinish: () -> Unit,
    isHealthConnectConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AirPrimary
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Title
        Text(
            "Connectez Health Connect",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AppText,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Description
        Text(
            "DrawRun utilise Health Connect pour synchroniser vos activités depuis Garmin, Strava, et autres applications de sport.",
            fontSize = 16.sp,
            color = AirTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Health Connect Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isHealthConnectConnected) AirPrimary.copy(alpha = 0.1f) else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                2.dp,
                if (isHealthConnectConnected) AirPrimary else AirSurface
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = if (isHealthConnectConnected) AirPrimary else AirTextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Health Connect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppText
                        )
                        Text(
                            if (isHealthConnectConnected) "✓ Connecté" else "Non connecté",
                            fontSize = 14.sp,
                            color = if (isHealthConnectConnected) AirPrimary else AirTextSecondary
                        )
                    }
                }
                
                if (!isHealthConnectConnected) {
                    Button(
                        onClick = onConnectHealthConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
                    ) {
                        Text("Connecter")
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Finish Button
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
            enabled = isHealthConnectConnected
        ) {
            Text(
                if (isHealthConnectConnected) "Commencer" else "Connectez Health Connect pour continuer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (!isHealthConnectConnected) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Vous pourrez toujours connecter Health Connect plus tard dans les paramètres",
                fontSize = 12.sp,
                color = AirTextLight,
                textAlign = TextAlign.Center
            )
        }
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
