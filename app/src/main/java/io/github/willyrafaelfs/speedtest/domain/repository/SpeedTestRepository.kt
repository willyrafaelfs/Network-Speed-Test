package io.github.willyrafaelfs.speedtest.domain.repository

import io.github.willyrafaelfs.speedtest.domain.model.PingResult
import io.github.willyrafaelfs.speedtest.domain.model.SpeedSample
import io.github.willyrafaelfs.speedtest.domain.model.SpeedTestException
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak untuk engine pengukuran kecepatan.
 *
 * Metode download/upload mengembalikan [Flow] agar bisa memancarkan sampel
 * kecepatan secara real-time selama proses berlangsung. Sampel terakhir tiap
 * Flow memuat hasil final fase tersebut.
 *
 * Semua kegagalan dilempar sebagai [SpeedTestException]; pembatalan tetap
 * berupa CancellationException biasa.
 */
interface SpeedTestRepository {

    /** Mengukur latency & jitter saat koneksi idle. */
    suspend fun measurePing(): PingResult

    /** Mengunduh data lewat beberapa koneksi paralel dan memancarkan [SpeedSample] secara berkala. */
    fun measureDownload(): Flow<SpeedSample>

    /** Mengunggah data acak lewat beberapa koneksi paralel dan memancarkan [SpeedSample] secara berkala. */
    fun measureUpload(): Flow<SpeedSample>
}
