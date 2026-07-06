package com.example.networkspeedtest.domain.model

/**
 * Snapshot progres satu sesi test yang dipancarkan terus-menerus ke UI.
 * Field metrik bersifat nullable karena terisi bertahap sesuai fase yang
 * sudah dilewati (mis. downloadMbps masih null selama fase PING).
 *
 * @param phase fase yang sedang berjalan.
 * @param currentSpeedMbps kecepatan sesaat untuk animasi gauge (0 di luar fase download/upload).
 * @param progressFraction progres fase saat ini (0f..1f).
 * @param pingMs hasil ping bila fase PING sudah selesai.
 * @param jitterMs hasil jitter bila fase PING sudah selesai.
 * @param downloadMbps hasil download bila fase DOWNLOAD sudah selesai.
 * @param uploadMbps hasil upload bila fase UPLOAD sudah selesai.
 */
data class SpeedTestProgress(
    val phase: SpeedTestPhase,
    val currentSpeedMbps: Double = 0.0,
    val progressFraction: Float = 0f,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
)
