package io.github.willyrafaelfs.speedtest.data.repository

import io.github.willyrafaelfs.speedtest.data.di.NetworkModule
import io.github.willyrafaelfs.speedtest.data.remote.DownloadSpeedTester
import io.github.willyrafaelfs.speedtest.data.remote.FakeSpeedServer
import io.github.willyrafaelfs.speedtest.data.remote.PingTester
import io.github.willyrafaelfs.speedtest.data.remote.SpeedTestConfig
import io.github.willyrafaelfs.speedtest.data.remote.UploadSpeedTester
import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Memastikan exception engine sampai ke domain sebagai [SpeedTestException] yang tepat. */
class SpeedTestRepositoryImplTest {

    private val server = FakeSpeedServer()
    private val client = NetworkModule.provideOkHttpClient()

    @After
    fun tearDown() = server.close()

    private fun repository(config: SpeedTestConfig): SpeedTestRepositoryImpl {
        val ping = PingTester(client, config)
        return SpeedTestRepositoryImpl(
            pingTester = ping,
            downloadTester = DownloadSpeedTester(client, config, ping),
            uploadTester = UploadSpeedTester(client, config, ping),
        )
    }

    @Test
    fun `server yang tidak bisa dihubungi menjadi NoConnection`() = runTest {
        val config = server.config()
        server.close() // port kini ditolak (connection refused)

        assertError(SpeedTestError.NoConnection) { repository(config).measurePing() }
    }

    @Test
    fun `status HTTP gagal saat download menjadi ServerError`() = runTest {
        server.statusOverrides["/down"] = 503

        assertError(SpeedTestError.ServerError(503)) {
            repository(server.config()).measureDownload(MeasurementOptions()).toList()
        }
    }

    @Test
    fun `status HTTP gagal saat upload menjadi ServerError`() = runTest {
        server.statusOverrides["/up"] = 429

        assertError(SpeedTestError.ServerError(429)) {
            repository(server.config()).measureUpload(MeasurementOptions()).toList()
        }
    }

    private suspend fun assertError(expected: SpeedTestError, block: suspend () -> Unit) {
        try {
            block()
            fail("Seharusnya melempar SpeedTestException($expected)")
        } catch (e: SpeedTestException) {
            assertEquals(expected, e.error)
        }
    }
}
