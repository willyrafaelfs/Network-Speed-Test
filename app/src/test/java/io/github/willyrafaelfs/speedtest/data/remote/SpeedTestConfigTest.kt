package io.github.willyrafaelfs.speedtest.data.remote

import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTestConfigTest {

    @Test
    fun `opsi dari Settings mengganti durasi dan jumlah koneksi saja`() {
        val base = SpeedTestConfig()

        val config = base.withOptions(MeasurementOptions(durationMillis = 20_000, streamCount = 8))

        assertEquals(20_000L, config.testDurationMillis)
        assertEquals(8, config.streamCount)
        assertEquals(base.rampUpMillis, config.rampUpMillis)
        assertEquals(base.downloadUrl, config.downloadUrl)
    }
}
