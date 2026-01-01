package com.orbital.run.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import com.orbital.run.ui.theme.*

/**
 * Centrally manages all metric educational content
 */
object MetricEducation {
    fun getInfo(metricName: String): Pair<String, String> {
        return when (metricName) {
            "TRIMP" -> "Training Impulse" to "C'est une mesure de la charge d'entraînement physiologique. Contrairement aux kilomètres, le TRIMP pondère l'intensité (fréquence cardiaque). Une séance intense compte beaucoup plus qu'une séance calme."
            "CTL" -> "Fitness (Chronique)" to "La Charge Chronique d'Entraînement représente votre niveau de forme à long terme (moyenne des 42 derniers jours). Elle montre votre capacité à supporter l'effort."
            "ATL" -> "Fatigue (Aiguë)" to "La Charge Aiguë d'Entraînement mesure le stress récent (7 derniers jours). Une ATL trop élevée par rapport à la CTL indique un risque de blessure."
            "TSB" -> "Forme (Balance)" to "C'est la différence entre Fitness et Fatigue (CTL - ATL). Vert (+5 à +25) indique un pic de forme."
            "ACWR" -> "Ratio de Charge" to "Le ratio entre votre charge récente (ATL) et votre charge habituelle (CTL). L'indice idéal se situe entre 0.8 et 1.3. Au-delà de 1.5, attention au surmenage !"
            "EF" -> "Efficiency Factor" to "Mesure le rapport entre votre allure et votre fréquence cardiaque. Si votre EF augmente, vous devenez plus économe."
            "SI" -> "Stroke Index" to "Indicateur d'efficience en natation (Allure x Distance par mouvement). Un SI élevé signifie une meilleure glisse."
            "RE" -> "Running Effectiveness" to "Rapport entre allure et puissance. Un score proche de 1.0 indique une excellente technique de course."
            "SWOLF" -> "SWOLF Score" to "Somme de votre temps et du nombre de coups de bras pour une longueur. Plus il est bas, plus vous êtes efficace dans l'eau."
            "Distance" -> "Distance Totale" to "La distance totale parcourue durant votre session, calculée via GPS ou capteur de foulée."
            "Durée" -> "Temps de Déplacement" to "Le temps total où vous étiez en mouvement durant l'activité."
            "Allure" -> "Allure Moyenne" to "Votre vitesse moyenne exprimée en minutes par kilomètre (ou par 100m). C'est l'indicateur de vitesse préféré des coureurs."
            "Calories" -> "Dépense Énergétique" to "Estimation des calories brûlées basée sur votre poids, l'intensité cardiaque et la durée."
            "kJ" -> "Travail Mécanique" to "L'énergie réelle produite par vos muscles (exprimée en kilojoules). Plus précis que les calories si vous utilisez un capteur de puissance."
            "FC" -> "Fréquence Cardiaque" to "Nombre de battements de votre cœur par minute. C'est l'indicateur principal du stress physiologique."
            "Altitude" -> "Dénivelé & Altitude" to "L'altitude moyenne et le dénivelé positif cumulé durant votre sortie."
            "Cadence" -> "Fréquence de Pas/Bras" to "Nombre de pas par minute (spm). Une cadence élevée (>170) réduit souvent l'impact au sol et le risque de blessure."
            "Watts" -> "Puissance" to "Mesure instantanée de l'effort produit. Contrairement à l'allure, la puissance est indépendante du vent ou de la pente."
            "QUADRANT" -> "Analyse par Quadrants" to "Méthode de visualisation de la relation entre la force (couple) et la vélocité (cadence). Permet d'identifier si votre puissance est produite en force ou en vitesse."
            else -> "Information" to "Détails sur la métrique $metricName à venir dans la prochaine mise à jour."
        }
    }
}

