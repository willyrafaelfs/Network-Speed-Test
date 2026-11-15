package io.github.willyrafaelfs.speedtest.testing

import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions
import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * SpeedTestRepository yang hasilnya ditentukan test. [sampleDelayMillis] memberi jeda
 * (virtual time) di antara sampel agar skenario pembatalan bisa diuji.
 */
class FakeSpeedTestRepository(
    var ping: PingResult = PingResult(latencyMs = 20.0, jitterMs = 2.0),
    var downloadSamples: List<SpeedSample> = DEFAULT_DOWNLOAD,
    var uploadSamples: List<SpeedSample> = DEFAULT_UPLOAD,
    var sampleDelayMillis: Long = 0,
    /** Bila diisi, fase download gagal dengan exception ini. */
    var downloadFailure: SpeedTestException? = null,
) : SpeedTestRepository {

    override suspend fun measurePing(): PingResult = ping

    override fun measureDownload(options: MeasurementOptions): Flow<SpeedSample> = flow {
        downloadFailure?.let { throw it }
        emitAll(downloadSamples)
    }

    override fun measureUpload(options: MeasurementOptions): Flow<SpeedSample> = flow { emitAll(uploadSamples) }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<SpeedSample>.emitAll(samples: List<SpeedSample>) {
        samples.forEach {
            if (sampleDelayMillis > 0) delay(sampleDelayMillis)
            emit(it)
        }
    }

    companion object {
        val DEFAULT_DOWNLOAD = listOf(
            SpeedSample(instantMbps = 80.0, averageMbps = 80.0, progressFraction = 0.5f),
            SpeedSample(instantMbps = 95.0, averageMbps = 92.5, progressFraction = 1f, loadedLatencyMs = 60.0),
        )
        val DEFAULT_UPLOAD = listOf(
            SpeedSample(instantMbps = 30.0, averageMbps = 30.0, progressFraction = 0.5f),
            SpeedSample(instantMbps = 35.0, averageMbps = 33.0, progressFraction = 1f, loadedLatencyMs = 45.0),
        )
    }
}
