package com.orbital.run.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbital.run.ui.theme.*

@Composable
fun SyncOnboardingScreen(
    onConnectStrava: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    onConnectGarmin: () -> Unit,
    onConnectPolar: () -> Unit,
    onConnectSuunto: () -> Unit,
    onFinish: () -> Unit,
    isStravaConnected: Boolean,
    isHealthConnectConnected: Boolean,
    isGarminConnected: Boolean,
    isPolarConnected: Boolean,
    isSuuntoConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AirSurface)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.CloudSync,
                null,
                modifier = Modifier.size(64.dp),
                tint = AirPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Synchronisez vos Données",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AirTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connectez vos services habituels pour importer votre historique et vos tracés GPS.",
                fontSize = 16.sp,
                color = AirTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Garmin Card
        SyncServiceCard(
            title = "Garmin Connect",
            description = "Synchronisation directe depuis votre montre Garmin.",
            icon = Icons.Default.Watch,
            accentColor = Color(0xFF007CC3), // Garmin Blue
            isConnected = isGarminConnected,
            onClick = onConnectGarmin
        )

        // Strava Card
        SyncServiceCard(
            title = "Strava",
            description = "Importez vos courses avec GPS, allure et cardio.",
            icon = Icons.Default.Sync,
            accentColor = Color(0xFFFC4C02), // Strava Orange
            isConnected = isStravaConnected,
            onClick = onConnectStrava
        )

        // Health Connect Card
        SyncServiceCard(
            title = "Santé Connect",
            description = "Récupérez vos pas, sommeil et données de santé unifiées.",
            icon = Icons.Rounded.HealthAndSafety,
            accentColor = Color(0xFF4285F4), // Google Blue-ish
            isConnected = isHealthConnectConnected,
            onClick = onConnectHealthConnect
        )

        // Polar Card
        SyncServiceCard(
            title = "Polar Flow",
            description = "Connectez votre compte Polar Flow.",
            icon = Icons.Default.FavoriteBorder,
            accentColor = Color(0xFFE2001A), // Polar Red
            isConnected = isPolarConnected,
            onClick = onConnectPolar
        )

        // Suunto Card
        SyncServiceCard(
            title = "Suunto App",
            description = "Connectez votre compte Suunto.",
            icon = Icons.Default.Explore,
            accentColor = Color(0xFF00D7D7), // Suunto Cyan
            isConnected = isSuuntoConnected,
            onClick = onConnectSuunto
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
        ) {
            Text(
                if (isStravaConnected || isHealthConnectConnected) "TERMINER L'INSTALLATION" else "PLUS TARD / CONTINUER",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SyncServiceCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isConnected) BorderStroke(2.dp, accentColor.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AirTextPrimary)
                Text(description, fontSize = 13.sp, color = AirTextSecondary, lineHeight = 18.sp)
            }
            
            Spacer(Modifier.width(8.dp))
            
            if (isConnected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Connecter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
