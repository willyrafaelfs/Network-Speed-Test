package com.example.networkspeedtest.domain.model

/**
 * Satu sampel kecepatan yang dipancarkan secara real-time selama fase
 * download/upload berjalan. Engine (tahap 3) memancarkan ini berulang kali
 * agar UI bisa menampilkan angka/gauge yang bergerak.
 *
 * @param instantMbps kecepatan sesaat pada interval sampel ini (Mbps).
 * @param averageMbps rata-rata kumulatif sejak fase dimulai (Mbps) — nilai final fase.
 * @param progressFraction progres fase saat ini dalam rentang 0f..1f.
 */
data class SpeedSample(
    val instantMbps: Double,
    val averageMbps: Double,
    val progressFraction: Float,
)
