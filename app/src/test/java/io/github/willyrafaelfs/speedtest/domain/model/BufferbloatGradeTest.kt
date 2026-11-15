package io.github.willyrafaelfs.speedtest.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BufferbloatGradeTest {

    @Test
    fun `grade mengikuti ambang kenaikan latency`() {
        val cases = mapOf(
            -3.0 to BufferbloatGrade.A_PLUS,
            0.0 to BufferbloatGrade.A_PLUS,
            5.0 to BufferbloatGrade.A_PLUS,
            5.1 to BufferbloatGrade.A,
            30.0 to BufferbloatGrade.A,
            45.0 to BufferbloatGrade.B,
            150.0 to BufferbloatGrade.C,
            400.0 to BufferbloatGrade.D,
            1_000.0 to BufferbloatGrade.F,
        )

        cases.forEach { (increase, expected) ->
            assertEquals("Kenaikan $increase ms", expected, BufferbloatGrade.fromLatencyIncrease(increase))
        }
    }

    @Test
    fun `grade memakai fase dengan kenaikan latency terburuk`() {
        // Idle 20 ms; saat download 35 ms (+15, A), saat upload 90 ms (+70, C).
        assertEquals(BufferbloatGrade.C, BufferbloatGrade.of(20.0, 35.0, 90.0))
        assertEquals(BufferbloatGrade.A, BufferbloatGrade.of(20.0, 35.0, null))
    }

    @Test
    fun `tanpa latency saat beban grade tidak tersedia`() {
        assertNull(BufferbloatGrade.of(20.0, null, null))
        assertNull(SpeedTestResultFixture.result(downloadLatencyMs = null, uploadLatencyMs = null).bufferbloatGrade)
    }

    private object SpeedTestResultFixture {
        fun result(downloadLatencyMs: Double?, uploadLatencyMs: Double?) = SpeedTestResult(
            timestamp = 0,
            pingMs = 20.0,
            jitterMs = 2.0,
            downloadMbps = 100.0,
            uploadMbps = 20.0,
            networkType = NetworkType.WIFI,
            networkName = null,
            downloadLatencyMs = downloadLatencyMs,
            uploadLatencyMs = uploadLatencyMs,
        )
    }
}
