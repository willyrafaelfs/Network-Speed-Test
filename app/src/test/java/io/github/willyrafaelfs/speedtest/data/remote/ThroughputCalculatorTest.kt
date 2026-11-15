package io.github.willyrafaelfs.speedtest.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThroughputCalculatorTest {

    /** Span per detik dengan laju [mbpsPerSecond] untuk detik ke-0, ke-1, dst. */
    private fun spansPerSecond(vararg mbpsPerSecond: Double) = mbpsPerSecond.mapIndexed { i, mbps ->
        ByteSpan(startNanos = i * SEC, endNanos = (i + 1) * SEC, bytes = (mbps * 1_000_000 / 8).toLong())
    }

    @Test
    fun `mbps memakai satuan desimal`() {
        // 1.250.000 byte dalam 1 detik = 10.000.000 bit/s = 10 Mbps.
        assertEquals(10.0, ThroughputCalculator.mbps(1_250_000.0, SEC), DELTA)
        assertEquals(0.0, ThroughputCalculator.mbps(1_000.0, 0), DELTA)
    }

    @Test
    fun `bytesBetween menghitung bagian span yang beririsan secara proporsional`() {
        val spans = listOf(ByteSpan(0, 2 * SEC, 1_000))

        assertEquals(500.0, ThroughputCalculator.bytesBetween(spans, SEC, 3 * SEC), DELTA)
        assertEquals(250.0, ThroughputCalculator.bytesBetween(spans, SEC / 2, SEC), DELTA)
        assertEquals(0.0, ThroughputCalculator.bytesBetween(spans, 2 * SEC, 3 * SEC), DELTA)
    }

    @Test
    fun `laju konstan menghasilkan nilai yang sama di setiap jendela`() {
        val spans = spansPerSecond(50.0, 50.0, 50.0, 50.0)

        val rates = ThroughputCalculator.windowRates(spans, 0, 4 * SEC, SEC, SEC / 4)

        assertEquals(13, rates.size) // jendela mulai di 0, 0.25, ..., 3.0 detik
        rates.forEach { assertEquals(50.0, it, DELTA) }
    }

    @Test
    fun `fase ramp-up yang lambat tidak menurunkan hasil final`() {
        // Slow start: 2 detik pertama lambat, lalu stabil di 100 Mbps. Tanpa membuang
        // ramp-up, 8 dari 13 jendela ikut memuat fase lambat sehingga median turun.
        val spans = spansPerSecond(5.0, 40.0, 100.0, 100.0)

        val withRampUp = ThroughputCalculator.finalMbps(spans, 0, 4 * SEC, analysis(SEC, SEC / 4, 0.5))!!
        val withoutRampUp = ThroughputCalculator.finalMbps(spans, 2 * SEC, 4 * SEC, analysis(SEC, SEC / 4, 0.5))!!

        assertTrue(withRampUp < 100.0)
        assertEquals(100.0, withoutRampUp, DELTA)
    }

    @Test
    fun `persentil memakai interpolasi linear`() {
        val values = listOf(40.0, 10.0, 30.0, 20.0)

        assertEquals(10.0, ThroughputCalculator.percentile(values, 0.0), DELTA)
        assertEquals(25.0, ThroughputCalculator.percentile(values, 0.5), DELTA)
        assertEquals(37.0, ThroughputCalculator.percentile(values, 0.9), DELTA)
        assertEquals(40.0, ThroughputCalculator.percentile(values, 1.0), DELTA)
    }

    @Test
    fun `persentil 90 mengabaikan satu jendela yang anjlok`() {
        val spans = spansPerSecond(80.0, 80.0, 10.0, 80.0, 80.0, 80.0)

        val p90 = ThroughputCalculator.finalMbps(spans, 0, 6 * SEC, analysis(SEC, SEC, 0.9))!!

        assertEquals(80.0, p90, DELTA)
    }

    @Test
    fun `rentang lebih pendek dari jendela dihitung sebagai satu jendela`() {
        val spans = spansPerSecond(24.0)

        val rates = ThroughputCalculator.windowRates(spans, 0, SEC / 2, SEC, SEC / 4)

        assertEquals(listOf(24.0), rates.map { Math.round(it * 1000) / 1000.0 })
    }

    @Test
    fun `rentang kosong tidak menghasilkan nilai`() {
        assertNull(ThroughputCalculator.finalMbps(spansPerSecond(10.0), SEC, SEC, analysis(SEC, SEC, 0.9)))
    }

    private fun analysis(windowNanos: Long, stepNanos: Long, percentile: Double) =
        ThroughputCalculator.Analysis(windowNanos, stepNanos, percentile)

    private companion object {
        const val SEC = 1_000_000_000L
        const val DELTA = 1e-6
    }
}
