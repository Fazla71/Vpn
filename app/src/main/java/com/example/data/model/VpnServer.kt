package com.example.data.model

import android.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Represents a VPN Gate server fetched from https://www.vpngate.net/api/iphone/
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val ping: Int,
    val speed: Long, // in bytes per second (bps)
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Long,
    val uptime: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigDataBase64: String
) {
    /**
     * Converts 2-letter country code (ISO 3166-1 alpha-2) to Flag Emoji.
     * E.g., "US" -> 🇺🇸, "JP" -> 🇯🇵
     */
    val flagEmoji: String
        get() {
            if (countryShort.length != 2) return "🌐"
            val upper = countryShort.uppercase()
            val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
            return if (Character.isLetter(upper[0]) && Character.isLetter(upper[1])) {
                String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
            } else {
                "🌐"
            }
        }

    /**
     * Speed formatted in Mbps
     */
    val speedMbps: Double
        get() = (speed.toDouble() / 1_000_000.0)

    val speedFormatted: String
        get() {
            return when {
                speedMbps >= 100.0 -> String.format("%.0f Mbps", speedMbps)
                speedMbps >= 1.0 -> String.format("%.1f Mbps", speedMbps)
                speed > 0 -> String.format("%.0f Kbps", speed.toDouble() / 1_000.0)
                else -> "N/A"
            }
        }

    val pingFormatted: String
        get() = if (ping > 0) "$ping ms" else "<50 ms"

    /**
     * Decoded plain text OpenVPN configuration (.ovpn)
     */
    val decodedConfig: String
        get() = try {
            val decodedBytes = Base64.decode(openVpnConfigDataBase64, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }

    /**
     * Extracted remote server port and protocol from OpenVPN config if available
     */
    val openVpnProtocolAndPort: Pair<String, Int>
        get() {
            var proto = "udp"
            var port = 1194
            try {
                decodedConfig.lineSequence().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("proto ", ignoreCase = true)) {
                        proto = trimmed.substring(6).trim().lowercase()
                    } else if (trimmed.startsWith("remote ", ignoreCase = true)) {
                        val parts = trimmed.split("\\s+".toRegex())
                        if (parts.size >= 3) {
                            parts[2].toIntOrNull()?.let { port = it }
                        }
                    }
                }
            } catch (_: Exception) {}
            return Pair(proto, port)
        }
}
