package com.example.networkspeedtest.data.repository

import com.example.networkspeedtest.data.remote.DownloadSpeedTester
import com.example.networkspeedtest.data.remote.PingTester
import com.example.networkspeedtest.data.remote.UploadSpeedTester
import com.example.networkspeedtest.domain.model.PingResult
import com.example.networkspeedtest.domain.model.SpeedSample
import com.example.networkspeedtest.domain.repository.SpeedTestRepository
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
