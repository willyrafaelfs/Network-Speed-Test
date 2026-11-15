package io.github.willyrafaelfs.speedtest.domain.model

/**
 * Satu sampel kecepatan yang dipancarkan secara real-time selama fase
 * download/upload berjalan, agar UI bisa menampilkan angka/gauge yang bergerak.
 *
 * Sampel terakhir sebuah fase (progress 1f) memuat hasil final fase tersebut.
 *
 * @param instantMbps kecepatan sesaat (jendela pendek terakhir), untuk gauge (Mbps).
 * @param averageMbps estimasi hasil fase sejauh ini; pada sampel terakhir = hasil final (Mbps).
 * @param progressFraction progres fase saat ini dalam rentang 0f..1f.
 * @param loadedLatencyMs median latency yang diukur selama fase ini berjalan (ms),
 *                        null bila belum ada sampel.
 */
data class SpeedSample(
    val instantMbps: Double,
    val averageMbps: Double,
    val progressFraction: Float,
    val loadedLatencyMs: Double? = null,
)
