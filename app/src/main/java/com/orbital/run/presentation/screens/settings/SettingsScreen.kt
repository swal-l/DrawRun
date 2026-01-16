package com.orbital.run.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbital.run.presentation.components.DrawRunButton
import com.orbital.run.presentation.components.DrawRunTopBar

/**
 * Settings screen with iOS/Linear-inspired clean design.
 *
 * Sections:
 * - Apparence (Dark Mode, Units)
 * - Synchronisation (Strava connection, sync status)
 * - Données (Export, Reset)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val useMetricUnits by viewModel.useMetricUnits.collectAsStateWithLifecycle()
    val stravaConnected by viewModel.stravaConnected.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            DrawRunTopBar(title = "Paramètres")
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ========================
            // APPARENCE
            // ========================
            
            item {
                SettingsSection(title = "Apparence") {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Mode sombre",
                        subtitle = "Thème de l'application",
                        trailing = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() }
                            )
                        }
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.Speed,
                        title = "Unités",
                        subtitle = if (useMetricUnits) "Kilomètres" else "Miles",
                        trailing = {
                            Switch(
                                checked = useMetricUnits,
                                onCheckedChange = { viewModel.toggleUnits() }
                            )
                        }
                    )
                }
            }
            
            // ========================
            // SYNCHRONISATION
            // ========================
            
            item {
                SettingsSection(title = "Synchronisation") {
                    SettingsItem(
                        icon = Icons.Default.CloudSync,
                        title = "Strava",
                        subtitle = if (stravaConnected) "Connecté" else "Non connecté",
                        trailing = {
                            if (stravaConnected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { viewModel.openStravaSettings() }
                    )
                    
                    if (stravaConnected) {
                        Divider()
                        
                        SettingsItem(
                            icon = Icons.Default.Sync,
                            title = "Dernière synchronisation",
                            subtitle = "Il y a 2 heures",
                            trailing = {
                                DrawRunButton(
                                    text = "Synchroniser",
                                    onClick = { viewModel.syncNow() },
                                    variant = com.orbital.run.presentation.components.ButtonVariant.Text
                                )
                            }
                        )
                    }
                }
            }
            
            // ========================
            // DONNÉES
            // ========================
            
            item {
                SettingsSection(title = "Données") {
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Exporter en CSV",
                        subtitle = "Télécharger vos données",
                        onClick = { viewModel.exportData() }
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Réinitialiser l'application",
                        subtitle = "Supprimer toutes les données",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.resetApp() }
                    )
                }
            }
            
            // ========================
            // À PROPOS
            // ========================
            
            item {
                SettingsSection(title = "À propos") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "3.42.0"
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Conditions d'utilisation",
                        onClick = { /* Open terms */ }
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Politique de confidentialité",
                        onClick = { /* Open privacy */ }
                    )
                }
            }
        }
    }
}

// ========================
// COMPONENTS
// ========================

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