@Composable
fun MetricExplanationDialog(metric: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp)) {
                when(metric) {
                    "CTL", "Condition" -> {
                        MetricInfoContent(
                            title = "Condition Physique (CTL)",
                            icon = Icons.Rounded.TrendingUp,
                            color = Color(0xFF00B0FF),
                            definition = "La Charge Chronique d'Entraînement représente votre niveau de forme à long terme (moyenne pondérée sur 42 jours).",
                            formula = "CTL_j = CTL_j-1 + (TSS - CTL_j-1) / 42",
                            utility = "Plus elle est élevée, plus votre organisme est capable d'absorber des charges d'entraînement importantes."
                        )
                    }
                    "ATL", "Fatigue" -> {
                        MetricInfoContent(
                            title = "Fatigue (ATL)",
                            icon = Icons.Rounded.Bolt,
                            color = Color(0xFFFF5252),
                            definition = "La Charge Aiguë d'Entraînement mesure le stress récent (moyenne pondérée sur 7 jours).",
                            formula = "ATL_j = ATL_j-1 + (TSS - ATL_j-1) / 7",
                            utility = "Indique le niveau d'épuisement immédiat. Une montée trop rapide de l'ATL est un signal d'alerte."
                        )
                    }
                    "TSB", "Forme" -> {
                        MetricInfoContent(
                            title = "Niveau de Forme (TSB)",
                            icon = Icons.Rounded.EmojiEvents,
                            color = Color(0xFF00E676),
                            definition = "La Balance de Stress d'Entraînement est la différence entre votre Fitness (CTL) et votre Fatigue (ATL).",
                            formula = "TSB = CTL - ATL",
                            interpretation = "• > +5 : Frais / Pic de forme\n• -10 à +5 : Entraînement productif\n• < -25 : Risque de surentraînement"
                        )
                    }
                    "EF" -> {
                        MetricInfoContent(
                            title = "Efficiency Factor (EF)",
                            icon = Icons.Rounded.Bolt,
                            color = AirPrimary,
                            definition = "Rapport entre allure et fréquence cardiaque. Mesure votre économie aérobie.",
                            formula = "EF = (Allure min/km) / FC Moyenne",
                            interpretation = "Une augmentation de l'EF à fréquence cardiaque égale indique une meilleure condition physique."
                        )
                    }
                    "RE" -> {
                         MetricInfoContent(
                            title = "Running Effectiveness",
                            icon = Icons.Rounded.Speed,
                            color = AirAccent,
                            definition = "Capacité à convertir la puissance brute en allure de déplacement.",
                            formula = "RE = (Allure) / (Puissance W/kg)",
                            interpretation = "Un score proche de 1.0 indique une technique biomécanique efficace."
                        )
                    }
                    "FC", "BPM" -> {
                         MetricInfoContent(
                            title = "Fréquence Cardiaque (BPM)",
                            icon = Icons.Rounded.MonitorHeart,
                            color = Color(0xFFFF0000),
                            definition = "Nombre de battements cardiaques par minute.",
                            utility = "Indicateur principal du stress physiologique et de l'intensité de l'effort cardiovasculaire."
                        )
                    }
                    "WATT", "PUISSANCE" -> {
                         MetricInfoContent(
                            title = "Puissance (Watts)",
                            icon = Icons.Rounded.Bolt,
                            color = Color(0xFFD500F9),
                            definition = "Mesure instantanée de l'énergie mécanique produite par vos muscles.",
                            formula = "Puissance = Force x Allure",
                            utility = "Indépendant des conditions (vent, pente), réagit instantanément aux changements de rythme."
                        )
                    }
                    "RSS" -> {
                         MetricInfoContent(
                            title = "Running Stress Score (RSS)",
                            icon = Icons.Rounded.Assessment,
                            color = AirAccent,
                            definition = "Le RSS quantifie la charge physiologique globale d'une séance de course à pied en utilisant la puissance et la durée.",
                            formula = "RSS = 100 * (Temps / 1h) * (Pwr_Moy / FTP)^2",
                            utility = "Permet de comparer l'effort de différentes séances (fractionné vs endurance) sur une base physiologique commune.",
                            interpretation = "• < 50 : Séance de récupération\n• 50-100 : Effort modéré / Endurance\n• 100-200 : Séance intense (PMA, Seuil)\n• > 250 : Effort extrême ou compétition"
                        )
                    }
                    "RTSS" -> {
                         MetricInfoContent(
                            title = "Run Training Stress Score (rTSS)",
                            icon = Icons.Rounded.Assessment,
                            color = AirAccent,
                            definition = "Le rTSS estime la charge d'une séance de course basée sur l'allure (pace) lorsque la puissance n'est pas disponible.",
                            formula = "rTSS = 100 * (Temps / 1h) * (NGP / NGP_Seuil)^2",
                            utility = "Utilise l'allure ajustée à la pente (NGP) pour estimer le stress métabolique subi.",
                            interpretation = "Équivalent aux zones du RSS. Moins précis car ne tient pas compte du vent ou de la technique, seulement de la vitesse et du dénivelé."
                        )
                    }
                    "CADENCE" -> {
                        MetricInfoContent(
                            title = "Cadence (SPM)",
                            icon = Icons.Rounded.DirectionsRun,
                            color = AirSecondary,
                            definition = "Nombre de pas par minute.",
                            utility = "Une cadence optimale réduit l'impact au sol et améliore l'économie de course."
                        )
                    }
                    "VMA" -> {
                        MetricInfoContent(
                            title = "VMA",
                            icon = Icons.Rounded.Speed,
                            color = AirSecondary,
                            definition = "La Vitesse Maximale Aérobie est la vitesse à laquelle votre consommation d'oxygène est maximale.",
                            utility = "C'est la donnée fondamentale pour calibrer vos allures d'entraînement fractionné."
                        )
                    }
                    "VO2Max" -> {
                        MetricInfoContent(
                            title = "VO2 Max",
                            icon = Icons.Rounded.MonitorHeart,
                            color = AirPrimary,
                            definition = "Volume maximum d'oxygène que votre corps peut utiliser par kilo de poids et par minute.",
                            utility = "Reflet de votre 'cylindrée' cardiovasculaire et de votre potentiel aérobie."
                        )
                    }
                    "FCM" -> {
                        MetricInfoContent(
                            title = "FC Max",
                            icon = Icons.Rounded.Favorite,
                            color = AirAccent,
                            definition = "Fréquence Cardiaque Maximale.",
                            utility = "Sert à définir vos zones d'intensité (Endurance, Seuil, PMA)."
                        )
                    }
                    "QUADRANT" -> {
                        MetricInfoContent(
                            title = "Analyse par Quadrants",
                            icon = Icons.Rounded.GroupWork,
                            color = AirAccent,
                            definition = "L'analyse par quadrants (AEPF vs CPV) permet de décomposer la puissance en deux composantes : la Force (Couple sur les pédales) et la Vélocité (Vitesse de rotation). Elle se divise en 4 zones basées sur votre FTP et votre cadence de seuil.",
                            interpretation = "• Q1 (Haut/Droit - Sprint) : Force élevée, Cadence élevée. Sollicite les fibres rapides et le système anaérobique.\n\n" +
                                             "• Q2 (Haut/Gauche - Force) : Force élevée, Cadence basse. Course en bosse ou vent de face. Travail musculaire intense.\n\n" +
                                             "• Q3 (Bas/Gauche - Récup) : Force basse, Cadence basse. Récupération ou descente. Stress minimal.\n\n" +
                                             "• Q4 (Bas/Droit - Vélocité) : Force basse, Cadence élevée. Souplesse de pédalage, typique du peloton ou de l'endurance fluide.",
                            utility = "Permet de vérifier si vous respectez les spécificités de votre entraînement (ex: séance de force vs travail de vélocité)."
                        )
                    }
                    else -> {
                        Text("Information non disponible pour $metric", modifier = Modifier.padding(16.dp))
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AirPrimary)
                ) {
                    Text("J'ai compris")
                }
            }
        }
    }
}

@Composable
private fun MetricInfoContent(
    title: String,
    icon: ImageVector,
    color: Color,
    definition: String,
    formula: String? = null,
    utility: String? = null,
    interpretation: String? = null
) {
    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        
        Text("Définition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(definition, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
        
        formula?.let {
            Spacer(Modifier.height(12.dp))
            Text("Formule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }

        utility?.let {
            Spacer(Modifier.height(12.dp))
            Text("Utilité", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        interpretation?.let {
            Spacer(Modifier.height(12.dp))
            Text("Interprétation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
