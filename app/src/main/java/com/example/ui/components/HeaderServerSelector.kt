package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.VpnServer
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.ConnectedGreenGlow
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.DisconnectRedGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.vpn.VpnConnectionStatus

@Composable
fun TopBrandingBar(
    status: VpnConnectionStatus,
    onOpenServers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Tech Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_branding_logo")
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceCardElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_vpn_logo),
                    contentDescription = "Proximity VPN Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "PROXIMITY VPN",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (status == VpnConnectionStatus.CONNECTED) ConnectedGreen
                                else DisconnectRed
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (status == VpnConnectionStatus.CONNECTED) "SECURE TUNNEL ACTIVE"
                        else "PROTECTION INACTIVE",
                        color = if (status == VpnConnectionStatus.CONNECTED) ConnectedGreen
                        else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Action button to open servers
        IconButton(
            onClick = onOpenServers,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SurfaceCardElevated)
                .border(1.dp, CardBorder, CircleShape)
                .testTag("btn_open_server_modal")
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Server Settings",
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ServerSelectorHeader(
    selectedServer: VpnServer?,
    onOpenServerSheet: () -> Unit,
    onAutoSelectBest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "SELECTED LOCATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .clickable { onOpenServerSheet() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("server_dropdown_header"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Flag
                Text(
                    text = selectedServer?.flagEmoji ?: "🌐",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Text(
                        text = selectedServer?.countryLong ?: "Auto-Select Optimal Server",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (selectedServer != null) "${selectedServer.ip} • ${selectedServer.pingFormatted}"
                        else "Tap to select from global server nodes",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Auto Best quick chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyanGlow)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onAutoSelectBest() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_auto_best_server")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Auto Best",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Auto",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open Server Selector",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
