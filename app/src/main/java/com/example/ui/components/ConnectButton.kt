package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.ConnectedGreenGlow
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.DisconnectRedGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberGlow
import com.example.vpn.VpnConnectionStatus

@Composable
fun ConnectButton(
    status: VpnConnectionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    // Dynamic wave animation based on connection status
    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == VpnConnectionStatus.CONNECTING) 1200 else 2400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_1"
    )

    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == VpnConnectionStatus.CONNECTING) 1200 else 2400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_1"
    )

    val ringScale2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == VpnConnectionStatus.CONNECTING) 1200 else 2400,
                delayMillis = 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_2"
    )

    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == VpnConnectionStatus.CONNECTING) 1200 else 2400,
                delayMillis = 600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_2"
    )

    val glowColor by animateColorAsState(
        targetValue = when (status) {
            VpnConnectionStatus.CONNECTED -> ConnectedGreen
            VpnConnectionStatus.CONNECTING -> WarningAmber
            VpnConnectionStatus.DISCONNECTING -> DisconnectRed
            VpnConnectionStatus.DISCONNECTED -> NeonCyan
            VpnConnectionStatus.ERROR -> DisconnectRed
        },
        label = "glow_color"
    )

    val glowBgColor by animateColorAsState(
        targetValue = when (status) {
            VpnConnectionStatus.CONNECTED -> ConnectedGreenGlow
            VpnConnectionStatus.CONNECTING -> WarningAmberGlow
            VpnConnectionStatus.DISCONNECTING -> DisconnectRedGlow
            VpnConnectionStatus.DISCONNECTED -> NeonCyanGlow
            VpnConnectionStatus.ERROR -> DisconnectRedGlow
        },
        label = "glow_bg_color"
    )

    val buttonText = when (status) {
        VpnConnectionStatus.CONNECTED -> "CONNECTED"
        VpnConnectionStatus.CONNECTING -> "CONNECTING..."
        VpnConnectionStatus.DISCONNECTING -> "DISCONNECTING..."
        VpnConnectionStatus.DISCONNECTED -> "CONNECT"
        VpnConnectionStatus.ERROR -> "RETRY"
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("vpn_connect_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layer glowing animated radar ripples
        Canvas(modifier = Modifier.size(240.dp)) {
            val centerRadius = size.minDimension / 2.7f

            // Outer ring 2
            if (status != VpnConnectionStatus.DISCONNECTED || ringAlpha2 > 0.05f) {
                drawCircle(
                    color = glowColor.copy(alpha = ringAlpha2),
                    radius = centerRadius * ringScale2,
                    style = Stroke(width = 2.5f)
                )
            }

            // Outer ring 1
            drawCircle(
                color = glowColor.copy(alpha = ringAlpha1),
                radius = centerRadius * ringScale1,
                style = Stroke(width = 3.5f)
            )

            // Soft static halo
            drawCircle(
                color = glowBgColor,
                radius = centerRadius * 1.15f
            )
        }

        // Center circular interactive button
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = if (status == VpnConnectionStatus.CONNECTED) 24.dp else 12.dp,
                    shape = CircleShape,
                    spotColor = glowColor,
                    ambientColor = glowColor
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SurfaceCardElevated,
                            SurfaceCard
                        )
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = glowColor),
                    onClick = onClick
                )
                .testTag("vpn_connect_button"),
            contentAlignment = Alignment.Center
        ) {
            // Circular outline border with cyber gradient
            Canvas(modifier = Modifier.size(150.dp)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.9f),
                            glowColor.copy(alpha = 0.2f),
                            glowColor.copy(alpha = 0.9f)
                        )
                    ),
                    radius = (size.minDimension / 2) - 2f,
                    style = Stroke(width = 3.5f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (status == VpnConnectionStatus.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(46.dp),
                        color = WarningAmber,
                        strokeWidth = 3.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "VPN Power Switch",
                        tint = glowColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buttonText,
                    color = glowColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
