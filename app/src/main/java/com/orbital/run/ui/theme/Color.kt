package com.orbital.run.ui.theme

import androidx.compose.ui.graphics.Color

// ========================
// LIGHT MODE COLORS
// ========================

// Surfaces (Backgrounds)
val LightBackground = Color(0xFFFBFCFE)       // Off-white, légèrement bleuté
val LightSurface = Color(0xFFFFFFFF)          // Blanc pur pour les cartes
val LightSurfaceVariant = Color(0xFFF5F7FA)   // Gris très clair pour zones secondaires
val LightSurfaceContainer = Color(0xFFEFF1F5) // Container avec plus de contraste

// Text
val LightOnBackground = Color(0xFF0F1419)     // Presque noir
val LightOnSurface = Color(0xFF1A1F26)        // Texte principal sur cartes
val LightOnSurfaceVariant = Color(0xFF5A6169) // Texte secondaire (labels)
val LightOnSurfaceTertiary = Color(0xFF8B9199)// Texte tertiaire (hints)

// Brand & Accent
val LightPrimary = Color(0xFF0066FF)          // Bleu vif Apple-like
val LightOnPrimary = Color(0xFFFFFFFF)        // Texte sur boutons primaires
val LightAccent = Color(0xFF00D9A0)           // Teal vibrant
val LightOnAccent = Color(0xFF0F1419)         // Texte sur accent

// States
val LightError = Color(0xFFE63946)            // Rouge premium
val LightOnError = Color(0xFFFFFFFF)
val LightWarning = Color(0xFFFF9500)          // Orange Apple
val LightSuccess = Color(0xFF00D9A0)          // Même que accent

// Borders & Dividers
val LightOutline = Color(0xFFE4E7EC)          // Bordures subtiles
val LightOutlineVariant = Color(0xFFF0F2F5)   // Dividers ultra-légers

// ========================
// DARK MODE COLORS
// ========================

// Surfaces (Deep Grays - Linear inspired)
val DarkBackground = Color(0xFF0F1419)        // Gris très profond
val DarkSurface = Color(0xFF1A1F26)           // Gris foncé pour cartes
val DarkSurfaceVariant = Color(0xFF23282F)    // Variant légèrement plus clair
val DarkSurfaceContainer = Color(0xFF2D3339)  // Container élevé

// Text (High Contrast)
val DarkOnBackground = Color(0xFFE6E8EB)      // Presque blanc
val DarkOnSurface = Color(0xFFDDE0E4)         // Texte principal
val DarkOnSurfaceVariant = Color(0xFF9DA3AA)  // Texte secondaire
val DarkOnSurfaceTertiary = Color(0xFF6B7179) // Texte tertiaire

// Brand & Accent (Adjusted for dark)
val DarkPrimary = Color(0xFF4D9FFF)           // Bleu plus clair pour contraste
val DarkOnPrimary = Color(0xFF0F1419)         // Texte foncé sur bleu clair
val DarkAccent = Color(0xFF00E6B0)            // Teal plus lumineux
val DarkOnAccent = Color(0xFF0F1419)

// States (Saturated for dark)
val DarkError = Color(0xFFFF5A67)             // Rouge plus vif
val DarkOnError = Color(0xFF0F1419)
val DarkWarning = Color(0xFFFFAA33)           // Orange plus lumineux
val DarkSuccess = Color(0xFF00E6B0)

// Borders (Subtiles mais visibles)
val DarkOutline = Color(0xFF3A4048)           // Bordures visibles
val DarkOutlineVariant = Color(0xFF2D3339)    // Dividers subtils

// ========================
// ZONE COLORS (Training)
// ========================

// Light Mode Zones
val LightZone1 = Color(0xFF9E9E9E)            // Recovery - Gris
val LightZone2 = Color(0xFF4CAF50)            // Endurance - Vert
val LightZone3 = Color(0xFFFFC107)            // Tempo - Jaune/Or
val LightZone4 = Color(0xFFFF9800)            // Threshold - Orange
val LightZone5 = Color(0xFFF44336)            // VO2max - Rouge

// Dark Mode Zones (Adjusted)
val DarkZone1 = Color(0xFFB0B0B0)             // Gris plus clair
val DarkZone2 = Color(0xFF66BB6A)             // Vert ajusté
val DarkZone3 = Color(0xFFFFD54F)             // Jaune ajusté
val DarkZone4 = Color(0xFFFFB74D)             // Orange ajusté
val DarkZone5 = Color(0xFFEF5350)             // Rouge ajusté

// ========================
// LEGACY COLORS (for gradual migration)
// ========================

// ========================
// LEGACY COMPATIBILITY
// ========================
// Keep old color names for legacy UI files (to be migrated)

val AirSurface = LightBackground
val AirWhite = LightSurface
val AirTextPrimary = LightOnSurface
val AirTextSecondary = LightOnSurfaceVariant
val AirTextLight = LightOnSurfaceTertiary
val AirAccent = LightAccent
val AirBackground = LightBackground
val AppText = LightOnSurface

val AirPrimary = LightPrimary
val AirSecondary = Color(0xFF2979FF)
val ZoneGrey = LightZone1
val ZoneBlue = Color(0xFF2196F3)
val ZoneGreen = LightZone2
val ZoneOrange = LightZone4
val ZoneRed = LightZone5
