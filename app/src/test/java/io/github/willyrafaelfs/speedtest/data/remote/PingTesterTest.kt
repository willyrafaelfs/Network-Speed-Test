package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.data.di.NetworkModule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PingTesterTest {

    private val server = FakeSpeedServer(pingDelayMillis = PING_DELAY_MILLIS)
    private val client = NetworkModule.provideOkHttpClient()

    @After
    fun tearDown() = server.close()

    @Test
    fun `latency mencakup waktu tunggu server dan jitter tidak negatif`() = runTest {
        val config = server.config()
        val result = PingTester(client, config).measure()

        assertTrue("Latency ${result.latencyMs} ms", result.latencyMs >= PING_DELAY_MILLIS)
        assertTrue(result.jitterMs >= 0.0)
        // 1 warm-up + pingCount sampel.
        assertEquals(config.pingCount + 1, server.requestsTo("/ping").size)
    }

    @Test
    fun `server yang menolak ping menghasilkan HttpStatusException`() = runTest {
        server.statusOverrides["/ping"] = 403
        try {
            PingTester(client, server.config()).measure()
            fail("Seharusnya melempar HttpStatusException")
        } catch (e: HttpStatusException) {
            assertEquals(403, e.code)
        }
    }

    @Test
    fun `latency memakai median sehingga satu ping lambat tidak menggeser hasil`() {
        val latencies = listOf(20.0, 21.0, 19.0, 480.0, 20.0, 22.0, 20.0, 21.0, 19.0, 20.0)

        val result = PingTester.summarize(latencies)

        // Rata-rata akan menjadi 66,2 ms; median tetap 20 ms.
        assertEquals(20.0, result.latencyMs, 1e-9)
        assertTrue("Jitter tetap mencerminkan lonjakan: ${result.jitterMs}", result.jitterMs > 50.0)
    }

    private companion object {
        const val PING_DELAY_MILLIS = 20L
    }
}
