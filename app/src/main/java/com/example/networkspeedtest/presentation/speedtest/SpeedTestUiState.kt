package com.example.networkspeedtest.presentation.speedtest

import com.example.networkspeedtest.domain.model.NetworkInfo
import com.example.networkspeedtest.domain.model.SpeedTestPhase

/**
 * State yang dirender oleh layar test. Field metrik nullable karena terisi
 * bertahap sesuai fase yang sudah dilewati.
 *
 * @param phase fase test yang sedang berjalan.
 * @param isRunning true selama test berlangsung (tombol jadi "Batal", input dikunci).
 * @param currentSpeedMbps kecepatan sesaat untuk animasi gauge.
 * @param progressFraction progres fase saat ini (0f..1f).
 * @param pingMs / jitterMs / downloadMbps / uploadMbps hasil tiap fase.
 * @param networkInfo info koneksi terkini (jenis, SSID/operator, IP publik).
 * @param errorMessage pesan error bila test gagal; null bila tidak ada.
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
    val networkInfo: NetworkInfo = NetworkInfo.Unknown,
    val errorMessage: String? = null,
) {
    /** True bila satu sesi test sudah selesai penuh. */
    val isFinished: Boolean get() = phase == SpeedTestPhase.FINISHED
}
