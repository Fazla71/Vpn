package com.example

import com.example.data.api.VpnGateService
import com.example.data.model.VpnServer
import com.example.data.repository.ServerSortMode
import com.example.data.repository.VpnRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnGateServiceTest {

    private val sampleCsv = """
        *vpn_servers
        #HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
        vpn1.opengw.net,198.51.100.1,985000,15,64000000,Japan,JP,45,100000,5000,10000000,2w,Tsukuba,High speed node,Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCg==
        vpn2.opengw.net,198.51.100.2,740000,55,32000000,United States,US,22,50000,2000,5000000,2w,US-West,Reliable server,Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCg==
        *
    """.trimIndent()

    @Test
    fun testParseCsv_filtersCommentsAndExtractsServers() {
        val service = VpnGateService()
        val servers = service.parseCsv(sampleCsv)

        assertEquals(2, servers.size)

        val jpServer = servers[0]
        assertEquals("vpn1.opengw.net", jpServer.hostName)
        assertEquals("198.51.100.1", jpServer.ip)
        assertEquals(15, jpServer.ping)
        assertEquals("Japan", jpServer.countryLong)
        assertEquals("JP", jpServer.countryShort)
        assertEquals("🇯🇵", jpServer.flagEmoji)

        val usServer = servers[1]
        assertEquals("US", usServer.countryShort)
        assertEquals("🇺🇸", usServer.flagEmoji)
    }

    @Test
    fun testFindBestServer_selectsLowestPing() {
        val repo = VpnRepository()
        val server1 = VpnServer("s1", "1.1.1.1", 100, 80, 20_000_000, "UK", "GB", 10, 100, 10, 100, "", "", "", "")
        val server2 = VpnServer("s2", "2.2.2.2", 200, 18, 50_000_000, "JP", "JP", 10, 100, 10, 100, "", "", "", "")
        val server3 = VpnServer("s3", "3.3.3.3", 150, 45, 30_000_000, "US", "US", 10, 100, 10, 100, "", "", "", "")

        val best = repo.findBestServer(listOf(server1, server2, server3))
        assertNotNull(best)
        assertEquals("JP", best?.countryShort)
        assertEquals(18, best?.ping)
    }

    @Test
    fun testSortServers_lowestPing() {
        val repo = VpnRepository()
        val server1 = VpnServer("s1", "1.1.1.1", 100, 120, 20_000_000, "UK", "GB", 10, 100, 10, 100, "", "", "", "")
        val server2 = VpnServer("s2", "2.2.2.2", 200, 15, 50_000_000, "JP", "JP", 10, 100, 10, 100, "", "", "", "")
        val server3 = VpnServer("s3", "3.3.3.3", 150, 40, 30_000_000, "US", "US", 10, 100, 10, 100, "", "", "", "")

        val sorted = repo.sortServers(listOf(server1, server2, server3), ServerSortMode.LOWEST_PING)
        assertEquals(15, sorted[0].ping)
        assertEquals(40, sorted[1].ping)
        assertEquals(120, sorted[2].ping)
    }
}
