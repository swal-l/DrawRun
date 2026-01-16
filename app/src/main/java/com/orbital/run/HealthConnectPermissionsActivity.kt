package com.orbital.run

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity required by Health Connect to display permission rationale.
 * This is shown when users click "Learn more" in Health Connect permissions screen.
 */
@AndroidEntryPoint
class HealthConnectPermissionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PermissionsRationaleScreen(
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsRationaleScreen(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Pourquoi DrawRun a besoin de ces permissions") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFF1E88E5),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Politique de confidentialité - Health Connect",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "DrawRun utilise Health Connect pour synchroniser vos données d'activité sportive depuis vos appareils connectés (Garmin, etc.).",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            PermissionSection(
                title = "Fréquence Cardiaque",
                description = "Permet d'analyser votre effort cardiovasculaire, calculer vos zones d'entraînement et détecter la dérive cardiaque."
            )
            
            PermissionSection(
                title = "Pas & Distance",
                description = "Synchronise vos courses et marches pour suivre votre volume d'entraînement hebdomadaire."
            )
            
            PermissionSection(
                title = "Calories",
                description = "Calcule votre dépense énergétique pour optimiser votre récupération et nutrition."
            )
            
            PermissionSection(
                title = "Sessions d'Exercice",
                description = "Importe vos activités complètes (course, natation) avec toutes leurs métriques."
            )
            
            PermissionSection(
                title = "Vitesse & Puissance",
                description = "Analyse votre performance et efficacité mécanique (pour cyclisme et course)."
            )
            
            PermissionSection(
                title = "Sommeil",
                description = "Évalue votre récupération pour ajuster vos entraînements."
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            Text(
                "🔒 Vos données restent privées",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                "• Toutes les données sont stockées localement sur votre appareil\n" +
                "• Aucune donnée n'est envoyée à des serveurs externes\n" +
                "• Vous pouvez révoquer ces permissions à tout moment dans les paramètres Android\n" +
                "• DrawRun lit uniquement vos données, ne les modifie jamais",
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E88E5)
                )
            ) {
                Text("J'ai compris", color = Color.White)
            }
        }
    }
}

@Composable
fun PermissionSection(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            "• $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            description,
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        )
    }
}
