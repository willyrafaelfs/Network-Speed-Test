package com.example.networkspeedtest.domain.usecase

import com.example.networkspeedtest.domain.model.SpeedTestPhase
import com.example.networkspeedtest.domain.model.SpeedTestProgress
import com.example.networkspeedtest.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Mengorkestrasi satu sesi test penuh: ping → download → upload, dan
 * memancarkan [SpeedTestProgress] secara real-time di sepanjang prosesnya.
 *
 * Emisi terakhir berfase [SpeedTestPhase.FINISHED] dan memuat seluruh metrik
 * final, yang bisa dipakai ViewModel untuk membentuk SpeedTestResult lalu
 * menyimpannya lewat [SaveTestResultUseCase].
 */
class RunSpeedTestUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository,
) {
    operator fun invoke(): Flow<SpeedTestProgress> = flow {
        // Fase 1 — Ping & Jitter
        emit(SpeedTestProgress(phase = SpeedTestPhase.PING))
        val ping = speedTestRepository.measurePing()
        emit(
            SpeedTestProgress(
                phase = SpeedTestPhase.PING,
                progressFraction = 1f,
                pingMs = ping.avgLatencyMs,
                jitterMs = ping.jitterMs,
            ),
        )

        // Fase 2 — Download (memancarkan sampel real-time)
        var downloadMbps = 0.0
        speedTestRepository.measureDownload().collect { sample ->
            downloadMbps = sample.averageMbps
            emit(
                SpeedTestProgress(
                    phase = SpeedTestPhase.DOWNLOAD,
                    currentSpeedMbps = sample.instantMbps,
                    progressFraction = sample.progressFraction,
                    pingMs = ping.avgLatencyMs,
                    jitterMs = ping.jitterMs,
                    downloadMbps = sample.averageMbps,
                ),
            )
        }

        // Fase 3 — Upload (memancarkan sampel real-time)
        var uploadMbps = 0.0
        speedTestRepository.measureUpload().collect { sample ->
            uploadMbps = sample.averageMbps
            emit(
                SpeedTestProgress(
                    phase = SpeedTestPhase.UPLOAD,
                    currentSpeedMbps = sample.instantMbps,
                    progressFraction = sample.progressFraction,
                    pingMs = ping.avgLatencyMs,
                    jitterMs = ping.jitterMs,
                    downloadMbps = downloadMbps,
                    uploadMbps = sample.averageMbps,
                ),
            )
        }

        // Selesai — rangkuman metrik final
        emit(
            SpeedTestProgress(
                phase = SpeedTestPhase.FINISHED,
                progressFraction = 1f,
                pingMs = ping.avgLatencyMs,
                jitterMs = ping.jitterMs,
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
            ),
        )
    }
}
