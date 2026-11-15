package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.data.di.NetworkModule
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Menguji engine upload terhadap [FakeSpeedServer] yang membatasi laju baca request body. */
class UploadSpeedTesterTest {

    private val server = FakeSpeedServer()
    private val client = NetworkModule.provideOkHttpClient()

    @After
    fun tearDown() = server.close()

    private fun tester(config: SpeedTestConfig = server.config()) =
        UploadSpeedTester(client, config, PingTester(client, config))

    @Test
    fun `hasil final mendekati bandwidth yang dibatasi server`() = runTest(timeout = TEST_TIMEOUT) {
        val config = server.config(streamCount = 2)
        val final = tester(config).measure().toList().last()

        val expectedMbps = server.bytesPerSecondPerConnection * config.streamCount * BITS_PER_BYTE / BITS_PER_MBIT
        assertEquals(1f, final.progressFraction)
        assertTrue(
            "Diharapkan sekitar $expectedMbps Mbps, didapat ${final.averageMbps}",
            final.averageMbps in expectedMbps * MIN_RATIO..expectedMbps * MAX_RATIO,
        )
        assertNotNull(final.loadedLatencyMs)
    }

    @Test
    fun `ukuran request berlipat ganda selama request selesai cepat`() = runTest(timeout = TEST_TIMEOUT) {
        val config = server.config(streamCount = 1)
        tester(config).measure().toList()

        val sizes = server.requestsTo("/up").map { it.bodySize }
        assertEquals(config.uploadMinRequestBytes, sizes.first())
        assertTrue("Ukuran request tidak bertambah: $sizes", sizes.max() > config.uploadMinRequestBytes)
        assertTrue(sizes.all { it <= config.uploadMaxRequestBytes })
    }

    @Test
    fun `body upload berupa data acak yang tidak bisa dikompresi`() = runTest(timeout = TEST_TIMEOUT) {
        tester().measure().toList()

        val request = server.requestsTo("/up").first()
        assertEquals("application/octet-stream", request.headers["Content-Type"])
        // Content-Length tetap (bukan chunked), sesuai ukuran request yang direncanakan.
        assertEquals(request.bodySize.toString(), request.headers["Content-Length"])
    }

    @Test
    fun `status HTTP gagal dilempar sebagai HttpStatusException`() = runTest(timeout = TEST_TIMEOUT) {
        server.statusOverrides["/up"] = 500
        try {
            tester().measure().toList()
            fail("Seharusnya melempar HttpStatusException")
        } catch (e: HttpStatusException) {
            assertEquals(500, e.code)
        }
    }

    private companion object {
        val TEST_TIMEOUT = 30.seconds
        const val BITS_PER_BYTE = 8.0
        const val BITS_PER_MBIT = 1_000_000.0

        // Seperti di DownloadSpeedTesterTest: request 256 KB = 8 gumpalan throttle yang
        // selesai dalam 7 periode, jadi laju aktual ~8/7 = 1,14x nominal.
        const val MIN_RATIO = 0.6
        const val MAX_RATIO = 1.25
    }
}
