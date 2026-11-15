package io.github.willyrafaelfs.speedtest.data.repository

import io.github.willyrafaelfs.speedtest.data.remote.DownloadSpeedTester
import io.github.willyrafaelfs.speedtest.data.remote.PingTester
import io.github.willyrafaelfs.speedtest.data.remote.UploadSpeedTester
import io.github.willyrafaelfs.speedtest.data.remote.toSpeedTestException
import io.github.willyrafaelfs.speedtest.domain.model.MeasurementOptions
import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Implementasi [SpeedTestRepository] yang mendelegasikan ke masing-masing engine.
 * Menjadi satu titik masuk untuk domain/use case tanpa perlu tahu detail OkHttp,
 * termasuk menerjemahkan exception jaringan ke SpeedTestException.
 */
class SpeedTestRepositoryImpl @Inject constructor(
    private val pingTester: PingTester,
    private val downloadTester: DownloadSpeedTester,
    private val uploadTester: UploadSpeedTester,
) : SpeedTestRepository {

    // Exception apa pun dari engine diterjemahkan ke SpeedTestException (kecuali pembatalan).
    @Suppress("TooGenericExceptionCaught")
    override suspend fun measurePing(): PingResult = try {
        pingTester.measure()
    } catch (e: Exception) {
        throw e.toSpeedTestException()
    }

    override fun measureDownload(options: MeasurementOptions): Flow<SpeedSample> =
        downloadTester.measure(options).catch { throw it.toSpeedTestException() }

    override fun measureUpload(options: MeasurementOptions): Flow<SpeedSample> =
        uploadTester.measure(options).catch { throw it.toSpeedTestException() }
}
