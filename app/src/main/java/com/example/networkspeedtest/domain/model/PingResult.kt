package com.example.networkspeedtest.domain.model

/**
 * Hasil pengukuran ping/jitter dari beberapa kali percobaan.
 *
 * @param avgLatencyMs rata-rata latency seluruh sampel (ms).
 * @param jitterMs rata-rata selisih absolut antar-sampel berurutan (ms).
 */
data class PingResult(
    val avgLatencyMs: Double,
    val jitterMs: Double,
)
