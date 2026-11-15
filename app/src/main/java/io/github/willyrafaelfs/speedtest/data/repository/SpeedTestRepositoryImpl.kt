package io.github.willyrafaelfs.speedtest.data.repository

import io.github.willyrafaelfs.speedtest.data.remote.DownloadSpeedTester
import io.github.willyrafaelfs.speedtest.data.remote.PingTester
import io.github.willyrafaelfs.speedtest.data.remote.UploadSpeedTester
import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementasi [SpeedTestRepository] yang mendelegasikan ke masing-masing engine.
 * Menjadi satu titik masuk untuk domain/use case tanpa perlu tahu detail OkHttp.
 */
class SpeedTestRepositoryImpl @Inject constructor(
    private val pingTester: PingTester,
    private val downloadTester: DownloadSpeedTester,
    private val uploadTester: UploadSpeedTester,
) : SpeedTestRepository {

    override suspend fun measurePing(): PingResult = pingTester.measure()

    override fun measureDownload(): Flow<SpeedSample> = downloadTester.measure()

    override fun measureUpload(): Flow<SpeedSample> = uploadTester.measure()
}
