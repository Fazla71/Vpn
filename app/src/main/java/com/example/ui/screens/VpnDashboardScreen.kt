package com.example.ui.screens

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConnectButton
import com.example.ui.components.LatencySpeedCard
import com.example.ui.components.ServerSelectionSheet
import com.example.ui.components.ServerSelectorHeader
import com.example.ui.components.SessionTimerCard
import com.example.ui.components.TopBrandingBar
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.VpnViewModel
import com.example.vpn.VpnConnectionStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnDashboardScreen(
    viewModel: VpnViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val status by viewModel.connectionStatus.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val filteredServers by viewModel.filteredServers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val livePing by viewModel.livePing.collectAsState()
    val isTestingPing by viewModel.isTestingPing.collectAsState()

    var showServerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Launcher for standard Android VpnService permission dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnWithSelected(context)
        } else {
            Toast.makeText(context, "VPN permission is required to create tunnel", Toast.LENGTH_SHORT).show()
        }
    }

    val onConnectToggle = {
        viewModel.toggleVpn(context) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
            } else {
                viewModel.startVpnWithSelected(context)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("vpn_dashboard_scaffold"),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Branding Bar
            TopBrandingBar(
                status = status,
                onOpenServers = { showServerSheet = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Selected Country Header with Dropdown
            ServerSelectorHeader(
                selectedServer = selectedServer,
                onOpenServerSheet = { showServerSheet = true },
                onAutoSelectBest = {
                    viewModel.autoSelectBestServer()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Auto-selected lowest latency server!")
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Dynamic Status Headline
            val statusTitle = when (status) {
                VpnConnectionStatus.CONNECTED -> "Your Connection is Protected"
                VpnConnectionStatus.CONNECTING -> "Establishing Secure Tunnel..."
                VpnConnectionStatus.DISCONNECTING -> "Disconnecting..."
                VpnConnectionStatus.DISCONNECTED -> "Your Connection is Unsecured"
                VpnConnectionStatus.ERROR -> "Connection Interrupted"
            }

            val statusColor = when (status) {
                VpnConnectionStatus.CONNECTED -> ConnectedGreen
                VpnConnectionStatus.CONNECTING -> WarningAmber
                VpnConnectionStatus.DISCONNECTING -> DisconnectRed
                VpnConnectionStatus.DISCONNECTED -> TextMuted
                VpnConnectionStatus.ERROR -> DisconnectRed
            }

            Text(
                text = statusTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Center Screen Prominent Circular Glowing Connect Button
            ConnectButton(
                status = status,
                onClick = onConnectToggle
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle hint
            Text(
                text = when (status) {
                    VpnConnectionStatus.CONNECTED -> "Tap power button to disconnect"
                    VpnConnectionStatus.CONNECTING -> "Please wait, routing encrypted traffic..."
                    VpnConnectionStatus.DISCONNECTING -> "Closing virtual interface..."
                    VpnConnectionStatus.DISCONNECTED -> "Tap power button to secure connection"
                    VpnConnectionStatus.ERROR -> "Tap to retry connection"
                },
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Active Session Timer Card (when connected)
            SessionTimerCard(
                status = status,
                sessionStats = sessionStats
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Live Latency/Speed Card (Ping ms, Country Name, IP address)
            LatencySpeedCard(
                server = selectedServer,
                livePing = livePing,
                isTestingPing = isTestingPing,
                status = status,
                sessionStats = sessionStats,
                onRefreshPing = {
                    selectedServer?.let { viewModel.testLivePing(it) }
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Server Selection Modal / Bottom Sheet
        if (showServerSheet) {
            ServerSelectionSheet(
                servers = filteredServers,
                selectedServer = selectedServer,
                searchQuery = searchQuery,
                sortMode = sortMode,
                isRefreshing = isRefreshing,
                onSearchChanged = { viewModel.setSearchQuery(it) },
                onSortModeChanged = { viewModel.setSortMode(it) },
                onServerSelected = { server ->
                    viewModel.selectServer(server)
                    showServerSheet = false
                    // If currently connected to another server, reconnect
                    if (status == VpnConnectionStatus.CONNECTED) {
                        viewModel.startVpnWithSelected(context)
                    }
                },
                onAutoBestSelected = {
                    viewModel.autoSelectBestServer()
                    showServerSheet = false
                },
                onRefreshServers = {
                    viewModel.loadServers(forceRefresh = true)
                },
                onDismiss = { showServerSheet = false },
                sheetState = sheetState
            )
        }
    }
}
