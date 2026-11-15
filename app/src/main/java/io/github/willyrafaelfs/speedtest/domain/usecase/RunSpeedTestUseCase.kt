package io.github.willyrafaelfs.speedtest.domain.usecase

import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestProgress
import io.github.willyrafaelfs.speedtest.domain.repository.SettingsRepository
import io.github.willyrafaelfs.speedtest.domain.repository.SpeedTestRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

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
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<SpeedTestProgress> = flow {
        // Durasi & jumlah koneksi dibaca sekali di awal: mengubah Settings saat test
        // berjalan tidak boleh mengubah test yang sedang diukur.
        val options = settingsRepository.settings.first().measurementOptions

        // Fase 1 — Ping & Jitter (koneksi idle)
        var progress = SpeedTestProgress(phase = SpeedTestPhase.PING)
        emit(progress)
        val ping = speedTestRepository.measurePing()
        progress = progress.copy(progressFraction = 1f, pingMs = ping.latencyMs, jitterMs = ping.jitterMs)
        emit(progress)

        // Fase 2 — Download. Sampel terakhir membawa hasil final + latency saat beban.
        speedTestRepository.measureDownload(options).collect { sample ->
            progress = progress.copy(
                phase = SpeedTestPhase.DOWNLOAD,
                currentSpeedMbps = sample.instantMbps,
                progressFraction = sample.progressFraction,
                downloadMbps = sample.averageMbps,
                downloadLatencyMs = sample.loadedLatencyMs,
            )
            emit(progress)
        }

        // Fase 3 — Upload.
        speedTestRepository.measureUpload(options).collect { sample ->
            progress = progress.copy(
                phase = SpeedTestPhase.UPLOAD,
                currentSpeedMbps = sample.instantMbps,
                progressFraction = sample.progressFraction,
                uploadMbps = sample.averageMbps,
                uploadLatencyMs = sample.loadedLatencyMs,
            )
            emit(progress)
        }

        // Selesai — rangkuman metrik final.
        emit(progress.copy(phase = SpeedTestPhase.FINISHED, currentSpeedMbps = 0.0, progressFraction = 1f))
    }
}
