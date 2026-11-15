package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.data.di.NetworkModule
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Menguji engine download terhadap [FakeSpeedServer] dengan bandwidth yang dibatasi.
 * Test ini memakai waktu nyata (bukan virtual time) karena mengukur I/O sungguhan.
 */
class DownloadSpeedTesterTest {

    private val server = FakeSpeedServer()
    private val client = NetworkModule.provideOkHttpClient()

    @After
    fun tearDown() = server.close()

    private fun tester(config: SpeedTestConfig = server.config()) =
        DownloadSpeedTester(client, config, PingTester(client, config))

    @Test
    fun `hasil final mendekati bandwidth yang dibatasi server`() = runTest(timeout = TEST_TIMEOUT) {
        val config = server.config(streamCount = 2)
        val final = tester(config).measure().toList().last()

        val expectedMbps = server.bytesPerSecondPerConnection * config.streamCount * BITS_PER_BYTE / BITS_PER_MBIT
        assertEquals(1f, final.progressFraction)
        assertEquals(final.instantMbps, final.averageMbps, 0.0)
        assertInRange(final.averageMbps, expectedMbps * MIN_RATIO, expectedMbps * MAX_RATIO)
    }

    @Test
    fun `memakai koneksi TCP terpisah untuk setiap stream`() = runTest(timeout = TEST_TIMEOUT) {
        tester(server.config(streamCount = 3)).measure().toList()

        val connections = server.requestsTo("/down").map { it.connectionIndex }.toSet()
        assertTrue("Butuh minimal 3 koneksi download, didapat $connections", connections.size >= 3)
        // Gzip transparan OkHttp harus mati agar byte terhitung = byte di jaringan.
        assertTrue(server.requestsTo("/down").all { it.headers["Accept-Encoding"] == "identity" })
    }

    @Test
    fun `mengulang request saat file habis sebelum waktu test selesai`() = runTest(timeout = TEST_TIMEOUT) {
        // Body 512 KB dengan laju ~640 KB/s per koneksi habis kurang dari 1 detik.
        tester(server.config(streamCount = 1)).measure().toList()

        assertTrue(server.requestsTo("/down").size >= 2)
    }

    @Test
    fun `progress naik bertahap dan sampel terakhir membawa latency saat beban`() = runTest(timeout = TEST_TIMEOUT) {
        val samples = tester().measure().toList()

        assertTrue(samples.size > 5)
        assertTrue(samples.zipWithNext().all { (a, b) -> b.progressFraction >= a.progressFraction })
        assertNotNull(samples.last().loadedLatencyMs)
        // Satu request ping dipakai untuk warm-up, lalu ping berkala tiap 100 ms.
        assertTrue(server.requestsTo("/ping").size >= 3)
    }

    @Test
    fun `status HTTP gagal dilempar sebagai HttpStatusException`() = runTest(timeout = TEST_TIMEOUT) {
        server.statusOverrides["/down"] = 503
        try {
            tester().measure().toList()
            fail("Seharusnya melempar HttpStatusException")
        } catch (e: HttpStatusException) {
            assertEquals(503, e.code)
        }
    }

    @Test
    fun `pembatalan menghentikan semua koneksi tanpa menunggu durasi test`() = runTest(timeout = TEST_TIMEOUT) {
        val config = server.config(testDurationMillis = 20_000)
        val started = TimeSource.Monotonic.markNow()

        // take() membatalkan flow setelah 3 sampel (~300 ms).
        tester(config).measure().take(3).toList()

        val elapsed = started.elapsedNow()
        assertTrue("Pembatalan butuh $elapsed", elapsed < 5.seconds)
    }

    private fun assertInRange(actual: Double, min: Double, max: Double) =
        assertTrue("Diharapkan $min..$max Mbps, didapat $actual", actual in min..max)

    private companion object {
        val TEST_TIMEOUT = 30.seconds
        const val BITS_PER_BYTE = 8.0
        const val BITS_PER_MBIT = 1_000_000.0

        // Throttle MockWebServer tidur DI ANTARA gumpalan, sehingga body berisi N gumpalan
        // selesai dalam (N-1) periode: laju aktual = N/(N-1) x nominal (~1,07x untuk body
        // 512 KB). Batas bawah memberi ruang untuk runner CI yang lambat.
        const val MIN_RATIO = 0.6
        const val MAX_RATIO = 1.25
    }
}
