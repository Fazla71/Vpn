package com.example.vpn

/**
 * Connection states for Proximity VPN
 */
enum class VpnConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR;

    val isConnected: Boolean
        get() = this == CONNECTED

    val isTransitioning: Boolean
        get() = this == CONNECTING || this == DISCONNECTING
}

/**
 * Real-time VPN session statistics
 */
data class VpnSessionStats(
    val durationSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val downSpeedBps: Long = 0L,
    val upSpeedBps: Long = 0L
) {
    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedDownloadSpeed: String
        get() = formatSpeed(downSpeedBps)

    val formattedUploadSpeed: String
        get() = formatSpeed(upSpeedBps)

    val formattedTotalIn: String
        get() = formatBytes(bytesIn)

    val formattedTotalOut: String
        get() = formatBytes(bytesOut)

    private fun formatSpeed(bps: Long): String {
        val mbps = (bps * 8).toDouble() / 1_000_000.0
        val kbps = (bps * 8).toDouble() / 1_000.0
        return when {
            mbps >= 1.0 -> String.format("%.1f Mbps", mbps)
            kbps >= 1.0 -> String.format("%.0f Kbps", kbps)
            else -> "0.0 Kbps"
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            bytes > 0 -> String.format("%d KB", bytes / 1024)
            else -> "0 KB"
        }
    }
}
