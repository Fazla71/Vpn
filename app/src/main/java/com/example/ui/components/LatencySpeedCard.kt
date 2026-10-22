package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VpnServer
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnSessionStats

@Composable
fun LatencySpeedCard(
    server: VpnServer?,
    livePing: Int?,
    isTestingPing: Boolean,
    status: VpnConnectionStatus,
    sessionStats: VpnSessionStats,
    onRefreshPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectivePing = livePing ?: server?.ping ?: 50

    val pingColor = when {
        effectivePing <= 45 -> ConnectedGreen
        effectivePing <= 95 -> WarningAmber
        else -> DisconnectRed
    }

    val pingQualityLabel = when {
        effectivePing <= 45 -> "Optimal"
        effectivePing <= 95 -> "Good"
        else -> "Moderate"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("latency_speed_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Section: Country and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server?.flagEmoji ?: "🌐",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Text(
                            text = server?.countryLong ?: "Select Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = server?.operator?.takeIf { it.isNotBlank() } ?: "VPNGate Public Relay",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                // Security Protocol Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Protocol",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-256",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Middle Grid: Live Latency (ms) and IP Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latency Card Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCardElevated)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(pingColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LATENCY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            if (isTestingPing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Test Ping",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRefreshPing() }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$effectivePing",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = pingColor
                            )
                            Text(
                                text = " ms",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = pingQualityLabel,
                                fontSize = 11.sp,
                                color = pingColor,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // IP Address Card Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCardElevated)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Server IP",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SERVER IP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            val serverIp = server?.ip ?: "127.0.0.1"
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy IP",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("VPN Server IP", serverIp))
                                        Toast.makeText(context, "Copied IP: $serverIp", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = server?.ip ?: "---.---.---.---",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "Port ${server?.openVpnProtocolAndPort?.second ?: 1194} / ${server?.openVpnProtocolAndPort?.first?.uppercase() ?: "UDP"}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Live Network Speed & Session Metrics (When Connected)
            AnimatedVisibility(
                visible = status == VpnConnectionStatus.CONNECTED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF091426))
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Download",
                                    tint = ConnectedGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "DOWNLOAD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = sessionStats.formattedDownloadSpeed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConnectedGreen
                                    )
                                }
                            }

                            // Upload
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Upload",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "UPLOAD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = sessionStats.formattedUploadSpeed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }

                            // Total Transferred
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DATA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                Text(
                                    text = sessionStats.formattedTotalIn,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
