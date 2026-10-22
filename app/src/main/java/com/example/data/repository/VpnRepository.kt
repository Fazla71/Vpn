package com.example.data.repository

import com.example.data.api.VpnGateService
import com.example.data.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class ServerSortMode {
    LOWEST_PING,
    FASTEST_SPEED,
    COUNTRY_NAME,
    MOST_SESSIONS
}

class VpnRepository(
    private val vpnGateService: VpnGateService = VpnGateService()
) {
    private var cachedServers: List<VpnServer> = emptyList()

    suspend fun getServers(forceRefresh: Boolean = false): List<VpnServer> {
        if (cachedServers.isNotEmpty() && !forceRefresh) {
            return cachedServers
        }

        val result = vpnGateService.fetchServers()
        cachedServers = result.getOrElse { vpnGateService.getFallbackServers() }
        return cachedServers
    }

    /**
     * Finds the optimal server: nearest / lowest ping, weighted with speed and score.
     */
    fun findBestServer(servers: List<VpnServer>): VpnServer? {
        if (servers.isEmpty()) return null
        return servers.minByOrNull { server ->
            // Penalize very high pings or 0 ping
            val effectivePing = if (server.ping <= 0) 100 else server.ping
            // Lower score is better for minBy (ping - speed bonus)
            val speedBonus = (server.speed / 10_000_000L).coerceAtMost(30)
            effectivePing - speedBonus
        } ?: servers.first()
    }

    fun sortServers(servers: List<VpnServer>, sortMode: ServerSortMode): List<VpnServer> {
        return when (sortMode) {
            ServerSortMode.LOWEST_PING -> servers.sortedWith(
                compareBy<VpnServer> { if (it.ping <= 0) 999 else it.ping }
                    .thenByDescending { it.speed }
            )
            ServerSortMode.FASTEST_SPEED -> servers.sortedByDescending { it.speed }
            ServerSortMode.COUNTRY_NAME -> servers.sortedBy { it.countryLong }
            ServerSortMode.MOST_SESSIONS -> servers.sortedByDescending { it.numVpnSessions }
        }
    }

    /**
     * Measures live latency to server IP via TCP socket
     */
    suspend fun measureRealPing(ip: String, port: Int = 443, timeoutMs: Int = 1500): Int = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                (System.currentTimeMillis() - start).toInt()
            }
        } catch (_: Exception) {
            -1
        }
    }
}
