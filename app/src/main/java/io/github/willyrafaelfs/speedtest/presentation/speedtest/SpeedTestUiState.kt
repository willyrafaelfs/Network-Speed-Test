package io.github.willyrafaelfs.speedtest.presentation.speedtest

import io.github.willyrafaelfs.speedtest.domain.model.BufferbloatGrade
import io.github.willyrafaelfs.speedtest.domain.model.NetworkInfo
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestError
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestPhase
import io.github.willyrafaelfs.speedtest.domain.model.SpeedUnit
import io.github.willyrafaelfs.speedtest.domain.model.TracePoint
import io.github.willyrafaelfs.speedtest.presentation.component.ResultMetrics

/**
 * State yang dirender oleh layar test. Field metrik nullable karena terisi
 * bertahap sesuai fase yang sudah dilewati.
 *
 * @param phase fase test yang sedang berjalan.
 * @param isRunning true selama test berlangsung (tombol jadi "Batal", input dikunci).
 * @param currentSpeedMbps kecepatan sesaat untuk animasi gauge.
 * @param progressFraction progres fase saat ini (0f..1f).
 * @param pingMs / jitterMs / downloadMbps / uploadMbps hasil tiap fase.
 * @param downloadLatencyMs / uploadLatencyMs latency saat beban selama download/upload.
 * @param downloadTrace / uploadTrace titik kecepatan sesaat per fase, untuk grafik real-time.
 * @param networkInfo info koneksi terkini (jenis, SSID/operator, IP publik).
 * @param error penyebab test gagal; null bila tidak ada.
 * @param speedUnit satuan tampilan kecepatan dari Settings.
 */
data class SpeedTestUiState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val isRunning: Boolean = false,
    val currentSpeedMbps: Double = 0.0,
    val progressFraction: Float = 0f,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val downloadLatencyMs: Double? = null,
    val uploadLatencyMs: Double? = null,
    val downloadTrace: List<TracePoint> = emptyList(),
    val uploadTrace: List<TracePoint> = emptyList(),
    val networkInfo: NetworkInfo = NetworkInfo.Unknown,
    val error: SpeedTestError? = null,
    val speedUnit: SpeedUnit = SpeedUnit.MBPS,
) {
    /** Metrik hasil yang sudah terukur sejauh ini, untuk panel readout. */
    val metrics: ResultMetrics
        get() = ResultMetrics(downloadMbps, uploadMbps, pingMs, jitterMs, downloadLatencyMs, uploadLatencyMs)

    /** True bila satu sesi test sudah selesai penuh. */
    val isFinished: Boolean get() = phase == SpeedTestPhase.FINISHED

    /** Grade bufferbloat; tersedia sejak latency saat beban pertama terukur. */
    val bufferbloatGrade: BufferbloatGrade?
        get() = metrics.bufferbloatGrade
}
