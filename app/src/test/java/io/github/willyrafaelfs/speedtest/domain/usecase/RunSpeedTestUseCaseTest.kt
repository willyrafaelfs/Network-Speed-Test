package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSpeedTestUseCaseTest {

    /** Repository palsu dengan hasil yang bisa ditentukan untuk pengujian. */
    private class FakeSpeedTestRepository(
        private val ping: PingResult,
        private val downloadSamples: List<SpeedSample>,
        private val uploadSamples: List<SpeedSample>,
    ) : SpeedTestRepository {
        override suspend fun measurePing(): PingResult = ping
        override fun measureDownload(): Flow<SpeedSample> = downloadSamples.asFlow()
        override fun measureUpload(): Flow<SpeedSample> = uploadSamples.asFlow()
    }

    private val fakeRepository = FakeSpeedTestRepository(
        ping = PingResult(avgLatencyMs = 20.0, jitterMs = 4.0),
        downloadSamples = listOf(
            SpeedSample(instantMbps = 10.0, averageMbps = 8.0, progressFraction = 0.5f),
            SpeedSample(instantMbps = 20.0, averageMbps = 15.0, progressFraction = 1f, loadedLatencyMs = 45.0),
        ),
        uploadSamples = listOf(
            SpeedSample(instantMbps = 5.0, averageMbps = 4.0, progressFraction = 0.5f),
            SpeedSample(instantMbps = 8.0, averageMbps = 6.0, progressFraction = 1f, loadedLatencyMs = 120.0),
        ),
    )

    @Test
    fun `melalui fase PING lalu DOWNLOAD lalu UPLOAD lalu FINISHED`() = runTest {
        val emissions = RunSpeedTestUseCase(fakeRepository)().toList()
        val phases = emissions.map { it.phase }.distinct()

        assertEquals(SpeedTestPhase.PING, emissions.first().phase)
        assertEquals(
            listOf(
                SpeedTestPhase.PING,
                SpeedTestPhase.DOWNLOAD,
                SpeedTestPhase.UPLOAD,
                SpeedTestPhase.FINISHED,
            ),
            phases,
        )
    }

    @Test
    fun `emisi FINISHED memuat seluruh metrik final`() = runTest {
        val finished = RunSpeedTestUseCase(fakeRepository)().toList().last()

        assertEquals(SpeedTestPhase.FINISHED, finished.phase)
        assertEquals(20.0, finished.pingMs!!, DELTA)
        assertEquals(4.0, finished.jitterMs!!, DELTA)
        // Nilai final = averageMbps dari sampel terakhir tiap fase.
        assertEquals(15.0, finished.downloadMbps!!, DELTA)
        assertEquals(6.0, finished.uploadMbps!!, DELTA)
    }

    @Test
    fun `hasil ping dibawa terus selama fase download dan upload`() = runTest {
        val emissions = RunSpeedTestUseCase(fakeRepository)().toList()

        val duringDownload = emissions.first { it.phase == SpeedTestPhase.DOWNLOAD }
        val duringUpload = emissions.first { it.phase == SpeedTestPhase.UPLOAD }

        assertEquals(20.0, duringDownload.pingMs!!, DELTA)
        // Saat upload, download sudah selesai dan nilainya ikut terbawa.
        assertTrue(duringUpload.downloadMbps != null)
        assertEquals(15.0, duringUpload.downloadMbps!!, DELTA)
    }

    @Test
    fun `latency saat beban dari sampel terakhir tiap fase ikut ke hasil final`() = runTest {
        val finished = RunSpeedTestUseCase(fakeRepository)().toList().last()

        assertEquals(45.0, finished.downloadLatencyMs!!, DELTA)
        assertEquals(120.0, finished.uploadLatencyMs!!, DELTA)
        assertEquals(0.0, finished.currentSpeedMbps, DELTA)
    }

    @Test
    fun `error dari repository diteruskan dan menghentikan fase berikutnya`() = runTest {
        val failing = object : SpeedTestRepository by fakeRepository {
            override fun measureDownload(): Flow<SpeedSample> = flow {
                throw SpeedTestException(SpeedTestError.Timeout)
            }
        }
        val emitted = mutableListOf<SpeedTestPhase>()

        val error = runCatching {
            RunSpeedTestUseCase(failing)().collect { emitted += it.phase }
        }.exceptionOrNull()

        assertEquals(SpeedTestError.Timeout, (error as SpeedTestException).error)
        assertTrue(SpeedTestPhase.UPLOAD !in emitted)
    }

    private companion object {
        const val DELTA = 0.0001
    }
}
