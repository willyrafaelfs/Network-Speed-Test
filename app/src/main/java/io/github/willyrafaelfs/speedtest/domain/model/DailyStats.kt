package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Rata-rata hasil test dalam satu hari (zona waktu perangkat).
 *
 * @param day tanggal dalam format ISO "yyyy-MM-dd".
 */
data class DailyStats(
    val day: String,
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgPingMs: Double,
    val testCount: Int,
)
