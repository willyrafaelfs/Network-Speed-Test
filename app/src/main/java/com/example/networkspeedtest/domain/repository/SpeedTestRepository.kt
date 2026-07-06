package com.example.networkspeedtest.domain.repository

import com.example.networkspeedtest.domain.model.PingResult
import com.example.networkspeedtest.domain.model.SpeedSample
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak untuk engine pengukuran kecepatan. Implementasinya (berbasis OkHttp)
 * dibuat di data layer pada tahap 3 & 4.
 *
 * Metode download/upload mengembalikan [Flow] agar bisa memancarkan sampel
 * kecepatan secara real-time selama proses berlangsung.
 */
interface SpeedTestRepository {

    /** Mengukur latency & jitter dengan beberapa kali ping ke server. */
    suspend fun measurePing(): PingResult

    /** Mengunduh data dan memancarkan [SpeedSample] secara berkala. */
    fun measureDownload(): Flow<SpeedSample>

    /** Mengunggah data dummy dan memancarkan [SpeedSample] secara berkala. */
    fun measureUpload(): Flow<SpeedSample>
}
