package com.orbital.run.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.orbital.run.logic.UpdateInfo
import com.orbital.run.ui.theme.AirPrimary
import com.orbital.run.ui.theme.AirSurface
import com.orbital.run.ui.theme.AirTextPrimary
import com.orbital.run.ui.theme.AirTextSecondary

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = AirPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Mise à jour disponible !",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AirTextPrimary
                )
                Text(
                    text = "Version ${updateInfo.latestVersionName}",
                    fontSize = 14.sp,
                    color = AirTextSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Release Notes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (updateInfo.releaseNotes.features.isNotEmpty()) {
                        SectionHeader("✨ Nouveautés")
                        updateInfo.releaseNotes.features.forEach { feature ->
                            BulletPoint(feature)
                        }
                    }
                    
                    if (updateInfo.releaseNotes.fixes.isNotEmpty()) {
                        if (updateInfo.releaseNotes.features.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("🐛 Corrections")
                        updateInfo.releaseNotes.fixes.forEach { fix ->
                            BulletPoint(fix)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Buttons
                Button(
                    onClick = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AirPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TÉLÉCHARGER", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Plus tard", color = AirTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = AirTextPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        crossAxisAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = AirTextSecondary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = AirTextPrimary,
            lineHeight = 20.sp
        )
    }
}
