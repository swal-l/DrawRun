package com.orbital.run.presentation.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Premium button with haptic feedback and micro-animations.
 *
 * Features:
 * - Spring animation on press (scale 0.96)
 * - Optional vibration feedback
 * - Icon support
 * - Multiple style variants (Filled, Outlined, Text)
 *
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 * @param vibrationFeedback Whether to vibrate on press (premium tactile feel)
 * @param icon Optional leading icon
 * @param variant Button style variant
 */
@Composable
fun DrawRunButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    vibrationFeedback: Boolean = false,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.Filled
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    
    // Micro-animation: scale down when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    // Haptic feedback on press
    if (isPressed && enabled && vibrationFeedback) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
    
    when (variant) {
        ButtonVariant.Filled -> {
            Button(
                onClick = onClick,
                modifier = modifier.scale(scale),
                enabled = enabled,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(icon, text)
            }
        }
        
        ButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.scale(scale),
                enabled = enabled,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(icon, text)
            }
        }
        
        ButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.scale(scale),
                enabled = enabled,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                ButtonContent(icon, text)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector?,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Button style variants
 */
enum class ButtonVariant {
    /** Filled background with primary color */
    Filled,
    
    /** Outlined border, transparent background */
    Outlined,
    
    /** Text only, no background */
    Text
}
